/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
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
package org.savapage.server.callback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.util.AppLogHelper;
import org.savapage.ext.ServerPlugin;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.ext.payment.PaymentGatewayPlugin.CallbackResponse;
import org.savapage.server.WebApp;
import org.savapage.server.ext.ServerPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callback servlet for Web API providers.
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "CallbackServlet",
        urlPatterns = { CallbackServlet.SERVLET_URL_PATTERN })
public final class CallbackServlet extends HttpServlet
        implements ServiceEntryPoint {

    /**
     * .
     */
    private static final String PATH_BASE = "callback";

    /**
     * .
     */
    public static final String SERVLET_URL_PATTERN = "/" + PATH_BASE + "/*";

    /**
     * .
     */
    private static final String SUB_PATH_0_PAYMENT = "payment";

    /**
     * .
     */
    private static final String SUB_PATH_1_TEST = "test";

    /**
     * .
     */
    private static final String SUB_PATH_1_LIVE = "live";

    /**
     * The {@link Logger}.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(CallbackServlet.class);

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     * Gets the callback {@link URL} for a {@link PaymentGatewayPlugin}.
     *
     * @param plugin
     *            The {@link PaymentGatewayPlugin}.
     * @return The callback {@link URL}.
     * @throws MalformedURLException
     *             When format of the URL is invalid.
     */
    public static URL getCallBackUrl(final PaymentGatewayPlugin plugin)
            throws MalformedURLException {

        final String subPath1;

        if (plugin.isLive()) {
            subPath1 = SUB_PATH_1_LIVE;
        } else {
            subPath1 = SUB_PATH_1_TEST;
        }

        return getCallBackUrl(plugin, SUB_PATH_0_PAYMENT, subPath1);
    }

    /**
     * Gets the callback {@link URL} for a {@link ServerPlugin}.
     *
     * @param plugin
     *            The {@link ServerPlugin}.
     * @param subPath0
     *            The first "node" of the URL sub path.
     * @param subPath1
     *            The second "node"of the URL sub path. Can be {@code null}.
     * @return The callback {@link URL}.
     * @throws MalformedURLException
     *             When format of the URL is invalid.
     */
    private static URL getCallBackUrl(final ServerPlugin plugin,
            final String subPath0, final String subPath1)
            throws MalformedURLException {

        final StringBuilder url = new StringBuilder();

        final String urlBase = ConfigManager.instance()
                .getConfigValue(Key.EXT_WEBAPI_CALLBACK_URL_BASE);

        if (StringUtils.isBlank(urlBase)) {
            throw new IllegalStateException(
                    "WebAPI callback URL base is not specified.");
        }

        url.append(urlBase).append('/').append(PATH_BASE).append('/')
                .append(subPath0).append('/');

        if (subPath1 != null) {
            url.append(subPath1).append('/');
        }

        url.append(plugin.getId());

        return new URL(url.toString());
    }

    /**
     * Chunks the pathinfo from an {@link HttpServletRequest} into an array of
     * sub paths.
     *
     * @param httpRequest
     *            The {@link HttpServletRequest}.
     * @return {@code null} when the pathInfo is not valid.
     */
    private static String[]
            chunkPathInfo(final HttpServletRequest httpRequest) {

        final String[] pathChunks =
                StringUtils.split(httpRequest.getPathInfo(), '/');

        if (pathChunks == null || pathChunks.length == 0) {
            return null;
        }

        if (pathChunks.length == 3
                && pathChunks[0].equals(SUB_PATH_0_PAYMENT)) {

            if (pathChunks[1].equals(SUB_PATH_1_LIVE)
                    || pathChunks[1].equals(SUB_PATH_1_TEST)) {
                return pathChunks;
            }
        }

        return null;
    }

    /**
     * Handles the Payment callback.
     *
     * @param plugin
     *            The plug-in.
     * @param httpRequest
     *            The {@link HttpServletRequest}.
     * @param httpResponse
     *            The {@link HttpServletResponse}.
     * @param isPostRequest
     *            {@code true} when callback is a POST request.
     * @param isLive
     *            {@code true} when this is a live callback. {@code false} when
     *            this is a test callback.
     * @return The {@link CallbackResponse}.
     * @throws ServletException
     *             When internal error.
     * @throws IOException
     *             When IO error.
     */
    private CallbackResponse onCallback(final PaymentGatewayPlugin plugin,
            final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse, final boolean isPostRequest,
            final boolean isLive) throws ServletException, IOException {

        CallbackResponse callbackResponse =
                new CallbackResponse(HttpStatus.OK_200);

        final PrintWriter writer =
                new PrintWriter(httpResponse.getOutputStream(), true);

        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
        ServiceContext.open();

        final DaoContext daoContext = ServiceContext.getDaoContext();
        daoContext.beginTransaction();

        try {

            final BufferedReader reader;

            if (isPostRequest) {
                /*
                 * Do NOT get the reader to prevent IllegalStateException when
                 * getting the calling httpRequest.getParameterMap() later on:
                 * all data is passed in POST parameters.
                 */
                reader = null;
            } else {
                reader = httpRequest.getReader();
            }

            callbackResponse = plugin.onCallBack(httpRequest.getParameterMap(),
                    isLive, ConfigManager.getAppCurrency(), reader, writer);

            daoContext.commit();

            plugin.onCallBackCommitted(callbackResponse.getPluginObject());

        } catch (PaymentGatewayException e) {
            /*
             * CAUTION: do NOT expose the urlQueryString in the logging, since
             * (depending on the plug-in) it could contain sensitive (secret)
             * data.
             */
            final String msg = AppLogHelper.logWarning(getClass(),
                    "paymentgateway-warning", plugin.getId(),
                    httpRequest.getPathInfo(), e.getMessage(),
                    String.valueOf(callbackResponse.getHttpStatus()));

            AdminPublisher.instance().publish(PubTopicEnum.PAYMENT_GATEWAY,
                    PubLevelEnum.WARN, msg);

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);

            /*
             * WARNING: see warning above.
             */
            final String msg = AppLogHelper.logError(getClass(),
                    "paymentgateway-exception", plugin.getId(),
                    httpRequest.getPathInfo(), e.getClass().getSimpleName(),
                    e.getMessage());

            AdminPublisher.instance().publish(PubTopicEnum.PAYMENT_GATEWAY,
                    PubLevelEnum.ERROR, msg);

            throw new ServletException(e.getMessage(), e);

        } finally {
            daoContext.rollback();
            ServiceContext.close();
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
        }

        // IMPORTANT: flush anything left in the buffer!
        writer.flush();

        //
        return callbackResponse;
    }

    /**
     * Handles the callback.
     *
     * @param httpRequest
     *            The {@link HttpServletRequest}.
     * @param httpResponse
     *            The {@link HttpServletResponse}.
     * @param isPostRequest
     *            {@code true} when callback is a POST request.
     * @throws IOException
     *             if an input or output error is detected when the servlet
     *             handles the GET request.
     * @throws ServletException
     *             if the request for the GET could not be handled.
     */
    private void onCallback(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse, final boolean isPostRequest)
            throws IOException, ServletException {

        final String pathInfo = httpRequest.getPathInfo();

        if (LOGGER.isTraceEnabled()) {

            final StringBuilder builder = new StringBuilder();

            builder.append(httpRequest.getMethod()).append(": ");

            if (pathInfo != null) {
                builder.append(pathInfo);
            }

            /*
             * CAUTION: do NOT log the httpRequest.getParameterMap() and
             * httpRequest.getQueryString() since they may contain sensitive
             * data (passwords, etc).
             */

            LOGGER.trace(builder.toString());
        }

        CallbackResponse callbackResponse =
                new CallbackResponse(HttpStatus.OK_200);

        final String[] pathChunks = chunkPathInfo(httpRequest);

        if (pathChunks != null) {

            /*
             * Find a plug-in to handle the request: the first plug-in will do.
             */
            final ServerPluginManager pluginManager =
                    WebApp.get().getPluginManager();

            if (pathChunks[0].equals(SUB_PATH_0_PAYMENT)) {

                final PaymentGatewayPlugin plugin =
                        pluginManager.getPaymentPlugins().get(pathChunks[2]);

                if (plugin != null) {
                    final boolean isLive =
                            pathChunks[1].equals(SUB_PATH_1_LIVE);
                    callbackResponse = onCallback(plugin, httpRequest,
                            httpResponse, isPostRequest, isLive);
                }
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("%s handled: return [%d]", pathInfo,
                    callbackResponse.getHttpStatus()));
        }

        if (StringUtils.isNotBlank(callbackResponse.getContentType())) {
            httpResponse.setContentType(callbackResponse.getContentType());
        }

        httpResponse.setStatus(callbackResponse.getHttpStatus());
    }

    @Override
    public void doGet(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse)
            throws IOException, ServletException {

        onCallback(httpRequest, httpResponse, false);
    }

    @Override
    public void doPost(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse)
            throws IOException, ServletException {

        onCallback(httpRequest, httpResponse, true);
    }
}
