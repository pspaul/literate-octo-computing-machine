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
package org.savapage.server.pages;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;
import org.savapage.core.services.UserService;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class StatsPageTotalPanel extends Panel {

    private static final long serialVersionUID = 1L;

    public StatsPageTotalPanel(String id) {
        super(id);
    }

    /**
     * Gets as localized date string of a Date. The locale of this session is
     * used.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    protected final String localizedDate(Locale locale, final Date date) {
        return DateFormat.getDateInstance(DateFormat.LONG, locale).format(date);
    }

    /**
     * Gives the localized string for a key.
     *
     * @param key
     *            The key from the XML resource file
     * @return The localized string.
     */
    protected final String localized(final String key) {
        return getLocalizer().getString(key, this);
    }

    /**
     *
     */
    public void populate() {

        add(new Label("page-totals-since", String.format(
                "%s %s",
                localized("since"),
                localizedDate(getSession().getLocale(), ConfigManager
                        .instance().getConfigDate(Key.STATS_TOTAL_RESET_DATE)))));
    }

    /**
     * Returns the jqPlot chartData of a User PieChart.
     *
     * @return
     */
    public static Map<String, Object> jqplotPieChart(
            org.savapage.core.jpa.User user) {
        return jqplotPieChart(user.getNumberOfPrintOutPages().longValue(), user
                .getNumberOfPrintInPages().longValue(), user
                .getNumberOfPdfOutPages().longValue());
    }

    /**
     * Returns the jqPlot chartData of the global PieChart
     *
     * @return
     */
    public static Map<String, Object> jqplotPieChart() {

        ConfigManager cm = ConfigManager.instance();

        return jqplotPieChart(
                cm.getConfigLong(Key.STATS_TOTAL_PRINT_OUT_PAGES),
                cm.getConfigLong(Key.STATS_TOTAL_PRINT_IN_PAGES),
                cm.getConfigLong(Key.STATS_TOTAL_PDF_OUT_PAGES));
    }

    /**
     * Returns the jqPlot chartData for a PieChart.
     *
     * @return
     */
    private static Map<String, Object> jqplotPieChart(Long pagesPrintOut,
            Long pagesPrintIn, Long pagesPdfOut) {

        Map<String, Object> chartData = new HashMap<String, Object>();

        /*
         * dataSeries
         *
         * Example: [ [['ProxyPrinters', 564], ['Queues', 1634], ['PDF', 862]]
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
        dataEntry.add("ProxyPrinters");
        dataEntry.add(pagesPrintOut);

        dataEntry = new ArrayList<>();
        dataSerie1.add(dataEntry);
        dataEntry.add("Queues");
        dataEntry.add(pagesPrintIn);

        dataEntry = new ArrayList<>();
        dataSerie1.add(dataEntry);
        dataEntry.add("PDF");
        dataEntry.add(pagesPdfOut);

        /*
         * optionObj
         */
        Map<String, Object> optionObj = new HashMap<String, Object>();
        chartData.put("optionObj", optionObj);

        Map<String, Object> title = new HashMap<String, Object>();
        optionObj.put("title", title);

        title.put("text", "Totals");

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
                jqplotXYLineChartSerie(printOut, observationTime,
                        TimeSeriesInterval.WEEK, data),
                jqplotXYLineChartSerie(printIn, observationTime,
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
                jqplotXYLineChartSerie(Key.STATS_PRINT_OUT_ROLLING_DAY_PAGES,
                        observationTime, TimeSeriesInterval.DAY, data),
                jqplotXYLineChartSerie(Key.STATS_PRINT_IN_ROLLING_DAY_PAGES,
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
    public static Map<String, Object> jqplotXYLineChart(
            List<Object> rollingDayPagesPrintOut,
            List<Object> rollingDayPagesPrintIn,
            List<Object> rollingDayPagesPdfOut) throws IOException {

        final Map<String, Object> chartData = new HashMap<String, Object>();

        /*
         * dataSeries
         *
         * Example: [ [['2008-06-01', 2], ['2008-06-02', 5], ['2008-06-03', 13],
         * ['2008-06-04', 33], ['2008-06-05', 85], ['2008-06-06', 219]], ... ]
         */
        final List<Object> dataSeries = new ArrayList<>();
        chartData.put("dataSeries", dataSeries);

        dataSeries.add(rollingDayPagesPrintOut);
        dataSeries.add(rollingDayPagesPrintIn);
        dataSeries.add(rollingDayPagesPdfOut);

        /*
         * optionObj
         */
        Map<String, Object> optionObj = new HashMap<String, Object>();
        chartData.put("optionObj", optionObj);

        Map<String, Object> title = new HashMap<String, Object>();
        optionObj.put("title", title);

        title.put("text", "Totals");

        // ----
        return chartData;
    }

    /**
     *
     * @param observationTime
     * @param data
     * @return
     * @throws IOException
     */
    private static List<Object> jqplotXYLineChartSerie(
            IConfigProp.Key configKey, final Date observationTime,
            TimeSeriesInterval interval,
            final JsonRollingTimeSeries<Integer> data) throws IOException {

        return jqplotXYLineChartSerie(
                ConfigManager.instance().getConfigValue(configKey),
                observationTime, interval, data);
    }

    /**
     *
     * @param observationTime
     * @param data
     * @return
     * @throws IOException
     */
    private static List<Object> jqplotXYLineChartSerie(String jsonSeries,
            final Date observationTime, TimeSeriesInterval interval,
            final JsonRollingTimeSeries<Integer> data) throws IOException {

        final List<Object> dataSerie = new ArrayList<>();

        data.clear();
        data.init(observationTime, jsonSeries);

        if (!data.getData().isEmpty()) {

            Date dateWlk = new Date(data.getLastTime());

            for (final Integer total : data.getData()) {

                final List<Object> entry = new ArrayList<>();
                dataSerie.add(entry);

                entry.add(Long.valueOf(dateWlk.getTime()));
                entry.add(total);
                switch (interval) {
                case DAY:
                    dateWlk = DateUtils.addDays(dateWlk, -1);
                    break;
                case MONTH:
                    dateWlk = DateUtils.addMonths(dateWlk, -1);
                    break;
                case WEEK:
                    dateWlk = DateUtils.addWeeks(dateWlk, -1);
                    break;
                case HOUR:
                    dateWlk = DateUtils.addHours(dateWlk, -1);
                    break;
                default:
                    throw new SpException("Oops missed interval [" + interval
                            + "]");
                }

            }
        }

        return dataSerie;
    }

}
