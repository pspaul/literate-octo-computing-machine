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
 * GNU LibreJS licenses.
 *
 * @author Rijk Ravestein
 *
 */
public enum LibreJsLicenseEnum {

    /** */
    AGPL_3_0("http://www.gnu.org/licenses/agpl-3.0.html", "AGPL-3.0"),

    /** */
    APACHE_2_0("http://www.apache.org/licenses/LICENSE-2.0", "Apache-2.0"),

    /** */
    BSD_3_CLAUSE("http://opensource.org/licenses/BSD-3-Clause", "BSD-3-Clause"),

    /** */
    CC0_1_0("http://creativecommons.org/publicdomain/zero/1.0/legalcode",
            "CC0-1.0"),
    /** */
    GPL_2_0("http://www.gnu.org/licenses/gpl-2.0.html", "GPL-2.0"),

    /** */
    MIT("http://www.jclark.com/xml/copying.txt", "Expat"),

    /** */
    UNLICENSE("http://unlicense.org/UNLICENSE", "Unlicense");

    /** */
    private final String url;
    /** */
    private final String id;

    /**
     * Constructor.
     *
     * @param u
     *            URL.
     * @param i
     *            Identifier.
     */
    LibreJsLicenseEnum(final String u, final String i) {
        this.url = u;
        this.id = i;
    }

    /**
     * @return License URL.
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return License identifier.
     */
    public String getId() {
        return id;
    }

}
