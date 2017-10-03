/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.i18n.PrintOutVerbEnum;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.JsonHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrintOutOptionsPanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public PrintOutOptionsPanel(final String id) {
        super(id);
    }

    /**
     * Populates the panel.
     *
     * @param printOut
     *            The {@link PrintOut}.
     */
    public void populate(final PrintOut printOut) {

        final MarkupHelper helper = new MarkupHelper(this);
        final Locale locale = this.getLocale();

        final Map<String, String> mapVisible = new HashMap<>();

        for (final String attr : new String[] { "landscape", "duplex",
                "simplex", "grayscale", "color", "punch", "staple", "fold",
                "booklet", "jobticket-media", "jobticket-copy",
                "jobticket-finishing-ext", "jobticket-custom-ext" }) {
            mapVisible.put(attr, null);
        }

        helper.addLabel("papersize", printOut.getPaperSize().toUpperCase());

        if (BooleanUtils.isTrue(printOut.getDuplex())) {
            mapVisible.put("duplex", PrintOutNounEnum.DUPLEX.uiText(locale));
        } else {
            mapVisible.put("simplex", PrintOutNounEnum.SIMPLEX.uiText(locale));
        }

        if (BooleanUtils.isTrue(printOut.getGrayscale())) {
            mapVisible.put("grayscale",
                    PrintOutNounEnum.GRAYSCALE.uiText(locale));
        } else {
            mapVisible.put("color", PrintOutNounEnum.COLOR.uiText(locale));
        }

        final Map<String, String> ippOptions =
                JsonHelper.createStringMapOrNull(printOut.getIppOptions());

        if (ippOptions != null) {
            final IppOptionMap optionMap = new IppOptionMap(ippOptions);

            if (optionMap.hasFinishingPunch()) {
                mapVisible.put("punch",
                        uiIppKeywordValue(getLocale(),
                                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH,
                                optionMap));
            }
            if (optionMap.hasFinishingStaple()) {
                mapVisible.put("staple",
                        uiIppKeywordValue(getLocale(),
                                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE,
                                optionMap));
            }
            if (optionMap.hasFinishingFold()) {
                mapVisible.put("fold",
                        String.format("%s %s",
                                helper.localized(PrintOutVerbEnum.FOLD),
                                uiIppKeywordValue(getLocale(),
                                        IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_FOLD,
                                        optionMap)));
            }
            if (optionMap.hasFinishingBooklet()) {
                mapVisible.put("booklet",
                        PrintOutNounEnum.BOOKLET.uiText(locale));
            }

            mapVisible.put("jobticket-media",
                    PROXYPRINT_SERVICE.getJobTicketOptionsUiText(getLocale(),
                            IppDictJobTemplateAttr.JOBTICKET_ATTR_MEDIA,
                            optionMap));

            mapVisible.put("jobticket-copy",
                    PROXYPRINT_SERVICE.getJobTicketOptionsUiText(getLocale(),
                            IppDictJobTemplateAttr.JOBTICKET_ATTR_COPY,
                            optionMap));

            mapVisible.put("jobticket-finishings-ext",
                    PROXYPRINT_SERVICE.getJobTicketOptionsUiText(getLocale(),
                            IppDictJobTemplateAttr.JOBTICKET_ATTR_FINISHINGS_EXT,
                            optionMap));

            mapVisible.put("jobticket-custom-ext", PROXYPRINT_SERVICE
                    .getJobTicketOptionsExtHtml(getLocale(), ippOptions));

            if (optionMap.isLandscapeJob()) {
                mapVisible.put("landscape",
                        PrintOutNounEnum.LANDSCAPE.uiText(locale));
            }
        }

        /*
         * Hide/Show
         */
        for (final Map.Entry<String, String> entry : mapVisible.entrySet()) {

            if (entry.getValue() == null) {
                helper.discloseLabel(entry.getKey());
            } else {
                helper.encloseLabel(entry.getKey(), entry.getValue(), true);
            }
        }

    }

    private String uiIppKeywordValue(final Locale locale,
            final String ippKeyword, final IppOptionMap map) {
        return PROXYPRINT_SERVICE.localizePrinterOptValue(locale, ippKeyword,
                map.getOptionValue(ippKeyword));
    }

    /**
     * Gives the localized string for a key.
     *
     * @param key
     *            The key from the XML resource file
     * @return The localized string.
     */
    private String localized(final String key) {
        return getLocalizer().getString(key, this);
    }

}
