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
package org.savapage.server.pages.printsite;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dto.CreditLimitDtoEnum;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.LabelEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrintSiteUserEdit extends AbstractPrintSitePage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    public PrintSiteUserEdit(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("internal-user-ind", "Interne gebruiker");
        helper.addLabel("label-password", NounEnum.PASSWORD);

        helper.addLabel("label-user-fullname", NounEnum.NAME);
        helper.addLabel("label-balance", NounEnum.BALANCE);
        helper.addLabel("label-credit-limit", NounEnum.CREDIT_LIMIT);
        helper.addLabel("label-user-disabled", AdverbEnum.DISABLED);

        helper.addLabel("label-user-email-main", LabelEnum.PRIMARY_EMAIL);
        helper.addLabel("label-user-email-other", LabelEnum.OTHER_EMAILS);

        helper.addLabel("label-user-card-number", NounEnum.CARD_NUMBER);
        helper.addLabel("user-delete-warning", PhraseEnum.USER_DELETE_WARNING);

        helper.addLabel("label-financial",
                ACLOidEnum.U_FINANCIAL.uiText(getLocale()));

        helper.addButton("button-ok", HtmlButtonEnum.OK);
        helper.addButton("button-cancel-1", HtmlButtonEnum.CANCEL);
        helper.addButton("button-cancel-2", HtmlButtonEnum.CANCEL);
        helper.addButton("button-delete", HtmlButtonEnum.DELETE);
        helper.addButton("header-delete", HtmlButtonEnum.DELETE);

        helper.addButton("button-user-pw-reset", HtmlButtonEnum.RESET);
        helper.addButton("button-user-pw-erase", HtmlButtonEnum.ERASE);

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
