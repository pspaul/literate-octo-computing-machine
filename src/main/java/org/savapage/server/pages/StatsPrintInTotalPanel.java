/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages;

import java.io.IOException;
import java.util.List;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.helpers.SparklineHtml;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class StatsPrintInTotalPanel extends StatsTotalPanel {

    private static final long serialVersionUID = 1L;

    /**
     * @param id
     *            Wicket ID.
     */
    public StatsPrintInTotalPanel(final String id) {
        super(id);
    }

    /**
     *
     */
    @Override
    public void populate() {
        final MarkupHelper helper = new MarkupHelper(this);
        final ConfigManager cm = ConfigManager.instance();

        helper.addLabel("received", AdjectiveEnum.RECEIVED);
        helper.addLabel("valid", AdjectiveEnum.VALID);
        helper.addLabel("repaired", AdjectiveEnum.REPAIRED);
        helper.addLabel("rejected", AdjectiveEnum.REJECTED);
        helper.addLabel("period", NounEnum.PERIOD);

        helper.addLabel("pdf-week", NounEnum.WEEK);
        helper.addLabel("pdf-month", NounEnum.MONTH);
        helper.addLabel("pdf-tot", NounEnum.TOTAL);

        helper.addLabel("printin-totals-since", String.format("%s %s",
                AdverbEnum.SINCE.uiText(getSession().getLocale()),
                localizedDate(getSession().getLocale(), cm
                        .getConfigDate(Key.STATS_TOTAL_RESET_DATE_PRINT_IN))));

        MarkupHelper.appendComponentAttr(
                helper.addTransparant("pdf-valid-cell"),
                MarkupHelper.ATTR_STYLE,
                String.format("color: %s;", SparklineHtml.COLOR_QUEUE));

        //
        long totPdf, totPdfRepaired, totPdfRejected;

        //
        TimeSeriesInterval intervalWlk = TimeSeriesInterval.WEEK;

        JsonRollingTimeSeries<Integer> data =
                new JsonRollingTimeSeries<>(intervalWlk, 1, 0);

        totPdf = getTodayValue(data, intervalWlk,
                Key.STATS_PRINT_IN_ROLLING_WEEK_PDF);
        totPdfRepaired = getTodayValue(data, intervalWlk,
                Key.STATS_PRINT_IN_ROLLING_WEEK_PDF_REPAIR)
                + getTodayValue(data, intervalWlk,
                        Key.STATS_PRINT_IN_ROLLING_WEEK_PDF_REPAIR_FONT);
        totPdfRejected = getTodayValue(data, intervalWlk,
                Key.STATS_PRINT_IN_ROLLING_WEEK_PDF_REPAIR_FAIL)
                + getTodayValue(data, intervalWlk,
                        Key.STATS_PRINT_IN_ROLLING_WEEK_PDF_REPAIR_FONT_FAIL);

        helper.addLabel("pdf-valid-week", Long
                .valueOf(totPdf - totPdfRepaired - totPdfRejected).toString());
        helper.addLabel("pdf-repaired-week",
                helper.localizedNumberOrSpace(totPdfRepaired))
                .setEscapeModelStrings(false);
        helper.addLabel("pdf-rejected-week",
                helper.localizedNumberOrSpace(totPdfRejected))
                .setEscapeModelStrings(false);

        //
        intervalWlk = TimeSeriesInterval.MONTH;
        data = new JsonRollingTimeSeries<>(intervalWlk, 1, 0);

        totPdf = getTodayValue(data, intervalWlk,
                Key.STATS_PRINT_IN_ROLLING_MONTH_PDF);
        totPdfRepaired = getTodayValue(data, intervalWlk,
                Key.STATS_PRINT_IN_ROLLING_MONTH_PDF_REPAIR)
                + getTodayValue(data, intervalWlk,
                        Key.STATS_PRINT_IN_ROLLING_MONTH_PDF_REPAIR_FONT);
        totPdfRejected = getTodayValue(data, intervalWlk,
                Key.STATS_PRINT_IN_ROLLING_MONTH_PDF_REPAIR_FAIL)
                + getTodayValue(data, intervalWlk,
                        Key.STATS_PRINT_IN_ROLLING_MONTH_PDF_REPAIR_FONT_FAIL);

        helper.addLabel("pdf-valid-month", Long
                .valueOf(totPdf - totPdfRepaired - totPdfRejected).toString());
        helper.addLabel("pdf-repaired-month",
                helper.localizedNumberOrSpace(totPdfRepaired))
                .setEscapeModelStrings(false);
        helper.addLabel("pdf-rejected-month",
                helper.localizedNumberOrSpace(totPdfRejected))
                .setEscapeModelStrings(false);

        //
        totPdf = cm.getConfigLong(Key.STATS_TOTAL_PRINT_IN_PDF);
        totPdfRepaired = cm.getConfigLong(Key.STATS_TOTAL_PRINT_IN_PDF_REPAIR)
                + cm.getConfigLong(Key.STATS_TOTAL_PRINT_IN_PDF_REPAIR_FONT);
        totPdfRejected =
                cm.getConfigLong(Key.STATS_TOTAL_PRINT_IN_PDF_REPAIR_FAIL)
                        + cm.getConfigLong(
                                Key.STATS_TOTAL_PRINT_IN_PDF_REPAIR_FONT_FAIL);

        helper.addLabel("pdf-valid-tot", Long
                .valueOf(totPdf - totPdfRepaired - totPdfRejected).toString());
        helper.addLabel("pdf-repaired-tot",
                helper.localizedNumberOrSpace(totPdfRepaired))
                .setEscapeModelStrings(false);
        helper.addLabel("pdf-rejected-tot",
                helper.localizedNumberOrSpace(totPdfRejected))
                .setEscapeModelStrings(false);
    }

    /**
     *
     * @param data
     * @param interval
     * @param configKey
     * @return
     */
    private static long getTodayValue(final JsonRollingTimeSeries<Integer> data,
            final TimeSeriesInterval interval, final Key configKey) {
        try {
            final List<Object> dataSerie = jqplotXYLineChartSerie(configKey,
                    ServiceContext.getTransactionDate(), interval, data);
            if (dataSerie.size() == 1) {
                return ((Integer) ((List<Object>) dataSerie.get(0)).get(1))
                        .longValue();
            }
        } catch (IOException e) {
            // noop
        }
        return 0;
    }

}
