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
package org.savapage.server.pages;

import org.apache.commons.lang3.EnumUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.AccountTrxTypeEnum;
import org.savapage.core.dao.helpers.AccountTrxPagerReq;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.session.SpSession;
import org.savapage.server.webapp.WebAppTypeEnum;

/**
 *
 * @author Rijk Ravestein
 */
public final class AccountTrxBase extends AbstractAuthPage {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The {@code PageParameters}.
     */
    public AccountTrxBase(final PageParameters parameters) {

        super(parameters);

        final WebAppTypeEnum webAppType = this.getSessionWebAppType();

        handlePage(webAppType == WebAppTypeEnum.ADMIN);
    }

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * @param adminWebApp
     */
    private void handlePage(final boolean adminWebApp) {

        final String data = getParmValue(POST_PARM_DATA);

        final AccountTrxPagerReq req = AccountTrxPagerReq.read(data);

        Long userId = null;
        Long accountId = null;

        boolean userNameVisible = false;
        boolean accountNameVisible = false;

        if (adminWebApp) {

            userId = req.getSelect().getUserId();
            userNameVisible = (userId != null);

            accountId = req.getSelect().getAccountId();
            accountNameVisible = (accountId != null);

        } else {
            /*
             * If we are called in a User WebApp context we ALWAYS use the user
             * of the current session.
             */
            userId = SpSession.get().getUser().getId();
        }

        //
        String userName = null;
        String accountName = null;

        final AccountTypeEnum accountType;

        if (userNameVisible) {

            final User user = ServiceContext.getDaoContext().getUserDao()
                    .findById(userId);

            userName = user.getUserId();

            accountType = AccountTypeEnum.USER;

        } else if (accountNameVisible) {

            final Account account = ServiceContext.getDaoContext()
                    .getAccountDao().findById(accountId);

            accountType = EnumUtils.getEnum(AccountTypeEnum.class,
                    account.getAccountType());

            final Account accountParent = account.getParent();
            if (accountParent == null) {
                accountName = account.getName();
            } else {
                accountName = String.format("%s <sub>%s %s</sub>",
                        accountParent.getName(),
                        MarkupHelper.HTML_ENT_OBL_ANGLE_OPENING_UP,
                        account.getName());
            }
        } else {
            accountType = null;
        }

        //
        Label hiddenLabel = new Label("hidden-user-id");
        String hiddenValue = "";

        if (userId != null) {
            hiddenValue = userId.toString();
        }
        hiddenLabel.add(new AttributeModifier("value", hiddenValue));
        add(hiddenLabel);

        //
        hiddenLabel = new Label("hidden-account-id");
        hiddenValue = "";

        if (accountId != null) {
            hiddenValue = accountId.toString();
        }
        hiddenLabel.add(new AttributeModifier("value", hiddenValue));
        add(hiddenLabel);

        //
        add(new JrExportFileExtButtonPanel("report-button-panel",
                "sp-btn-accounttrx-report"));

        //
        final MarkupHelper helper = new MarkupHelper(this);

        helper.addModifyLabelAttr("accounttrx-select-type-initial", "value",
                AccountTrxTypeEnum.INITIAL.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-adjust", "value",
                AccountTrxTypeEnum.ADJUST.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-deposit", "value",
                AccountTrxTypeEnum.DEPOSIT.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-gateway", "value",
                AccountTrxTypeEnum.GATEWAY.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-transfer", "value",
                AccountTrxTypeEnum.TRANSFER.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-voucher", "value",
                AccountTrxTypeEnum.VOUCHER.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-printin", "value",
                AccountTrxTypeEnum.PRINT_IN.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-printout", "value",
                AccountTrxTypeEnum.PRINT_OUT.toString());

        //
        helper.encloseLabel("select-and-sort-user", userName, userNameVisible);
        helper.encloseLabel("select-and-sort-account", accountName,
                accountNameVisible).setEscapeModelStrings(false);

        if (accountType != null) {
            helper.addModifyLabelAttr("accountImage", "", "src",
                    MarkupHelper.getImgUrlPath(accountType));
        }

    }

}
