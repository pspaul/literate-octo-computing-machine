/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.server.restful;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.util.InetUtils;
import org.savapage.server.webapp.WebAppHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class RestAuthFilter
        implements javax.ws.rs.container.ContainerRequestFilter {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RestAuthFilter.class);

    /**
     * Admin role for Basic Authentication.
     */
    public static final String ROLE_ADMIN = "ADMIN";

    /** */
    public static final String HEADER_AUTHORIZATION_PROPERTY = "Authorization";
    /** */
    public static final String HEADER_AUTHENTICATION_SCHEME = "Basic";

    /** */
    @Context
    private ResourceInfo resourceInfo;

    /** */
    @Context
    private HttpServletRequest servletRequest;

    /**
     *
     * @param requestContext
     *            context.
     * @param status
     *            Response status.
     */
    private static void abortWith(final ContainerRequestContext requestContext,
            final Response.Status status) {
        requestContext.abortWith(
                Response.status(status).entity("Access denied.").build());
    }

    @Override
    public void filter(final ContainerRequestContext context) {

        if (!isRemoteAddressAllowed()) {
            abortWith(context, Response.Status.FORBIDDEN);
            return;
        }

        final Method method = resourceInfo.getResourceMethod();

        if (method.isAnnotationPresent(PermitAll.class)) {
            return;
        }

        if (method.isAnnotationPresent(DenyAll.class)) {
            abortWith(context, Response.Status.FORBIDDEN);
            return;
        }

        final boolean rolesAllowedPresent =
                method.isAnnotationPresent(RolesAllowed.class);

        if (!rolesAllowedPresent) {
            return;
        }

        // Fetch authorization header
        final MultivaluedMap<String, String> headers = context.getHeaders();

        final List<String> authorization =
                headers.get(HEADER_AUTHORIZATION_PROPERTY);

        if (authorization == null || authorization.isEmpty()) {
            abortWith(context, Response.Status.UNAUTHORIZED);
            return;
        }

        // Get encoded username and password
        final String encodedUserPassword = authorization.get(0)
                .replaceFirst(HEADER_AUTHENTICATION_SCHEME + " ", "");

        // Decode username and password
        final String usernameAndPassword = new String(
                Base64.getDecoder().decode(encodedUserPassword.getBytes()));

        // Split username and password tokens
        final StringTokenizer tokenizer =
                new StringTokenizer(usernameAndPassword, ":");
        final String username = tokenizer.nextToken();
        final String password = tokenizer.nextToken();

        // Verify user access
        if (rolesAllowedPresent) {

            final RolesAllowed rolesAnnotation =
                    method.getAnnotation(RolesAllowed.class);

            final Set<String> rolesSet =
                    new HashSet<String>(Arrays.asList(rolesAnnotation.value()));

            if (!isBasicAuthValid(username, password, rolesSet)) {
                abortWith(context, Response.Status.UNAUTHORIZED);
                return;
            }
        }
    }

    /**
     * @return {@code true} if remote address is allowed.
     */
    private boolean isRemoteAddressAllowed() {

        final String clientAddress = WebAppHelper.getClientIP(servletRequest);

        final String cidrRanges = ConfigManager.instance()
                .getConfigValue(Key.API_RESTFUL_IP_ADDRESSES_ALLOWED);

        final boolean allowed = StringUtils.isBlank(cidrRanges)
                || InetUtils.isIpAddrInCidrRanges(cidrRanges, clientAddress);

        if (!allowed) {
            LOGGER.warn("Access denied for {}. Allowed CIDR ranges: {}",
                    clientAddress, cidrRanges);

            AdminPublisher.instance().publish(PubTopicEnum.WEB_SERVICE,
                    PubLevelEnum.WARN,
                    String.format(
                            "RESTful service denied for remote address %s.",
                            clientAddress));
        }
        return allowed;
    }

    /**
     * @param username
     *            User name.
     * @param pw
     *            User password.
     * @param rolesSet
     *            Roles.
     * @return {@code true} when allowed.
     */
    private boolean isBasicAuthValid(final String username, final String pw,
            final Set<String> rolesSet) {

        final ConfigManager cm = ConfigManager.instance();

        return rolesSet.contains(ROLE_ADMIN) && StringUtils.isNotBlank(username)
                && StringUtils.isNotBlank(pw)
                && cm.getConfigValue(Key.API_RESTFUL_AUTH_USERNAME)
                        .equals(username)
                && cm.getConfigValue(Key.API_RESTFUL_AUTH_PASSWORD).equals(pw);

    }

}