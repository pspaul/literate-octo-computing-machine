/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.savapage.core.util.LocaleHelper;
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

        add(new PropertyListView<Locale>("language-list",
                LocaleHelper.getAvailableLanguages()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<Locale> item) {

                final Locale locale = item.getModel().getObject();

                final Label label = new Label("language", StringUtils
                        .capitalize(locale.getDisplayLanguage(locale)));

                label.add(new AttributeModifier("data-language",
                        locale.getLanguage()));
                label.add(new AttributeModifier("data-country",
                        locale.getCountry()));

                item.add(label);
            }
        });
    }

}
