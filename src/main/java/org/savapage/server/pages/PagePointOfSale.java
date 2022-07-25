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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dto.PosSalesItemDto;
import org.savapage.core.dto.PosSalesLabelDomainPartDto;
import org.savapage.core.dto.PosSalesLocationDto;
import org.savapage.core.dto.PosSalesPriceDto;
import org.savapage.core.dto.PosSalesShopDto;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
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

    /** */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /** */
    private static final String WID_TAB_SALES = "tab-sales";
    /** */
    private static final String WID_CURRENCY_SYMBOL_1 = "currency-symbol-1";

    /** */
    private static final String WID_POS_SALES_COMMENT = "prompt-comment-1";

    /** */
    private static final String WID_POS_SALES_LOCATION_OPTION_SELECT =
            "pos-sales-location-option-select";
    /** */
    private static final String WID_POS_SALES_SHOP_OPTION_SELECT =
            "pos-sales-shop-option-select";
    /** */
    private static final String WID_POS_SALES_ITEM_OPTION_SELECT =
            "pos-sales-item-option-select";

    /** */
    private static final String WID_POS_SALES_ITEM_RADIO =
            "pos-sales-item-radio";

    private static final String WID_POS_SALES_PRICE_OPTION_SELECT =
            "pos-sales-price-option-select";

    /** */
    private static final String WID_POS_SALES_PRICE_RADIO =
            "pos-sales-price-radio";

    /** */
    private static final String CSS_CLASS_POS_SALES_LOCATION =
            "sp-pos-sales-location";

    /** */
    private static final String CSS_CLASS_POS_SALES_LOCATION_PFX =
            CSS_CLASS_POS_SALES_LOCATION + "-";

    /** */
    private static final String CSS_CLASS_POS_SALES_SHOP = "sp-pos-sales-shop";

    /** */
    private static final String CSS_CLASS_POS_SALES_SHOP_PFX =
            CSS_CLASS_POS_SALES_SHOP + "-";

    /** */
    private static final String CSS_CLASS_POS_SALES_ITEM = "sp-pos-sales-item";

    /** */
    private static final String CSS_CLASS_POS_SALES_ITEM_PFX =
            CSS_CLASS_POS_SALES_ITEM + "-";

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

            helper.addLabel(WID_TAB_SALES, this.localized("tab-sales"));
            helper.addLabel(WID_CURRENCY_SYMBOL_1,
                    SpSession.getAppCurrencySymbol());
            helper.addLabel("decimal-separator-1",
                    SpSession.getDecimalSeparator());

            helper.addButton("btn-clear-1", HtmlButtonEnum.CLEAR);

            helper.addLabel("txt-activate-reader",
                    PhraseEnum.ACTIVATE_CARD_READER);
            helper.addLabel("txt-swipe-card", PhraseEnum.SWIPE_CARD);

            this.addAccountContext(cm, helper);
            this.addPosSalesLabels(cm, helper);

            // helper.addLabel(WID_POS_SALES_COMMENT, NounEnum.NOTE);
            helper.discloseLabel(WID_POS_SALES_COMMENT);

        } else {
            helper.discloseLabel(WID_TAB_SALES);
            helper.discloseLabel(WID_CURRENCY_SYMBOL_1);
            helper.discloseLabel(WID_POS_SALES_LOCATION_OPTION_SELECT);
            helper.discloseLabel(WID_POS_SALES_SHOP_OPTION_SELECT);

            helper.discloseLabel(WID_POS_SALES_ITEM_OPTION_SELECT);
            helper.discloseLabel(WID_POS_SALES_ITEM_RADIO);

            helper.discloseLabel(WID_POS_SALES_PRICE_OPTION_SELECT);
            helper.discloseLabel(WID_POS_SALES_PRICE_RADIO);

            helper.discloseLabel(WID_POS_SALES_COMMENT);
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

    /**
     * @param cm
     * @param helper
     */
    private void addPosSalesLabels(final ConfigManager cm,
            final MarkupHelper helper) {

        final boolean isPosSalesLocationsEnable = cm
                .isConfigValue(Key.FINANCIAL_POS_SALES_LABEL_LOCATIONS_ENABLE);

        final Collection<PosSalesLocationDto> posSalesLocations;

        @SuppressWarnings("unused")
        final UserIdDto userIdDto = SpSession.get().getUserIdDto();

        if (isPosSalesLocationsEnable) {

            posSalesLocations = ACCOUNTING_SERVICE.getPosSalesLocationsByName();

            if (!posSalesLocations.isEmpty()) {

                helper.encloseLabel("pos-sales-location-option-select",
                        NounEnum.LOCATION.uiText(getLocale()).toLowerCase()
                                .concat(HtmlButtonEnum.DOTTED_SUFFIX),
                        true);

                this.addPosSalesLocations(posSalesLocations);
            }
        } else {
            posSalesLocations = null;
        }

        if (posSalesLocations == null || posSalesLocations.isEmpty()) {
            helper.discloseLabel(WID_POS_SALES_LOCATION_OPTION_SELECT);
        }

        //
        final boolean hasShops;

        if (cm.isConfigValue(Key.FINANCIAL_POS_SALES_LABEL_SHOPS_ENABLE)) {

            final Collection<PosSalesShopDto> posSalesShops =
                    ACCOUNTING_SERVICE.getPosSalesShopsByName();
            hasShops = !posSalesShops.isEmpty();
            if (hasShops) {
                MarkupHelper.modifyLabelAttr(
                        helper.addLabel(WID_POS_SALES_SHOP_OPTION_SELECT,
                                NounEnum.SHOP.uiText(getLocale()).toLowerCase()
                                        .concat(HtmlButtonEnum.DOTTED_SUFFIX)),
                        MarkupHelper.ATTR_CLASS, CSS_CLASS_POS_SALES_LOCATION);

                this.addPosSalesShops(isPosSalesLocationsEnable, posSalesShops);
            }
        } else {
            hasShops = false;
        }
        if (!hasShops) {
            helper.discloseLabel(WID_POS_SALES_SHOP_OPTION_SELECT);
        }

        //
        final boolean hasItems;

        if (cm.isConfigValue(Key.FINANCIAL_POS_SALES_LABEL_ITEMS_ENABLE)) {
            final Collection<PosSalesItemDto> posSalesItems =
                    ACCOUNTING_SERVICE.getPosSalesItemsByName();
            hasItems = !posSalesItems.isEmpty();
            if (hasItems) {
                this.addPosSalesItems(helper, hasShops, posSalesItems,
                        cm.getConfigInt(
                                Key.WEBAPP_POS_SALES_LABEL_ITEMS_BUTTON_MAX));
            }
        } else {
            hasItems = false;
        }
        if (!hasItems) {
            helper.discloseLabel(WID_POS_SALES_ITEM_OPTION_SELECT);
            helper.discloseLabel(WID_POS_SALES_ITEM_RADIO);
        }

        //
        final boolean hasPrices;

        if (cm.isConfigValue(Key.FINANCIAL_POS_SALES_LABEL_PRICES_ENABLE)) {
            final Collection<PosSalesPriceDto> posSalesPrices =
                    ACCOUNTING_SERVICE.getPosSalesPricesByName();
            hasPrices = !posSalesPrices.isEmpty();
            if (hasPrices) {
                this.addPosSalesPrices(helper, hasItems, posSalesPrices,
                        cm.getConfigInt(
                                Key.WEBAPP_POS_SALES_LABEL_PRICES_BUTTON_MAX));
            }
        } else {
            hasPrices = false;
        }
        if (!hasPrices) {
            helper.discloseLabel(WID_POS_SALES_PRICE_OPTION_SELECT);
            helper.discloseLabel(WID_POS_SALES_PRICE_RADIO);
        }

    }

    /**
     * Adds the Pos Sales Location as select options.
     *
     * @param posSalesLocations
     *            The locations.
     */
    private void addPosSalesLocations(
            final Collection<PosSalesLocationDto> posSalesLocations) {

        final List<PosSalesLocationDto> locations = new ArrayList<>();

        for (final PosSalesLocationDto location : posSalesLocations) {
            locations.add(location);
        }

        add(new PropertyListView<PosSalesLocationDto>(
                "pos-sales-location-option", locations) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void
                    populateItem(final ListItem<PosSalesLocationDto> item) {

                final PosSalesLocationDto dto = item.getModel().getObject();
                final Label label = new Label("option", dto.getName());

                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_VALUE,
                        dto.getId());

                item.add(label);
            }
        });
    }

    /**
     * Adds the POS Sales Shops select options.
     *
     * @param isPosSalesLocationsEnable
     *            If {@code true} POS Sales location label is enabled.
     * @param posSalesShops
     *            The shops.
     */
    private void addPosSalesShops(final boolean isPosSalesLocationsEnable,
            final Collection<PosSalesShopDto> posSalesShops) {

        final List<PosSalesLabelDomainPartDto> labels = new ArrayList<>();

        for (final PosSalesShopDto dto : posSalesShops) {
            if (isPosSalesLocationsEnable || dto.getDomainIDs().isEmpty()) {
                labels.add(dto);
            }
        }

        this.addPosSalesLabelDomainPart("pos-sales-shop-option",
                isPosSalesLocationsEnable, labels, CSS_CLASS_POS_SALES_LOCATION,
                CSS_CLASS_POS_SALES_LOCATION_PFX);
    }

    /**
     * Adds the POS Sales Items as select (radio) options.
     *
     * @param helper
     *            MarkupHelper.
     * @param hasShops
     *            If {@code true} POS Sales Shops are present.
     * @param posSalesItems
     *            The items.
     * @param maxRadioButtons
     *            Max radio buttons.
     */
    private void addPosSalesItems(final MarkupHelper helper,
            final boolean hasShops,
            final Collection<PosSalesItemDto> posSalesItems,
            final int maxRadioButtons) {

        final List<PosSalesLabelDomainPartDto> labels = new ArrayList<>();

        for (final PosSalesItemDto dto : posSalesItems) {
            if (hasShops || dto.getDomainIDs().isEmpty()) {
                labels.add(dto);
            }
        }

        if (posSalesItems.size() > maxRadioButtons) {

            MarkupHelper.modifyLabelAttr(
                    helper.addLabel(WID_POS_SALES_ITEM_OPTION_SELECT,
                            NounEnum.ITEM.uiText(getLocale()).toLowerCase()
                                    .concat(HtmlButtonEnum.DOTTED_SUFFIX)),
                    MarkupHelper.ATTR_CLASS, CSS_CLASS_POS_SALES_SHOP);

            this.addPosSalesLabelDomainPart("pos-sales-item-option", hasShops,
                    labels, CSS_CLASS_POS_SALES_SHOP,
                    CSS_CLASS_POS_SALES_SHOP_PFX);

            helper.discloseLabel(WID_POS_SALES_ITEM_RADIO);

        } else {
            this.addPosSalesLabelDomainPartRadio(WID_POS_SALES_ITEM_RADIO,
                    hasShops, labels, CSS_CLASS_POS_SALES_SHOP,
                    CSS_CLASS_POS_SALES_SHOP_PFX);

            helper.discloseLabel(WID_POS_SALES_ITEM_OPTION_SELECT);
        }
    }

    /**
     * Adds the POS Sales Prices as select (radio) options.
     *
     * @param helper
     *            MarkupHelper.
     * @param hasItems
     *            If {@code true} POS Sales Items are present.
     * @param posSalesPrices
     *            The prices.
     * @param maxRadioButtons
     *            Max radio buttons.
     */
    private void addPosSalesPrices(final MarkupHelper helper,
            final boolean hasItems,
            final Collection<PosSalesPriceDto> posSalesPrices,
            final int maxRadioButtons) {

        final List<PosSalesLabelDomainPartDto> labels = new ArrayList<>();

        for (final PosSalesPriceDto dto : posSalesPrices) {
            if (hasItems || dto.getDomainIDs().isEmpty()) {
                labels.add(dto);
            }
        }

        if (posSalesPrices.size() > maxRadioButtons) {

            MarkupHelper.modifyLabelAttr(
                    helper.addLabel(WID_POS_SALES_PRICE_OPTION_SELECT,
                            ServiceContext.getAppCurrencySymbol()
                                    .concat(HtmlButtonEnum.DOTTED_SUFFIX)),
                    MarkupHelper.ATTR_CLASS, CSS_CLASS_POS_SALES_ITEM);

            this.addPosSalesLabelDomainPart("pos-sales-price-option", hasItems,
                    labels, CSS_CLASS_POS_SALES_ITEM,
                    CSS_CLASS_POS_SALES_ITEM_PFX);

            helper.discloseLabel(WID_POS_SALES_PRICE_RADIO);

        } else {
            this.addPosSalesLabelDomainPartRadio(WID_POS_SALES_PRICE_RADIO,
                    hasItems, labels, CSS_CLASS_POS_SALES_ITEM,
                    CSS_CLASS_POS_SALES_ITEM_PFX);

            helper.discloseLabel(WID_POS_SALES_PRICE_OPTION_SELECT);
        }
    }

    /**
     *
     * Fills select options.
     *
     * @param wicketID
     *            Wicket ID of option list.
     * @param isPosSalesLocationsEnable
     *            If {@code true} POS Sales location label is enabled.
     * @param labels
     *            The job ticket labels.
     * @param domainCssClass
     *            CSS domain class for unrestricted parts.
     * @param domainCssClassPfx
     *            CSS domain class prefix for domain restricted part.
     */
    private void addPosSalesLabelDomainPart(final String wicketID,
            final boolean isPosSalesLocationsEnable,
            final List<PosSalesLabelDomainPartDto> labels,
            final String domainCssClass, final String domainCssClassPfx) {

        add(new PropertyListView<PosSalesLabelDomainPartDto>(wicketID, labels) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(
                    final ListItem<PosSalesLabelDomainPartDto> item) {
                final PosSalesLabelDomainPartDto dto =
                        item.getModel().getObject();

                final Label label = new Label("option", dto.getName());
                item.add(label);

                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_VALUE,
                        dto.getId());

                if (!isPosSalesLocationsEnable) {
                    return;
                }

                if (dto.getDomainIDs().isEmpty()) {
                    MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_CLASS,
                            domainCssClass);
                    return;
                }

                boolean first = true;

                for (final String id : dto.getDomainIDs()) {
                    final String cssClass = domainCssClassPfx.concat(id);
                    if (first) {
                        MarkupHelper.modifyLabelAttr(label,
                                MarkupHelper.ATTR_CLASS, cssClass);
                    } else {
                        MarkupHelper.appendLabelAttr(label,
                                MarkupHelper.ATTR_CLASS, cssClass);
                    }
                    first = false;
                }
            }
        });
    }

    /**
     *
     * Fills radio button set.
     *
     * @param wicketID
     *            Wicket ID of option list.
     * @param isPosSalesLocationsEnable
     *            If {@code true} POS Sales location label is enabled.
     * @param labels
     *            The job ticket labels.
     * @param domainCssClass
     *            CSS domain class for unrestricted parts.
     * @param domainCssClassPfx
     *            CSS domain class prefix for domain restricted part.
     */
    private void addPosSalesLabelDomainPartRadio(final String wicketID,
            final boolean isPosSalesLocationsEnable,
            final List<PosSalesLabelDomainPartDto> labels,
            final String domainCssClass, final String domainCssClassPfx) {

        add(new PropertyListView<PosSalesLabelDomainPartDto>(wicketID, labels) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(
                    final ListItem<PosSalesLabelDomainPartDto> item) {

                final PosSalesLabelDomainPartDto dto =
                        item.getModel().getObject();

                final String idFor = UUID.randomUUID().toString();

                // Label
                final Label label1 = new Label("label", dto.getName());
                item.add(label1);
                MarkupHelper.modifyLabelAttr(label1, MarkupHelper.ATTR_FOR,
                        idFor);

                // Input
                final Label label = new Label("input", "");
                item.add(label);
                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_VALUE,
                        dto.getId());
                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_ID,
                        idFor);

                if (!isPosSalesLocationsEnable) {
                    return;
                }

                if (dto.getDomainIDs().isEmpty()) {
                    MarkupHelper.appendLabelAttr(label1,
                            MarkupHelper.ATTR_CLASS, domainCssClass);
                    return;
                }

                boolean first = true;

                for (final String id : dto.getDomainIDs()) {
                    final String cssClass = domainCssClassPfx.concat(id);
                    if (first) {
                        MarkupHelper.appendLabelAttr(label1,
                                MarkupHelper.ATTR_CLASS, cssClass);
                        MarkupHelper.appendLabelAttr(label,
                                MarkupHelper.ATTR_CLASS, cssClass);
                    } else {
                        MarkupHelper.appendLabelAttr(label1,
                                MarkupHelper.ATTR_CLASS, cssClass);
                        MarkupHelper.appendLabelAttr(label,
                                MarkupHelper.ATTR_CLASS, cssClass);
                    }
                    first = false;
                }
            }
        });
    }

    @Override
    protected final boolean needMembership() {
        return false;
    }

}
