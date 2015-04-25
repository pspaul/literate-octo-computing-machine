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
    public void onStart() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public PaymentGatewayTrx startPayment(final String userId,
            final double amount, final String description) {
        return null;
    }

    @Override
    public Integer onCallBack(final String pathInfo, final String queryString,
            final Map<String, String[]> parameterMap) {
        return Integer.valueOf(0);
    }

    @Override
    public String getCallbackUrl() {
        return null;
    }

    @Override
    public String getRedirectUrl() {
        return null;
    }

    @Override
    public void onInit(final Properties props) {
        // noop
    }

    @Override
    public void onInit(final PaymentGatewayListener listener) {
        // noop
    }

    @Override
    public String getCallbackSubPath() {
        return String.format("/%s", this.getClass().getSimpleName());
    }

}
