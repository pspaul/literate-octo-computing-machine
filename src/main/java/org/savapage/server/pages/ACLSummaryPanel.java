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

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.wicket.markup.html.panel.Panel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ACLSummaryPanel extends Panel {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private final String imgSrcEnabled;
    /** */
    private final String imgSrcDisabled;

    /**
     *
     * @param id
     *            The non-null id of this component.
     * @param imgSrcEnabled
     *            Enabled IMG src.
     * @param imgSrcDisabled
     *            Disabled IMG src.
     */
    public ACLSummaryPanel(final String id, final String imgSrcEnabled,
            final String imgSrcDisabled) {
        super(id);
        this.imgSrcEnabled = imgSrcEnabled;
        this.imgSrcDisabled = imgSrcDisabled;
    }

    /**
     * @param nRolesEnabled
     * @param nRolesDisabled
     * @param txtRolesEnabled
     * @param txtRolesDisabled
     */
    protected final void populate(final MutableInt nRolesEnabled,
            final MutableInt nRolesDisabled,
            final StringBuilder txtRolesEnabled,
            final StringBuilder txtRolesDisabled) {

        final MarkupHelper helper = new MarkupHelper(this);

        if (nRolesEnabled.intValue() == 0) {
            helper.discloseLabel("enabled");
        } else {
            MarkupHelper.modifyComponentAttr(helper.addTransparant("enabled"),
                    MarkupHelper.ATTR_TITLE, txtRolesEnabled.toString());

            MarkupHelper.modifyComponentAttr(
                    helper.addTransparant("enabled-img"), MarkupHelper.ATTR_SRC,
                    this.imgSrcEnabled);

            helper.addLabel("enabled-count",
                    nRolesEnabled.toInteger().toString());
        }

        if (nRolesDisabled.intValue() == 0) {
            helper.discloseLabel("disabled");
        } else {
            MarkupHelper.modifyComponentAttr(helper.addTransparant("disabled"),
                    MarkupHelper.ATTR_TITLE, txtRolesDisabled.toString());

            MarkupHelper.modifyComponentAttr(
                    helper.addTransparant("disabled-img"),
                    MarkupHelper.ATTR_SRC, this.imgSrcDisabled);

            helper.addLabel("disabled-count",
                    nRolesDisabled.toInteger().toString());
        }

    }

}
