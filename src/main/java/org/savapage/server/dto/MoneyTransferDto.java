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
package org.savapage.server.dto;

import org.savapage.core.dao.enums.AccountTrxTypeEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.ext.payment.PaymentMethodEnum;

/**
 * Information for a {@link AccountTrxTypeEnum#GATEWAY} deposit.
 *
 * @author Rijk Ravestein
 *
 */
public final class MoneyTransferDto extends AbstractDto {

    /**
     * .
     */
    private String gatewayId;

    private PaymentMethodEnum method;

    /**
     * The user who requested the payment.
     */
    private String userId;

    /**
     * The deposited main amount.
     */
    private String amountMain;

    /**
     * The deposited amount cents.
     */
    private String amountCents;

    /**
     * The WebApp URL of the sender.
     */
    private String senderUrl;

    /**
     *
     * @return The unique ID of the Payment Gateway. See
     *         {@link PaymentGatewayPlugin#getId()}.
     */
    public String getGatewayId() {
        return gatewayId;
    }

    /**
     * Sets the unique ID of the Payment Gateway. See
     * {@link PaymentGatewayPlugin#getId()}.
     *
     * @param gatewayId
     *            The gateway ID.
     */
    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public PaymentMethodEnum getMethod() {
        return method;
    }

    public void setMethod(PaymentMethodEnum method) {
        this.method = method;
    }

    /**
     *
     * @return The user ID.
     */
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAmountMain() {
        return amountMain;
    }

    public void setAmountMain(String amountMain) {
        this.amountMain = amountMain;
    }

    public String getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(String amountCents) {
        this.amountCents = amountCents;
    }

    /**
     *
     * @return The WebApp URL of the sender.
     */
    public String getSenderUrl() {
        return senderUrl;
    }

    /**
     *
     * @param senderUrl
     *            The WebApp URL of the sender.
     */
    public void setSenderUrl(final String senderUrl) {
        this.senderUrl = senderUrl;
    }

}
