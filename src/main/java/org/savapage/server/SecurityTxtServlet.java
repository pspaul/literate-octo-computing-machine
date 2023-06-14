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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;

/**
 * See <a href="https://securitytxt.org/">securitytxt.org</a> and
 * <a href="https://tools.ietf.org/html/rfc8615">RFC8615</a>.
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "SecurityTxtServlet",
        urlPatterns = { "/.well-known/security.txt" })
public final class SecurityTxtServlet extends HttpServlet {

    /** */
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, IOException {

        /*
         * Before streaming content, set content characteristics.
         */
        resp.setHeader(HttpHeader.CACHE_CONTROL.toString(),
                "no-cache, no-store, must-revalidate"); // HTTP 1.1
        resp.setHeader(HttpHeader.PRAGMA.toString(), "no-cache"); // HTTP 1.0
        resp.setDateHeader(HttpHeader.EXPIRES.toString(), 0);

        final ConfigManager cm = ConfigManager.instance();

        final String strContactMailTo =
                cm.getConfigValue(Key.SECURITYTXT_CONTACT_MAILTO);
        final String strContactTel =
                cm.getConfigValue(Key.SECURITYTXT_CONTACT_TEL);
        final String strContactUrl =
                cm.getConfigValue(Key.SECURITYTXT_CONTACT_URL);

        final boolean isEnabled = cm.isConfigValue(Key.SECURITYTXT_ENABLE)
                && (StringUtils.isNotBlank(strContactMailTo)
                        || StringUtils.isNotBlank(strContactTel)
                        || StringUtils.isNotBlank(strContactUrl));

        if (!isEnabled) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        final StringBuilder txt = new StringBuilder();
        String strWlk;

        /*
         * Required at least 1 contact.
         */
        strWlk = strContactMailTo;
        if (StringUtils.isNotBlank(strWlk)) {
            txt.append(String.format("Contact: mailto:%s\n", strWlk));
        }
        strWlk = strContactTel;
        if (StringUtils.isNotBlank(strWlk)) {
            txt.append(String.format("Contact: tel:%s\n", strWlk));
        }
        strWlk = strContactUrl;
        if (StringUtils.isNotBlank(strWlk)) {
            txt.append(String.format("Contact: %s\n", strWlk));
        }

        // Expires 1 month from now.
        txt.append(String.format("Expires: %s\n",
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX")
                        .format(DateUtils.addMonths(new Date(), 1))));
        /*
         * Optional.
         */
        strWlk = cm.getConfigValue(Key.SECURITYTXT_ENCRYPTION_URI);
        if (StringUtils.isNotBlank(strWlk)) {
            txt.append(String.format("Encryption: %s\n", strWlk));
        }

        strWlk = cm.getConfigValue(Key.SECURITYTXT_ACKNOWLEDGMENTS_URL);
        if (StringUtils.isNotBlank(strWlk)) {
            txt.append(String.format("Acknowledgments: %s\n", strWlk));
        }
        strWlk = cm.getConfigValue(Key.SECURITYTXT_PREFERRED_LANGUAGES);
        if (StringUtils.isNotBlank(strWlk)) {
            txt.append(String.format("Preferred-Languages: %s\n", strWlk));
        }
        strWlk = cm.getConfigValue(Key.SECURITYTXT_POLICY_URL);
        if (StringUtils.isNotBlank(strWlk)) {
            txt.append(String.format("Policy: %s\n", strWlk));
        }
        strWlk = cm.getConfigValue(Key.SECURITYTXT_HIRING_URL);
        if (StringUtils.isNotBlank(strWlk)) {
            txt.append(String.format("Hiring: %s\n", strWlk));
        }

        //
        resp.getOutputStream().write(txt.toString().getBytes());
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
