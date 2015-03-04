/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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

import java.text.NumberFormat;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class StatsEnvImpactPanel extends Panel {

    private static final long serialVersionUID = 1L;

    public StatsEnvImpactPanel(String id) {
        super(id);
    }

    /**
     * Gets as localized string of a Double. The locale of the current session
     * is used.
     *
     * @param number
     *            The double.
     * @param maxFractionDigits
     *            Number of digits for the fraction.
     * @return The localized string.
     */
    protected final String localizedNumber(final double number,
            final int maxFractionDigits) {
        NumberFormat fm = NumberFormat.getInstance(getSession().getLocale());
        fm.setMaximumFractionDigits(maxFractionDigits);
        return fm.format(number);
    }

    /**
     *
     * @param esu
     */
    public void populate(Double esu) {

        final ConfigManager cm = ConfigManager.instance();
        /*
         * Environmental Impact
         */
        add(new Label("total-esu-printed-out", localizedNumber(esu, 2)));

        Double wh =
                (esu * cm.getConfigDouble(Key.ENV_WATT_HOURS_PER_SHEET)) / 1000;

        String txtEnergy = null;
        /*
         * 10-3 mW·h milliwatt hour
         *
         * 10^-6 µW·h microwatt hour
         *
         * 10^3 kW·h kilowatt hour
         *
         * 10^6 MW·h megawatt hour
         *
         * 10^9 GW·h gigawatt hour
         *
         * 10^12 TW·h terawatt hour
         *
         * 10^15 PW·h petawatt hour
         */
        if (wh < 1) {
            txtEnergy = localizedNumber(wh, 3) + " W-h";
        } else if (wh < (1000 * 1000)) {
            txtEnergy = localizedNumber(wh / 1000, 3) + " kW-h";
        } else {
            txtEnergy = localizedNumber(wh / (1000 * 1000), 3) + " MW-h";
        }
        add(new Label("environmental-impact-energy", txtEnergy));

        /*
         *
         */
        Double trees = esu / cm.getConfigDouble(Key.ENV_SHEETS_PER_TREE);
        String txtTrees = null;
        if (trees < 1.0) {
            txtTrees = localizedNumber(trees * 100, 3) + "%";
        } else {
            txtTrees = localizedNumber(trees, 2);
        }
        add(new Label("environmental-impact-trees", txtTrees));

        /*
         * 10^−3 g mg milligram
         *
         * 10^3 g kg kilogram
         *
         * 10^6 g Mg megagram (tonne)
         */
        Double co2 = esu * cm.getConfigDouble(Key.ENV_CO2_GRAMS_PER_SHEET);
        String txtCo2 = null;

        if (co2 < 1) {
            txtCo2 = localizedNumber(co2 * 1000, 0) + " mg";
        } else if (co2 < 1000) {
            txtCo2 = localizedNumber(co2, 0) + " g";
        } else if (co2 < (1000 * 1000)) {
            txtCo2 = localizedNumber(co2 / 1000, 2) + " kg";
        } else {
            txtCo2 = localizedNumber(co2 / (1000 * 1000), 3) + " Mg";
        }
        add(new Label("environmental-impact-co2", txtCo2));
    }
}
