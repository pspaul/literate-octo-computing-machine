/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.json.JsonPrinterDetail;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.print.proxy.JsonProxyPrinterOptGroup;
import org.savapage.core.services.DocStoreService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrintScalingEnum;
import org.savapage.core.util.MediaUtils;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketEditAddIn extends JobTicketAddInBase {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final DocStoreService DOC_STORE_SERVICE =
            ServiceContext.getServiceFactory().getDocStoreService();

    /** */
    private static final JobTicketService JOB_TICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /**
     * .
     */
    private static class PrinterOptionsView
            extends PropertyListView<JsonProxyPrinterOpt> {

        /**
         * .
         */
        private static final long serialVersionUID = 1L;

        private final OutboxJobDto jobTicket;

        private final boolean isReopenedTicket;

        /**
         *
         * @param id
         *            The wicket id.
         * @param list
         *            The item list.
         * @param job
         *            The job.
         * @param reopenedTicket
         *            If {@code true}, this ticket is reopened.
         */
        PrinterOptionsView(final String id,
                final List<JsonProxyPrinterOpt> list, final OutboxJobDto job,
                final boolean reopenedTicket) {

            super(id, list);
            this.jobTicket = job;
            this.isReopenedTicket = reopenedTicket;
        }

        @Override
        protected void populateItem(final ListItem<JsonProxyPrinterOpt> item) {

            final JsonProxyPrinterOpt printerOption = item.getModelObject();

            final String id = UUID.randomUUID().toString();

            Label labelWlk;

            final Fragment optionFragment =
                    new Fragment("ipp-option-div", "ipp-option-select", this);

            optionFragment
                    .add(new AttributeModifier(MarkupHelper.ATTR_ID, "todo"));

            labelWlk = new Label("label", printerOption.getUiText());
            MarkupHelper.modifyLabelAttr(labelWlk, "for", id);

            if (this.isReopenedTicket) {
                MarkupHelper.appendLabelAttr(labelWlk, MarkupHelper.ATTR_CLASS,
                        MarkupHelper.CSS_TXT_INFO);
            }

            optionFragment.add(labelWlk);

            // How to to this the proper wicket:fragment way?
            final StringBuilder choices = new StringBuilder();

            final String ticketChoice = this.jobTicket.getOptionValues()
                    .get(printerOption.getKeyword());

            for (final JsonProxyPrinterOptChoice choice : printerOption
                    .getChoices()) {

                choices.append("<option value=\"").append(choice.getChoice())
                        .append("\"");

                if (ticketChoice != null
                        && choice.getChoice().equals(ticketChoice)) {
                    choices.append(" selected");
                }
                choices.append(">").append(choice.getUiText())
                        .append("</option>");
            }

            labelWlk = new Label("select", choices);
            labelWlk.setEscapeModelStrings(false);

            MarkupHelper.modifyLabelAttr(labelWlk, MarkupHelper.ATTR_ID, id);
            MarkupHelper.modifyLabelAttr(labelWlk,
                    MarkupHelper.ATTR_DATA_SAVAPAGE,
                    printerOption.getKeyword());

            if (this.isReopenedTicket) {
                MarkupHelper.modifyLabelAttr(labelWlk,
                        MarkupHelper.ATTR_DISABLED, MarkupHelper.ATTR_DISABLED);
            }

            optionFragment.add(labelWlk);

            item.add(optionFragment);
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
    public JobTicketEditAddIn(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        final OutboxJobDto job = this.getJobTicket();

        if (job == null) {
            helper.discloseLabel("btn-save");
            return;
        }

        final boolean isReopenedTicket =
                JOB_TICKET_SERVICE.isReopenedTicket(job);

        final JsonPrinterDetail printer =
                PROXY_PRINT_SERVICE.getPrinterDetailUserCopy(
                        getSession().getLocale(), job.getPrinter(), true);

        final List<JsonProxyPrinterOpt> optionList = new ArrayList<>();
        JsonProxyPrinterOpt optionScaling = this.createPrintScalingOpt(job);

        for (final JsonProxyPrinterOptGroup group : printer.getGroups()) {
            for (final JsonProxyPrinterOpt option : group.getOptions()) {
                if (option.getKeyword()
                        .equals(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE)) {
                    continue;
                }
                optionList.add(option);
                if (option.getKeyword()
                        .equals(IppDictJobTemplateAttr.ATTR_MEDIA)) {
                    optionList.add(optionScaling);
                    optionScaling = null; // done
                }
            }
        }

        if (optionScaling != null) {
            optionList.add(optionScaling);
        }

        //
        add(new PrinterOptionsView("ipp-option-list", optionList, job,
                isReopenedTicket));

        //
        final String jobFileName = this.getJobFileName();

        Label label =
                new Label("btn-save", HtmlButtonEnum.SAVE.uiText(getLocale()));
        MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_DATA_SAVAPAGE,
                jobFileName);
        add(label);

        //
        final boolean isSingleAccountPrint = job.isSingleAccountPrint()
                || job.isSingleAccountUserGroupPrint();

        label = MarkupHelper.createEncloseLabel("jobticket-copies", "",
                isSingleAccountPrint);

        if (isSingleAccountPrint) {
            MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_VALUE,
                    String.valueOf(job.getCopies()));
        }

        add(label);

        //
        if (job.getMedia() == null) {
            helper.discloseLabel("pdf-media-prompt");
        } else {
            helper.addLabel("pdf-media-prompt",
                    NounEnum.DOCUMENT.uiText(getLocale()));
            helper.addLabel("pdf-media", MediaUtils.getUserFriendlyMediaName(
                    MediaUtils.getMediaSizeFromInboxMedia(job.getMedia())));
        }
        //
        if (!isReopenedTicket && DOC_STORE_SERVICE.isEnabled(
                DocStoreTypeEnum.ARCHIVE, DocStoreBranchEnum.OUT_PRINT)) {

            helper.addLabel("cb-archive-label", NounEnum.ARCHIVE);
            helper.addCheckbox("cb-archive",
                    BooleanUtils.isTrue(job.getArchive()));

            final StringBuilder imgSrc = new StringBuilder();
            imgSrc.append(WebApp.PATH_IMAGES).append('/')
                    .append("archive-32x32.png");

            helper.addModifyLabelAttr("img-archive", MarkupHelper.ATTR_SRC,
                    imgSrc.toString());

        } else {
            helper.discloseLabel("cb-archive");
        }

        //
        add(new Label("btn-cancel", HtmlButtonEnum.CANCEL.uiText(getLocale())));
    }

    /**
     * Create print-scaling option.
     *
     * @param job
     *            The job.
     * @return The option.
     */
    private JsonProxyPrinterOpt createPrintScalingOpt(final OutboxJobDto job) {

        final JsonProxyPrinterOpt opt = new JsonProxyPrinterOpt();

        opt.setKeyword(PrintScalingEnum.IPP_NAME);

        final ArrayList<JsonProxyPrinterOptChoice> choices = new ArrayList<>();

        JsonProxyPrinterOptChoice wlk;

        wlk = new JsonProxyPrinterOptChoice();
        wlk.setChoice(PrintScalingEnum.NONE.getIppValue());
        choices.add(wlk);

        wlk = new JsonProxyPrinterOptChoice();
        wlk.setChoice(PrintScalingEnum.AUTO.getIppValue());
        choices.add(wlk);

        wlk = new JsonProxyPrinterOptChoice();
        wlk.setChoice(PrintScalingEnum.FIT.getIppValue());
        choices.add(wlk);

        opt.setChoices(choices);

        opt.setDefchoice(StringUtils.defaultString(
                job.getOptionValues().get(PrintScalingEnum.IPP_NAME),
                PrintScalingEnum.NONE.getIppValue()));

        PROXY_PRINT_SERVICE.localizePrinterOpt(getLocale(), opt);

        return opt;
    }

}
