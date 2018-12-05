/*! SavaPage jQuery Mobile PDF Verification Web App | (c) 2011-2018 Datraverse
 * B.V. | GNU Affero General Public License */

/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * SavaPage jQuery Mobile PDF Verification Web App
 */
( function($, window, document, JSON, _ns) {
        "use strict";

        /**
         * Constructor
         */
        _ns.Controller = function(_i18n, _model, _view, _api) {

            var _util = _ns.Utils,
                i18nRefresh;

            i18nRefresh = function(i18nNew) {
                if (i18nNew && i18nNew.i18n) {
                    _i18n.refresh(i18nNew.i18n);
                    //$.mobile.loadingMessage = _i18n.string('msg-page-loading');
                    $.mobile.pageLoadErrorMessage = _i18n.string('msg-page-load-error');
                } else {
                    _view.message('i18n initialization failed.');
                }
            };

            /*
             *
             */
            this.init = function() {

                var res,
                    language,
                    country;

                res = _api.call({
                    request : 'constants',
                    webAppType : _ns.WEBAPP_TYPE
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

                _model.locale = res.locale;

                // WebPrint
                _model.pdfpgpUploadUrl = res.pdfpgpUploadUrl;
                _model.pdfpgpUploadFileParm = res.pdfpgpUploadFileParm;
                _model.pdfpgpMaxBytes = res.pdfpgpMaxBytes;
                _model.pdfpgpFileExt = res.pdfpgpFileExt;

                //
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

        };
        /**
         *
         */
        _ns.Model = function(_i18n) {

            var _LOC_LANG = 'sp.pdfpgp.language',
                _LOC_COUNTRY = 'sp.pdfpgp.country';

            this.authToken = {};

            this.sessionExpired = false;

            this.startSession = function() {
                $.noop();
            };

            this.setLanguage = function(lang) {
                this.authToken.language = lang;
                window.localStorage[_LOC_LANG] = lang;
            };

            this.setCountry = function(country) {
                this.authToken.country = country;
                window.localStorage[_LOC_COUNTRY] = country;
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
                _DROPZONE,
                _DROPZONE_HTML
            //
            ,
                _showUploadButton = function(enable) {
                _view.visible($('#sp-btn-pdfpgp-upload-submit').parent(), enable);
            }
            //
            ,
                _showResetButton = function(enable) {
                _view.visible($('#sp-btn-pdfpgp-upload-reset').parent(), enable);
            }
            //
            ,
                _onUploadStart = function() {
                $('#sp-btn-pdfpgp-upload-reset').click();
                $('#sp-pdfpgp-upload-feedback').html('&nbsp;').show();
            }
            //
            ,
                _onUploadDone = function() {
                _showUploadButton(false);
                _showResetButton(true);
            }
            //
            ,
                _onUploadMsgWarn = function(warn, files, filesStatus) {
                $('#sp-pdfpgp-upload-feedback').html(_ns.DropZone.getHtmlWarning(_i18n, warn, files, filesStatus)).show();
                _showUploadButton(false);
                _showResetButton(true);
                $('#sp-pdfpgp-upload-file').val('');
            }
            //
            ,
                _onUploadMsgInfo = function(files, info) {
                $('#sp-pdfpgp-upload-feedback').html(info);
                _showResetButton(true);
            }
            //
            ,
                _setUploadFeedbackDefault = function(dropzone) {
                var feedback = $('#sp-pdfpgp-upload-feedback');
                feedback.removeClass('sp-txt-warn').removeClass('sp-txt-valid').addClass('sp-txt-info');
                if (dropzone) {
                    feedback.html(_DROPZONE_HTML);
                } else {
                    feedback.html('').hide();
                }
            };

            _ns.commonWebAppInit();

            _view.pages = {
                language : new _ns.PageLanguage(_i18n, _view, _model)
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

            /**
             *
             */
            this.init = function() {
                var zone;

                _ctrl.init();

                $(window).on('beforeunload', function() {
                    // By NOT returning anything the unload dialog will not show.
                    $.noop();
                }).on('unload', function() {
                    _api.removeCallbacks();
                });

                _DROPZONE = _ns.DropZone.isSupported();
                _DROPZONE_HTML = _i18n.format('pdfpgp-upload-dropzone-prompt-dialog');

                _showUploadButton(false);
                _showResetButton(false);

                _setUploadFeedbackDefault(_DROPZONE);

                if (_DROPZONE) {

                    zone = $('#sp-pdfpgp-upload-feedback');
                    zone.addClass('sp-dropzone-small');

                    _ns.DropZone.setCallbacks(zone, 'sp-dropzone-hover-small', _model.pdfpgpUploadUrl, _model.pdfpgpUploadFileParm, null
                    //
                    , function() {
                        return null;
                    }
                    //
                    , _model.pdfpgpMaxBytes, _model.pdfpgpFileExt, _i18n
                    //
                    , function() {// before send
                        _onUploadStart();
                    }, function() {// after send
                        _onUploadDone();
                    }, function(warn, infoArray, filesStatus) {
                        _onUploadMsgWarn(warn, infoArray, filesStatus);
                    }, function(files, info) {
                        _onUploadMsgInfo(files, info);
                    });
                }

                $('#sp-pdfpgp-upload-form').submit(function(e) {
                    var files = document.getElementById('sp-pdfpgp-upload-file').files;

                    _ns.logger.warn(files);

                    e.preventDefault();

                    _ns.DropZone.sendFiles(files, _model.pdfpgpUploadUrl, _model.pdfpgpUploadFileParm, null
                    //
                    , function() {
                        return null;
                    }
                    //
                    , _model.pdfpgpMaxBytes, _model.pdfpgpFileExt, _i18n
                    //
                    , function() {// before send
                        _onUploadStart();
                    }, function() {// after send
                        _onUploadDone();
                    }, function(warn, infoArray, filesStatus) {
                        _onUploadMsgWarn(warn, infoArray, filesStatus);
                    }, function(files, info) {
                        _onUploadMsgInfo(files, info);
                    });

                    return false;
                });

                $('#sp-pdfpgp-upload-form').on('change', '#sp-pdfpgp-upload-file', function() {
                    var show = $(this).val();
                    _showUploadButton(show);
                    _showResetButton(show);
                    _setUploadFeedbackDefault(_DROPZONE);
                });

                $('#sp-btn-pdfpgp-upload-reset').click(function() {
                    _setUploadFeedbackDefault(_DROPZONE);
                    _showUploadButton(false);
                    _showResetButton(false);
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
            $.savapageApp.init();
        });

    }(jQuery, this, this.document, JSON, this.org.savapage));
