/*! (c) 2011-2015 Datraverse B.V. */

/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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

/*
 * NOTE: the *! comment blocks are part of the compressed version.
 */

/* jslint browser: true */
/* global $, jQuery, alert */

/*
 * SavaPage jQuery Mobile Admin Pages.
 */
( function($, window, document, JSON, _ns) {"use strict";

        // =========================================================================
        /**
         * Constructor
         */
        _ns.PagePointOfSale = function(_i18n, _view, _model, _api) {

            //_self.onBack();

            /*
            * SavaPage call-backs from pagecontainershow / pagecontainerhide.
            */
            //_self.onPageShow()
            //_self.onPageHide()

            var _page = new _ns.Page(_i18n, _view, "#page-point-of-sale", "admin.PagePointOfSale")
            //
            , _self = _ns.derive(_page)
            //
            //,                       _this = this
            , _quickUserCache = []
            //
            , _quickUserSelected
            //
            , _onQuickUserSearch = function(target, filter) {
                /* QuickSearchFilterDto */
                var res, html = "";

                $(".sp-pos-user-selected").hide();

                _quickUserCache = [];
                _quickUserSelected = undefined;

                if (filter && filter.length > 0) {
                    res = _api.call({
                        request : "user-quick-search",
                        dto : JSON.stringify({
                            filter : filter,
                            maxResults : 5
                        })
                    });
                    if (res.result.code === '0') {
                        _quickUserCache = res.items;
                        $.each(_quickUserCache, function(key, item) {
                            html += "<li class=\"ui-mini\" data-savapage=\"" + key + "\"><a tabindex=\"2\" href=\"#\">" + item.text + " &bull; " + (item.email || "&nbsp;") + "</a></li>";
                        });
                    } else {
                        _view.showApiMsg(res);
                    }
                }
                target.html(html).filterable("refresh");
            }
            //
            , _onSelectUser = function(selection, filterable) {
                var attr = "data-savapage"
                //
                , sel = $("#sp-pos-userid")
                //
                //
                ;
                _quickUserSelected = _quickUserCache[selection.attr(attr)];

                sel.attr(attr, _quickUserSelected.key);
                sel.val(_quickUserSelected.text);

                filterable.empty();

                $("#sp-pos-user-balance").text(_quickUserSelected.balance);
                $("#sp-pos-receipt-as-email-label").html(_quickUserSelected.email || "&nbsp;");
                $(".sp-pos-user-selected").show();

                $("#sp-pos-amount-main").focus();
            }
            //
            , _clear = function() {
                $("#sp-pos-userid").val("").focus();
                $("#sp-pos-amount-main").val("");
                $("#sp-pos-comment").val("");
                $("#sp-pos-amount-cents").val("00");
                $(".sp-pos-user-selected").hide();
            }
            //
            , _onDeposit = function() {

                var sel = $('#sp-pos-payment-type')
                // PosDepositDto
                , res = _api.call({
                    request : "pos-deposit",
                    dto : JSON.stringify({
                        userId : _quickUserSelected.text,
                        amountMain : $("#sp-pos-amount-main").val(),
                        amountCents : $("#sp-pos-amount-cents").val(),
                        comment : $("#sp-pos-comment").val(),
                        paymentType : ( sel ? sel.val() : undefined ),
                        receiptDelivery : _view.getRadioValue('sp-pos-receipt-delivery'),
                        userEmail : _quickUserSelected.email
                    })
                })
                //
                ;
                _view.showApiMsg(res);

                if (res.result.code === '0') {

                    if (_api.call({
                        request : "user-notify-account-change",
                        dto : JSON.stringify({
                            key : _quickUserSelected.key
                        })
                    }).result.code !== '0') {
                        _view.showApiMsg(res);
                    }

                    _clear();
                }
            }
            // Get Date as yyyymmdd. Usage: _getQuickDate(new Date())
            , _getQuickDate = function(date) {
                var yyyy = date.getFullYear().toString()
                //
                , mm = (date.getMonth() + 1).toString()
                // getMonth() is zero-based
                , dd = date.getDate().toString()
                //
                ;
                // padding
                return yyyy + (mm[1] ? mm : "0" + mm[0]) + (dd[1] ? dd : "0" + dd[0]);
            }
            //
            , _onQuickPurchaseSearch = function(target, filter) {
                /* QuickSearchFilterDto */
                var res
                //
                , btnCls = "ui-btn ui-btn-inline ui-btn-icon-left ui-mini"
                //
                , html = ""
                //
                ;

                if (filter && filter.length > 0) {
                    res = _api.call({
                        request : "pos-deposit-quick-search",
                        dto : JSON.stringify({
                            filter : filter,
                            maxResults : 20
                        })
                    });
                    if (res.result.code === '0') {

                        $.each(res.items, function(key, item) {

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
                            html += "<a tabindex=\"2\" href=\"#\" data-savapage=\"" + item.key + "\" class=\"sp-download-receipt ui-icon-arrow-d " + btnCls + "\">PDF</a>";
                            if (item.userEmail) {
                                html += "<a tabindex=\"2\" href=\"#\" data-savapage=\"" + item.key + "\" class=\"sp-download-mail ui-icon-mail " + btnCls + "\">" + item.userEmail + "</a>";
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

                var filterableUserId = $("#sp-pos-userid-filter")
                //
                , filterableDateTime = $("#sp-pos-quickdate-filter")
                //
                ;

                // Deposit tab
                filterableUserId.on("filterablebeforefilter", function(e, data) {
                    e.preventDefault();
                    _onQuickUserSearch($(this), data.input.get(0).value);
                });

                $(this).on('click', '#sp-pos-userid-filter li', null, function() {
                    _onSelectUser($(this), filterableUserId);
                });

                $(this).on('click', '#sp-pos-button-deposit', null, function() {
                    _onDeposit();
                });

                $(this).on('click', '#sp-pos-button-clear', null, function() {
                    _clear();
                });

                $(this).on('click', "#sp-pos-tab-deposit-button", null, function() {
                    $("#sp-pos-userid").val("").focus();
                });

                $(this).on('click', "#sp-pos-tab-receipts-button", null, function() {
                    var value = _getQuickDate(new Date());
                    $("#sp-pos-quickdate").val(value).focus();
                    _onQuickPurchaseSearch(filterableDateTime, value);
                });

                // Receipts tab
                filterableDateTime.on("filterablebeforefilter", function(e, data) {
                    e.preventDefault();
                    _onQuickPurchaseSearch($(this), data.input.get(0).value);
                });

                $(this).on('click', ".sp-download-receipt", null, function() {
                    _api.download("pos-receipt-download", null, $(this).attr('data-savapage'));
                    return false;
                });

                $(this).on('click', ".sp-download-mail", null, function() {
                    var res = _api.call({
                        request : "pos-receipt-sendmail",
                        // PrimaryKeyDto
                        dto : JSON.stringify({
                            key : $(this).attr('data-savapage')
                        })
                    });
                    _view.showApiMsg(res);
                    return false;
                });

                $(this).on('click', "#sp-pos-button-back", null, function() {
                    if (_self.onBack) {
                        return _self.onBack();
                    }
                    return true;
                });

            }).on("pageshow", function(event, ui) {
                $("#sp-pos-tab-deposit-button").click();
                _clear();
            });

            return _self;
        };

    }(jQuery, this, this.document, JSON, this.org.savapage));
