/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.helpers.AccountPagerReq;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.server.pages.MarkupHelper;

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
     *
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5; // must be odd number

    /**
     * @return {@code false} to give Admin a chance to inspect the accounts.
     */
    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     *
     */
    public AccountsPage(final PageParameters parameters) {

        super(parameters);

        /*
         * We need a transaction because of the lazy creation of UserAccount
         * instances.
         */
        ServiceContext.getDaoContext().beginTransaction();
        handlePage();
    }

    /**
     *
     */
    private void handlePage() {

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

        final Locale locale = getSession().getLocale();

        //
        add(new PropertyListView<Account>("accounts-view", entryList) {

            private static final long serialVersionUID = 1L;

            /**
             *
             * @param item
             * @param mapVisible
             */
            private void evaluateVisible(final ListItem<Account> item,
                    final Map<String, String> mapVisible) {

                for (Map.Entry<String, String> entry : mapVisible.entrySet()) {

                    if (entry.getValue() == null) {
                        entry.setValue("");
                    }

                    final String cssClassWlk = null;

                    item.add(createVisibleLabel(
                            StringUtils.isNotBlank(entry.getValue()),
                            entry.getKey(), entry.getValue(), cssClassWlk));
                }

            }

            @Override
            protected void populateItem(final ListItem<Account> item) {

                /*
                 *
                 */
                final Map<String, String> mapVisible = new HashMap<>();

                final Account account = item.getModelObject();

                Label labelWrk = null;

                /*
                 *
                 */
                if (account.getParent() == null) {
                    labelWrk =
                            new Label("accountNameParent", account.getName());
                    item.add(labelWrk);
                    labelWrk = new Label("accountName", "");
                    item.add(labelWrk);
                } else {
                    labelWrk = new Label("accountNameParent",
                            account.getParent().getName());
                    item.add(labelWrk);

                    // ⦦ : U+29A6 (OBLIQUE ANGLE OPENING UP)
                    labelWrk = new Label("accountName",
                            String.format("⦦ %s", account.getName()));
                    item.add(labelWrk);
                }

                /*
                 * Signal
                 */
                String signalKey = null;
                String color = null;

                if (account.getDeleted()) {
                    color = MarkupHelper.CSS_TXT_ERROR;
                    signalKey = "signal-deleted";
                }

                String signal = "";
                if (signalKey != null) {
                    signal = localized(signalKey);
                }
                labelWrk = new Label("signal", signal);
                labelWrk.add(new AttributeModifier("class", color));
                item.add(labelWrk);

                /*
                 * Balance
                 */
                try {
                    labelWrk = new Label("balance-amount",
                            BigDecimalUtil.localize(account.getBalance(),
                                    ConfigManager.getUserBalanceDecimals(),
                                    locale,
                                    ServiceContext.getAppCurrencySymbol(),
                                    true));

                    if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                        labelWrk.add(new AttributeModifier("class",
                                MarkupHelper.CSS_AMOUNT_MIN));
                    }

                    item.add(labelWrk);

                } catch (ParseException e) {
                    throw new SpException(e.getMessage());
                }

                /*
                 * Period + Totals
                 */
                final StringBuilder period = new StringBuilder();

                period.append(localizedMediumDate(account.getCreatedDate()));

                if (account.getModifiedDate() != null) {
                    period.append(" ~ ").append(
                            localizedMediumDate(account.getModifiedDate()));
                }

                item.add(new Label("period", period));

                /*
                 * Set the uid in 'data-savapage' attribute, so it can be picked
                 * up in JavaScript for editing.
                 */
                labelWrk = new Label("button-edit",
                        getLocalizer().getString("button-edit", this));

                labelWrk.add(new AttributeModifier(
                        MarkupHelper.ATTR_DATA_SAVAPAGE, account.getId()));

                item.add(labelWrk);

                /*
                 *
                 */
                labelWrk = new Label("button-transaction",
                        getLocalizer().getString("button-transaction", this));
                labelWrk.add(new AttributeModifier(
                        MarkupHelper.ATTR_DATA_SAVAPAGE, account.getId()));
                item.add(labelWrk);

                /*
                 *
                 */
                evaluateVisible(item, mapVisible);

            }
        });

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, accountCount, MAX_PAGES_IN_NAVBAR,
                "sp-shared-accounts-page",
                new String[] { "nav-bar-1", "nav-bar-2" });
    }

}
