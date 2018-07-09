/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.server.pages;

import org.savapage.core.dao.enums.AppLogLevelEnum;

/**
 * Page with illegal state message .
 *
 * @author Rijk Ravestein
 *
 */
public class PageIllegalState extends AbstractPage {

    private static final long serialVersionUID = 1L;

    /**
     *
     * @param level
     *            The severity level of the message.
     * @param msg
     *            The message to display.
     */
    public PageIllegalState(final AppLogLevelEnum level, final String msg) {

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addModifyLabelAttr("msg", msg, MarkupHelper.ATTR_CLASS,
                new StringBuilder().append(MarkupHelper.CSS_TXT_WRAP)
                        .append(" ").append(MarkupHelper.getCssTxtClass(level))
                        .toString());
    }

}
