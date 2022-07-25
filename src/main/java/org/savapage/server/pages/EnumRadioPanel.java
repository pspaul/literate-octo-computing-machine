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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.services.helpers.InboxSelectScopeEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class EnumRadioPanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    private class EnumRadioView<E extends Enum<E>> extends PropertyListView<E> {

        private static final long serialVersionUID = 1L;

        private final String htmlName;
        private final Enum<E> selected;
        private final Map<E, String> uiText;

        /**
         *
         * @param id
         * @param entryList
         * @param htmlName
         */
        EnumRadioView(final String id, final List<E> entryList,
                final Enum<E> selected, final Map<E, String> uiText,
                final String htmlName) {
            super(id, entryList);
            this.htmlName = htmlName;
            this.selected = selected;
            this.uiText = uiText;
        }

        @Override
        protected void populateItem(final ListItem<E> item) {

            final Enum<E> value = item.getModelObject();
            final String htmlId = UUID.randomUUID().toString();

            //
            Label label = new Label("label-radio", this.uiText.get(value));

            MarkupHelper.modifyLabelAttr(label, "for", htmlId);
            item.add(label);

            //
            label = new Label("input-radio", "");

            MarkupHelper.modifyLabelAttr(label, "id", htmlId);
            MarkupHelper.modifyLabelAttr(label, "value", value.toString());
            MarkupHelper.modifyLabelAttr(label, "name", this.htmlName);

            if (value == this.selected || (this.selected == null
                    && value == InboxSelectScopeEnum.ALL)) {
                MarkupHelper.modifyLabelAttr(label, "checked", "checked");
            }

            item.add(label);
        }
    }

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public EnumRadioPanel(final String id) {
        super(id);
    }

    /**
     *
     * @param scopes
     * @param selected
     * @param uiText
     * @param htmlName
     */
    public <E extends Enum<E>> void populate(final EnumSet<E> scopes,
            final Enum<E> selected, final Map<E, String> uiText,
            final String htmlName) {

        final List<E> entryList = new ArrayList<>();

        for (final E scope : scopes) {
            entryList.add(scope);
        }

        // //value.uiText(getLocale()));

        add(new EnumRadioView<>("enum-values", entryList, selected, uiText,
                htmlName));
    }

}
