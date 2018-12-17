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
package org.savapage.server.pages.admin;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import org.apache.commons.lang3.EnumUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.helpers.AccountPagerReq;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.account.UserAccountContextEnum;
import org.savapage.core.services.helpers.account.UserAccountContextFactory;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.server.helpers.account.UserAccountContextHtmlFactory;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountsPage extends AbstractAdminListPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Note: must be odd number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /**
     * @return {@code false} to give Admin a chance to inspect the accounts.
     */
    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private final class AccountListView extends PropertyListView<Account> {

        private static final long serialVersionUID = 1L;

        /** */
        private static final String WID_BUTTON_EDIT = "button-edit";
        /** */
        private static final String WID_BUTTON_TRX = "button-transaction";

        /** */
        private static final String WID_ACCOUNT_NAME = "accountName";

        /** */
        private static final String WID_ACCOUNT_NAME_PARENT =
                "accountNameParent";

        /** */
        private static final String WID_BALANCE_SAVAPAGE_ICON =
                "balance-savapage-icon";

        /***/
        private final boolean isEditor;
        /***/
        private final boolean hasAccessTrx;

        /** */
        private final boolean hasPaperCutUserAccountView;

        /**
         * .
         *
         * @param id
         * @param list
         */
        public AccountListView(final String id, final List<Account> list,
                final boolean isEditor) {

            super(id, list);

            this.isEditor = isEditor;
            this.hasAccessTrx = ACCESS_CONTROL_SERVICE.hasAccess(
                    SpSession.get().getUser(), ACLOidEnum.A_TRANSACTIONS);
            this.hasPaperCutUserAccountView =
                    UserAccountContextFactory.hasContextPaperCut();
        }

        @Override
        protected void populateItem(final ListItem<Account> item) {

            final Account account = item.getModelObject();

            Label labelWrk = null;

            if (account.getParent() == null) {
                labelWrk =
                        new Label(WID_ACCOUNT_NAME_PARENT, account.getName());
                item.add(labelWrk);
                labelWrk = new Label(WID_ACCOUNT_NAME, "");
                item.add(labelWrk);
            } else {
                labelWrk = new Label(WID_ACCOUNT_NAME_PARENT,
                        account.getParent().getName());
                item.add(labelWrk);

                labelWrk = new Label(WID_ACCOUNT_NAME,
                        String.format("%s %s",
                                MarkupHelper.HTML_ENT_OBL_ANGLE_OPENING_UP,
                                account.getName()));
                labelWrk.setEscapeModelStrings(false);
                item.add(labelWrk);
            }

            /*
             * Image
             */
            final AccountTypeEnum accountType = EnumUtils
                    .getEnum(AccountTypeEnum.class, account.getAccountType());

            labelWrk = new Label("accountImage", "");
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_SRC,
                    MarkupHelper.getImgUrlPath(accountType)));
            item.add(labelWrk);

            /*
             * Signal
             */
            String signalKey = null;
            String color = null;

            if (account.getDeleted()) {
                color = MarkupHelper.CSS_TXT_ERROR;
                signalKey = "signal-deleted";
            } else if (account.getDisabled()) {
                color = MarkupHelper.CSS_TXT_ERROR;
                signalKey = "signal-disabled";
            }

            String signal = "";
            if (signalKey != null) {
                signal = localized(signalKey);
            }
            labelWrk = new Label("signal", signal);
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS, color));
            item.add(labelWrk);

            final MarkupHelper helper = new MarkupHelper(item);

            /*
             * Balance
             */
            try {
                labelWrk = new Label("balance-amount",
                        BigDecimalUtil.localize(account.getBalance(),
                                ConfigManager.getUserBalanceDecimals(),
                                getLocale(),
                                ServiceContext.getAppCurrencySymbol(), true));

                if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                    labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS,
                            MarkupHelper.CSS_AMOUNT_MIN));
                }

                item.add(labelWrk);

                if (this.hasPaperCutUserAccountView) {
                    helper.addTransparant(WID_BALANCE_SAVAPAGE_ICON)
                            .add(new AttributeModifier(MarkupHelper.ATTR_SRC,
                                    UserAccountContextHtmlFactory.getContext(
                                            UserAccountContextEnum.SAVAPAGE)
                                            .getImgUrl()));
                } else {
                    helper.discloseLabel(WID_BALANCE_SAVAPAGE_ICON);
                }

            } catch (ParseException e) {
                throw new SpException(e.getMessage());
            }

            /*
             * Period + Totals
             */
            final StringBuilder period = new StringBuilder();

            period.append(localizedMediumDate(account.getCreatedDate()));

            if (account.getModifiedDate() != null) {
                period.append(" ~ ")
                        .append(localizedMediumDate(account.getModifiedDate()));
            }

            item.add(new Label("period", period));

            /*
             * Set the uid in 'data-savapage' attribute, so it can be picked up
             * in JavaScript for editing.
             */
            if (this.isEditor) {
                helper.addModifyLabelAttr(WID_BUTTON_EDIT,
                        getLocalizer().getString("button-edit", this),
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        account.getId().toString());
            } else {
                helper.discloseLabel(WID_BUTTON_EDIT);
            }

            if (this.hasAccessTrx) {
                helper.addModifyLabelAttr(WID_BUTTON_TRX,
                        getLocalizer().getString("button-transaction", this),
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        account.getId().toString());
            } else {
                helper.discloseLabel(WID_BUTTON_TRX);
            }

            helper.addTransparantInvisible("button-group",
                    !this.isEditor && !this.hasAccessTrx);
        }

    }

    /**
     *
     */
    public AccountsPage(final PageParameters parameters) {

        super(parameters);

        final boolean hasEditorAccess =
                this.probePermissionToEdit(ACLOidEnum.A_ACCOUNTS);
        /*
         * We need a transaction because of the lazy creation of UserAccount
         * instances.
         */
        ServiceContext.getDaoContext().beginTransaction();

        final String data = getParmValue(POST_PARM_DATA);
        final AccountPagerReq req = AccountPagerReq.read(data);

        final AccountDao.Field sortField = req.getSort().getSortField();
        final boolean sortAscending = req.getSort().getAscending();

        final AccountDao.ListFilter filter = new AccountDao.ListFilter();

        final AccountTypeEnum accountTypeSelect =
                req.getSelect().getAccountType();

        if (accountTypeSelect == null) {
            filter.setAccountType(Account.AccountTypeEnum.SHARED);
            filter.setAccountTypeExtra(Account.AccountTypeEnum.GROUP);
        } else {
            filter.setAccountType(accountTypeSelect);
        }

        filter.setContainingNameText(req.getSelect().getNameContainingText());
        filter.setDeleted(req.getSelect().getDeleted());

        final AccountDao accountDao =
                ServiceContext.getDaoContext().getAccountDao();

        final long accountCount = accountDao.getListCount(filter);

        /*
         * Display the requested page.
         */
        final List<Account> entryList =
                accountDao.getListChunk(filter, req.calcStartPosition(),
                        req.getMaxResults(), sortField, sortAscending);

        //
        add(new AccountListView("accounts-view", entryList, hasEditorAccess));

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, accountCount, MAX_PAGES_IN_NAVBAR,
                "sp-shared-accounts-page",
                new String[] { "nav-bar-1", "nav-bar-2" });
    }

}
