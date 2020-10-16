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
package org.savapage.server.pages.user;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dto.AccountDisplayInfoDto;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.payment.PaymentGateway;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.PaymentMethodEnum;
import org.savapage.ext.payment.PaymentMethodInfo;
import org.savapage.ext.payment.bitcoin.BitcoinGateway;
import org.savapage.server.WebApp;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;
import org.savapage.server.pages.StatsEnvImpactPanel;
import org.savapage.server.pages.StatsPageTotalPanel;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserDashboardAddIn extends AbstractUserPage {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final String WID_ENV_IMPACT = "environmental-impact";

    /**
     *
     */
    public UserDashboardAddIn(final PageParameters parameters) {

        super(parameters);
        handlePage();
    }

    /**
     *
     * @param em
     * @throws ParseException
     */
    private void handlePage() {

        final SpSession session = SpSession.get();

        final org.savapage.core.jpa.User user = ServiceContext.getDaoContext()
                .getUserDao().findById(session.getUserDbKey());

        final MarkupHelper helper = new MarkupHelper(this);

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
        if (ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_USER_SHOW_ENV_INFO)) {

            final Double esu = (double) (user.getNumberOfPrintOutEsu() / 100);
            final StatsEnvImpactPanel envImpactPanel =
                    new StatsEnvImpactPanel(WID_ENV_IMPACT);
            add(envImpactPanel);
            envImpactPanel.populate(esu);

        } else {
            helper.discloseLabel(WID_ENV_IMPACT);
        }

        /*
         * Financial.
         */

        final String keyTitleFinancial = "title-financial";

        final Integer financialPriv = ACCESS_CONTROL_SERVICE.getPrivileges(
                SpSession.get().getUserIdDto(), ACLOidEnum.U_FINANCIAL);

        if (financialPriv == null
                || ACLPermissionEnum.READER.isPresent(financialPriv)) {
            helper.addLabel(keyTitleFinancial, localized(keyTitleFinancial));
            showFinancialDetails(helper, user, financialPriv == null
                    || ACLPermissionEnum.EDITOR.isPresent(financialPriv));
        } else {
            helper.discloseLabel(keyTitleFinancial);
        }

    }

    /**
     *
     * @param helper
     *            The {@link MarkupHelper}.
     * @param user
     *            The requesting user.
     * @param allowFinancialTrx
     *            {@code true} when financial transactions are allowed.
     */
    private void showFinancialDetails(final MarkupHelper helper,
            final org.savapage.core.jpa.User user,
            final boolean allowFinancialTrx) {

        final ConfigManager cm = ConfigManager.instance();

        final AccountDisplayInfoDto dto =
                ServiceContext.getServiceFactory().getAccountingService()
                        .getAccountDisplayInfo(user, ServiceContext.getLocale(),
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
            throw new SpException(
                    "Status [" + dto.getStatus() + "] not handled.");
        }

        helper.addModifyLabelAttr("balance", dto.getBalance(), "class",
                clazzBalance);

        // Redeem voucher?
        final Label labelVoucherRedeem = MarkupHelper.createEncloseLabel(
                "button-voucher-redeem", localized("button-voucher"),
                allowFinancialTrx && cm
                        .isConfigValue(Key.FINANCIAL_USER_VOUCHERS_ENABLE));

        add(MarkupHelper.appendLabelAttr(labelVoucherRedeem, "title",
                localized("button-title-voucher")));

        // Credit transfer?
        final boolean enableTransferCredit = allowFinancialTrx
                && dto.getStatus() == AccountDisplayInfoDto.Status.DEBIT
                && cm.isConfigValue(Key.FINANCIAL_USER_TRANSFER_ENABLE);

        final Label labelTransferCredit = MarkupHelper.createEncloseLabel(
                "button-transfer-credit", localized("button-transfer-to-user"),
                enableTransferCredit);

        add(MarkupHelper.appendLabelAttr(labelTransferCredit, "title",
                localized("button-title-transfer-to-user")));

        /*
         * Payment Gateways
         */
        final String appCurrencyCode = ConfigManager.getAppCurrencyCode();

        final ServerPluginManager pluginMgr = WebApp.get().getPluginManager();

        int methodCount = 0;

        /*
         * Bitcoin Gateway?
         */
        final BitcoinGateway bitcoinPlugin = pluginMgr.getBitcoinGateway();

        final boolean isBitcoinGateway = allowFinancialTrx
                && bitcoinPlugin != null && bitcoinPlugin.isOnline()
                && bitcoinPlugin.isCurrencySupported(appCurrencyCode);

        if (isBitcoinGateway) {

            final Label labelWrk = new Label("img-transfer-bitcoin");

            labelWrk.add(new AttributeModifier("title",
                    localized("button-title-bitcoin")));

            labelWrk.add(new AttributeModifier("src", WebApp
                    .getPaymentMethodImgUrl(PaymentMethodEnum.BITCOIN, false)));

            add(labelWrk);

            methodCount++;

        } else {
            helper.discloseLabel("img-transfer-bitcoin");
        }

        /*
         * External Gateway?
         */
        final List<PaymentMethodInfo> list = new ArrayList<PaymentMethodInfo>();

        final boolean isExternalGateway;

        final PaymentGateway externalPlugin;

        try {
            externalPlugin = pluginMgr.getExternalPaymentGateway();

            isExternalGateway = allowFinancialTrx && externalPlugin != null
                    && externalPlugin.isOnline()
                    && externalPlugin.isCurrencySupported(appCurrencyCode);

            if (isExternalGateway) {
                list.addAll(
                        externalPlugin.getExternalPaymentMethods().values());
            }

        } catch (PaymentGatewayException e) {
            setResponsePage(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));
            return;
        }

        methodCount += list.size();

        add(new PropertyListView<PaymentMethodInfo>("payment-methods", list) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void
                    populateItem(final ListItem<PaymentMethodInfo> item) {

                final PaymentMethodInfo info = item.getModelObject();

                final Label labelWrk = new Label("img-payment-method", "");

                labelWrk.add(new AttributeModifier("src", WebApp
                        .getPaymentMethodImgUrl(info.getMethod(), false)));

                labelWrk.add(new AttributeModifier("title",
                        localized("button-title-transfer",
                                info.getMethod().toString().toLowerCase())));

                labelWrk.add(new AttributeModifier("data-payment-gateway",
                        externalPlugin.getId()));

                labelWrk.add(new AttributeModifier("data-payment-method",
                        info.getMethod().toString()));

                item.add(labelWrk);
            }

        });

        helper.encloseLabel("header-gateway", localized("header-gateway"),
                methodCount > 0);

    }
}
