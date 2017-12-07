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
    /**  */
    ABOUT,
    /**  */
    APPLY,
    /**  */
    BACK,
    /**  */
    BROWSE,
    /**  */
    CANCEL,
    /**  */
    CANCEL_ALL,
    /**  */
    CHECK,
    /**  */
    CLOSE,
    /**  */
    DEFAULT,
    /**  */
    DELETE,
    /**  */
    DOWNLOAD,
    /**  */
    EDIT,
    /**  */
    ERASE,
    /**  */
    EXTEND,
    /**  */
    HELP,
    /**  */
    INBOX,
    /**  */
    LOGIN,
    /**  */
    LOGOUT,
    /**  */
    NEXT,
    /** As opposite of {@link #YES}. */
    NO,
    /**  */
    OK,
    /**  */
    PREVIEW,
    /**  */
    PREVIOUS,
    /**  */
    PRINT,
    /**  */
    REFRESH,
    /**  */
    REFUND,
    /**  */
    RESET,
    /**  */
    RETRY,
    /**  */
    SAVE,
    /**  */
    SEARCH,
    /**  */
    SELECT,
    /**  */
    SEND,
    /**  */
    SETTINGS,
    /**  */
    SETTLE,
    /**  */
    SORT,
    /**  */
    START,
    /**  */
    UPLOAD,
    /** As opposite of {@link #NO}. */
    YES;

    /**
     * Dotted suffix for localized text.
     */
    public static final String DOTTED_SUFFIX = " . . .";

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

    /**
     *
     * @param locale
     *            The {@link Locale}.
     * @param dottedSuffix
     *            {@code true} when localized text has {@link #DOTTED_SUFFIX}.
     * @return The localized text.
     */
    public String uiText(final Locale locale, final boolean dottedSuffix) {
        if (dottedSuffix) {
            return String.format("%s%s", this.uiText(locale), DOTTED_SUFFIX);
        }
        return uiText(locale);
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text with {@link #DOTTED_SUFFIX}.
     */
    public String uiTextDottedSfx(final Locale locale) {
        return uiText(locale, true);
    }
}
