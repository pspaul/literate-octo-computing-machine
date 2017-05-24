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
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.i18n.PrintOutVerbEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketSettingsAddIn extends JobTicketAddInBase {

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
     * IPP attribute keywords for setting the Tray.
     */
    public static final String[] ATTR_SET_TRAY = new String[] {
            /* */
            IppDictJobTemplateAttr.ATTR_MEDIA,
            /* */
            IppDictJobTemplateAttr.ATTR_MEDIA_COLOR,
            /* */
            IppDictJobTemplateAttr.ATTR_MEDIA_TYPE,
            //
    };

    /**
     * IPP attribute keywords for driver.
     */
    public static final String[] ATTR_SET_DRIVER = new String[] {
            /* */
            IppDictJobTemplateAttr.ATTR_COPIES,
            /* */
            IppDictJobTemplateAttr.ATTR_SIDES,
            /* */
            IppDictJobTemplateAttr.ATTR_NUMBER_UP,
            /* */
            IppDictJobTemplateAttr.ATTR_PRINTER_RESOLUTION,
            /* */
            IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE,
            /* */
            IppDictJobTemplateAttr.ATTR_SHEET_COLLATE
            //
    };

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

        final String jobFileName = this.getParmValue(PARM_JOBFILENAME);

        if (StringUtils.isBlank(jobFileName)) {
            setResponsePage(new MessageContent(AppLogLevelEnum.ERROR, String
                    .format("\"%s\" parameter missing", PARM_JOBFILENAME)));
        }

        // The job
        final OutboxJobDto job = JOBTICKET_SERVICE.getTicket(jobFileName);

        final MarkupHelper helper = new MarkupHelper(this);
        helper.encloseLabel("jobticket-remark", job.getComment(),
                StringUtils.isNotBlank(job.getComment()));

        // Options
        final List<String[]> options = new ArrayList<>();
        final IppOptionMap optionMap = job.createIppOptionMap();

        String thWlk;
        String tdWlk;

        // ---------- Tray
        thWlk = PrintOutNounEnum.TRAY.uiText(getLocale());

        for (final String keyword : ATTR_SET_TRAY) {

            tdWlk = optionMap.getOptionValue(keyword);

            if (tdWlk == null) {
                continue;
            }
            options.add(new String[] { thWlk, PROXY_PRINT_SERVICE
                    .localizePrinterOptValue(getLocale(), keyword, tdWlk) });
            thWlk = null;
        }

        // ---------- Setting
        thWlk = PrintOutNounEnum.SETTING.uiText(getLocale());

        for (final String keyword : ATTR_SET_DRIVER) {

            tdWlk = null;

            if (keyword.equals(IppDictJobTemplateAttr.ATTR_COPIES)) {

                tdWlk = String.format("%d %s", job.getCopies(),
                        PrintOutNounEnum.COPY.uiText(getLocale(),
                                job.getCopies() > 1));
            } else if (keyword
                    .equals(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE)) {

                if (job.isCollate()) {
                    tdWlk = PrintOutVerbEnum.COLLATE.uiText(getLocale());
                }
            } else if (keyword.equals(IppDictJobTemplateAttr.ATTR_NUMBER_UP)) {

                if (optionMap.getNumberUp() != null) {
                    final int nUp = optionMap.getNumberUp().intValue();
                    if (nUp > 1) {
                        tdWlk = PrintOutNounEnum.N_UP.uiText(getLocale(),
                                optionMap.getNumberUp().toString());
                    } else {
                        tdWlk = null;
                    }
                }
            } else {
                tdWlk = optionMap.getOptionValue(keyword);
            }

            if (tdWlk == null) {
                continue;
            }
            options.add(new String[] { thWlk, PROXY_PRINT_SERVICE
                    .localizePrinterOptValue(getLocale(), keyword, tdWlk) });

            thWlk = null;
        }

        // ---------- Finishings
        thWlk = PrintOutNounEnum.FINISHING.uiText(getLocale());

        thWlk = addOptionsWhenSet(optionMap, options, getLocale(),
                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_V_NONE,
                thWlk);

        thWlk = addOptionsWhenSet(optionMap, options, getLocale(),
                IppDictJobTemplateAttr.JOBTICKET_ATTR_COPY_V_NONE, thWlk);

        thWlk = addOptionsExtWhenSet(job.getOptionValues(), options,
                getLocale(), thWlk);

        //
        add(new PrinterOptionsView("setting-row", options));
    }

    /**
     * Adds options when present and set (not-none).
     *
     * @param optionMap
     *            The chosen option map.
     * @param options
     *            The list to append options on.
     * @param locale
     *            The locale.
     * @param attrNone
     *            Array of 2-element array elements, one for each IPP attribute:
     *            the first element is the IPP option key, and the second
     *            element its NONE value.
     * @param thWlkStart
     *            The header text to start with.
     * @return The current header text.
     */
    private static String addOptionsWhenSet(final IppOptionMap optionMap,
            final List<String[]> options, final Locale locale,
            final String[][] attrNone, final String thWlkStart) {

        String thWlk = thWlkStart;
        String tdWlk;

        for (final String[] finishing : attrNone) {

            tdWlk = optionMap.getOptionValue(finishing[0]);

            if (tdWlk == null || tdWlk.equals(finishing[1])) {
                continue;
            }
            options.add(new String[] { thWlk, PROXY_PRINT_SERVICE
                    .localizePrinterOptValue(locale, finishing[0], tdWlk) });
            thWlk = null;
        }
        return thWlk;
    }

    /**
     * Adds Customer Ext options when present and set (not-none).
     *
     * @param optionMap
     *            The chosen option map.
     * @param options
     *            The list to append options on.
     * @param locale
     *            The locale.
     * @param thWlkStart
     *            The header text to start with.
     * @return The current header text.
     */
    private static String addOptionsExtWhenSet(
            final Map<String, String> optionMap, final List<String[]> options,
            final Locale locale, final String thWlkStart) {

        String thWlk = thWlkStart;

        for (final Entry<String, String> entry : optionMap.entrySet()) {

            if (!IppDictJobTemplateAttr.isCustomExtAttr(entry.getKey())
                    || IppDictJobTemplateAttr
                            .isCustomExtAttrValueNone(entry.getValue())) {
                continue;
            }

            options.add(new String[] { thWlk,
                    String.format("%s %s",
                            PROXY_PRINT_SERVICE.localizePrinterOpt(locale,
                                    entry.getKey()),
                            PROXY_PRINT_SERVICE.localizePrinterOptValue(locale,
                                    entry.getKey(), entry.getValue())) });
            thWlk = null;
        }
        return thWlk;
    }

}
