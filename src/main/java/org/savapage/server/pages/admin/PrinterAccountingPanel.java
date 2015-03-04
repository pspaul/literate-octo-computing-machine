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
package org.savapage.server.pages.admin;

import java.text.ParseException;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dto.IppMediaCostDto;
import org.savapage.core.dto.MediaPageCostDto;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.jpa.Printer;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class PrinterAccountingPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private static final AccountingService ACCOUNTING_SERVICE = ServiceContext
            .getServiceFactory().getAccountingService();

    private static final ProxyPrintService PROXYPRINT_SERVICE = ServiceContext
            .getServiceFactory().getProxyPrintService();

    private final boolean isVisible;

    /**
    *
    */
    private class MediaCostView extends PropertyListView<IppMediaCostDto> {

        /**
        *
        */
        private static final long serialVersionUID = 1L;

        private final MarkupHelper helper;
        private final boolean isColorPrinter;
        private final boolean isDuplexPrinter;

        private final Locale locale = getSession().getLocale();
        private final int fractionDigits = ConfigManager
                .getPrinterCostDecimals();

        /**
         *
         * @param id
         * @param list
         */
        public MediaCostView(String id, List<? extends IppMediaCostDto> list,
                final boolean isColorPrinter, final boolean isDuplexPrinter) {
            super(id, list);
            this.helper = new MarkupHelper(this);
            this.isColorPrinter = isColorPrinter;
            this.isDuplexPrinter = isDuplexPrinter;
        }

        /**
         *
         * @param item
         * @param wicketId
         * @param htmlId
         * @param plainStringCost
         * @throws ParseException
         */
        private void addCostCell(final ListItem<IppMediaCostDto> item,
                final String wicketId, final String plainStringCost,
                final boolean isActive, final boolean isEnabled) {

            final String plainCost;

            if (isEnabled) {
                plainCost = plainStringCost;
            } else {
                plainCost = "0.00";
            }

            final String cost;

            try {
                cost =
                        BigDecimalUtil.localize(
                                BigDecimalUtil.valueOf(plainCost),
                                fractionDigits, locale, false);
            } catch (ParseException e) {
                throw new SpException(e);
            }

            Label labelWrk = new Label(wicketId, "");

            labelWrk.add(new AttributeModifier("value", cost));
            labelWrk.add(new AttributeModifier("maxlength", "10"));

            if (!isActive) {
                labelWrk.add(new AttributeModifier("style", "display:none"));
            }

            if (!isEnabled) {
                labelWrk.add(new AttributeModifier("disabled", "disabled"));
            }

            item.add(labelWrk);
        }

        @Override
        protected void populateItem(final ListItem<IppMediaCostDto> item) {

            final IppMediaCostDto dto = item.getModelObject();

            Label labelWrk;

            /*
             * media (checkbox + label)
             */
            String htmlId =
                    ACCOUNTING_SERVICE.getCostMediaAttr(dto.getMedia())
                            .getKey();

            labelWrk =
                    MarkupHelper.createCheckbox("media-checkbox", htmlId,
                            dto.getActive() || dto.isDefault());

            // label
            String mediaMnemonic;

            if (dto.isDefault()) {
                mediaMnemonic = helper.localized("default");
                labelWrk.add(new AttributeModifier("disabled", "disabled"));
            } else {
                mediaMnemonic =
                        PROXYPRINT_SERVICE.localizeMnemonic(IppMediaSizeEnum
                                .findMediaSizeName(dto.getMedia()));
            }
            item.add(labelWrk);

            labelWrk = new Label("media-checkbox-label", mediaMnemonic);
            labelWrk.add(new AttributeModifier("for", htmlId));
            item.add(labelWrk);

            /*
            *
            */
            MediaPageCostDto cost;

            cost = dto.getPageCost().getCostOneSided();
            addCostCell(item, "one-sided-grayscale", cost.getCostGrayscale(),
                    dto.getActive(), true);
            addCostCell(item, "one-sided-color", cost.getCostColor(),
                    dto.getActive(), isColorPrinter);

            cost = dto.getPageCost().getCostTwoSided();
            addCostCell(item, "two-sided-grayscale", cost.getCostGrayscale(),
                    dto.getActive(), isDuplexPrinter);
            addCostCell(item, "two-sided-color", cost.getCostColor(),
                    dto.getActive(), isColorPrinter && isDuplexPrinter);
        }

    }

    /**
     *
     * @param id
     * @param visible
     */
    public PrinterAccountingPanel(String id, final boolean visible) {
        super(id);
        this.isVisible = visible;
    }

    @Override
    public final boolean isVisible() {
        return this.isVisible;
    }

    /**
     *
     */
    public void populate(final Printer printer) {

        final List<IppMediaCostDto> entryList =
                PROXYPRINT_SERVICE.getProxyPrinterCostMedia(printer);

        final boolean isColorPrinter =
                PROXYPRINT_SERVICE.isColorPrinter(printer.getPrinterName());
        final boolean isDuplexPrinter =
                PROXYPRINT_SERVICE.isDuplexPrinter(printer.getPrinterName());

        add(new MediaCostView("media-row", entryList, isColorPrinter,
                isDuplexPrinter));

    }

}
