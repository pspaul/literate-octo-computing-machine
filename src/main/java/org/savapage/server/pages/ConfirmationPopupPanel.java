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

import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.server.helpers.HtmlButtonEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ConfirmationPopupPanel extends Panel {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param id
     *            The non-null id of this component.
     */
    public ConfirmationPopupPanel(final String id) {
        super(id);
    }

    /**
     * 
     * @param htmlID
     *            HTML id of popup.
     * @param title
     *            Header title.
     * @param msg
     *            Message.
     */
    public void populate(final String htmlID, final String title,
            final String msg) {

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addTransparentModifyAttr("popup", MarkupHelper.ATTR_ID, htmlID);

        helper.addLabel("title", title);
        helper.addLabel("msg", msg);

        helper.addButton("btn-ok", HtmlButtonEnum.OK);
        helper.addButton("btn-cancel", HtmlButtonEnum.CANCEL);
    }

}
