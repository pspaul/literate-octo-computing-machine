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
package org.savapage.server.pages.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.IppRoutingEnum;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PageQueue extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param parameters
     *            The page parameters.
     */
    public PageQueue(final PageParameters parameters) {
        super(parameters, ACLOidEnum.A_QUEUES, RequiredPermission.EDIT);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("queue-journal", NounEnum.JOURNAL);
        helper.addLabel("journal-disabled", AdverbEnum.DISABLED);

        helper.addLabel("ipp-routing-prompt", "IPP Routing Options");

        helper.addLabel("label-ipp-routing-type-none",
                IppRoutingEnum.NONE.uiText(getLocale()));
        helper.addModifyLabelAttr("ipp-routing-type-none", "",
                MarkupHelper.ATTR_VALUE, IppRoutingEnum.NONE.toString());

        helper.addLabel("label-ipp-routing-type-terminal",
                IppRoutingEnum.TERMINAL.uiText(getLocale()));
        helper.addModifyLabelAttr("ipp-routing-type-terminal", "",
                MarkupHelper.ATTR_VALUE, IppRoutingEnum.TERMINAL.toString());
    }

}
