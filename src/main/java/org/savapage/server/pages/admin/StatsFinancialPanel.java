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
package org.savapage.server.pages.admin;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dto.FinancialDisplayInfoDto;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.BitcoinUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.bitcoin.BitcoinGateway;
import org.savapage.ext.payment.bitcoin.BitcoinWalletInfo;
import org.savapage.server.WebApp;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class StatsFinancialPanel extends Panel {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    private static final String VALUE_NOT_FOUND = "-";

    /**
     *
     * @param id
     *            The panel id.
     */
    public StatsFinancialPanel(final String id) {
        super(id);
    }

    /**
     *
     */
    public void populate() {

        final MarkupHelper helper = new MarkupHelper(this);

        final AccountingService svc =
                ServiceContext.getServiceFactory().getAccountingService();

        final FinancialDisplayInfoDto dto =
                svc.getFinancialDisplayInfo(getLocale(), null);

        final String appCurrencyCode = ConfigManager.getAppCurrencyCode();

        add(new Label("currency-code", appCurrencyCode));

        add(new Label("accounts-deb-cnt", dto.getUserDebit().getCount()));
        add(new Label("accounts-deb-min", dto.getUserDebit().getMin()));
        add(new Label("accounts-deb-max", dto.getUserDebit().getMax()));
        add(new Label("accounts-deb-sum", dto.getUserDebit().getSum()));
        add(new Label("accounts-deb-avg", dto.getUserDebit().getAvg()));

        add(new Label("accounts-crd-cnt", dto.getUserCredit().getCount()));
        add(new Label("accounts-crd-min", dto.getUserCredit().getMin()));
        add(new Label("accounts-crd-max", dto.getUserCredit().getMax()));
        add(new Label("accounts-crd-sum", dto.getUserCredit().getSum()));
        add(new Label("accounts-crd-avg", dto.getUserCredit().getAvg()));

        final ServerPluginManager pluginMgr = WebApp.get().getPluginManager();

        final BitcoinGateway plugin = pluginMgr.getBitcoinGateway();

        helper.encloseLabel("bitcoin-wallet-cur-code", appCurrencyCode,
                plugin != null);

        if (plugin == null) {
            helper.discloseLabel("bitcoin-wallet-msg");
            return;
        }

        final String walletHeaderTxt =
                this.getLocalizer().getString("bitcoin-wallet", this);

        final BitcoinWalletInfo wallet;

        try {

            wallet = pluginMgr.getWalletInfoCache(plugin, false);

            helper.discloseLabel("bitcoin-wallet-msg");

        } catch (PaymentGatewayException | IOException e) {

            helper.encloseLabel("bitcoin-wallet-cur", VALUE_NOT_FOUND, true);
            helper.encloseLabel("bitcoin-wallet-btc", VALUE_NOT_FOUND, true);
            helper.encloseLabel("bitcoin-wallet-datetime", VALUE_NOT_FOUND,
                    true);
            helper.encloseLabel("bitcoin-wallet-addr", VALUE_NOT_FOUND, true);
            helper.encloseLabel("bitcoin-wallet-addr-open", VALUE_NOT_FOUND,
                    true);
            helper.encloseLabel("bitcoin-wallet-addr-received", VALUE_NOT_FOUND,
                    true);
            helper.encloseLabel("bitcoin-wallet-msg", e.getMessage(), true);
            helper.addLabel("bitcoin-wallet-link", walletHeaderTxt);

            return;
        }

        final BigDecimal btc = BigDecimal.valueOf(wallet.getSatoshiBalance())
                .divide(BigDecimal.valueOf(BitcoinUtil.SATOSHIS_IN_BTC));

        final BigDecimal cur = btc
                .multiply(BigDecimal.valueOf(wallet.getBitcoinExchangeRate()));

        try {
            helper.encloseLabel("bitcoin-wallet-cur",
                    BigDecimalUtil.localize(cur,
                            ConfigManager.getUserBalanceDecimals(),
                            getSession().getLocale(), true),
                    true);
            helper.encloseLabel("bitcoin-wallet-btc", BigDecimalUtil
                    .localize(btc, 8, getSession().getLocale(), true), true);

        } catch (ParseException e) {
            throw new SpException(e.getMessage(), e);
        }

        helper.addLabel("bitcoin-wallet-datetime",
                DateUtil.formattedDateTime(wallet.getDate()));

        String count;

        //
        if (wallet.getAddressCount() == null) {
            count = VALUE_NOT_FOUND;
        } else {
            count = wallet.getAddressCount().toString();
        }
        helper.addLabel("bitcoin-wallet-addr", count);

        //
        if (wallet.getAddressCountOpen() == null) {
            count = VALUE_NOT_FOUND;
        } else {
            count = wallet.getAddressCountOpen().toString();
        }
        helper.addLabel("bitcoin-wallet-addr-open", count);

        //
        if (wallet.getAddressCountReceived() == null) {
            count = VALUE_NOT_FOUND;
        } else {
            count = wallet.getAddressCountReceived().toString();
        }
        helper.addLabel("bitcoin-wallet-addr-received", count);

        //
        final StringBuilder walletHeader = new StringBuilder(128);

        if (wallet.getWebPageUrl() == null) {
            walletHeader.append(walletHeaderTxt);
        } else {
            walletHeader.append("<a href=\"")
                    .append(wallet.getWebPageUrl().toString())
                    .append("\" target=\"_blank\">").append(walletHeaderTxt)
                    .append("</a>");
        }

        final Label walletLabel =
                new Label("bitcoin-wallet-link", walletHeader.toString());
        walletLabel.setEscapeModelStrings(false);

        add(walletLabel);

    }
}
