/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
import org.savapage.core.util.DateUtil;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.bitcoin.BitcoinGateway;
import org.savapage.ext.payment.bitcoin.BitcoinWalletInfo;
import org.savapage.server.WebApp;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class StatsFinancialPanel extends Panel {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

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

        final BitcoinWalletInfo wallet;

        try {

            wallet = pluginMgr.getWalletInfoCache(plugin, false);

            helper.discloseLabel("bitcoin-wallet-msg");

        } catch (PaymentGatewayException | IOException e) {

            helper.encloseLabel("bitcoin-wallet-cur", "-", true);
            helper.encloseLabel("bitcoin-wallet-btc", "-", true);
            helper.encloseLabel("bitcoin-wallet-msg", e.getMessage(), true);
            return;
        }

        final BigDecimal btc =
                BigDecimal.valueOf(wallet.getSatoshiBalance()).divide(
                        BigDecimal.valueOf(100000000));

        final BigDecimal cur =
                btc.multiply(BigDecimal.valueOf(wallet.getBitcoinExchangeRate()));

        try {
            helper.encloseLabel("bitcoin-wallet-cur", BigDecimalUtil.localize(
                    cur, ConfigManager.getUserBalanceDecimals(), getSession()
                            .getLocale(), true), true);
            helper.encloseLabel("bitcoin-wallet-btc", BigDecimalUtil.localize(
                    btc, 8, getSession().getLocale(), true), true);

        } catch (ParseException e) {
            throw new SpException(e.getMessage(), e);
        }

        helper.addLabel("bitcoin-wallet-datetime",
                DateUtil.formattedDateTime(wallet.getDate()));

    }
}
