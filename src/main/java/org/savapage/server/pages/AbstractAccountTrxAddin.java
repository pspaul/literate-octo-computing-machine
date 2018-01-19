/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutAdjectiveEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
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
     * Dummy name for a missing account.
     */
    private static final String MISSING_ACCOUNT_NAME = "";

    /** */
    private static final class TrxLine {
        private String imgSrcChoice;
        private String accountName;
        private String delegators;
        private String copies;
        private String formattedCost;
        private String remark;
    }

    /** */
    private static final class DelegatorItem {
        private int sourceGroups;
        private int sourcePersonal;
        private int copies;
        private int copiesRefund;
    }

    /** */
    protected static final class AccountTrxView
            extends PropertyListView<TrxLine> {

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
        AccountTrxView(final String id, final List<TrxLine> list) {
            super(id, list);
        }

        @Override
        protected void populateItem(final ListItem<TrxLine> item) {

            final TrxLine trxLine = item.getModelObject();
            final MarkupHelper helper = new MarkupHelper(item);

            helper.addModifyLabelAttr("img-choice", MarkupHelper.ATTR_SRC,
                    trxLine.imgSrcChoice);

            helper.addLabel("account", trxLine.accountName);
            helper.addLabel("delegators", trxLine.delegators);
            helper.addLabel("copies", trxLine.copies);
            helper.addLabel("cost", trxLine.formattedCost)
                    .setEscapeModelStrings(false);
            MarkupHelper.appendLabelAttr(
                    helper.addLabel("remark",
                            StringUtils.defaultString(trxLine.remark)),
                    MarkupHelper.ATTR_CLASS, MarkupHelper.CSS_TXT_WARN);
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

        final List<TrxLine> displayOptions = new ArrayList<>();

        final SortedMap<String, DelegatorItem> delegators =
                fillOptions(trxList, displayOptions);

        add(new AccountTrxView("setting-row", displayOptions));

        final String personalDelegators = formatDelegatorDetails(delegators);
        helper.encloseLabel("persons", personalDelegators,
                StringUtils.isNotBlank(personalDelegators));
    }

    /**
     * Formats delegator information.
     *
     * @param delegators
     *            The delegators.
     * @return The formatted result.
     */
    private String formatDelegatorDetails(
            final SortedMap<String, DelegatorItem> delegators) {

        final StringBuilder details = new StringBuilder();

        int totCopies = 0;
        int totCopiesRefund = 0;

        for (final Entry<String, DelegatorItem> entry : delegators.entrySet()) {

            if (details.length() > 0) {
                details.append(", ");
            }

            final DelegatorItem item = entry.getValue();
            if (item.sourceGroups + item.sourcePersonal > 1) {
                details.append("* ");
            }

            details.append(entry.getKey()).append(" (");

            if (item.copies > 0) {
                details.append(item.copies);
                totCopies += item.copies;
            }
            if (item.copiesRefund > 0) {
                details.append("-").append(item.copiesRefund);
                totCopiesRefund += item.copiesRefund;
            }
            details.append(")");
        }

        final StringBuilder pfx = new StringBuilder();

        if (delegators.size() > 1) {
            pfx.append(delegators.size());
            pfx.append(" ").append(
                    NounEnum.DELEGATOR.uiText(getLocale(), true).toLowerCase())
                    .append(" (");

            if (totCopies > 0) {
                pfx.append(totCopies);
            }
            if (totCopiesRefund > 0) {
                pfx.append("-").append(totCopiesRefund);
            }

            pfx.append(" ").append(PrintOutNounEnum.COPY
                    .uiText(getLocale(), true).toLowerCase());
            pfx.append(") : ");
        }

        return String.format("%s%s", pfx.toString(), details.toString());
    }

    /**
     * Collects delegator totals per Group Account.
     *
     * @param accountTrxList
     *            The account transactions.
     * @return The counters.
     */
    private static Map<String, Integer> collectAccountNameDelegatorCount(
            final List<AccountTrx> accountTrxList) {

        final Map<String, Integer> groupDelegatorCount = new HashMap<>();

        for (final AccountTrx trx : accountTrxList) {

            final Account account = trx.getAccount();

            // Weak link to account name.
            final String accountName;

            if (account == null) {
                accountName = MISSING_ACCOUNT_NAME;
            } else {
                final AccountTypeEnum accountType =
                        AccountTypeEnum.valueOf(account.getAccountType());

                if (accountType == AccountTypeEnum.SHARED
                        || accountType == AccountTypeEnum.GROUP) {
                    continue;
                }

                accountName = trx.getExtDetails();
            }

            if (accountName != null) {
                if (groupDelegatorCount.containsKey(accountName)) {
                    groupDelegatorCount.put(accountName,
                            groupDelegatorCount.get(accountName).intValue()
                                    + 1);
                } else {
                    groupDelegatorCount.put(accountName, 1);
                }
            }
        }

        return groupDelegatorCount;
    }

    /**
     * Fills the display options from the account transactions.
     *
     * @param accountTrxList
     *            The account transactions.
     * @param options
     *            The display values.
     * @return The map of delegators that are charged.
     */
    private SortedMap<String, DelegatorItem> fillOptions(
            final List<AccountTrx> accountTrxList,
            final List<TrxLine> options) {

        final Map<String, Integer> accountNameDelegatorCount =
                collectAccountNameDelegatorCount(accountTrxList);

        final SortedMap<String, DelegatorItem> delegatorItemMap =
                new TreeMap<>();

        final int currencyDecimals = ConfigManager.getUserBalanceDecimals();

        int totIndividualDelegators = 0;
        int totIndividualDelegatorsWeight = 0;
        BigDecimal totIndividualCost = BigDecimal.ZERO;

        int totIndividualDelegatorsRefund = 0;
        int totIndividualDelegatorsWeightRefund = 0;
        BigDecimal totIndividualCostRefund = BigDecimal.ZERO;

        String currencySymbolWlk = null;

        for (final AccountTrx trx : accountTrxList) {

            currencySymbolWlk = CurrencyUtil
                    .getCurrencySymbol(trx.getCurrencyCode(), getLocale());

            final Account account = trx.getAccount();

            final AccountTypeEnum accountType;
            final String accountNameWlk;

            if (account == null) {
                accountType = null;
                accountNameWlk = MISSING_ACCOUNT_NAME;
            } else {
                accountType = AccountTypeEnum.valueOf(account.getAccountType());
                accountNameWlk = account.getName();
            }

            final boolean isRefund =
                    trx.getAmount().compareTo(BigDecimal.ZERO) == 1;

            if (account != null && accountType != AccountTypeEnum.SHARED
                    && accountType != AccountTypeEnum.GROUP) {

                final DelegatorItem item;

                if (delegatorItemMap.containsKey(account.getName())) {
                    item = delegatorItemMap.get(account.getName());
                } else {
                    item = new DelegatorItem();
                    item.sourceGroups = 0;
                    item.sourcePersonal = 0;
                    item.copies = 0;
                    item.copiesRefund = 0;
                    delegatorItemMap.put(account.getName(), item);
                }

                if (isRefund) {
                    item.copiesRefund += trx.getTransactionWeight();
                } else {
                    item.copies += trx.getTransactionWeight();
                }

                if (StringUtils.isBlank(trx.getExtDetails())) {

                    if (isRefund) {
                        totIndividualDelegatorsRefund++;
                        totIndividualDelegatorsWeightRefund +=
                                trx.getTransactionWeight().intValue();
                        totIndividualCostRefund =
                                totIndividualCostRefund.add(trx.getAmount());
                    } else {
                        totIndividualDelegators++;
                        totIndividualDelegatorsWeight +=
                                trx.getTransactionWeight().intValue();
                        totIndividualCost =
                                totIndividualCost.add(trx.getAmount());
                        item.sourceGroups++;
                    }
                } else {
                    if (!isRefund) {
                        item.sourcePersonal++;
                    }
                }

                continue;
            }

            final TrxLine trxLine = new TrxLine();

            if (accountType == AccountTypeEnum.GROUP) {
                if (accountNameDelegatorCount.containsKey(account.getName())) {
                    trxLine.delegators = accountNameDelegatorCount
                            .get(account.getName()).toString();
                } else {
                    trxLine.delegators = "-";
                }
            } else {
                trxLine.delegators = "-";
            }

            trxLine.imgSrcChoice = MarkupHelper.getImgUrlPath(accountType);

            final Account accountParent;

            if (account == null) {
                accountParent = null;
            } else {
                accountParent = account.getParent();
            }

            if (accountParent == null) {
                trxLine.accountName = accountNameWlk;
            } else {
                trxLine.accountName = String.format("%s \\ %s",
                        accountParent.getName(), account.getName());
            }

            if (isRefund) {
                trxLine.copies =
                        String.valueOf(-trx.getTransactionWeight().intValue());
                trxLine.remark =
                        NounEnum.REFUND.uiText(getLocale()).toLowerCase();
            } else {
                trxLine.copies = trx.getTransactionWeight().toString();
            }

            final StringBuilder sbAccTrx = new StringBuilder();

            sbAccTrx.append(currencySymbolWlk).append("&nbsp;")
                    .append(BigDecimalUtil.localizeUc(trx.getAmount(),
                            currencyDecimals, getSession().getLocale(), true));

            trxLine.formattedCost = sbAccTrx.toString();
            options.add(trxLine);
        }

        if (totIndividualDelegators > 0) {

            final TrxLine values = getPersonCatOptions(
                    PrintOutAdjectiveEnum.PERSONAL.uiText(getLocale()),
                    totIndividualDelegators, totIndividualDelegatorsWeight,
                    totIndividualCost, currencySymbolWlk, currencyDecimals,
                    options.size() > 1 || totIndividualDelegators > 1);

            options.add(values);
        }

        if (totIndividualDelegatorsRefund > 0) {

            final TrxLine values = getPersonCatOptions(
                    PrintOutAdjectiveEnum.PERSONAL.uiText(getLocale()),
                    totIndividualDelegatorsRefund,
                    totIndividualDelegatorsWeightRefund,
                    totIndividualCostRefund, currencySymbolWlk,
                    currencyDecimals,
                    options.size() > 1 || totIndividualDelegatorsRefund > 1);

            options.add(values);
        }

        return delegatorItemMap;
    }

    /**
     * Creates an {@link TrxLine} for delegators.
     *
     * @param uiName
     *            The name.
     * @param totDelegators
     *            Number of delegators.
     * @param totDelegatorsWeight
     *            Number of copies.
     * @param totCost
     *            Cost.
     * @param currencySymbolWlk
     *            Currency symbol.
     * @param currencyDecimals
     *            Number of currency decimals.
     * @param showTotDelegators
     *            {@code true} when number of delegators must be shown.
     * @return The line.
     */
    private TrxLine getPersonCatOptions(final String uiName,
            final int totDelegators, final int totDelegatorsWeight,
            final BigDecimal totCost, final String currencySymbolWlk,
            final int currencyDecimals, final boolean showTotDelegators) {

        final TrxLine trxLine = new TrxLine();

        trxLine.accountName = uiName;
        if (showTotDelegators) {
            trxLine.delegators = String.valueOf(totDelegators);
        } else {
            trxLine.delegators = "";
        }

        trxLine.imgSrcChoice = MarkupHelper.getImgUrlPath(AccountTypeEnum.USER);

        trxLine.formattedCost = String.format("%s&nbsp;%s", currencySymbolWlk,
                BigDecimalUtil.localizeUc(totCost, currencyDecimals,
                        getSession().getLocale(), true));

        if (totCost.compareTo(BigDecimal.ZERO) == 1) {
            trxLine.copies = String.valueOf(-totDelegatorsWeight);
            trxLine.remark = NounEnum.REFUND.uiText(getLocale()).toLowerCase();
        } else {
            trxLine.copies = String.valueOf(totDelegatorsWeight);
        }

        return trxLine;
    }

}
