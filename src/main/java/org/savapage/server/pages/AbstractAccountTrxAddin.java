/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.pages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
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
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.CurrencyUtil;
import org.savapage.core.util.LocaleHelper;
import org.savapage.server.helpers.HtmlButtonEnum;

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

    /** */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /**
     * Dummy name for a missing account.
     */
    private static final String MISSING_ACCOUNT_NAME = "";

    /** */
    private static final String WID_BTN_APPLY = "btn-apply";
    /** */
    private static final String WID_BTN_RESET = "btn-reset";

    /** */
    private static final class TrxLine {

        private static final String NO_DELEGATORS = "-";
        private static final String NO_COPIES = "-";

        private String imgSrcChoice;
        private String accountNameDisplay;
        private Long accountID;
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
        private BigDecimal copiesDecimal;
        private BigDecimal copiesRefundDecimal;
    }

    /** */
    protected static final class AccountTrxView
            extends PropertyListView<TrxLine> {

        /** */
        private static final long serialVersionUID = 1L;

        /** */
        private static final String WID_IMG_CHOICE = "img-choice";
        /** */
        private static final String WID_ACCCOUNT = "account";

        /** */
        private static final String WID_DELEGATORS = "delegators";
        /** */
        private static final String WID_COPIES = "copies";
        /** */
        private static final String WID_COPIES_EDIT = "copies-edit";
        /** */
        private static final String WID_COST = "cost";
        /** */
        private static final String WID_REMARK = "remark";

        /**
         * If {@code true} named account copies can be edited.
         */
        private final boolean allowEditCopies;

        /**
         *
         * @param id
         *            The wicket id.
         * @param list
         *            The item list.
         * @param editCopies
         *            If {@code true} named account copies can be edited.
         */
        AccountTrxView(final String id, final List<TrxLine> list,
                final boolean editCopies) {
            super(id, list);
            this.allowEditCopies = editCopies;
        }

        /**
         * @return {@code true} if there is at least one account ID for which
         *         copies can be edited.
         */
        public boolean hasEditableCopies() {
            if (this.allowEditCopies) {
                for (final TrxLine line : this.getList()) {
                    if (line.accountID != null) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        protected void populateItem(final ListItem<TrxLine> item) {

            final TrxLine trxLine = item.getModelObject();
            final MarkupHelper helper = new MarkupHelper(item);

            helper.addModifyLabelAttr(WID_IMG_CHOICE, MarkupHelper.ATTR_SRC,
                    trxLine.imgSrcChoice);

            final Label labelAccount =
                    helper.addLabel(WID_ACCCOUNT, trxLine.accountNameDisplay);

            if (this.allowEditCopies && trxLine.accountID != null) {

                helper.discloseLabel(WID_COPIES);

                if (!trxLine.delegators.equals(TrxLine.NO_DELEGATORS)) {
                    MarkupHelper.modifyLabelAttr(labelAccount,
                            MarkupHelper.ATTR_DATA_SAVAPAGE,
                            trxLine.accountName);
                }

                final Label labelCopies = helper.encloseLabel(WID_COPIES_EDIT,
                        "", trxLine.accountID != null);
                MarkupHelper.modifyLabelAttr(labelCopies,
                        MarkupHelper.ATTR_VALUE, trxLine.copies);
                MarkupHelper.modifyLabelAttr(labelCopies,
                        MarkupHelper.ATTR_DATA_SAVAPAGE_KEY,
                        trxLine.accountID.toString());
                MarkupHelper.modifyLabelAttr(labelCopies,
                        MarkupHelper.ATTR_DATA_SAVAPAGE, trxLine.copies);

            } else {
                helper.discloseLabel(WID_COPIES_EDIT);
                final Label labelCopies =
                        helper.addLabel(WID_COPIES, trxLine.copies);
                if (this.allowEditCopies) {
                    MarkupHelper.modifyLabelAttr(labelCopies,
                            MarkupHelper.ATTR_ALIGN, "left");
                    MarkupHelper.modifyLabelAttr(labelCopies,
                            MarkupHelper.ATTR_STYLE, "padding-left:15px;");
                }
            }

            helper.addLabel(WID_DELEGATORS, trxLine.delegators);

            helper.addLabel(WID_COST, trxLine.formattedCost)
                    .setEscapeModelStrings(false);

            MarkupHelper.appendLabelAttr(
                    helper.addLabel(WID_REMARK,
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
     * @return Job ticket file name, or {@code null} when not applicable.
     */
    abstract String getJobTicketFileName();

    /**
     * Displays account transactions.
     *
     * @param totalAmount
     *            Total amount (cost).
     * @param totalCopies
     *            Total number of printed copies.
     * @param trxList
     *            The account transactions (can be {@code null}).
     * @param editCopies
     *            If {@code true} named account copies can be edited.
     */
    protected final void populate(final BigDecimal totalAmount,
            final int totalCopies, final List<AccountTrx> trxList,
            final boolean editCopies) {

        final MarkupHelper helper = new MarkupHelper(this);

        if (trxList == null) {
            helper.discloseLabel("setting-row");
            return;
        }

        final List<TrxLine> displayTrxLines = new ArrayList<>();

        final SortedMap<String, DelegatorItem> delegators =
                this.fillDisplayOptions(totalAmount, totalCopies, trxList,
                        displayTrxLines, editCopies);

        final AccountTrxView trxView =
                new AccountTrxView("setting-row", displayTrxLines, editCopies);

        add(trxView);

        if (trxView.hasEditableCopies()
                && this.getJobTicketFileName() != null) {
            MarkupHelper.modifyLabelAttr(
                    helper.addLabel(WID_BTN_APPLY,
                            HtmlButtonEnum.APPLY.uiText(getLocale())),
                    MarkupHelper.ATTR_DATA_SAVAPAGE,
                    this.getJobTicketFileName());

            helper.encloseLabel(WID_BTN_RESET,
                    HtmlButtonEnum.RESET.uiText(getLocale()), editCopies);
        } else {
            helper.discloseLabel(WID_BTN_APPLY);
        }

        final String personalDelegators =
                this.formatDelegatorDetails(delegators);
        helper.encloseLabel("persons", personalDelegators,
                StringUtils.isNotBlank(personalDelegators));
    }

    /**
     * Formats delegator copies information.
     *
     * @param delegators
     *            The delegators.
     * @return The formatted result.
     */
    private String formatDelegatorDetails(
            final SortedMap<String, DelegatorItem> delegators) {

        final LocaleHelper localeHelper = new LocaleHelper(getLocale());

        final StringBuilder details = new StringBuilder();

        BigDecimal totCopiesDecimal = BigDecimal.ZERO;
        BigDecimal totCopiesRefundDecimal = BigDecimal.ZERO;

        for (final Entry<String, DelegatorItem> entry : delegators.entrySet()) {

            if (details.length() > 0) {
                details.append(", ");
            }

            final DelegatorItem item = entry.getValue();
            if (item.sourceGroups + item.sourcePersonal > 1) {
                details.append("* ");
            }

            details.append(entry.getKey());

            final StringBuilder entryDetails = new StringBuilder();
            if (item.copiesDecimal.compareTo(BigDecimal.ZERO) != 0) {
                entryDetails.append(localeHelper
                        .asExactIntegerOrScaled(item.copiesDecimal));
                totCopiesDecimal = totCopiesDecimal.add(item.copiesDecimal);
            }

            if (item.copiesRefundDecimal.compareTo(BigDecimal.ZERO) != 0) {
                entryDetails.append(localeHelper
                        .asExactIntegerOrScaled(item.copiesRefundDecimal));
                totCopiesRefundDecimal =
                        totCopiesRefundDecimal.add(item.copiesRefundDecimal);
            }
            if (entryDetails.length() > 0) {
                details.append(" (").append(entryDetails).append(")");
            }
        }

        final StringBuilder pfx = new StringBuilder();

        if (delegators.size() > 1) {
            pfx.append(delegators.size());
            pfx.append(" ").append(
                    NounEnum.DELEGATOR.uiText(getLocale(), true).toLowerCase());

            final StringBuilder pfxDetails = new StringBuilder();

            boolean copiesPlural = false;

            if (totCopiesDecimal.compareTo(BigDecimal.ZERO) != 0) {
                final BigDecimal bd =
                        totCopiesDecimal.setScale(0, RoundingMode.HALF_DOWN);
                pfxDetails.append(bd);
                if (bd.compareTo(BigDecimal.ONE) == 1) {
                    copiesPlural = true;
                }
            }

            if (totCopiesRefundDecimal.compareTo(BigDecimal.ZERO) != 0) {
                final BigDecimal bd = totCopiesRefundDecimal.setScale(0,
                        RoundingMode.HALF_DOWN);
                pfxDetails.append(bd);
                if (bd.abs().compareTo(BigDecimal.ONE) == 1) {
                    copiesPlural = true;
                }
            }

            if (pfxDetails.length() > 0) {
                pfxDetails.append(" ").append(PrintOutNounEnum.COPY
                        .uiText(getLocale(), copiesPlural).toLowerCase());
                pfx.append(" (").append(pfxDetails).append(")");
            }
            pfx.append(" : ");
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
     * @param totalCost
     *            The total cost of all transactions. A negative value is a
     *            refund.
     * @param totalCopies
     *            Total number of (printed) copies.
     * @param accountTrxList
     *            The account transactions (input).
     * @param displayTrxLines
     *            The display lines (output).
     * @param editCopies
     *            If {@code true} named account copies can be edited.
     * @return The map of delegators that are charged.
     */
    private SortedMap<String, DelegatorItem> fillDisplayOptions(
            BigDecimal totalCost, int totalCopies,
            final List<AccountTrx> accountTrxList,
            final List<TrxLine> displayTrxLines, final boolean editCopies) {

        final BigDecimal costPerCopy = ACCOUNTING_SERVICE
                .calcCostPerPrintedCopy(totalCost, totalCopies);

        final Map<String, Integer> accountNameDelegatorCount =
                collectAccountNameDelegatorCount(accountTrxList);

        final SortedMap<String, DelegatorItem> delegatorItemMap =
                new TreeMap<>();

        final int currencyDecimals = ConfigManager.getUserBalanceDecimals();

        int totIndividualDelegators = 0;
        BigDecimal totIndividualDelegatorsCopies = BigDecimal.ZERO;
        BigDecimal totIndividualCost = BigDecimal.ZERO;

        int totIndividualDelegatorsRefund = 0;
        BigDecimal totIndividualDelegatorsCopiesRefund = BigDecimal.ZERO;
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

            // Individual delegator.
            if (account != null && accountType != AccountTypeEnum.SHARED
                    && accountType != AccountTypeEnum.GROUP) {

                final DelegatorItem item;

                if (delegatorItemMap.containsKey(account.getName())) {
                    item = delegatorItemMap.get(account.getName());
                } else {
                    item = new DelegatorItem();

                    item.sourceGroups = 0;
                    item.sourcePersonal = 0;
                    item.copiesDecimal = BigDecimal.ZERO;
                    item.copiesRefundDecimal = BigDecimal.ZERO;

                    delegatorItemMap.put(account.getName(), item);
                }

                final BigDecimal printedCopies;

                if (costPerCopy.compareTo(BigDecimal.ZERO) == 0) {
                    printedCopies = BigDecimal.ZERO;
                } else {
                    printedCopies = ACCOUNTING_SERVICE.calcPrintedCopies(
                            trx.getAmount().negate(), costPerCopy, 2);

                    if (isRefund) {
                        item.copiesRefundDecimal =
                                item.copiesRefundDecimal.add(printedCopies);
                    } else {
                        item.copiesDecimal =
                                item.copiesDecimal.add(printedCopies);
                    }
                }

                if (StringUtils.isBlank(trx.getExtDetails())) {

                    if (isRefund) {
                        totIndividualDelegatorsRefund++;
                        totIndividualDelegatorsCopiesRefund =
                                totIndividualDelegatorsCopiesRefund
                                        .add(printedCopies);
                        totIndividualCostRefund =
                                totIndividualCostRefund.add(trx.getAmount());
                    } else {
                        totIndividualDelegators++;
                        totIndividualDelegatorsCopies =
                                totIndividualDelegatorsCopies
                                        .add(printedCopies);
                        totIndividualCost =
                                totIndividualCost.add(trx.getAmount());
                        item.sourceGroups++;
                    }
                } else {
                    if (!isRefund) {
                        item.sourcePersonal++;
                    }
                }

                if (!editCopies
                        || StringUtils.isNotBlank(trx.getExtDetails())) {
                    continue;
                }
            }

            // Account summary line.
            final TrxLine trxLine = new TrxLine();

            if (accountType == AccountTypeEnum.GROUP) {
                if (accountNameDelegatorCount.containsKey(account.getName())) {
                    trxLine.delegators = accountNameDelegatorCount
                            .get(account.getName()).toString();
                } else {
                    trxLine.delegators = TrxLine.NO_DELEGATORS;
                }
            } else {
                trxLine.delegators = TrxLine.NO_DELEGATORS;
            }

            trxLine.imgSrcChoice = MarkupHelper.getImgUrlPath(accountType);

            final Account accountParent;

            if (account == null) {
                accountParent = null;
            } else {
                trxLine.accountID = account.getId();
                trxLine.accountName = account.getName();
                accountParent = account.getParent();
            }

            if (accountParent == null) {
                trxLine.accountNameDisplay = accountNameWlk;
            } else {
                trxLine.accountNameDisplay = String.format("%s \\ %s",
                        accountParent.getName(), account.getName());
            }

            if (costPerCopy.compareTo(BigDecimal.ZERO) == 0) {
                trxLine.copies = TrxLine.NO_COPIES;
            } else {
                trxLine.copies = ACCOUNTING_SERVICE
                        .calcPrintedCopies(trx.getAmount().negate(),
                                costPerCopy, 0)
                        .toPlainString();
            }

            if (isRefund) {
                trxLine.remark =
                        NounEnum.REFUND.uiText(getLocale()).toLowerCase();
            }

            final StringBuilder sbAccTrx = new StringBuilder();

            sbAccTrx.append(currencySymbolWlk).append("&nbsp;")
                    .append(BigDecimalUtil.localizeUc(trx.getAmount(),
                            currencyDecimals, getSession().getLocale(), true));

            trxLine.formattedCost = sbAccTrx.toString();
            displayTrxLines.add(trxLine);
        }

        // Summary line for personally charged delegators.
        if (!editCopies && totIndividualDelegators > 0) {

            final TrxLine values = getPersonCatOptions(
                    PrintOutAdjectiveEnum.PERSONAL.uiText(getLocale()),
                    totIndividualDelegators, totIndividualDelegatorsCopies,
                    totIndividualCost, currencySymbolWlk, currencyDecimals,
                    displayTrxLines.size() > 1 || totIndividualDelegators > 1);

            displayTrxLines.add(values);
        }

        // Summary line for personally refunded delegators.
        if (totIndividualDelegatorsRefund > 0) {

            final TrxLine values = getPersonCatOptions(
                    PrintOutAdjectiveEnum.PERSONAL.uiText(getLocale()),
                    totIndividualDelegatorsRefund,
                    totIndividualDelegatorsCopiesRefund,
                    totIndividualCostRefund, currencySymbolWlk,
                    currencyDecimals, displayTrxLines.size() > 1
                            || totIndividualDelegatorsRefund > 1);

            displayTrxLines.add(values);
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
     * @param totDelegatorsCopies
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
            final int totDelegators, final BigDecimal totDelegatorsCopies,
            final BigDecimal totCost, final String currencySymbolWlk,
            final int currencyDecimals, final boolean showTotDelegators) {

        final TrxLine trxLine = new TrxLine();

        trxLine.accountNameDisplay = uiName;
        if (showTotDelegators) {
            trxLine.delegators = String.valueOf(totDelegators);
        } else {
            trxLine.delegators = "";
        }

        trxLine.imgSrcChoice = MarkupHelper.getImgUrlPath(AccountTypeEnum.USER);

        trxLine.formattedCost = String.format("%s&nbsp;%s", currencySymbolWlk,
                BigDecimalUtil.localizeUc(totCost, currencyDecimals,
                        getSession().getLocale(), true));

        trxLine.copies = totDelegatorsCopies.setScale(0, RoundingMode.HALF_EVEN)
                .toPlainString();

        if (totCost.compareTo(BigDecimal.ZERO) == 1) {
            trxLine.remark = NounEnum.REFUND.uiText(getLocale()).toLowerCase();
        }

        return trxLine;
    }

}
