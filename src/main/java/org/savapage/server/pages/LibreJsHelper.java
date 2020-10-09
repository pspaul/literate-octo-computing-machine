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
package org.savapage.server.pages;

/**
 * Helper for GNU LibreJS.
 *
 * @author Rijk Ravestein
 *
 */
public final class LibreJsHelper {

    /**
     * Initial {@code <script>} snippet to place in {@code <header>}.
     */
    private static final String INITIAL_HEADER_SCRIPT = "\n" //
            + "<script>\n" //
            + "/*<![CDATA[*/\n" //
            + "  /*\n" //
            + "   * @licstart  The following is the entire license notice for the \n"
            + "   * JavaScript code in this page.\n" //
            + "   * \n"
            + "   * This file is part of the SavaPage project <https://savapage.org>\n"
            + "   * Copyright (c) 2020 Datraverse B.V. | Author: Rijk Ravestein\n"
            + "   * \n" //
            + "   * The JavaScript code in this page is free software: you can redistribute\n"
            + "   * it and/or modify it under the terms of the GNU Affero General Public License\n"
            + "   * as published by the Free Software Foundation, either version 3 of the\n"
            + "   * License, or (at your option) any later version.\n" //
            + "   * \n" //
            + "   * @licend  The above is the entire license notice\n" //
            + "   * for the JavaScript code in this page\n" //
            + "   */\n" //
            + "/*]]>*/\n" //
            + "</script>";

    /** */
    private static final String LICENCE_START_PREFIX = "// @license";

    /** */
    private static final String LICENCE_END = "\n// @license-end\n";

    /**
     * Utility class.
     */
    private LibreJsHelper() {
    }

    /**
     * @param lic
     *            License.
     * @return License start tag.
     */
    public static String getJsLicenseStartTag(final LibreJsLicenseEnum lic) {
        return String.format("%s %s %s\n\n", LICENCE_START_PREFIX, lic.getUrl(),
                lic.getId());
    }

    /**
     * @return License end tag.
     */
    public static String getJsLicenseEndTag() {
        return LICENCE_END;
    }

    /**
     * @return Initial {@code <script>} snippet to place in {@code <header>}.
     */
    public static String getInitialHeaderScript() {
        return INITIAL_HEADER_SCRIPT;
    }

}
