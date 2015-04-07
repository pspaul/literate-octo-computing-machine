/*! SavaPage jQuery Mobile Exceeded WebApp | (c) 2011-2015 Datraverse B.V. | GNU Affero General Public License */

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

/*jslint browser: true*/
/*global $, jQuery, alert*/

/*
 * SavaPage jQuery Mobile Web App to handle exceeded WebApp count in browser
 * session.
 */
( function($, window, document, JSON, _ns) {"use strict";

        /**
         * Constructor
         */
        _ns.Controller = function(_i18n, _model, _view, _api) {

            /*
             *
             */
            this.init = function() {
                _model.initAuth();
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

            var
            //
            _LOC_AUTH_NAME = 'sp.auth.admin.name'
            //
            , _LOC_AUTH_ADMIN_TOKEN = 'sp.auth.admin.token'
            //
            , _LOC_AUTH_USER_TOKEN = 'sp.auth.user.token'
            //
            , _LOC_LANG = 'sp.admin.language';

            this.user = new _ns.User();

            this.authToken = {};

            this.initAuth = function() {

                var item;

                this.authToken = {};

                item = _LOC_AUTH_NAME;
                if (window.localStorage[item] !== null) {
                    this.authToken.user = window.localStorage[item];
                }

                item = _LOC_AUTH_ADMIN_TOKEN;
                if (window.localStorage[item] !== null) {
                    this.authToken.tokenAdminApp = window.localStorage[item];
                }

                item = _LOC_AUTH_USER_TOKEN;
                if (window.localStorage[item] !== null) {
                    this.authToken.tokenUserApp = window.localStorage[item];
                }

                item = _LOC_LANG;
                if (window.localStorage[item] !== null) {
                    this.authToken.language = window.localStorage[item];
                }

            };

            this.setLanguage = function(lang) {
                this.authToken.language = lang;
                window.localStorage[_LOC_LANG] = lang;
            };

        };
        /**
         *
         */
        $.SavaPageApp = function(name) {

            var _i18n = null
            //
            , _model = new _ns.Model(_i18n)
            //
            , _api = new _ns.Api(_i18n, _model.user)
            //
            , _view = new _ns.View(_i18n, _api)
            //
            , _ctrl = new _ns.Controller(_i18n, _model, _view, _api)
            //
            ;

            this.init = function() {

                _ctrl.init();

                $('#button-login').click(function() {

                    var res = _api.call({
                        request : 'webapp-close-session',
                        authTokenAdmin : _model.authToken.tokenAdminApp,
                        authTokenUser : _model.authToken.tokenUserApp
                    });

                    if (res.result.code === '0') {
                        window.location.replace(window.location.protocol + "//" + window.location.host + "/user");
                        return false;
                    }
                    alert('error');
                    return true;
                });

                /**
                 * http://www.dotnetvishal.com/2013/01/close-current-browser-tab-using.html
                 */
                $('#button-continue').click(function() {
                    var win = window.open('', '_self');
                    win.close();
                    return false;
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
            $.savapageApp.init();

        });

    }(jQuery, this, this.document, JSON, this.org.savapage));
