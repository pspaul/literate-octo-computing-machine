/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
import java.util.Collections;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PagePointOfSale extends AbstractAuthPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public PagePointOfSale(final PageParameters parameters) {

        super(parameters);

        MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("currency-symbol", SpSession.getAppCurrencySymbol());
        helper.addLabel("decimal-separator", SpSession.getDecimalSeparator());

        /*
         * Option list: Payment types
         */
        final List<String> paymentTypeList = new ArrayList<>(ConfigManager
                .instance().getConfigSet(Key.FINANCIAL_POS_PAYMENT_METHODS));

        Collections.sort(paymentTypeList);

        final String wicketIdPaymentList = "option-list-payment-types";

        if (paymentTypeList.isEmpty()) {
            helper.discloseLabel(wicketIdPaymentList);
        } else {

            add(new PropertyListView<String>(wicketIdPaymentList,
                    paymentTypeList) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(final ListItem<String> item) {
                    final String paymentType = item.getModel().getObject();
                    final Label label =
                            new Label("option-payment-type", paymentType);
                    label.add(new AttributeModifier("value", paymentType));
                    item.add(label);
                }

            });
        }
    }

    @Override
    protected boolean needMembership() {
        return false;
    }
}
