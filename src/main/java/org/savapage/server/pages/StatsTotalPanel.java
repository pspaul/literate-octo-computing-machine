/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class StatsTotalPanel extends Panel {

    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            Wicket ID.
     */
    public StatsTotalPanel(final String id) {
        super(id);
    }

    /**
     * Gets as localized date string of a Date. The locale of this session is
     * used.
     *
     * @param locale
     *            Locale.
     * @param date
     *            The date.
     * @return The localized date string.
     */
    protected final String localizedDate(final Locale locale, final Date date) {
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
    public abstract void populate();

    /**
     *
     * @param configKey
     * @param observationTime
     * @param interval
     * @param data
     * @return
     * @throws IOException
     */
    protected static List<Object> jqplotXYLineChartSerie(
            final IConfigProp.Key configKey, final Date observationTime,
            final TimeSeriesInterval interval,
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
    protected static List<Object> jqplotXYLineChartSerie(String jsonSeries,
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
                    throw new SpException(
                            "Oops missed interval [" + interval + "]");
                }
            }
        }

        return dataSerie;
    }

}
