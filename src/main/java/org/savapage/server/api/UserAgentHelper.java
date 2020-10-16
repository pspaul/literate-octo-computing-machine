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
package org.savapage.server.api;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserAgentHelper {

    /**
     * Blank when no user agent found.
     */
    private final String userAgentHeader;

    /**
     *
     * @param request
     *            The {@link HttpServletRequest}.
     */
    public UserAgentHelper(final HttpServletRequest request) {
        this.userAgentHeader =
                StringUtils.defaultString(request.getHeader("User-Agent"));
    }

    /**
     *
     * @return
     */
    public String getUserAgentHeader() {
        return userAgentHeader;
    }

    /**
     * @return {@code true} is a mobile Safari browser.
     */
    public boolean isSafariBrowserMobile() {

        final String userAgent = this.userAgentHeader.toLowerCase();

        if (StringUtils.isBlank(userAgent)) {
            return false;
        }

        return !userAgent.contains("crios") && !userAgent.contains("android")
                && !userAgent.contains("linux")
                && (userAgent.contains("iphone") || userAgent.contains("ipod")
                        || (userAgent.contains("ipad"))
                        || (userAgent.contains("mobile")
                                && userAgent.contains("safari")));
    }

    /**
     * @return {@code true} if Mac OS X Safari browser.
     */
    public boolean isSafariBrowserMacOsX() {

        final String userAgent = this.userAgentHeader.toLowerCase();

        if (StringUtils.isBlank(userAgent)) {
            return false;
        }
        return userAgent.contains("safari") && userAgent.contains("mac os");
    }

    /**
     * @return {@code true} if Firefox browser.
     */
    public boolean isFirefoxBrowser() {

        final String userAgent = this.userAgentHeader.toLowerCase();

        if (StringUtils.isBlank(userAgent)) {
            return false;
        }
        return userAgent.contains("firefox");
    }

    /**
     * Checks if the WebApp is run from a mobile device.
     *
     * @return {@code true} when User Agent shows that client is a mobile
     *         browser.
     */
    public boolean isMobileBrowser() {

        final String userAgent = this.userAgentHeader.toLowerCase();

        if (StringUtils.isBlank(userAgent)) {
            return false;
        }

        return userAgent.contains("android") || userAgent.contains("mobile")
                || userAgent.contains("iphone") || userAgent.contains("ipod")
                || userAgent.contains("ipad");
    }

}
