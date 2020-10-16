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
package org.savapage.server.plugin.test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.savapage.ext.ServerPluginContext;
import org.savapage.ext.payment.PaymentGateway;
import org.savapage.ext.payment.PaymentGatewayListener;
import org.savapage.ext.payment.PaymentGatewayTrx;
import org.savapage.ext.payment.PaymentMethodEnum;
import org.savapage.ext.payment.PaymentMethodInfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class TestPlugin implements PaymentGateway {

    /**
     *
     */
    public TestPlugin() {

    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getName() {
        return "Test Plugin";
    }

    @Override
    public Map<PaymentMethodEnum, PaymentMethodInfo>
            getExternalPaymentMethods() {
        return new HashMap<>();
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public PaymentGatewayTrx onPaymentRequest(final PaymentRequest req) {
        return null;
    }

    @Override
    public CallbackResponse onCallBack(final Map<String, String[]> parameterMap,
            final boolean live, final Currency currency,
            final BufferedReader request, final PrintWriter response) {
        return new CallbackResponse(0);
    }

    @Override
    public void onCallBackCommitted(final Object pluginObject) {
    }

    @Override
    public void onInit(final String id, final String name, final boolean live,
            final boolean online, final Properties props,
            final ServerPluginContext context) {
        // noop
    }

    @Override
    public void onInit(final PaymentGatewayListener listener) {
        // noop
    }

    @Override
    public boolean isLive() {
        return false;
    }

    @Override
    public boolean isCurrencySupported(String currencyCode) {
        return true;
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public void setOnline(boolean online) {
        // noop
    }
}
