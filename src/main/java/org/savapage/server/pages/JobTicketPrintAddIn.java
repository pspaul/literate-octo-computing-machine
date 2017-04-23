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
package org.savapage.server.pages;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.Printer;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.ext.papercut.PaperCutHelper;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;

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

    /**
     * .
     */
    private static final String PARM_SETTLE = "settle";

    /**
     * .
     */
    private static final String PARM_RETRY = "retry";

    /**
     * .
     */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    private static final String WICKET_ID_CHOICE = "choice";
    private static final String WICKET_ID_MEDIA_SOURCE = "media-source";

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

            MarkupHelper.modifyLabelAttr(labelWlk, "value", choice.getChoice());

            if (this.defaultChoice != null && choice.getChoice()
                    .equals(this.defaultChoice.getChoice())) {
                MarkupHelper.modifyLabelAttr(labelWlk, "selected", "selected");
            }

            item.add(labelWlk);
        }
    }

    /**
     * .
     */
    private static class RedirectPrinterListView
            extends PropertyListView<RedirectPrinterDto> {

        /**
         * .
         */
        private static final long serialVersionUID = 1L;

        /**
         *
         */
        private final boolean isSettlement;

        private int tabindexWlk;

        /**
         * @param id
         *            The wicket id.
         * @param list
         *            The item list.
         * @param settlement
         */
        RedirectPrinterListView(final String id,
                final List<RedirectPrinterDto> list, final boolean settlement) {
            super(id, list);
            this.isSettlement = settlement;
            this.tabindexWlk = 9;
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
            MarkupHelper.modifyLabelAttr(labelWlk, "for", id);
            MarkupHelper.modifyLabelAttr(labelWlk, "tabindex",
                    String.valueOf(++tabindexWlk));
            item.add(labelWlk);

            //
            labelWlk = new Label("input", "");
            MarkupHelper.modifyLabelAttr(labelWlk, "id", id);
            MarkupHelper.modifyLabelAttr(labelWlk, "value",
                    printer.getId().toString());
            if (printer.isPreferred()) {
                MarkupHelper.modifyLabelAttr(labelWlk, "checked", "checked");
            }
            item.add(labelWlk);

            //
            final MarkupHelper helper = new MarkupHelper(item);

            if (this.isSettlement) {
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
                        mediaTypeOptChoice.getUiText(), "value",
                        mediaTypeOptChoice.getChoice());
            }

            //
            final Printer dbPrinter = ServiceContext.getDaoContext()
                    .getPrinterDao().findById(item.getModelObject().getId());

            item.add(new PrinterOptListView(WICKET_ID_MEDIA_SOURCE,
                    filterMediaSourcesForUser(new PrinterAttrLookup(dbPrinter),
                            printer.getMediaSourceOpt().getChoices()),
                    printer.getMediaSourceOptChoice()));

            //
            final JsonProxyPrinterOpt outputBinOpt = printer.getOutputBinOpt();

            if (outputBinOpt == null) {
                helper.discloseLabel(WICKET_ID_OUTPUT_BIN);
                helper.discloseLabel(WICKET_ID_JOG_OFFSET);
            } else {
                item.add(new PrinterOptListView(WICKET_ID_OUTPUT_BIN,
                        outputBinOpt.getChoices(),
                        printer.getOutputBinOptChoice()));

                final JsonProxyPrinterOpt jogOffsetOpt =
                        printer.getJogOffsetOpt();

                if (jogOffsetOpt == null) {
                    helper.discloseLabel(WICKET_ID_JOG_OFFSET);
                } else {
                    item.add(new PrinterOptListView(WICKET_ID_JOG_OFFSET,
                            jogOffsetOpt.getChoices(),
                            printer.getJogOffsetOptChoice()));
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

        final String jobFileName = this.getParmValue(PARM_JOBFILENAME);

        if (StringUtils.isBlank(jobFileName)) {
            setResponsePage(new MessageContent(AppLogLevelEnum.ERROR, String
                    .format("\"%s\" parameter missing", PARM_JOBFILENAME)));
        }

        final boolean isSettlement = this.getParmBoolean(PARM_SETTLE, false);
        final boolean isRetry = this.getParmBoolean(PARM_RETRY, false);

        //
        final List<RedirectPrinterDto> printerList;

        try {
            printerList = JOBTICKET_SERVICE.getRedirectPrinters(jobFileName,
                    IppOptionMap.createVoid(), getLocale());
        } catch (Exception e) {
            setResponsePage(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));
            return;
        }

        if (printerList == null) {
            setResponsePage(new MessageContent(AppLogLevelEnum.WARN,
                    localized("msg-jobticket-not-found")));
            return;
        }

        final MarkupHelper helper = new MarkupHelper(this);

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
                isSettlement));
    }

}
