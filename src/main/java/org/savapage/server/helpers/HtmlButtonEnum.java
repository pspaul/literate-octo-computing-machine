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

import org.savapage.core.util.LocaleHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum HtmlButtonEnum {
    /** */
    ABOUT,
    /** */
    ADD,
    /** */
    APPLY,
    /** */
    ARCHIVE,
    /** */
    BACK,
    /** */
    BROWSE,
    /** */
    CANCEL,
    /** */
    CANCEL_ALL,
    /** */
    CHANGE,
    /** */
    CHECK,
    /** */
    CLEAR,
    /** */
    CLEAR_ALL,
    /** */
    CLEAR_SELECTION,
    /** */
    CLOSE,
    /** */
    CONTINUE,
    /** copy, paste, cut. */
    COPY,
    /** copy, paste, cut. */
    CUT,
    /** */
    DEFAULT,
    /** */
    DELETE,
    /** */
    DOWNLOAD,
    /** */
    EDIT,
    /** */
    ERASE,
    /** */
    EXTEND,
    /** */
    GENERATE,
    /** */
    HELP,
    /** */
    HIDE,
    /** */
    INBOX,
    /** */
    LOGIN,
    /** */
    LOGOUT,
    /** */
    MORE,
    /** */
    NEW,
    /** */
    NEXT,
    /** As opposite of {@link #YES}. */
    NO,
    /** */
    OK,
    /** copy, paste, cut. */
    PASTE,
    /** */
    PREVIEW,
    /** */
    PREVIOUS,
    /** */
    PRINT,
    /** */
    RASTERIZE,
    /** */
    REFRESH,
    /** */
    REFUND,
    /** */
    REGISTER,
    /** */
    RENAME,
    /** */
    REPLACE,
    /** */
    RESET,
    /** */
    RESTORE,
    /** */
    RETRY,
    /** */
    REVERSE,
    /** */
    ROLLBACK,
    /** */
    ROTATE,
    /** */
    SAVE,
    /** */
    SEARCH,
    /** */
    SELECT,
    /** */
    SELECT_ALL,
    /** */
    SEND,
    /** */
    SETTINGS,
    /** */
    SETTLE,
    /** */
    SORT,
    /** As opposite of {@link #STOP}. */
    START,
    /** As opposite of {@link #START}. */
    STOP,
    /** */
    SYNCHRONIZE,
    /** */
    TEST,
    /** */
    UNDO,
    /** */
    UNSELECT_ALL,
    /** */
    UPLOAD,
    /** */
    VERIFY,
    /** As opposite of {@link #NO}. */
    YES,
    /** */
    ZOOM_IN,
    /** */
    ZOOM_OUT;

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
