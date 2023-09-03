/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2023 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2023 Datraverse B.V. <info@datraverse.com>
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

import org.apache.wicket.markup.html.panel.Panel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class PageItemIconPanel extends Panel {

    /** */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public PageItemIconPanel(final String id) {
        super(id);
    }

    /**
     *
     * @param helper
     * @param nCount
     * @param wicketIdTitle
     * @param title
     * @param wicketIdImg
     * @param imgSrc
     * @param wicketIdCount
     */
    protected void populate(final MarkupHelper helper,
            final String wicketIdCount, final int nCount,
            final String wicketIdTitle, final String title,
            final String wicketIdImg, final String imgSrc) {

        if (nCount == 0) {
            helper.discloseLabel(wicketIdTitle);
        } else {
            MarkupHelper.modifyComponentAttr(
                    helper.addTransparant(wicketIdTitle),
                    MarkupHelper.ATTR_TITLE, title);

            MarkupHelper.modifyComponentAttr(helper.addTransparant(wicketIdImg),
                    MarkupHelper.ATTR_SRC, imgSrc);

            helper.addLabel(wicketIdCount, String.valueOf(nCount));
        }
    }

}
