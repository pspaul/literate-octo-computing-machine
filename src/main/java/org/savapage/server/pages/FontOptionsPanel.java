/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.fonts.InternalFontFamilyEnum;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class FontOptionsPanel extends Panel {

    private static final long serialVersionUID = 1L;

    public FontOptionsPanel(String id) {
        super(id);
    }

    /**
     *
     */
    public void populate(final InternalFontFamilyEnum fontFamilyDefault) {

        final List<InternalFontFamilyEnum> entryList =
                new ArrayList<>(Arrays.asList(InternalFontFamilyEnum.values()));

        add(new PropertyListView<InternalFontFamilyEnum>(
                "option-font-families", entryList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<InternalFontFamilyEnum> item) {

                final InternalFontFamilyEnum font = item.getModel().getObject();
                final Label label =
                        new Label("option-font-family", font.getName());
                label.add(new AttributeModifier("value", font.toString()));
                if (fontFamilyDefault.equals(font)) {
                    label.add(new AttributeModifier("selected", "selected"));
                }
                item.add(label);
            }
        });

    }

}
