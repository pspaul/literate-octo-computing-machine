/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.Messages;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ApiRequestMixin implements ApiRequestHandler {

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
     *            The locked {@link User} instance, can be {@code null}.
     * @throws Exception
     *             When an unexpected error is encountered.
     */
    protected abstract void onRequest(final String requestingUser,
            final User lockedUser) throws Exception;

    /**
     *
     * @return The response {@link Map}.
     */
    private Map<String, Object> getResponseMap() {
        return responseMap;
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
     *
     * @return The {@link Locale}.
     */
    protected final Locale getLocale() {
        return ServiceContext.getLocale();
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
    private String localize(final String key) {
        return Messages.getMessage(getClass(), ServiceContext.getLocale(), key,
                (String[]) null);
    }

    /**
     * Return a localized message string. IMPORTANT: The locale from the
     * {@link ServiceContext} is used.
     *
     * @param key
     *            The key of the message.
     * @return The message text.
     */
    private String localize(final String key, final String... args) {
        return Messages.getMessage(getClass(), ServiceContext.getLocale(), key,
                args);
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
    private void createApiResult(final ApiResultCodeEnum code,
            final String msg, final String txt) {

        final Map<String, Object> out = this.getResponseMap();

        final Map<String, Object> result = new HashMap<String, Object>();

        out.put(RSP_KEY_RESULT, result);

        result.put(RSP_KEY_CODE, code.getValue());

        if (msg != null) {
            result.put(RSP_KEY_MSG, msg);
        }

        if (txt != null) {
            result.put(RSP_KEY_TXT, txt);
        }
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
        createApiResult(code, "msg-single-parm", text);
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

}
