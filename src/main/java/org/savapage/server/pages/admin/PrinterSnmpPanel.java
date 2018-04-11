/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.dao.helpers.ProxyPrinterSnmpInfoDto;
import org.savapage.core.snmp.SnmpPrinterVendorEnum;
import org.savapage.core.snmp.SnmpPrtMarkerColorantValueEnum;
import org.savapage.core.util.LocaleHelper;
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

    /**
     * @param id
     *            The wicket id.
     * @param date
     *            The date of SNMP data retrieval.
     * @param info
     *            The SNMP info. Populates the html.
     * @param extended
     *            If {@code true}, extended view.
     */
    public PrinterSnmpPanel(final String id, final Date date,
            final ProxyPrinterSnmpInfoDto info, final boolean extended) {

        super(id);

        final MarkupHelper helper = new MarkupHelper(this);
        final LocaleHelper localeHelper = new LocaleHelper(getLocale());

        helper.addLabel("date", localeHelper.getLongMediumDateTime(date));

        final String vendor;

        if (extended) {
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

        helper.encloseLabel("vendor", vendor, extended);
        helper.encloseLabel("model", info.getModel(), extended);
        helper.encloseLabel("serial", info.getSerial(), extended);

        helper.addLabel("supplies", info.getSupplies().uiText(getLocale()));
        final StringBuilder markers = new StringBuilder();

        for (final Entry<SnmpPrtMarkerColorantValueEnum, Integer> entry : info
                .getMarkers().entrySet()) {

            markers.append(" &bull; ")
                    .append(entry.getKey().uiText(getLocale())).append("&nbsp;")
                    .append(entry.getValue()).append("%");
        }

        this.add(new Label("markers", markers.toString())
                .setEscapeModelStrings(false));
    }

}
