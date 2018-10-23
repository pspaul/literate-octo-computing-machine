/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
import java.util.EnumSet;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dao.helpers.JsonUserGroupAccess;
import org.savapage.core.job.SpJobScheduler;
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
import org.savapage.core.json.rpc.JsonRpcParserException;
import org.savapage.core.json.rpc.ParamsPaging;
import org.savapage.core.json.rpc.impl.ParamsAddInternalUser;
import org.savapage.core.json.rpc.impl.ParamsAuthUserSource;
import org.savapage.core.json.rpc.impl.ParamsChangeBaseCurrency;
import org.savapage.core.json.rpc.impl.ParamsNameValue;
import org.savapage.core.json.rpc.impl.ParamsPrinterAccessControl;
import org.savapage.core.json.rpc.impl.ParamsPrinterSnmp;
import org.savapage.core.json.rpc.impl.ParamsSetUserGroupProperties;
import org.savapage.core.json.rpc.impl.ParamsSetUserProperties;
import org.savapage.core.json.rpc.impl.ParamsSingleFilterList;
import org.savapage.core.json.rpc.impl.ParamsSourceGroupMembers;
import org.savapage.core.json.rpc.impl.ParamsSyncUsers;
import org.savapage.core.json.rpc.impl.ParamsUniqueName;
import org.savapage.core.json.rpc.impl.ResultUserGroupAccess;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ConfigPropertyService;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.services.UserGroupService;
import org.savapage.core.services.UserService;
import org.savapage.core.snmp.SnmpConnectException;
import org.savapage.core.users.IExternalUserAuthenticator;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.InetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "JsonRpcServlet", urlPatterns = {
        JsonRpcServlet.URL_PATTERN_BASE, JsonRpcServlet.URL_PATTERN_BASE_V1 })
public final class JsonRpcServlet extends HttpServlet
        implements ServiceEntryPoint {

    public static final String URL_PATTERN_BASE = "/jsonrpc";
    public static final String URL_PATTERN_BASE_V1 = URL_PATTERN_BASE + "/v1";

    /** */
    private static final String HEADER_X_AUTH_KEY = "X-Auth-Key";

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(JsonRpcServlet.class);

    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /**
     * .
     */
    private static final ConfigPropertyService CONFIG_PROPERTY_SERVICE =
            ServiceContext.getServiceFactory().getConfigPropertyService();

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

    /**
     * JSON-RPC public methods.
     */
    private static final EnumSet<JsonRpcMethodName> PUBLIC_METHODS =
            EnumSet.of(JsonRpcMethodName.AUTH_USER_SOURCE,
                    JsonRpcMethodName.SYSTEM_STATUS);

    /** */
    private static final AtomicLong ACCESS_VIOLATION_COUNTER_PRIVATE =
            new AtomicLong(0L);

    /** */
    private static final AtomicLong ACCESS_VIOLATION_COUNTER_PUBLIC =
            new AtomicLong(0L);

    /** */
    private static final long ACCESS_VIOLATION_CHECK_MSEC =
            DateUtil.DURATION_MSEC_HOUR;

    /** */
    private static long lastAccessViolationsCheck;

    /** */
    private static Thread threadSignalAccessViolation =
            new Thread(new Runnable() {

                @Override
                public void run() {

                    lastAccessViolationsCheck = System.currentTimeMillis();

                    try {
                        while (true) {
                            Thread.sleep(ACCESS_VIOLATION_CHECK_MSEC);
                            checkAccessViolations();
                        }
                    } catch (InterruptedException e) {
                        // no code intended
                    }
                }
            });

    /**
     * Checks and logs access violations.
     */
    private static void checkAccessViolations() {

        final long lastAccessViolationsCheckPrv = lastAccessViolationsCheck;
        lastAccessViolationsCheck = System.currentTimeMillis();

        final long countPrv = ACCESS_VIOLATION_COUNTER_PRIVATE.getAndSet(0);
        final long countPub = ACCESS_VIOLATION_COUNTER_PUBLIC.getAndSet(0);

        if (countPrv == 0 && countPub == 0) {
            return;
        }

        final StringBuilder msg = new StringBuilder();

        msg.append("JSON-RPC denied during last ")
                .append(DateUtil.formatDuration(lastAccessViolationsCheck
                        - lastAccessViolationsCheckPrv))
                .append(" :");

        if (countPrv > 0) {
            msg.append(" ").append(countPrv).append(" private");
        }

        if (countPrv > 0 && countPub > 0) {
            msg.append(",");
        }

        if (countPub > 0) {
            msg.append(" ").append(countPub).append(" public");
        }
        msg.append(" (see server.log for details).");

        LOGGER.warn(msg.toString());
        AdminPublisher.instance().publish(PubTopicEnum.SERVER_COMMAND,
                PubLevelEnum.WARN, msg.toString());
        AppLogHelper.log(AppLogLevelEnum.WARN, msg.toString());

    }

    @Override
    public void init() {
        threadSignalAccessViolation.start();
        SpInfo.instance().log("JSON-RPC monitoring started.");
    }

    @Override
    public void destroy() {
        checkAccessViolations();
        SpInfo.instance().log("Shutting down JSON-RPC monitor ...");
        super.destroy();
    }

    /**
     * @param isPrivateApi
     *            If {@code true}, this is a private JSON-RPC access violation.
     */
    private static void onAccessViolation(final boolean isPrivateApi) {
        if (isPrivateApi) {
            ACCESS_VIOLATION_COUNTER_PRIVATE.incrementAndGet();
        } else {
            ACCESS_VIOLATION_COUNTER_PUBLIC.incrementAndGet();
        }
    }

    /**
     * Creates a method exception.
     *
     * @param ex
     *            The {@link Throwable}.
     * @param log
     *            If {@code true}, log exception as error.
     * @return The {@link JsonRpcMethodError}.
     */
    private JsonRpcMethodError createMethodException(final Throwable ex,
            final boolean log) {

        if (log) {
            LOGGER.error(ex.getMessage(), ex);
        }

        if (ex.getCause() == null) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INTERNAL_ERROR, ex.getMessage());
        }

        return JsonRpcMethodError.createBasicError(
                JsonRpcError.Code.INTERNAL_ERROR, ex.getMessage(),
                ex.getCause().getMessage());
    }

    @Override
    public void doPost(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse)
            throws IOException, ServletException {

        final AbstractJsonRpcMessage rpcResponse = handleRequest(httpRequest);
        final String jsonOut = rpcResponse.stringifyPrettyPrinted();

        httpResponse.setContentType(JsonRpcConfig.INTERNET_MEDIA_TYPE);
        httpResponse.setCharacterEncoding(JsonRpcConfig.CHAR_ENCODING);

        httpResponse.getOutputStream().print(jsonOut);
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

        if (methodName == JsonRpcMethodName.SYSTEM_STATUS
                || methodName == JsonRpcMethodName.AUTH_USER_SOURCE) {
            return;
        }

        if (methodName == JsonRpcMethodName.CHANGE_BASE_CURRENCY) {
            ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(lock);
        } else {
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(lock);
        }
    }

    /**
     * Checks invariants.
     *
     * @param httpRequest
     * @param methodParser
     * @param methodName
     * @return {@code null} when all invariants are satisfied.
     */
    private JsonRpcMethodError checkInvariants(
            final HttpServletRequest httpRequest,
            final JsonRpcMethodParser methodParser,
            final JsonRpcMethodName methodName) {

        final String secretKey = httpRequest.getHeader(HEADER_X_AUTH_KEY);
        final boolean isPrivateApi = StringUtils.isBlank(secretKey);

        /*
         * INVARIANT (ACCESS): SSL only.
         */
        if (!httpRequest.isSecure()) {

            onAccessViolation(isPrivateApi);

            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Access denied.",
                    "Secure connection required.");
        }

        final String clientAddress = httpRequest.getRemoteAddr();
        final String serverAdress;

        try {
            serverAdress = InetUtils.getServerHostAddress();
        } catch (UnknownHostException e1) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INTERNAL_ERROR, "Access denied.",
                    "Server IP address could not be retrieved.");
        }

        /*
         * INVARIANT (ACCESS): Public API, valid API key and client IP on white
         * list.
         */
        if (!isPrivateApi) {

            final String cidrRanges = ConfigManager.instance()
                    .getConfigValue(Key.API_JSONRPC_IP_ADDRESSES_ALLOWED);

            if (StringUtils.isBlank(cidrRanges)
                    || !InetUtils.isIp4AddrInCidrRanges(cidrRanges,
                            clientAddress)
                    || !secretKey.equals(ConfigManager.instance()
                            .getConfigValue(Key.API_JSONRPC_SECRET_KEY))) {

                onAccessViolation(isPrivateApi);

                return JsonRpcMethodError.createBasicError(
                        JsonRpcError.Code.INVALID_REQUEST, "Access denied.",
                        "Not authorized");
            }
        }

        /*
         * INVARIANT (ACCESS): Private API, localhost access only.
         */
        if (isPrivateApi && !clientAddress.equals(serverAdress)) {

            onAccessViolation(isPrivateApi);

            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Access denied.",
                    String.format(
                            "Client [%s] must be on same platform "
                                    + "as server [%s].",
                            clientAddress, serverAdress));
        }

        /*
         * INVARIANT: JSON must be valid.
         */
        if (methodParser.getMethod() == null) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                    "No method specified.");
        }

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
        /*
         * INVARIANT (ACCESS): API key MUST be present.
         */
        if (isPrivateApi && !methodParser.hasParams()) {
            onAccessViolation(isPrivateApi);
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                    "Parameters are missing.");
        }

        /*
         * INVARIANT (ACCESS): API key MUST be valid.
         */
        if (isPrivateApi) {

            final String apiKey = methodParser.getApiKey();

            if (apiKey == null) {
                onAccessViolation(isPrivateApi);
                return JsonRpcMethodError.createBasicError(
                        JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                        "API Key is missing.");
            }

            if (!JsonRpcConfig.isApiKeyValid(JsonRpcConfig.API_INTERNAL_ID,
                    apiKey)) {
                onAccessViolation(isPrivateApi);
                return JsonRpcMethodError.createBasicError(
                        JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                        "Invalid API Key.");
            }

        } else if (!PUBLIC_METHODS.contains(methodName)) {
            onAccessViolation(isPrivateApi);
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INVALID_REQUEST, "Invalid request.",
                    "Method [" + methodParser.getMethod()
                            + "] is not supported.");
        }

        return null;
    }

    /**
     *
     * @param httpRequest
     *            The HTTP request.
     * @return The response message.
     */
    private AbstractJsonRpcMessage
            handleRequest(final HttpServletRequest httpRequest) {

        final String jsonInput;

        try {
            jsonInput = IOUtils.toString(httpRequest.getInputStream());
        } catch (IOException e) {
            return createMethodException(e, false);
        }

        // IMPORTANT: do NOT log since it exposes the API Key.

        final JsonRpcMethodParser methodParser;

        try {
            methodParser = new JsonRpcMethodParser(jsonInput);
        } catch (IOException e) {
            return JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.PARSE_ERROR, "JSON parsing error.",
                    "JSON syntax is not valid.");
        }

        final JsonRpcMethodName methodName =
                JsonRpcMethodName.asEnum(methodParser.getMethod());

        final JsonRpcMethodError rpcMethodError =
                checkInvariants(httpRequest, methodParser, methodName);

        if (rpcMethodError != null) {
            rpcMethodError.setId(methodParser.getId());
            logResponse(methodName, rpcMethodError, httpRequest);
            return rpcMethodError;
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

            case AUTH_USER_SOURCE:

                final IExternalUserAuthenticator userAuth =
                        ConfigManager.instance().getUserAuthenticator();

                if (userAuth == null) {
                    rpcResponse = JsonRpcMethodError.createBasicError(
                            JsonRpcError.Code.INTERNAL_ERROR,
                            "External user source not configured.");
                } else {
                    final ParamsAuthUserSource authParms =
                            methodParser.getParams(ParamsAuthUserSource.class);
                    try {
                        rpcResponse = JsonRpcMethodResult.createBooleanResult(
                                userAuth.authenticate(authParms.getUserName(),
                                        authParms.getPassword()) != null);
                    } catch (Throwable e) {
                        rpcResponse = createMethodException(e, false);
                    }
                }
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

            case ERASE_USER:
                rpcResponse = USER_SERVICE.eraseUser(methodParser
                        .getParams(ParamsUniqueName.class).getUniqueName());
                break;

            case GET_CONFIG_PROPERTY:
                rpcResponse = CONFIG_PROPERTY_SERVICE.getPropertyValue(
                        methodParser.getParams(ParamsUniqueName.class)
                                .getUniqueName());
                break;

            case SET_CONFIG_PROPERTY:
                rpcResponse = CONFIG_PROPERTY_SERVICE.setPropertyValue(
                        methodParser.getParams(ParamsNameValue.class));
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

            case SYNC_USERS_AND_GROUPS:

                SpJobScheduler.instance().scheduleOneShotUserSync(false,
                        BooleanUtils.isTrue(
                                methodParser.getParams(ParamsSyncUsers.class)
                                        .getDeleteUsers()));
                rpcResponse = JsonRpcMethodResult.createOkResult();
                break;

            case SYSTEM_STATUS:
                rpcResponse = JsonRpcMethodResult.createEnumResult(
                        ConfigManager.instance().getSystemStatus());
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

        } catch (JsonProcessingException | JsonRpcParserException e) {

            rpcResponse = JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.PARSE_ERROR, "JSON parsing error.",
                    "JSON method parameters are not valid.");

        } catch (SnmpConnectException e) {

            rpcResponse = JsonRpcMethodError.createBasicError(
                    JsonRpcError.Code.INTERNAL_ERROR, "SNMP connect error.",
                    e.getMessage());

        } catch (Throwable e) {

            rpcResponse = createMethodException(e, true);

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
        logResponse(methodName, rpcResponse, httpRequest);
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
            final AbstractJsonRpcMessage rpcResponse,
            final HttpServletRequest httpRequest) {

        final PubLevelEnum level;
        final String msg;

        if (rpcResponse instanceof AbstractJsonRpcMethodResponse) {

            AbstractJsonRpcMethodResponse methodRsp =
                    (AbstractJsonRpcMethodResponse) rpcResponse;

            if (methodRsp.isError()) {
                level = PubLevelEnum.ERROR;

                final JsonRpcMethodError methodError = methodRsp.asError();
                msg = methodError.getError().getMessage();

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

        final StringBuilder msgTxt = new StringBuilder();

        if (StringUtils.isBlank(httpRequest.getHeader(HEADER_X_AUTH_KEY))) {
            msgTxt.append("Server Command");
        } else {
            msgTxt.append("JSON-RPC");
        }

        if (methodName != null) {
            msgTxt.append(" \"").append(methodName.getMethodName())
                    .append("\"");
        }

        msgTxt.append(" from ").append(httpRequest.getRemoteAddr());

        if (StringUtils.isNotBlank(msg)) {
            msgTxt.append(" : ").append(msg);
        }

        final String message = msgTxt.toString();

        AdminPublisher.instance().publish(PubTopicEnum.SERVER_COMMAND, level,
                message);

        switch (level) {
        case ERROR:
            LOGGER.error(message);
            break;
        case WARN:
            LOGGER.warn(message);
            break;
        case INFO:
            // no break intended
        default:
            LOGGER.info(message);
            break;
        }
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
