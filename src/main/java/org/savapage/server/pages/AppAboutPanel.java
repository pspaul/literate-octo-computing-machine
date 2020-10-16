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

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.MissingResourceException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.server.WebApp;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AppAboutPanel extends Panel {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    private static final boolean SHOW_TRANSLATOR_INFO = false;

    /**
     * Gives the localized string for a key.
     *
     * @param key
     *            The key from the XML resource file
     * @return The localized string.
     */
    private final String localized(final String key) {
        return getLocalizer().getString(key, this);
    }

    /**
     * Localizes and format a string with placeholder arguments.
     *
     * @param key
     *            The key from the XML resource file
     * @param objects
     *            The values to fill the placeholders
     * @return The localized string.
     */
    protected final String localized(final String key,
            final Object... objects) {
        return MessageFormat.format(getLocalizer().getString(key, this),
                objects);
    }

    /**
     *
     */
    public AppAboutPanel(final String id) {

        super(id);

        final MarkupHelper helper = new MarkupHelper(this);
        Label labelWrk;

        //
        add(new Label("current-year",
                String.valueOf(Calendar.getInstance().get(Calendar.YEAR))));

        add(new Label("app-name-1", CommunityDictEnum.SAVAPAGE.getWord()));
        add(new Label("app-name-2", CommunityDictEnum.SAVAPAGE.getWord()));

        helper.addModifyLabelAttr("gdpr-url-img",
                String.format(
                        "<img width=\"50\" class=\"sp-img-round-corners-3\""
                                + " src=\"%s/eu-flag-125x83.png\" />",
                        WebApp.PATH_IMAGES_THIRDPARTY),
                MarkupHelper.ATTR_HREF, CommunityDictEnum.EU_GDPR_URL.getWord())
                .setEscapeModelStrings(false);

        helper.addModifyLabelAttr("gdpr-url",
                CommunityDictEnum.EU_GDPR_FULL_TXT.getWord(),
                MarkupHelper.ATTR_HREF,
                CommunityDictEnum.EU_GDPR_URL.getWord());

        helper.addModifyLabelAttr("eu-url",
                CommunityDictEnum.EU_FULL_TXT.getWord(), MarkupHelper.ATTR_HREF,
                CommunityDictEnum.EU_URL.getWord());

        //
        add(new Label("app-copyright-owner",
                CommunityDictEnum.DATRAVERSE_BV.getWord(getLocale())));

        //
        labelWrk = new Label("savapage-url",
                CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG.getWord());
        labelWrk.add(new AttributeModifier("href",
                CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG_URL.getWord()));
        add(labelWrk);

        //
        labelWrk = new Label("savapage-source-code-url",
                localized("source-code-link"));
        labelWrk.add(new AttributeModifier("href",
                CommunityDictEnum.COMMUNITY_SOURCE_CODE_URL
                        .getWord(getLocale())));
        add(labelWrk);

        //
        final String downloadPanelId = "printerdriver-download-panel";

        if (ConfigManager.instance().isConfigValue(
                IConfigProp.Key.WEBAPP_ABOUT_DRIVER_DOWNLOAD_ENABLE)) {

            final PrinterDriverDownloadPanel downloadPanel =
                    new PrinterDriverDownloadPanel(downloadPanelId);
            add(downloadPanel);
            downloadPanel.populate();

        } else {
            helper.discloseLabel(downloadPanelId);
        }

        //
        String translatorInfo = null;

        if (SHOW_TRANSLATOR_INFO) {
            try {
                translatorInfo = localized("translator-info",
                        localized("_translator_name"));
            } catch (MissingResourceException e) {
                translatorInfo = null;
            }
        }

        helper.encloseLabel("translator-info", translatorInfo,
                StringUtils.isNotBlank(translatorInfo));

    }

}
