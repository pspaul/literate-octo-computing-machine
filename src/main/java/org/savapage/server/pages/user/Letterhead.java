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
package org.savapage.server.pages.user;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class Letterhead extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /**
     * @param parameters
     *            PageParameters.
     */
    public Letterhead(final PageParameters parameters) {

        super(parameters);

        add(MarkupHelper.createEncloseLabel("letterhead-public",
                AdjectiveEnum.PUBLIC.uiText(getLocale()), isAdminUser()));

        final boolean canCreate;

        if (isAdminUser()) {
            canCreate = true;
        } else {
            final Integer letterheadPriv = ACCESS_CONTROL_SERVICE.getPrivileges(
                    SpSession.get().getUserIdDto(), ACLOidEnum.U_LETTERHEAD);
            canCreate = letterheadPriv == null || ACLPermissionEnum.EDITOR
                    .isPresent(letterheadPriv.intValue());
        }

        add(MarkupHelper.createEncloseLabel("button-new",
                HtmlButtonEnum.NEW.uiText(getLocale(), true), canCreate));

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("title", NounEnum.LETTERHEAD.uiText(getLocale(), true));
        helper.addLabel("prompt-name", NounEnum.NAME);

        helper.addLabel("prompt-position", NounEnum.POSITION);
        helper.addLabel("pos-foreground", NounEnum.FOREGROUND);
        helper.addLabel("pos-background", NounEnum.BACKGROUND);

        helper.addButton("button-apply", HtmlButtonEnum.APPLY);
        helper.addButton("button-delete", HtmlButtonEnum.DELETE);
        helper.addButton("button-refresh", HtmlButtonEnum.REFRESH);
        helper.addButton("button-back", HtmlButtonEnum.BACK);
    }
}
