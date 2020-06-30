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

import java.util.EnumSet;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dto.CreditLimitDtoEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PageUserGroup extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public PageUserGroup(final PageParameters parameters) {

        super(parameters, ACLOidEnum.A_USER_GROUPS, RequiredPermission.EDIT);

        final MarkupHelper helper = new MarkupHelper(this);

        //
        helper.addModifyLabelAttr("img-prompt-roles", "", MarkupHelper.ATTR_SRC,
                MarkupHelper.IMG_PATH_USER_ROLES);
        helper.addModifyLabelAttr("img-prompt-privileges-user", "",
                MarkupHelper.ATTR_SRC, MarkupHelper.IMG_PATH_USER_PRIVILEGES);
        helper.addModifyLabelAttr("img-prompt-privileges-admin", "",
                MarkupHelper.ATTR_SRC, MarkupHelper.IMG_PATH_ADMIN_PRIVILEGES);
        //
        final ACLRoleEnumPanel aclRolePanel =
                new ACLRoleEnumPanel("ACLRoleEnumCheckboxes");

        final EnumSet<ACLRoleEnum> selected = EnumSet
                .of(ACLRoleEnum.JOB_TICKET_OPERATOR, ACLRoleEnum.WEB_CASHIER);

        aclRolePanel.populate(selected);

        add(aclRolePanel);

        //
        ACLPermissionPanel aclPermPanel =
                new ACLPermissionPanel("ACLPermissionsUser");
        aclPermPanel.populate(ACLOidEnum.getUserOidList());
        add(aclPermPanel);

        //
        aclPermPanel = new ACLPermissionPanel("ACLPermissionsAdmin");
        aclPermPanel.populate(ACLOidEnum.getAdminOidList());
        add(aclPermPanel);

        helper.addModifyLabelAttr("credit-limit-none", MarkupHelper.ATTR_VALUE,
                CreditLimitDtoEnum.NONE.toString());
        helper.addModifyLabelAttr("credit-limit-default",
                MarkupHelper.ATTR_VALUE, CreditLimitDtoEnum.DEFAULT.toString());
        helper.addModifyLabelAttr("credit-limit-individual",
                MarkupHelper.ATTR_VALUE,
                CreditLimitDtoEnum.INDIVIDUAL.toString());

        helper.addLabel("label-credit-limit-none",
                CreditLimitDtoEnum.NONE.uiText(getLocale()));
        helper.addLabel("label-credit-limit-default",
                CreditLimitDtoEnum.DEFAULT.uiText(getLocale()));
        helper.addLabel("label-credit-limit-individual",
                CreditLimitDtoEnum.INDIVIDUAL.uiText(getLocale()));
    }

}
