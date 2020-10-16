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
package org.savapage.server.pages.printsite;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dto.CreditLimitDtoEnum;
import org.savapage.core.dto.UserDto;
import org.savapage.core.dto.UserEmailDto;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.LabelEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
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

    /**
     * Database ID as String.
     */
    private static final String PAGE_PARM_USERKEY = "userKey";

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();

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

        ServiceContext.setLocale(getLocale());

        final IRequestParameters parms =
                getRequestCycle().getRequest().getPostParameters();

        final Long userKey =
                parms.getParameterValue(PAGE_PARM_USERKEY).toOptionalLong();

        //
        final org.savapage.core.jpa.User dbUser = USER_DAO.findById(userKey);
        final UserDto dto = USER_SERVICE.createUserDto(dbUser);

        final MarkupHelper helper = new MarkupHelper(this);

        //
        helper.addLabel("user-userid", dbUser.getUserId());
        helper.addModifyLabelAttr("input-user-fullname",
                MarkupHelper.ATTR_VALUE, dbUser.getFullName());

        //
        helper.addCheckbox("cb-user-disabled",
                BooleanUtils.isTrue(dto.getDisabled()));

        helper.addTextInput("txt-user-email", dto.getEmail());

        final StringBuilder emails = new StringBuilder();

        for (final UserEmailDto em : dto.getEmailOther()) {
            if (emails.length() > 0) {
                emails.append('\n');
            }
            emails.append(em.getAddress());
        }
        helper.addLabel("area-user-email-other", emails.toString());
        helper.addTextInput("txt-user-card-number", dto.getCard());

        //
        final boolean isInternalUser = BooleanUtils.isTrue(dto.getInternal());

        helper.encloseLabel("label-password",
                NounEnum.PASSWORD.uiText(getLocale()), isInternalUser);

        if (isInternalUser) {
            helper.addButton("button-user-pw-reset", HtmlButtonEnum.RESET);
            helper.addButton("button-user-pw-erase", HtmlButtonEnum.ERASE);
        }

        //
        helper.addLabel("label-user-fullname", NounEnum.NAME);

        //
        helper.addLabel("label-balance", NounEnum.BALANCE);
        helper.addTextInput("txt-balance", dto.getAccounting().getBalance());

        helper.addLabel("label-credit-limit", NounEnum.CREDIT_LIMIT);
        helper.addLabel("label-user-disabled", AdverbEnum.DISABLED);

        helper.addLabel("label-user-email-main", LabelEnum.PRIMARY_EMAIL);
        helper.addLabel("label-user-email-other", LabelEnum.OTHER_EMAILS);
        //
        helper.addLabel("label-user-card-number", NounEnum.CARD_NUMBER);

        helper.addButton("button-apply", HtmlButtonEnum.APPLY);

        helper.encloseLabel("button-delete",
                HtmlButtonEnum.DELETE.uiTextDottedSfx(getLocale()),
                isInternalUser);

        //
        helper.addTextInput("txt-account-credit-limit-amount",
                dto.getAccounting().getCreditLimitAmount());

        setCreditLimit(helper, "credit-limit-none", CreditLimitDtoEnum.NONE,
                dto.getAccounting().getCreditLimit());
        setCreditLimit(helper, "credit-limit-default",
                CreditLimitDtoEnum.DEFAULT,
                dto.getAccounting().getCreditLimit());
        setCreditLimit(helper, "credit-limit-individual",
                CreditLimitDtoEnum.INDIVIDUAL,
                dto.getAccounting().getCreditLimit());

        helper.addLabel("label-credit-limit-none",
                CreditLimitDtoEnum.NONE.uiText(getLocale()));
        helper.addLabel("label-credit-limit-default",
                CreditLimitDtoEnum.DEFAULT.uiText(getLocale()));
        helper.addLabel("label-credit-limit-individual",
                CreditLimitDtoEnum.INDIVIDUAL.uiText(getLocale()));
    }

    /**
     * Sets (selects) credit limit radio button.
     *
     * @param helper
     *            Helper.
     * @param labelId
     *            Wicket label id.
     * @param credLimit
     *            Credit limit
     * @param credLimitSel
     *            Selected credit limit.
     */
    private static void setCreditLimit(final MarkupHelper helper,
            final String labelId, final CreditLimitDtoEnum credLimit,
            final CreditLimitDtoEnum credLimitSel) {
        final Label label = helper.addModifyLabelAttr(labelId,
                MarkupHelper.ATTR_VALUE, credLimit.toString());
        if (credLimit == credLimitSel) {
            MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_CHECKED,
                    MarkupHelper.ATTR_CHECKED);
        }
    }
}
