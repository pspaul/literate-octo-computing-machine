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
package org.savapage.server.helpers;

import java.util.Locale;

import org.savapage.core.jpa.Device;
import org.savapage.core.util.LocaleHelper;
import org.savapage.server.WebApp;

/**
 * Images for {@link Device}.
 *
 * @author Rijk Ravestein
 *
 */
public enum HtmlDeviceImgEnum {

    /**
     * A Network Card Reader used for NFC Card Login at a Terminal.
     */
    READER_LOGON("device-card-reader-terminal-16x16.png"),

    /**
     * A Network Card Reader used for NFC Card Proxy Print Authentication.
     */
    READER_PRINT_AUTH("device-card-reader-16x16.png"),

    /**
     * A Terminal with custom settings.
     */
    TERMINAL("device-terminal-16x16.png"),

    /**
     * A Terminal with custom settings and a Network Card Reader used for NFC
     * Card Login.
     */
    TERMINAL_READER_LOGON("device-terminal-card-reader-16x16.png");

    /**
     * .
     */
    private final String img;

    /**
     * Constructor.
     *
     * @param value
     *            The CSS class.
     */
    HtmlDeviceImgEnum(final String value) {
        this.img = value;
    }

    /**
     *
     * @return URL path of printer image.
     */
    public String urlPath() {
        return String.format("%s/%s", WebApp.PATH_IMAGES, this.img);
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized tooltip text.
     */
    public String uiToolTip(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

}
