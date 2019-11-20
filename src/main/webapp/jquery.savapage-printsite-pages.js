/*! SavaPage jQuery Mobile Print Site Web App | (c) 2011-2019 Datraverse B.V.
 * | GNU Affero General Public License */

/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
                _quickUserSearch = new _ns.QuickObjectSearch(_view, _api),
                _userKey,
                _userText,
                _refresh,
                _refreshPanel,
                _refreshPanelAdmin,
                _refreshPanelCommon,
                _refreshPanelByUrl,
                _panelCur,
                _panelCurClass,
                _onQuickSearchUserItemDisplay,
                _onSelectUser,
                _onClearUser,
                _onOutboxDeleteJob,
                _onOutboxReleaseJob,
                _onAccountTrxInfo;

            /*
             * The map of panels. The key is the Java Wicket class. Each key has
             * the following attributes.
             *
             * getInput : function(my) {}
             *
             * onOutput : function(my, output) {}
             */
            _panel = {
                UserOutbox : _ns.PanelUserOutbox,
                DocLogBase : _ns.PanelDocLogBase,
                AccountTrxBase : _ns.PanelAccountTrxBase,
                PaperCutAccountTrxBase : _ns.PanelPaperCutAccountTrxBase,
                Dashboard : _ns.PanelDashboard,
                UserEdit : _ns.PanelUserEdit
            };

            _self.onDisconnected = function() {
                window.location.reload();
            };

            _refreshPanel = function(wClass, skipBeforeLoad) {
                _refreshPanelByUrl('printsite/', wClass, skipBeforeLoad);
            };

            _refreshPanelAdmin = function(wClass, skipBeforeLoad) {
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
            _ns.PanelCommon.refreshPanelAdmin = _refreshPanelAdmin;
            _ns.PanelCommon.refreshPanelPrintSite = _refreshPanel;

            /** */
            _refresh = function() {
                $.noop();
            };

            /** */
            _onQuickSearchUserItemDisplay = function(item) {
                return item.text + " &bull; " + (item.email || "&nbsp;");
            };

            /** */
            _onSelectUser = function(quickUserSelected) {
                $("#sp-main-userid").val(quickUserSelected.text);
                _userKey = quickUserSelected.key;
                _userText = quickUserSelected.text;
                _refresh();
                _view.enableUI($('.sp-printsite-main-li-user'), true);
                _panel.UserEdit.userKey = _userKey;
                _ns.Utils.asyncFoo(function() {
                    $('#sp-btn-user-details').click();
                });
            };

            /** */
            _onClearUser = function() {
                $("#sp-main-userid").val('');
                _userKey = null;
                _refresh();
                _view.enableUI($('.sp-printsite-main-li-user'), false);
                if ($('#live-messages').length === 0) {
                    _ns.Utils.asyncFoo(function() {
                        $('#sp-btn-dashboard').click();
                    });
                }
            };

            _onOutboxDeleteJob = function(jobFileName, isJobTicket) {
                var res = _api.call({
                    request : 'outbox-delete-job',
                    dto : JSON.stringify({
                        jobFileName : jobFileName,
                        jobTicket : isJobTicket,
                        userDbId : _userKey
                    })
                });
                if (res.result.code === '0') {
                    $('#sp-btn-user-pending-jobs').click();
                }
                _view.showApiMsg(res);
            };

            _onAccountTrxInfo = function(src, jobticket) {
                var html = _view.getPageHtml('OutboxAccountTrxAddin', {
                    jobFileName : src.attr('data-savapage'),
                    jobticket : jobticket,
                    userDbId : _userKey
                }) || 'error';
                $('#sp-outbox-popup-addin').html(html);
                // remove trailing poit suffix
                $('#sp-outbox-popup-title').text(src.attr('title').replace('. . .', ''));
                $('#sp-outbox-popup').enhanceWithin().popup('open', {
                    positionTo : src,
                    arrow : 't'
                });
            };

            _onOutboxReleaseJob = function(src) {

                $.mobile.loading("show");

                _ns.Utils.asyncFoo(function() {
                    var res = _api.call({
                        request : 'outbox-release-job',
                        dto : JSON.stringify({
                            jobFileName : src.attr('data-savapage'),
                            userDbId : _userKey
                        })
                    });
                    if (res.result.code === '0') {
                        $('#sp-btn-user-pending-jobs').click();
                    }
                    _view.showApiMsg(res);
                    $.mobile.loading("hide");
                });
            };

            /** */
            _self.initView = function() {
                var name = 'Dashboard';
                if ($('.content-secondary').find('[name=' + name + ']').length > 0) {
                    _loadPanel(name);
                }
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

                _quickUserSearch.onCreate($(this), 'sp-main-userid-filter', 'user-quick-search', null, _onQuickSearchUserItemDisplay, _onSelectUser, _onClearUser);

                _onClearUser();

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

                _ns.KeyboardLogger.setCallback($('#sp-user-card-local-group'), _model.cardLocalMaxMsecs,
                //
                function() {// focusIn
                    $('#sp-user-card-local-focusin').show();
                    $('#sp-user-card-local-focusout').hide();
                }, function() {// focusOut
                    $('#sp-user-card-local-focusin').hide();
                    $('#sp-user-card-local-focusout').fadeIn(700);
                }, function(id) {
                    var res = _api.call({
                        request : 'usercard-quick-search',
                        dto : JSON.stringify({
                            card : id
                        })
                    });
                    if (res.result.code !== '0') {
                        _view.message(res.result.txt);
                    } else {
                        _onSelectUser(res.dto);
                        $('#sp-btn-user-details').focus();
                    }
                });

                /*
                 * UserOutbox Panel
                 */
                $(this).on('click', '#sp-btn-user-pending-jobs', null, function() {
                    var pnl = _panel.UserOutbox;
                    pnl.userKey = _userKey;
                    pnl.refresh(pnl, true);
                }).on('click', '.sp-outbox-account-trx-info-job', null, function() {
                    _onAccountTrxInfo($(this), false);
                }).on('click', '.sp-outbox-account-trx-info-jobticket', null, function() {
                    _onAccountTrxInfo($(this), true);
                }).on('click', '.sp-outbox-preview-job', null, function() {
                    _api.download("pdf-outbox", null, $(this).attr('data-savapage'), _userKey);
                }).on('click', '.sp-outbox-preview-jobticket', null, function() {
                    _api.download("pdf-jobticket", null, $(this).attr('data-savapage'));
                }).on('click', '.sp-outbox-cancel-job', null, function() {
                    _onOutboxDeleteJob($(this).attr('data-savapage'), false);
                }).on('click', '.sp-outbox-cancel-jobticket', null, function() {
                    _onOutboxDeleteJob($(this).attr('data-savapage'), true);
                }).on('click', '.sp-outbox-print-release', null, function() {
                    _onOutboxReleaseJob($(this));
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
                 * AccountTrx Panel
                 */
                $(this).on('click', '#sp-btn-user-trx', null, function() {
                    var pnl = _panel.AccountTrxBase;
                    //pnl.applyDefaultForPrintSite(pnl);
                    pnl.input.select.user_id = _userKey;

                    // HACK: hidden field is present/set in THIS container.
                    $('#sp-accounttrx-hidden-user-id').val(_userKey);
                    pnl.refresh(pnl, true);
                    return false;
                });

                $(this).on('click', '#button-accounttrx-apply', null, function() {
                    var pnl = _panel.AccountTrxBase;
                    pnl.page(pnl, 1);
                    return false;
                });

                $(this).on('click', '#button-accounttrx-default', null, function() {
                    var pnl = _panel.AccountTrxBase;
                    pnl.applyDefaults(pnl);
                    pnl.m2v(pnl);
                    return false;
                });

                $(this).on('click', '.sp-btn-accounttrx-report', null, function() {
                    var pnl = _panel.AccountTrxBase;
                    pnl.v2m(pnl);
                    _self.onDownload("report", pnl.input, "AccountTrxList", $(this).attr('data-savapage'));
                    return true;
                });

                $(this).on('click', ".sp-download-receipt", null, function() {
                    _self.onDownload("pos-receipt-download", null, $(this).attr('data-savapage'));
                    return false;
                });

                /*
                 * PaperCutAccountTrx Panel
                 */
                $(this).on('click', '#sp-btn-user-trx-pc', null, function() {
                    var pnl = _panel.PaperCutAccountTrxBase;
                    //pnl.applyDefaultForPrintSite(pnl);
                    pnl.input.select.user_id = _userKey;

                    // HACK: hidden field is present/set in THIS container.
                    $('#sp-accounttrx-hidden-user-id-pc').val(_userKey);
                    pnl.refresh(pnl, true);
                    return false;
                });

                $(this).on('click', '#sp-btn-accounttrx-apply-pc', null, function() {
                    var pnl = _panel.PaperCutAccountTrxBase;
                    pnl.page(pnl, 1);
                    return false;
                });

                $(this).on('click', '#sp-btn-accounttrx-default-pc', null, function() {
                    var pnl = _panel.PaperCutAccountTrxBase;
                    pnl.applyDefaults(pnl);
                    pnl.m2v(pnl);
                    return false;
                });

                /*
                 * Dashboard Panel
                 */
                $(this).on('click', '#sp-btn-dashboard', null, function() {
                    var pnl = _panel.Dashboard;
                    pnl.refresh(pnl, true);
                    return false;
                });

                $(this).on('click', "#live-messages-clear", null, function() {
                    _model.pubMsgStack = [];
                    $('#live-messages').html('');
                    return false;
                });

                /*
                 * UserEdit Panel
                 */
                $(this).on('click', '#sp-btn-user-details', null, function() {
                    var pnl = _panel.UserEdit;
                    pnl.refresh(pnl, true);
                    return false;
                });

                $(this).on('click', '#sp-btn-user-save', null, function() {
                    var pnl = _panel.UserEdit,
                        res = _api.call({
                        request : 'printsite-user-set',
                        dto : JSON.stringify(pnl.v2m(pnl, _view))
                    });
                    _view.showApiMsg(res);
                    return false;

                }).on('click', '#sp-btn-user-delete', null, function() {
                    var dlg = $('#sp-printsite-popup-delete-user');
                    dlg.popup('open', {
                        positionTo : $(this)
                    });
                    $("#sp-printsite-popup-delete-user-btn-no").focus();

                }).on('click', '#sp-printsite-popup-delete-user-btn-yes', null, function() {
                    alert('not implemented yet');
                    return false;

                }).on('click', '#sp-btn-user-pw-reset', null, function() {
                    alert('not implemented yet');
                    return false;

                }).on('click', '#sp-btn-user-pw-erase', null, function() {
                    var pnl = _panel.UserEdit,
                        res = _api.call({
                        request : 'erase-user-pw',
                        dto : JSON.stringify({
                            userDbId : pnl.userKey
                        })
                    });
                    _view.showApiMsg(res);
                });

            });
            return _self;
        };

    }(jQuery, this, this.document, JSON, this.org.savapage));
