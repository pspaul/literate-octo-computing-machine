// @license http://www.gnu.org/licenses/agpl-3.0.html AGPL-3.0

/*! SavaPage jQuery Mobile Admin POS Page | (c) 2020 Datraverse B.V. | GNU
 * Affero General Public License */

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

/*
 * NOTE: the *! comment blocks are part of the compressed version.
 */

/*jslint browser: true*/
/*global $, jQuery, alert*/

/*
 * SavaPage jQuery Mobile Admin Pages.
 */
(function($, window, document, JSON, _ns) {
    "use strict";

    // =========================================================================
    /**
     * Constructor
     */
    _ns.PagePointOfSale = function(_i18n, _view, _model, _api, isMain,
        _salesLocation, _salesShop) {
        var _page = new _ns.Page(_i18n, _view, "#page-point-of-sale",
            (isMain ? "PagePointOfSaleMain" : "PagePointOfSalePage")),
            _self = _ns.derive(_page),
            _quickUserSelected,
            _quickUserSearch = new _ns.QuickObjectSearch(_view, _api),
            //
            _onQuickSearchUserBefore = function() {
                $(".sp-pos-user-selected").hide();
            },
            _onQuickSearchUserItemDisplay = function(item) {
                return item.text + " &bull; " + (item.email || "&nbsp;");
            },
            _onSelectUser = function(quickUserSelected) {

                var attr = "data-savapage",
                    sel = $("#sp-pos-userid");

                _quickUserSelected = quickUserSelected;

                sel.attr(attr, quickUserSelected.key);
                sel.val(quickUserSelected.text);

                $("#sp-pos-user-balance").text(quickUserSelected.balance);
                $("#sp-pos-receipt-as-email-label").html(quickUserSelected.email || "&nbsp;");
                $(".sp-pos-user-selected").show();

                $("#sp-pos-amount-main").focus();
            },
            _onClearUser = function() {
                $.noop();
            },
            _clearSales = function() {
                $("#sp-pos-sales-amount-main").val("");
                $("#sp-pos-sales-comment").val("");
                _view.setSelectedValue($('#sp-pos-sales-price-list'), '');
                _view.checkRadioValue('sp-pos-sales-price-radio', '');
                _view.asyncFocus($("#sp-pos-sales-amount-cents").val(""));
            },
            _clear = function() {
                $("#sp-pos-userid").val("").focus();
                $("#sp-pos-amount-main").val("");
                $("#sp-pos-comment").val("");
                $("#sp-pos-amount-cents").val("00");
                $(".sp-pos-user-selected").hide();
            },
            // 
            _onEnableSalesItems = function(locationID, shopID, itemID) {
                var sel;
                if (!locationID) {
                    sel = _view.getSelPresent($('#sp-pos-sales-location-list'));
                    if (sel) {
                        locationID = sel.find(':selected').val() !== '';
                    } else {
                        locationID = 'no-location';
                    }
                }
                if (!shopID) {
                    sel = _view.getSelPresent($('#sp-pos-sales-shop-list'));
                    if (sel) {
                        shopID = sel.find(':selected').val() !== '';
                    } else {
                        shopID = 'no-shop';
                    }
                }
                _view.enableUI($('#sp-a-menu'), locationID && shopID);

                if (!itemID) {
                    sel = _view.getSelPresent($('#sp-pos-sales-item-list'));
                    if (sel) {
                        itemID = sel.val() !== '';
                    } else {
                        sel = _view.getSelPresent($('#sp-pos-sales-item-radio-list'));
                        if (sel) {
                            itemID = _view.getRadioValue('sp-pos-sales-item-radio');
                        } else {
                            itemID = 'no-item';
                        }
                    }
                }
                _view.enableUI($('.sp-a-menu-price'), locationID && shopID && itemID);
            },
            //
            _onSalesLocationSelect = function() {
                var selLocation = $('#sp-pos-sales-location-list :selected'),
                    locationID = selLocation.val(),
                    selShopList = $('#sp-pos-sales-shop-list'),
                    selShops = selShopList.find('option'),
                    selItemList = $('#sp-pos-sales-item-list'),
                    selItems = selItemList.find('option');

                _view.visible(selShops, false);
                _view.visible(selItems, false);

                _view.visible(selShops.filter('.sp-pos-sales-location'), true);
                _view.visible(selItems.filter('.sp-pos-sales-location'), true);

                _view.setSelectedValue(selShopList, '');
                _view.setSelectedValue(selItemList, '');

                if (locationID) {
                    _view.visible(selShops.filter('.sp-pos-sales-location-' + locationID), true);
                    _view.visible(selItems.filter('.sp-pos-sales-location-' + locationID), true);
                }
                _onEnableSalesItems(locationID, null);
            },
            //
            _onSalesShopSelect = function() {
                var selShop = $('#sp-pos-sales-shop-list :selected'),
                    shopID = selShop.val(),
                    selItemList = $('#sp-pos-sales-item-list'),
                    selItems = selItemList.find('option'),
                    selItemsRadio = $('.sp-pos-sales-item-radio-label');

                _view.visible(selItems, false);
                _view.visible(selItemsRadio.parent(), false);

                _view.visible(selItems.filter('.sp-pos-sales-shop'), true);
                _view.visible(selItemsRadio.filter('.sp-pos-sales-shop').parent(), true);

                _view.setSelectedValue(selItemList, '');
                _view.uncheckRadioValue('sp-pos-sales-item-radio');

                if (shopID) {
                    _view.visible(selItems.filter('.sp-pos-sales-shop-' + shopID), true);
                    _view.visible(selItemsRadio.filter('.sp-pos-sales-shop-' + shopID).parent(), true);
                }
                _onEnableSalesItems(null, shopID);
            },
            //
            _onSalesItemSelect = function() {
                var selItem = $('#sp-pos-sales-item-list :selected'),
                    itemID = selItem.val() || _view.getRadioValue('sp-pos-sales-item-radio'),
                    selPriceList = $('#sp-pos-sales-price-list'),
                    selPrices = selPriceList.find('option'),
                    selPricesRadio = $('.sp-pos-sales-price-radio-label'),
                    clazzDomainFilter = '.sp-pos-sales-item';

                _view.visible(selPrices, false);
                _view.visible(selPricesRadio.parent(), false);

                _view.visible(selPrices.filter(clazzDomainFilter), true);
                _view.visible(selPricesRadio.filter(clazzDomainFilter).parent(), true);

                _clearSales();

                if (itemID) {
                    _view.visible(selPrices.filter(clazzDomainFilter + '-' + itemID), true);
                    _view.visible(selPricesRadio.filter(clazzDomainFilter + '-' + itemID).parent(), true);
                }
                _onEnableSalesItems(null, null, itemID);
            },
            //
            _onSalesPriceSelect = function() {
                var priceID = $('#sp-pos-sales-price-list :selected').val() ||
                    _view.getRadioValue('sp-pos-sales-price-radio');
                $("#sp-pos-sales-amount-main").val(priceID.slice(0, -2) || '0');
                $("#sp-pos-sales-amount-cents").val(priceID.slice(-2));
                $('#sp-pos-sales-user-card-local-group').focus();
            },
            //
            _onSales = function(userKey, userId) {
                var sel, res, sound, soundURL,
                    posLocation, posShop, posItem;

                sel = $('#sp-pos-sales-location-list');
                posLocation = sel.length > 0 ? sel.val() : null;

                sel = $('#sp-pos-sales-shop-list');
                posShop = sel.length > 0 ? sel.val() : null;

                sel = $('#sp-pos-sales-item-list');
                posItem = sel.length > 0 ? sel.val() :
                    _view.getRadioValue('sp-pos-sales-item-radio');

                res = _api.call({
                    request: "pos-sales",
                    dto: JSON.stringify({
                        userKey: userKey,
                        accountContext: $('#sp-pos-sales-account').val(),
                        userId: userId,
                        amountMain: $("#sp-pos-sales-amount-main").val(),
                        amountCents: $("#sp-pos-sales-amount-cents").val(),
                        posLocation: posLocation,
                        posShop: posShop,
                        posItem: posItem,
                        comment: $("#sp-pos-sales-comment").val(),
                        invoiceDelivery: undefined
                    })
                });

                _view.showApiMsg(res);

                if (res.result.code === _ns.ApiResultCodeEnum.OK ||
                    res.result.code === _ns.ApiResultCodeEnum.INFO) {
                    sound = _model.sounds.success;
                    if (_api.call({
                        request: "user-notify-account-change",
                        dto: JSON.stringify({
                            key: userKey
                        })
                    }).result.code !== _ns.ApiResultCodeEnum.OK) {
                        _view.showApiMsg(res);
                    }
                    _clearSales();
                } else {
                    sound = _model.sounds.failure;
                }
                _ns.playSound(sound);
            },
            _onDeposit = function() {
                var sel = $('#sp-pos-payment-type')
                    // PosDepositDto
                    ,
                    res = _api.call({
                        request: "pos-deposit",
                        dto: JSON.stringify({
                            userId: _quickUserSelected.text,
                            amountMain: $("#sp-pos-amount-main").val(),
                            amountCents: $("#sp-pos-amount-cents").val(),
                            comment: $("#sp-pos-comment").val(),
                            paymentType: (sel ? sel.val() : undefined),
                            receiptDelivery: _view.getRadioValue('sp-pos-receipt-delivery'),
                            userEmail: _quickUserSelected.email
                        })
                    });

                _view.showApiMsg(res);

                if (res.result.code === _ns.ApiResultCodeEnum.OK) {

                    if (_api.call({
                        request: "user-notify-account-change",
                        dto: JSON.stringify({
                            key: _quickUserSelected.key
                        })
                    }).result.code !== _ns.ApiResultCodeEnum.OK) {
                        _view.showApiMsg(res);
                    }

                    _clear();
                }
            },
            // Get Date as yyyymmdd. Usage: _getQuickDate(new Date())
            _getQuickDate = function(date) {
                var yyyy = date.getFullYear().toString(),
                    mm = (date.getMonth() + 1).toString(),
                    // getMonth() is zero-based
                    dd = date.getDate().toString();
                // padding
                return yyyy + (mm[1] ? mm : "0" + mm[0]) + (dd[1] ? dd : "0" + dd[0]);
            },
            _onQuickPurchaseSearch = function(target, filter) {
                /* QuickSearchFilterDto */
                var res,
                    btnCls = "ui-btn ui-btn-inline ui-btn-icon-left ui-mini",
                    html = "";

                if (filter && filter.length > 0) {
                    res = _api.call({
                        request: "pos-deposit-quick-search",
                        dto: JSON.stringify({
                            filter: filter,
                            maxResults: 20
                        })
                    });
                    if (res.result.code === _ns.ApiResultCodeEnum.OK) {

                        $.each(res.dto.items, function(key, item) {

                            // item = QuickSearchPosPurchaseItemDto

                            html += "<li>";
                            html += "<h3 class=\"sp-txt-wrap\">" + item.userId + "</h3>";
                            html += "<div class=\"sp-txt-wrap\">" + item.totalCost;
                            if (item.comment) {
                                html += " &bull; " + item.comment;
                            }
                            html += "</div>";

                            // Download + mail buttons
                            html += "<div>";
                            html += "<a tabindex=\"0\" href=\"#\" data-savapage=\"" + item.key + "\" class=\"sp-download-receipt ui-icon-arrow-d " + btnCls + "\">PDF</a>";
                            if (item.userEmail) {
                                html += "<a tabindex=\"0\" href=\"#\" data-savapage=\"" + item.key + "\" class=\"sp-download-mail ui-icon-mail " + btnCls + "\">" + item.userEmail + "</a>";
                            }
                            html += "</div>";

                            /*
                             * IMPORTANT: The filter MUST be part of the item
                             * text. If filter is NOT part of the item, the item
                             * is hidden by JQM, because of <input
                             * data-type="search"
                             */
                            html += "<span style=\"font-size:0px;\">" + filter + "</span>";
                            html += "<p class=\"ui-li-aside\">" + item.dateTime + "</p>";

                            html += "</li>";
                        });
                    } else {
                        _view.showApiMsg(res);
                    }
                }
                target.html(html).filterable("refresh");
            };

        /**
         *
         */
        $(_self.id()).on('pagecreate', function(event) {

            var sel, filterableDateTime = $("#sp-pos-quickdate-filter");

            $(this).on('click', '#sp-pos-button-deposit', null, function() {
                _onDeposit();
            });

            $(this).on('click', '#sp-pos-button-clear', null, function() {
                _clear();
            }).on('click', '#sp-pos-sales-button-clear', null, function() {
                _clearSales();
            }).on('click', "#sp-pos-tab-sales-button", null, function() {
                _clearSales();
            });

            //
            sel = $('#sp-pos-sales-location-list');
            if (_salesLocation) {
                _view.enableUI(sel, false);
            } else {
                sel.change(function() {
                    _onSalesLocationSelect();
                    return false;
                });
            }
            sel = $('#sp-pos-sales-shop-list');
            if (_salesShop) {
                _view.enableUI(sel, false);
            } else {
                sel.change(function() {
                    _onSalesShopSelect();
                    return false;
                });
            }
            sel = $('#sp-pos-sales-item-list');
            sel.change(function() {
                _onSalesItemSelect();
                return false;
            });
            sel = $("input:radio[name='" + 'sp-pos-sales-item-radio' + "']")
            sel.change(function() {
                _onSalesItemSelect();
                return false;
            });
            sel = $("input:radio[name='" + 'sp-pos-sales-price-radio' + "']")
            sel.change(function() {
                _onSalesPriceSelect();
                return false;
            });

            sel = $('#sp-pos-sales-price-list');
            sel.change(function() {
                _onSalesPriceSelect();
                return false;
            });

            //
            $(this).on('click', "#sp-pos-tab-deposit-button", null, function() {
                _view.asyncFocus($("#sp-pos-userid").val(""));
            });

            $(this).on('click', "#sp-pos-tab-receipts-button", null, function() {
                var value = _getQuickDate(new Date());
                $("#sp-pos-quickdate").val(value).focus();
                _onQuickPurchaseSearch(filterableDateTime, value);
            });

            // Receipts tab
            filterableDateTime.on("filterablebeforefilter", function(e, data) {
                _onQuickPurchaseSearch($(this), data.input.get(0).value);
            });

            $(this).on('click', ".sp-download-receipt", null, function() {
                _api.download("pos-receipt-download", null, $(this).attr('data-savapage'));
                return false;
            });

            $(this).on('click', ".sp-download-mail", null, function() {
                var res = _api.call({
                    request: "pos-receipt-sendmail",
                    // PrimaryKeyDto
                    dto: JSON.stringify({
                        key: $(this).attr('data-savapage')
                    })
                });
                _view.showApiMsg(res);
                return false;
            });

            $(this).on('click', ".sp-pos-button-back", null, function() {
                if (_self.onBack) {
                    return _self.onBack();
                }
                return true;
            });

            _quickUserSearch.onCreate($(this), 'sp-pos-userid-filter', 'user-quick-search', null, _onQuickSearchUserItemDisplay, _onSelectUser, _onClearUser, _onQuickSearchUserBefore);

            _ns.KeyboardLogger.setCallback($('#sp-pos-sales-user-card-local-group'), _model.cardLocalMaxMsecs,
                //
                function() {// focusIn
                    $('#sp-pos-sales-user-card-local-focusout').hide();
                    $('#sp-pos-sales-user-card-local-focusin').show();
                    $('#sp-pos-sales-user-card-local-group').parent().css('background-color', 'lightgreen');
                    $("#sp-pos-sales-amount-main").css('background-color', 'lightgreen');
                    $("#sp-pos-sales-amount-cents").css('background-color', 'lightgreen');
                }, function() {// focusOut
                    $('#sp-pos-sales-user-card-local-focusin').hide();
                    $('#sp-pos-sales-user-card-local-focusout').show();
                    $('#sp-pos-sales-user-card-local-group').parent().css('background-color', '');
                    $("#sp-pos-sales-amount-main").css('background-color', '');
                    $("#sp-pos-sales-amount-cents").css('background-color', '');
                }, function(id) {
                    var res = _api.call({
                        request: 'usercard-quick-search',
                        dto: JSON.stringify({
                            card: id
                        })
                    });
                    if (res.result.code !== _ns.ApiResultCodeEnum.OK) {
                        _view.message(res.result.txt);
                        _ns.playSound(_model.sounds.failure);
                    } else {
                        _onSales(res.dto.key, res.dto.text);
                    }
                });

        }).on("pagebeforeshow", function(event, ui) {
            var sel;
            sel = $('#sp-pos-sales-location-list');
            if (sel.length > 0) {
                _view.setSelectedValue(sel, _salesLocation);
                _onSalesLocationSelect();
            }
            sel = $('#sp-pos-sales-shop-list');
            if (sel.length > 0) {
                _view.setSelectedValue(sel, _salesShop);
                _onSalesShopSelect();
            }

        }).on("pageshow", function(event, ui) {
            // open first tab
            $('.sp-pos-tab-button').get(0).click();
            _clear();
        });

        return _self;
    };

}(jQuery, this, this.document, JSON, this.org.savapage));

// @license-end
