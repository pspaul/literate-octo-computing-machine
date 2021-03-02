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
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class PagePrinter extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param parameters
     *            The page parameters.
     */
    public PagePrinter(final PageParameters parameters) {

        super(parameters, ACLOidEnum.A_PRINTERS, RequiredPermission.EDIT);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addButton("btn-apply", HtmlButtonEnum.APPLY);
        helper.addButton("btn-cancel-1", HtmlButtonEnum.CANCEL);
        helper.addButton("btn-cancel-2", HtmlButtonEnum.CANCEL);

        helper.addButton("btn-rename", HtmlButtonEnum.RENAME);
        helper.addLabel("printer-rename",
                HtmlButtonEnum.RENAME.uiText(getLocale()));

        helper.addLabel("printer-disabled", AdverbEnum.DISABLED);

        helper.addLabel("printer-location", NounEnum.LOCATION);

        helper.addLabel("printer-archive", NounEnum.ARCHIVE);
        helper.addLabel("archive-disabled", AdverbEnum.DISABLED);

        helper.addLabel("printer-journal", NounEnum.JOURNAL);
        helper.addLabel("journal-disabled", AdverbEnum.DISABLED);
    }

}
