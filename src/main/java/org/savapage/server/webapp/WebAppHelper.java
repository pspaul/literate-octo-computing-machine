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
package org.savapage.server.webapp;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.cometd.bayeux.server.ServerMessage;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.util.InetUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppHelper {

    /**
     * HTTP XFF header.
     */
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * Static methods only.
     */
    private WebAppHelper() {
    }

    /**
     * @return {@code true} if XFF is trusted.
     */
    private static boolean useXFF() {
        return ConfigManager.instance()
                .isConfigValue(Key.WEBSERVER_HTTP_HEADER_XFF_ENABLE);
    }

    /**
     * Gets first IP address from {@code X-Forwarded-For} header. See
     * documentation at <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For">Mozilla</a>
     * or <a href="https://support.f5.com/csp/article/K12264">AskF5</a>.
     *
     * @param xffHeader
     *            The {@link #HEADER_X_FORWARDED_FOR} value.
     * @return {@code null} when not found or proxy IP address not allowed.
     */
    private static String getClientIPFromXFF(final String xffHeader) {

        final boolean isXFFDebug = ConfigManager.instance()
                .isConfigValue(Key.WEBSERVER_HTTP_HEADER_XFF_DEBUG);

        if (StringUtils.isBlank(xffHeader)) {
            if (isXFFDebug) {
                if (xffHeader == null) {
                    debugProxyXFF(xffHeader, "header not found.");
                } else {
                    debugProxyXFF(xffHeader, "header is empty.");
                }
            }
            return null;
        }

        // X-Forwarded-For: <client>, <proxy1>, <proxy2>
        final String[] xffArray = xffHeader.split(",");

        final String cidrRangesProxy = ConfigManager.instance()
                .getConfigValue(Key.WEBSERVER_HTTP_HEADER_XFF_PROXIES_ALLOWED);

        if (StringUtils.isNotBlank(cidrRangesProxy)) {

            if (xffArray.length < 2) {
                warnProxyXFF(xffHeader, cidrRangesProxy);
                return null;
            }

            for (int i = 1; i < xffArray.length; i++) {
                if (!InetUtils.isIpAddrInCidrRanges(cidrRangesProxy,
                        xffArray[i].trim())) {
                    warnProxyXFF(xffHeader, cidrRangesProxy);
                    return null;
                }
            }
        }

        final String clientIP = xffArray[0].trim();

        if (isXFFDebug) {
            debugProxyXFF(xffHeader, clientIP.concat(" (Client IP)."));
        }

        return clientIP;
    }

    /**
     * Publish warning that XFF proxy is not allowed.
     *
     * @param xffHeader
     *            XFF Header.
     * @param msg
     *            Message.
     */
    private static void debugProxyXFF(final String xffHeader,
            final String msg) {

        AdminPublisher.instance().publish(PubTopicEnum.USER, PubLevelEnum.INFO,
                String.format("[DEBUG] HTTP %s [%s]: %s",
                        HEADER_X_FORWARDED_FOR,
                        StringUtils.defaultString(xffHeader, " "), msg));
    }

    /**
     * Publish warning that XFF proxy is not allowed.
     *
     * @param xffHeader
     *            XFF Header.
     * @param cidrRangesProxy
     *            CIDR ranges for allowed proxy IP addresses.
     */
    private static void warnProxyXFF(final String xffHeader,
            final String cidrRangesProxy) {

        AdminPublisher.instance().publish(PubTopicEnum.USER, PubLevelEnum.WARN,
                String.format(
                        "HTTP %s [%s] does not match allowed proxies [%s]: "
                                + "using remote IP address.",
                        HEADER_X_FORWARDED_FOR, xffHeader, cidrRangesProxy));
    }

    /**
     * Gets the client address taking "X-Forwarded-For" HTTP header into
     * account.
     *
     * @param request
     *            {@link org.apache.wicket.request.Request}.
     * @return Client IP address.
     */
    public static String
            getClientIP(final org.apache.wicket.request.Request request) {
        return getClientIP(((ServletWebRequest) request).getContainerRequest());
    }

    /**
     * Gets the client IP address taking "X-Forwarded-For" HTTP header into
     * account.
     *
     * @param message
     *            {@link ServerMessage}.
     * @return Client IP address.
     */
    public static String getClientIP(final ServerMessage message) {

        String clientIP = null;

        if (useXFF()) {
            clientIP = getClientIPFromXFF(message.getBayeuxContext()
                    .getHeader(HEADER_X_FORWARDED_FOR));
        }

        if (StringUtils.isBlank(clientIP)) {
            clientIP = message.getBayeuxContext().getRemoteAddress()
                    .getHostString();
        }
        return clientIP;
    }

    /**
     * Gets the client IP address taking "X-Forwarded-For" HTTP header into
     * account.
     *
     * @param request
     *            {@link HttpServletRequest}.
     * @return Client IP address.
     */
    public static String getClientIP(final HttpServletRequest request) {

        String clientIP = null;

        if (useXFF()) {
            clientIP = getClientIPFromXFF(
                    request.getHeader(HEADER_X_FORWARDED_FOR));
        }

        if (StringUtils.isBlank(clientIP)) {
            clientIP = sanitizeRemoteAddr(request.getRemoteAddr());
        }
        return clientIP;
    }

    /**
     * @param remoteAddrRaw
     *            The raw remote address. Note: IPv6 address might be enclosed
     *            by brackets.
     *
     * @return Plain IP address, without formatting.
     */
    private static String sanitizeRemoteAddr(final String remoteAddrRaw) {
        return StringUtils
                .removeEnd(StringUtils.removeStart(remoteAddrRaw, "["), "]");
    }

}
