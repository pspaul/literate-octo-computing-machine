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
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.server.WebApp;
import org.savapage.server.ext.ServerPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callback servlet for Web API providers.
 * <p>
 * See {@code web.xml}.
 * </p>
 *
 * @author Datraverse B.V.
 */
public final class CallbackServlet extends HttpServlet implements
        ServiceEntryPoint {

    /**
     * The {@link Logger}.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(CallbackServlet.class);

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void init() {
    }

    /**
     *
     * @param httpRequest
     * @param httpResponse
     * @throws IOException
     * @throws ServletException
     */
    private int onCallback(final HttpServletRequest httpRequest,
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

        /**
         *
         */
        ServiceContext.open();

        final DaoContext daoContext = ServiceContext.getDaoContext();

        /*
         * Find a plug-in to handle the request: the first plug-in will do.
         */
        final ServerPluginManager pluginManager =
                WebApp.get().getPluginManager();

        Integer httpStatus = null;

        daoContext.beginTransaction();

        try {

            for (final PaymentGatewayPlugin plugin : pluginManager
                    .getPaymentGatewayPlugins()) {

                if (!plugin.getCallbackSubPath().equals(pathInfo)) {
                    continue;
                }

                final Integer status =
                        plugin.onCallBack(pathInfo, urlQueryString,
                                httpRequest.getParameterMap());

                if (status != null) {
                    daoContext.commit();
                    httpStatus = status;
                    break;
                }
            }

        } catch (PaymentGatewayException e) {

            throw new ServletException(e.getMessage(), e);

        } finally {
            daoContext.rollback();
            ServiceContext.close();
        }

        if (httpStatus == null) {
            httpStatus = Integer.valueOf(HttpStatus.OK_200);
            LOGGER.warn(String.format("%s [%s] not handled: return [%d]",
                    pathInfo, urlQueryString, httpStatus.intValue()));
        }

        return httpStatus;
    }

    @Override
    public void doGet(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse) throws IOException,
            ServletException {

        httpResponse.setStatus(onCallback(httpRequest, httpResponse));
    }

    @Override
    public void doPost(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse) throws IOException,
            ServletException {

        httpResponse.setStatus(onCallback(httpRequest, httpResponse));
    }

}
