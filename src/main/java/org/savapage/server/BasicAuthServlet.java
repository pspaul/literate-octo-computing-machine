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
package org.savapage.server;

import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Base64;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.savapage.server.webapp.WebAppHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class BasicAuthServlet extends HttpServlet {

    /** */
    private static final long serialVersionUID = -8742672385466251710L;

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(BasicAuthServlet.class);

    /**
     * Check username and password acquired by basic authentication.
     *
     * @param username
     *            Username.
     * @param pw
     *            Password
     * @return {@code true} when credentials are valid.
     */
    protected abstract boolean isBasicAuthValid(String username, String pw);

    /**
     *
     * @param remoteAddr
     *            Remote IP address.
     * @return {@code true} when remote IP address is allowed.
     */
    protected abstract boolean isRemoteAddrAllowed(String remoteAddr);

    /**
     * Checks Remote Client access and Basic Authentication credentials.
     *
     * @param request
     *            The HTTP request.
     * @return {@code true} when credentials are valid.
     */
    protected final boolean
            checkBasicAuthAccess(final HttpServletRequest request) {

        final String clientIP = WebAppHelper.getClientIP(request);

        if (!isRemoteAddrAllowed(clientIP)) {
            LOGGER.warn("{}: {} denied.", this.getClass().getSimpleName(),
                    clientIP);
            return false;
        }

        if (LOGGER.isDebugEnabled()) {

            final Principal principal = request.getUserPrincipal();
            if (principal == null) {
                return false;
            }
            LOGGER.debug("User [{}]", principal.getName());
        }

        final String authorization = request.getHeader("Authorization");

        if (authorization != null
                && request.getAuthType().equalsIgnoreCase("Basic")) {

            /*
             * Authorization: Basic base64credentials
             */
            final String base64Credentials =
                    authorization.substring("Basic".length()).trim();
            final String credentials =
                    new String(Base64.getDecoder().decode(base64Credentials),
                            Charset.forName("UTF-8"));

            /*
             * credentials = username:password
             */
            final String[] values = credentials.split(":", 2);

            if (values.length != 2) {
                return false;
            }

            final String uid = values[0];
            final String pw = values[1];

            boolean isValid = isBasicAuthValid(uid, pw);

            if (!isValid) {
                LOGGER.warn("{}: {} denied (userid/password).",
                        this.getClass().getSimpleName(), clientIP);
            }
            return isValid;
        }
        return false;
    }

}
