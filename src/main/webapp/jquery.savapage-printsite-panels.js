// @license http://www.gnu.org/licenses/agpl-3.0.html AGPL-3.0

/*! SavaPage jQuery Mobile Print Site Web App | (c) 2020 Datraverse B.V.
 * | GNU Affero General Public License */

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
 * NOTE: the *! comment blocks are part of compressed version.
 */

/*
 * SavaPage jQuery Mobile Print Site Panels
 */
( function($, window, document, JSON, _ns) {
        "use strict";

        /*jslint browser: true*/
        /*global $, jQuery, alert*/

        /** */
        _ns.PanelUserEdit = {
            // Parameter to be filled
            userKey : null,

            /** */
            refresh : function(skipBeforeLoad) {
                var _this = this,
                    html = _ns.PanelCommon.view.getPageHtml('printsite/PrintSiteUserEdit', {
                    userKey : _this.userKey
                });
                $('#sp-a-content').html(html).enhanceWithin();
            },

            /** */
            beforeload : function() {
                $.noop();
            },

            /** */
            afterload : function() {
                $.noop();
            },

            /** */
            v2m : function(_view) {
                var dto = {};

                dto.dbId = this.userKey;
                dto.userName = $('#user-name').text();
                dto.fullName = $('#user-fullname').val();
                dto.disabled = $('#user-disabled').is(':checked');
                dto.email = $('#user-email').val();

                dto.emailOther = [];
                $.each($('#user-email-other').val().split("\n"), function(key, val) {
                    var address = val.trim();
                    if (address.length > 0) {
                        dto.emailOther.push({
                            address : address
                        });
                    }
                });

                dto.card = $('#user-card-number').val();

                dto.accounting = {};
                dto.accounting.balance = $('#user-account-balance').val();
                dto.accounting.creditLimit = _view.getRadioValue("user-account-credit-limit-type");
                dto.accounting.creditLimitAmount = $('#user-account-credit-limit-amount').val();

                return dto;
            }
        };

        /** */
        _ns.PanelUserOutbox = {

            // Parameter to be filled
            userKey : null,

            /** */
            refresh : function(skipBeforeLoad) {
                var _this = this,
                    html = _ns.PanelCommon.view.getUserPageHtml('OutboxAddin', {
                    jobTickets : false,
                    expiryAsc : false,
                    userKey : _this.userKey
                });

                if (html) {
                    html = '<ul data-role="listview" data-inset="true"><li data-role="list-divider">&nbsp;</li><li>' + html;
                    html += '</li></ul>';
                    $('#sp-a-content').html(html).enhanceWithin();
                    $('.sp-sparkline-printout').sparkline('html', {
                        enableTagOptions : true
                    });

                }
            },

            /** */
            beforeload : function() {
                $.noop();
            },

            /** */
            afterload : function() {
                $.noop();
            }
        };

        /** */
        _ns.PanelDashboard = {

            // Parameter to be filled
            model : null,

            // Refresh every minute.
            REFRESH_MSEC : 60000,

            // The saved scroll position.
            scrollTop : null,

            timeout : null,

            /*
             * A refresh of the WHOLE panel.
             */
            refresh : function(skipBeforeLoad) {
                this.scrollTop = $(window).scrollTop();
                _ns.PanelCommon.refreshPanelPrintSite('Dashboard', skipBeforeLoad);
            },

            beforeload : function() {
                $.noop();
            },

            afterload : function() {
                var i,
                    j,
                    _model = this.model;

                $.mobile.loading("hide");

                for ( i = 0; i < _model.pubMsgStack.length; i++) {
                    $('#live-messages').append(_model.pubMsgAsHtml(_model.pubMsgStack[i]));
                }
                // Fill it up...
                for ( j = i; j < _model.MAX_PUB_MSG; j++) {
                    $('#live-messages').append('<div>&nbsp;</div>');
                }

            },

            onUnload : function() {
                $.noop();
            }
        };

    }(jQuery, this, this.document, JSON, this.org.savapage));

// @license-end
