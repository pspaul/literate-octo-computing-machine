// @license http://www.gnu.org/licenses/agpl-3.0.html AGPL-3.0

/*! SavaPage jQuery Mobile Common | © 2020 Datraverse B.V. | GNU Affero
 * General Public License */

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

(function($, window, document, _ns) {
    "use strict";

    /*jslint browser: true*/
    /*global $, jQuery, alert*/

    var _watchdogHeartbeatSecs,
        _watchdogTimeoutSecs,
        _watchdogTimer,
        _onWatchdogHeartbeat,
        _lastAppHeartbeat,
        _deferAppWakeUp,
        _onAppWakeUp,
        _onAppWakeUpAutoRestore,
        //
        _doAppHeartbeat = function() {
            _lastAppHeartbeat = new Date().getTime();
        };

    _ns.ApiResultCodeEnum = {
        OK: '0',
        INFO: "1",
        WARN: "2",
        ERROR: "3",
        UNAVAILABLE: "5",
        UNAUTH: "9"
    };

    /**
     * Common initializing actions for all Web App types.
     */
    _ns.commonWebAppInit = function() {
        /*
         * JQM dialogs that are pre-loaded are initially made invisible with
         * attribute style="display: none;". This is done inhibit display
         * when JavaScript is not supported or disabled.
         * We remove the attribute here. Mantis #701.
         */
        $('.sp-initial-hidden').attr('style', '');

        /*
         * Disable the default browser action for file drops on the document.
         * Specific drop zones must be explicitly activated.
         */
        $(document).bind('drop dragover', function(e) {
            e.preventDefault();
        });

        /*
         * Listener for .ajax HTML status codes. All codes are reported
         * (including informational and succes).
         */
        $(document).ajaxError(function(event, request, settings) {
            /*
             * Do not alert 1xx Informational response and 2xx Success. Also,
             * do not alert on 401 (Unauthorized), since this code is
             * returned with a valid JSON result.code === '9', and handled
             * accordingly.
             * https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
             */
            if (request.status !== 401 && request.status >= 300) {
                _ns.logger.warn('[' + settings.url + '] status ' + request.status + ' (' + request.statusText + ')');
                alert('Request: ' + settings.url + '\nStatus: ' + request.status + ' (' + request.statusText + ')');
            }
        });
    };

    /*
     * Indicates if the WebApp was restarted by a WebApp action
     * (an not by a browser action like F5, close browser tab,
     * close browser window).
     */
    _ns.isRestartWebApp = false;

    /**
     *
     */
    _ns.restartWebApp = function() {
        _ns.isRestartWebApp = true;
        $('#button-refresh-webapp').click();
    };

    /**
     *
     */
    _onWatchdogHeartbeat = function() {
        var now,
            delta,
            wokeUp;

        now = new Date().getTime();
        delta = (now - _lastAppHeartbeat);
        wokeUp = (delta > (_watchdogTimeoutSecs * 1000));

        if (wokeUp && _deferAppWakeUp) {
            return;
        }

        _lastAppHeartbeat = now;

        if (wokeUp && _onAppWakeUp) {
            // Do NOT stop the timer here!!
            _onAppWakeUp(delta);
        }
    };

    /**
     * Mantis #717
     */
    _ns.checkAppWakeUpAutoRestore = function() {
        if (_onAppWakeUpAutoRestore) {
            var now = new Date().getTime(),
                delta = (now - _lastAppHeartbeat);
            _lastAppHeartbeat = now;
            if (delta > (_watchdogTimeoutSecs * 1000)) {
                _onAppWakeUpAutoRestore(_deferAppWakeUp);
            }
        }
    };

    _ns.configAppWatchdog = function(onAppWakeUp, watchdogHeartbeatSecs, watchdogTimeoutSecs, onAppWakeUpAutoRestore) {
        _onAppWakeUp = onAppWakeUp;
        _watchdogHeartbeatSecs = watchdogHeartbeatSecs;
        _watchdogTimeoutSecs = watchdogTimeoutSecs;
        _onAppWakeUpAutoRestore = onAppWakeUpAutoRestore;
    };

    _ns.isAppWakeUpDeferred = function() {
        return _deferAppWakeUp;
    };

    _ns.startAppWatchdog = function(defer) {

        _ns.deferAppWakeUp(defer);

        _doAppHeartbeat();

        if (!_watchdogTimer) {
            _watchdogTimer = window.setInterval(_onWatchdogHeartbeat, _watchdogHeartbeatSecs * 1000);
        }
    };

    _ns.stopAppWatchdog = function() {
        if (_watchdogTimer) {
            window.clearInterval(_watchdogTimer);
            _watchdogTimer = null;
        }
    };

    /**
     * See #397: IE 10 and 11: Welcome back dialog after Web Print file
     * selection.
     *
     * @since 0.9.6
     * @param defer If true, the AppWatchdog will defer the wakeup
     * callback.
     */
    _ns.deferAppWakeUp = function(defer) {
        _deferAppWakeUp = defer;
    };

    /**
     *
     */
    _ns.isUserIdle = true;

    // call-back function: to be set by client.
    _ns.onUserIdle = null;

    _ns.userIdleMonitor = function() {
        if (_ns.isUserIdle) {
            _ns.onUserIdle();
        }
        _ns.isUserIdle = true;
    };

    _ns.monitorUserIdle = function(idleSeconds, onUserIdle) {
        window.setInterval(_ns.userIdleMonitor, idleSeconds * 1000);
        _ns.onUserIdle = onUserIdle;
        $(document).mousemove(function(e) {
            _ns.isUserIdle = false;
        });
        $(document).keypress(function(e) {
            _ns.isUserIdle = false;
        });
    };

    /**
     * Constructor for the CometD connectivity wrapper.
     *
     * Use configure() at the start of the application. Use start() to start
     * the after successful login. Use stop() to stop after logout.
     */
    _ns.Cometd = function() {
        var _super = new _ns.Base(),
            _self = _ns.derive(_super),
            _connected = false,
            // TODO: _isOn is ambiguous and needs rework
            _isOn = false;

        /**
         * Public function to toggle on/off (start/stop)
         */
        _self.toggleOnOff = function() {
            if (_isOn) {
                _self.stop();
            } else {
                _self.start();
            }
        };

        /**
         *
         */
        _self.isOn = function() {
            return _isOn;
        };

        /**
         * Public function to start
         */
        _self.start = function(token) {
            _isOn = true;
            _self.onConnecting();

            if (_ns.logger.isDebugEnabled()) {
                _ns.logger.debug('CometD: handshake...');
            }

            // See Java class: CometdConnectDto
            try {
                $.cometd.handshake({
                    ext: {
                        'org.savapage.authn': {
                            token: token
                        }
                    }
                });
            } catch (msg) {
                _ns.logger.warn('CometD exception on handshake: ' + msg);
            }
        };

        /**
         * Public function to stop (disconnect).
         * This has SYNCHRONOUS effect.
         */
        _self.stop = function() {
            var sync = true;
            _isOn = false;
            try {
                _self.onDisconnecting();
                $.cometd.disconnect(sync);
            } catch (msg) {
                _ns.logger.warn('CometD exception on disconnect: ' + msg);
            }
        };

        /**
         * Public function to listen to a channel, a callback function is
         * provided as 2nd argument.
         *
         * http://cometd.org/documentation/cometd-javascript/subscription
         *
         * NOTE: addListener is SYNCHRONOUS: when it returns, you are
         * guaranteed that the listener has been added.
         *
         */
        _self.addListener = function(channel, onMessage) {
            return $.cometd.addListener(channel, function(msg) {
                onMessage(msg);
            });
        };

        /**
         * Public function to subscribe to a channel, a callback function is
         * provided as 2nd argument.
         *
         * http://cometd.org/documentation/cometd-javascript/subscription
         *
         * NOTE: subscription() is asynchronous: it returns immediately, well
         * before the Bayeux server has received the subscription request.
         *
         * @return The subscription.
         */
        _self.subscribe = function(channel, onMessage) {
            var subscription;
            $.cometd.batch(function() {
                subscription = $.cometd.subscribe(channel, function(msg) {
                    onMessage(msg);
                });
            });
            return subscription;
        };

        _self.removeListener = function(subscription) {
            try {
                $.cometd.removeListener(subscription);
            } catch (ignore) {
            }
        };

        _self.unsubscribe = function(subscription) {
            try {
                $.cometd.unsubscribe(subscription);
            } catch (ignore) {
            }
        };

        /**
         * One-time initialization, setting the connection parameters and the
         * call-back functions. On $(window).unload CometD is disconnected.
         */
        _self.configure = function(maxNetworkDelay) {
            var cometURL = window.location.protocol + "//" + window.location.host + "/cometd";

            // Disconnect when the page unloads
            $(window).unload(function() {
                $.cometd.disconnect(true);
            });

            /*
             * "After you have configured the cometd object, it has not
             * started
             * the Bayeux communication yet. To start the Bayeux
             * communication,
             * you need to call handshake()"
             */

            /*
             * IMPORTANT: Because websocket is not supported yet by SavaPage
             * server,
             * we disable websocket transport (before doing the CometD
             * handshake).
             * This makes sure that CometD will not spill effort to try
             * WebSockets
             * and fall back to long-polling (because WebSockets fails).
             */
            $.cometd.websocketEnabled = false;
            $.cometd.unregisterTransport('websocket');

            /*
             * See:
             * http://cometd.org/documentation/2.x/cometd-javascript/configuration
             * 'maxNetworkDelay' (default 10000) : max number of milliseconds
             * to
             * wait before considering a request to the Bayeux server failed.
             */
            $.cometd.configure({
                url: cometURL,
                // logLevel : 'debug',
                logLevel: 'warn',
                maxNetworkDelay: maxNetworkDelay
            });

            // ...........................................................
            // CALLBACK-FUNCTION invoked when first contacting the server
            // and when the server has lost the state of this client
            // ...........................................................
            $.cometd.addListener('/meta/handshake', function(handshake) {

                // TODO: to be tested
                // var auth = handshake.ext &&
                // handshake.ext.'org.savapage.authn';
                // if (auth && auth.failed === true) {
                // // Authentication failed, tell the user
                // alert('Authentication failed!');
                // }

                if (handshake.successful === true) {
                    _self.onHandshakeSuccess();
                } else {
                    _self.onHandshakeFailure();
                }
            });
            // ...........................................................
            // CALLBACK-FUNCTION that manages the connection status with
            // the Bayeux server
            // ...........................................................
            $.cometd.addListener('/meta/connect', function(message) {
                /*
                 * "If a disconnect is issued during an active poll, the
                 * active
                 * poll is returned by the server and this triggers the
                 * /meta/connect listener."
                 *
                 * Ergo: when doing a disconnect() this request is queued
                 * behind
                 * the active long-poll. After this long-poll returns, this
                 * method will be called.
                 *
                 * This callback is also called after a successful return of
                 * a
                 * long-poll (as confirmation that the connection is still
                 * there)
                 */
                if ($.cometd.isDisconnected()) {
                    // Also after a disconnect()
                    _connected = false;
                    _self.onConnectionClosed();
                    return;
                }

                var wasConnected = _connected;
                _connected = message.successful === true;

                if (!wasConnected && _connected) {
                    // Reconnected
                    _self.onReconnect();
                } else if (wasConnected && !_connected) {
                    // Disconnected
                    _self.onConnectionBroken();
                }
            });
            // ...........................................................
            // CALLBACK-FUNCTION
            // ...........................................................
            $.cometd.addListener('/meta/disconnect', function(message) {
                if (message.successful) {
                    _connected = false;
                    _self.onConnectionClosed();
                }
            });
            // ...........................................................
            // CALLBACK-FUNCTION
            // ...........................................................
            $.cometd.addListener('/meta/unsuccessful', function(message) {
                /*
                 * #327: We handle this message as redundant: no action
                 * needed for now...
                 */
                $.noop();
            });

            // We will $.cometd.handshake() after a successful login;
        };

        /*
         * IMPORTANT
         */
        return _self;
    };

    /**
     * Constructor
     */
    _ns.Api = function(_i18n, _user) {
        var _onExpired = null,
            _onDisconnected = null;

        this.removeCallbacks = function() {
            _onExpired = null;
            _onDisconnected = null;
        };

        this.onExpired = function(foo) {
            _onExpired = foo;
        };

        this.onDisconnected = function(foo) {
            _onDisconnected = foo;
        };

        /**
         * This produces a unique URL by adding current time (milliseconds)
         * as parameter. This is done to workaround IE8 caching problems. I.e
         * IE8 caches according to the /api?... URL, so although content
         * differs
         * in subsequent calls the same old cached PDF is shown.
         */
        this.getUrl4Pdf = function(ranges, removeGraphics, ecoprint, grayscale, rasterize, jobIndex) {
            // number of milliseconds since 1970/01/01
            var d = new Date();
            return '/api?webAppType=' + _ns.WEBAPP_TYPE + '&request=pdf&user=' + _user.id
                + '&jobIndex=' + jobIndex + '&ranges=' + ranges + '&removeGraphics=' + removeGraphics
                + '&ecoprint=' + ecoprint + '&grayscale=' + grayscale + '&rasterize=' + rasterize
                + '&unique=' + d.getTime().toString();
        };

        this.getUrl4PdfHoldJob = function(fileName, isJobTicket) {
            var req = isJobTicket ? 'pdf-jobticket' : 'pdf-outbox';
            return '/api?webAppType=' + _ns.WEBAPP_TYPE + '&request=' + req + '&user=' + _user.id + '&fileName=' + fileName;
        };

        /**
         * Simulate error response for disconnect situation
         */
        this.simulateDisconnectError = function() {
            var msg = 'msg-disconnected';
            return {
                result: {
                    code: '99',
                    msg: msg,
                    txt: _i18n.string(msg)
                }
            };
        };

        /**
         * Downloads a file.
         */
        this.download = function(request, data, requestSub, requestParm) {
            var form = "#sp-file-download-api-form";
            $(form + " input[name='request']").attr("value", request);
            $(form + " input[name='request-sub']").attr("value", requestSub);
            $(form + " input[name='request-parm']").attr("value", requestParm);
            $(form + " input[name='user']").attr("value", _user.id);
            $(form + " input[name='data']").attr("value", JSON.stringify(data));
            $(form).submit();
        };

        /** */
        this.unloadWebApp = function() {
            var request = 'webapp-unload';
            if (navigator.sendBeacon) {
                navigator.sendBeacon('/api?request=' + request +
                    '&webAppType=' + _ns.WEBAPP_TYPE +
                    '&user=' + encodeURIComponent(_user.id), null);
            } else {
                this.removeCallbacks();
                this.call({
                    request: request
                });
            }
        };

        /**
         *
         */
        this.call = function(apiData) {
            var res,
                json;

            if (_ns.logger.isDebugEnabled()) {
                _ns.logger.debug(_ns.WEBAPP_TYPE + ' /api : ' + apiData.request);
            }

            apiData.webAppType = _ns.WEBAPP_TYPE;

            if (_user && _user.loggedIn) {
                apiData.user = _user.id;
            }

            // Since this is a synchronous call, this does NOT show in
            // Chrome browser (at the moment of the call).
            $.mobile.loading("show");

            json = $.ajax({
                type: "POST",
                async: false,
                dataType: "json",
                url: '/api',
                data: apiData
            }).responseText;

            $.mobile.loading("hide");

            if (json) {
                res = $.parseJSON(json);
            }

            if (res && res.result && _ns.logger.isDebugEnabled()) {
                _ns.logger.debug('/api -> return [' + (res.result.code || '?') + '] ' + (res.result.txt === undefined ? '' : ('\"' + res.result.txt + '\"')));
            }

            if (!res) {
                res = this.simulateDisconnectError();
                if (_onDisconnected) {
                    _onDisconnected();
                }
            } else if (res.result.code === '9') {
                if (_onExpired) {
                    _onExpired();
                }
            }

            return res;
        };

        /**
         *
         */
        this.callAsync = function(apiData, onSuccess, onFinished, onError) {
            var _this = this;

            if (_ns.logger.isDebugEnabled()) {
                _ns.logger.debug(_ns.WEBAPP_TYPE + ' /api (async): ' + apiData.request);
            }

            apiData.webAppType = _ns.WEBAPP_TYPE;

            $.mobile.loading("show");

            $.post('/api/', apiData, function(data) {
                $.noop();
            }).done(function(data) {// @JQ-1.8
                onSuccess(data);
            }).fail(function() {// @JQ-1.8
                onError(_this.simulateDisconnectError());
                if (_onDisconnected) {
                    _onDisconnected();
                }
            }).always(function() {
                $.mobile.loading("hide");
                onFinished();
            });
            // @JQ-1.8
        };
    };

    /*
     * Mix-in for _ns.Panel* objects.
     *
     * NOTE: null values need to be set be the client.
     */
    _ns.PanelCommon = {

        userId: null,
        api: null,
        view: null,
        refreshPanelCommon: null,
        onDisconnected: null,

        /**
         * Loads an ADMIN page of a list in a Panel.
         *
         * @param panel A Panel object.
         * @param wClassPage The Wicket class of teh Panel
         * @param jqId The HTML id attribute of the Panel
         */
        loadListPageAdmin: function(panel, wClassPage, jqId) {
            _ns.PanelCommon.loadListPage(panel, "admin/" + wClassPage, jqId);
        },

        /**
         * Loads a COMMON page of a list in a Panel.
         *
         * @param panel A Panel object.
         * @param wClassPage The Wicket class of teh Panel
         * @param jqId The HTML id attribute of the Panel
         */
        loadListPageCommon: function(panel, wClassPage, jqId) {
            _ns.PanelCommon.loadListPage(panel, wClassPage, jqId);
        },

        /**
         * Loads a page of a list in a Panel.
         *
         * @param panel A Panel object.
         * @param wClassPage The Wicket class of teh Panel
         * @param jqId The HTML id attribute of the Panel
         */
        loadListPage: function(panel, wClassPage, jqId) {

            var data = null,
                jsonData = null,
                url = '/pages/' + wClassPage + _ns.WebAppTypeUrlParm();

            _ns.logger.debug(url);

            if (panel.getInput) {
                data = panel.getInput();
            }

            if (data) {
                jsonData = JSON.stringify(data);
            }

            $.mobile.loading("show");
            $.ajax({
                type: "POST",
                async: true,
                url: url,
                data: {
                    user: _ns.PanelCommon.userId,
                    data: jsonData
                }
            }).done(function(html) {
                var json;
                $(jqId).html(html).enhanceWithin();
                json = $(jqId + ' .json-rsp').text();
                if (panel.onOutput && json) {
                    panel.onOutput($.parseJSON(json));
                }
            }).fail(function() {
                _ns.PanelCommon.onDisconnected();
            }).always(function() {
                $.mobile.loading("hide");
            });

        },

        /**
         * Checks if we got the right page: may be we got the
         * LicenseMsg page or the auth error page.
         */
        onValidPage: function(onOK) {
            if ($('#license-msg').length > 0 || $('#sp-not-authorized').length > 0) {
                $.mobile.loading("hide");
                return;
            }
            onOK();
        }
    };

    /**
     *
     */
    _ns.PanelAccountTrxBase = {

        // The HTML id attribute of the Panel.
        jqId: null,

        applyDefaults: function() {

            this.input.page = 1;
            this.input.maxResults = 10;

            this.input.select.user_id = null;
            this.input.select.account_id = null;

            this.input.select.text = null;

            this.input.select.trxType = null;
            this.input.select.date_from = null;
            this.input.select.date_to = null;

            this.input.sort.field = 'TRX_DATE';
            this.input.sort.ascending = false;

        },

        beforeload: function() {
            this.applyDefaults();
        },

        afterload: function() {
            var _view = _ns.PanelCommon.view;

            _view.mobipick($("#sp-accounttrx-date-from"));
            _view.mobipick($("#sp-accounttrx-date-to"));

            this.m2v();
            this.page(this.input.page);
        },

        m2v: function() {
            var _view = _ns.PanelCommon.view;

            $('#sp-accounttrx-containing-text').val('');

            _view.checkRadio('sp-accounttrx-select-type', 'sp-accounttrx-select-type-all');

            _view.mobipickSetDate($('#sp-accounttrx-date-from'), this.input.select.date_from);
            _view.mobipickSetDate($('#sp-accounttrx-date-to'), this.input.select.date_to);

            _view.checkRadio('sp-accounttrx-sort-by', 'sp-accounttrx-sort-by-date');
            _view.checkRadio('sp-accounttrx-sort-dir', 'sp-accounttrx-sort-dir-desc');
        },

        v2m: function() {
            var _view = _ns.PanelCommon.view,
                sel = $('#sp-accounttrx-date-from'),
                date = _view.mobipickGetDate(sel),
                val,
                present = (sel.val().length > 0);

            //
            this.input.select.date_from = (present ? date.getTime() : null);

            //
            val = $('#sp-accounttrx-hidden-user-id').val();
            present = (val.length > 0);
            this.input.select.user_id = (present ? val : null);

            //
            val = $('#sp-accounttrx-hidden-account-id').val();
            present = (val.length > 0);
            this.input.select.account_id = (present ? val : null);

            //
            val = _view.getRadioValue('sp-accounttrx-select-type');
            present = (val.length > 0);
            this.input.select.trxType = (present ? val : null);

            sel = $('#sp-accounttrx-date-to');
            date = _view.mobipickGetDate(sel);
            present = (sel.val().length > 0);
            this.input.select.date_to = (present ? date.getTime() : null);

            sel = $('#sp-accounttrx-containing-text');
            present = (sel.val().length > 0);
            this.input.select.text = (present ? sel.val() : null);

            this.input.sort.field = _view.getRadioValue('sp-accounttrx-sort-by');
            this.input.sort.ascending = _view.isRadioIdSelected('sp-accounttrx-sort-dir', 'sp-accounttrx-sort-dir-asc');
        },

        // JSON input
        input: {
            page: 1,
            maxResults: 10,
            select: {
                user_id: null,
                account_id: null,
                // See Java: org.savapage.core.dao.enums.AccountTrxTypeEnum
                trxType: null,
                date_from: null,
                date_to: null,
                text: null
            },
            sort: {
                field: 'TRX_DATE', // enum: AccountTrx.TrxType
                ascending: false
            }
        },

        // JSON output
        output: {
            lastPage: null,
            nextPage: null,
            prevPage: null
        },

        refresh: function(skipBeforeLoad) {
            _ns.PanelCommon.refreshPanelCommon('AccountTrxBase', skipBeforeLoad, this);
        },

        // show page
        page: function(nPage) {
            var _this = this;
            _ns.PanelCommon.onValidPage(function() {
                _this.input.page = nPage;
                _this.v2m();
                _ns.PanelCommon.loadListPageCommon(_this, 'AccountTrxPage', '#sp-accounttrx-list-page');
            });
        },

        getInput: function() {
            return this.input;
        },

        onOutput: function(output) {
            var _this = this;
            this.output = output;
            /*
             * NOTICE the $().one() construct. Since the page get
             * reloaded all the time, we want a single-shot binding.
             */
            $(".sp-accounttrx-page").one('click', null, function() {
                _this.page(parseInt($(this).text(), 10));
                // return false so URL is not followed.
                return false;
            });
            $(".sp-accounttrx-page-next").one('click', null, function() {
                _this.page(_this.output.nextPage);
                // return false so URL is not followed.
                return false;
            });
            $(".sp-accounttrx-page-prev").one('click', null, function() {
                _this.page(_this.output.prevPage);
                // return false so URL is not followed.
                return false;
            });

        }
    };

    /**
     *
     */
    _ns.PanelPaperCutAccountTrxBase = {

        // The HTML id attribute of the Panel.
        jqId: null,

        applyDefaults: function() {

            this.input.page = 1;
            this.input.maxResults = 10;

            this.input.select.user_id = null;

            this.input.select.text = null;

            this.input.select.trxType = null;
            this.input.select.date_from = null;
            this.input.select.date_to = null;

            this.input.sort.field = 'TRX_DATE';
            this.input.sort.ascending = false;
        },

        beforeload: function() {
            this.applyDefaults();
        },

        afterload: function() {
            var _view = _ns.PanelCommon.view;

            _view.mobipick($("#sp-accounttrx-date-from-pc"));
            _view.mobipick($("#sp-accounttrx-date-to-pc"));

            this.m2v();
            this.page(this.input.page);
        },

        m2v: function() {
            var _view = _ns.PanelCommon.view;

            $('#sp-accounttrx-containing-text-pc').val('');

            _view.checkRadio('sp-accounttrx-select-type-pc', 'sp-accounttrx-select-type-all-pc');

            _view.mobipickSetDate($('#sp-accounttrx-date-from-pc'), this.input.select.date_from);
            _view.mobipickSetDate($('#sp-accounttrx-date-to-pc'), this.input.select.date_to);

            _view.checkRadio('sp-accounttrx-sort-by-pc', 'sp-accounttrx-sort-by-date-pc');
            _view.checkRadio('sp-accounttrx-sort-dir-pc', 'sp-accounttrx-sort-dir-desc-pc');
        },

        v2m: function() {
            var _view = _ns.PanelCommon.view,
                sel = $('#sp-accounttrx-date-from-pc'),
                date = _view.mobipickGetDate(sel),
                val,
                present = (sel.val().length > 0);

            this.input.select.date_from = (present ? date.getTime() : null);

            this.input.select.user_id = $('#sp-accounttrx-hidden-user-id-pc').val();

            val = _view.getRadioValue('sp-accounttrx-select-type-pc');
            present = (val.length > 0);
            this.input.select.trxType = (present ? val : null);

            sel = $('#sp-accounttrx-date-to-pc');
            date = _view.mobipickGetDate(sel);
            present = (sel.val().length > 0);
            this.input.select.date_to = (present ? date.getTime() : null);

            sel = $('#sp-accounttrx-containing-text-pc');
            present = (sel.val().length > 0);
            this.input.select.text = (present ? sel.val() : null);

            this.input.sort.field = _view.getRadioValue('sp-accounttrx-sort-by-pc');
            this.input.sort.ascending = _view.isRadioIdSelected('sp-accounttrx-sort-dir-pc', 'sp-accounttrx-sort-dir-asc-pc');
        },

        // JSON input
        input: {
            page: 1,
            maxResults: 10,
            select: {
                user_id: null,
                // org.savapage.ext.papercut.PapercutAccountTrx.TrxType
                trxType: null,
                date_from: null,
                date_to: null,
                text: null
            },
            sort: {
                // org.savapage.ext.papercut.PaperCutDb.Field
                field: 'TRX_DATE',
                ascending: false
            }
        },

        // JSON output
        output: {
            lastPage: null,
            nextPage: null,
            prevPage: null
        },

        refresh: function(skipBeforeLoad) {
            _ns.PanelCommon.refreshPanelCommon('PaperCutAccountTrxBase', skipBeforeLoad, this);
        },

        // show page
        page: function(nPage) {
            var _this = this;
            _ns.PanelCommon.onValidPage(function() {
                _this.input.page = nPage;
                _this.v2m();
                _ns.PanelCommon.loadListPageCommon(_this, 'PaperCutAccountTrxPage', '#sp-accounttrx-list-page-pc');
            });
        },

        getInput: function() {
            return this.input;
        },

        onOutput: function(output) {
            var _this = this;
            this.output = output;
            /*
             * NOTICE the $().one() construct. Since the page get
             * reloaded all the time, we want a single-shot binding.
             */
            $(".sp-accounttrx-page-pc").one('click', null, function() {
                _this.page(parseInt($(this).text(), 10));
                return false;
            });
            $(".sp-accounttrx-page-pc-next").one('click', null, function() {
                _this.page(_this.output.nextPage);
                return false;
            });
            $(".sp-accounttrx-page-pc-prev").one('click', null, function() {
                _this.page(_this.output.prevPage);
                return false;
            });
        }
    };

    /**
     * NOTE: null values need to be set be the client.
     */
    _ns.PanelDocLogBase = {

        // The HTML id attribute of the Panel.
        jqId: null,

        doc_type_default: undefined,

        // HACK: hidden fields must be present/set in the container of
        // this panel.
        clearHiddenUserid: function() {
            $('#sp-doclog-hidden-user-id').val("");
            $("#sp-doclog-user-title").html("");

            $('#sp-doclog-hidden-account-id').val("");
            $("#sp-doclog-account-title").html("");
        },

        applyDefaultForTicket: function() {
            this.applyDefaults();
            this.input.select.doc_type = 'TICKET';
        },

        applyDefaultForPrintSite: function() {
            this.applyDefaults();
            this.input.select.doc_type = 'PRINT';
        },

        /*
         * Generic: can be reused
         */
        applyDefaults: function() {

            this.input.page = 1;
            this.input.maxResults = 10;

            /*
             * NOTE: In User WebApp the user_id of the logged in user
             * is used. In Admin WebApp value of field below is used.
             */
            this.input.select.user_id = null;

            //
            this.input.select.account_id = null;

            // ALL, IN, OUT, PDF, PRINT, TICKET
            this.input.select.doc_type = this.doc_type_default;

            this.input.select.date_from = null;
            this.input.select.date_to = null;
            this.input.select.doc_name = '';

            this.input.select.queue_id = null;

            this.input.select.signature = "";
            this.input.select.destination = "";
            // Boolean
            this.input.select.letterhead = undefined;

            this.input.select.printer_id = null;
            this.input.select.job_state = "0";
            // Boolean
            this.input.select.duplex = undefined;

            this.input.select.author = '';
            this.input.select.subject = '';
            this.input.select.keywords = '';
            this.input.select.userpw = '';
            this.input.select.ownerpw = '';
            // Boolean
            this.input.select.encrypted = undefined;

            this.input.select.ticket_number = '';
            this.input.select.ticket_number_mail = '';

            this.input.sort.field = 'date';
            this.input.sort.ascending = false;
        },

        beforeload: function() {
            this.applyDefaults();
        },

        afterload: function() {
            var _view = _ns.PanelCommon.view;

            _view.mobipick($("#sp-doclog-date-from"));
            _view.mobipick($("#sp-doclog-date-to"));

            this.m2v();
            this.page(this.input.page);
        },

        setVisibility: function() {
            var _view = _ns.PanelCommon.view;

            $('.sp-doclog-cat-out').hide();
            $('.sp-doclog-cat-pdf').hide();
            $('.sp-doclog-cat-queue').hide();
            $('.sp-doclog-cat-printer').hide();
            $('.sp-doclog-cat-ticket').hide();

            if (_view.isRadioIdSelected('sp-doclog-select-type', 'sp-doclog-select-type-in')) {
                $('.sp-doclog-cat-queue').show();
            } else if (_view.isRadioIdSelected('sp-doclog-select-type', 'sp-doclog-select-type-out')) {
                $('.sp-doclog-cat-out').show();
                $('.sp-doclog-cat-out-detail').show();
            } else if (_view.isRadioIdSelected('sp-doclog-select-type', 'sp-doclog-select-type-pdf')) {
                $('.sp-doclog-cat-out').show();
                $('.sp-doclog-cat-out-detail').show();
                $('.sp-doclog-cat-pdf').show();
            } else if (_view.isRadioIdSelected('sp-doclog-select-type', 'sp-doclog-select-type-print')) {
                $('.sp-doclog-cat-out').show();
                $('.sp-doclog-cat-out-detail').show();
                $('.sp-doclog-cat-printer').show();
                $('.sp-doclog-cat-printer-list').show();
                $('.sp-doclog-cat-printer-print-layout').show();
            } else if (_view.isRadioIdSelected('sp-doclog-select-type', 'sp-doclog-select-type-ticket')) {
                $('.sp-doclog-cat-ticket').show();
                $('.sp-doclog-cat-out').show();
                $('.sp-doclog-cat-out-detail').hide();
                $('.sp-doclog-cat-printer').show();
                $('.sp-doclog-cat-printer-list').hide();
                $('.sp-doclog-cat-printer-print-layout').hide();
            }
        },

        m2v: function() {
            var sel, val, id,
                _view = _ns.PanelCommon.view;

            $('#sp-doclog-document-name').val(this.input.select.doc_name);
            $('#sp-doclog-ticket-number').val(this.input.select.ticket_number);

            sel = $('#sp-doclog-ticket-number-mail');
            if (sel.length > 0) {
                sel.val(this.input.select.ticket_number_mail);
            }

            // For future use.
            //$('#sp-doc-out-signature').val(this.input.select.signature);

            sel = $('#sp-doc-out-destination');
            if (sel.length > 0) {
                sel.val(this.input.select.destination);
            }

            _view.mobipickSetDate($('#sp-doclog-date-from'), this.input.select.date_from);
            _view.mobipickSetDate($('#sp-doclog-date-to'), this.input.select.date_to);

            //--
            _view.checkRadioValue('sp-doclog-sort-by', this.input.sort.field);

            id = 'sp-doclog-sort-dir';
            _view.checkRadio(id, this.input.sort.ascending ? id + '-asc' : id + '-desc');

            //---------------------------------
            if (this.input.select.printer_id) {
                $('#sp-print-out-printer').val(this.input.select.printer_id).selectmenu('refresh');
                this.input.select.doc_type = "PRINT";
            } else if (this.input.select.queue_id) {
                $('#sp-print-in-queue').val(this.input.select.queue_id).selectmenu('refresh');
                this.input.select.doc_type = "IN";
            }

            $('#sp-print-out-state').val(this.input.select.job_state).selectmenu('refresh');

            //--
            if (!this.doc_type_default) {
                // Initialize default from first-time setting.
                this.doc_type_default = _view.getRadioValue('sp-doclog-select-type');
                this.input.select.doc_type = this.doc_type_default;
            }

            _view.checkRadioValue('sp-doclog-select-type', this.input.select.doc_type);

            val = this.input.select.letterhead;
            _view.checkRadioValue('sp-doc-out-lh', val === undefined ? "" : (val ? "1" : "0"));

            val = this.input.select.duplex;
            _view.checkRadioValue('sp-print-out-duplex', val === undefined ? "" : (val ? "1" : "0"));

            val = this.input.select.encrypted;
            _view.checkRadioValue('sp-pdf-out-encrypt', val === undefined ? "" : (val ? "1" : "0"));

            //---------------------------------
            this.setVisibility();
        },

        v2m: function() {
            var _view = _ns.PanelCommon.view,
                val,
                sel,
                date,
                present;

            /*
             * QUICK & DIRTY: Use sel as indicator to see if WebApp is
             * properly loaded (e.g. when F5 browser refresh).
             */
            sel = $('#sp-doclog-date-from');
            if (sel.length === 0) {
                return;
            }

            // HACK: hidden field must be present/set in the container of
            // this panel.
            val = $('#sp-doclog-hidden-user-id').val();
            present = (val.length > 0);
            this.input.select.user_id = (present ? val : null);

            // HACK: hidden field must be present/set in the container of
            // this panel.
            val = $('#sp-doclog-hidden-account-id').val();
            present = (val.length > 0);
            this.input.select.account_id = (present ? val : null);

            //
            date = _view.mobipickGetDate(sel);
            present = (sel.val().length > 0);
            this.input.select.date_from = (present ? date.getTime() : null);

            this.input.select.doc_type = _view.getRadioValue('sp-doclog-select-type');

            sel = $('#sp-doclog-date-to');
            date = _view.mobipickGetDate(sel);
            present = (sel.val().length > 0);
            this.input.select.date_to = (present ? date.getTime() : null);

            sel = $('#sp-doclog-document-name');
            present = (sel.val().length > 0);
            this.input.select.doc_name = (present ? sel.val() : null);

            sel = $('#sp-print-out-printer');
            present = (sel.val() !== "0");
            this.input.select.printer_id = ((this.input.select.doc_type === "PRINT" && present) ? sel.val() : null);

            this.input.select.job_state = $('#sp-print-out-state').val();

            sel = $('#sp-print-in-queue');
            present = (sel.val() !== "0");
            this.input.select.queue_id = ((this.input.select.doc_type === "IN" && present) ? sel.val() : null);

            this.input.sort.field = _view.getRadioValue('sp-doclog-sort-by');

            this.input.sort.ascending = _view.isRadioIdSelected('sp-doclog-sort-dir', 'sp-doclog-sort-dir-asc');

            // For future use.
            /*
             sel = $('#sp-doc-out-signature');
             present = (sel.val().length > 0);
             this.input.select.signature = ( present ? sel.val() : null);
             */
            this.input.select.signature = null;

            sel = $('#sp-doc-out-destination');
            present = sel.length > 0 && sel.val().length > 0;
            this.input.select.destination = (present ? sel.val() : null);

            // val is undefined when radiobutton 'sp-doc-out-lh' is missing,
            // due to user privileges.
            val = _view.getRadioValue('sp-doc-out-lh');
            this.input.select.letterhead = (!val || val === "" ? undefined : (val === "1"));

            //
            val = _view.getRadioValue('sp-print-out-duplex');
            this.input.select.duplex = (val === "" ? undefined : (val === "1"));

            //
            sel = $('#sp-pdf-out-author');
            present = (sel.val().length > 0);
            this.input.select.author = (present ? sel.val() : null);

            sel = $('#sp-pdf-out-subject');
            present = (sel.val().length > 0);
            this.input.select.subject = (present ? sel.val() : null);

            sel = $('#sp-pdf-out-keywords');
            present = (sel.val().length > 0);
            this.input.select.keywords = (present ? sel.val() : null);

            sel = $('#sp-pdf-out-pw-user');
            present = (sel.val().length > 0);
            this.input.select.userpw = (present ? sel.val() : null);

            sel = $('#sp-pdf-out-pw-owner');
            present = (sel.val().length > 0);
            this.input.select.ownerpw = (present ? sel.val() : null);

            //
            val = _view.getRadioValue('sp-pdf-out-encrypt');
            this.input.select.encrypted = (val === "" ? undefined : (val === "1"));

            //
            sel = $('#sp-doclog-ticket-number');
            present = (sel.val().length > 0);
            this.input.select.ticket_number = (present ? sel.val() : null);

            //
            sel = $('#sp-doclog-ticket-number-mail');
            present = sel.length > 0;
            this.input.ticketNumberMailView = present;

            present = present && sel.val().length > 0;
            this.input.select.ticket_number_mail = (present ? sel.val() : null);
        },

        // JSON input
        input: {
            page: 1,
            maxResults: 10,
            ticketNumberMailView: null,
            select: {
                date_from: null,
                date_to: null,
                doc_name: null,
                doc_type: "ALL",
                user_id: null,
                printer_id: null,
                queue_id: null,
                ticket_number: null,
                ticket_number_mail: null
            },
            sort: {
                field: 'date', // date | name | queue | printer
                ascending: false
            }
        },

        // JSON output
        output: {
            lastPage: null,
            nextPage: null,
            prevPage: null
        },

        refresh: function(skipBeforeLoad) {
            _ns.PanelCommon.refreshPanelCommon('DocLogBase', skipBeforeLoad, this);
        },

        // show page
        page: function(nPage) {
            var _this = this;
            _ns.PanelCommon.onValidPage(function() {
                _this.input.page = nPage;
                _this.v2m();
                _ns.PanelCommon.loadListPageCommon(_this, 'DocLogPage', '#sp-doclog-list-page');
            });
        },

        getInput: function() {
            return this.input;
        },

        onOutput: function(output) {
            var _this = this;
            this.output = output;
            /*
             * NOTICE the $().one() construct. Since the page get
             * reloaded all the time, we want a single-shot binding.
             */
            $(".sp-doclog-page").one('click', null, function() {
                _this.page(parseInt($(this).text(), 10));
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-doclog-page-next").one('click', null, function() {
                _this.page(_this.output.nextPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });
            $(".sp-doclog-page-prev").one('click', null, function() {
                _this.page(_this.output.prevPage);
                /*
                 * return false so URL is not followed.
                 */
                return false;
            });

            $('.sp-sparkline-doclog').sparkline('html', {
                enableTagOptions: true
            });
        },

        onDocStoreDownloadDelete: function(jqParent, _api, _view) {
            var _this = this;
            jqParent.on('click', '.sp-doclog-docstore-archive-download', null, function() {
                _api.download("pdf-docstore-archive", null, $(this).attr('data-savapage'));
                return false;
            }).on('click', '.sp-doclog-docstore-journal-download', null, function() {
                _api.download("pdf-docstore-journal", null, $(this).attr('data-savapage'));
                return false;
            }).on('click', '.sp-doclog-docstore-delete', null, function() {
                $('#sp-doclog-store-delete-popup .sp-ui-btn-ok').attr('data-savapage',
                    $(this).attr('data-savapage'));
                $('#sp-doclog-store-delete-popup').popup('open', {
                    positionTo: $(this)
                });
                return false;
            }).on('click', '#sp-doclog-store-delete-popup .sp-ui-btn-ok', null, function() {
                var res;
                $('#sp-doclog-store-delete-popup').popup('close');
                res = _api.call({
                    'request': 'doclog-store-delete',
                    'dto': JSON.stringify({
                        'docLogId': $(this).attr('data-savapage')
                    })
                });
                if (res.result.code === _ns.ApiResultCodeEnum.OK) {
                    _this.refresh();
                }
                _view.showApiMsg(res);
                return false;
            }).on('click', '#sp-doclog-store-delete-popup .sp-ui-btn-cancel', null, function() {
                $('#sp-doclog-store-delete-popup').popup('close');
            });
        }
    };

    // =========================================================================
    _ns.QuickSearchUtils = {
        /**
         * @param rsp DtoQuickSearchRsp
         */
        setNavigation: function(_view, qsButtons, rsp) {
            var selWlk;
            if (qsButtons) {
                selWlk = qsButtons.find('.sp-button-prev');
                selWlk.attr('data-savapage', rsp.prevPosition).show();

                selWlk = qsButtons.find('.sp-button-curr');
                selWlk.attr('data-savapage', rsp.currPosition).show();
                selWlk.html(rsp.currPage);

                selWlk = qsButtons.find('.sp-button-next');
                selWlk.attr('data-savapage', rsp.nextPosition).show();

                selWlk = qsButtons.find('.sp-button-last');
                selWlk.attr('data-savapage', rsp.lastPosition).show();
                selWlk.html(rsp.lastPage);

                _view.visible(qsButtons, rsp.lastPosition > 0);
            }
        }
    };
    /**
     * Constructor: Generic Quick Search on Object
     */
    _ns.QuickObjectSearch = function(_view, _api) {
        var _this,
            _quickObjectCache = [],
            _quickObjectSelected,
            _lastFilter,
            //
            _onQuickObjectSearch = function(target, request, filter, onFilterProps, onDisplayObject) {
                /* QuickSearchFilterDto */
                var res,
                    filterProps,
                    html = "";

                // Prevent duplicate search on "focusout" of search field.
                if (_lastFilter === filter) {
                    return;
                }
                _lastFilter = filter;

                _quickObjectCache = [];
                _quickObjectSelected = undefined;

                if (filter && filter.length > 0) {

                    filterProps = {
                        filter: filter,
                        maxResults: 5
                    };
                    if (onFilterProps) {
                        onFilterProps(filterProps);
                    }

                    res = _api.call({
                        request: request,
                        dto: JSON.stringify(filterProps)
                    });
                    if (res.result.code === _ns.ApiResultCodeEnum.OK) {
                        _quickObjectCache = res.dto.items;
                        $.each(_quickObjectCache, function(key, item) {
                            html += "<li class=\"ui-mini\" data-icon=\"false\" data-savapage=\"" + key + "\">";
                            html += "<a tabindex=\"0\" href=\"#\">";
                            if (onDisplayObject) {
                                html += onDisplayObject(item);
                            } else {
                                html += item.text;
                            }
                            html += "</a></li>";
                        });
                    } else {
                        _view.showApiMsg(res);
                    }
                } else {
                    if (_this.onClearObject) {
                        _this.onClearObject();
                    }
                }
                target.html(html).filterable("refresh");
            };

        this.onCreate = function(parent, filterId, request, onFilterProps,
            onDisplayObject, onSelectObject, onClearObject, onQuickSearchBefore,
            listItemSelector) {

            var filterableObjectId = $("#" + filterId), clickSelector;

            _this = this;

            this.onSelectObject = onSelectObject;
            this.onClearObject = onClearObject;
            this.onQuickSearchBefore = onQuickSearchBefore;

            filterableObjectId.on("filterablebeforefilter", function(e, data) {
                if (_this.onQuickSearchBefore) {
                    onQuickSearchBefore();
                }
                _onQuickObjectSearch($(this), request, data.input.get(0).value, onFilterProps, onDisplayObject);
            });

            clickSelector = '#' + filterId + ' li';
            if (listItemSelector) {
                clickSelector += ' ' + listItemSelector;
            }

            parent.on('click', clickSelector, null, function(event) {
                var attr = "data-savapage",
                    selListItem = listItemSelector ? $(this).closest('li') : $(this),
                    iCache = selListItem.attr(attr);
                _quickObjectSelected = _quickObjectCache[iCache];
                filterableObjectId.empty().filterable("refresh");
                if (_this.onSelectObject) {
                    _this.onSelectObject(_quickObjectSelected, event);
                }
            });
        };
    };

    /**
     * Constructor
     *
     * @param _id :
     *            The jquery selector identifying the page, e.g.: '#mypage'
     * @param _class :
     *            The Wicket java class (with URL path prefix)
     */
    _ns.Page = function(_i18n, _view, _id, _class) {

        this.isLoaded = false;

        this.id = function() {
            return _id;
        };

        this.loadShowAsync = function(onDone, jsonData) {
            var _this = this;
            _view.showPageAsync($(_id), _class, function() {
                _this.isLoaded = true;
                if (onDone) {
                    onDone();
                }
            }, jsonData);
        };

        this.load = function() {
            _view.loadPage(_id, _class);
            this.isLoaded = true;
            return this;
        };

        this.show = function() {
            _view.changePage($(_id));
            return this;
        };

        this.showApiMsg = function(res) {
            if (res && res.result) {
                _view.apiResMsg(res.result);
            }
        };

    };

    /**
     * Constructor
     */
    _ns.PageLanguage = function(_i18n, _view) {
        var _page,
            _self,
            _onSelectLocale;

        _page = new _ns.Page(_i18n, _view, '#page-language', 'Language');
        _self = _ns.derive(_page);

        /**
         *
         */
        _self.onSelectLocale = function(foo) {
            _onSelectLocale = foo;
        };

        /**
         *
         */
        $(_self.id()).on('pagecreate', function(event) {

            $('#language-list').on('click', null, null, function(event) {
                var target = $(event.target), sel = target;
                if (!sel.attr('data-language')) {
                    sel = sel.closest('[data-language]'); // up
                    if (!sel.attr('data-language')) {
                        sel = target.find('[data-language]'); // down
                    }
                }
                _onSelectLocale(sel.attr('data-language'), sel.attr('data-country'));
                $('#button-lang-cancel').click();
                return false;
            });
        });
        /*
         * IMPORTANT
         */
        return _self;
    };

    /**
     * Constructor
     */
    _ns.PageUserPasswordReset = function(_i18n, _view) {
        var _page,
            _self,
            _onSelectReset;

        _page = new _ns.Page(_i18n, _view, '#page-user-pw-reset', 'UserPasswordReset');
        _self = _ns.derive(_page);

        /**
         *
         */
        _self.onSelectReset = function(foo) {
            _onSelectReset = foo;
        };

        /**
         *
         */
        $(_self.id()).on('pagecreate', function(event) {

            $('#button-user-pw-reset').click(function(e) {
                if (_view.checkPwMatch($('#user-pw-reset'), $('#user-pw-reset-confirm'))) {
                    _onSelectReset($("#user-pw-reset").val());
                }
                return false;
            });
        }).on('pagebeforehide', function(event, ui) {
            $("#user-pw-reset").val('');
            $("#user-pw-reset-confirm").val('');
        });
        /*
         * IMPORTANT
         */
        return _self;
    };

    /**
     * NOTE: this 'class' is only used locally in this file by 'page-login'.
     */
    _ns.PageAbout = function(_i18n, _view) {
        var _page = new _ns.Page(_i18n, _view, '#page-info', 'AppAbout'),
            _self = _ns.derive(_page);

        $(_self.id()).on('pagebeforehide', function(event, ui) {
            if (_self.onHide) {
                _self.onHide(ui.nextPage.attr('id'));
            }
        });

        /*
         * IMPORTANT
         */
        return _self;
    };

    /**
     * Constructor
     */
    _ns.PageLogin = function(_i18n, _view, _api) {
        var _page,
            _self,
            _pageAbout,
            _util = _ns.Utils,
            _onLanguage,
            _onLogin,
            _onShow,
            _authName,
            _authEmail,
            _authId,
            _authCardLocal,
            _authCardIp,
            _authYubiKey,
            _authCardPinReq,
            _authCardSelfAssoc,

            _ID_MODE_NAME = '#sp-div-login-name',
            _ID_MODE_EMAIL = '#sp-div-login-email',
            _ID_MODE_ID = '#sp-div-login-number',
            _ID_MODE_YUBIKEY = '#sp-div-login-yubikey',
            _ID_MODE_CARD_LOCAL = '#sp-div-login-card-local',
            _ID_MODE_CARD_IP = '#sp-div-login-card-ip',
            _ID_MODE_CARD_ASSOC = '#sp-div-login-card-assoc',
            _ID_MODE_TOTP = '#sp-div-login-totp',

            _ID_BTN_MODE_NAME = '#sp-btn-login-mode-name',
            _ID_BTN_MODE_EMAIL = '#sp-btn-login-mode-email',
            _ID_BTN_MODE_ID = '#sp-btn-login-mode-id',
            _ID_BTN_MODE_YUBIKEY = '#sp-btn-login-mode-yubikey',
            _ID_BTN_MODE_CARD_LOCAL = '#sp-btn-login-mode-card-local',
            _ID_BTN_MODE_CARD_IP = '#sp-btn-login-mode-card-ip',

            _ID_BTN_LOGIN_NAME = '#sp-btn-login-name',
            _ID_BTN_LOGIN_EMAIL = '#sp-btn-login-email',
            _ID_BTN_LOGIN_ID = '#sp-btn-login-number',
            _ID_BTN_LOGIN_YUBIKEY = '#sp-btn-login-yubikey',
            _ID_BTN_LOGIN_CARD_LOCAL = '#sp-btn-login-card-local',
            _ID_BTN_LOGIN_CARD_IP = '#sp-btn-login-card-ip',

            _ID_BTN_LOGIN_CARD_ASSOC = '#sp-btn-login-card-assoc',
            _ID_BTN_LOGIN_CARD_ASSOC_CANCEL = '#sp-btn-login-card-assoc-cancel',

            _ID_BTN_LOGIN_TOTP_VERIFY = '#sp-btn-login-totp-verify',
            _ID_BTN_LOGIN_TOTP_VERIFY_CODE_SENT = '#sp-btn-login-totp-verify-code-sent',
            _ID_BTN_LOGIN_TOTP_CANCEL = '#sp-btn-login-totp-cancel',
            _ID_BTN_LOGIN_TOTP_SEND_CODE = '#sp-btn-login-totp-send-code',

            _ID_LOGIN_TOTP_CODE = '#sp-login-user-totp-code',
            _ID_LOGIN_TOTP_CODE_SENT = '#sp-login-user-totp-code-sent',

            _authTOTP,

            _authModeDefault,
            _onAuthModeSelect,
            _modeSelected,
            _authKeyLoggerStartTime = null,
            //
            // The max number of milliseconds allowed for entering the
            // local card number.
            _MAX_CARD_NUMBER_MSECS = 500,
            //
            // The max number of milliseconds allowed for entering the
            // YubiKey OTP.
            _MAX_YUBIKEY_MSECS = 1500,
            //
            // The YubiKey OTP,    or collected local card number from individual
            // keystrokes,  or the cached Card Number to associate with a user.
            _authKeyLoggerCollected,
            _timeoutCardAssoc,
            _countdownCardAssoc,
            //
            // Max number of seconds the assoc card dialog is visible.
            _MAX_CARD_ASSOC_SECS = 30,

            _isCardRegistered;

        _page = new _ns.Page(_i18n, _view, '#page-login', 'Login');
        _self = _ns.derive(_page);

        _pageAbout = new _ns.PageAbout(_i18n, _view);

        /**
         * See Mantis #123 : Asus Transformer does not handle closing of
         * window
         * and back-button correctly: it shows 'page-zero'. So if next page
         * is
         * 'page-zero' we correct and show the login page instead.
         */
        _pageAbout.onHide = function(nextPageId) {
            if (nextPageId === 'page-zero') {
                _self.show();
            }
        };

        _self.loadShow = function(webAppType) {
            var data = {
                webAppType: webAppType
            };
            if (_ns.Utils.hasUrlParam(_ns.URL_PARM.LOGIN_LOCAL)) {
                data[_ns.URL_PARM.LOGIN_LOCAL] = "1";
            }
            _self.loadShowAsync(null, data);
        };

        _self.onLogin = function(foo) {
            _onLogin = foo;
        };
        _self.onShow = function(foo) {
            _onShow = foo;
        };
        _self.onLanguage = function(foo) {
            _onLanguage = foo;
        };

        _self.notifyLogout = function() {
            $.noop();
        };

        /**
         * Sets the authentication mode and adapts the visibility of the dialog.
         */
        _self.setAuthMode = function(authName, authEmail, authId, authYubiKey,
            authCardLocal, authCardIp, modeDefault, authCardPinReq, authCardSelfAssoc,
            yubikeyMaxMsecs, cardLocalMaxMsecs, cardAssocMaxSecs) {
            _authName = authName;
            _authEmail = authEmail;
            _authId = authId;
            _authYubiKey = authYubiKey;
            _authCardLocal = authCardLocal;
            _authCardIp = authCardIp;
            _authModeDefault = modeDefault;
            _authCardPinReq = authCardPinReq;
            _authCardSelfAssoc = authCardSelfAssoc;

            _MAX_CARD_NUMBER_MSECS = cardLocalMaxMsecs;
            _MAX_YUBIKEY_MSECS = yubikeyMaxMsecs;
            _MAX_CARD_ASSOC_SECS = cardAssocMaxSecs;
        };

        /**
         *
         */
        _self.notifyCardIp = function(cardNumber) {
            $('#sp-login-card-ip-number').val(cardNumber);
            $(_ID_BTN_LOGIN_CARD_IP).click();
        };

        /**
         *
         */
        _self.notifyCardAssoc = function(cardNumber) {
            _authKeyLoggerStartTime = new Date().getTime();
            _authKeyLoggerCollected = cardNumber;
            _onAuthModeSelect(_view.AUTH_MODE_CARD_ASSOC);
        };

        /**
         *
         */
        _self.notifyTOTPRequired = function() {
            _onAuthModeSelect(_view.AUTH_MODE_TOTP);
        };

        /**
         *
         */
        _self.notifyLoginFailed = function(modeSelected, msg) {
            _onAuthModeSelect(modeSelected || _authModeDefault);
            // Do NOT use _view.showApiMsg(data), cause it spoils the
            // focus() in case of Card Swipe.
            _view.message(msg);
        };

        _isCardRegistered = function(card) {
            var reg = false,
                res = _api.call({
                    'request': 'card-is-registered',
                    card: card
                });
            if (res.result.code === _ns.ApiResultCodeEnum.OK) {
                reg = res.registered;
            } else {
                _view.showApiMsg(res);
            }
            return reg;
        };

        /**
         *
         */
        _onAuthModeSelect = function(modeSelected) {
            var nMethods = 0;

            // clean all input
            $('#page-login-content :text, :password').val('');

            //
            if (_authTOTP) {
                $(_ID_BTN_LOGIN_TOTP_SEND_CODE).closest('[data-role=collapsible]').collapsible('collapse');
            }

            //
            if (_timeoutCardAssoc) {
                window.clearTimeout(_timeoutCardAssoc);
                _timeoutCardAssoc = null;
            }

            //
            _modeSelected = modeSelected;

            $(_ID_MODE_NAME).hide();
            $(_ID_MODE_EMAIL).hide();
            $(_ID_MODE_ID).hide();
            $(_ID_MODE_YUBIKEY).hide();
            $(_ID_MODE_CARD_LOCAL).hide();
            $(_ID_MODE_CARD_IP).hide();
            $(_ID_MODE_CARD_ASSOC).hide();
            $(_ID_MODE_TOTP).hide();

            $(_ID_BTN_MODE_NAME).hide();
            $(_ID_BTN_MODE_EMAIL).hide();
            $(_ID_BTN_MODE_ID).hide();
            $(_ID_BTN_MODE_YUBIKEY).hide();
            $(_ID_BTN_MODE_CARD_LOCAL).hide();
            $(_ID_BTN_MODE_CARD_IP).hide();

            if (modeSelected === _view.AUTH_MODE_NAME) {

                $(_ID_MODE_NAME).show();
                $(_ID_BTN_MODE_NAME).hide();

                if (_authEmail) {
                    $(_ID_BTN_MODE_EMAIL).show();
                }
                if (_authId) {
                    $(_ID_BTN_MODE_ID).show();
                }
                if (_authYubiKey) {
                    $(_ID_BTN_MODE_YUBIKEY).show();
                }
                if (_authCardLocal) {
                    $(_ID_BTN_MODE_CARD_LOCAL).show();
                }
                if (_authCardIp) {
                    $(_ID_BTN_MODE_CARD_IP).show();
                }

                $('#sp-login-user-name').focus();

            } else if (modeSelected === _view.AUTH_MODE_EMAIL) {

                $(_ID_MODE_EMAIL).show();
                $(_ID_BTN_MODE_EMAIL).hide();

                if (_authId) {
                    $(_ID_BTN_MODE_ID).show();
                }
                if (_authYubiKey) {
                    $(_ID_BTN_MODE_YUBIKEY).show();
                }
                if (_authName) {
                    $(_ID_BTN_MODE_NAME).show();
                }
                if (_authCardLocal) {
                    $(_ID_BTN_MODE_CARD_LOCAL).show();
                }
                if (_authCardIp) {
                    $(_ID_BTN_MODE_CARD_IP).show();
                }

                $('#sp-login-email').focus();

            } else if (modeSelected === _view.AUTH_MODE_ID) {

                $(_ID_MODE_ID).show();
                $(_ID_BTN_MODE_ID).hide();

                if (_authYubiKey) {
                    $(_ID_BTN_MODE_YUBIKEY).show();
                }
                if (_authName) {
                    $(_ID_BTN_MODE_NAME).show();
                }
                if (_authEmail) {
                    $(_ID_BTN_MODE_EMAIL).show();
                }
                if (_authCardLocal) {
                    $(_ID_BTN_MODE_CARD_LOCAL).show();
                }
                if (_authCardIp) {
                    $(_ID_BTN_MODE_CARD_IP).show();
                }

                $('#sp-login-id-number').focus();

            } else if (modeSelected === _view.AUTH_MODE_YUBIKEY) {

                $(_ID_MODE_YUBIKEY).show();
                $(_ID_BTN_MODE_YUBIKEY).hide();

                if (_authName) {
                    $(_ID_BTN_MODE_NAME).show();
                }
                if (_authEmail) {
                    $(_ID_BTN_MODE_EMAIL).show();
                }
                if (_authId) {
                    $(_ID_BTN_MODE_ID).show();
                }
                if (_authCardLocal) {
                    $(_ID_BTN_MODE_CARD_LOCAL).show();
                }
                if (_authCardIp) {
                    $(_ID_BTN_MODE_CARD_IP).show();
                }

                /*
                 * Note: the <div id=""> must have the tabindex="0" attribute
                 * to make it focusable.
                 *
                 * A trick to make the focus() work :-)
                 */
                window.setTimeout(function() {
                    $('#sp-login-yubikey-otp-group').show().focus();
                }, 1);

            } else if (modeSelected === _view.AUTH_MODE_CARD_LOCAL) {

                $(_ID_MODE_CARD_LOCAL).show();
                $(_ID_BTN_MODE_CARD_LOCAL).hide();

                if (_authName) {
                    $(_ID_BTN_MODE_NAME).show();
                }
                if (_authEmail) {
                    $(_ID_BTN_MODE_EMAIL).show();
                }
                if (_authId) {
                    $(_ID_BTN_MODE_ID).show();
                }
                if (_authYubiKey) {
                    $(_ID_BTN_MODE_YUBIKEY).show();
                }
                if (_authCardIp) {
                    $(_ID_BTN_MODE_CARD_IP).show();
                }

                $('#sp-login-card-local-pin-group').hide();

                /*
                 * Note: the <div id=""> must have the tabindex="0" attribute
                 * to make it focusable.
                 */

                /*
                 * A trick to make the focus() work :-)
                 */
                window.setTimeout(function() {
                    $('#sp-login-card-local-number-group').show().focus();
                }, 1);

            } else if (modeSelected === _view.AUTH_MODE_CARD_IP) {
                $(_ID_MODE_CARD_IP).show();

                if (_authName) {
                    $(_ID_BTN_MODE_NAME).show();
                }
                if (_authEmail) {
                    $(_ID_BTN_MODE_EMAIL).show();
                }
                if (_authId) {
                    $(_ID_BTN_MODE_ID).show();
                }
                if (_authYubiKey) {
                    $(_ID_BTN_MODE_YUBIKEY).show();
                }
                if (_authCardLocal) {
                    $(_ID_BTN_MODE_CARD_LOCAL).show();
                }
                $('#sp-login-card-ip-pin-group').hide();
                $('#sp-login-card-ip-number-group').show();
            }

            if (modeSelected === _view.AUTH_MODE_TOTP) {

                $('.sp-login-dialog').hide();
                $(_ID_MODE_TOTP).show();
                $(_ID_LOGIN_TOTP_CODE).focus();

            } else if (modeSelected === _view.AUTH_MODE_CARD_ASSOC) {

                $('.sp-login-dialog').hide();
                $('.sp-login-dialog-assoc').show();

                $(_ID_MODE_CARD_ASSOC).show();
                $('#sp-login-user-name-assoc').focus();

                $('#sp-login-card-assoc-countdown').text(_MAX_CARD_ASSOC_SECS);

                _countdownCardAssoc = _MAX_CARD_ASSOC_SECS - 1;
                _timeoutCardAssoc = window.setInterval(function() {
                    $('#sp-login-card-assoc-countdown').text(_countdownCardAssoc);
                    if (_countdownCardAssoc-- === 0) {
                        $(_ID_BTN_LOGIN_CARD_ASSOC_CANCEL).click();
                    }
                }, 1000);

            } else {
                _authKeyLoggerCollected = null;
                $('.sp-login-dialog').show();
                $('.sp-login-dialog-assoc').hide();
            }

            if (_authName) {
                nMethods++;
            }
            if (_authEmail) {
                nMethods++;
            }
            if (_authId) {
                nMethods++;
            }
            if (_authYubiKey) {
                nMethods++;
            }
            if (_authCardLocal) {
                nMethods++;
            }
            if (_authCardIp) {
                nMethods++;
            }

            if ($('.sp-btn-login-mode-oauth').length === 0 && nMethods < 2) {
                $('#sp-login-modes').hide();
            }

        };

        $(_self.id()).on('pagecreate', function(event) {
            // ----------------------------------------------------------------
            // PROBLEM: IE8 executes HTTP GET request on form submission even
            // though we capture key code 13.
            // SOLUTION: create <form method="post" data-ajax="false">
            // ----------------------------------------------------------------
            $(this).keyup(function(e) {
                var key = e.keyCode || e.which,
                    selClick;
                // 13 = <Enter>, 27 = <ESC>
                if (key === 13) {
                    if (_modeSelected === _view.AUTH_MODE_NAME) {
                        selClick = $(_ID_BTN_LOGIN_NAME);
                    } else if (_modeSelected === _view.AUTH_MODE_EMAIL) {
                        selClick = $(_ID_BTN_LOGIN_EMAIL);
                    } else if (_modeSelected === _view.AUTH_MODE_ID) {
                        selClick = $(_ID_BTN_LOGIN_ID);
                    } else if (_modeSelected === _view.AUTH_MODE_CARD_LOCAL) {
                        $('#sp-login-card-local-number').val(_authKeyLoggerCollected);
                        selClick = $(_ID_BTN_LOGIN_CARD_LOCAL);
                    } else if (_modeSelected === _view.AUTH_MODE_YUBIKEY) {
                        $('#sp-login-yubikey').val(_authKeyLoggerCollected);
                        selClick = $(_ID_BTN_LOGIN_YUBIKEY);
                    } else if (_modeSelected === _view.AUTH_MODE_CARD_IP && $('#sp-login-card-ip-number').val().length > 0) {
                        selClick = $(_ID_BTN_LOGIN_CARD_IP);
                    } else if (_modeSelected === _view.AUTH_MODE_CARD_ASSOC) {
                        selClick = $(_ID_BTN_LOGIN_CARD_ASSOC);
                    } else if (_modeSelected === _view.AUTH_MODE_TOTP) {
                        if ($(_ID_LOGIN_TOTP_CODE).is(':focus')) {
                            selClick = $(_ID_BTN_LOGIN_TOTP_VERIFY);
                        } else if ($(_ID_LOGIN_TOTP_CODE_SENT).is(':focus')) {
                            selClick = $(_ID_BTN_LOGIN_TOTP_VERIFY_CODE_SENT);
                        }
                    }
                    // Mantis #735
                    if (selClick && !selClick.is(':focus')) {
                        selClick.click();
                    }
                } else if (key === 27) {
                    if (_modeSelected === _view.AUTH_MODE_CARD_ASSOC) {
                        $(_ID_BTN_LOGIN_CARD_ASSOC_CANCEL).click();
                    } else if (_modeSelected === _view.AUTH_MODE_TOTP) {
                        $(_ID_BTN_LOGIN_TOTP_CANCEL).click();
                    }
                } else if ((_modeSelected === _view.AUTH_MODE_CARD_LOCAL && $('#sp-login-card-local-number').val().length === 0) || (_modeSelected === _view.AUTH_MODE_YUBIKEY && $('#sp-login-yubikey').val().length === 0)) {
                    /*
                     * IMPORTANT: only look at printable chars. When doing an
                     * alt-tab
                     * to return to THIS application we do not want to
                     * collect !!!
                     */
                    if (32 < key && key < 127) {
                        if (_authKeyLoggerStartTime === null) {
                            _authKeyLoggerStartTime = new Date().getTime();
                            _authKeyLoggerCollected = '';
                        }
                        _authKeyLoggerCollected += String.fromCharCode(key);
                    }
                }
            });

            $('#button-app-about').click(function() {
                _pageAbout.load().show();
                return false;
            });

            $('#button-app-language').click(function() {
                _onLanguage();
                return false;
            });

            $('.sp-btn-login-mode-oauth').click(function() {
                var res = _api.call({
                    request: "oauth-url",
                    dto: JSON.stringify({
                        provider: $(this).attr('data-savapage'),
                        instanceId: $(this).attr('data-savapage-type')
                    })
                });

                if (res.result.code === _ns.ApiResultCodeEnum.OK) {
                    window.location.assign(res.dto.url);
                } else {
                    _view.showApiMsg(res);
                }
            });

            $('#sp-login-card-local-number-group').focusin(function() {
                _authKeyLoggerStartTime = null;
                $('#sp-login-card-local-focusin').show();
                $('#sp-login-card-local-focusout').hide();
            });

            $('#sp-login-card-local-number-group').focusout(function() {
                _authKeyLoggerStartTime = null;
                $('#sp-login-card-local-focusin').hide();
                // Use the fadeIn to prevent a 'flash' effect when just
                // anotherfocus is lost because another auth method is
                // selected.
                $('#sp-login-card-local-focusout').fadeIn(700);
            });

            $('#sp-login-yubikey-otp-group').focusin(function() {
                _authKeyLoggerStartTime = null;
                $('#sp-login-yubikey-focusin').show();
                $('#sp-login-yubikey-focusout').hide();
            });

            $('#sp-login-yubikey-otp-group').focusout(function() {
                _authKeyLoggerStartTime = null;
                $('#sp-login-yubikey-focusin').hide();
                // Use the fadeIn to prevent a 'flash' effect when just
                // anotherfocus is lost because another auth method is
                // selected.
                $('#sp-login-yubikey-focusout').fadeIn(700);
            });

            if (_authName) {

                $(_ID_BTN_MODE_NAME).click(function() {
                    _onAuthModeSelect(_view.AUTH_MODE_NAME);
                    return false;
                });

                $(_ID_BTN_LOGIN_NAME).click(function() {
                    var sel = $('#sp-login-user-password'),
                        pw = sel.val();
                    sel.val('');
                    _onLogin(_view.AUTH_MODE_NAME, $('#sp-login-user-name').val(), pw);
                    return false;
                });

                $("#sp-login-user-name").focus(function() {
                    // Select input field contents
                    $(this).select();
                });
            }

            if (_authEmail) {

                $(_ID_BTN_MODE_EMAIL).click(function() {
                    _onAuthModeSelect(_view.AUTH_MODE_EMAIL);
                    return false;
                });

                $(_ID_BTN_LOGIN_EMAIL).click(function() {
                    var sel = $('#sp-login-email-password'),
                        pw = sel.val();
                    sel.val('');
                    _onLogin(_view.AUTH_MODE_EMAIL, $('#sp-login-email').val(), pw);
                    return false;
                });

                $("#sp-login-email").focus(function() {
                    // Select input field contents
                    $(this).select();
                });
            }

            if (_authId) {

                $(_ID_BTN_MODE_ID).click(function() {
                    _onAuthModeSelect(_view.AUTH_MODE_ID);
                    return false;
                });

                $(_ID_BTN_LOGIN_ID).click(function() {
                    var selNum = $('#sp-login-id-number'),
                        num = selNum.val(),
                        selPin = $('#sp-login-id-pin'),
                        pin = selPin.val();

                    selNum.val('');
                    selPin.val('');

                    _onLogin(_view.AUTH_MODE_ID, num, pin);
                    return false;
                });
            }

            if (_authYubiKey) {

                $(_ID_BTN_MODE_YUBIKEY).click(function() {
                    _onAuthModeSelect(_view.AUTH_MODE_YUBIKEY);
                    return false;
                });

                $(_ID_BTN_LOGIN_YUBIKEY).click(function() {
                    var selCard = $('#sp-login-yubikey'),
                        card = selCard.val()
                        // Elapsed time since the first keyup in the key logger.
                        ,
                        authKeyLoggerElapsed
                        //
                        ;

                    if (card.length === 0) {
                        $('#sp-login-yubikey-otp-group').focus();
                        return false;
                    }

                    if (_authKeyLoggerStartTime) {
                        authKeyLoggerElapsed = new Date().getTime() - _authKeyLoggerStartTime;
                        _authKeyLoggerStartTime = null;
                        if (authKeyLoggerElapsed > _MAX_YUBIKEY_MSECS) {
                            selCard.val('');
                            return false;
                        }
                    }

                    selCard.val('');

                    _onLogin(_view.AUTH_MODE_YUBIKEY, card);

                    return false;
                });

            }

            if (_authCardLocal) {

                $(_ID_BTN_MODE_CARD_LOCAL).click(function() {
                    _onAuthModeSelect(_view.AUTH_MODE_CARD_LOCAL);
                    return false;
                });

                $(_ID_BTN_LOGIN_CARD_LOCAL).click(function() {
                    var selCard = $('#sp-login-card-local-number'),
                        card = selCard.val(),
                        selPin = $('#sp-login-card-local-pin'),
                        pin = selPin.val(),
                        // Elapsed time since the first keyup in the key logger.
                        authKeyLoggerElapsed;

                    if (card.length === 0) {
                        $('#sp-login-card-local-number-group').focus();
                        return false;
                    }

                    if (_authKeyLoggerStartTime) {
                        authKeyLoggerElapsed = new Date().getTime() - _authKeyLoggerStartTime;
                        _authKeyLoggerStartTime = null;
                        if (authKeyLoggerElapsed > _MAX_CARD_NUMBER_MSECS) {
                            selCard.val('');
                            return false;
                        }
                    }

                    if (_authCardPinReq && pin.length === 0) {
                        /*
                         * Check if card is registered (if card sef assoc is
                         * active).
                         */
                        $('#sp-login-card-local-number-group').hide();

                        if (!_authCardSelfAssoc || _isCardRegistered(card)) {
                            $('#sp-login-card-local-pin-group').show();
                            $('#sp-login-card-local-pin').focus();
                            return false;
                        }
                    }

                    selCard.val('');
                    selPin.val('');

                    _onLogin(_view.AUTH_MODE_CARD_LOCAL, card, pin);

                    return false;
                });

            }

            if (_authCardIp) {
                $(_ID_BTN_MODE_CARD_IP).click(function() {
                    _onAuthModeSelect(_view.AUTH_MODE_CARD_IP);
                    return false;
                });

                $(_ID_BTN_LOGIN_CARD_IP).click(function() {
                    var
                        // hidden field
                        selCard = $('#sp-login-card-ip-number'),
                        //
                        card = selCard.val(),
                        selPin = $('#sp-login-card-ip-pin'),
                        pin = selPin.val();

                    if (_authCardPinReq && pin.length === 0) {
                        /*
                         * Check if card is registered (if card sef assoc is
                         * active).
                         */
                        $('#sp-login-card-ip-number-group').hide();

                        if (!_authCardSelfAssoc || _isCardRegistered(card)) {
                            $('#sp-login-card-ip-pin-group').show();
                            $('#sp-login-card-ip-pin').focus();
                            return false;
                        }
                    }

                    selCard.val('');
                    selPin.val('');

                    _onLogin(_view.AUTH_MODE_CARD_IP, card, pin);

                    return false;
                });

            }

            if (_authCardSelfAssoc) {
                $(_ID_BTN_LOGIN_CARD_ASSOC).click(function() {
                    _onLogin(_view.AUTH_MODE_NAME, $('#sp-login-user-name-assoc').val(), $('#sp-login-user-password-assoc').val(), _authKeyLoggerCollected);
                    return false;
                });
                $(_ID_BTN_LOGIN_CARD_ASSOC_CANCEL).click(function() {
                    _onAuthModeSelect(_authModeDefault);
                    _self.onCardAssocCancel();
                    return false;
                });
            }

            _authTOTP = $(_ID_MODE_TOTP).length > 0;

            if (_authTOTP) {
                $(_ID_BTN_LOGIN_TOTP_VERIFY).click(function() {
                    _onLogin(null, $(_ID_LOGIN_TOTP_CODE).val());
                    return false;
                });
                $(_ID_BTN_LOGIN_TOTP_VERIFY_CODE_SENT).click(function() {
                    _onLogin(null, null, $(_ID_LOGIN_TOTP_CODE_SENT).val());
                    return false;
                });
                $(_ID_BTN_LOGIN_TOTP_CANCEL).click(function() {
                    _onAuthModeSelect(_authModeDefault);
                    return false;
                });
                $(_ID_BTN_LOGIN_TOTP_SEND_CODE).click(function() {
                    _view.showApiMsg(_api.call({
                        request: 'user-totp-send-recovery-code'
                    }));
                    return false;
                });
            }

        }).on('pagebeforehide', function(event, ui) {
            /*
             * JQM 1.3.1: A correction for Android and iOS.
             */
            if (ui.nextPage.attr('id') === 'page-zero') {
                _self.show();
            }

        }).on('pagebeforeshow', function(event, ui) {

            _onAuthModeSelect(_modeSelected || _authModeDefault);
            _onShow(event, ui);

            // Pick up URL parameters
            $('#sp-login-user-name').val(_util.getUrlParam(_ns.URL_PARM.USER));
            $('#sp-login-user-password').val('');
        });
        /*
         * IMPORTANT
         */
        return _self;
    };

    /**
     * Constructor
     * Note: _api is used for 'jqplot' requests.
     */
    _ns.View = function(_i18n, _api) {
        var _view = this,
            _onDisconnected = null;

        /*
         * AUTH Modes used for login. See UserAuthModeEnum in Java code.
         */
        this.AUTH_MODE_NAME = 'name';
        this.AUTH_MODE_EMAIL = 'email';
        this.AUTH_MODE_ID = 'id';
        this.AUTH_MODE_CARD_LOCAL = 'nfc-local';
        this.AUTH_MODE_CARD_IP = 'nfc-network';
        this.AUTH_MODE_YUBIKEY = 'yubikey';
        this.AUTH_MODE_OAUTH = 'oauth';

        // Dummy AUTH Mode to associate Card with user.
        this.AUTH_MODE_CARD_ASSOC = '_CA';

        // Dummy AUTH Mode to enter TOTP.
        this.AUTH_MODE_TOTP = '_TOTP';

        // HTML color: to be set from 'constants' API.
        this.userChartColors = ['printIn', 'printOut', 'pdfOut'];

        this.imgBase64 = false;

        this.onDisconnected = function(foo) {
            _onDisconnected = foo;
        };

        /**
         *
         */
        this.isPageActive = function(pageId) {
            return (_view.activePage().attr('id') === pageId);
        };

        /**
         * Gets the <img> src attribute value from the url.
         */
        this.getImgSrc = function(url) {
            if (this.imgBase64) {
                return 'data:image/png;base64,' + $.ajax({
                    type: "POST",
                    async: false,
                    dataType: "text",
                    url: url,
                    data: {}
                }).responseText;
            }
            return url;
        };

        /**
         *
         */
        this.showXyChart = function(id, xydata) {

            return $.jqplot(id, xydata.dataSeries, {
                textColor: "#ff0000",
                grid: {
                    shadow: false,
                    borderWidth: 1.0
                },
                seriesColors: this.userChartColors,
                axes: {
                    xaxis: {
                        renderer: $.jqplot.DateAxisRenderer,
                        tickOptions: {
                            formatString: this.jqPlotMonthDayFormat()
                        },
                        tickInterval: '1 week'
                    },
                    yaxis: {
                        renderer: $.jqplot.LogAxisRenderer
                    }
                },
                title: {
                    //text : xydata.optionObj.title.text,
                    show: false
                },
                highlighter: {
                    show: true,
                    sizeAdjust: 7.5
                }
            });
        };

        /**
         *
         */
        this.showPieChart = function(id, piedata) {

            return $.jqplot(id, piedata.dataSeries, {
                seriesDefaults: {
                    renderer: $.jqplot.PieRenderer,
                    rendererOptions: {
                        dataLabels: 'value',
                        // Put data labels on the pie slices.
                        // By default, labels show the percentage of the
                        // slice.
                        showDataLabels: true,
                        // Turn off filling of slices.
                        fill: false,
                        // Add a margin to separate the slices.
                        sliceMargin: 4,
                        // stroke the slices with a little thicker line.
                        lineWidth: 5
                    }
                },
                seriesColors: this.userChartColors,
                grid: {
                    shadow: false,
                    borderWidth: 1.0
                },
                legend: {
                    show: true,
                    location: 'se'
                },
                title: {
                    //text : piedata.optionObj.title.text,
                    show: false
                },
                highlighter: {
                    show: true,
                    useAxesFormatters: false,
                    tooltipFormatString: '%s'
                }
            });

        };

        /**
         *
         */
        this.jqPlotData = function(chartType, isGlobal) {
            var res = _api.call({
                request: 'jqplot',
                chartType: chartType,
                isGlobal: isGlobal
            });
            _view.showApiMsg(res);
            return res.chartData;
        };

        /**
         * Use the current locale to get the jqPlot month/day format.
         */
        this.jqPlotMonthDayFormat = function() {
            return $.jsDate.regional[$.jsDate.config.defaultLocale].savapageDayMonthFormat;
        };

        /**
         *
         */
        this.showExpiredDialog = function() {
            $('#page-zero').show();
            _view.changePage('#page-zero', {
                changeHash: false
            });
        };

        /**
         *
         */
        this.initI18n = function(language) {
            this.language = language;
        };

        /**
         * Create temp <div> to determine overflow scrollbar width.
         * */
        this.getOverflowScrollBarWidth = function() {
            var w1,
                w2,
                div = $('<div style="width:50px;height:50px;overflow:hidden;position:absolute;top:-200px;left:-200px;"><div style="height:100px;"></div></div>');
            $('body').append(div);
            w1 = $('div', div).innerWidth();
            div.css('overflow-y', 'auto');
            w2 = $('div', div).innerWidth();
            $(div).remove();
            return (w1 - w2);
        };

        /**
         * http://css-tricks.com/snippets/javascript/viewport-size-screen-resolution-mouse-postition/
         */
        this.getViewportHeight = function() {
            if (window.innerHeight) {
                return window.innerHeight;
            }
            if (document.body && document.body.offsetHeight) {
                return document.body.offsetHeight;
            }
            return 0;
        };

        /**
         * http://css-tricks.com/snippets/javascript/viewport-size-screen-resolution-mouse-postition/
         */
        this.getViewportWidth = function() {
            if (window.innerWidth) {
                return window.innerWidth;
            }
            if (document.body && document.body.offsetWidth) {
                return document.body.offsetWidth;
            }
            return 0;
        };

        /**
         * Loads a page.
         *
         * NOTE: '/pages/user' is a Wicket mountPackage() construct.
         *
         * @param pageSelector
         * @param page
         *            The Wicket class
         * @return true if page was loaded ok.
         */
        this.loadPage = function(pageSelector, page) {
            var html;
            if ($(pageSelector).children().length === 0) {
                html = this.getPageHtml(page);
                if (html) {
                    $(pageSelector).html(html).page();
                    return true;
                }
                return false;
            }
            return true;
        };

        /**
         * Synchronous .ajax call to get HTML
         * @param page
         *            The Wicket class.
         * @param data Object with data.
         */
        this.getPageHtml = function(page, data) {
            var html,
                url = '/pages/' + page + _ns.WebAppTypeUrlParm();

            _ns.logger.debug(url);

            // Since this is a synchronous call, this does NOT show in
            // Chrome browser (at the moment of the call).
            $.mobile.loading("show");

            html = $.ajax({
                type: "POST",
                async: false,
                url: url,
                dataType: 'json',
                data: data || {}
            }).responseText;

            $.mobile.loading("hide");

            if (html) {
                return html;
            }

            if (_onDisconnected) {
                _onDisconnected();
            }
            return false;
        };

        /**
         * Loads a User page.
         * @param page
         *            The Wicket class
         */
        this.loadUserPage = function(sel, page) {
            return this.loadPage(sel, 'user/' + page);
        };

        /**
         * Loads an Admin page.
         * @param page
         *            The Wicket class
         */
        this.loadAdminPage = function(sel, page) {
            return this.loadPage(sel, 'admin/' + page);
        };

        /**
         * Gets HTML of a User page (synchronous).
         * @param page
         *            The Wicket class
         */
        this.getUserPageHtml = function(page, data) {
            return this.getPageHtml('user/' + page, data);
        };

        /**
         * Gets HTML of an Admin page (synchronous).
         * @param page
         *            The Wicket class
         */
        this.getAdminPageHtml = function(page, data) {
            return this.getPageHtml('admin/' + page, data);
        };

        /** */
        this.scrollToAnchor = function(aid) {
            var aTag = $("a[name='" + aid + "']");
            $('html,body').animate({
                scrollTop: aTag.offset().top
            }, 'slow');
        };

        /**
         * Show (load and change to) a page.
         * @param page
         *            The Wicket class
         */
        this.showPage = function(sel, page) {
            if (this.loadPage(sel, page)) {
                _view.changePage($(sel));
            }
        };
        /**
         * Show (load and change to) a User page.
         * @param page
         *            The Wicket class
         */
        this.showUserPage = function(sel, page) {
            this.showPage(sel, 'user/' + page);
        };

        this.showUserPageAsync = function(sel, page, onDone) {
            this.showPageAsync(sel, 'user/' + page, onDone);
        };

        this.showPageAsync = function(sel, page, onDone, jsonData) {
            var url;

            if ($(sel).children().length === 0) {

                url = '/pages/' + page + _ns.WebAppTypeUrlParm();

                _ns.logger.debug(url);

                $.mobile.loading("show");

                $.ajax({
                    type: "POST",
                    async: true,
                    dataType: 'html',
                    data: jsonData || {},
                    url: url
                }).done(function(html) {
                    $(sel).html(html).page();
                    _view.changePage($(sel));
                    if (onDone) {
                        onDone();
                    }
                }).fail(function() {
                    if (_onDisconnected) {
                        _onDisconnected();
                    }
                }).always(function() {
                    $.mobile.loading("hide");
                });
            } else {
                _view.changePage($(sel));
                if (onDone) {
                    onDone();
                }
            }
        };

        /**
         * Inspired on:
         * http://mentaljetsam.wordpress.com/2011/05/10/pop-up-message-in-jquery-mobile/
         */
        this.message = function(msg) {

            $("<div class='ui-overlay-shadow ui-body-b ui-corner-all' style='max-width:600px'>" + "<center><p style='padding: 10px;'>" + msg + "<p></center></div>").css({
                "z-index": 5000,
                display: "block",
                position: "relative",
                opacity: 0.96,
                top: $(window).scrollTop() + 100,
                // 'margin-top' : '100px',
                'margin-left': 'auto',
                'margin-right': 'auto',
                // width : "400px"
                width: $(window).width() * 0.8
            }).prependTo($('body')).delay(2500).fadeOut(1000, function() {
                $(this).remove();
            });
        };

        this.apiResMsgTitle = function(result) {
            var title;
            if (result.code === _ns.ApiResultCodeEnum.INFO) {
                title = _i18n.string('title-info');
            } else if (result.code === _ns.ApiResultCodeEnum.WARN || result.code === _ns.ApiResultCodeEnum.UNAVAILABLE) {
                title = _i18n.string('title-warning');
            } else {
                title = _i18n.string('title-error');
            }
            return title;
        };

        this.apiResMsgCssClass = function(result) {
            var klas;
            if (result.code === _ns.ApiResultCodeEnum.INFO) {
                klas = 'sp-msg-popup-info';
            } else if (result.code === _ns.ApiResultCodeEnum.WARN || result.code === _ns.ApiResultCodeEnum.UNAVAILABLE) {
                klas = 'sp-msg-popup-warn';
            } else {
                klas = 'sp-msg-popup-error';
            }
            return klas;
        };

        this.apiResMsg = function(result) {
            var title,
                cssClass,
                txt,
                sel,
                html;

            if (result.code === _ns.ApiResultCodeEnum.UNAUTH) {
                /*
                 * Do NOT show a session expiration error, since this will be
                 * handled in the onExpire() callback.
                 */
                return;
            }

            if (result.code === _ns.ApiResultCodeEnum.OK) {
                if (result.txt) {
                    this.message(result.txt);
                }
                return;
            }

            title = this.apiResMsgTitle(result);
            cssClass = this.apiResMsgCssClass(result);

            txt = result.txt;

            if (!txt) {
                txt = result.msg;
            }

            sel = _view.activePage().find('.sp-msg-popup');

            // remove CR/LF, CR and replace all multiple spaces with one
            // single space
            txt = txt ? txt.replace(/\r?\n|\r/g, "").replace(/ +(?= )/g, '') : '';

            html = '<a href="#" data-rel="back" class="ui-btn ui-corner-all ui-shadow ui-btn-a ui-icon-delete ui-btn-icon-notext ui-btn-right">Close</a>';
            html += '<h3 class="ui-title ' + cssClass + '">' + title + '</h3>';
            html += '<p class="sp-txt-wrap ' + cssClass + '">' + txt + '</p>';
            html += '<div data-iconpos="none" data-mini="true" data-role="collapsible" data-collapsed="true">';
            html += '<h3 style="width: 60px;">&nbsp;&middot;&nbsp;&middot;&nbsp;&middot;</h3>';
            html += '<textarea onclick="this.focus();this.select()" readonly="readonly" autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false">' + txt + '</textarea>';
            html += '</div>';

            sel.html(html);
            sel.enhanceWithin().popup("open");
        };

        this.msgDialogBox = function(txt, cssClass) {
            var sel = _view.activePage().find('.sp-msg-popup'),
                html;

            html = '<a href="#" data-rel="back" class="ui-btn ui-corner-all ui-shadow ui-btn-a ui-icon-delete ui-btn-icon-notext ui-btn-right">Close</a>';
            html += '<p class="sp-txt-wrap ' + cssClass + '">' + txt + '</p>';

            sel.html(html);
            sel.enhanceWithin().popup("open");
        };

        /**
         *
         */
        this.isLoginPageActive = function() {
            return this.activePage().attr('id') === 'page-login';
        };

        this.asyncFocus = function(sel) {
            _ns.Utils.asyncFoo(function(sel) {
                sel.focus();
            }, sel);
        };
        /**
         * @since JQM 1.4.0
         */
        this.activePage = function() {
            return $('body').pagecontainer('getActivePage');
        };
        /**
         * @since JQM 1.4.0
         */
        this.changePage = function(sel, options) {
            $('body').pagecontainer('change', sel, options);
        };

        /**
         * (Un)checks a checkbox button.
         */
        this.checkCb = function(sel, isChecked) {
            $(sel).prop("checked", isChecked).checkboxradio("refresh");
        };

        /**
         * (Un)checks a checkbox button.
         */
        this.checkCbSel = function(sel, isChecked) {
            sel.prop("checked", isChecked).checkboxradio("refresh");
        };

        /**
         * Toggles a checkbox button.
         */
        this.toggleCb = function(sel) {
            this.checkCb(sel, !this.isCbChecked(sel));
        };

        /**
         * Returns boolean.
         */
        this.isCbDisabled = function(sel) {
            return sel.checkboxradio("option", "disabled");
        };

        /**
         * Returns boolean.
         */
        this.isFlipswitchDisabled = function(sel) {
            return sel.flipswitch("option", "disabled");
        };

        /**
         * (Un)checks a flipswitch.
         */
        this.checkFlipswitchSel = function(sel, isOn) {
            sel.prop("checked", isOn).flipswitch("refresh");
        };

        /**
         * Returns boolean.
         */
        this.isCbChecked = function(sel) {
            return sel.is(':checked');
        };

        /**
         * Returns 'Y' | 'N'.
         */
        this.isCheckedYN = function(sel) {
            return sel.is(':checked') ? 'Y' : 'N';
        };

        /**
         * Get array with values of :checked children of input selector.
         */
        this.checkedValues = function(sel) {
            var values = [],
                i = 0;
            $.each(sel.find(':checked'), function() {
                values[i++] = $(this).val();
            });
            return values;
        };

        /**
         * Checks a radiobutton.
         */
        this.checkRadio = function(name, id) {
            var sel = $('input[name="' + name + '"]');
            sel.prop('checked', false);
            $('#' + id).prop('checked', true);
            sel.checkboxradio("refresh");
        };

        /**
         * Checks first a radio button.
         */
        this.checkRadioFirst = function(name) {
            var sel = $('input[name="' + name + '"]');
            sel.prop('checked', false).first().prop('checked', true);
            sel.checkboxradio("refresh");
        };

        /**
         * Unchecks a radiobutton.
         */
        this.uncheckRadioValue = function(name) {
            var radio = 'input[name="' + name + '"]';
            $(radio).prop('checked', false);
            $(radio).checkboxradio("refresh");
        };

        /**
         * Checks a radiobutton value.
         */
        this.checkRadioValue = function(name, value) {
            var radio = 'input[name="' + name + '"]',
                sel = $(radio + '[value="' + value + '"]');
            $(radio).prop('checked', false);
            sel.prop('checked', true);
            $(radio).checkboxradio("refresh");
        };

        this.getRadioSelected = function(name) {
            return $("input:radio[name='" + name + "']:checked");
        };

        this.getRadioValue = function(name) {
            return $("input:radio[name='" + name + "']:checked").val();
        };

        this.isRadioIdSelected = function(name, id) {
            return ($("input:radio[name='" + name + "']:checked").attr('id') === id);
        };

        /** Return sel if present, or null if not. */
        this.getSelPresent = function(sel) {
            return sel.length > 0 ? sel : null;
        }
        /**
         * Set JQM selectmenu value.
         */
        this.setSelectedValue = function(sel, val) {
            sel.val(val).selectmenu('refresh');
        };

        /**
         * Select first option.
         */
        this.setSelectedFirst = function(sel) {
            this.setSelectedValue(sel, sel.find("option:first").val());
        };

        /**
         * Get array with JQM selected values from (multiple) "select"
         * element.
         */
        this.selectedValues = function(id) {
            var values = [],
                i = 0;
            $.each($("select[id='" + id + "']").find(':selected'), function() {
                values[i++] = $(this).val();
            });
            return values;
        };

        this.countSelectedValues = function(id) {
            var val = this.selectedValues(id);
            return val ? val.length : 0;
        };

        this.showApiMsg = function(res) {
            if (res && res.result) {
                this.apiResMsg(res.result);
            }
        };

        this.mobipick = function(sel, localeDate) {
            return sel.mobipick({
                locale: _view.language,
                intlStdDate: !localeDate
            });
        };

        this.mobipickResetDate = function(sel) {
            this.mobipickSetDate(sel, null);
        };

        this.mobipickSetDate = function(sel, milliseconds) {
            sel.mobipick("option", "date", milliseconds ? new Date(milliseconds) : null).mobipick("updateDateInput");
        };

        this.mobipickGetDate = function(sel) {
            return sel.mobipick("option", "date");
        };

        /**
         * Show/hides jQuery selector.
         */
        this.visible = function(jqsel, show) {
            if (show) {
                jqsel.show();
            } else {
                jqsel.hide();
            }
            return jqsel;
        };

        this.visibleCheckboxRadio = function(jqsel, show) {
            this.visible(jqsel.parent(), show);
        };

        /**
         * A workaround method to fix incomplete rendering of background image 
         * by performing a dummy windows resize.
         */
        this.repairVisibility = function() {
            $(window).trigger('resize');
        };

        /**
         * Enables/Disables jQuery selector (does not work for radio
         * buttons).
         */
        this.enable = function(jqsel, enable) {
            var attr = "disabled";
            if (enable) {
                jqsel.removeAttr(attr);
            } else {
                jqsel.attr(attr, "");
            }
        };

        /**
         * Enables/Disables jQuery selector with ui-disabled (does not
         * work for radio buttons).
         */
        this.enableUI = function(jqsel, enable) {
            var clazz = "ui-disabled";
            if (enable) {
                jqsel.removeClass(clazz).attr('tabindex', '0');
            } else {
                jqsel.addClass(clazz).attr('tabindex', '-1');
            }
        };

        this.enableCheckboxRadio = function(jqsel, enable) {
            jqsel.checkboxradio(enable ? 'enable' : 'disable');
        };

        this.enableFlipswitch = function(jqsel, enable) {
            jqsel.flipswitch(enable ? 'enable' : 'disable');
        };

        /**
         *
         */
        this.checkPwMatch = function(jqPassw, jqConfirm, canBeVoid) {
            var pw_valid = false,
                pw1 = jqPassw.val(),
                pw2 = jqConfirm.val(),
                msg;

            if (pw1 !== pw2) {
                msg = 'msg-input-mismatch';
            } else if (!pw1 && !canBeVoid) {
                msg = 'msg-input-empty';
            } else {
                pw_valid = true;
            }
            if (msg) {
                this.apiResMsg({
                    result: '3',
                    txt: _i18n.format(msg)
                });
            }

            return pw_valid;
        };

    };
}(jQuery, this, this.document, this.org.savapage));

//--------------------------------------------------------------
// Logger for Keyboard Emulator Cards/Keys (NFC, Barcode, Yubikey)
//--------------------------------------------------------------
(function(window, document, navigator, _ns) {
    "use strict";

    _ns.KeyboardLogger = {

        /** */
        setCallback: function(keyzone, maxMsecs, fooFocusIn, fooFocusOut, fooInfo) {
            var _obj = this,
                _hasFocus = false,
                _collectStartTime = null,
                _collectedKeys = '';

            keyzone.focusin(function() {
                _collectStartTime = null;
                _hasFocus = true;
                fooFocusIn();
            }).focusout(function() {
                _collectStartTime = null;
                _hasFocus = false;
                fooFocusOut();
            }).keyup(function(e) {
                var key,
                    collectTimeElapsed;
                if (!_hasFocus) {
                    return;
                }
                key = e.keyCode || e.which;
                if (key === 13) {// <Enter>
                    if (_collectStartTime) {
                        collectTimeElapsed = new Date().getTime() - _collectStartTime;
                        _collectStartTime = null;
                        if (collectTimeElapsed > maxMsecs) {
                            return false;
                        }
                    }
                    fooInfo(_collectedKeys);
                    _collectedKeys = '';
                } else {
                    /*
                     * IMPORTANT: only look at printable chars. When doing an
                     * alt-tab to return to THIS application we do not want
                     * to collect !!!
                     */
                    if (32 < key && key < 127) {
                        if (_collectStartTime === null) {
                            _collectStartTime = new Date().getTime();
                            _collectedKeys = '';
                        }
                        _collectedKeys += String.fromCharCode(key);
                    }
                }
            });
        }
    };
}(this, this.document, this.navigator, this.org.savapage));

//--------------------------------------------------------------
// Drop & Drop File Upload
//--------------------------------------------------------------
(function(window, document, navigator, _ns) {
    "use strict";

    _ns.DropZone = {

        /**
         * Format warning as HTML.
         */
        getHtmlWarning: function(i18n, warn, files, filesStatus) {
            var i,
                pfx,
                nameWlk,
                statWlk,
                htmlWlk,
                fileSize,
                htmlAccepted = '',
                htmlRejected = '';

            if (files && filesStatus) {
                for (i = 0; i < files.length; i++) {
                    nameWlk = files[i].name;
                    statWlk = filesStatus[nameWlk];
                    if (statWlk !== undefined) {
                        fileSize = this.humanFileSize(files[i].size);
                        htmlWlk = '<span class="sp-txt-' + (statWlk ? 'valid' : 'warn') + '">';
                        htmlWlk += '&bull; ' + nameWlk;
                        if (fileSize) {
                            htmlWlk += ' (' + fileSize + ')';
                        }
                        htmlWlk += '</span><br>';
                        if (statWlk) {
                            htmlAccepted += htmlWlk;
                        } else {
                            htmlRejected += htmlWlk;
                        }
                    }
                }
            }
            htmlWlk = htmlAccepted;
            if (htmlWlk.length > 0) {
                htmlWlk += '<span class="sp-txt-valid">&bull; ' + i18n.format('msg-file-upload-completed') + '</span><br>';
            }
            htmlWlk += htmlRejected;
            pfx = htmlWlk.length > 0 ? '&bull; ' : '';

            return htmlWlk + '<span class="sp-txt-warn">' + pfx + warn + '</span>';
        },

        /**
         * Is FileReader object supported?
         */
        hasFileReader: function() {
            return typeof FileReader != 'undefined';
        },

        /**
         *
         */
        hasDraggable: function() {
            return 'draggable' in document.createElement('span');
        },

        /**
         * The FormData interface is needed for XMLHttpRequest.send().
         */
        hasFormData: function() {
            return !!window.FormData;
        },

        /**
         *
         */
        hasProgress: function() {
            return "upload" in new XMLHttpRequest;
        },

        /**
         * Is DropZone supported?
         */
        isSupported: function() {
            return !_ns.Utils.isMobileOrTablet() && this.hasDraggable() && this.hasFormData && this.hasFileReader;
        },

        /**
         *
         */
        humanFileSize: function(size, bin) {
            var i,
                decimals = 1,
                unit = bin ? 1024 : 1000,
                unitArray = bin ? ['', 'Ki', 'Mi', 'Gi', 'Ti'] : ['', 'k', 'M', 'G', 'T'];
            i = Math.floor(Math.log(size) / Math.log(unit));
            return size ? ((size / Math.pow(unit, i)).toFixed(decimals) * 1 + '&nbsp;' + unitArray[i] + 'B') : undefined;
        },

        /**
         *
         */
        isFileTypeSupported: function(fileExt, file) {
            var i,
                name = file.name.toLowerCase(),
                arrayLength = fileExt.length;
            for (i = 0; i < arrayLength; i++) {
                if (name.endsWith(fileExt[i])) {
                    return true;
                }
            }
            return false;
        },
        /**
         *
         */
        isHtmlTypeSupported: function(fileExt) {
            var wlk = [];
            wlk.name = 'probe.html';
            return this.isFileTypeSupported(fileExt, wlk);
        },
        /**
         *
         */
        sendFiles: function(files, url, fileField, fontField, fontEnum, maxBytes, fileExt, i18n, fooBefore, fooAfter, fooWarn, fooInfo) {
            var i,
                formData = new FormData(),
                totBytes = 0,
                file,
                allFileNames = '',
                infoArray = [];

            if (files.length === 0) {
                fooWarn(i18n.format('msg-file-upload-size-zero', [' ']));
                // todo
                return;
            }

            for (i = 0; i < files.length; i++) {

                file = files[i];

                if (!this.isFileTypeSupported(fileExt, file)) {
                    fooWarn(i18n.format('msg-file-upload-type-unsupported', [file.name]));
                    return;
                }

                if (i > 0) {
                    allFileNames += ' + ';
                }
                allFileNames += file.name;

                if (file.size > 0) {
                    formData.append(fileField, file);
                    totBytes += file.size;

                    infoArray.push({
                        name: file.name,
                        size: file.size
                    });
                }

            }

            if (totBytes === 0) {
                fooWarn(i18n.format('msg-file-upload-size-zero', [allFileNames]));
                return;
            }
            if (totBytes > maxBytes) {
                fooWarn(i18n.format('msg-file-upload-size-exceeded', [allFileNames, this.humanFileSize(totBytes), this.humanFileSize(maxBytes)]));
                return;
            }

            if (fooBefore) {
                fooBefore();
            }

            $.mobile.loading("show");

            $.ajax({
                url: (fontField && fontEnum) ? url + '?' + fontField + '=' + fontEnum : url,
                type: 'POST',
                data: formData,
                async: true,
                cache: false,
                contentType: false,
                processData: false,
                dataType: 'json'
            }).done(function(res) {
                if (res.result.code !== '0') {
                    fooWarn(res.result.txt, infoArray, res.filesStatus);
                } else if (fooInfo) {
                    fooInfo(infoArray, res.result.txt);
                }
            }).fail(function() {
                _ns.PanelCommon.onDisconnected();
            }).always(function() {
                $.mobile.loading("hide");
                if (fooAfter) {
                    fooAfter();
                }
            });
        },

        /**
         *
         */
        printURL: function(dataTransfer, url, fontEnum, fileExt, i18n, fooBefore, fooAfter, fooWarn, fooInfo) {
            var _obj = this,
                // Note: "URL" is not supported by Firefox.
                dataUrl = dataTransfer.getData("URL"),
                dataText,
                wlk,
                res;

            if (fooBefore) {
                fooBefore();
            }

            if (dataUrl.length === 0) {
                dataText = dataTransfer.getData("TEXT").trim();
                /*
                 * Be kind to Firefox, and interpret drag/drop of
                 * selected text as http(s) URL along the way :-)
                 */
                if (dataText.indexOf(' ') < 0 && (dataText.startsWith('http://') || dataText.startsWith('https://'))) {
                    dataUrl = dataText;
                } else {
                    fooWarn('[' + dataText + '] is not supported.');
                    return;
                }
            }

            wlk = [];
            wlk.name = dataUrl;

            if (!_obj.isFileTypeSupported(fileExt, wlk) && !_obj.isHtmlTypeSupported(fileExt)) {
                fooWarn(i18n.format('msg-file-upload-type-unsupported', [dataUrl]));
                return;
            }

            $.mobile.loading("show");
            _ns.Utils.asyncFoo(function() {
                res = _ns.api.call({
                    request: 'url-print',
                    dto: JSON.stringify({
                        url: dataUrl,
                        fontEnum: fontEnum
                    })
                });
                if (res.result.code === _ns.ApiResultCodeEnum.OK) {
                    if (fooInfo) {
                        wlk = [];
                        wlk.push({
                            name: dataUrl,
                            size: undefined
                        });
                        fooInfo(wlk);
                    }
                    if (fooAfter) {
                        fooAfter();
                    }
                } else {
                    fooWarn(res.result.txt);
                }
                $.mobile.loading("hide");
            });
        },

        /**
         *
         * @param {Object} dropzone (JQuery selector).
         */
        setCallbacks: function(dropzone, cssClassDragover, url, fileField, fontField, fooFontEnum, maxBytes, fileExt, i18n, fooBeforeSend, fooAfterSend, fooWarn, fooInfo, forPrint) {
            var _obj = this;

            dropzone.bind('dragover', function(e) {
                $(this).addClass(cssClassDragover);
                return false;
            });

            dropzone.bind('dragleave dragend', function(e) {
                $(this).removeClass(cssClassDragover);
                return false;
            });

            dropzone.bind('drop', function(e) {
                var dataTransfer = e.originalEvent.dataTransfer,
                    files = dataTransfer.files,
                    fontEnum = fooFontEnum();

                $(this).removeClass(cssClassDragover);

                if (files.length > 0) {
                    _obj.sendFiles(files, url, fileField, fontField, fontEnum, maxBytes, fileExt, i18n, fooBeforeSend, fooAfterSend, fooWarn, fooInfo);
                } else if (forPrint) {
                    _obj.printURL(dataTransfer, url, fontEnum, fileExt, i18n, fooBeforeSend, fooAfterSend, fooWarn, fooInfo);
                } else {
                    fooWarn(i18n.format('msg-file-upload-type-unsupported', ['TEXT']));
                }
                return false;
            });
        }
    };

}(this, this.document, this.navigator, this.org.savapage));

//--------------------------------------------------------------
// Switch Buttons
//--------------------------------------------------------------
(function($, window, document, _ns) {
    "use strict";

    //-----------------------------------------
    _ns.GenericButtonSwitch = {};
    _ns.GenericButtonSwitch.toggle = function(button, icons) {
        var classRem,
            classAdd;

        if (button.hasClass(icons.CLASS_ICON_ON)) {
            classRem = icons.CLASS_ICON_ON;
            classAdd = icons.CLASS_ICON_OFF;
        } else {
            classRem = icons.CLASS_ICON_OFF;
            classAdd = icons.CLASS_ICON_ON;
        }

        button.removeClass(classRem);
        button.addClass(classAdd);

        return classAdd === icons.CLASS_ICON_ON;
    };

    _ns.GenericButtonSwitch.isOn = function(button, icons) {
        return button.hasClass(icons.CLASS_ICON_ON);
    };

    _ns.GenericButtonSwitch.setState = function(button, isOn, icons) {
        button.removeClass(icons.CLASS_ICON_ON);
        button.removeClass(icons.CLASS_ICON_OFF);
        button.addClass(isOn ? icons.CLASS_ICON_ON : icons.CLASS_ICON_OFF);
    };

    //-----------------------------------------
    _ns.PreferredButtonSwitch = {
        icons: {
            CLASS_ICON_ON: 'ui-icon-mini-preferred-on',
            CLASS_ICON_OFF: 'ui-icon-mini-preferred-off'
        },
        toggle: function(button) {
            return _ns.GenericButtonSwitch.toggle(button, this.icons);
        },
        isOn: function(button) {
            return _ns.GenericButtonSwitch.isOn(button, this.icons);
        },
        setState: function(button, isOn) {
            _ns.GenericButtonSwitch.setState(button, isOn, this.icons);
        }
    };
    //-----------------------------------------
    _ns.SelectAllButtonSwitch = {
        icons: {
            CLASS_ICON_ON: 'ui-icon-mini-select-all-on',
            CLASS_ICON_OFF: 'ui-icon-mini-select-all-off'
        },
        toggle: function(button) {
            return _ns.GenericButtonSwitch.toggle(button, this.icons);
        },
        isOn: function(button) {
            return _ns.GenericButtonSwitch.isOn(button, this.icons);
        },
        setState: function(button, isOn) {
            _ns.GenericButtonSwitch.setState(button, isOn, this.icons);
        }
    };

}(this, this.document, this.navigator, this.org.savapage));

//--------------------------------------------------------------
// Number-up preview
//--------------------------------------------------------------
(function($, window, document, _ns) {
    "use strict";

    _ns.NumberUpPreview = {};

    _ns.NumberUpPreview.classStaple = {
        '3': null,
        '20': 'sp-nup-staple-top-left',
        '21': 'sp-nup-staple-bottom-left',
        '22': 'sp-nup-staple-top-right',
        '23': 'sp-nup-staple-bottom-right',
        '24': '',
        '25': '',
        '26': '',
        '27': '',
        '28': 'sp-nup-staple-left-dual',
        '29': 'sp-nup-staple-top-dual',
        '30': 'sp-nup-staple-right-dual',
        '31': 'sp-nup-staple-bottom-dual',
        '32': 'sp-nup-staple-left-triple',
        '33': 'sp-nup-staple-top-triple',
        '34': 'sp-nup-staple-right-triple',
        '35': 'sp-nup-staple-bottom-triple'
    };
    _ns.NumberUpPreview.classPunch = {
        '3': null,
        '70': '',
        '71': '',
        '72': '',
        '73': '',
        '74': 'sp-nup-punch-left',
        '75': 'sp-nup-punch-top',
        '76': 'sp-nup-punch-right',
        '77': 'sp-nup-punch-bottom',
        '78': 'sp-nup-punch-left',
        '79': 'sp-nup-punch-top',
        '80': 'sp-nup-punch-right',
        '81': 'sp-nup-punch-bottom',
        '82': 'sp-nup-punch-left',
        '83': 'sp-nup-punch-top',
        '84': 'sp-nup-punch-right',
        '85': 'sp-nup-punch-bottom'
    };

    _ns.NumberUpPreview.show = function(_view, numberUp, rotate180, punch, staple, landscape) {
        var clazz,
            sel = $('.sp-nup-preview-sheet').filter('[data-savapage=' + numberUp + '-' + (landscape ? 'l' : 'p') + ']'),
            selTbl = sel.find('table');

        _view.visible($('.sp-nup-preview'), true);

        _view.visible($('.sp-nup-preview-portrait'), !landscape && !rotate180);
        _view.visible($('.sp-nup-preview-portrait-180'), !landscape && rotate180);
        _view.visible($('.sp-nup-preview-landscape'), landscape && !rotate180);
        _view.visible($('.sp-nup-preview-landscape-180'), landscape && rotate180);

        _view.visible($('.sp-nup-preview-sheet'), false);
        _view.visible(sel, true);

        selTbl.attr('class', '');

        clazz = this.classPunch[punch];
        if (clazz && clazz.length) {
            selTbl.addClass(clazz);
        }
        clazz = this.classStaple[staple];
        if (clazz && clazz.length) {
            selTbl.addClass(clazz);
        }

    };

}(jQuery, this, this.document, this.org.savapage));

// @license-end
