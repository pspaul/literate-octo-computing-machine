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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.jpa.Printer;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.ext.papercut.PaperCutIntegrationEnum;
import org.savapage.ext.papercut.services.PaperCutService;
import org.savapage.server.WebApp;
import org.savapage.server.pages.AbstractPage;
import org.savapage.server.pages.MarkupHelper;

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

    /** */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final PrinterService PRINTER_SERVICE =
            ServiceContext.getServiceFactory().getPrinterService();

    /** */
    private static final PaperCutService PAPERCUT_SERVICE =
            ServiceContext.getServiceFactory().getPaperCutService();

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

    /** */
    private static final String WID_DEVICE_URI_IMG = "deviceUriImg";

    /**
     * @param printerId
     *            Primary key of printer.
     */
    private void handlePage(final Long printerId) {

        final MarkupHelper helper = new MarkupHelper(this);

        final Printer printer = ServiceContext.getDaoContext().getPrinterDao()
                .findById(printerId);

        final boolean showMediaCost;

        final StringBuilder remark = new StringBuilder();

        if (PAPERCUT_SERVICE.isExtPaperCutPrint(printer.getPrinterName())) {

            helper.addModifyLabelAttr(WID_DEVICE_URI_IMG, MarkupHelper.ATTR_SRC,
                    WebApp.getThirdPartyEnumImgUrl(ThirdPartyEnum.PAPERCUT));

            final PaperCutIntegrationEnum papercutInt =
                    PAPERCUT_SERVICE.getPrintIntegration();

            switch (papercutInt) {

            case DELEGATED_PRINT:
                showMediaCost = false;
                remark.append(localized("papercut-delegated-print")).append(" ")
                        .append(localized(
                                "papercut-personal-print-cost-papercut-ticket"));
                break;

            case PERSONAL_PRINT:
                remark.append(localized("papercut-personal-print"));
                if (remark.length() > 0) {
                    remark.append(" ");
                }
                if (PRINTER_SERVICE.isHoldReleasePrinter(printer)) {
                    showMediaCost = true;
                    /*
                     * Because SavaPage cost is communicated to user before
                     * print release, SavaPage cost must be in-sync with
                     * PaperCut cost.
                     */
                    remark.append(
                            localized("papercut-personal-print-cost-papercut"))
                            .append(" ")
                            .append(localized("papercut-media-cost-match"));
                } else {
                    showMediaCost = false;
                    remark.append(localized(
                            "papercut-personal-print-cost-papercut-ticket"));
                }
                break;

            default:
                showMediaCost = true;
                break;
            }
        } else {
            showMediaCost = true;
            helper.discloseLabel(WID_DEVICE_URI_IMG);
        }

        helper.addModifyLabelAttr("printerImage", MarkupHelper.ATTR_SRC,
                AbstractPage.getImgSrc(printer).urlPath());

        //
        add(new Label("cost-per-side", localized("cost-per-side",
                ConfigManager.getAppCurrencyCode())));

        final boolean isCupsPrinterDetails = PROXYPRINT_SERVICE
                .isCupsPrinterDetails(printer.getPrinterName());

        final PrinterMediaSourcePanel mediaSourcePanel =
                new PrinterMediaSourcePanel("printer-media-source-panel",
                        isCupsPrinterDetails);

        add(mediaSourcePanel);

        final JsonProxyPrinter proxyPrinter =
                PROXYPRINT_SERVICE.getCachedPrinter(printer.getPrinterName());

        final boolean isCustomCostMedia;
        final boolean isCustomCostSet;
        final boolean isCustomCostCopy;
        final boolean isCustomCostSheet;

        if (proxyPrinter == null) {
            isCustomCostMedia = false;
            isCustomCostSet = false;
            isCustomCostCopy = false;
            isCustomCostSheet = false;
        } else {
            isCustomCostMedia = proxyPrinter.hasCustomCostRulesMedia();
            isCustomCostSet = proxyPrinter.hasCustomCostRulesSet();
            isCustomCostCopy = proxyPrinter.hasCustomCostRulesCopy();
            isCustomCostSheet = proxyPrinter.hasCustomCostRulesSheet();
        }

        if (isCustomCostMedia || isCustomCostSet || isCustomCostCopy
                || isCustomCostSheet) {

            if (remark.length() > 0) {
                remark.append(" ");
            }

            final StringBuilder costFactor = new StringBuilder();

            if (isCustomCostMedia) {
                costFactor.append("Media");
            }
            if (isCustomCostSheet) {
                if (costFactor.length() > 0) {
                    costFactor.append("+");
                }
                costFactor.append("Sheet");
            }
            if (isCustomCostCopy) {
                if (costFactor.length() > 0) {
                    costFactor.append("+");
                }
                costFactor.append("Copy");
            }
            if (isCustomCostSet) {
                if (costFactor.length() > 0) {
                    costFactor.append("+");
                }
                costFactor.append("Set");
            }

            remark.append(localized("custom-cost-info", costFactor.toString()));
        }

        if (isCupsPrinterDetails) {
            mediaSourcePanel.populate(printer,
                    showMediaCost && !isCustomCostMedia);
        }

        helper.encloseLabel("media-source-remark", remark.toString(),
                remark.length() > 0);
    }

}
