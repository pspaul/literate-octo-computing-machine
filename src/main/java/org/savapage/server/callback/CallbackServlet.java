/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.callback;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;

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
 * @author Datraverse B.V.
 * @since 0.9.9
 */
@WebServlet(name = "CallbackServlet",
        urlPatterns = { CallbackServlet.SERVLET_URL_PATTERN })
public final class CallbackServlet extends HttpServlet implements
        ServiceEntryPoint {

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
    private static final Logger LOGGER = LoggerFactory
            .getLogger(CallbackServlet.class);

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

        final StringBuilder url = new StringBuilder();

        final String urlBase =
                ConfigManager.instance().getConfigValue(
                        Key.EXT_WEBAPI_CALLBACK_URL_BASE);

        if (StringUtils.isBlank(urlBase)) {
            throw new IllegalStateException(
                    "WebAPI callback URL base is not specified.");
        }

        url.append(urlBase).append('/').append(PATH_BASE).append('/')
                .append(SUB_PATH_0_PAYMENT).append('/');

        if (plugin.isLive()) {
            url.append(SUB_PATH_1_LIVE);
        } else {
            url.append(SUB_PATH_1_TEST);
        }

        url.append('/').append(plugin.getId());

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
    private static String[] chunkPathInfo(final HttpServletRequest httpRequest) {

        final String[] pathChunks =
                StringUtils.split(httpRequest.getPathInfo(), '/');

        if (pathChunks != null && pathChunks.length == 3
                && pathChunks[0].equals(SUB_PATH_0_PAYMENT)) {

            if (pathChunks[1].equals(SUB_PATH_1_LIVE)
                    || pathChunks[1].equals(SUB_PATH_1_TEST)) {
                return pathChunks;
            }
        }

        return null;
    }

    /**
     *
     * @param httpRequest
     * @param httpResponse
     * @throws IOException
     * @throws ServletException
     */
    private void onCallback(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse) throws IOException,
            ServletException {

        final String pathInfo = httpRequest.getPathInfo();

        String urlQueryString = httpRequest.getQueryString();

        if (httpRequest.getQueryString() == null) {
            urlQueryString = "";
        }

        if (LOGGER.isTraceEnabled()) {

            final StringBuilder builder = new StringBuilder();

            builder.append(httpRequest.getMethod()).append(": ");

            if (pathInfo != null) {
                builder.append(pathInfo);
            }

            if (urlQueryString != null) {
                builder.append(" [").append(urlQueryString).append(']');
            }

            for (final Entry<String, String[]> entry : httpRequest
                    .getParameterMap().entrySet()) {
                builder.append("\n").append(entry.getKey()).append(" :");
                for (final String value : entry.getValue()) {
                    builder.append(" [").append(value).append("]");
                }
            }

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

            final PaymentGatewayPlugin plugin =
                    pluginManager.getPaymentPlugins().get(pathChunks[2]);

            if (plugin != null) {

                final PrintWriter writer =
                        new PrintWriter(httpResponse.getOutputStream(), true);

                ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
                ServiceContext.open();
                final DaoContext daoContext = ServiceContext.getDaoContext();
                daoContext.beginTransaction();

                try {

                    callbackResponse =
                            plugin.onCallBack(httpRequest.getParameterMap(),
                                    pathChunks[1].equals(SUB_PATH_1_LIVE),
                                    ConfigManager.getAppCurrency(), writer);

                    daoContext.commit();

                } catch (PaymentGatewayException e) {
                    /*
                     * WARNING: do NOT expose the urlQueryString in the logging,
                     * since (depending on the plug-in) it could contain
                     * sensitive (secret) data.
                     */
                    final String msg =
                            AppLogHelper.logWarning(getClass(),
                                    "paymentgateway-warning", plugin.getId(),
                                    pathInfo, e.getMessage(), String
                                            .valueOf(callbackResponse
                                                    .getHttpStatus()));

                    AdminPublisher.instance().publish(
                            PubTopicEnum.PAYMENT_GATEWAY, PubLevelEnum.WARN,
                            msg);

                } catch (Exception e) {

                    LOGGER.error(e.getMessage(), e);

                    /*
                     * WARNING: see warning above.
                     */
                    final String msg =
                            AppLogHelper.logError(getClass(),
                                    "paymentgateway-exception", plugin.getId(),
                                    pathInfo, e.getClass().getSimpleName(),
                                    e.getMessage());

                    AdminPublisher.instance().publish(
                            PubTopicEnum.PAYMENT_GATEWAY, PubLevelEnum.ERROR,
                            msg);

                    throw new ServletException(e.getMessage(), e);

                } finally {
                    daoContext.rollback();
                    ServiceContext.close();
                    ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
                }

                // Important: flush anything left in the buffer!
                writer.flush();
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("%s [%s] handled: return [%d]",
                    pathInfo, urlQueryString, callbackResponse.getHttpStatus()));
        }

        if (StringUtils.isNotBlank(callbackResponse.getContentType())) {
            httpResponse.setContentType(callbackResponse.getContentType());
        }

        httpResponse.setStatus(callbackResponse.getHttpStatus());
    }

    @Override
    public void doGet(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse) throws IOException,
            ServletException {

        onCallback(httpRequest, httpResponse);
    }

    @Override
    public void doPost(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse) throws IOException,
            ServletException {

        onCallback(httpRequest, httpResponse);
    }

}
