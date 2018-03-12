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
package org.savapage.server.pages.admin;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.jpa.Printer;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrinterMediaSourceAddin extends AbstractAdminPage {

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
     * @param parameters
     *            The page parameters.
     */
    public PrinterMediaSourceAddin(final PageParameters parameters) {

        super(parameters, ACLOidEnum.A_PRINTERS, RequiredPermission.EDIT);

        final Long printerId = getRequestCycle().getRequest()
                .getPostParameters().getParameterValue("id").toLong();

        handlePage(printerId);
    }

    /**
     *
     */
    private void handlePage(final Long printerId) {

        add(new Label("cost-per-side", localized("cost-per-side",
                ConfigManager.getAppCurrencyCode())));

        final Printer printer = ServiceContext.getDaoContext().getPrinterDao()
                .findById(printerId);

        final boolean isCupsPrinterDetails = PROXYPRINT_SERVICE
                .isCupsPrinterDetails(printer.getPrinterName());

        final PrinterMediaSourcePanel mediaSourcePanel =
                new PrinterMediaSourcePanel("printer-media-source-panel",
                        isCupsPrinterDetails);

        add(mediaSourcePanel);

        if (isCupsPrinterDetails) {
            mediaSourcePanel.populate(printer);
        }

    }
}
