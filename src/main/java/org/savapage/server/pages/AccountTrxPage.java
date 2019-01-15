/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.AccountTrxDao.ListFilter;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.AccountTrxTypeEnum;
import org.savapage.core.dao.enums.DaoEnumHelper;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.dao.helpers.AccountTrxPagerReq;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrintSupplierData;
import org.savapage.core.services.helpers.account.UserAccountContextFactory;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.BitcoinUtil;
import org.savapage.core.util.CurrencyUtil;
import org.savapage.core.util.LocaleHelper;
import org.savapage.ext.payment.PaymentMethodEnum;
import org.savapage.server.WebApp;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 */
public final class AccountTrxPage extends AbstractListPage {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AccountTrxPage.class);

    /**
     * Maximum number of pages in the navigation bar. IMPORTANT: this must be an
     * ODD number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private final class AccountTrxListView
            extends PropertyListView<AccountTrx> {

        private static final long serialVersionUID = 1L;

        /** */
        private static final String WID_BALANCE_SAVAPAGE_ICON =
                "balance-savapage-icon";

        /** */
        private final int balanceDecimals;
        /** */
        private final String bitcoinUrlPatternTrx;

        /** */
        @SuppressWarnings("unused")
        private final String bitcoinUrlPatternAddr;

        /** */
        private final boolean hasPaperCutUserAccountView;

        /**
         *
         * @param id
         * @param entryList
         */
        public AccountTrxListView(final String id,
                final List<AccountTrx> entryList) {

            super(id, entryList);

            final ConfigManager cm = ConfigManager.instance();

            this.balanceDecimals = ConfigManager.getUserBalanceDecimals();

            this.bitcoinUrlPatternTrx = cm.getConfigValue(
                    Key.FINANCIAL_BITCOIN_USER_PAGE_URL_PATTERN_TRX);

            this.bitcoinUrlPatternAddr = cm.getConfigValue(
                    Key.FINANCIAL_BITCOIN_USER_PAGE_URL_PATTERN_ADDRESS);

            this.hasPaperCutUserAccountView =
                    UserAccountContextFactory.hasContextPaperCut();
        }

        /**
         *
         * @param accountTrx
         * @return
         */
        private User getUserSubject(final AccountTrx trx) {

            final List<UserAccount> members = trx.getAccount().getMembers();

            if (members == null || members.isEmpty()) {
                return null;
            }
            return members.get(0).getUser();
        }

        @Override
        protected void populateItem(final ListItem<AccountTrx> item) {

            //
            final MarkupHelper helper = new MarkupHelper(item);

            final AccountTrx accountTrx = item.getModelObject();
            final DocLog docLog = accountTrx.getDocLog();

            final PrintOut printOut;

            if (docLog != null && docLog.getDocOut() != null) {
                printOut = docLog.getDocOut().getPrintOut();
            } else {
                printOut = null;
            }

            final AccountTrxTypeEnum trxType =
                    AccountTrxTypeEnum.valueOf(accountTrx.getTrxType());

            //
            item.add(new Label("trxDate",
                    localizedShortDateTime(accountTrx.getTransactionDate())));
            //
            item.add(new Label("trxActor", accountTrx.getTransactedBy()));

            if (this.hasPaperCutUserAccountView) {
                helper.addTransparant(WID_BALANCE_SAVAPAGE_ICON)
                        .add(new AttributeModifier(MarkupHelper.ATTR_SRC,
                                WebApp.getExtSupplierEnumImgUrl(
                                        ExternalSupplierEnum.SAVAPAGE)));
            } else {
                helper.discloseLabel(WID_BALANCE_SAVAPAGE_ICON);
            }

            //
            final String currencySymbol = String.format("%s ",
                    CurrencyUtil.getCurrencySymbol(accountTrx.getCurrencyCode(),
                            getSession().getLocale()));

            final String amount;
            final String balance;

            try {
                amount = BigDecimalUtil.localizeMinimalPrecision(
                        accountTrx.getAmount(), this.balanceDecimals,
                        getSession().getLocale(), currencySymbol, true);

                balance = BigDecimalUtil.localize(accountTrx.getBalance(),
                        this.balanceDecimals, getSession().getLocale(),
                        currencySymbol, true);

            } catch (ParseException e) {
                throw new SpException(e);
            }

            helper.encloseLabel("amount-refund",
                    NounEnum.REFUND.uiText(getLocale()).toLowerCase(),
                    printOut != null && accountTrx.getAmount()
                            .compareTo(BigDecimal.ZERO) > 0);

            Label labelWrk;

            labelWrk = new Label("balance", balance);
            if (accountTrx.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS,
                        MarkupHelper.CSS_AMOUNT_MIN));
            }
            item.add(labelWrk);

            if (trxType == AccountTrxTypeEnum.INITIAL) {
                helper.discloseLabel("amount");
            } else {
                labelWrk = helper.encloseLabel("amount", amount, true);
                if (accountTrx.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CLASS,
                            MarkupHelper.CSS_AMOUNT_MIN));
                }
            }

            //
            PaymentMethodEnum extPaymentMethod = null;

            if (accountTrx.getExtMethod() != null) {
                try {
                    extPaymentMethod = PaymentMethodEnum
                            .valueOf(accountTrx.getExtMethod());
                } catch (Exception e) {
                    // noop
                }
            }

            //
            //
            final boolean showDocLogTitle = ConfigManager.instance()
                    .isConfigValue(Key.WEBAPP_DOCLOG_SHOW_DOC_TITLE);

            StringBuilder printOutInfo = new StringBuilder();
            String comment = accountTrx.getComment();
            String imageSrc = null;
            String delegate = null;
            String jobticket = null;

            //
            switch (trxType) {

            case GATEWAY:
                if (extPaymentMethod != null) {
                    imageSrc = WebApp.getPaymentMethodImgUrl(extPaymentMethod,
                            false);
                }
                break;

            case PRINT_IN:
                /*
                 * PRINT_IN account transactions are tied to a DocLog/PrintOut
                 * when the print was released by an external system (PaperCut).
                 *
                 * PRINT_IN account transactions are tied to a DocLog/PrintIn
                 * when the print was is pending (on hold) in an external system
                 * (PaperCut).
                 */
                final StringBuilder cmt = new StringBuilder();

                if (printOut != null) {
                    cmt.append(printOut.getPrinter().getDisplayName());
                }

                if (showDocLogTitle) {
                    if (printOut != null) {
                        cmt.append(" ");
                    }
                    cmt.append("(").append(docLog.getTitle()).append(")");
                }

                int totalPagesIn = docLog.getNumberOfPages().intValue()
                        * accountTrx.getTransactionWeight().intValue();

                printOutInfo.append(String.format("%d %s", totalPagesIn,
                        PrintOutNounEnum.PAGE.uiText(getLocale(),
                                totalPagesIn > 1)));

                helper.encloseLabel("printOutCopies", "", false);
                helper.encloseLabel("printOutSheets", "", false);

                comment = cmt.toString();

                break;

            case PRINT_OUT:

                if (printOut == null) {
                    throw new SpException("TrxType [" + trxType
                            + "] : unhandled cost source");
                }

                final int totalPagesOut = docLog.getNumberOfPages().intValue();

                printOutInfo.append(String.format("%d %s", totalPagesOut,
                        PrintOutNounEnum.PAGE.uiText(getLocale(),
                                totalPagesOut > 1)));

                final BigDecimal costPerCopy = ACCOUNTING_SERVICE
                        .calcCostPerPrintedCopy(docLog.getCostOriginal(),
                                printOut.getNumberOfCopies());

                final LocaleHelper localeHelper = new LocaleHelper(getLocale());

                final BigDecimal printedCopies;
                if (costPerCopy.compareTo(BigDecimal.ZERO) == 0) {
                    printedCopies = BigDecimal.ZERO;
                } else {
                    printedCopies = ACCOUNTING_SERVICE.calcPrintedCopies(
                            accountTrx.getAmount(), costPerCopy, 2).abs();
                }

                final int nCopies = printedCopies
                        .setScale(0, RoundingMode.HALF_UP).intValue();

                //
                final int nSheets = printOut.getNumberOfSheets().intValue()
                        / printOut.getNumberOfCopies().intValue();

                helper.encloseLabel("printOutCopies", String.format("%s %s",
                        localeHelper.asExactIntegerOrScaled(printedCopies),
                        PrintOutNounEnum.COPY.uiText(getLocale(), nCopies > 1)),
                        true);

                helper.encloseLabel(
                        "printOutSheets", String
                                .format("%d %s", nSheets,
                                        PrintOutNounEnum.SHEET.uiText(
                                                getLocale(), nSheets > 1)),
                        true);
                //
                comment = printOut.getPrinter().getDisplayName();

                if (showDocLogTitle
                        && StringUtils.isNotBlank(docLog.getTitle())) {
                    comment += String.format(" (%s)", docLog.getTitle());
                }

                final AccountTypeEnum accountType = AccountTypeEnum
                        .valueOf(accountTrx.getAccount().getAccountType());

                final User userActor = docLog.getUser();

                if (accountType == AccountTypeEnum.SHARED
                        || accountType == AccountTypeEnum.GROUP) {
                    delegate = localized("by-user", userActor.getUserId());
                } else {
                    final User userSubject = this.getUserSubject(accountTrx);
                    if (userSubject != null
                            && !userSubject.getId().equals(userActor.getId())) {
                        delegate =
                                localized("by-delegate", userActor.getUserId());
                    }
                }

                final PrintModeEnum printMode =
                        DaoEnumHelper.getPrintMode(printOut);

                if (printMode == PrintModeEnum.TICKET
                        || printMode == PrintModeEnum.TICKET_C
                        || printMode == PrintModeEnum.TICKET_E) {

                    final String operator;

                    if (docLog.getExternalData() == null) {
                        operator = null;

                    } else {
                        final PrintSupplierData supplierData = PrintSupplierData
                                .createFromData(docLog.getExternalData());
                        operator = supplierData.getOperator();
                    }
                    jobticket = String.format("%s %s (%s)",
                            printMode.uiText(getLocale()),
                            StringUtils.defaultString(docLog.getExternalId(),
                                    "???"),
                            StringUtils.defaultString(operator, "???"));
                } else if (StringUtils.isNotBlank(docLog.getExternalId())) {
                    jobticket = docLog.getExternalId();
                }
                break;

            case INITIAL:
            case ADJUST:
            case DEPOSIT:
            case TRANSFER:
            case VOUCHER:
                break;

            default:
                throw new SpException(
                        "TrxType [" + trxType + "] unknown: not handled");
            }

            //
            helper.encloseLabel("delegate", delegate, delegate != null);
            helper.encloseLabel("jobticket", jobticket, jobticket != null);

            final boolean isExtBitcoin =
                    StringUtils.isNotBlank(accountTrx.getExtCurrencyCode())
                            && accountTrx.getExtCurrencyCode()
                                    .equals(CurrencyUtil.CURRENCY_CODE_BITCOIN);

            // External supplier
            final ExternalSupplierEnum extSupplierEnum =
                    DaoEnumHelper.getExtSupplier(docLog);

            final String extSupplier;

            if (extSupplierEnum == null) {
                extSupplier = null;
            } else {
                extSupplier = extSupplierEnum.getUiText();
                imageSrc = WebApp.getExtSupplierEnumImgUrl(extSupplierEnum);
            }

            // External supplier status
            final ExternalSupplierStatusEnum extSupplierStatus =
                    DaoEnumHelper.getExtSupplierStatus(docLog);

            if (StringUtils.isBlank(imageSrc)) {
                helper.discloseLabel("trxImage");
            } else {
                labelWrk =
                        MarkupHelper.createEncloseLabel("trxImage", "", true);
                labelWrk.add(
                        new AttributeModifier(MarkupHelper.ATTR_SRC, imageSrc));
                item.add(labelWrk);
            }

            //
            final boolean isGatewayPaymentMethodInHeader =
                    trxType == AccountTrxTypeEnum.GATEWAY
                            && extPaymentMethod != null;

            if (isGatewayPaymentMethodInHeader) {
                item.add(new Label("trxType",
                        extPaymentMethod.toString().toLowerCase()));
            } else {
                item.add(new Label("trxType", trxType.uiText(getLocale())));
            }

            //
            helper.encloseLabel("extSupplier", extSupplier,
                    extSupplier != null);

            if (extSupplierStatus == null) {
                helper.discloseLabel("extStatus");
            } else {
                helper.encloseLabel("extStatus",
                        extSupplierStatus.uiText(getLocale()), true)
                        .add(new AttributeAppender("class", MarkupHelper
                                .getCssTxtClass(extSupplierStatus)));
            }

            //
            final boolean isVisible = accountTrx.getPosPurchase() != null;

            if (isVisible) {

                helper.encloseLabel("receiptNumber",
                        accountTrx.getPosPurchase().getReceiptNumber(),
                        isVisible);

                if (StringUtils.isNotBlank(
                        accountTrx.getPosPurchase().getPaymentType())) {
                    helper.encloseLabel("paymentMethod",
                            accountTrx.getPosPurchase().getPaymentType(),
                            isVisible);
                } else {
                    helper.discloseLabel("paymentMethod");
                }

                if (trxType == AccountTrxTypeEnum.DEPOSIT) {

                    helper.encloseLabel("downloadReceipt",
                            localized("button-receipt"), isVisible);

                    labelWrk = (Label) item.get("downloadReceipt");
                    labelWrk.add(new AttributeModifier(
                            MarkupHelper.ATTR_DATA_SAVAPAGE,
                            accountTrx.getId().toString()));
                }

            } else {
                helper.discloseLabel("receiptNumber");
                helper.encloseLabel("paymentMethod", accountTrx.getExtMethod(),
                        !isGatewayPaymentMethodInHeader && StringUtils
                                .isNotBlank(accountTrx.getExtMethod()));
            }

            if (isExtBitcoin) {

                helper.discloseLabel("bitcoinAddress"); // not now
                helper.discloseLabel("paymentMethodAddress");

                if (StringUtils.isBlank(this.bitcoinUrlPatternTrx)) {

                    helper.discloseLabel("bitcoinTrxHash");

                    helper.encloseLabel("extId", accountTrx.getExtId(), true);

                } else {
                    helper.discloseLabel("extId");

                    helper.addModifyLabelAttr("bitcoinTrxHash",
                            StringUtils.abbreviateMiddle(accountTrx.getExtId(),
                                    "...", 13),
                            "href",
                            MessageFormat.format(this.bitcoinUrlPatternTrx,
                                    accountTrx.getExtId()));

                }

            } else {
                helper.encloseLabel("paymentMethodAddress",
                        accountTrx.getExtMethodAddress(), StringUtils
                                .isNotBlank(accountTrx.getExtMethodAddress()));

                helper.encloseLabel("extId", accountTrx.getExtId(),
                        StringUtils.isNotBlank(accountTrx.getExtId()));

                helper.discloseLabel("bitcoinAddress");
                helper.discloseLabel("bitcoinTrxHash");
            }

            if (!isVisible || trxType != AccountTrxTypeEnum.DEPOSIT) {
                helper.discloseLabel("downloadReceipt");
            }

            //
            item.add(createVisibleLabel(StringUtils.isNotBlank(comment),
                    "comment", comment));
            //
            item.add(createVisibleLabel(printOutInfo.length() > 0, "printOut",
                    printOutInfo.toString()));

            if (printOut == null) {
                helper.discloseLabel("printout-options");
            } else {
                final PrintOutOptionsPanel panel =
                        new PrintOutOptionsPanel("printout-options");
                panel.populate(printOut);
                item.add(panel);
            }

            if (trxType == AccountTrxTypeEnum.GATEWAY
                    && accountTrx.getExtAmount() != null) {

                final StringBuilder ext = new StringBuilder();

                try {

                    final boolean isPending = accountTrx.getAmount()
                            .compareTo(BigDecimal.ZERO) == 0;

                    ext.append(CurrencyUtil.getCurrencySymbol(
                            accountTrx.getExtCurrencyCode(),
                            getSession().getLocale())).append(" ");

                    if (isExtBitcoin) {
                        ext.append(BigDecimalUtil.localize(
                                accountTrx.getExtAmount(),
                                BitcoinUtil.BTC_DECIMALS,
                                getSession().getLocale(), true));
                    } else {
                        ext.append(BigDecimalUtil.localize(
                                accountTrx.getExtAmount(), this.balanceDecimals,
                                getSession().getLocale(), "", true));
                    }

                    if (accountTrx.getExtFee() != null && accountTrx.getExtFee()
                            .compareTo(BigDecimal.ZERO) != 0) {

                        ext.append(" -/- ");

                        if (isExtBitcoin) {
                            ext.append(BigDecimalUtil.localize(
                                    accountTrx.getExtFee(),
                                    BitcoinUtil.BTC_DECIMALS,
                                    getSession().getLocale(), true));
                        } else {
                            ext.append(BigDecimalUtil.localize(
                                    accountTrx.getExtFee(),
                                    this.balanceDecimals,
                                    getSession().getLocale(), "", true));
                        }
                    }

                    if (isPending) {
                        ext.append(" Confirmations (")
                                .append(accountTrx.getExtConfirmations())
                                .append(")");
                    }

                } catch (ParseException e) {
                    throw new SpException(e);
                }

                helper.encloseLabel("extAmount", ext.toString(), true);
            } else {
                helper.discloseLabel("extAmount");
            }

            helper.encloseLabel("extDetails", accountTrx.getExtDetails(),
                    StringUtils.isNotBlank(accountTrx.getExtDetails()));
        }
    }

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * @param parameters
     *            The page parameters.
     */
    public AccountTrxPage(final PageParameters parameters) {

        super(parameters);

        //
        final String data = getParmValue(POST_PARM_DATA);
        final AccountTrxPagerReq req = AccountTrxPagerReq.read(data);

        //
        final ListFilter filter = new ListFilter();

        // filter.setAccountType(AccountTypeEnum.USER); // for now ...

        filter.setTrxType(req.getSelect().getTrxType());
        filter.setDateFrom(req.getSelect().dateFrom());
        filter.setDateTo(req.getSelect().dateTo());
        filter.setContainingCommentText(req.getSelect().getContainingText());

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("trxType [" + filter.getTrxType() + "] dateFrom ["
                    + filter.getDateFrom() + "] dateTo [" + filter.getDateTo()
                    + "] text [" + filter.getContainingCommentText() + "]");
        }

        Long userId = null;
        Long accountId = null;

        if (this.getSessionWebAppType() == WebAppTypeEnum.ADMIN) {

            this.probePermissionToRead(ACLOidEnum.A_TRANSACTIONS);

            userId = req.getSelect().getUserId();
            accountId = req.getSelect().getAccountId();

        } else if (this.getSessionWebAppType() == WebAppTypeEnum.PRINTSITE) {

            userId = req.getSelect().getUserId();

        } else {
            /*
             * If we are called in a User WebApp context we ALWAYS use the user
             * of the current session.
             */
            userId = SpSession.get().getUser().getId();
        }

        filter.setUserId(userId);
        filter.setAccountId(accountId);

        //
        final AccountTrxDao accountTrxDao =
                ServiceContext.getDaoContext().getAccountTrxDao();

        final long logCount = accountTrxDao.getListCount(filter);

        /*
         * Display the requested page.
         */
        final List<AccountTrx> entryList = accountTrxDao.getListChunk(filter,
                req.calcStartPosition(), req.getMaxResults(),
                req.getSort().getField(), req.getSort().getAscending());

        add(new AccountTrxListView("log-entry-view", entryList));

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, logCount, MAX_PAGES_IN_NAVBAR,
                "sp-accounttrx-page",
                new String[] { "nav-bar-1", "nav-bar-2" });
    }
}
