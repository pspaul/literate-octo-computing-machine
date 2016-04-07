/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages.admin;

import java.util.EnumSet;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dto.UserAccountingDto;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PageUser extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public PageUser(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        //
        Label labelWrk = new Label("internal-user",
                getLocalizer().getString("internal-user", this));
        labelWrk.add(new AttributeModifier("class",
                MarkupHelper.CSS_TXT_INTERNAL_USER));
        add(labelWrk);

        //
        final ACLRoleEnumPanel aclRolePanel =
                new ACLRoleEnumPanel("ACLRoleEnumCheckboxes");
        aclRolePanel.populate(EnumSet.noneOf(ACLRoleEnum.class));

        add(aclRolePanel);

        //
        helper.addModifyLabelAttr("credit-limit-none", "value",
                UserAccountingDto.CreditLimitEnum.NONE.toString());

        helper.addModifyLabelAttr("credit-limit-default", "value",
                UserAccountingDto.CreditLimitEnum.DEFAULT.toString());

        helper.addModifyLabelAttr("credit-limit-individual", "value",
                UserAccountingDto.CreditLimitEnum.INDIVIDUAL.toString());
    }
}
