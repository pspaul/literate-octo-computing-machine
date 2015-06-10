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

import org.apache.wicket.markup.html.basic.Label;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.jpa.Printer;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Datraverse B.V.
 */
public class PrinterMediaSourceAddin extends AbstractAdminPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private static final ProxyPrintService PROXYPRINT_SERVICE = ServiceContext
            .getServiceFactory().getProxyPrintService();

    /**
     *
     */
    public PrinterMediaSourceAddin() {

        final Long printerId =
                getRequestCycle().getRequest().getPostParameters()
                        .getParameterValue("id").toLong();

        handlePage(printerId);
    }

    /**
     *
     */
    private void handlePage(Long printerId) {

        add(new Label("cost-per-side", localized("cost-per-side",
                ConfigManager.getAppCurrencyCode())));

        final Printer printer =
                ServiceContext.getDaoContext().getPrinterDao()
                        .findById(printerId);

        final boolean isCupsPrinterDetails =
                PROXYPRINT_SERVICE.isCupsPrinterDetails(printer
                        .getPrinterName());

        final PrinterMediaSourcePanel mediaSourcePanel =
                new PrinterMediaSourcePanel("printer-media-source-panel",
                        isCupsPrinterDetails);

        add(mediaSourcePanel);

        if (isCupsPrinterDetails) {
            mediaSourcePanel.populate(printer);
        }

    }
}
