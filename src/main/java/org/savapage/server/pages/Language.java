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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.savapage.core.util.I18nStats;
import org.savapage.core.util.LocaleHelper;
import org.savapage.server.WebServer;
import org.savapage.server.helpers.HtmlButtonEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class Language extends AbstractPage {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public Language() {

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addButton("button-cancel", HtmlButtonEnum.CANCEL);

        final Map<Locale, Integer> i18nPercentage;
        if (WebServer.isDeveloperEnv()) {
            i18nPercentage = I18nStats.getI18nPercentages();
        } else {
            i18nPercentage = I18nStats.getI18nPercentagesCached(true);
        }

        final Set<Locale> i18nAvailable = LocaleHelper.getI18nAvailable();
        final List<Locale> i18nAvailableSorted = new ArrayList<>();
        for (final Entry<Locale, Integer> entry : i18nPercentage.entrySet()) {
            final Locale key = entry.getKey();
            if (i18nAvailable.contains(key)) {
                i18nAvailableSorted.add(key);
            }
        }

        this.add(new PropertyListView<Locale>("language-list",
                i18nAvailableSorted) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<Locale> item) {

                final MarkupHelper itemHelper = new MarkupHelper(item);
                final Locale locale = item.getModel().getObject();

                final Component lang = itemHelper.addTransparant("language");

                MarkupHelper.modifyComponentAttr(lang, "data-language",
                        locale.getLanguage());
                MarkupHelper.modifyComponentAttr(lang, "data-country",
                        locale.getCountry());

                item.add(new Label("language-txt", StringUtils
                        .capitalize(locale.getDisplayLanguage(locale))));

                final Integer perc = i18nPercentage.get(locale);
                final StringBuilder uiPerc = new StringBuilder();
                if (perc == null) {
                    uiPerc.append("-");
                } else {
                    uiPerc.append(perc);
                }
                uiPerc.append("%");
                item.add(new Label("language-perc", uiPerc));
            }
        });
    }

}
