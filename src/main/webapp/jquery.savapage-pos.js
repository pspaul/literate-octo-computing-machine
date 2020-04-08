/*! SavaPage jQuery Mobile Admin POS Web App | (c) 2011-2020 Datraverse B.V. |
 * GNU Affero General Public License */

/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
 * SavaPage jQuery Mobile Pos Web App
 */
( function($, window, document, JSON, _ns) {
        "use strict";

        /**
         * Constructor
         */
        _ns.Controller = function(_i18n, _model, _view, _api) {

            var _this = this,
                _util = _ns.Utils,
                i18nRefresh,
                _handleLoginResult;

            /**
             *
             */
            i18nRefresh = function(i18nNew) {
                if (i18nNew && i18nNew.i18n) {
                    _i18n.refresh(i18nNew.i18n);
                    //$.mobile.loadingMessage = _i18n.string('msg-page-loading');
                    $.mobile.pageLoadErrorMessage = _i18n.string('msg-page-load-error');
                } else {
                    _view.message('i18n initialization failed.');
                }
            };

            /**
             *
             */
            _handleLoginResult = function(authMode, data) {

                _model.user.loggedIn = (data.result.code <= '2');

                if (_model.user.loggedIn) {

                    _model.startSession();

                    _model.user.key_id = data.key_id;
                    _model.user.id = data.id;

                    _model.language = data.language;
                    _model.country = data.country;

                    _model.setAuthToken(data.id, data.authtoken, _model.language, _model.country);

                    /*
                     * This is the token used for CometD authentication.
                     * See: Java org.savapage.server.cometd.BayeuxAuthenticator
                     */
                    _model.user.cometdToken = data.cometdToken;

                    _model.user.admin = data.admin;
                    _model.user.mail = data.mail;
                    _model.user.mailDefault = data.mail;

                    /*
                     * Used to display a login info/warning popup message at a
                     * later time.
                     */
                    _model.user.logonApiResult = (data.result.code > '0') ? data.result : null;

                    if (_view.pages.pointOfSale.isLoaded) {
                        _view.pages.pointOfSale.show();
                    } else {
                        _view.pages.pointOfSale.load().show();
                    }

                } else if (data.authTOTPRequired) {
                    _view.pages.login.notifyTOTPRequired();
                } else {
                    _view.pages.login.notifyLogout();

                    if (_view.activePage().attr('id') === 'page-login') {
                        _view.pages.login.notifyLoginFailed(authMode, data.result.txt);
                    } else {
                        _view.pages.login.loadShow(_ns.WEBAPP_TYPE);
                    }
                }
            };

            /*
             *
             */
            this.init = function() {
                var res,
                    language,
                    country,
                    authModeRequest = _util.getUrlParam(_ns.URL_PARM.LOGIN);

                _model.initAuth();

                res = _api.call({
                    request : 'constants',
                    webAppType : _ns.WEBAPP_TYPE,
                    authtoken : _model.authToken.token,
                    authMode : authModeRequest
                });

                /*
                 * FIX for Opera to prevent endless reloads when server is down:
                 * check the return code. Display a basic message (English is all
                 * we have), no fancy stuff (cause jQuery might not be working).
                 */
                if (res.result.code !== '0') {
                    _view.message(res.result.txt || 'connection to server is lost');
                    return;
                }

                _view.userChartColors = [res.colors.printIn, res.colors.printOut, res.colors.pdfOut];

                _model.locale = res.locale;
                _model.maxIdleSeconds = res.maxIdleSeconds;

                // NOTE: authCardSelfAssoc is DISABLED
                _view.pages.login.setAuthMode(res.authName, res.authId, res.authYubiKey, res.authCardLocal, res.authCardIp, res.authModeDefault, res.authCardPinReq, null, res.yubikeyMaxMsecs, res.cardLocalMaxMsecs, res.cardAssocMaxSecs);

                language = _util.getUrlParam(_ns.URL_PARM.LANGUAGE);
                if (!language) {
                    language = _model.authToken.language || '';
                }

                country = _util.getUrlParam(_ns.URL_PARM.COUNTRY);
                if (!country) {
                    country = _model.authToken.country || '';
                }

                res = _api.call({
                    request : 'language',
                    language : language,
                    country : country
                });

                i18nRefresh(res);

                _view.initI18n(res.language);

                //
                // Call-back: api
                //
                _api.onExpired(function() {
                    _view.showExpiredDialog();
                });

                // Call-back: api
                _api.onDisconnected(function() {
                    window.location.reload();
                });

            };

            /**
             *
             */
            this.login = function(authMode, authId, authPw, authToken) {

                _model.user.loggedIn = false;

                $('#overlay-curtain').show();

                _api.callAsync({
                    request : 'login',
                    dto : JSON.stringify({
                        webAppType : _ns.WEBAPP_TYPE,
                        authMode : authMode,
                        authId : authId,
                        authPw : authPw,
                        authToken : authToken
                    })
                }, function(data) {
                    _handleLoginResult(authMode, data);
                }, function() {
                    $('#overlay-curtain').hide();
                }, function(data) {
                    // Do NOT use _view.showApiMsg(data), cause it spoils the
                    // focus() in case of Card Swipe.
                    _view.message(data.result.txt);
                });
            };

            /*
             *
             */
            _view.onDisconnected(function() {
                window.location.reload();
            });

            /**
             * Callbacks: page LANGUAGE
             */
            _view.pages.language.onSelectLocale(function(lang, country) {
                /*
                 * This call sets the locale for the current session and returns
                 * strings needed for off-line mode.
                 */
                var res = _api.call({
                    'request' : 'language',
                    language : lang,
                    country : country
                });

                if (res.result.code === "0") {

                    _model.setLanguage(lang);
                    _model.setCountry(country);

                    i18nRefresh(res);

                    /*
                     * By restarting, the newly localized login page is displayed
                     */
                    _ns.restartWebApp();
                }
            });

            /**
             * Callbacks: page LOGIN
             */
            _view.pages.login.onShow(function() {
                $.noop();
            });

            _view.pages.login.onLanguage(function() {
                _view.pages.language.loadShowAsync();
            });

            _view.pages.login.onLogin(function(mode, id, pw) {
                _this.login(mode, id, pw);
            });

            /**
             * Callbacks: page PointOfSale
             */
            _view.pages.pointOfSale.onBack = function() {
                var res;
                /*
                 * Since this method is also called by the generic onPageHide
                 * call-back,
                 * as set in on('pagecontainershow'), when Back button is
                 * pressed.
                 */
                if (!_model.user.loggedIn) {
                    return true;
                }
                /*
                 * NOTE: This is the same solution as in the User WebApp.
                 * See remarks over there.
                 */

                /*
                 * Prevent that BACK button shows private data when disconnected.
                 * Mantis #108
                 */
                _model.startSession();

                res = _api.call({
                    request : 'logout',
                    dto : JSON.stringify({
                        authToken : _model.authToken.token
                    })
                });

                _model.user.loggedIn = false;

                if (res.result.code !== '0') {
                    _ns.logger.warn(res.result.txt);
                    _model.setAuthToken(null, null);
                }

                _ns.restartWebApp();

                return true;
            };

            _view.pages.pointOfSale.onPageHide = function() {
                _view.pages.pointOfSale.onBack();
            };

        };
        /**
         *
         */
        _ns.Model = function(_i18n) {
            var _LOC_AUTH_NAME = 'sp.auth.pos.name',
                _LOC_AUTH_TOKEN = 'sp.auth.pos.token',
                _LOC_LANG = 'sp.pos.language',
                _LOC_COUNTRY = 'sp.pos.country';

            this.user = new _ns.User();

            this.authToken = {};

            this.gcp = {};

            this.sessionExpired = false;

            this.startSession = function() {
                $.noop();
            };

            this.initAuth = function() {
                var item;

                this.authToken = {};

                item = _LOC_AUTH_NAME;
                if (window.localStorage[item] !== null) {
                    this.authToken.user = window.localStorage[item];
                }

                item = _LOC_AUTH_TOKEN;
                if (window.localStorage[item] !== null) {
                    this.authToken.token = window.localStorage[item];
                }

                item = _LOC_LANG;
                if (window.localStorage[item] !== null) {
                    this.authToken.language = window.localStorage[item];
                }

                item = _LOC_COUNTRY;
                if (window.localStorage[item] !== null) {
                    this.authToken.country = window.localStorage[item];
                }
            };

            this.setLanguage = function(lang) {
                this.authToken.language = lang;
                window.localStorage[_LOC_LANG] = lang;
            };

            this.setCountry = function(country) {
                this.authToken.country = country;
                window.localStorage[_LOC_COUNTRY] = country;
            };

            this.setAuthToken = function(user, token, language, country) {
                var item;

                item = _LOC_AUTH_NAME;
                this.authToken.user = user;
                window.localStorage[item] = user;

                item = _LOC_AUTH_TOKEN;
                this.authToken.token = token;
                window.localStorage[item] = token;

                if (language) {
                    this.setLanguage(language);
                }
                if (country) {
                    this.setCountry(country);
                }
            };
        };

        /**
         *
         */
        $.SavaPageApp = function(name) {
            var _i18n = new _ns.I18n(),
                _model = new _ns.Model(_i18n),
                _api = new _ns.Api(_i18n, _model.user),
                _view = new _ns.View(_i18n, _api),
                _viewById = {},
                _ctrl,
                _nativeLogin;

            _ns.commonWebAppInit();

            _view.pages = {
                language : new _ns.PageLanguage(_i18n, _view, _model),
                login : new _ns.PageLogin(_i18n, _view, _api),
                pointOfSale : new _ns.PagePointOfSale(_i18n, _view, _model, _api)
            };

            $.each(_view.pages, function(key, page) {
                _viewById[page.id().substring(1)] = page;
            });

            $(document).on('pagecontainershow', function(event, ui) {
                var prevPage = ui.prevPage[0] ? _viewById[ui.prevPage[0].id] : undefined;
                if (prevPage && prevPage.onPageHide) {
                    prevPage.onPageHide();
                }
            }).on('pagecontainerhide', function(event, ui) {
                var nextPage = _viewById[ui.nextPage[0].id];
                if (nextPage && nextPage.onPageShow) {
                    nextPage.onPageShow();
                }
            });

            _ctrl = new _ns.Controller(_i18n, _model, _view, _api);

            _nativeLogin = function(user, authMode) {
                if (_model.authToken.user && _model.authToken.token) {
                    _ctrl.login(_view.AUTH_MODE_NAME, _model.authToken.user, null, _model.authToken.token);
                } else {
                    // Initial load/show of Login dialog
                    _view.pages.login.loadShow(_ns.WEBAPP_TYPE);
                }
            };

            /**
             *
             */
            this.init = function() {
                var user = _ns.Utils.getUrlParam(_ns.URL_PARM.USER),
                    authMode = _ns.Utils.getUrlParam(_ns.URL_PARM.LOGIN);

                _ns.initWebApp('POS');

                _ctrl.init();

                _nativeLogin(user, authMode);

                $(window).on('beforeunload', function() {
                    // By NOT returning anything the unload dialog will not show.
                    $.noop();
                }).on('unload', function() {
                    _api.unloadWebApp();
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
