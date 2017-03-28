/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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

import java.util.Locale;

import org.savapage.core.util.LocaleHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum HtmlButtonEnum {

    /**
     *
     */
    APPLY,

    /**
    *
    */
    BACK,

    /**
     *
     */
    CANCEL,

    /**
    *
    */
    CANCEL_ALL,

    /**
    *
    */
    CLOSE,

    /**
     *
     */
    DEFAULT,

    /**
    *
    */
    DELETE,

    /**
    *
    */
    DOWNLOAD,

    /**
     *
     */
    EDIT,

    /**
    *
    */
    ERASE,

    /**
    *
    */
    EXTEND,

    /**
    *
    */
    LOGIN,

    /**
    *
    */
    LOGOUT,

    /**
     *
     */
    NEXT,

    /**
     *
     */
    OK,

    /**
     *
     */
    PREVIEW,

    /**
    *
    */
    PREVIOUS,

    /**
    *
    */
    PRINT,

    /**
    *
    */
    RESET,

    /**
    *
    */
    RETRY,

    /**
     *
     */
    SAVE,

    /**
    *
    */
    SEND,

    /**
    *
    */
    SETTLE,

    /**
    *
    */
    UPLOAD;

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text with " . . ." suffix.
     */
    public String uiTextDottedSfx(final Locale locale) {
        return String.format("%s . . .", this.uiText(locale));
    }
}
