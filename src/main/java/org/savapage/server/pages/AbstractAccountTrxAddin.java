/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.i18n.PrintOutAdjectiveEnum;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.CurrencyUtil;

/**
 *
 * @author Rijk Ravestein
 *
 */
abstract class AbstractAccountTrxAddin extends AbstractAuthPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    protected static final class AccountTrxView
            extends PropertyListView<String[]> {

        /**
         * .
         */
        private static final long serialVersionUID = 1L;

        /**
         *
         * @param id
         *            The wicket id.
         * @param list
         *            The item list.
         */
        AccountTrxView(final String id, final List<String[]> list) {
            super(id, list);
        }

        @Override
        protected void populateItem(final ListItem<String[]> item) {

            final String[] column = item.getModelObject();
            final MarkupHelper helper = new MarkupHelper(item);

            helper.addLabel("account", column[0]);
            helper.addLabel("copies", column[1]);
            helper.addLabel("cost", column[2]).setEscapeModelStrings(false);
        }
    }

    @Override
    protected final boolean needMembership() {
        return false;
    }

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    AbstractAccountTrxAddin(final PageParameters parameters) {
        super(parameters);
    }

    /**
     * Displays account transactions.
     *
     * @param trxList
     *            The account transactions (can be {@code null}).
     */
    protected void populate(final List<AccountTrx> trxList) {

        final MarkupHelper helper = new MarkupHelper(this);

        if (trxList == null) {
            helper.discloseLabel("setting-row");
            return;
        }

        final List<String[]> displayOptions = new ArrayList<>();
        final String personalDelegators = fillOptions(trxList, displayOptions);

        helper.encloseLabel("persons", personalDelegators,
                StringUtils.isNotBlank(personalDelegators));
        add(new AccountTrxView("setting-row", displayOptions));
    }

    /**
     * Fills the display options from the account transactions.
     *
     * @param accountTrxList
     *            The account transactions.
     * @param options
     *            The display values.
     * @return The string of persons that are charged.
     */
    private String fillOptions(final List<AccountTrx> accountTrxList,
            final List<String[]> options) {

        final StringBuilder personalDelegators = new StringBuilder();

        final int currencyDecimals = ConfigManager.getUserBalanceDecimals();

        int totPersonalDelegators = 0;
        int totPersonalDelegatorsWeight = 0;
        int totImplicitDelegatorsWeight = 0;

        BigDecimal totPersonalCost = BigDecimal.ZERO;

        String currencySymbolWlk = null;

        for (final AccountTrx trx : accountTrxList) {

            currencySymbolWlk = CurrencyUtil
                    .getCurrencySymbol(trx.getCurrencyCode(), getLocale());

            final Account account = trx.getAccount();

            final AccountTypeEnum accountType =
                    AccountTypeEnum.valueOf(account.getAccountType());

            if (accountType != AccountTypeEnum.SHARED
                    && accountType != AccountTypeEnum.GROUP) {

                if (totPersonalDelegators > 0) {
                    personalDelegators.append(", ");
                }
                personalDelegators.append(trx.getAccount().getName())
                        .append(" (").append(trx.getTransactionWeight())
                        .append(")");

                totPersonalDelegators++;
                totPersonalDelegatorsWeight +=
                        trx.getTransactionWeight().intValue();
                totPersonalCost = totPersonalCost.add(trx.getAmount());
                continue;
            }

            totImplicitDelegatorsWeight +=
                    trx.getTransactionWeight().intValue();

            final Account accountParent = account.getParent();

            final String[] values = new String[3];

            if (accountParent == null) {
                values[0] = account.getName();
            } else {
                values[0] = String.format("%s \\ %s", accountParent.getName(),
                        account.getName());
            }

            values[1] = trx.getTransactionWeight().toString();

            final StringBuilder sbAccTrx = new StringBuilder();

            sbAccTrx.append(currencySymbolWlk).append("&nbsp;")
                    .append(BigDecimalUtil.localizeUc(trx.getAmount(),
                            currencyDecimals, getSession().getLocale(), true));

            values[2] = sbAccTrx.toString();
            options.add(values);
        }

        if (totPersonalDelegators > 0) {
            final String[] values = new String[3];

            final String personal =
                    PrintOutAdjectiveEnum.PERSONAL.uiText(getLocale());

            if (totPersonalDelegators > 1) {
                values[0] = String.format("%s (%d)", personal,
                        totPersonalDelegators);
            } else {
                values[0] = personal;
            }

            values[1] = String.valueOf(totPersonalDelegatorsWeight);
            values[2] = String.format("%s&nbsp;%s", currencySymbolWlk,
                    BigDecimalUtil.localizeUc(totPersonalCost, currencyDecimals,
                            getSession().getLocale(), true));

            options.add(values);
        }
        return personalDelegators.toString();
    }

}
