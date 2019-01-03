/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.server.helpers;

import org.savapage.core.jpa.Printer;
import org.savapage.server.WebApp;

/**
 * Images for {@link Printer}.
 *
 * @author Rijk Ravestein
 *
 */
public enum HtmlPrinterImgEnum {

    /**  */
    JOBTICKET("printer-jobticket-32x32.png"),

    /**  */
    TERMINAL_AND_READER("printer-terminal-custom-or-auth-16x16.png"),

    /**  */
    TERMINAL("printer-terminal-custom-16x16.png"),

    /**  */
    READER("printer-terminal-auth-16x16.png"),

    /**  */
    SECURE("printer-terminal-none-16x16.png"),

    /** */
    NON_SECURE("printer-terminal-any-16x16.png");

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
    HtmlPrinterImgEnum(final String value) {
        this.img = value;
    }

    /**
     *
     * @return URL path of printer image.
     */
    public String urlPath() {
        return String.format("%s/%s", WebApp.PATH_IMAGES, this.img);
    }

}
