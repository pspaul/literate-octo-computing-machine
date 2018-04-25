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
package org.savapage.server.feed;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.services.AtomFeedService;
import org.savapage.core.services.ServiceContext;
import org.savapage.lib.feed.AtomFeedWriter;
import org.savapage.lib.feed.FeedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Atom Feed.
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "AtomFeedServlet",
        urlPatterns = { AtomFeedServlet.SERVLET_URL_PATTERN })
@ServletSecurity(@HttpConstraint(
        transportGuarantee = TransportGuarantee.CONFIDENTIAL,
        rolesAllowed = { AtomFeedServlet.ROLE_ALLOWED }))
public final class AtomFeedServlet extends HttpServlet {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AtomFeedServlet.class);

    /**
     * Role for Basic Authentication.
     */
    public static final String ROLE_ALLOWED = "FeedReader";

    /**
     * Base path of custom web files (without leading or trailing '/').
     */
    public static final String PATH_BASE = "feed";

    /**
     * Path info of admin feed (with leading '/').
     */
    public static final String PATH_INFO_ADMIN = "/admin";

    /** */
    public static final String SERVLET_URL_PATTERN = "/" + PATH_BASE + "/*";

    /**
     * The full file path of the content.
     */
    public static final String CONTENT_HOME = ConfigManager.getServerHome();

    @Override
    public void doGet(final HttpServletRequest request,
            final HttpServletResponse response)
            throws ServletException, IOException {

        if (!ConfigManager.instance()
                .isConfigValue(Key.FEED_ATOM_ADMIN_ENABLE)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!StringUtils.defaultString(request.getPathInfo())
                .equals(PATH_INFO_ADMIN)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!checkCredentials(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            response.setContentType("application/atom+xml");

            final AtomFeedService svc =
                    ServiceContext.getServiceFactory().getAtomFeedService();

            // svc.refreshAdminFeed(); // TEST

            final AtomFeedWriter writer = svc.getAdminFeedWriter(
                    new URI(request.getRequestURL().toString()),
                    response.getOutputStream());

            writer.process();

        } catch (FeedException | URISyntaxException e) {
            throw new ServletException(e.getMessage());
        }
    }

    /**
     * Checks credentials acquired from Basic Authentication.
     *
     * @param request
     *            The HTTP request.
     * @return {@code true} when credentials are valid.
     */
    public boolean checkCredentials(final HttpServletRequest request) {

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

            final ConfigManager cm = ConfigManager.instance();

            final String uid = values[0];
            final String pw = values[1];

            return StringUtils.isNotBlank(uid) && StringUtils.isNotBlank(pw)
                    && cm.getConfigValue(Key.FEED_ATOM_ADMIN_USERNAME)
                            .equals(uid)
                    && cm.getConfigValue(Key.FEED_ATOM_ADMIN_PASSWORD)
                            .equals(pw);
        }
        return false;
    }

}
