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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class QuickSearchPanel extends Panel {

    /** */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public QuickSearchPanel(final String id) {
        super(id);
    }

    /**
     *
     * @param htmlBaseId
     *            The HTML "id" attribute of the search input field.
     * @param searchPrompt
     *            The search prompt.
     * @param searchPlaceholder
     *            The placeholder string.
     * @param compact
     *            If {@code true}, panel is rendered in compact mode.
     * @param htmlIdCompactScopeButton
     *            When not {@code null} <i>and</i> panel is rendered in compact
     *            mode, a button is added that can be used in JavaScript context
     *            to set search scope.
     * @param htmlIdCompactScopeTitle
     *            The HTML title of the compact scope button.
     */
    public void populate(final String htmlBaseId, final String searchPrompt,
            final String searchPlaceholder, final boolean compact,
            final String htmlIdCompactScopeButton,
            final String htmlIdCompactScopeTitle) {

        final MarkupHelper helper = new MarkupHelper(this);

        Label labelWrk;
        final Label labelInput;

        if (compact) {

            add(new Label("search-label-compact", searchPrompt));
            labelInput = new Label("search-input-compact", "");
            helper.discloseLabel("search-label");

            helper.encloseLabel("btn-search-scope-compact",
                    htmlIdCompactScopeButton,
                    StringUtils.isNotBlank(htmlIdCompactScopeButton))
                    .add(new AttributeModifier(MarkupHelper.ATTR_ID,
                            htmlIdCompactScopeButton))
                    .add(new AttributeModifier(MarkupHelper.ATTR_TITLE,
                            StringUtils
                                    .defaultString(htmlIdCompactScopeTitle)));

        } else {

            labelWrk = new Label("search-label", searchPrompt);
            labelWrk.add(
                    new AttributeModifier(MarkupHelper.ATTR_FOR, htmlBaseId));
            add(labelWrk);

            labelInput = new Label("search-input", "");

            helper.discloseLabel("search-label-compact");
        }

        //
        labelInput.add(new AttributeModifier(MarkupHelper.ATTR_ID, htmlBaseId));
        if (StringUtils.isNotBlank(searchPlaceholder)) {
            labelInput.add(new AttributeModifier(MarkupHelper.ATTR_PLACEHOLDER,
                    searchPlaceholder));
        }
        add(labelInput);

        //
        labelWrk = new Label("search-filter", "");
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_ID,
                String.format("%s-filter", htmlBaseId)));
        labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_DATA_INPUT,
                String.format("#%s", htmlBaseId)));

        add(labelWrk);
    }

}
