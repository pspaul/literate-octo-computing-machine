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
package org.savapage.server.xmlrpc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.metadata.XmlRpcSystemImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;

/**
 * Wrapper class for the main {@link XmlRpcServlet} to capture the Client IP
 * Address in a static {@link ThreadLocal} variable.
 * <p>
 * Note: {@code "enabledForExtensions" = "true"} enables vendor extensions
 * support for XML-RPC.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "XmlRpcServlet",
        urlPatterns = { SpXmlRpcServlet.URL_PATTERN_BASE,
                SpXmlRpcServlet.URL_PATTERN_BASE_V1 },
        initParams = {
                @WebInitParam(name = "enabledForExtensions", value = "true"), })
public final class SpXmlRpcServlet extends XmlRpcServlet {

    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    public static final String URL_PATTERN_BASE = "/xmlrpc";
    public static final String URL_PATTERN_BASE_V1 = URL_PATTERN_BASE + "/v1";

    private static final String HANDLER_KEY_ADMIN = "admin";
    private static final String HANDLER_KEY_CLIENT = "client";
    private static final String HANDLER_KEY_CUPS_EVENT = "cups-event";
    private static final String HANDLER_KEY_RFID_EVENT = "rfid-event";
    private static final String HANDLER_KEY_ONE_TIME_AUTH = "onetime-auth";

    private static ThreadLocal<String> clientIpAddress = new ThreadLocal<>();
    private static ThreadLocal<Boolean> isSslConnection = new ThreadLocal<>();

    /**
     * Gets the IP address of the client.
     * <p>
     * Adapted from
     * <a href="http://ws.apache.org/xmlrpc/faq.html#client_ip">http://ws.
     * apache. org/xmlrpc/faq.html#client_ip</a>
     * </p>
     *
     * @return IP address of the client.
     */
    public static String getClientIpAddress() {
        return clientIpAddress.get();
    }

    /**
     * @return {@code true} if this is an SSL (secure) connection.
     */
    public static boolean isSslConnection() {
        return isSslConnection.get().booleanValue();
    }

    @Override
    protected XmlRpcHandlerMapping newXmlRpcHandlerMapping()
            throws XmlRpcException {

        final PropertyHandlerMapping mapping = new PropertyHandlerMapping();

        mapping.addHandler(HANDLER_KEY_ADMIN, AdminHandler.class);
        mapping.addHandler(HANDLER_KEY_CLIENT, ClientAppHandler.class);
        mapping.addHandler(HANDLER_KEY_CUPS_EVENT, CupsEventHandler.class);
        mapping.addHandler(HANDLER_KEY_RFID_EVENT, RfidEventHandler.class);
        mapping.addHandler(HANDLER_KEY_ONE_TIME_AUTH, OneTimeAuthHandler.class);

        /*
         * This call configures the XML-RPC server for introspection (i.e. it
         * implements introspection methods under the XML-RPC system.
         * namespace).
         */
        XmlRpcSystemImpl.addSystemHandler(mapping);

        return mapping;
    }

    @Override
    public void doPost(final HttpServletRequest pRequest,
            final HttpServletResponse pResponse)
            throws IOException, ServletException {

        clientIpAddress.set(pRequest.getRemoteAddr());
        isSslConnection.set(Boolean.valueOf(pRequest.isSecure()));

        super.doPost(pRequest, pResponse);
    }

}
