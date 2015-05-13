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
package org.savapage.server.plugin.test;

import java.io.PrintWriter;
import java.net.URL;
import java.util.Currency;
import java.util.Map;
import java.util.Properties;

import org.savapage.ext.payment.PaymentGatewayListener;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.ext.payment.PaymentGatewayTrx;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class TestPlugin implements PaymentGatewayPlugin {

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
    public void onStart() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public PaymentGatewayTrx startPayment(final String userId,
            final Currency currency, final double amount,
            final String description, URL callbackUrl, URL redirectUrl) {
        return null;
    }

    @Override
    public CallbackResponse onCallBack(
            final Map<String, String[]> parameterMap, final boolean live,
            final Currency currency, final PrintWriter response) {
        return new CallbackResponse(0);
    }

    @Override
    public void onInit(final String id, final String name, final boolean live,
            final Properties props) {
        // noop
    }

    @Override
    public void onInit(final PaymentGatewayListener listener) {
        // noop
    }

    @Override
    public boolean isLive() {
        // TODO Auto-generated method stub
        return false;
    }

}
