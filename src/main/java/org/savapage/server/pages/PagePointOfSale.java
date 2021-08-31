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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.services.helpers.account.UserAccountContextEnum;
import org.savapage.core.services.helpers.account.UserAccountContextFactory;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.session.SpSession;

/**
 * @author Rijk Ravestein
 */
public abstract class PagePointOfSale extends AbstractAuthPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @return Back button.
     */
    protected abstract HtmlButtonEnum buttonBack();

    /**
     * @param parameters
     *            Page parameters.
     */
    PagePointOfSale(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);
        helper.addButton("btn-back-main", this.buttonBack());

        final ConfigManager cm = ConfigManager.instance();

        if (cm.isConfigValue(Key.FINANCIAL_POS_SALES_ENABLE)) {

            helper.addLabel("tab-sales", this.localized("tab-sales"));
            helper.addLabel("currency-symbol-1",
                    SpSession.getAppCurrencySymbol());
            helper.addLabel("decimal-separator-1",
                    SpSession.getDecimalSeparator());

            helper.addButton("btn-clear-1", HtmlButtonEnum.CLEAR);

            helper.addLabel("txt-activate-reader",
                    PhraseEnum.ACTIVATE_CARD_READER);
            helper.addLabel("txt-swipe-card", PhraseEnum.SWIPE_CARD);

            this.addAccountContext(cm, helper);

        } else {
            helper.discloseLabel("tab-sales");
            helper.discloseLabel("currency-symbol-1");
        }

        if (!cm.isConfigValue(Key.FINANCIAL_POS_DEPOSIT_ENABLE)) {
            helper.discloseLabel("tab-deposit");
            helper.discloseLabel("currency-symbol-2");
            return;
        }

        helper.addLabel("tab-deposit", this.localized("tab-deposit"));

        helper.addButton("btn-clear-2", HtmlButtonEnum.CLEAR);
        helper.addLabel("currency-symbol-2", SpSession.getAppCurrencySymbol());
        helper.addLabel("decimal-separator-2", SpSession.getDecimalSeparator());

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

    /**
     * @param cm
     * @param helper
     */
    private void addAccountContext(final ConfigManager cm,
            final MarkupHelper helper) {

        final Set<UserAccountContextEnum> accountContextSet =
                cm.getConfigEnumSet(UserAccountContextEnum.class,
                        Key.FINANCIAL_POS_SALES_ACCOUNTS);

        final boolean isPaperCutAccount =
                UserAccountContextFactory.hasContextPaperCut()
                        && (accountContextSet.isEmpty() || accountContextSet
                                .contains(UserAccountContextEnum.PAPERCUT));
        //
        String widWlk = "sp-pos-sales-account-papercut";
        UserAccountContextEnum ctxWlk = UserAccountContextEnum.PAPERCUT;
        if (isPaperCutAccount) {
            helper.addModifyLabelAttr(widWlk, ctxWlk.getUiText(),
                    MarkupHelper.ATTR_VALUE, ctxWlk.toString());
        } else {
            helper.discloseLabel(widWlk);
        }
        //
        widWlk = "sp-pos-sales-account-savapage";
        ctxWlk = UserAccountContextEnum.SAVAPAGE;
        if (accountContextSet.contains(ctxWlk)
                || (accountContextSet.isEmpty() && !isPaperCutAccount)) {
            helper.addModifyLabelAttr(widWlk, ctxWlk.getUiText(),
                    MarkupHelper.ATTR_VALUE, ctxWlk.toString());
        } else {
            helper.discloseLabel(widWlk);
        }
    }

    @Override
    protected final boolean needMembership() {
        return false;
    }

}
