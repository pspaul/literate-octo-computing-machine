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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dto.IppMediaCostDto;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.dto.MediaPageCostDto;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.jpa.Printer;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class PrinterMediaSourcePanel extends Panel {

    private static final long serialVersionUID = 1L;

    private static final AccountingService ACCOUNTING_SERVICE = ServiceContext
            .getServiceFactory().getAccountingService();

    private static final PrinterService PRINTER_SERVICE = ServiceContext
            .getServiceFactory().getPrinterService();

    private static final ProxyPrintService PROXYPRINT_SERVICE = ServiceContext
            .getServiceFactory().getProxyPrintService();

    private static final String DEFAULT_MARKER = "*";

    private final boolean isVisible;

    /**
     *
     */
    private class MediaListView extends
            PropertyListView<JsonProxyPrinterOptChoice> {

        private final String media;

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public MediaListView(String id,
                List<? extends JsonProxyPrinterOptChoice> list,
                final String media) {

            super(id, list);
            this.media = media;
        }

        @Override
        protected void populateItem(ListItem<JsonProxyPrinterOptChoice> item) {

            final JsonProxyPrinterOptChoice dto = item.getModelObject();

            final Label label = new Label("media-source-media", dto.getText());

            label.add(new AttributeModifier("value", dto.getChoice()));

            if (dto.getChoice().equals(this.media)) {
                label.add(new AttributeModifier("selected", "selected"));
            }

            item.add(label);
        }
    }

    /**
     *
     */
    private class MediaSourceListView extends
            PropertyListView<IppMediaSourceCostDto> {

        private static final long serialVersionUID = 1L;

        private final Locale locale = getSession().getLocale();
        private final int fractionDigits = ConfigManager
                .getPrinterCostDecimals();

        private final List<JsonProxyPrinterOptChoice> mediaList;

        private final boolean isColorPrinter;
        private final boolean isDuplexPrinter;

        /**
         *
         * @param id
         * @param mediaSourceList
         * @param mediaList
         * @param isColorPrinter
         * @param isDuplexPrinter
         */
        public MediaSourceListView(final String id,
                final List<? extends IppMediaSourceCostDto> mediaSourceList,
                List<JsonProxyPrinterOptChoice> mediaList,
                final boolean isColorPrinter, final boolean isDuplexPrinter) {

            super(id, mediaSourceList);
            this.mediaList = mediaList;
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
        private void addCostCell(final ListItem<IppMediaSourceCostDto> item,
                final String wicketId, final String plainStringCost,
                final boolean isActive) {

            final String plainCost;

            if (isActive) {
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
                // /labelWrk.add(new AttributeModifier("style",
                // "display:none"));
                labelWrk.add(new AttributeModifier("disabled", "disabled"));
            }

            item.add(labelWrk);

        }

        @Override
        protected void populateItem(final ListItem<IppMediaSourceCostDto> item) {

            final IppMediaSourceCostDto dto = item.getModelObject();

            Label labelWrk;

            /*
             * media-source (checkbox + label)
             */
            String htmlId =
                    ACCOUNTING_SERVICE.getMediaSourceAttr(dto.getSource())
                            .getKey();

            labelWrk =
                    MarkupHelper.createCheckbox("media-source-checkbox",
                            htmlId, dto.getActive());

            item.add(labelWrk);

            /*
             * label
             */
            String mediaMnemonic = dto.getSource();

            labelWrk = new Label("media-source-checkbox-label", mediaMnemonic);
            labelWrk.add(new AttributeModifier("for", htmlId));
            item.add(labelWrk);

            /*
             * Media cost.
             */
            final IppMediaCostDto mediaCost = dto.getMedia();

            MediaPageCostDto cost;

            cost = mediaCost.getPageCost().getCostOneSided();
            addCostCell(item, "one-sided-grayscale", cost.getCostGrayscale(),
                    true);
            addCostCell(item, "one-sided-color", cost.getCostColor(),
                    isColorPrinter);

            cost = mediaCost.getPageCost().getCostTwoSided();
            addCostCell(item, "two-sided-grayscale", cost.getCostGrayscale(),
                    isDuplexPrinter);
            addCostCell(item, "two-sided-color", cost.getCostColor(),
                    isColorPrinter && isDuplexPrinter);

            /*
             * Media select
             */
            item.add(new MediaListView("media-source-media-select", mediaList,
                    dto.getMedia().getMedia()));

            /*
             * Display name.
             */
            labelWrk = new Label("media-source-display-name", "");

            labelWrk.add(new AttributeModifier("value", dto.getDisplay()));

            item.add(labelWrk);
        }

    }

    /**
     *
     * @param id
     * @param visible
     */
    public PrinterMediaSourcePanel(final String id, final boolean visible) {
        super(id);
        this.isVisible = visible;
    }

    @Override
    public final boolean isVisible() {
        return this.isVisible;
    }

    /**
     *
     * @param printer
     */
    public void populate(final Printer printer) {

        final List<IppMediaSourceCostDto> mediaSourceList = new ArrayList<>();

        IppMediaSourceCostDto dtoAuto = null;
        IppMediaSourceCostDto dtoManual = null;

        final boolean isColorPrinter =
                PROXYPRINT_SERVICE.isColorPrinter(printer.getPrinterName());
        final boolean isDuplexPrinter =
                PROXYPRINT_SERVICE.isDuplexPrinter(printer.getPrinterName());
        final boolean hasMediaSourceAuto =
                PROXYPRINT_SERVICE.hasMediaSourceAuto(printer.getPrinterName());

        for (final IppMediaSourceCostDto dto : PROXYPRINT_SERVICE
                .getProxyPrinterCostMediaSource(printer)) {

            final boolean isAuto = dto.isAutoSource();
            final boolean isManual = dto.isManualSource();

            if (dtoAuto == null && isAuto) {
                dtoAuto = dto;
            } else if (dtoManual == null && isManual) {
                dtoManual = dto;
            } else if (!isAuto && !isManual) {
                /*
                 * Believe it or not, some printers report 'auto' twice :-)
                 */
                mediaSourceList.add(dto);
            }
        }

        final MarkupHelper helper = new MarkupHelper(this);

        Label labelWrk;

        //
        final String signalMediaSourecAuto;

        if (hasMediaSourceAuto) {
            signalMediaSourecAuto = DEFAULT_MARKER + "auto";
        } else {
            signalMediaSourecAuto = "";
        }
        helper.addLabel("signal-media-source-auto", signalMediaSourecAuto);

        /*
         * For now, ALWAYS hide media-source: auto
         */
        helper.discloseLabel("media-source-auto-display-name");

        /*
         * media-source: manual
         */
        if (dtoManual != null) {

            final String htmlId =
                    ACCOUNTING_SERVICE
                            .getMediaSourceAttr(dtoManual.getSource()).getKey();

            // checkbox
            labelWrk =
                    MarkupHelper.createCheckbox("media-source-checkbox-manual",
                            htmlId, dtoManual.getActive());

            add(labelWrk);

            // label
            String mediaMnemonic = dtoManual.getSource();

            labelWrk =
                    new Label("media-source-checkbox-manual-label",
                            mediaMnemonic);
            labelWrk.add(new AttributeModifier("for", htmlId));
            add(labelWrk);

            // display
            labelWrk =
                    helper.encloseLabel("media-source-manual-display-name", "",
                            true);
            labelWrk.add(new AttributeModifier("value", dtoManual.getDisplay()));

        } else {
            helper.discloseLabel("media-source-manual-display-name");
        }

        /*
         * Get the media list for this printer.
         */
        final List<JsonProxyPrinterOptChoice> mediaList =
                PROXYPRINT_SERVICE.getMediaChoices(printer.getPrinterName(),
                        getSession().getLocale());

        add(new MediaSourceListView("media-source-row", mediaSourceList,
                mediaList, isColorPrinter, isDuplexPrinter));

        /*
         * Mark the defaults in the header.
         */
        final Map<String, JsonProxyPrinterOpt> lookup =
                PROXYPRINT_SERVICE.getOptionsLookup(printer.getPrinterName());

        String ippKeyword;

        // print-color-mode
        ippKeyword = IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE;
        String grayscaleMark = "";
        String colorMark = "";

        final boolean isDefaultColor;

        if (lookup.containsKey(ippKeyword)) {

            isDefaultColor =
                    IppKeyword.PRINT_COLOR_MODE_COLOR.equalsIgnoreCase(lookup
                            .get(ippKeyword).getDefchoiceIpp());

            if (isDefaultColor) {
                colorMark = DEFAULT_MARKER;
            } else {
                grayscaleMark = DEFAULT_MARKER;
            }
        } else {
            isDefaultColor = false;
        }

        // sides
        ippKeyword = IppDictJobTemplateAttr.ATTR_SIDES;
        String oneSidedMark = "";
        String twoSidedMark = "";

        if (lookup.containsKey(ippKeyword)) {

            final boolean isDefaultIppOneSided =
                    IppKeyword.SIDES_ONE_SIDED.equalsIgnoreCase(lookup.get(
                            ippKeyword).getDefchoiceIpp());

            if (isDefaultIppOneSided) {
                oneSidedMark = DEFAULT_MARKER;
            } else {
                twoSidedMark = DEFAULT_MARKER;
            }
        }

        add(new Label("th-one-sided", oneSidedMark));
        add(new Label("th-two-sided", twoSidedMark));

        final String[] markerLabels =
                { "th-grayscale-1", "th-color-1", "th-grayscale-2",
                        "th-color-2" };

        final String[] markers;

        if (oneSidedMark.equals(DEFAULT_MARKER)) {
            markers = new String[] { grayscaleMark, colorMark, "", "" };
        } else {
            markers = new String[] { "", "", grayscaleMark, colorMark };
        }

        for (int i = 0; i < markerLabels.length; i++) {
            add(new Label(markerLabels[i], markers[i]));
        }

        /*
         * Override 'color' with monochrome?
         */
        final String labelDefaultMonochrome = "use-grayscale-as-default";

        final boolean showMonochromeDefault = isColorPrinter && isDefaultColor;
        labelWrk =
                helper.encloseLabel(labelDefaultMonochrome, "",
                        showMonochromeDefault);

        // Checked?
        if (showMonochromeDefault && printer.getAttributes() != null) {

            final String colorModeDefault =
                    PRINTER_SERVICE.getPrintColorModeDefault(printer);

            if (colorModeDefault != null
                    && colorModeDefault
                            .equalsIgnoreCase(IppKeyword.PRINT_COLOR_MODE_MONOCHROME)) {
                labelWrk.add(new AttributeModifier("checked", "checked"));
            }
        }

        //
        add(labelWrk);

    }
}
