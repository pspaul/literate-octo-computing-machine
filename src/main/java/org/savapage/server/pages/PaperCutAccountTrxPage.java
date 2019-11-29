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

import java.util.List;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.account.UserAccountContextEnum;
import org.savapage.ext.papercut.PaperCutAccountTrx;
import org.savapage.ext.papercut.PaperCutAccountTrxPagerReq;
import org.savapage.ext.papercut.PaperCutAccountTrxTypeEnum;
import org.savapage.ext.papercut.PaperCutDb;
import org.savapage.ext.papercut.services.PaperCutService;
import org.savapage.server.helpers.account.UserAccountContextHtmlFactory;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 */
public final class PaperCutAccountTrxPage extends AbstractListPage {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PaperCutAccountTrxPage.class);

    /** */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /** */
    private static final PaperCutService PAPERCUT_SERVICE =
            ServiceContext.getServiceFactory().getPaperCutService();

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
            extends PropertyListView<PaperCutAccountTrx> {

        /** */
        private static final long serialVersionUID = 1L;

        /** */
        private static final String WID_PAPERCUT_ICON = "papercut-icon";

        /**
         * The currency symbol of the current Locale.
         */
        private final String currencySymbol;

        /** */
        private final int balanceDecimals;

        /**
         *
         * @param id
         *            The Wicket id.
         * @param entryList
         *            The transaction list.
         */
        AccountTrxListView(final String id,
                final List<PaperCutAccountTrx> entryList) {

            super(id, entryList);

            this.balanceDecimals = ConfigManager.getUserBalanceDecimals();
            this.currencySymbol = SpSession.getAppCurrencySymbol();
        }

        @Override
        protected void populateItem(final ListItem<PaperCutAccountTrx> item) {

            final MarkupHelper helper = new MarkupHelper(item);
            final PaperCutAccountTrx trx = item.getModelObject();

            //
            final PaperCutAccountTrxTypeEnum trxType = EnumUtils.getEnum(
                    PaperCutAccountTrxTypeEnum.class, trx.getTransactionType());

            final String trxTypeUI;

            if (trxType == null) {
                trxTypeUI = StringUtils.defaultString(trx.getTransactionType(),
                        "\"?\"");
                LOGGER.warn("Unknown transaction type [{}]",
                        trx.getTransactionType());
            } else {
                trxTypeUI = trxType.uiText(getLocale());
            }
            helper.addLabel("trx-type", trxTypeUI);

            //
            helper.addLabel("amount",
                    ACCOUNTING_SERVICE.formatUserBalance(
                            PaperCutDb.getAmountBigBecimal(trx.getAmount(),
                                    balanceDecimals),
                            getLocale(), currencySymbol));

            helper.addLabel("balance",
                    ACCOUNTING_SERVICE.formatUserBalance(
                            PaperCutDb.getAmountBigBecimal(trx.getBalance(),
                                    balanceDecimals),
                            getLocale(), currencySymbol));

            helper.encloseLabel("document", trx.getDocumentName(),
                    StringUtils.isNotBlank(trx.getDocumentName()));
            helper.encloseLabel("comment", trx.getComment(),
                    StringUtils.isNotBlank(trx.getComment()));

            helper.addLabel("trxDate",
                    localizedShortDateTime(trx.getTransactionDate()));
            helper.addLabel("trxActor", trx.getTransactedBy());

            helper.addTransparant(WID_PAPERCUT_ICON)
                    .add(new AttributeModifier(MarkupHelper.ATTR_SRC,
                            UserAccountContextHtmlFactory
                                    .getContext(UserAccountContextEnum.PAPERCUT)
                                    .getImgUrl()));
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
    public PaperCutAccountTrxPage(final PageParameters parameters) {

        super(parameters);

        final String data = getParmValue(POST_PARM_DATA);
        final PaperCutAccountTrxPagerReq req =
                PaperCutAccountTrxPagerReq.read(data);

        final PaperCutDb.TrxFilter filter = new PaperCutDb.TrxFilter();

        final Long userId;

        if (this.getSessionWebAppType() == WebAppTypeEnum.ADMIN) {

            this.probePermissionToRead(ACLOidEnum.A_TRANSACTIONS);

            userId = req.getSelect().getUserId();

        } else if (this.getSessionWebAppType() == WebAppTypeEnum.PRINTSITE) {

            userId = req.getSelect().getUserId();

        } else {
            /*
             * If we are called in a User WebApp context we ALWAYS use the user
             * of the current session.
             */
            userId = SpSession.get().getUserDbKey();
        }

        final User userDb =
                ServiceContext.getDaoContext().getUserDao().findById(userId);

        filter.setUsername(userDb.getUserId());
        filter.setTrxType(req.getSelect().getTrxType());
        filter.setDateFrom(req.getSelect().dateFrom());
        filter.setDateTo(req.getSelect().dateTo());
        filter.setContainingCommentText(req.getSelect().getContainingText());

        final long logCount = PAPERCUT_SERVICE.getAccountTrxCount(filter);

        /*
         * Display the requested page.
         */
        final List<PaperCutAccountTrx> entryList =
                PAPERCUT_SERVICE.getAccountTrxListChunk(filter,
                        req.calcStartPosition(), req.getMaxResults(),
                        req.getSort().getField(), req.getSort().getAscending());

        add(new AccountTrxListView("log-entry-view", entryList));

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, logCount, MAX_PAGES_IN_NAVBAR,
                "sp-accounttrx-page-pc",
                new String[] { "nav-bar-1", "nav-bar-2" });
    }
}
