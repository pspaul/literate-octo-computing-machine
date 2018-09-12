/*! SavaPage jQuery Mobile Print Site Web App | (c) 2011-2018 Datraverse B.V. 
 * | GNU Affero General Public License */

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
 * SavaPage jQuery Mobile Admin Pages
 */
( function($, window, document, JSON, _ns) {
        "use strict";

        /**
         * Constructor
         */
        _ns.PageMain = function(_i18n, _view, _model, _api) {

            var _page = new _ns.Page(_i18n, _view, '#page-main', 'printsite/Main'),
                _self = _ns.derive(_page),
                _panel,
                _quickUserSearch = new _ns.QuickUserSearch(_view, _api),
                _authKeyLoggerStartTime = null,
                _userKey,
                _userText,
                _refresh,
                _refreshPanel,
                _refreshPanelCommon,
                _refreshPanelByUrl,
                _panelCur,
                _panelCurClass,
                _onSelectUser,
                _onClearUser;

            /*
             * The map of panels. The key is the Java Wicket class. Each key has
             * the following attributes.
             *
             * getInput : function(my) {}
             *
             * onOutput : function(my, output) {}
             */
            _panel = {
                DocLogBase : _ns.PanelDocLogBase,
                UserOutbox : _ns.PanelUserOutbox
            };

            _refreshPanel = function(wClass, skipBeforeLoad) {
                _refreshPanelByUrl('admin/', wClass, skipBeforeLoad);
            };

            _refreshPanelCommon = function(wClass, skipBeforeLoad) {
                _refreshPanelByUrl('', wClass, skipBeforeLoad);
            };

            /**
             * Refreshes an admin panel: using the current state (no defaults
             * applied). The content of the panel is determined by the data
             * parameter object. This object is converted to JSON in the ajax
             * call. Remember: we always get HTML back to display the panel.
             */
            _refreshPanelByUrl = function(url, wClass, skipBeforeLoad) {

                var panel = _panel[wClass],
                    data = null,
                    jsonData = null;

                if (!skipBeforeLoad && panel.beforeload) {
                    panel.beforeload(panel);
                }

                if (panel.getInput) {
                    data = panel.getInput(panel);
                }

                if (data) {
                    jsonData = JSON.stringify(data);
                }

                /*
                 * NOTE: '/pages' is a Wicket mountPackage() construct. This
                 * is the full url:
                 *
                 * '/wicket/bookmarkable/org.savapage.server.pages.'
                 * +  wClass
                 */
                $.mobile.loading("show");
                $.ajax({
                    type : "POST",
                    async : true,
                    url : '/pages/' + url + wClass + _ns.WebAppTypeUrlParm(),
                    data : {
                        user : _model.user.id,
                        data : jsonData
                    }
                }).done(function(html) {

                    $('.content-primary').html(html).enhanceWithin();

                    if (panel.onOutput) {
                        /*
                         * We can't retrieve the json-rsp here, why?
                         */
                        panel.onOutput(panel, undefined);
                    }

                    /*
                     * IMPORTANT: afterload() SHOULD take care of hiding the
                     * spinner. This is (usually) achieved by retrieving the
                     * first page of the list.
                     */
                    if (panel.afterload) {
                        panel.afterload(panel);
                    } else {
                        $.mobile.loading("hide");
                    }

                    _panelCurClass = wClass;
                    _panelCur = _panel[wClass];

                    $("#sp-a-content-button").click();

                }).fail(function() {
                    $.mobile.loading("hide");
                    _self.onDisconnected();
                });

            };

            /*
             * Common Panel parameters to be set be the client.
             */
            _ns.PanelCommon.view = _view;
            _ns.PanelCommon.userId = _model.user.id;
            _ns.PanelCommon.refreshPanelCommon = _refreshPanelCommon;
            _ns.PanelCommon.refreshPanelAdmin = _refreshPanel;

            /** */
            _refresh = function() {
                $.noop();
            };

            /** */
            _onSelectUser = function(quickUserSelected) {
                $("#sp-main-userid").val(quickUserSelected.text);
                _userKey = quickUserSelected.key;
                _userText = quickUserSelected.text;
                _refresh();
            };

            /** */
            _onClearUser = function() {
                $("#sp-main-userid").val('');
                _userKey = null;
                _refresh();
            };

            /** */
            _self.initView = function() {
                $.noop();
            };

            /** */
            $(_self.id()).on('pagebeforeshow', function(event, ui) {
                /*
                 * Wow, this took some time to find out. Since the 'popup' is
                 * created ad-hoc to display the message, timing is important
                 * here... We cannot expect the popup is created all right at
                 * this stage.
                 *
                 * (1) 'pagebeforeshow' is the place to be ('pagecreate' is too
                 * early) even for the setTimeout construct...
                 * (2) setTimeout take the processing at the back of event queue?
                 */
                if (_model.user.logonApiResult) {
                    window.setTimeout(function() {
                        if (_model.user.logonApiResult) {
                            _view.apiResMsg(_model.user.logonApiResult);
                            _model.user.logonApiResult = null;
                        }
                    }, 100);
                }

                /*
                 * When session expired in a dialog, this is the place to go to
                 * page-zero (since, when the dialog closes, we get a
                 * 'pagebeforeshow' event here).
                 */
                if (_model.sessionExpired) {
                    _view.showExpiredDialog();
                }

            }).on('pagecreate', function(event) {

                _self.initView();

                _quickUserSearch.onCreate($(this), 'sp-main-userid-filter', _onSelectUser, _onClearUser);

                /*
                 * Smooth scrolling to primary content
                 *
                 * Every <a href="#id" class="sp-anchor"> must smoothly scroll
                 * to the href selector.
                 */
                $(this).on('click', 'a.sp-anchor', null, function(ev) {
                    $('html, body').animate({
                        scrollTop : $($.attr(this, 'href')).offset().top
                    }, 1000);
                    return false;
                });

                $('#button-logout').click(function() {
                    $('.content-primary').empty();
                    _self.onLogout();
                    return false;
                });

                $('.sp-btn-about-org').click(function() {
                    _view.showPageAsync('#page-info', 'AppAbout');
                    return false;
                });

                $('#sp-user-card-local-group').focusin(function() {
                    _authKeyLoggerStartTime = null;
                    $('#sp-user-card-local-focusin').show();
                    $('#sp-user-card-local-focusout').hide();
                });

                $('#sp-user-card-local-group').focusout(function() {
                    _authKeyLoggerStartTime = null;
                    $('#sp-user-card-local-focusin').hide();
                    // Use the fadeIn to prevent a 'flash' effect when just
                    // anotherfocus is lost because another auth method is
                    // selected.
                    $('#sp-user-card-local-focusout').fadeIn(700);
                });

                /*
                 * DocLog Panel
                 */
                $(this).on('click', '#sp-btn-user-doc-log', null, function() {
                    var pnl = _panel.DocLogBase;
                    pnl.applyDefaultForPrintSite(pnl);
                    pnl.input.select.user_id = _userKey;

                    // HACK: hidden field is present/set in THIS container.
                    $('#sp-doclog-hidden-user-id').val(_userKey);
                    pnl.refresh(pnl, true);
                    return false;
                });

                $(this).on('click', '#button-doclog-apply', null, function() {
                    var pnl = _panel.DocLogBase;
                    pnl.page(pnl, 1);
                    return false;
                });

                $(this).on('click', '#button-doclog-default', null, function() {
                    var pnl = _panel.DocLogBase;
                    pnl.applyDefaultForPrintSite(pnl);
                    pnl.m2v(pnl);
                    return false;
                });

                $(this).on('change', "input[name='sp-doclog-select-type']", null, function() {
                    var pnl = _panel.DocLogBase;
                    pnl.setVisibility(pnl);
                    return false;
                });

                /*
                 * UserOutbox Panel
                 */
                $(this).on('click', '#sp-btn-user-pending-jobs', null, function() {
                    var pnl = _panel.UserOutbox;
                    pnl.userKey = _userKey;
                    pnl.refresh(pnl, true);
                    return false;
                });

            });
            return _self;
        };

    }(jQuery, this, this.document, JSON, this.org.savapage));
