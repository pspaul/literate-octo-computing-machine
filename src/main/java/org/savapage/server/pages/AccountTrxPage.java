/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.server.pages;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.AccountTrxDao.ListFilter;
import org.savapage.core.dao.helpers.AccountTrxPagerReq;
import org.savapage.core.dao.helpers.AccountTrxTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.server.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 */
public class AccountTrxPage extends AbstractListPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AccountTrxPage.class);

    /**
     * Maximum number of pages in the navigation bar. IMPORTANT: this must be an
     * ODD number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    @Override
    protected final boolean needMembership() {
        return isAdminRoleContext();
    }

    /**
     *
     */
    public AccountTrxPage() {

        if (isAuthErrorHandled()) {
            return;
        }

        // this.openServiceContext();

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

        /*
         * isAdminWebAppContext() sometimes returns null. Why !?
         */
        final boolean adminWebApp = isAdminRoleContext();

        if (adminWebApp) {
            userId = req.getSelect().getUserId();
        } else {
            /*
             * If we are called in a User WebApp context we ALWAYS use the user
             * of the current session.
             */
            userId = SpSession.get().getUser().getId();
        }

        filter.setUserId(userId);

        //
        final AccountTrxDao accountTrxDao =
                ServiceContext.getDaoContext().getAccountTrxDao();

        final int balanceDecimals = ConfigManager.getUserBalanceDecimals();

        final long logCount = accountTrxDao.getListCount(filter);

        /*
         * Display the requested page.
         */
        final List<AccountTrx> entryList =
                accountTrxDao.getListChunk(filter, req.calcStartPosition(), req
                        .getMaxResults(), req.getSort().getField(), req
                        .getSort().getAscending());

        add(new PropertyListView<AccountTrx>("log-entry-view", entryList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<AccountTrx> item) {

                final AccountTrx accountTrx = item.getModelObject();
                final DocLog docLog = accountTrx.getDocLog();

                final PrintOut printOut;

                if (docLog != null && docLog.getDocOut() != null) {
                    printOut = docLog.getDocOut().getPrintOut();
                } else {
                    printOut = null;
                }

                final PrintIn printIn;

                if (docLog != null && docLog.getDocIn() != null) {
                    printIn = docLog.getDocIn().getPrintIn();
                } else {
                    printIn = null;
                }

                //
                item.add(new Label("trxDate", localizedShortDateTime(accountTrx
                        .getTransactionDate())));
                //
                item.add(new Label("trxActor", accountTrx.getTransactedBy()));

                //
                final String currencySymbol = SpSession.getCurrencySymbol();

                final String amount;
                final String balance;

                try {
                    amount =
                            BigDecimalUtil.localize(accountTrx.getAmount(),
                                    balanceDecimals, getSession().getLocale(),
                                    currencySymbol, true);

                    balance =
                            BigDecimalUtil.localize(accountTrx.getBalance(),
                                    balanceDecimals,
                                    ServiceContext.getLocale(), currencySymbol,
                                    true);

                } catch (ParseException e) {
                    throw new SpException(e);
                }

                Label labelWrk;

                labelWrk = new Label("amount", amount);
                if (accountTrx.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    labelWrk.add(new AttributeModifier("class",
                            MarkupHelper.CSS_AMOUNT_MIN));
                }
                item.add(labelWrk);

                labelWrk = new Label("balance", balance);
                if (accountTrx.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                    labelWrk.add(new AttributeModifier("class",
                            MarkupHelper.CSS_AMOUNT_MIN));
                }
                item.add(labelWrk);

                //
                String printOutInfo = null;
                String comment = accountTrx.getComment();

                //
                String msgKey;

                String key;

                AccountTrxTypeEnum trxType =
                        AccountTrxTypeEnum.valueOf(accountTrx.getTrxType());

                switch (trxType) {

                case ADJUST:
                    key = "type-adjust";
                    break;

                case DEPOSIT:
                    key = "type-deposit";
                    break;

                case GATEWAY:
                    key = "type-gateway";
                    break;

                case VOUCHER:
                    key = "type-voucher";
                    break;

                case PRINT_IN:

                    key = "type-print-in";

                    final StringBuilder cmt = new StringBuilder();

                    cmt.append("(").append(docLog.getTitle()).append(")");

                    if (docLog.getNumberOfPages() == 1) {
                        msgKey = "printed-pages-one";
                    } else {
                        msgKey = "printed-pages-multiple";
                    }

                    printOutInfo =
                            localized(msgKey, docLog.getNumberOfPages()
                                    .toString());

                    comment = cmt.toString();

                    break;

                case PRINT_OUT:

                    if (printOut == null) {
                        throw new SpException("TrxType [" + trxType
                                + "] : unhandled cost source");
                    }

                    key = "type-print-out";

                    if (docLog.getNumberOfPages() == 1) {
                        msgKey = "printed-pages-one";
                    } else {
                        msgKey = "printed-pages-multiple";
                    }

                    printOutInfo =
                            localized(msgKey, docLog.getNumberOfPages()
                                    .toString());

                    comment = printOut.getPrinter().getDisplayName();

                    if (StringUtils.isNotBlank(docLog.getTitle())) {
                        comment += " (" + docLog.getTitle() + ")";
                    }

                    break;

                default:
                    throw new SpException("TrxType [" + trxType
                            + "] unknown: not handled");
                }

                //
                item.add(new Label("trxType", localized(key)));

                //
                final MarkupHelper helper = new MarkupHelper(item);

                final boolean isVisible = accountTrx.getPosPurchase() != null;

                if (isVisible) {

                    helper.encloseLabel("receiptNumber", accountTrx
                            .getPosPurchase().getReceiptNumber(), isVisible);

                    if (StringUtils.isNotBlank(accountTrx.getPosPurchase()
                            .getPaymentType())) {
                        helper.encloseLabel("paymentType", accountTrx
                                .getPosPurchase().getPaymentType(), isVisible);
                    } else {
                        helper.discloseLabel("paymentType");
                    }

                    if (trxType == AccountTrxTypeEnum.DEPOSIT) {

                        helper.encloseLabel("downloadReceipt",
                                localized("button-receipt"), isVisible);

                        labelWrk = (Label) item.get("downloadReceipt");
                        labelWrk.add(new AttributeModifier("data-savapage",
                                accountTrx.getId().toString()));
                    }

                } else {
                    helper.discloseLabel("receiptNumber");
                    helper.discloseLabel("paymentType");
                }

                if (!isVisible || trxType != AccountTrxTypeEnum.DEPOSIT) {
                    helper.discloseLabel("downloadReceipt");
                }

                //
                item.add(createVisibleLabel(StringUtils.isNotBlank(comment),
                        "comment", comment));
                //
                item.add(createVisibleLabel(
                        StringUtils.isNotBlank(printOutInfo), "printOut",
                        printOutInfo));
            }
        });
        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, logCount, MAX_PAGES_IN_NAVBAR,
                "sp-accounttrx-page", new String[] { "nav-bar-1", "nav-bar-2" });
    }
}
