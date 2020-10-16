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
package org.savapage.server.pages.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.pdf.PdfDocumentFonts;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.pages.AbstractAuthPage;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfDocumentFontsAddIn extends AbstractAuthPage {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final InboxService INBOX_SERVICE =
            ServiceContext.getServiceFactory().getInboxService();

    /** */
    private static final int MAX_EMBEDDED_FONTS_IN_ROW = 3;

    /**
     *
     */
    public PdfDocumentFontsAddIn(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        final int iJob = Integer.parseInt(this.getParmValue("ijob"));

        Map<String, PdfDocumentFonts.Font> mapFont;
        String msgException = null;
        try {
            final PdfDocumentFonts fonts = INBOX_SERVICE
                    .getJobFonts(SpSession.get().getUserId(), iJob);
            mapFont = fonts.getFonts();
        } catch (Exception e) {
            msgException = e.getMessage();
            mapFont = new HashMap<>();
        }

        final List<String> fontKeys = new ArrayList<>();

        final Set<String> subsetFonts = new HashSet<>();

        final StringBuilder embeddedFonts = new StringBuilder();

        int nFontStandard = 0;
        int nFontNonEmbedded = 0;
        int nFontEmbedded = 0;
        int nEmbedPart = 0;

        for (final Entry<String, PdfDocumentFonts.Font> entry : mapFont
                .entrySet()) {
            final String key = entry.getKey();
            final PdfDocumentFonts.Font font = entry.getValue();

            if (font.isStandardFont()) {
                nFontStandard++;
            } else {
                if (font.isEmbedded()) {
                    nFontEmbedded++;
                } else {
                    nFontNonEmbedded++;
                }
            }
            if (font.isEmbedded()) {

                final String embedPart;

                if (font.isSubset()) {
                    final String[] fontNameSplit =
                            StringUtils.split(font.getName(), '+');

                    final String fontPart;

                    if (fontNameSplit.length > 1) {
                        fontPart = fontNameSplit[1];
                    } else {
                        // Yes, this can happen :-(
                        fontPart = fontNameSplit[0];
                    }

                    if (subsetFonts.contains(fontPart)) {
                        embedPart = null;
                    } else {
                        if (fontNameSplit.length > 1) {
                            embedPart = "+".concat(fontPart);
                        } else {
                            // Yes, this can happen :-(
                            embedPart = fontPart.concat("+");
                        }
                        subsetFonts.add(fontPart);
                    }

                } else {
                    embedPart = key;
                    embeddedFonts.append(" ").append(key);
                }

                if (embedPart != null) {
                    if (nEmbedPart > 0
                            && nEmbedPart % MAX_EMBEDDED_FONTS_IN_ROW == 0) {
                        embeddedFonts.append("<br>");
                    } else {
                        embeddedFonts.append(" ");
                    }
                    embeddedFonts.append(embedPart);
                    nEmbedPart++;
                }

            } else {
                fontKeys.add(key);
            }
        }

        final PhraseEnum msg;
        String cssClass = MarkupHelper.CSS_TXT_INFO;

        final boolean noFonts = fontKeys.isEmpty() && nFontEmbedded == 0;

        if (noFonts) {
            msg = PhraseEnum.PDF_FONTS_NONE;
            if (msgException != null) {
                cssClass = MarkupHelper.CSS_TXT_ERROR;
            }
        } else if (nFontNonEmbedded == 0) {
            if (nFontEmbedded > 0 && nFontStandard > 0) {
                msg = PhraseEnum.PDF_FONTS_STANDARD_OR_EMBEDDED;
            } else if (nFontEmbedded > 0) {
                msg = PhraseEnum.PDF_FONTS_ALL_EMBEDDED;
            } else {
                msg = PhraseEnum.PDF_FONTS_ALL_STANDARD;
            }
        } else {
            if (nFontEmbedded + nFontStandard == 0) {
                msg = PhraseEnum.PDF_FONTS_ALL_NON_EMBEDDED;
            } else {
                msg = PhraseEnum.PDF_FONTS_SOME_NON_EMBEDDED;
            }
            cssClass = MarkupHelper.CSS_TXT_WARN;
        }

        helper.addAppendLabelAttr("summary", msg.uiText(getLocale()),
                MarkupHelper.ATTR_CLASS, cssClass);

        helper.addLabel("fonts-embedded", embeddedFonts.toString())
                .setEscapeModelStrings(false);

        //
        helper.encloseLabel("fonts-header",
                NounEnum.FONT.uiText(getLocale(), true), !noFonts);

        helper.encloseLabel("table-header-font",
                NounEnum.FONT.uiText(getLocale()), !fontKeys.isEmpty());

        if (!fontKeys.isEmpty()) {

            helper.addLabel("table-header-rendering",
                    NounEnum.RENDERING.uiText(getLocale()));

            final Map<String, PdfDocumentFonts.Font> mapFontWrk = mapFont;

            add(new PropertyListView<String>("font-entry", fontKeys) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(final ListItem<String> item) {

                    final String key = item.getModelObject();
                    final PdfDocumentFonts.Font font = mapFontWrk.get(key);

                    final String fontName;
                    if (font.isSubset()) {
                        fontName = "+".concat(
                                StringUtils.split(font.getName(), '+')[1]);

                    } else {
                        fontName = key;
                    }

                    Label labelWlk;
                    labelWlk = new Label("font-name", fontName);

                    if (font.isEmbedded()) {
                        MarkupHelper.appendLabelAttr(labelWlk,
                                MarkupHelper.ATTR_CLASS,
                                MarkupHelper.CSS_TXT_VALID);
                    } else {
                        if (!font.isStandardFont()) {
                            MarkupHelper.appendLabelAttr(labelWlk,
                                    MarkupHelper.ATTR_CLASS,
                                    MarkupHelper.CSS_TXT_WARN);
                        }
                    }
                    item.add(labelWlk);

                    item.add(new Label("font-rendering", StringUtils
                            .defaultString(font.getSystemFontMatch())));
                }
            });
        }
    }

    @Override
    protected boolean needMembership() {
        return false;
    }
}
