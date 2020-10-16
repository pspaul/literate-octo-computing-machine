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

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class PageNavPanel extends Panel {

    private static final long serialVersionUID = 1L;

    /**
     * Navigation panel component (pager).
     *
     * @param id
     *            <p>
     *            The id of the component.
     *            </p>
     * @param list
     *            <p>
     *            The list with ordinal page numbers to display a selection.
     *            </p>
     * @param cssClassBase
     *            <p>
     *            The CSS class name used for the navigation buttons.
     *            <p>
     *            <ul>
     *            <li><code>cssClassBase</code> is used for attribute 'class' of
     *            the leftmost button and the buttons in the list.</li>
     *            <li>The 'prev' button gets the class value:
     *            <code>cssClassBase + "-prev'</code></li>
     *            <li>The 'next' button gets the class value:
     *            <code>cssClassBase + "-next'</code></li>
     *            <li>The 'last' button gets the class value:
     *            <code>cssClassBase + "-last'</code></li>
     *            </ul>
     *            <p>
     *            Make sure this value is unique within the JavaScript scope.
     *            </p>
     * @param nSelectedPage
     *            The selected page (which will be in view).
     * @param nLastPage
     *            The ordinal number of the last page of the entire collection.
     */
    public PageNavPanel(final String id, List<Long> list,
            final String cssClassBase, final Long nSelectedPage,
            final long nLastPage) {

        super(id);

        Label label = null;
        AttributeModifier attMod = null;
        AttributeModifier attModNone =
                new AttributeModifier("style", "display: none;");

        final String appendedClass = " " + cssClassBase;

        /*
         *
         */
        label = new Label("nav-button-first", "1");
        label.add(new AttributeAppender("class", appendedClass));
        add(label);

        /*
         *
         */
        label = new Label("nav-button-prev", "<");
        if (nLastPage > 1) {
            attMod = new AttributeAppender("class", appendedClass + "-prev");
        } else {
            attMod = attModNone;
        }
        label.add(attMod);
        add(label);

        /*
         *
         */
        label = new Label("nav-button-next", ">");
        if (nLastPage > 1) {
            attMod = new AttributeAppender("class", appendedClass + "-next");
        } else {
            attMod = attModNone;
        }
        label.add(attMod);
        add(label);

        /*
         *
         */
        label = new Label("nav-button-last", String.valueOf(nLastPage));
        if (nLastPage > 1) {
            attMod = new AttributeAppender("class", appendedClass);
        } else {
            attMod = attModNone;
        }
        label.add(attMod);
        add(label);

        /*
         *
         */
        ListView<Long> listview = new ListView<Long>("log-nav-1", list) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<Long> item) {
                Label label = new Label("nav-button", item.getModel());
                if (item.getModelObject().equals(nSelectedPage)) {
                    label.add(new AttributeAppender("class",
                            appendedClass + " ui-btn-active"));
                } else {
                    label.add(new AttributeAppender("class", appendedClass));
                }
                item.add(label);
            }
        };
        add(listview);
    }

}
