/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2022 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2022 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.server.pages;

import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.AccountTrxTypeEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class StatsFinancialDetailsPanel extends StatsTotalPanel {

    private static final long serialVersionUID = 1L;

    /** */
    private final boolean isPosDeposit;
    /** */
    private final boolean isPosPurchase;
    /** */
    private final boolean isGateway;

    /**
     * @param id
     *            Wicket ID.
     * @param posDeposit
     *            If {@code true}, POS Deposit is active.
     * @param posPurchase
     *            If {@code true}, POS Sales is active.
     * @param gateway
     *            If {@code true}, Payment Gateway is configured.
     */
    public StatsFinancialDetailsPanel(final String id, final boolean posDeposit,
            final boolean posPurchase, final boolean gateway) {
        super(id);
        this.isGateway = gateway;
        this.isPosDeposit = posDeposit;
        this.isPosPurchase = posPurchase;
    }

    /**
     *
     */
    @Override
    public void populate() {

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("period", NounEnum.PERIOD);
        helper.addLabel("day", NounEnum.DAY);
        helper.addLabel("week", NounEnum.WEEK);
        helper.addLabel("month", NounEnum.MONTH);

        if (this.isPosDeposit) {
            helper.addLabel("deposit",
                    AccountTrxTypeEnum.DEPOSIT.uiText(getLocale()));

            this.addLabel(helper, "deposit-day", "deposit-day-cents",
                    TimeSeriesInterval.DAY,
                    Key.STATS_POS_DEPOSIT_ROLLING_DAY_COUNT,
                    Key.STATS_POS_DEPOSIT_ROLLING_DAY_CENTS);

            this.addLabel(helper, "deposit-week", "deposit-week-cents",
                    TimeSeriesInterval.WEEK,
                    Key.STATS_POS_DEPOSIT_ROLLING_WEEK_COUNT,
                    Key.STATS_POS_DEPOSIT_ROLLING_WEEK_CENTS);

            this.addLabel(helper, "deposit-month", "deposit-month-cents",
                    TimeSeriesInterval.MONTH,
                    Key.STATS_POS_DEPOSIT_ROLLING_MONTH_COUNT,
                    Key.STATS_POS_DEPOSIT_ROLLING_MONTH_CENTS);

        } else {
            helper.discloseLabel("deposit");
            helper.discloseLabel("deposit-day");
        }

        if (this.isGateway) {
            helper.addLabel("gateway",
                    AccountTrxTypeEnum.GATEWAY.uiText(getLocale()));

            this.addLabel(helper, "gateway-day", "gateway-day-cents",
                    TimeSeriesInterval.DAY,
                    Key.STATS_PAYMENT_GATEWAY_ROLLING_DAY_COUNT,
                    Key.STATS_PAYMENT_GATEWAY_ROLLING_DAY_CENTS);
            this.addLabel(helper, "gateway-week", "gateway-week-cents",
                    TimeSeriesInterval.WEEK,
                    Key.STATS_PAYMENT_GATEWAY_ROLLING_WEEK_COUNT,
                    Key.STATS_PAYMENT_GATEWAY_ROLLING_WEEK_CENTS);
            this.addLabel(helper, "gateway-month", "gateway-month-cents",
                    TimeSeriesInterval.MONTH,
                    Key.STATS_PAYMENT_GATEWAY_ROLLING_MONTH_COUNT,
                    Key.STATS_PAYMENT_GATEWAY_ROLLING_MONTH_CENTS);
        } else {
            helper.discloseLabel("gateway");
            helper.discloseLabel("gateway-day");
        }

        if (this.isPosPurchase) {
            helper.addLabel("purchase",
                    AccountTrxTypeEnum.PURCHASE.uiText(getLocale()));
            this.addLabel(helper, "purchase-day", "purchase-day-cents",
                    TimeSeriesInterval.DAY,
                    Key.STATS_POS_PURCHASE_ROLLING_DAY_COUNT,
                    Key.STATS_POS_PURCHASE_ROLLING_DAY_CENTS);
            this.addLabel(helper, "purchase-week", "purchase-week-cents",
                    TimeSeriesInterval.WEEK,
                    Key.STATS_POS_PURCHASE_ROLLING_WEEK_COUNT,
                    Key.STATS_POS_PURCHASE_ROLLING_WEEK_CENTS);
            this.addLabel(helper, "purchase-month", "purchase-month-cents",
                    TimeSeriesInterval.MONTH,
                    Key.STATS_POS_PURCHASE_ROLLING_MONTH_COUNT,
                    Key.STATS_POS_PURCHASE_ROLLING_MONTH_CENTS);
        } else {
            helper.discloseLabel("purchase");
            helper.discloseLabel("purchase-day");
        }
    }

    /**
     * Adds a Wicket label for a Financial Count and Amount.
     *
     * @param helper
     * @param wicketIdCount
     * @param wicketIdCents
     * @param intervalWlk
     * @param keyCount
     * @param keyCents
     */
    private void addLabel(final MarkupHelper helper, final String wicketIdCount,
            final String wicketIdCents, final TimeSeriesInterval intervalWlk,
            final Key keyCount, final Key keyCents) {
        this.addLabelCount(helper, wicketIdCount, intervalWlk, keyCount);
        this.addLabelCents(helper, wicketIdCents, intervalWlk, keyCents);
    }

    /**
     * Adds a Wicket label for a Financial Count.
     *
     * @param helper
     * @param wicketId
     * @param intervalWlk
     * @param key
     */
    private void addLabelCount(final MarkupHelper helper, final String wicketId,
            final TimeSeriesInterval intervalWlk, final Key key) {
        helper.addLabel(wicketId,
                helper.localizedNumberOrSpace(getTodayValue(
                        new JsonRollingTimeSeries<>(intervalWlk, 1, 0),
                        intervalWlk, key)))
                .setEscapeModelStrings(false);
    }

    /**
     * Adds a Wicket label for a Financial Amount.
     *
     * @param helper
     * @param wicketIdAmount
     * @param intervalWlk
     * @param keyCents
     */
    private void addLabelCents(final MarkupHelper helper,
            final String wicketIdAmount, final TimeSeriesInterval intervalWlk,
            final Key keyCents) {
        final long value =
                getTodayValue(new JsonRollingTimeSeries<>(intervalWlk, 1, 0),
                        intervalWlk, keyCents);
        helper.addLabel(wicketIdAmount, helper.localizedNumberOrSpace(value, 2))
                .setEscapeModelStrings(false);
    }

}
