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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.json.JsonPrinterDetail;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.print.proxy.JsonProxyPrinterOptGroup;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
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

    /**
     * .
     */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /**
     * .
     */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

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

        /**
         *
         * @param id
         *            The wicket id.
         * @param list
         *            The item list.
         * @param job
         *            The job.
         */
        PrinterOptionsView(final String id,
                final List<JsonProxyPrinterOpt> list, final OutboxJobDto job) {

            super(id, list);
            this.jobTicket = job;
        }

        @Override
        protected void populateItem(final ListItem<JsonProxyPrinterOpt> item) {

            final JsonProxyPrinterOpt printerOption = item.getModelObject();

            final String id = UUID.randomUUID().toString();

            Label labelWlk;

            final Fragment optionFragment =
                    new Fragment("ipp-option-div", "ipp-option-select", this);

            optionFragment.add(new AttributeModifier("id", "todo"));

            labelWlk = new Label("label", printerOption.getUiText());
            MarkupHelper.modifyLabelAttr(labelWlk, "for", id);
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

            MarkupHelper.modifyLabelAttr(labelWlk, "id", id);
            MarkupHelper.modifyLabelAttr(labelWlk,
                    MarkupHelper.ATTR_DATA_SAVAPAGE,
                    printerOption.getKeyword());

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

        final String jobFileName = this.getParmValue(PARM_JOBFILENAME);

        if (StringUtils.isBlank(jobFileName)) {
            setResponsePage(new MessageContent(AppLogLevelEnum.ERROR, String
                    .format("\"%s\" parameter missing", PARM_JOBFILENAME)));
        }

        // The job
        final OutboxJobDto job = JOBTICKET_SERVICE.getTicket(jobFileName);

        final JsonPrinterDetail printer =
                PROXY_PRINT_SERVICE.getPrinterDetailUserCopy(
                        getSession().getLocale(), job.getPrinter());

        final List<JsonProxyPrinterOpt> optionList = new ArrayList<>();

        for (final JsonProxyPrinterOptGroup group : printer.getGroups()) {
            for (final JsonProxyPrinterOpt option : group.getOptions()) {
                if (option.getKeyword()
                        .equals(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE)) {
                    continue;
                }
                optionList.add(option);
            }
        }

        add(new PrinterOptionsView("ipp-option-list", optionList, job));

        //
        Label label =
                new Label("btn-save", HtmlButtonEnum.SAVE.uiText(getLocale()));
        MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_DATA_SAVAPAGE,
                jobFileName);
        add(label);

        //
        label = MarkupHelper.createEncloseLabel("jobticket-copies", "",
                !job.isDelegatedPrint());

        if (!job.isDelegatedPrint()) {
            MarkupHelper.modifyLabelAttr(label, "value",
                    String.valueOf(job.getCopies()));
        }

        add(label);

        //
        add(new Label("btn-cancel", HtmlButtonEnum.CANCEL.uiText(getLocale())));
    }

}
