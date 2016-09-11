/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.jsonrpc;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Currency;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dao.helpers.JsonUserGroupAccess;
import org.savapage.core.jpa.Entity;
import org.savapage.core.json.rpc.AbstractJsonRpcMessage;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcConfig;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.JsonRpcError.Code;
import org.savapage.core.json.rpc.JsonRpcMethodError;
import org.savapage.core.json.rpc.JsonRpcMethodName;
import org.savapage.core.json.rpc.JsonRpcMethodParser;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.json.rpc.ParamsPaging;
import org.savapage.core.json.rpc.impl.ParamsAddInternalUser;
import org.savapage.core.json.rpc.impl.ParamsChangeBaseCurrency;
import org.savapage.core.json.rpc.impl.ParamsPrinterAccessControl;
import org.savapage.core.json.rpc.impl.ParamsPrinterSnmp;
import org.savapage.core.json.rpc.impl.ParamsSetUserGroupProperties;
import org.savapage.core.json.rpc.impl.ParamsSetUserProperties;
import org.savapage.core.json.rpc.impl.ParamsSingleFilterList;
import org.savapage.core.json.rpc.impl.ParamsSourceGroupMembers;
import org.savapage.core.json.rpc.impl.ParamsUniqueName;
import org.savapage.core.json.rpc.impl.ResultUserGroupAccess;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.services.UserGroupService;
import org.savapage.core.services.UserService;
import org.savapage.core.snmp.SnmpConnectException;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.InetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "JsonRpcServlet", urlPatterns = { "/jsonrpc" })
public final class JsonRpcServlet extends HttpServlet
        implements ServiceEntryPoint {

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(JsonRpcServlet.class);

    private static final long serialVersionUID = 1L;

    private static final String LOOPBACK_ADDRESS = "127.0.0.1";

    /**
     * .
     */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /**
     * .
     */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /**
     * .
     */
    private static final UserGroupService USER_GROUP_SERVICE =
            ServiceContext.getServiceFactory().getUserGroupService();

    /**
     * .
     */
    private static final PrinterService PRINTER_SERVICE =
            ServiceContext.getServiceFactory().getPrinterService();

    /**
     * .
     */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    @Override
    public void init() {
    }

    /**
     * Creates a method exception.
     *
     * @param ex
     *            The {@link Exception}.
     * @return The {@link JsonRpcMethodError}.
     */
    private JsonRpcMethodError createMethodException(final Exception ex) {

        LOGGER.error(ex.getMessage(), ex);

        return JsonRpcMethodError.createBasicError(
                JsonRpcError.Code.INTERNAL_ERROR,
                "Server exception: " + ex.getClass().getSimpleName(),
                ex.getMessage());
    }

    @Override
    public void doPost(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse)
            throws IOException, ServletException {

        AbstractJsonRpcMessage rpcResponse = null;

        try {
            rpcResponse = handleRequest(httpRequest);
        } catch (Exception e) {
            rpcResponse = createMethodException(e);
        }

        /*
         * Write the response.
         */
        final String jsonOut = rpcResponse.stringifyPrettyPrinted();

        httpResponse.getOutputStream().print(jsonOut);
        httpResponse.setContentType(JsonRpcConfig.INTERNET_MEDIA_TYPE);

    }

    /**
     * Locks or unlocks the database.
     *
     * @param methodName
     *            The {@link JsonRpcMethodName}.
     * @param lock
     *            {@code true==lock, false==unlock}.
     */
    private static void setDatabaseLock(final JsonRpcMethodName methodName,
            final boolean lock) {

        if (methodName == JsonRpcMethodName.CHANGE_BASE_CURRENCY) {
            ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(lock);
        } else {
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(lock);
        }
    }

    /**
     *
     * @param httpRequest
     * @return
     */
    private AbstractJsonRpcMessage
            handleRequest(final HttpServletRequest httpRequest) {

        /*
         * INVARIANT: secure access only.
         */
        if (!httpRequest.isSecure()) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Access denied.",
                    "Secure connection required.");
        }

        /*
         * INVARIANT: (for now) localhost access only.
         */
        final String serverAdress;
        try {
            serverAdress = InetUtils.getServerHostAddress();
        } catch (UnknownHostException e1) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INTERNAL_ERROR, "Access denied.",
                    "Server IP address could not be retrieved.");
        }

        if (!httpRequest.getRemoteAddr().equals(serverAdress)
                && !httpRequest.getRemoteAddr().equals(LOOPBACK_ADDRESS)) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Access denied.",
                    "Client must be localhost.");
        }

        /*
         * Read the request.
         */
        String jsonInput;
        try {
            jsonInput = IOUtils.toString(httpRequest.getInputStream());
        } catch (IOException e) {
            return createMethodException(e);
        }

        /*
         * IMPORTANT: do NOT log since it exposes the API Key.
         */
        // theLogger.trace(jsonInput);

        /*
         * Is JSON well-formed?
         */

        // JsonRpcMethod rpcRequest = null;

        JsonRpcMethodParser methodParser;

        try {

            methodParser = new JsonRpcMethodParser(jsonInput);

        } catch (IOException e) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.PARSE_ERROR, "JSON parsing error.",
                    "JSON syntax is not valid.");
        }

        /*
         * Is JSON valid?
         */
        if (methodParser.getMethod() == null) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                    "No method specified.");
        }

        final JsonRpcMethodName methodName =
                JsonRpcMethodName.asEnum(methodParser.getMethod());

        if (methodName == null) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                    "Method [" + methodParser.getMethod() + "] is unknown.");
        }

        //
        if (methodParser.getJsonrpc() == null) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                    "No JSON-RPC version specified.");
        }
        if (!methodParser.getJsonrpc().equals(JsonRpcConfig.RPC_VERSION)) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                    "Wrong JSON-RPC version [" + methodParser.getJsonrpc()
                            + "].");
        }
        //
        if (!methodParser.hasParams()) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                    "Parameters are missing.");
        }

        /*
         * INVARIANT: API key MUST be valid.
         */
        final String apiKey = methodParser.getApiKey();

        if (apiKey == null) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                    "API Key is missing.");
        }

        if (!JsonRpcConfig.isApiKeyValid(JsonRpcConfig.API_INTERNAL_ID,
                apiKey)) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                    "Invalid API Key.");
        }

        /*
         * Process request.
         */
        ServiceContext.open();

        ServiceContext.setActor(Entity.ACTOR_SYSTEM_API);
        ServiceContext.setLocale(Locale.getDefault()); // TODO: JSON param

        final DaoContext daoContext = ServiceContext.getDaoContext();

        setDatabaseLock(methodName, true);

        AbstractJsonRpcMessage rpcResponse = null;

        DaoBatchCommitter batchCommitter = null;

        try {

            daoContext.beginTransaction();

            switch (methodName) {

            case ADD_INTERNAL_USER:
                rpcResponse = USER_SERVICE.addInternalUser(methodParser
                        .getParams(ParamsAddInternalUser.class).getUser());
                break;

            case ADD_USER_GROUP:

                batchCommitter = openBatchCommitter();

                rpcResponse = USER_GROUP_SERVICE.addUserGroup(batchCommitter,
                        methodParser.getParams(ParamsUniqueName.class)
                                .getUniqueName());
                break;

            case CHANGE_BASE_CURRENCY:

                final ParamsChangeBaseCurrency parmsChangeBaseCurrency =
                        methodParser.getParams(ParamsChangeBaseCurrency.class);

                batchCommitter = openBatchCommitter();
                batchCommitter.setTest(parmsChangeBaseCurrency.isTest());

                rpcResponse = ACCOUNTING_SERVICE.changeBaseCurrency(
                        batchCommitter,
                        Currency.getInstance(
                                parmsChangeBaseCurrency.getCurrencyCodeFrom()),
                        Currency.getInstance(
                                parmsChangeBaseCurrency.getCurrencyCodeTo()),
                        parmsChangeBaseCurrency.getExchangeRate());

                break;

            case DELETE_USER:
                rpcResponse = USER_SERVICE.deleteUserAutoCorrect(methodParser
                        .getParams(ParamsUniqueName.class).getUniqueName());
                break;

            case DELETE_USER_GROUP:
                rpcResponse = USER_GROUP_SERVICE.deleteUserGroup(methodParser
                        .getParams(ParamsUniqueName.class).getUniqueName());
                break;

            case LIST_USERS:

                final ParamsPaging parmsListUsers =
                        methodParser.getParams(ParamsPaging.class);

                rpcResponse =
                        USER_SERVICE.listUsers(parmsListUsers.getStartIndex(),
                                parmsListUsers.getItemsPerPage());
                break;

            case LIST_USER_GROUPS:

                final ParamsPaging parmsListUserGroups =
                        methodParser.getParams(ParamsPaging.class);

                rpcResponse = USER_GROUP_SERVICE.listUserGroups(
                        parmsListUserGroups.getStartIndex(),
                        parmsListUserGroups.getItemsPerPage());
                break;

            case LIST_USER_GROUP_MEMBERS:

                final ParamsSingleFilterList parmsGroupMembers =
                        methodParser.getParams(ParamsSingleFilterList.class);

                rpcResponse = USER_GROUP_SERVICE.listUserGroupMembers(
                        parmsGroupMembers.getFilter(),
                        parmsGroupMembers.getStartIndex(),
                        parmsGroupMembers.getItemsPerPage());
                break;

            case LIST_USER_GROUP_MEMBERSHIPS:

                final ParamsSingleFilterList parmsMemberships =
                        methodParser.getParams(ParamsSingleFilterList.class);

                rpcResponse = USER_GROUP_SERVICE.listUserGroupMemberships(
                        parmsMemberships.getFilter(),
                        parmsMemberships.getStartIndex(),
                        parmsMemberships.getItemsPerPage());
                break;

            case LIST_USER_SOURCE_GROUP_MEMBERS:

                final ParamsSourceGroupMembers parmsSourceGroupMembers =
                        methodParser.getParams(ParamsSourceGroupMembers.class);

                rpcResponse = USER_GROUP_SERVICE.listUserSourceGroupMembers(
                        parmsSourceGroupMembers.getGroupName(),
                        parmsSourceGroupMembers.getNested());

                break;

            case LIST_USER_SOURCE_GROUPS:

                rpcResponse = USER_GROUP_SERVICE.listUserSourceGroups();

                break;

            case LIST_USER_SOURCE_GROUP_NESTING:

                rpcResponse = USER_GROUP_SERVICE.listUserSourceGroupNesting(
                        methodParser.getParams(ParamsUniqueName.class)
                                .getUniqueName());
                break;

            case PRINTER_ACCESS_CONTROL:

                rpcResponse = handlePrinterAccessControl(methodParser
                        .getParams(ParamsPrinterAccessControl.class));
                break;

            case PRINTER_SNMP:

                rpcResponse = PROXY_PRINT_SERVICE.readSnmp(
                        methodParser.getParams(ParamsPrinterSnmp.class));
                break;

            case SET_USER_PROPERTIES:

                rpcResponse = USER_SERVICE.setUserProperties(
                        methodParser.getParams(ParamsSetUserProperties.class)
                                .getUserProperties());
                break;

            case SET_USER_GROUP_PROPERTIES:

                rpcResponse =
                        USER_GROUP_SERVICE.setUserGroupProperties(methodParser
                                .getParams(ParamsSetUserGroupProperties.class)
                                .getUserGroupProperties());
                break;

            case SYNC_USER_GROUP:

                batchCommitter = openBatchCommitter();

                rpcResponse = USER_GROUP_SERVICE.syncUserGroup(batchCommitter,
                        methodParser.getParams(ParamsUniqueName.class)
                                .getUniqueName());
                break;

            default:
                rpcResponse = JsonRpcMethodError.createBasicError(
                        JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                        "Method [" + methodParser.getMethod()
                                + "] is not implemented.");
                break;
            }

            if (batchCommitter == null) {
                daoContext.commit();
            } else {
                batchCommitter.commit();
            }

        } catch (JsonProcessingException e) {

            rpcResponse = JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.PARSE_ERROR, "JSON parsing error.",
                    "JSON method parameters are not valid.");

        } catch (SnmpConnectException e) {

            rpcResponse = JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INTERNAL_ERROR, "SNMP connect error.",
                    e.getMessage());

        } catch (IOException e) {

            rpcResponse = createMethodException(e);

        } finally {

            /*
             * First statement.
             */
            setDatabaseLock(methodName, false);

            if (batchCommitter == null) {
                daoContext.rollback();
            } else {
                batchCommitter.rollback();
                batchCommitter.close();
            }

            ServiceContext.close();
        }

        rpcResponse.setId(methodParser.getId());

        logResponse(methodName, rpcResponse);

        return rpcResponse;
    }

    /**
     *
     * @param methodName
     *            The {@link JsonRpcMethodName}.
     * @param rpcResponse
     *            The {@link AbstractJsonRpcMessage}.
     */
    private static void logResponse(final JsonRpcMethodName methodName,
            final AbstractJsonRpcMessage rpcResponse) {

        final PubLevelEnum level;
        final String msg;

        if (rpcResponse instanceof AbstractJsonRpcMethodResponse) {

            AbstractJsonRpcMethodResponse methodRsp =
                    (AbstractJsonRpcMethodResponse) rpcResponse;

            if (methodRsp.isError()) {
                level = PubLevelEnum.ERROR;
                msg = methodRsp.asError().getError().getMessage();
            } else {
                level = PubLevelEnum.INFO;
                msg = null;
            }

        } else if (rpcResponse instanceof JsonRpcMethodError) {

            JsonRpcMethodError error = (JsonRpcMethodError) rpcResponse;

            msg = error.getError().getMessage();
            level = PubLevelEnum.ERROR;

        } else {
            msg = null;
            level = PubLevelEnum.INFO;
        }

        final StringBuilder msgTxt =
                new StringBuilder().append("Server Command \"")
                        .append(methodName.getMethodName()).append("\"");

        if (StringUtils.isNotBlank(msg)) {
            msgTxt.append(": ").append(msg);
        }

        AdminPublisher.instance().publish(PubTopicEnum.SERVER_COMMAND, level,
                msgTxt.toString());

        final AppLogLevelEnum logLevel;

        switch (level) {
        case ERROR:
            logLevel = AppLogLevelEnum.ERROR;
            break;
        case WARN:
            logLevel = AppLogLevelEnum.WARN;
            break;
        case INFO:
            // no break intended
        default:
            logLevel = AppLogLevelEnum.INFO;
            break;
        }

        AppLogHelper.log(logLevel, msgTxt.toString());
    }

    /**
     *
     * @param parms
     *            The parameters.
     * @return The response.
     * @throws IOException
     *             When JSON error.
     */
    private AbstractJsonRpcMessage handlePrinterAccessControl(
            final ParamsPrinterAccessControl parms) throws IOException {

        switch (parms.getAction()) {

        case ADD:
            return PRINTER_SERVICE.addAccessControl(parms.getScope(),
                    parms.getPrinterName(), parms.getGroupName());

        case LIST:

            final JsonUserGroupAccess access =
                    PRINTER_SERVICE.getAccessControl(parms.getPrinterName());

            if (access == null) {
                return JsonRpcMethodError.createBasicError(
                        Code.INVALID_REQUEST, "Printer ["
                                + parms.getPrinterName() + "] does not exist.",
                        null);
            } else {

                final ResultUserGroupAccess data = new ResultUserGroupAccess();
                data.setUserGroupAccess(access);
                return JsonRpcMethodResult.createResult(data);
            }

        case REMOVE:
            return PRINTER_SERVICE.removeAccessControl(parms.getPrinterName(),
                    parms.getGroupName());

        case REMOVE_ALL:
            return PRINTER_SERVICE.removeAccessControl(parms.getPrinterName());

        default:
            throw new SpException(
                    "Unhandled action [" + parms.getAction() + "].");
        }
    }

    /**
     * Creates and opens a {@link DaoBatchCommitter}.
     *
     * @return The committer.
     */
    private DaoBatchCommitter openBatchCommitter() {
        final DaoBatchCommitter committer = ServiceContext.getDaoContext()
                .createBatchCommitter(ConfigManager.getDaoBatchChunkSize());
        committer.open();
        return committer;
    }
}
