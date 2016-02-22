/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.server.pages.admin;

import java.text.ParseException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.jpa.Printer;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrinterAccountingAddin extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     *
     */
    public PrinterAccountingAddin(final PageParameters parameters) {

        super(parameters);

        final Long printerId = getRequestCycle().getRequest()
                .getPostParameters().getParameterValue("id").toLong();

        handlePage(printerId);

    }

    /**
     * @param printerId
     *            The database key of the printer.
     */
    private void handlePage(final Long printerId) {

        add(new Label("cost-per-side", localized("cost-per-side",
                ConfigManager.getAppCurrencyCode())));

        final Printer printer = ServiceContext.getDaoContext().getPrinterDao()
                .findById(printerId);

        final String printerName = printer.getPrinterName();

        final boolean isVisible;

        if (!PROXYPRINT_SERVICE.isCupsPrinterDetails(printerName)) {
            isVisible = false;
        } else if (PROXYPRINT_SERVICE.getProxyPrinterCostMediaSource(printer)
                .isEmpty()) {
            isVisible = true;
        } else {
            isVisible = PROXYPRINT_SERVICE.hasMediaSourceManual(printerName);
        }

        final PrinterAccountingPanel pageTotalPanel =
                new PrinterAccountingPanel("printer-accounting-panel",
                        isVisible);

        add(pageTotalPanel);

        if (isVisible) {
            pageTotalPanel.populate(printer);
        }

        final MarkupHelper helper = new MarkupHelper(this);

        final boolean isSimple = printer.getChargeType()
                .equals(Printer.ChargeType.SIMPLE.toString());

        helper.tagRadio("printer-charge-simple",
                Printer.ChargeType.SIMPLE.toString(), isSimple);

        helper.tagRadio("printer-charge-media",
                Printer.ChargeType.MEDIA.toString(), !isSimple);

        String cost;
        try {
            cost = BigDecimalUtil.localize(printer.getDefaultCost(),
                    ConfigManager.instance()
                            .getConfigInt(Key.FINANCIAL_PRINTER_COST_DECIMALS),
                    getSession().getLocale(), false);
        } catch (ParseException e) {
            throw new SpException(e);
        }

        helper.addTextInput("printer-simple-cost", cost);

    }
}
