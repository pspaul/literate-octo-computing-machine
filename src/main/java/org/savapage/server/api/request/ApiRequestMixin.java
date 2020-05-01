/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.jpa.User;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.ResultDataBasic;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.DeviceService;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.PrintDelegationService;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserGroupService;
import org.savapage.core.services.UserService;
import org.savapage.core.util.Messages;
import org.savapage.ext.notification.JobTicketCancelEvent;
import org.savapage.ext.notification.JobTicketCloseEvent;
import org.savapage.ext.notification.JobTicketEvent;
import org.savapage.ext.notification.NotificationListener;
import org.savapage.ext.papercut.services.PaperCutService;
import org.savapage.server.WebApp;
import org.savapage.server.api.UserAgentHelper;
import org.savapage.server.session.SpSession;
import org.savapage.server.webapp.WebAppHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ApiRequestMixin implements ApiRequestHandler {

    private static final String REQ_KEY_DTO = "dto";

    private static final String RSP_KEY_DTO = "dto";
    private static final String RSP_KEY_CODE = "code";
    private static final String RSP_KEY_RESULT = "result";
    private static final String RSP_KEY_MSG = "msg";
    private static final String RSP_KEY_TXT = "txt";

    /**
     *
     */
    private final Map<String, Object> responseMap =
            new HashMap<String, Object>();

    private RequestCycle requestCycle;
    private PageParameters pageParameters;
    private boolean isGetAction;

    /**
     * .
     */
    protected static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     * .
     */
    protected static final AccessControlService ACCESSCONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /**
     * .
     */
    protected static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /**
     * .
     */
    protected static final DeviceService DEVICE_SERVICE =
            ServiceContext.getServiceFactory().getDeviceService();

    /**
     * .
     */
    protected static final InboxService INBOX_SERVICE =
            ServiceContext.getServiceFactory().getInboxService();

    /**
     * .
     */
    protected static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /**
     * .
     */
    protected static final OutboxService OUTBOX_SERVICE =
            ServiceContext.getServiceFactory().getOutboxService();

    /**
     * .
     */
    protected static final PaperCutService PAPERCUT_SERVICE =
            ServiceContext.getServiceFactory().getPaperCutService();

    /**
     * .
     */
    protected static final PrinterService PRINTER_SERVICE =
            ServiceContext.getServiceFactory().getPrinterService();

    /**
     * .
     */
    protected static final PrintDelegationService PRINT_DELEGATION_SERVICE =
            ServiceContext.getServiceFactory().getPrintDelegationService();

    /**
     * .
     */
    protected static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();

    /**
     * .
     */
    protected static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /**
     * .
     */
    protected static final UserGroupService USER_GROUP_SERVICE =
            ServiceContext.getServiceFactory().getUserGroupService();

    /**
     * .
     */
    protected static final AccountDao ACCOUNT_DAO =
            ServiceContext.getDaoContext().getAccountDao();

    /**
     * .
     */
    protected static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();
    //

    @Override
    public final Map<String, Object> process(final RequestCycle requestCycle,
            final PageParameters parameters, final boolean isGetAction,
            final String requestingUser, final User lockedUser)
            throws Exception {

        this.requestCycle = requestCycle;
        this.pageParameters = parameters;
        this.isGetAction = isGetAction;

        onRequest(requestingUser, lockedUser);
        return this.getResponseMap();
    }

    /**
     * Notifies the API request.
     *
     * @param requestingUser
     *            The user if of the requesting user.
     * @param lockedUser
     *            The locked {@link User} instance: is {@code null} when user is
     *            <i>not</i> locked.
     * @throws Exception
     *             When an unexpected error is encountered.
     */
    protected abstract void onRequest(String requestingUser, User lockedUser)
            throws Exception;

    /**
     * @return {@code true} when notification listener is active.
     */
    protected static boolean hasNotificationListener() {
        return WebApp.get().getPluginManager().hasNotificationListener();
    }

    /**
     * @return The notification listener.
     */
    protected static NotificationListener getNotificationListener() {
        return WebApp.get().getPluginManager();
    }

    /**
     *
     * @return The response {@link Map}.
     */
    private Map<String, Object> getResponseMap() {
        return responseMap;
    }

    /**
     * @return <code>String</code> containing the IP address of the client that
     *         sent the request.
     */
    protected final String getClientIP() {
        return WebAppHelper.getClientIP(RequestCycle.get().getRequest());
    }

    /**
     * Gets the authenticated {@link WebAppTypeEnum} from {@link SpSession}.
     *
     * @return The {@link WebAppTypeEnum}.
     */
    protected final WebAppTypeEnum getSessionWebAppType() {

        WebAppTypeEnum webAppType = WebAppTypeEnum.UNDEFINED;

        final SpSession session = SpSession.get();

        if (session != null) {
            webAppType = session.getWebAppType();
        }
        return webAppType;
    }

    /**
     * Gets the {@link getUserIdDto} from {@link SpSession}.
     *
     * @return The {@link getUserIdDto}.
     */
    protected final UserIdDto getSessionUserIdDto() {
        return SpSession.get().getUserIdDto();
    }

    /**
     * @return The primary database key of authenticated {@link SpSession} User,
     *         or {@code null} if no authenticated {@link User}.
     */
    protected final Long getSessionUserDbKey() {
        return SpSession.get().getUserDbKey();
    }

    /**
     * @deprecated
     * @return The response {@link Map}.
     */
    @Deprecated
    protected final Map<String, Object> getUserData() {
        return responseMap;
    }

    /**
     * @return The {@link Locale}.
     */
    protected final Locale getLocale() {
        return ServiceContext.getLocale();
    }

    /**
     * @return The currency symbol according to the {@link Locale}.
     */
    protected final String getCurrencySymbol() {
        return ServiceContext.getAppCurrencySymbol();
    }

    /**
     * Sets the response.
     *
     * @param response
     *            The {@link AbstractDto} response.
     * @throws IOException
     *             When an IO error occurs.
     */
    protected final void setResponse(final AbstractDto response)
            throws IOException {
        this.getResponseMap().put(RSP_KEY_DTO, response.asMap());
    }

    /**
     * Return a localized message string. IMPORTANT: The locale from the
     * {@link ServiceContext} is used.
     *
     * @param key
     *            The key of the message.
     * @return The message text.
     */
    protected final String localize(final String key) {
        return this.localize(ServiceContext.getLocale(), key);
    }

    /**
     * Return a localized message string.
     *
     * @param locale
     *            Locale.
     * @param key
     *            The key of the message.
     * @return The message text.
     */
    protected final String localize(final Locale locale, final String key) {
        return Messages.getMessage(getClass(), locale, key, (String[]) null);
    }

    /**
     * Return a localized message string. IMPORTANT: The locale from the
     * {@link ServiceContext} is used.
     *
     * @param key
     *            The key of the message.
     * @param args
     *            Message placeholder arguments.
     * @return The message text.
     */
    protected final String localize(final String key, final String... args) {
        return localize(ServiceContext.getLocale(), key, args);
    }

    /**
     * Return a localized message string.
     *
     * @param locale
     *            Locale.
     * @param key
     *            The key of the message.
     * @param args
     *            Message placeholder arguments.
     * @return The message text.
     */
    protected final String localize(final Locale locale, final String key,
            final String... args) {
        return Messages.getMessage(getClass(), locale, key, args);
    }

    /**
     * Creates the API result on parameter {@code out}.
     *
     * @param code
     *            The {@link ApiResultCodeEnum}.
     * @param msg
     *            The key of the message
     * @param txt
     *            The message text.
     */
    private void createApiResult(final ApiResultCodeEnum code, final String msg,
            final String txt) {
        final Map<String, Object> out = this.getResponseMap();
        createApiResult(out, code, msg, txt);
    }

    /**
     * Creates the API result on parameter {@code out}.
     *
     * @param out
     *            The Map to put the result on.
     * @param code
     *            The {@link #ApiResultCodeEnum}.
     * @param msg
     *            The key of the message
     * @param txt
     *            The message text.
     * @return the {@code out} parameter.
     *
     */
    public static Map<String, Object> createApiResult(
            final Map<String, Object> out, final ApiResultCodeEnum code,
            final String msg, final String txt) {

        final Map<String, Object> result = new HashMap<String, Object>();

        out.put(RSP_KEY_RESULT, result);

        result.put(RSP_KEY_CODE, code.getValue());

        if (msg != null) {
            result.put(RSP_KEY_MSG, msg);
        }

        if (txt != null) {
            result.put(RSP_KEY_TXT, txt);
        }
        return out;
    }

    /**
     * Creates API result with a single text message.
     *
     * @param code
     *            The {@link ApiResultCodeEnum}.
     * @param text
     *            The message text
     * @return the {@code out} parameter.
     */
    public static Map<String, Object> createApiResultText(
            final ApiResultCodeEnum code, final String text) {
        final Map<String, Object> out = new HashMap<>();
        return createApiResult(out, code, "msg-single-parm", text);
    }

    /**
     * Sets the API result with a single text message.
     *
     * @param code
     *            The {@link ApiResultCodeEnum}.
     * @param text
     *            The message text
     */
    protected final void setApiResultText(final ApiResultCodeEnum code,
            final String text) {
        this.createApiResult(code, "msg-single-parm", text);
    }

    /**
     * @param rpcResponse
     *            The {@link AbstractJsonRpcMethodResponse}.
     * @return {@code true} when result is OK.
     */
    protected static boolean
            isApiResultOk(final AbstractJsonRpcMethodResponse rpcResponse) {
        return rpcResponse.isResult();
    }

    /**
     * @param code
     *            The {@link #ApiResultCodeEnum}.
     * @return {@code true} if the result code equals the enum value.
     */
    protected boolean isApiResultCode(final ApiResultCodeEnum code) {
        return isApiResultCode(EnumSet.of(code));
    }

    /**
     *
     * @param set
     *            The {@link EnumSet} of codes to checks.
     * @return {@code true} if the result code equals one of the codes in the
     *         {@link EnumSet}.
     */
    protected boolean isApiResultCode(final EnumSet<ApiResultCodeEnum> set) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> result =
                (Map<String, Object>) responseMap.get("result");

        for (final ApiResultCodeEnum code : set) {
            if (result.get("code").equals(code.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return
     */
    protected boolean isApiResultOk() {
        return isApiResultCode(ApiResultCodeEnum.OK);
    }

    /**
     * Sets the API result from JSOn method response.
     *
     * @param rpcResponse
     *            The {@link AbstractJsonRpcMethodResponse}.
     */
    protected final void
            setApiResultText(final AbstractJsonRpcMethodResponse rpcResponse) {

        if (rpcResponse.isResult()) {

            final ResultDataBasic result = rpcResponse.asResult().getResult()
                    .data(ResultDataBasic.class);

            this.setApiResultText(ApiResultCodeEnum.OK, result.getMessage());

        } else {

            final JsonRpcError error = rpcResponse.asError().getError();
            final StringBuilder text = new StringBuilder();

            if (StringUtils.isNotBlank(error.getMessage())) {
                text.append(error.getMessage());
            }

            final ErrorDataBasic errorData = error.data(ErrorDataBasic.class);

            if (errorData != null
                    && StringUtils.isNotBlank(errorData.getReason())) {

                final boolean hasMessage = text.length() > 0;

                if (hasMessage) {
                    text.append(" : ");
                }
                text.append(errorData.getReason());
            }

            this.setApiResultText(ApiResultCodeEnum.ERROR, text.toString());
        }
    }

    /**
     * Sets the API result.
     *
     * @param code
     *            The {@link ApiResultCodeEnum}.
     * @param key
     *            The key of the message
     * @param args
     *            The placeholder arguments for the message.
     */
    protected final void setApiResult(final ApiResultCodeEnum code,
            final String key, final String... args) {
        createApiResult(code, key, localize(key, args));
    }

    /**
     * Sets the API result.
     *
     * @param code
     *            The {@link ApiResultCodeEnum}.
     * @param key
     *            The key of the message
     */
    protected final void setApiResult(final ApiResultCodeEnum code,
            final String key) {
        createApiResult(code, key, localize(key));
    }

    /**
     * Sets the API result to {@link ApiResultCodeEnum#OK}.
     */
    protected final void setApiResultOk() {
        createApiResult(ApiResultCodeEnum.OK, null, null);
    }

    /**
     *
     * @return The JSON string value of the "dto" POST or GET parameter.
     */
    protected final String getParmValueDto() {
        return this.getParmValue(REQ_KEY_DTO);
    }

    /**
     * Gets the POST or GET parameter value.
     *
     * @param parm
     *            The parameter name.
     * @return The parameter value.
     */
    protected final String getParmValue(final String parm) {

        if (this.isGetAction) {
            return this.pageParameters.get(parm).toString();
        }
        /*
         * Get the POST-ed parameter.
         */
        return this.requestCycle.getRequest().getPostParameters()
                .getParameterValue(parm).toString();
    }

    /**
     * @return The {@link UserAgentHelper}.
     */
    protected final UserAgentHelper createUserAgentHelper() {
        final HttpServletRequest request =
                (HttpServletRequest) this.requestCycle.getRequest()
                        .getContainerRequest();
        return new UserAgentHelper(request);
    }

    /**
     * Fills {@link JobTicketEvent} object.
     *
     * @param event
     *            The event to fill.
     * @param requestingUser
     *            Userid of job ticket operator.
     * @param user
     *            The job ticket creator.
     * @param dto
     *            The job ticket.
     */
    private void fillEventCommon(final JobTicketEvent event,
            final String requestingUser, final User user,
            final OutboxJobDto dto) {

        final User userOperator =
                USER_DAO.findActiveUserByUserId(requestingUser);

        event.setDocumentName(dto.getJobName());
        event.setOperator(requestingUser);

        if (userOperator == null
                || StringUtils.isBlank(userOperator.getFullName())) {
            event.setOperatorName(requestingUser);
        } else {
            event.setOperatorName(userOperator.getFullName());
        }

        event.setTicketNumber(dto.getTicketNumber());
        event.setCreator(user.getUserId());

        if (StringUtils.isBlank(user.getFullName())) {
            event.setCreatorName(user.getUserId());
        } else {
            event.setCreatorName(user.getFullName());
        }

        event.setLocale(getLocale());
    }

    /**
     * Fills {@link JobTicketCancelEvent} object.
     *
     * @param event
     *            The event to fill.
     * @param requestingUser
     *            Userid of job ticket operator.
     * @param user
     *            The job ticket creator.
     * @param dto
     *            The job ticket.
     * @param reason
     *            A reason text.
     *
     * @return The event (same as input parameter).
     */
    protected final JobTicketCancelEvent fillEvent(
            final JobTicketCancelEvent event, final String requestingUser,
            final User user, final OutboxJobDto dto, final String reason) {

        fillEventCommon(event, requestingUser, user, dto);
        event.setReason(reason);
        return event;
    }

    /**
     * Fills {@link JobTicketCloseEvent} object.
     *
     * @param event
     *            The event to fill.
     * @param requestingUser
     *            Userid of job ticket operator.
     * @param user
     *            The job ticket creator.
     * @param dto
     *            The job ticket.
     *
     * @return The event (same as input parameter).
     */
    protected final JobTicketCloseEvent fillEvent(
            final JobTicketCloseEvent event, final String requestingUser,
            final User user, final OutboxJobDto dto) {

        fillEventCommon(event, requestingUser, user, dto);
        return event;
    }

}
