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
package org.savapage.server.pages;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.dto.RedirectPrinterDto;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.Printer;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.print.proxy.TicketJobSheetDto;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.ext.papercut.PaperCutHelper;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.session.JobTicketSession;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketPrintAddIn extends JobTicketAddInBase {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(JobTicketPrintAddIn.class);

    /** */
    private static final String PARM_SETTLE = "settle";

    /** */
    private static final String PARM_RETRY = "retry";

    private static final String WICKET_ID_CHOICE = "choice";

    private static final String WICKET_ID_MEDIA_SOURCE = "media-source";
    private static final String WICKET_ID_MEDIA_SOURCE_JOB_SHEET =
            WICKET_ID_MEDIA_SOURCE + "-job-sheet";

    private static final String WICKET_ID_OUTPUT_BIN = "output-bin";
    private static final String WICKET_ID_JOG_OFFSET = "jog-offset";
    private static final String WICKET_ID_MEDIA_TYPE = "media-type";

    /**
     * .
     */
    private static class PrinterOptListView
            extends PropertyListView<JsonProxyPrinterOptChoice> {

        private static final long serialVersionUID = 1L;

        /**
         * The default media-source choice.
         */
        private final JsonProxyPrinterOptChoice defaultChoice;

        /**
         * @param id
         *            The wicket id.
         * @param list
         *            The item list.
         * @param choice
         *            The default choice.
         */
        PrinterOptListView(final String id,
                final List<JsonProxyPrinterOptChoice> list,
                final JsonProxyPrinterOptChoice choice) {
            super(id, list);
            this.defaultChoice = choice;
        }

        @Override
        protected void
                populateItem(final ListItem<JsonProxyPrinterOptChoice> item) {

            final JsonProxyPrinterOptChoice choice = item.getModelObject();

            final Label labelWlk =
                    new Label(WICKET_ID_CHOICE, choice.getUiText());

            MarkupHelper.modifyLabelAttr(labelWlk, MarkupHelper.ATTR_VALUE,
                    choice.getChoice());

            if (this.defaultChoice != null && choice.getChoice()
                    .equals(this.defaultChoice.getChoice())) {
                MarkupHelper.modifyLabelAttr(labelWlk,
                        MarkupHelper.ATTR_SELECTED, MarkupHelper.ATTR_SELECTED);
            }

            item.add(labelWlk);
        }
    }

    /**
     * .
     */
    private static class RedirectPrinterListView
            extends PropertyListView<RedirectPrinterDto> {

        /** */
        private static final long serialVersionUID = 1L;

        /** */
        private final boolean isSettlement;

        /** */
        private final TicketJobSheetDto jobSheetDto;

        /** */
        private final int printerListSize;

        /** */
        private int tabindexWlk;

        /**
         * @param id
         *            The wicket id.
         * @param list
         *            The item list.
         * @param settlement
         */
        RedirectPrinterListView(final String id,
                final List<RedirectPrinterDto> list, final boolean settlement,
                final TicketJobSheetDto jobSheet) {

            super(id, list);
            this.isSettlement = settlement;
            this.jobSheetDto = jobSheet;
            this.tabindexWlk = 9;
            this.printerListSize = list.size();

            final Long lastRedirectPrinterId = getLastRedirectPrinterId();

            if (lastRedirectPrinterId != null
                    && isPrinterOnList(list, lastRedirectPrinterId)) {
                for (final RedirectPrinterDto dto : list) {
                    dto.setPreferred(dto.getId().equals(lastRedirectPrinterId));
                }
            }
        }

        /**
         * @return Printer ID (DB primary key) of last selected redirect
         *         printer, or {@code null} if not present.
         */
        private static Long getLastRedirectPrinterId() {

            final JobTicketSession session =
                    SpSession.get().getJobTicketSession();

            if (session != null && session.getLastRedirectPrinterId() != null) {
                return session.getLastRedirectPrinterId();
            }
            return null;
        }

        /**
         * Checks if printer is on the list.
         *
         * @param list
         *            Printer list.
         * @param printerId
         *            Printer ID (DB primary key).
         * @return {@code true} if printerId is part of the list.
         */
        private static boolean isPrinterOnList(
                final List<RedirectPrinterDto> list, final Long printerId) {

            for (final RedirectPrinterDto dto : list) {
                if (dto.getId().equals(printerId)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Transforms a vanilla text to an HTMl string with non-breaking hyphens
         * and spaces.
         *
         * @param vanilla
         *            The vanilla text.
         * @return The HTML string.
         */
        private String asHtml(final String vanilla) {
            return StringUtils.replace(
                    StringUtils.replace(vanilla, "-", "&#8209;"), " ",
                    "&nbsp;");
        }

        /**
         * Gets the last {@link JobTicketSession.PrinterOpt} choice from server
         * session.
         *
         * @param printerId
         *            The printer primary database key.
         * @param printerOpt
         *            The {@link JobTicketSession.PrinterOpt}.
         * @param choices
         *            The list of options.
         * @param dfltChoice
         *            The default choice when not found in session.
         * @return The last choice.
         */
        private JsonProxyPrinterOptChoice getLastIppChoice(final Long printerId,
                final JobTicketSession.PrinterOpt printerOpt,
                final ArrayList<JsonProxyPrinterOptChoice> choices,
                final JsonProxyPrinterOptChoice dfltChoice) {

            final JobTicketSession session =
                    SpSession.get().getJobTicketSession();

            if (session == null) {
                return dfltChoice;
            }

            final Map<Long, Map<JobTicketSession.PrinterOpt, String>> opts =
                    session.getRedirectPrinterOptions();

            if (opts != null && opts.containsKey(printerId)) {

                final String lastChoice = opts.get(printerId).get(printerOpt);

                if (lastChoice == null) {
                    return dfltChoice;
                }

                for (final JsonProxyPrinterOptChoice choice : choices) {
                    if (choice.getChoice().equals(lastChoice)) {
                        return choice;
                    }
                }
            }
            return dfltChoice;
        }

        /**
         * Gets the last
         * {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_FINISHINGS_JOG_OFFSET}
         * choice from server session.
         *
         * @param choices
         * @param dfltChoice
         *            The default choice when not found in session.
         * @return The last choice.
         */
        private JsonProxyPrinterOptChoice getLastJogOffsetChoice(
                final ArrayList<JsonProxyPrinterOptChoice> choices,
                final JsonProxyPrinterOptChoice dfltChoice) {

            final JobTicketSession session =
                    SpSession.get().getJobTicketSession();

            if (session != null
                    && StringUtils.isNotBlank(session.getJogOffsetOption())) {
                for (final JsonProxyPrinterOptChoice choice : choices) {
                    if (choice.getChoice()
                            .equals(session.getJogOffsetOption())) {
                        return choice;
                    }
                }
            }
            return dfltChoice;
        }

        @Override
        protected void populateItem(final ListItem<RedirectPrinterDto> item) {

            final RedirectPrinterDto printer = item.getModelObject();

            final String imgHtml;

            if (PaperCutHelper
                    .isPaperCutPrinter(URI.create(printer.getDeviceUri()))) {
                imgHtml = String.format(
                        "<img src=\"%s\" height=\"12\"/>&nbsp;&nbsp;",
                        WebApp.getThirdPartyEnumImgUrl(
                                ThirdPartyEnum.PAPERCUT));
            } else {
                imgHtml = "";
            }

            final String id = UUID.randomUUID().toString();

            Label labelWlk;

            //
            labelWlk = new Label("label",
                    String.format("%s%s", imgHtml, asHtml(printer.getName())));
            labelWlk.setEscapeModelStrings(false);
            MarkupHelper.modifyLabelAttr(labelWlk, MarkupHelper.ATTR_FOR, id);
            MarkupHelper.modifyLabelAttr(labelWlk, MarkupHelper.ATTR_TABINDEX,
                    String.valueOf(++tabindexWlk));
            item.add(labelWlk);

            //
            labelWlk = new Label("input", "");
            MarkupHelper.modifyLabelAttr(labelWlk, MarkupHelper.ATTR_ID, id);
            MarkupHelper.modifyLabelAttr(labelWlk, MarkupHelper.ATTR_VALUE,
                    printer.getId().toString());
            if (printer.isPreferred() || this.printerListSize == 1) {
                MarkupHelper.modifyLabelAttr(labelWlk,
                        MarkupHelper.ATTR_CHECKED, MarkupHelper.ATTR_CHECKED);
            }
            item.add(labelWlk);

            //
            final MarkupHelper helper = new MarkupHelper(item);

            if (this.isSettlement) {
                helper.discloseLabel(WICKET_ID_MEDIA_SOURCE_JOB_SHEET);
                helper.discloseLabel(WICKET_ID_MEDIA_SOURCE);
                helper.discloseLabel(WICKET_ID_OUTPUT_BIN);
                helper.discloseLabel(WICKET_ID_JOG_OFFSET);
                helper.discloseLabel(WICKET_ID_MEDIA_TYPE);
                return;
            }

            //
            final JsonProxyPrinterOptChoice mediaTypeOptChoice =
                    printer.getMediaTypeOptChoice();

            if (mediaTypeOptChoice == null) {
                helper.discloseLabel(WICKET_ID_MEDIA_TYPE);
            } else {
                helper.addModifyLabelAttr(WICKET_ID_MEDIA_TYPE,
                        mediaTypeOptChoice.getUiText(), MarkupHelper.ATTR_VALUE,
                        mediaTypeOptChoice.getChoice());
            }

            //
            final Printer dbPrinter = ServiceContext.getDaoContext()
                    .getPrinterDao().findById(item.getModelObject().getId());

            final PrinterAttrLookup printerAttrLookup =
                    new PrinterAttrLookup(dbPrinter);

            final List<JsonProxyPrinterOptChoice> filteredMediaSourcesForUser =
                    filterMediaSourcesForUser(printerAttrLookup,
                            printer.getMediaSourceOpt().getChoices());

            if (this.jobSheetDto == null || !this.jobSheetDto.isEnabled()) {
                helper.discloseLabel(WICKET_ID_MEDIA_SOURCE_JOB_SHEET);
            } else {
                item.add(new PrinterOptListView(
                        WICKET_ID_MEDIA_SOURCE_JOB_SHEET,
                        filteredMediaSourcesForUser,
                        this.getLastIppChoice(printer.getId(),
                                JobTicketSession.PrinterOpt.MEDIA_SOURCE_SHEET,
                                printer.getMediaSourceOpt().getChoices(),
                                printer.getMediaSourceJobSheetOptChoice())));
            }

            final JsonProxyPrinterOptChoice lastMediaSourceChoice =
                    this.getLastIppChoice(printer.getId(),
                            JobTicketSession.PrinterOpt.MEDIA_SOURCE,
                            printer.getMediaSourceOpt().getChoices(),
                            printer.getMediaSourceOptChoice());

            /*
             * Check if lastMediaSourceChoice contains requested media, if not,
             * take printer.getMediaSourceOptChoice() as default media source.
             */
            JsonProxyPrinterOptChoice dfltMediaSourceChoice =
                    printer.getMediaSourceOptChoice();

            if (lastMediaSourceChoice != null
                    && printer.getMediaSourceOptChoice() != null) {

                final String mediaChoiceLast = printer.getMediaSourceMediaMap()
                        .get(lastMediaSourceChoice.getChoice());

                final String mediaChoiceCurr = printer.getMediaSourceMediaMap()
                        .get(dfltMediaSourceChoice.getChoice());

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(
                            "Last choice [{}] [{}] Default choice [{}] [{}]",
                            lastMediaSourceChoice.getChoice(), mediaChoiceLast,
                            dfltMediaSourceChoice.getChoice(), mediaChoiceCurr);
                }

                if (mediaChoiceLast.equals(mediaChoiceCurr)) {
                    dfltMediaSourceChoice = lastMediaSourceChoice;
                }
            }

            item.add(new PrinterOptListView(WICKET_ID_MEDIA_SOURCE,
                    filteredMediaSourcesForUser, dfltMediaSourceChoice));

            //
            final JsonProxyPrinterOpt outputBinOpt = printer.getOutputBinOpt();

            if (outputBinOpt == null) {
                helper.discloseLabel(WICKET_ID_OUTPUT_BIN);
                helper.discloseLabel(WICKET_ID_JOG_OFFSET);
            } else {
                item.add(new PrinterOptListView(WICKET_ID_OUTPUT_BIN,
                        outputBinOpt.getChoices(),
                        this.getLastIppChoice(printer.getId(),
                                JobTicketSession.PrinterOpt.OUTPUT_BIN,
                                outputBinOpt.getChoices(),
                                printer.getOutputBinOptChoice())));

                final JsonProxyPrinterOpt jogOffsetOpt =
                        printer.getJogOffsetOpt();

                if (jogOffsetOpt == null) {
                    helper.discloseLabel(WICKET_ID_JOG_OFFSET);
                } else {
                    item.add(new PrinterOptListView(WICKET_ID_JOG_OFFSET,
                            jogOffsetOpt.getChoices(),
                            getLastJogOffsetChoice(jogOffsetOpt.getChoices(),
                                    printer.getJogOffsetOptChoice())));
                }
            }
        }

        /**
         * Filters the active (configured) media-source choices of a
         * {@link Printer} for user display, and sets the UI text for each
         * filtered choice.
         *
         * @param lookup
         *            The attribute lookup for the printer.
         * @param mediaSourceChoices
         *            The IPP media-source choices of the printer.
         * @return The filtered choices.
         */
        private List<JsonProxyPrinterOptChoice> filterMediaSourcesForUser(
                final PrinterAttrLookup lookup,
                final List<JsonProxyPrinterOptChoice> mediaSourceChoices) {

            final List<JsonProxyPrinterOptChoice> prunedList =
                    new ArrayList<>();

            final Iterator<JsonProxyPrinterOptChoice> iterChoices =
                    mediaSourceChoices.iterator();

            while (iterChoices.hasNext()) {

                final JsonProxyPrinterOptChoice optChoice = iterChoices.next();

                final PrinterDao.MediaSourceAttr mediaSourceAttr =
                        new PrinterDao.MediaSourceAttr(optChoice.getChoice());

                final String json = lookup.get(mediaSourceAttr.getKey());

                if (json == null) {
                    continue;
                }

                try {
                    final IppMediaSourceCostDto dto =
                            IppMediaSourceCostDto.create(json);

                    if (dto.getActive()) {

                        optChoice.setUiText(dto.getDisplay());

                        if (dto.getMedia() != null) {
                            prunedList.add(optChoice);
                        }
                    }
                } catch (IOException e) {
                    // be forgiving
                    // LOGGER.error(e.getMessage());
                }
            }
            return prunedList;
        }

    }

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    public JobTicketPrintAddIn(final PageParameters parameters) {

        super(parameters);

        final String jobFileName = this.getJobFileName();

        final TicketJobSheetDto jobSheetDto;
        final List<RedirectPrinterDto> printerList;

        final MarkupHelper helper = new MarkupHelper(this);

        if (StringUtils.isBlank(jobFileName)) {

            setResponsePage(new MessageContent(AppLogLevelEnum.ERROR, String
                    .format("\"%s\" parameter missing", PARM_JOBFILENAME)));
            printerList = null;
            jobSheetDto = null;

        } else {

            final OutboxJobDto job = JOBTICKET_SERVICE.getTicket(jobFileName);

            if (job == null) {
                setResponsePage(JobTicketNotFound.class);
                helper.discloseLabel("printer-radio");
                return;
            }

            jobSheetDto = JOBTICKET_SERVICE
                    .getTicketJobSheet(job.createIppOptionMap());

            printerList = JOBTICKET_SERVICE.getRedirectPrinters(job,
                    IppOptionMap.createVoid(), getLocale());
        }

        if (printerList == null) {
            setResponsePage(JobTicketNotFound.class);
            helper.discloseLabel("printer-radio");
            return;
        }

        final boolean isSettlement = this.getParmBoolean(PARM_SETTLE, false);
        final boolean isRetry = this.getParmBoolean(PARM_RETRY, false);

        //
        final String prompt;
        final Label labelButton;

        if (isSettlement) {
            prompt = localized("prompt-header-settle");
            helper.discloseLabel("btn-print");
            helper.discloseLabel("btn-print-retry");
            labelButton = MarkupHelper.createEncloseLabel("btn-settle",
                    HtmlButtonEnum.SETTLE.uiText(getLocale()), true);
        } else {
            prompt = localized("prompt-header-print");
            helper.discloseLabel("btn-settle");

            if (isRetry) {
                labelButton = MarkupHelper.createEncloseLabel("btn-print-retry",
                        HtmlButtonEnum.PRINT.uiText(getLocale()), true);
                helper.discloseLabel("btn-print");
            } else {
                labelButton = MarkupHelper.createEncloseLabel("btn-print",
                        HtmlButtonEnum.PRINT.uiText(getLocale()), true);
                helper.discloseLabel("btn-print-retry");
            }
        }

        MarkupHelper.modifyLabelAttr(labelButton,
                MarkupHelper.ATTR_DATA_SAVAPAGE, jobFileName);
        add(labelButton);

        //
        add(new Label("btn-cancel", HtmlButtonEnum.CANCEL.uiText(getLocale())));

        add(new Label("prompt-header", prompt));

        //
        add(new RedirectPrinterListView("printer-radio", printerList,
                isSettlement, jobSheetDto));
    }

}
