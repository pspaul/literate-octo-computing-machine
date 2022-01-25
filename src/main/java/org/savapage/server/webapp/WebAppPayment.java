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
package org.savapage.server.webapp;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.server.WebApp;
import org.savapage.server.pages.MessageContent;

public final class WebAppPayment extends WebAppUser {

    /** */
    private static final long serialVersionUID = -4054764972856050822L;

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppPayment(final PageParameters parameters) {
        super(parameters);

        if (!WebApp.get().isPaymentGatewayOnline()) {
            throw new RestartResponseException(new MessageContent(
                    AppLogLevelEnum.ERROR, "No payment gateway available."));
        }
    }

    @Override
    protected IConfigProp.Key getInternetEnableKey() {
        return IConfigProp.Key.WEBAPP_INTERNET_PAYMENT_ENABLE;
    }

    @Override
    public WebAppTypeEnum getWebAppType() {
        return WebAppTypeEnum.PAYMENT;
    }

}
