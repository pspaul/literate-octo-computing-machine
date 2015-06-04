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
package org.savapage.server.pages.user;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dto.AccountDisplayInfoDto;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.payment.PaymentGateway;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.ext.payment.PaymentMethodInfo;
import org.savapage.server.SpSession;
import org.savapage.server.WebApp;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.StatsEnvImpactPanel;
import org.savapage.server.pages.StatsPageTotalPanel;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class UserDashboard extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public UserDashboard() {

        // this.openServiceContext();
        handlePage();
    }

    /**
     *
     * @param em
     * @throws ParseException
     */
    private void handlePage() {

        final MarkupHelper helper = new MarkupHelper(this);

        final SpSession session = SpSession.get();

        final org.savapage.core.jpa.User user =
                ServiceContext.getDaoContext().getUserDao()
                        .findById(session.getUser().getId());

        /*
         * Page Totals.
         */
        final StatsPageTotalPanel pageTotalPanel =
                new StatsPageTotalPanel("stats-pages-total");
        add(pageTotalPanel);
        pageTotalPanel.populate();

        /*
         * Environmental Impact.
         */
        final Double esu = (double) (user.getNumberOfPrintOutEsu() / 100);
        final StatsEnvImpactPanel envImpactPanel =
                new StatsEnvImpactPanel("environmental-impact");
        add(envImpactPanel);
        envImpactPanel.populate(esu);

        /*
         * Accounting.
         */
        final AccountDisplayInfoDto dto =
                ServiceContext
                        .getServiceFactory()
                        .getAccountingService()
                        .getAccountDisplayInfo(user,
                                ServiceContext.getLocale(),
                                SpSession.getAppCurrencySymbol());

        // ------------------
        String creditLimit = dto.getCreditLimit();

        if (StringUtils.isBlank(creditLimit)) {
            creditLimit = helper.localized("credit-limit-none");
        }
        helper.addModifyLabelAttr("credit-limit", creditLimit, "class",
                MarkupHelper.CSS_TXT_INFO);

        // ------------------
        final String clazzBalance;

        switch (dto.getStatus()) {
        case CREDIT:
            clazzBalance = MarkupHelper.CSS_TXT_WARN;
            break;
        case DEBIT:
            clazzBalance = MarkupHelper.CSS_TXT_VALID;
            break;
        case OVERDRAFT:
            clazzBalance = MarkupHelper.CSS_TXT_ERROR;
            break;
        default:
            throw new SpException("Status [" + dto.getStatus()
                    + "] not handled.");
        }

        helper.addModifyLabelAttr("balance", dto.getBalance(), "class",
                clazzBalance);

        //
        helper.addModifyLabelAttr("button-voucher-redeem",
                localized("button-voucher"), "title",
                localized("button-title-voucher"));

        helper.addModifyLabelAttr("button-assign-credit",
                localized("button-assign"), "title",
                localized("button-title-assign"));

        /*
         * Payment Gateways
         */
        final String appCurrencyCode = ConfigManager.getAppCurrencyCode();

        final ServerPluginManager pluginMgr = WebApp.get().getPluginManager();

        /*
         * Bitcoin Gateway?
         */
        final PaymentGatewayPlugin bitcoinPlugin =
                pluginMgr.getBitcoinGateway();

        final boolean isBitcoinGateway =
                bitcoinPlugin != null
                        && bitcoinPlugin.isCurrencySupported(appCurrencyCode);

        if (isBitcoinGateway) {

            final Label labelWrk = new Label("button-transfer-bitcoin");
            labelWrk.add(new AttributeModifier("title",
                    localized("button-title-bitcoin")));
            add(labelWrk);

        } else {
            helper.discloseLabel("button-transfer-bitcoin");
        }

        /*
         * External Gateway?
         */
        final PaymentGateway externalPlugin =
                pluginMgr.getExternalPaymentGateway();

        final boolean isExternalGateway =
                externalPlugin != null
                        && externalPlugin.isCurrencySupported(appCurrencyCode);

        //
        if (!isExternalGateway) {
            helper.discloseLabel("button-transfer-money");
            return;
        }

        final List<PaymentMethodInfo> list =
                new ArrayList<PaymentMethodInfo>(externalPlugin
                        .getExternalPaymentMethods().values());

        add(new PropertyListView<PaymentMethodInfo>("money-transfer-methods",
                list) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<PaymentMethodInfo> item) {

                final PaymentMethodInfo info = item.getModelObject();

                final Label labelWrk = new Label("money-transfer-method", "");

                labelWrk.add(new AttributeModifier("src", info
                        .getUrlImageNormal().toExternalForm()));

                labelWrk.add(new AttributeModifier("data-payment-gateway",
                        externalPlugin.getId()));

                labelWrk.add(new AttributeModifier("data-payment-method",
                        info.getMethod().toString()));

                item.add(labelWrk);
            }

        });

    }
}
