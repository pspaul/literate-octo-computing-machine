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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.i18n.PrintOutVerbEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.json.JsonPrinterDetail;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketSettingsAddIn extends AbstractAuthPage {

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
    private static class PrinterOptionsView extends PropertyListView<String[]> {

        /**
         * .
         */
        private static final long serialVersionUID = 1L;

        /**
         *
         * @param id
         *            The wicket id.
         * @param list
         *            The item list.
         */
        PrinterOptionsView(final String id, final List<String[]> list) {

            super(id, list);
        }

        @Override
        protected void populateItem(final ListItem<String[]> item) {

            final String[] option = item.getModelObject();

            final String th = option[0];
            final String td = option[1];

            final MarkupHelper helper = new MarkupHelper(item);

            if (th == null) {
                helper.discloseLabel("cat-th");
                helper.encloseLabel("th", th, true);
                helper.addLabel("td", td);
            } else {
                helper.discloseLabel("td");
                helper.encloseLabel("cat-th", th, true);
                helper.addLabel("cat-td", td);
            }
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
    public JobTicketSettingsAddIn(final PageParameters parameters) {

        super(parameters);
        populate();
    }

    /**
     *
     */
    private void populate() {

        final String jobFileName = this.getParmValue("jobFileName");

        if (StringUtils.isBlank(jobFileName)) {
            setResponsePage(new MessageContent(AppLogLevelEnum.ERROR,
                    "\"jobFileName\" parameter missing"));
        }

        // The job
        final OutboxJobDto job = JOBTICKET_SERVICE.getTicket(jobFileName);

        final JsonPrinterDetail printer =
                PROXY_PRINT_SERVICE.getPrinterDetailUserCopy(
                        getSession().getLocale(), job.getPrinter());

        //
        // final List<JsonProxyPrinterOpt> optionList = new ArrayList<>();
        //
        // for (final JsonProxyPrinterOptGroup group : printer.getGroups()) {
        // for (final JsonProxyPrinterOpt option : group.getOptions()) {
        // if (option.getKeyword()
        // .equals(IppDictJobTemplateAttr.ATTR_MEDIA_SOURCE)) {
        // continue;
        // }
        // optionList.add(option);
        // }
        // }

        final List<String[]> options = new ArrayList<>();
        final IppOptionMap optionMap = job.createIppOptionMap();

        String thWlk;
        String tdWlk;

        // ---------- Tray
        thWlk = "Tray";

        // media
        tdWlk = "??";
        options.add(new String[] { thWlk, tdWlk });

        thWlk = null;

        // media-color
        tdWlk = optionMap
                .getOptionValue(IppDictJobTemplateAttr.ATTR_MEDIA_COLOR);
        if (tdWlk != null) {
            options.add(new String[] { thWlk,
                    PROXY_PRINT_SERVICE.localizePrinterOptValue(getLocale(),
                            IppDictJobTemplateAttr.ATTR_MEDIA_COLOR, tdWlk) });
        }

        // media-type
        tdWlk = optionMap
                .getOptionValue(IppDictJobTemplateAttr.ATTR_MEDIA_TYPE);
        if (tdWlk != null) {
            options.add(new String[] { thWlk,
                    PROXY_PRINT_SERVICE.localizePrinterOptValue(getLocale(),
                            IppDictJobTemplateAttr.ATTR_MEDIA_TYPE, tdWlk) });
        }

        // ---------- Setting
        thWlk = "Setting";

        // copies
        options.add(new String[] { thWlk,
                String.format("%d %s", job.getCopies(), PrintOutNounEnum.COPY
                        .uiText(getLocale(), job.getCopies() > 1)) });
        thWlk = null;

        // sides
        tdWlk = optionMap.getOptionValue(IppDictJobTemplateAttr.ATTR_SIDES);
        if (tdWlk != null) {
            options.add(new String[] { thWlk,
                    PROXY_PRINT_SERVICE.localizePrinterOptValue(getLocale(),
                            IppDictJobTemplateAttr.ATTR_SIDES, tdWlk) });
        }

        // collate
        if (job.isCollate()) {
            options.add(new String[] { thWlk,
                    PrintOutVerbEnum.COLLATE.uiText(getLocale()) });
        }

        // color-mode
        if (optionMap.isColorJob()) {
            tdWlk = PrintOutNounEnum.COLOR.uiText(getLocale());
        } else {
            tdWlk = PrintOutNounEnum.GRAYSCALE.uiText(getLocale());
        }
        options.add(new String[] { thWlk, tdWlk });

        thWlk = null;

        // ---------- Finishings
        // :
        // : Job Ticket options

        //
        add(new PrinterOptionsView("setting-row", options));
    }

}
