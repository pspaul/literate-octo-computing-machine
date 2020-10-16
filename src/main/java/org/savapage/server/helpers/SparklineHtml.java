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

/**
 * Constants and helper methods for
 * <a href="https://omnipotent.net/jquery.sparkline">JQuery Sparklines</a>.
 *
 * @author Rijk Ravestein
 *
 */
public final class SparklineHtml {

    /** */
    private static final String CSS_CLASS = "sp-sparkline-";

    /** */
    public static final String CSS_CLASS_PRINTOUT = CSS_CLASS + "printout";

    /** */
    public static final String CSS_CLASS_DOCLOG = CSS_CLASS + "doclog";

    /** */
    public static final String CSS_CLASS_PRINTER = CSS_CLASS + "printer";

    /** */
    public static final String CSS_CLASS_QUEUE = CSS_CLASS + "queue";

    /** */
    public static final String CSS_CLASS_USER = CSS_CLASS + "user";

    /** */
    public static final String TYPE_LINE = "line";

    /** */
    public static final String TYPE_PIE = "pie";

    /** Color: orange. */
    public static final String COLOR_SHEET = "#FF9900";

    /** Color: reddish. */
    public static final String COLOR_PRINTER = "#D14719";

    /** Color: blue-ish. */
    public static final String COLOR_QUEUE = "#597BDE";

    /** Color: greenish. */
    public static final String COLOR_PDF = "#33AD33";

    /** */
    private static final String ATTR_PFX = "spark";

    /** */
    public static final String ATTR_TYPE = ATTR_PFX + "Type";

    /** */
    public static final String ATTR_SLICE_COLORS = ATTR_PFX + "SliceColors";

    /** */
    public static final String ATTR_COLOR_MAP = ATTR_PFX + "ColorMap";

    /** */
    public static final String ATTR_LINE_COLOR = ATTR_PFX + "LineColor";

    /** */
    public static final String ATTR_FILL_COLOR = ATTR_PFX + "FillColor";

    /** */
    public static final String ATTR_BAR_WIDTH = ATTR_PFX + "BarWidth";

    /**
     * Utility class.
     */
    private SparklineHtml() {
    }

    /**
     * Creates a string representation of an array, to be used as spark
     * attribute value.
     *
     * @param elements
     *            Array elements.
     * @return String representation of an array.
     */
    public static String arrayAttr(final String... elements) {
        final StringBuilder arr = new StringBuilder();
        arr.append("[").append(valueString(elements)).append("]");
        return arr.toString();
    }

    /**
     * Creates a comma separated value string, to be used as spark attribute or
     * element value.
     *
     * @param values
     *            Value array.
     * @return Comma separated value string.
     */
    public static String valueString(final String... values) {
        final StringBuilder arr = new StringBuilder();
        int i = 0;
        for (final String value : values) {
            if (i > 0) {
                arr.append(",");
            }
            arr.append(value);
            i++;
        }
        return arr.toString();
    }

}
