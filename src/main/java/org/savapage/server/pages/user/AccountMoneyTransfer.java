/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import java.math.BigDecimal;
import java.text.ParseException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.ext.payment.PaymentGateway;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.PaymentMethodEnum;
import org.savapage.ext.payment.PaymentMethodInfo;
import org.savapage.server.WebApp;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class AccountMoneyTransfer extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public AccountMoneyTransfer(final PageParameters parameters) {

        super(parameters);

        final IRequestParameters parms =
                getRequestCycle().getRequest().getPostParameters();

        final String gatewayId = parms.getParameterValue("gateway").toString();
        final String methodName = parms.getParameterValue("method").toString();

        final PaymentMethodEnum method = PaymentMethodEnum.valueOf(methodName);

        final ServerPluginManager pluginMgr = WebApp.get().getPluginManager();
        final PaymentGateway gateway =
                pluginMgr.getExternalPaymentGateway(gatewayId);

        if (!gateway.isOnline()) {
            setResponsePage(new MessageContent(AppLogLevelEnum.INFO,
                    this.localized("msg-payment-disabled", method.toString())));
            return;
        }

        final PaymentMethodInfo methodInfo;

        try {
            methodInfo = gateway.getExternalPaymentMethods().get(method);
        } catch (PaymentGatewayException e) {
            setResponsePage(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));
            return;
        }

        final MarkupHelper helper = new MarkupHelper(this);

        methodInfo.getMinAmount();

        final String currencySymbol = SpSession.getAppCurrencySymbol();
        final int amountDecimals = ConfigManager.getUserBalanceDecimals();

        //
        helper.addModifyLabelAttr("img-payment-method", "", "src",
                WebApp.getPaymentMethodImgUrl(method, true));

        //
        try {

            BigDecimal decimalWrk;

            final String amount;
            final String perc;

            decimalWrk = methodInfo.getFeeAmount();

            if (decimalWrk == null
                    || decimalWrk.compareTo(BigDecimal.ZERO) == 0) {
                amount = null;
            } else {
                amount = BigDecimalUtil.localize(decimalWrk, amountDecimals,
                        getLocale(), true);
            }

            //
            decimalWrk = methodInfo.getFeePercentage();

            if (decimalWrk == null
                    || decimalWrk.compareTo(BigDecimal.ZERO) == 0) {
                perc = null;
            } else {
                perc = BigDecimalUtil.localize(
                        decimalWrk.multiply(BigDecimal.valueOf(100)),
                        getLocale(), true);

            }

            if (amount == null && perc == null) {

                helper.discloseLabel("payment-costs");

            } else {

                final String prompt;

                if (amount == null) {
                    prompt = localized("prompt-payment-fee-perc", perc);
                } else if (perc == null) {
                    prompt = localized("prompt-payment-fee-amount",
                            currencySymbol, amount);
                } else {
                    prompt = localized("prompt-payment-fee-amount-perc",
                            currencySymbol, amount, perc);
                }
                helper.encloseLabel("payment-costs", prompt, true);
            }

            //
            decimalWrk = methodInfo.getMinAmount();

            if (decimalWrk == null) {
                helper.discloseLabel("payment-min-amount");
            } else {
                helper.encloseLabel("payment-min-amount",
                        localized("prompt-payment-min-amount", currencySymbol,
                                BigDecimalUtil.localize(decimalWrk,
                                        amountDecimals, getLocale(), true)),
                        true);
            }

        } catch (ParseException e) {
            throw new SpException(e.getMessage(), e);
        }

        helper.addLabel("prompt-transfer-money", localized(
                "prompt-transfer-money", method.toString().toLowerCase()));

        //
        helper.addLabel("currency-symbol", currencySymbol);
        helper.addLabel("decimal-separator", SpSession.getDecimalSeparator());

        final Label labelWrk = new Label("money-transfer-gateway", "");

        labelWrk.add(new AttributeModifier("data-payment-gateway", gatewayId));

        labelWrk.add(new AttributeModifier("data-payment-method", methodName));

        add(labelWrk);

    }
}
