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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dto.IppMediaCostDto;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.dto.MediaPageCostDto;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
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
 * @author Rijk Ravestein
 *
 */
public final class PrinterMediaSourcePanel extends Panel {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /** */
    private static final PrinterService PRINTER_SERVICE =
            ServiceContext.getServiceFactory().getPrinterService();

    /** */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final PrinterDao PRINTER_DAO =
            ServiceContext.getDaoContext().getPrinterDao();

    /** */
    private static final String DEFAULT_MARKER = "*";

    /** */
    private final boolean isVisible;

    /**
     *
     */
    private class MediaListView
            extends PropertyListView<JsonProxyPrinterOptChoice> {

        /** */
        private final String media;

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public MediaListView(final String id,
                final List<JsonProxyPrinterOptChoice> list,
                final String media) {

            super(id, list);
            this.media = media;
        }

        @Override
        protected void
                populateItem(final ListItem<JsonProxyPrinterOptChoice> item) {

            final JsonProxyPrinterOptChoice dto = item.getModelObject();

            final Label label =
                    new Label("media-source-media", dto.getUiText());

            label.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                    dto.getChoice()));

            if (dto.getChoice().equals(this.media)) {
                label.add(new AttributeModifier(MarkupHelper.ATTR_SELECTED,
                        MarkupHelper.ATTR_SELECTED));
            }

            item.add(label);
        }
    }

    /**
     *
     */
    private class MediaSourceListView
            extends PropertyListView<IppMediaSourceCostDto> {

        private static final long serialVersionUID = 1L;

        private final Locale locale = getSession().getLocale();
        private final int fractionDigits =
                ConfigManager.getPrinterCostDecimals();

        private final List<JsonProxyPrinterOptChoice> mediaList;

        private final boolean isColorPrinter;
        private final boolean isDuplexPrinter;
        private final boolean showCost;

        /**
         *
         * @param id
         * @param mediaSourceList
         * @param mediaList
         * @param isColorPrinter
         * @param isDuplexPrinter
         */
        public MediaSourceListView(final String id,
                final List<IppMediaSourceCostDto> mediaSourceList,
                List<JsonProxyPrinterOptChoice> mediaList,
                final boolean isColorPrinter, final boolean isDuplexPrinter,
                final boolean showCost) {

            super(id, mediaSourceList);
            this.mediaList = mediaList;
            this.isColorPrinter = isColorPrinter;
            this.isDuplexPrinter = isDuplexPrinter;
            this.showCost = showCost;
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
                cost = BigDecimalUtil.localize(
                        BigDecimalUtil.valueOf(plainCost), fractionDigits,
                        locale, false);

            } catch (ParseException e) {
                throw new SpException(e);
            }

            final Label labelWrk = new Label(wicketId, "");

            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_VALUE, cost));
            labelWrk.add(
                    new AttributeModifier(MarkupHelper.ATTR_MAXLENGTH, "10"));

            if (!isActive) {
                labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_DISABLED,
                        MarkupHelper.ATTR_DISABLED));
            }
            item.add(labelWrk);

            final MarkupHelper helper = new MarkupHelper(item);
            org.apache.wicket.Component cmp =
                    helper.addTransparant(wicketId.concat("-td"));

            if (!this.showCost) {
                MarkupHelper.appendComponentAttr(cmp, MarkupHelper.ATTR_STYLE,
                        "display:none;");
            }

        }

        @Override
        protected void
                populateItem(final ListItem<IppMediaSourceCostDto> item) {

            final IppMediaSourceCostDto dto = item.getModelObject();

            Label labelWrk;

            /*
             * media-source (checkbox + label + preferred)
             */
            String htmlId = ACCOUNTING_SERVICE
                    .getMediaSourceAttr(dto.getSource()).getKey();

            labelWrk = MarkupHelper.createCheckbox("media-source-checkbox",
                    htmlId, dto.getActive());

            item.add(labelWrk);

            //
            String mediaMnemonic = dto.getSource();

            labelWrk = new Label("media-source-checkbox-label", mediaMnemonic);
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_FOR, htmlId));
            item.add(labelWrk);

            //
            final String preferredIcon;

            if (BooleanUtils.isTrue(dto.getPreferred())) {
                preferredIcon = MarkupHelper.CSS_PREFERRED_ICON_ON;
            } else {
                preferredIcon = MarkupHelper.CSS_PREFERRED_ICON_OFF;
            }

            item.add(MarkupHelper.appendLabelAttr(
                    new Label("btn-preferred-switch", ""),
                    MarkupHelper.ATTR_CLASS, preferredIcon));

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

            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                    dto.getDisplay()));

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
     *            The printer.
     */
    public void populate(final Printer printer, final boolean showCost) {

        final List<IppMediaSourceCostDto> mediaSourceList = new ArrayList<>();

        IppMediaSourceCostDto dtoManual = null;

        final boolean isColorPrinter =
                PROXYPRINT_SERVICE.isColorPrinter(printer.getPrinterName());

        final boolean isDuplexPrinter =
                PROXYPRINT_SERVICE.isDuplexPrinter(printer.getPrinterName());

        final boolean hasMediaSourceAuto =
                PROXYPRINT_SERVICE.hasMediaSourceAuto(printer.getPrinterName());

        //
        for (final IppMediaSourceCostDto dto : PROXYPRINT_SERVICE
                .getProxyPrinterCostMediaSource(printer)) {

            if (dto.isAutoSource()) {
                continue;
            }

            if (dto.isManualSource()) {
                if (dtoManual == null) {
                    dtoManual = dto;
                }
                continue;
            }

            mediaSourceList.add(dto);
        }

        final MarkupHelper helper = new MarkupHelper(this);

        Label labelWrk;

        //
        final String signalMediaSourceAuto;

        if (hasMediaSourceAuto) {
            signalMediaSourceAuto =
                    String.format("%s%s", DEFAULT_MARKER, "auto");
        } else {
            signalMediaSourceAuto = "";
        }
        helper.encloseLabel("signal-media-source-auto", signalMediaSourceAuto,
                hasMediaSourceAuto);

        helper.addLabel("legend-source-preferred",
                AdjectiveEnum.PREFERRED.uiText(getLocale()).toLowerCase());

        /*
         * For now, ALWAYS hide media-source: auto
         */
        helper.discloseLabel("media-source-auto-display-name");

        /*
         * media-source: manual
         */
        if (dtoManual != null) {

            final String htmlId = ACCOUNTING_SERVICE
                    .getMediaSourceAttr(dtoManual.getSource()).getKey();

            // checkbox
            labelWrk =
                    MarkupHelper.createCheckbox("media-source-checkbox-manual",
                            htmlId, dtoManual.getActive());

            add(labelWrk);

            // label
            String mediaMnemonic = dtoManual.getSource();

            labelWrk = new Label("media-source-checkbox-manual-label",
                    mediaMnemonic);
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_FOR, htmlId));
            add(labelWrk);

            // display
            labelWrk = helper.encloseLabel("media-source-manual-display-name",
                    "", true);
            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                    dtoManual.getDisplay()));

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
                mediaList, isColorPrinter, isDuplexPrinter, showCost));

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

            isDefaultColor = IppKeyword.PRINT_COLOR_MODE_COLOR
                    .equalsIgnoreCase(lookup.get(ippKeyword).getDefchoiceIpp());

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

            final boolean isDefaultIppOneSided = IppKeyword.SIDES_ONE_SIDED
                    .equalsIgnoreCase(lookup.get(ippKeyword).getDefchoiceIpp());

            if (isDefaultIppOneSided) {
                oneSidedMark = DEFAULT_MARKER;
            } else {
                twoSidedMark = DEFAULT_MARKER;
            }
        }

        add(new Label("th-one-sided", oneSidedMark));
        add(new Label("th-two-sided", twoSidedMark));

        helper.addTransparantDisabled("th-two-sided-txt", !isDuplexPrinter);
        helper.addTransparantDisabled("th-color-1-txt", !isColorPrinter);
        helper.addTransparantDisabled("th-color-2-txt", !isColorPrinter);
        helper.addTransparantDisabled("th-grayscale-2-txt", !isDuplexPrinter);

        final String[] markerLabels = { "th-grayscale-1", "th-color-1",
                "th-grayscale-2", "th-color-2" };

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
        final boolean showMonochromeDefault = isColorPrinter && isDefaultColor;

        labelWrk = helper.encloseLabel("use-grayscale-as-default", "",
                showMonochromeDefault);

        if (showMonochromeDefault && printer.getAttributes() != null) {

            // Checked?
            final String colorModeDefault =
                    PRINTER_SERVICE.getPrintColorModeDefault(printer);

            if (colorModeDefault != null && colorModeDefault
                    .equalsIgnoreCase(IppKeyword.PRINT_COLOR_MODE_MONOCHROME)) {
                labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CHECKED,
                        MarkupHelper.ATTR_CHECKED));
            }
        }

        add(labelWrk);

        /*
         * Client-side monochrome conversion.
         */
        final boolean isJobTicketPrinter =
                PRINTER_SERVICE.isJobTicketPrinter(printer.getId());

        final boolean showClientSideMonochrome =
                isColorPrinter && !isJobTicketPrinter;

        labelWrk = helper.encloseLabel("client-side-monochrome-conversion", "",
                showClientSideMonochrome);

        if (showClientSideMonochrome && printer.getAttributes() != null) {
            // Checked?
            if (PRINTER_SERVICE.isClientSideMonochrome(printer)) {
                labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_CHECKED,
                        MarkupHelper.ATTR_CHECKED));
            }
        }
        add(labelWrk);

        //
        handleJobSheets(helper, printer, isJobTicketPrinter, mediaSourceList);
    }

    /**
     * @param printer
     *            The printer.
     * @return The media-sources for job sheets.
     */
    private static Set<String> getJobSheetSources(final Printer printer) {
        final Set<String> sources =
                PRINTER_SERVICE.getJobSheetsMediaSources(printer);
        if (sources == null) {
            return new HashSet<>();
        }
        return sources;
    }

    /**
     * Encloses selection of preferred media-source for
     * {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_JOB_SHEETS}.
     *
     * @param helper
     *            The helper.
     * @param printer
     *            The printer.
     * @param jobTicketPrinter
     *            {@code true} if job ticket printer.
     * @param mediaSourceList
     *            The list of media sources.
     */
    private void handleJobSheets(final MarkupHelper helper,
            final Printer printer, final boolean jobTicketPrinter,
            final List<IppMediaSourceCostDto> mediaSourceList) {

        final boolean enclose = !jobTicketPrinter
                && (PRINTER_DAO.isJobTicketRedirectPrinter(printer.getId())
                        || PROXYPRINT_SERVICE
                                .getCachedPrinter(printer.getPrinterName())
                                .hasJobSheets());

        helper.encloseLabel("job-sheets-label",
                PrintOutNounEnum.JOB_SHEET.uiText(getLocale(), true), enclose);

        if (!enclose) {
            return;
        }

        final Set<String> jobSheetSources = getJobSheetSources(printer);

        add(new PropertyListView<IppMediaSourceCostDto>(
                "job-sheets-media-source-select", mediaSourceList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void
                    populateItem(final ListItem<IppMediaSourceCostDto> item) {

                final IppMediaSourceCostDto dto = item.getModelObject();

                final Label label =
                        new Label("job-sheets-media-source", dto.getSource());

                label.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                        dto.getSource()));

                if (jobSheetSources.contains(dto.getSource())) {
                    label.add(new AttributeModifier(MarkupHelper.ATTR_SELECTED,
                            MarkupHelper.ATTR_SELECTED));
                }

                item.add(label);
            }
        });

    }
}
