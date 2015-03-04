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
import org.savapage.core.reports.JrVoucherPageLayoutEnum;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class VoucherDesignOptionsPanel extends Panel {

    private static final long serialVersionUID = 1L;

    public VoucherDesignOptionsPanel(String id) {
        super(id);
    }

    /**
     *
     */
    public void populate(final JrVoucherPageLayoutEnum designDefault) {

        final List<JrVoucherPageLayoutEnum> entryList =
                new ArrayList<>(Arrays.asList(JrVoucherPageLayoutEnum.values()));

        add(new PropertyListView<JrVoucherPageLayoutEnum>("option-list",
                entryList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<JrVoucherPageLayoutEnum> item) {

                final JrVoucherPageLayoutEnum design =
                        item.getModel().getObject();
                final Label label = new Label("option", design.getName());
                label.add(new AttributeModifier("value", design.toString()));
                if (designDefault.equals(design)) {
                    label.add(new AttributeModifier("selected", "selected"));
                }
                item.add(label);
            }
        });

    }

}
