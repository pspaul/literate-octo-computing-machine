/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.server.xmlrpc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.webserver.XmlRpcServlet;

/**
 * Wrapper class for the main {@link XmlRpcServlet} to capture the Client IP
 * Address in a static {@link ThreadLocal} variable.
 * <p>
 * Note: {@code "enabledForExtensions" = "true"} enables vendor extensions
 * support for XML-RPC.
 * </p>
 *
 * @author Datraverse B.V.
 */
@WebServlet(name = "XmlRpcServlet", urlPatterns = { "/xmlrpc" },
        initParams = { @WebInitParam(name = "enabledForExtensions",
                value = "true"), })
public final class SpXmlRpcServlet extends XmlRpcServlet {

    private static final long serialVersionUID = 1L;

    private static ThreadLocal<String> clientIpAddress = new ThreadLocal<>();
    private static ThreadLocal<Boolean> isSslConnection = new ThreadLocal<>();

    /**
     * Gets the IP address of the client.
     * <p>
     * Adapted from <a
     * href="http://ws.apache.org/xmlrpc/faq.html#client_ip">http://ws.apache.
     * org/xmlrpc/faq.html#client_ip</a>
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
    public void doPost(final HttpServletRequest pRequest,
            final HttpServletResponse pResponse) throws IOException,
            ServletException {

        clientIpAddress.set(pRequest.getRemoteAddr());
        isSslConnection.set(Boolean.valueOf(pRequest.isSecure()));

        super.doPost(pRequest, pResponse);
    }

}
