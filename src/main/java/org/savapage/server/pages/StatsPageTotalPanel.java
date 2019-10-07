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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.basic.Label;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class StatsPageTotalPanel extends StatsTotalPanel {

    private static final long serialVersionUID = 1L;

    /**
     * @param id
     *            Wicket ID.
     */
    public StatsPageTotalPanel(final String id) {
        super(id);
    }

    /**
     *
     */
    @Override
    public void populate() {

        add(new Label("page-totals-since", String.format("%s %s",
                localized("since"),
                localizedDate(getSession().getLocale(), ConfigManager.instance()
                        .getConfigDate(Key.STATS_TOTAL_RESET_DATE)))));
    }

    /**
     * Returns the jqPlot chartData of a User PieChart.
     *
     * @return
     */
    public static Map<String, Object>
            jqplotPieChart(org.savapage.core.jpa.User user) {
        return jqplotPieChart(user.getNumberOfPrintInPages().longValue(),
                user.getNumberOfPrintOutPages().longValue(),
                user.getNumberOfPdfOutPages().longValue());
    }

    /**
     * Returns the jqPlot chartData of the global PieChart
     *
     * @return
     */
    public static Map<String, Object> jqplotPieChart() {

        ConfigManager cm = ConfigManager.instance();

        return jqplotPieChart(cm.getConfigLong(Key.STATS_TOTAL_PRINT_IN_PAGES),
                cm.getConfigLong(Key.STATS_TOTAL_PRINT_OUT_PAGES),
                cm.getConfigLong(Key.STATS_TOTAL_PDF_OUT_PAGES));
    }

    /**
     * Returns the jqPlot chartData for a PieChart.
     *
     * @param pagesPrintIn
     * @param pagesPrintOut
     * @param pagesPdfOut
     * @return
     */
    private static Map<String, Object> jqplotPieChart(Long pagesPrintIn,
            Long pagesPrintOut, Long pagesPdfOut) {

        Map<String, Object> chartData = new HashMap<String, Object>();

        /*
         * dataSeries
         *
         * Example: [ [['Queues', 1634], ['ProxyPrinters', 564], ['PDF', 862]]
         * ];
         */
        final List<Object> dataSeries = new ArrayList<>();
        chartData.put("dataSeries", dataSeries);

        // Serie 1
        final List<Object> dataSerie1 = new ArrayList<>();
        dataSeries.add(dataSerie1);

        List<Object> dataEntry = null;

        dataEntry = new ArrayList<>();
        dataSerie1.add(dataEntry);
        dataEntry
                .add(AdjectiveEnum.RECEIVED.uiText(ServiceContext.getLocale()));
        dataEntry.add(pagesPrintIn);

        dataEntry = new ArrayList<>();
        dataSerie1.add(dataEntry);
        dataEntry.add(AdjectiveEnum.PRINTED.uiText(ServiceContext.getLocale()));
        dataEntry.add(pagesPrintOut);

        dataEntry = new ArrayList<>();
        dataSerie1.add(dataEntry);
        dataEntry.add(
                AdjectiveEnum.DOWLOADED.uiText(ServiceContext.getLocale()));
        dataEntry.add(pagesPdfOut);

        /*
         * optionObj
         */
        Map<String, Object> optionObj = new HashMap<String, Object>();
        chartData.put("optionObj", optionObj);

        Map<String, Object> title = new HashMap<String, Object>();
        optionObj.put("title", title);

        title.put("text",
                NounEnum.TOTAL.uiText(ServiceContext.getLocale(), true));

        // ----
        return chartData;
    }

    /**
     * Returns the jqPlot chartData for the User XYLineChart (rolling week).
     *
     * @return
     * @throws IOException
     */
    public static Map<String, Object> jqplotXYLineChart(
            org.savapage.core.jpa.User user) throws IOException {

        final Date observationTime = new Date();

        final JsonRollingTimeSeries<Integer> data =
                new JsonRollingTimeSeries<>(TimeSeriesInterval.WEEK,
                        UserService.MAX_TIME_SERIES_INTERVALS_WEEK, 0);

        String printOut = "0";
        String printIn = "0";
        String pdfOut = "0";

        for (final UserAttr attr : user.getAttributes()) {

            if (attr.getName().equals(
                    UserAttrEnum.PRINT_OUT_ROLLING_WEEK_PAGES.getName())) {
                printOut = attr.getValue();
            } else if (attr.getName().equals(
                    UserAttrEnum.PRINT_IN_ROLLING_WEEK_PAGES.getName())) {
                printIn = attr.getValue();
            } else if (attr.getName().equals(
                    UserAttrEnum.PDF_OUT_ROLLING_WEEK_PAGES.getName())) {
                pdfOut = attr.getValue();
            }
        }

        return jqplotXYLineChart(
                jqplotXYLineChartSerie(printIn, observationTime,
                        TimeSeriesInterval.WEEK, data),
                jqplotXYLineChartSerie(printOut, observationTime,
                        TimeSeriesInterval.WEEK, data),
                jqplotXYLineChartSerie(pdfOut, observationTime,
                        TimeSeriesInterval.WEEK, data));
    }

    /**
     * Returns the jqPlot chartData for the global XYLineChart.
     *
     * @return
     * @throws IOException
     */
    public static Map<String, Object> jqplotXYLineChart() throws IOException {

        final JsonRollingTimeSeries<Integer> data =
                new JsonRollingTimeSeries<>(TimeSeriesInterval.DAY, 30, 0);

        final Date observationTime = new Date();

        return jqplotXYLineChart(
                jqplotXYLineChartSerie(Key.STATS_PRINT_IN_ROLLING_DAY_PAGES,
                        observationTime, TimeSeriesInterval.DAY, data),
                jqplotXYLineChartSerie(Key.STATS_PRINT_OUT_ROLLING_DAY_PAGES,
                        observationTime, TimeSeriesInterval.DAY, data),
                jqplotXYLineChartSerie(Key.STATS_PDF_OUT_ROLLING_DAY_PAGES,
                        observationTime, TimeSeriesInterval.DAY, data));
    }

    /**
     * Returns the jqPlot chartData for the global XYLineChart.
     *
     * @return
     * @throws IOException
     */
    private static Map<String, Object> jqplotXYLineChart(
            final List<Object> rollingDayPagesPrintIn,
            final List<Object> rollingDayPagesPrintOut,
            final List<Object> rollingDayPagesPdfOut) throws IOException {

        final Map<String, Object> chartData = new HashMap<String, Object>();

        /*
         * dataSeries
         *
         * Example: [ [['2008-06-01', 2], ['2008-06-02', 5], ['2008-06-03', 13],
         * ['2008-06-04', 33], ['2008-06-05', 85], ['2008-06-06', 219]], ... ]
         */
        final List<Object> dataSeries = new ArrayList<>();
        chartData.put("dataSeries", dataSeries);

        dataSeries.add(rollingDayPagesPrintIn);
        dataSeries.add(rollingDayPagesPrintOut);
        dataSeries.add(rollingDayPagesPdfOut);

        /*
         * optionObj
         */
        Map<String, Object> optionObj = new HashMap<String, Object>();
        chartData.put("optionObj", optionObj);

        Map<String, Object> title = new HashMap<String, Object>();
        optionObj.put("title", title);

        title.put("text",
                NounEnum.TOTAL.uiText(ServiceContext.getLocale(), true));

        // ----
        return chartData;
    }

}
