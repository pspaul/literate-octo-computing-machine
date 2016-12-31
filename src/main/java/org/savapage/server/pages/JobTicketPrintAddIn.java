/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dto.RedirectPrinterDto;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.ext.papercut.PaperCutHelper;
import org.savapage.server.WebApp;
import org.savapage.server.helpers.HtmlButtonEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketPrintAddIn extends AbstractAuthPage {

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
    private static class RedirectPrinterListView
            extends PropertyListView<RedirectPrinterDto> {

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
        RedirectPrinterListView(final String id,
                final List<RedirectPrinterDto> list) {

            super(id, list);
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
        }

    }

    @Override
    protected boolean needMembership() {
        return false;
    }

    private static final String PARM_JOBFILENAME = "jobFileName";
    private static final String PARM_SETTLE = "settle";

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

        //
        final List<RedirectPrinterDto> printerList;

        try {
            printerList = JOBTICKET_SERVICE.getRedirectPrinters(jobFileName);
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
            labelButton = MarkupHelper.createEncloseLabel("btn-settle",
                    HtmlButtonEnum.SETTLE.uiText(getLocale()), true);
        } else {
            prompt = localized("prompt-header-print");
            helper.discloseLabel("btn-settle");
            labelButton = MarkupHelper.createEncloseLabel("btn-print",
                    HtmlButtonEnum.PRINT.uiText(getLocale()), true);
        }

        MarkupHelper.modifyLabelAttr(labelButton,
                MarkupHelper.ATTR_DATA_SAVAPAGE, jobFileName);
        add(labelButton);

        add(new Label("prompt-header", prompt));

        //
        add(new RedirectPrinterListView("printer-radio", printerList));
    }

}
