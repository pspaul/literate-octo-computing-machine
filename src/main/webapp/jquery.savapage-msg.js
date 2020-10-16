// @license http://www.gnu.org/licenses/agpl-3.0.html AGPL-3.0

/*! SavaPage jQuery Mobile Exceeded WebApp | (c) 2020 Datraverse B.V. | GNU
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
 * SavaPage jQuery Mobile Web App to handle exceeded WebApp count in browser
 * session.
 */
( function($, window, document, JSON, _ns) {
        "use strict";

        /**
         * Constructor
         */
        _ns.Controller = function(_i18n, _model, _view, _api) {

            /*
             *
             */
            this.init = function() {
                $.noop();
            };

            /*
             *
             */
            _view.onDisconnected = function() {
                alert('Disconnected');
            };

        };
        /**
         *
         */
        _ns.Model = function(_i18n) {
            this.user = new _ns.User();
        };
        /**
         *
         */
        $.SavaPageApp = function(name) {

            var _i18n = null
            //
            ,
                _model = new _ns.Model(_i18n)
            //
            ,
                _api = new _ns.Api(_i18n, _model.user)
            //
            ,
                _view = new _ns.View(_i18n, _api)
            //
            ,
                _ctrl = new _ns.Controller(_i18n, _model, _view, _api)
            //
            ;

            this.init = function() {

                _ctrl.init();

                $('#button-login').click(function() {

                    var res = _api.call({
                        request : 'webapp-close-session'
                    });

                    if (res.result.code === '0') {
                        window.location.replace(window.location.protocol + "//" + window.location.host + $('#sp-webapp-mountpath').attr('value'));
                        return false;
                    }
                    alert('error');
                    return true;
                });

            };
        };

        $(function() {
            $.savapageApp = new $.SavaPageApp();
            // do NOT initialize here (to early for some browsers, like Opera)
        });

        $(document).on("mobileinit", null, null, function() {
            $.mobile.defaultPageTransition = "none";
            $.mobile.defaultDialogTransition = "none";
        }).on("ready", null, null, function() {
            // Initialize AFTER document is read
            try {
                $.savapageApp.init();
            } catch (e) {
                _ns.onLoadException();
            }

        });

    }(jQuery, this, this.document, JSON, this.org.savapage));

// @license-end
