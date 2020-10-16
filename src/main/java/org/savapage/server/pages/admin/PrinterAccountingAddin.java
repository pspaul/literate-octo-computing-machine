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

import java.text.ParseException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.jpa.Printer;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.ext.papercut.PaperCutIntegrationEnum;
import org.savapage.ext.papercut.services.PaperCutService;
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

    /** */
    private static final PaperCutService PAPERCUT_SERVICE =
            ServiceContext.getServiceFactory().getPaperCutService();

    /** */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     * @param parameters
     *            The page parameters.
     */
    public PrinterAccountingAddin(final PageParameters parameters) {

        super(parameters, ACLOidEnum.A_PRINTERS, RequiredPermission.EDIT);

        final Long printerId = getRequestCycle().getRequest()
                .getPostParameters().getParameterValue("id").toLong();

        handlePage(printerId);

    }

    /**
     * Checks extra visibility conditions (PaperCut).
     *
     * @param printer
     *            Printer
     * @return {@code true} if visible.
     */
    public static boolean isVisibleExt(final Printer printer) {
        final boolean showMediaCost;

        if (PAPERCUT_SERVICE.isExtPaperCutPrint(printer.getPrinterName())) {
            showMediaCost = PAPERCUT_SERVICE
                    .getPrintIntegration() != PaperCutIntegrationEnum.PERSONAL_PRINT;
        } else {
            showMediaCost = true;
        }

        if (showMediaCost) {
            final JsonProxyPrinter proxyPrinter = PROXYPRINT_SERVICE
                    .getCachedPrinter(printer.getPrinterName());
            return !proxyPrinter.hasCustomCostRulesMedia();
        }

        return showMediaCost;
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

        if (PROXYPRINT_SERVICE.isCupsPrinterDetails(printerName)) {
            if (PROXYPRINT_SERVICE.hasMediaSourceManual(printerName)) {
                isVisible = true;
            } else {
                isVisible = PROXYPRINT_SERVICE
                        .getProxyPrinterCostMediaSource(printer).isEmpty();
            }
        } else {
            isVisible = false;
        }
        final boolean isVisibleExt = isVisible && isVisibleExt(printer);

        final PrinterAccountingPanel pageTotalPanel =
                new PrinterAccountingPanel("printer-accounting-panel",
                        isVisibleExt);

        add(pageTotalPanel);

        if (isVisibleExt) {
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
