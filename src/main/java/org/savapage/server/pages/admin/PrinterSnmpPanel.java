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
package org.savapage.server.pages.admin;

import java.util.Date;
import java.util.Map.Entry;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.dao.helpers.ProxyPrinterSnmpInfoDto;
import org.savapage.core.snmp.SnmpPrinterErrorStateEnum;
import org.savapage.core.snmp.SnmpPrinterVendorEnum;
import org.savapage.core.snmp.SnmpPrtMarkerColorantValueEnum;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.LocaleHelper;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.SparklineHtml;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrinterSnmpPanel extends Panel {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final String WID_MARKERS = "markers";

    /** */
    private static final String WID_MARKER_BAR = "marker-bar";

    /** */
    private static final String WID_DATE = "date";

    /** */
    private static final String WID_VENDOR = "vendor";

    /** */
    private static final String WID_MODEL = "model";

    /** */
    private static final String WID_SERIAL = "serial";

    /** */
    private static final String WID_STATES = "states";

    /** */
    private static final String WID_BTN_REFRESH = "btn-refresh";

    /** */
    private static final int BAR_WIDTH[] = new int[] { 10, 5, 5, 5 };

    /**
     * @param id
     *            The wicket id.
     * @param printerID
     *            The primary database key of printer. If {@code null}, no
     *            refresh link is shown.
     * @param date
     *            The date of SNMP data retrieval.
     * @param info
     *            The SNMP info. Populates the html.
     * @param extended
     *            If {@code true}, extended view.
     */
    public PrinterSnmpPanel(final String id, final Long printerID,
            final Date date, final ProxyPrinterSnmpInfoDto info,
            final boolean extended) {

        super(id);

        final MarkupHelper helper = new MarkupHelper(this);
        final LocaleHelper localeHelper = new LocaleHelper(getLocale());

        if (printerID == null) {
            helper.discloseLabel(WID_BTN_REFRESH);
        } else {
            final Component btnRefresh = helper.addTransparant(WID_BTN_REFRESH);

            MarkupHelper.modifyComponentAttr(btnRefresh,
                    MarkupHelper.ATTR_TITLE,
                    HtmlButtonEnum.REFRESH.uiText(getLocale(), true));

            MarkupHelper.modifyComponentAttr(btnRefresh,
                    MarkupHelper.ATTR_DATA_SAVAPAGE, printerID.toString());
        }
        //
        final boolean hasInfo = info != null;

        final String vendor;

        if (hasInfo && extended) {
            final SnmpPrinterVendorEnum vendorEnum =
                    SnmpPrinterVendorEnum.fromEnterprise(info.getVendor());

            if (vendorEnum == null) {
                vendor = info.getVendor().toString();
            } else {
                vendor = vendorEnum.getUiText();
            }
        } else {
            vendor = "";
        }

        helper.encloseLabel(WID_VENDOR, vendor, hasInfo && extended);

        //
        final StringBuilder states = new StringBuilder();

        if (hasInfo) {

            helper.addLabel(WID_DATE,
                    localeHelper.getLongMediumDateTime(info.getDate()));

            helper.encloseLabel(WID_MODEL, info.getModel(),
                    hasInfo && extended);
            helper.encloseLabel(WID_SERIAL, info.getSerial(),
                    hasInfo && extended);

            final StringBuilder markers = new StringBuilder();
            final String[] markerValues = new String[info.getMarkers().size()];
            final String[] markerColors = new String[info.getMarkers().size()];

            int i = 0;

            for (final Entry<SnmpPrtMarkerColorantValueEnum, Integer> entry : //
            info.getMarkers().entrySet()) {

                if (extended) {
                    markers.append(" &bull; ")
                            .append(entry.getKey().uiText(getLocale()))
                            .append("&nbsp;").append(entry.getValue())
                            .append("%");
                }

                markerValues[i] = entry.getValue().toString();
                markerColors[i] = entry.getKey().getHtmlColor();

                i++;
            }

            if (markers.length() > 0) {
                helper.addLabel("supplies",
                        info.getSupplies().uiText(getLocale()));

                this.add(new Label(WID_MARKERS, markers.toString())
                        .setEscapeModelStrings(false));
            } else {
                helper.discloseLabel(WID_MARKERS);
            }

            final int barWidth;

            if (markerColors.length > 0 && markerColors.length < 5) {
                barWidth = BAR_WIDTH[markerColors.length - 1];
            } else {
                barWidth = BAR_WIDTH[BAR_WIDTH.length - 1];
            }

            final Label bar = helper.addModifyLabelAttr(WID_MARKER_BAR,
                    SparklineHtml.valueString(markerValues),
                    SparklineHtml.ATTR_COLOR_MAP,
                    SparklineHtml.arrayAttr(markerColors));

            MarkupHelper.modifyLabelAttr(bar, MarkupHelper.ATTR_CLASS,
                    SparklineHtml.CSS_CLASS_PRINTER);

            MarkupHelper.modifyLabelAttr(bar, SparklineHtml.ATTR_BAR_WIDTH,
                    String.valueOf(barWidth));

            //
            final long duration = date.getTime() - info.getDate().getTime();

            if (duration == 0) {

                if (info.getErrorStates() != null) {
                    for (final SnmpPrinterErrorStateEnum error : info
                            .getErrorStates()) {
                        if (states.length() > 0) {
                            states.append(", ");
                        }
                        states.append(error.uiText(getLocale()));
                    }
                }

            } else {
                states.append(
                        SnmpPrinterErrorStateEnum.OFFLINE.uiText(getLocale()))
                        .append(" (").append(DateUtil.formatDuration(duration))
                        .append(")");
            }

        } else {

            helper.addLabel(WID_DATE, localeHelper.getLongMediumDateTime(date));

            helper.discloseLabel(WID_MODEL);
            helper.discloseLabel(WID_SERIAL);
            helper.discloseLabel(WID_MARKERS);
            helper.discloseLabel(WID_MARKER_BAR);

            states.append(
                    SnmpPrinterErrorStateEnum.OFFLINE.uiText(getLocale()));
        }

        helper.encloseLabel(WID_STATES, states.toString(), states.length() > 0);
    }

}
