/*! SavaPage jQuery Mobile Common | (c) 2011-2015 Datraverse B.V. | GNU Affero General Public License */

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

( function($, window, document, _ns) {"use strict";

		/*jslint browser: true*/
		/*global $, jQuery, alert*/

		var
		//-----------------------------------------------
		_watchdogHeartbeatSecs, _watchdogTimeoutSecs
		//
		, _watchdogTimer, _onWatchdogHeartbeat, _lastAppHeartbeat
		//
		, _deferAppWakeUp, _onAppWakeUp
		//
		, _doAppHeartbeat = function() {
			_lastAppHeartbeat = new Date().getTime();
		}
		//
		;

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
			var now, delta, wokeUp;

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

		_ns.configAppWatchdog = function(onAppWakeUp, watchdogHeartbeatSecs, watchdogTimeoutSecs) {
			_onAppWakeUp = onAppWakeUp;
			_watchdogHeartbeatSecs = watchdogHeartbeatSecs;
			_watchdogTimeoutSecs = watchdogTimeoutSecs;
		};

		_ns.startAppWatchdog = function() {

			_ns.deferAppWakeUp(false);

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
		 * the
		 * after successful login. Use stop() to stop after logout.
		 */
		_ns.Cometd = function() {

			var _super = new _ns.Base()
			//
			, _self = _ns.derive(_super)
			//
			, _connected = false
			// TODO: _isOn is ambiguous and needs rework
			, _isOn = false;

			// -------------------------------------------------------------
			// DOCUMENTATION for Callback Functions
			// Do NOT remove the commented documentation block below
			// -------------------------------------------------------------
			// _self.onConnecting = null;
			// _self.onDisconnecting = null;
			// _self.onHandshakeSuccess = null;
			// _self.onHandshakeFailure = null;
			// _self.onConnectionClosed = null;
			// _self.onReconnect = null;
			// _self.onConnectionBroken = null;
			// _self.onUnsuccessful = null;

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
				// See Java class: CometdConnectDto
				$.cometd.handshake({
					ext : {
						'org.savapage.authn' : {
							token : token
						}
					}
				});
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
				} catch (ignore) {
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
				 * See:
				 * http://cometd.org/documentation/2.x/cometd-javascript/configuration
				 * 'maxNetworkDelay' (default 10000) : max number of milliseconds
				 * to
				 * wait before considering a request to the Bayeux server failed.
				 */
				$.cometd.configure({
					url : cometURL,
					// logLevel : 'debug',
					logLevel : 'warn',
					maxNetworkDelay : maxNetworkDelay
				});

				// ...........................................................
				// CALLBACK-FUNCTION invoked when first contacting the server
				// and when the server has lost the state of this client
				// ...........................................................
				$.cometd.addListener('/meta/handshake', function(handshake) {

					// TODO: to be tested
					// var auth = handshake.ext && handshake.ext.'org.savapage.authn';
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

			var _onExpired = null, _onDisconnected = null;

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
			 * IE8 caches according to the /api?... URL, so although content differs
			 * in subsequent calls the same old cached PDF is shown.
			 */
			this.getUrl4Pdf = function(ranges, removeGraphics, jobIndex) {
				// number of milliseconds since 1970/01/01
				var d = new Date();
				return '/api?request=pdf&user=' + _user.id + '&jobIndex=' + jobIndex + '&ranges=' + ranges + '&graphics=' + ( removeGraphics ? '0' : '1') + '&unique=' + d.getTime().toString();
			};

			/**
			 * Simulate error response for disconnect situation
			 */
			this.simulateDisconnectError = function() {
				var msg = 'msg-disconnected';
				return {
					result : {
						code : '99',
						msg : msg,
						txt : _i18n.string(msg)
					}
				};
			};

			/**
			 * Downloads a file.
			 */
			this.download = function(request, data, requestSub) {
				var form = "#sp-file-download-api-form";
				$(form + " input[name='request']").attr("value", request);
				$(form + " input[name='request-sub']").attr("value", requestSub);
				$(form + " input[name='user']").attr("value", _user.id);
				$(form + " input[name='data']").attr("value", JSON.stringify(data));
				$(form).submit();
			};

			/**
			 *
			 */
			this.call = function(apiData) {

				var res, json;

				if (_user.loggedIn) {
					apiData.user = _user.id;
				}

				// Since this is a synchronous call, this does NOT show in
				// Chrome browser (at the moment of the call).
				$.mobile.loading("show");

				json = $.ajax({
					type : "POST",
					async : false,
					dataType : "json",
					url : '/api',
					data : apiData
				}).responseText;

				$.mobile.loading("hide");

				if (json) {
					res = $.parseJSON(json);
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

			userId : null,
			view : null,
			refreshPanelCommon : null,
			onDisconnected : null,

			/**
			 * Loads an ADMIN page of a list in a Panel.
			 *
			 * @param panel A Panel object.
			 * @param wClassPage The Wicket class of teh Panel
			 * @param jqId The HTML id attribute of the Panel
			 */
			loadListPageAdmin : function(panel, wClassPage, jqId) {
				_ns.PanelCommon.loadListPage(panel, "admin/" + wClassPage, jqId);
			},

			/**
			 * Loads a COMMON page of a list in a Panel.
			 *
			 * @param panel A Panel object.
			 * @param wClassPage The Wicket class of teh Panel
			 * @param jqId The HTML id attribute of the Panel
			 */
			loadListPageCommon : function(panel, wClassPage, jqId) {
				_ns.PanelCommon.loadListPage(panel, wClassPage, jqId);
			},

			/**
			 * Loads a page of a list in a Panel.
			 *
			 * @param panel A Panel object.
			 * @param wClassPage The Wicket class of teh Panel
			 * @param jqId The HTML id attribute of the Panel
			 */
			loadListPage : function(panel, wClassPage, jqId) {

				var
				//
				data = null
				//
				, jsonData = null
				//
				;

				if (panel.getInput) {
					data = panel.getInput(panel);
				}

				if (data) {
					jsonData = JSON.stringify(data);
				}

				$.mobile.loading("show");
				$.ajax({
					type : "POST",
					async : true,
					url : '/pages/' + wClassPage,
					data : {
						user : _ns.PanelCommon.userId,
						data : jsonData
					}
				}).done(function(html) {
					$(jqId).html(html).enhanceWithin();
					if (panel.onOutput) {
						panel.onOutput(panel, $.parseJSON($(jqId + ' .json-rsp').text()));
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
			onValidPage : function(onOK) {
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
			jqId : null,

			applyDefaults : function(my) {

				my.input.page = 1;
				my.input.maxResults = 10;

				my.input.select.user_id = null;

				my.input.select.text = null;

				my.input.select.trxType = null;
				my.input.select.date_from = null;
				my.input.select.date_to = null;

				my.input.sort.field = 'TRX_DATE';
				my.input.sort.ascending = false;

			},

			beforeload : function(my) {
				my.applyDefaults(my);
			},

			afterload : function(my) {
				var _view = _ns.PanelCommon.view;

				_view.mobipick($("#sp-accounttrx-date-from"));
				_view.mobipick($("#sp-accounttrx-date-to"));

				my.m2v(my);
				my.page(my, my.input.page);
			},

			m2v : function(my) {
				var _view = _ns.PanelCommon.view;

				$('#sp-accounttrx-containing-text').val('');

				_view.checkRadio('sp-accounttrx-select-type', 'sp-accounttrx-select-type-all');

				_view.mobipickSetDate($('#sp-accounttrx-date-from'), my.input.select.date_from);
				_view.mobipickSetDate($('#sp-accounttrx-date-to'), my.input.select.date_to);

				_view.checkRadio('sp-accounttrx-sort-by', 'sp-accounttrx-sort-by-date');
				_view.checkRadio('sp-accounttrx-sort-dir', 'sp-accounttrx-sort-dir-desc');
			},

			v2m : function(my) {
				var _view = _ns.PanelCommon.view
				//
				, sel = $('#sp-accounttrx-date-from')
				//
				, date = _view.mobipickGetDate(sel)
				//
				, val
				//
				, present = (sel.val().length > 0);

				//
				my.input.select.date_from = ( present ? date.getTime() : null);

				//
				val = $('#sp-accounttrx-hidden-user-id').val();
				present = (val.length > 0);
				my.input.select.user_id = ( present ? val : null);

				//
				val = _view.getRadioValue('sp-accounttrx-select-type');
				present = (val.length > 0);
				my.input.select.trxType = ( present ? val : null);

				sel = $('#sp-accounttrx-date-to');
				date = _view.mobipickGetDate(sel);
				present = (sel.val().length > 0);
				my.input.select.date_to = ( present ? date.getTime() : null);

				sel = $('#sp-accounttrx-containing-text');
				present = (sel.val().length > 0);
				my.input.select.text = ( present ? sel.val() : null);

				my.input.sort.field = _view.getRadioValue('sp-accounttrx-sort-by');
				my.input.sort.ascending = _view.isRadioIdSelected('sp-accounttrx-sort-dir', 'sp-accounttrx-sort-dir-asc');

			},

			// JSON input
			input : {
				page : 1,
				maxResults : 10,
				select : {
					user_id : null,
					// See Java: org.savapage.core.jpa.AccountTrx.TrxType
					trxType : null,
					date_from : null,
					date_to : null,
					text : null
				},
				sort : {
					field : 'TRX_DATE', // enum: AccountTrx.TrxType
					ascending : false
				}
			},

			// JSON output
			output : {
				lastPage : null,
				nextPage : null,
				prevPage : null
			},

			refresh : function(my, skipBeforeLoad) {
				_ns.PanelCommon.refreshPanelCommon('AccountTrxBase', skipBeforeLoad, my);
			},

			// show page
			page : function(my, nPage) {
				_ns.PanelCommon.onValidPage(function() {
					my.input.page = nPage;
					my.v2m(my);
					_ns.PanelCommon.loadListPageCommon(my, 'AccountTrxPage', '#sp-accounttrx-list-page');
				});
			},

			getInput : function(my) {
				return my.input;
			},

			onOutput : function(my, output) {

				my.output = output;
				/*
				 * NOTICE the $().one() construct. Since the page get
				 * reloaded all the time, we want a single-shot binding.
				 */
				$(".sp-accounttrx-page").one('click', null, function() {
					my.page(my, parseInt($(this).text(), 10));
					/*
					 * return false so URL is not followed.
					 */
					return false;
				});
				$(".sp-accounttrx-page-next").one('click', null, function() {
					my.page(my, my.output.nextPage);
					/*
					 * return false so URL is not followed.
					 */
					return false;
				});
				$(".sp-accounttrx-page-prev").one('click', null, function() {
					my.page(my, my.output.prevPage);
					/*
					 * return false so URL is not followed.
					 */
					return false;
				});

			}
		};

		/**
		 * NOTE: null values need to be set be the client.
		 */
		_ns.PanelDocLogBase = {

			// The HTML id attribute of the Panel.
			jqId : null,

			clearHiddenUserid : function() {
				$('#sp-doclog-hidden-user-id').val("");
				$("#sp-doclog-user-title").html("");
			},

			/*
			 * Generic: can be reused
			 */
			applyDefaults : function(my) {

				my.input.page = 1;
				my.input.maxResults = 10;

				/*
				 * NOTE: In User WebApp the user_id of the logged in user
				 * is used. In Admin WebApp value of field below is used.
				 */
				my.input.select.user_id = null;

				// ALL, IN, OUT, PDF, PRINT
				my.input.select.doc_type = "ALL";

				my.input.select.date_from = null;
				my.input.select.date_to = null;
				my.input.select.doc_name = '';

				my.input.select.queue_id = null;

				my.input.select.signature = "";
				my.input.select.destination = "";
				// Boolean
				my.input.select.letterhead = null;

				my.input.select.printer_id = null;
				my.input.select.job_state = null;
				// Boolean
				my.input.select.duplex = null;

				my.input.select.author = '';
				my.input.select.subject = '';
				my.input.select.keywords = '';
				my.input.select.userpw = '';
				my.input.select.ownerpw = '';
				// Boolean
				my.input.select.encrypted = null;

				my.input.sort.field = 'date';
				my.input.sort.ascending = false;

			},

			beforeload : function(my) {
				my.applyDefaults(my);
			},

			afterload : function(my) {
				var _view = _ns.PanelCommon.view;

				_view.mobipick($("#sp-doclog-date-from"));
				_view.mobipick($("#sp-doclog-date-to"));

				my.m2v(my);
				my.page(my, my.input.page);
			},

			setVisibility : function(my) {
				var _view = _ns.PanelCommon.view;

				$('.sp-doclog-cat').hide();

				if (_view.isRadioIdSelected('sp-doclog-select-type', 'sp-doclog-select-type-in')) {
					$('#sp-doclog-cat-queue').show();
					$('#sp-doclog-sort-by-queue-cat').show();
				} else if (_view.isRadioIdSelected('sp-doclog-select-type', 'sp-doclog-select-type-out')) {
					$('#sp-doclog-cat-out').show();
				} else if (_view.isRadioIdSelected('sp-doclog-select-type', 'sp-doclog-select-type-pdf')) {
					$('#sp-doclog-cat-out').show();
					$('#sp-doclog-cat-pdf').show();
				} else if (_view.isRadioIdSelected('sp-doclog-select-type', 'sp-doclog-select-type-print')) {
					$('#sp-doclog-cat-out').show();
					$('#sp-doclog-cat-printer').show();
					$('#sp-doclog-sort-by-printer-cat').show();
				}
			},

			m2v : function(my) {
				var val, id
				//
				, _view = _ns.PanelCommon.view;

				$('#sp-doclog-document-name').val(my.input.select.doc_name);

				// For future use.
				//$('#sp-doc-out-signature').val(my.input.select.signature);

				$('#sp-doc-out-destination').val(my.input.select.destination);

				_view.mobipickSetDate($('#sp-doclog-date-from'), my.input.select.date_from);
				_view.mobipickSetDate($('#sp-doclog-date-to'), my.input.select.date_to);

				//--
				_view.checkRadioValue('sp-doclog-sort-by', my.input.sort.field);

				id = 'sp-doclog-sort-dir';
				_view.checkRadio(id, my.input.sort.ascending ? id + '-asc' : id + '-desc');

				//---------------------------------
				if (my.input.select.printer_id) {
					$('#sp-print-out-printer').val(my.input.select.printer_id).selectmenu('refresh');
					$('#sp-print-out-state').val(my.input.select.job_state).selectmenu('refresh');
					my.input.select.doc_type = "PRINT";
				} else if (my.input.select.queue_id) {
					$('#sp-print-in-queue').val(my.input.select.queue_id).selectmenu('refresh');
					my.input.select.doc_type = "IN";
				}

				//--
				_view.checkRadioValue('sp-doclog-select-type', my.input.select.doc_type);

				val = my.input.select.letterhead;
				_view.checkRadioValue('sp-doc-out-lh', val === null ? "" : ( val ? "1" : "0"));

				val = my.input.select.duplex;
				_view.checkRadioValue('sp-print-out-duplex', val === null ? "" : ( val ? "1" : "0"));

				val = my.input.select.encrypted;
				_view.checkRadioValue('sp-pdf-out-encrypt', val === null ? "" : ( val ? "1" : "0"));

				//---------------------------------
				my.setVisibility(my);
			},

			v2m : function(my) {
				var _view = _ns.PanelCommon.view
				//
				, val, sel, date, present;

				/*
				 * QUICK & DIRTY: Use sel as indicator to see if WebApp is
				 * properly loaded (e.g. when F5 browser refresh).
				 */
				sel = $('#sp-doclog-date-from');
				if (sel.length === 0) {
					return;
				}

				//
				val = $('#sp-doclog-hidden-user-id').val();
				present = (val.length > 0);
				my.input.select.user_id = ( present ? val : null);

				//
				date = _view.mobipickGetDate(sel);
				present = (sel.val().length > 0);
				my.input.select.date_from = ( present ? date.getTime() : null);

				my.input.select.doc_type = _view.getRadioValue('sp-doclog-select-type');

				sel = $('#sp-doclog-date-to');
				date = _view.mobipickGetDate(sel);
				present = (sel.val().length > 0);
				my.input.select.date_to = ( present ? date.getTime() : null);

				sel = $('#sp-doclog-document-name');
				present = (sel.val().length > 0);
				my.input.select.doc_name = ( present ? sel.val() : null);

				sel = $('#sp-print-out-printer');
				present = (sel.val() !== "0");
				my.input.select.printer_id = ((my.input.select.doc_type === "PRINT" && present) ? sel.val() : null);

				my.input.select.job_state = $('#sp-print-out-state').val();

				sel = $('#sp-print-in-queue');
				present = (sel.val() !== "0");
				my.input.select.queue_id = ((my.input.select.doc_type === "IN" && present) ? sel.val() : null);

				my.input.sort.field = _view.getRadioValue('sp-doclog-sort-by');

				my.input.sort.ascending = _view.isRadioIdSelected('sp-doclog-sort-dir', 'sp-doclog-sort-dir-asc');

				// For future use.
				/*
				 sel = $('#sp-doc-out-signature');
				 present = (sel.val().length > 0);
				 my.input.select.signature = ( present ? sel.val() : null);
				 */
				my.input.select.signature = null;

				sel = $('#sp-doc-out-destination');
				present = (sel.val().length > 0);
				my.input.select.destination = ( present ? sel.val() : null);

				//
				val = _view.getRadioValue('sp-doc-out-lh');
				my.input.select.letterhead = (val === "" ? null : (val === "1"));

				//
				val = _view.getRadioValue('sp-print-out-duplex');
				my.input.select.duplex = (val === "" ? null : (val === "1"));

				//
				sel = $('#sp-pdf-out-author');
				present = (sel.val().length > 0);
				my.input.select.author = ( present ? sel.val() : null);

				sel = $('#sp-pdf-out-subject');
				present = (sel.val().length > 0);
				my.input.select.subject = ( present ? sel.val() : null);

				sel = $('#sp-pdf-out-keywords');
				present = (sel.val().length > 0);
				my.input.select.keywords = ( present ? sel.val() : null);

				sel = $('#sp-pdf-out-pw-user');
				present = (sel.val().length > 0);
				my.input.select.userpw = ( present ? sel.val() : null);

				sel = $('#sp-pdf-out-pw-owner');
				present = (sel.val().length > 0);
				my.input.select.ownerpw = ( present ? sel.val() : null);

				//
				val = _view.getRadioValue('sp-pdf-out-encrypt');
				my.input.select.encrypted = (val === "" ? null : (val === "1"));

			},

			// JSON input
			input : {
				page : 1,
				maxResults : 10,
				select : {
					date_from : null,
					date_to : null,
					doc_name : null,
					doc_type : "ALL",
					user_id : null,
					printer_id : null,
					queue_id : null
				},
				sort : {
					field : 'date', // date | name | queue | printer
					ascending : false
				}
			},

			// JSON output
			output : {
				lastPage : null,
				nextPage : null,
				prevPage : null
			},

			refresh : function(my, skipBeforeLoad) {
				_ns.PanelCommon.refreshPanelCommon('DocLogBase', skipBeforeLoad, my);
			},

			// show page
			page : function(my, nPage) {
				_ns.PanelCommon.onValidPage(function() {
					my.input.page = nPage;
					my.v2m(my);
					_ns.PanelCommon.loadListPageCommon(my, 'DocLogPage', '#sp-doclog-list-page');
				});
			},

			getInput : function(my) {
				return my.input;
			},

			onOutput : function(my, output) {
				var _view = _ns.PanelCommon.view;

				my.output = output;
				/*
				 * NOTICE the $().one() construct. Since the page get
				 * reloaded all the time, we want a single-shot binding.
				 */
				$(".sp-doclog-page").one('click', null, function() {
					my.page(my, parseInt($(this).text(), 10));
					/*
					 * return false so URL is not followed.
					 */
					return false;
				});
				$(".sp-doclog-page-next").one('click', null, function() {
					my.page(my, my.output.nextPage);
					/*
					 * return false so URL is not followed.
					 */
					return false;
				});
				$(".sp-doclog-page-prev").one('click', null, function() {
					my.page(my, my.output.prevPage);
					/*
					 * return false so URL is not followed.
					 */
					return false;
				});

				$('.sparkline-printin-pie').sparkline('html', {
					type : 'pie',
					sliceColors : [_view.colorQueue]
				});
				$('.sparkline-pdfout-pie').sparkline('html', {
					type : 'pie',
					sliceColors : [_view.colorPDF]
				});
				$('.sparkline-printout-pie').sparkline('html', {
					type : 'pie',
					sliceColors : [_view.colorPrinter, _view.colorSheet]
				});
			}
		};

		/**
		 * Constructor
		 *
		 * @param _id :
		 *            The jquery selector identifying the page, e.g.: '#mypage'
		 * @param _class :
		 *            The Wicket java class
		 */
		_ns.Page = function(_i18n, _view, _id, _class) {

			this.isLoaded = false;

			this.id = function() {
				return _id;
			};

			this.loadShowAsync = function(onDone) {
				var _this = this;
				_view.showPageAsync($(_id), _class, function() {
					_this.isLoaded = true;
					if (onDone) {
						onDone();
					}
				});
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

			var _page, _self, _onSelectLanguage;

			_page = new _ns.Page(_i18n, _view, '#page-language', 'Language');
			_self = _ns.derive(_page);

			/**
			 *
			 */
			_self.onSelectLanguage = function(foo) {
				_onSelectLanguage = foo;
			};

			/**
			 *
			 */
			$(_self.id()).on('pagecreate', function(event) {

				$('#language-list').on('click', null, null, function(event) {
					var str, lang;
					str = $(event.target).attr('id');
					lang = str.substr(str.indexOf("#") + 1);

					_onSelectLanguage(lang);

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

			var _page, _self, _onSelectReset;

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
			var _page = new _ns.Page(_i18n, _view, '#page-info', 'AppAbout')
			//
			, _self = _ns.derive(_page)
			//
			;

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
			var _page, _self, _pageAbout, _util = _ns.Utils
			//
			, _onLanguage, _onLogin, _onShow
			//
			, _authName, _authId, _authCardLocal, _authCardIp
			//
			, _authCardPinReq, _authCardSelfAssoc
			//
			, _ID_MODE_NAME = '#sp-div-login-name'
			//
			, _ID_MODE_ID = '#sp-div-login-number'
			//
			, _ID_MODE_CARD_LOCAL = '#sp-div-login-card-local'
			//
			, _ID_MODE_CARD_IP = '#sp-div-login-card-ip'
			//
			, _ID_MODE_CARD_ASSOC = '#sp-div-login-card-assoc'
			//
			, _ID_BTN_MODE_NAME = '#sp-btn-login-mode-name'
			//
			, _ID_BTN_MODE_ID = '#sp-btn-login-mode-id'
			//
			, _ID_BTN_MODE_CARD_LOCAL = '#sp-btn-login-mode-card-local'
			//
			, _ID_BTN_MODE_CARD_IP = '#sp-btn-login-mode-card-ip'
			//
			, _ID_BTN_LOGIN_NAME = '#sp-btn-login-name'
			//
			, _ID_BTN_LOGIN_ID = '#sp-btn-login-number'
			//
			, _ID_BTN_LOGIN_CARD_LOCAL = '#sp-btn-login-card-local'
			//
			, _ID_BTN_LOGIN_CARD_IP = '#sp-btn-login-card-ip'
			//
			, _ID_BTN_LOGIN_CARD_ASSOC = '#sp-btn-login-card-assoc'
			//
			, _authModeDefault, _onAuthModeSelect, _modeSelected
			//
			, _startTimeCardNumber = null
			// The max number of milliseconds allowed for entering the
			// local card number.
			, _MAX_CARD_NUMBER_MSECS = 500
			// The collected local card number from individual keystrokes,
			// or the cached Card Number to associate with a user.
			, _collectedCardNumber
			//
			, _timeoutCardAssoc, _countdownCardAssoc
			// Max number of seconds the assoc card dialog is visible.
			, _MAX_CARD_ASSOC_SECS = 30
			//
			, _isCardRegistered
			//
			;

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

			_self.onLogin = function(foo) {
				_onLogin = foo;
			};
			_self.onShow = function(foo) {
				_onShow = foo;
			};
			_self.onLanguage = function(foo) {
				_onLanguage = foo;
			};

			//_self.onCardAssocCancel

			/**
			 * Sets the authentication mode and adapts the visibility of the
			 * dialog.
			 *
			 * Example: pages.login.setAuthMode(true, true, true,
			 * _view.AUTH_MODE_NAME);
			 */
			_self.setAuthMode = function(authName, authId, authCardLocal, authCardIp, modeDefault, authCardPinReq, authCardSelfAssoc, cardLocalMaxMsecs, cardAssocMaxSecs) {
				_authName = authName;
				_authId = authId;
				_authCardLocal = authCardLocal;
				_authCardIp = authCardIp;
				_authModeDefault = modeDefault;
				_authCardPinReq = authCardPinReq;
				_authCardSelfAssoc = authCardSelfAssoc;
				_MAX_CARD_NUMBER_MSECS = cardLocalMaxMsecs;
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
				_startTimeCardNumber = new Date().getTime();
				_collectedCardNumber = cardNumber;
				_onAuthModeSelect(_view.AUTH_MODE_CARD_ASSOC);
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
				var reg = false, res = _api.call({
					'request' : 'card-is-registered',
					card : card
				});
				if (res.result.code === '0') {
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
				if (_timeoutCardAssoc) {
					window.clearTimeout(_timeoutCardAssoc);
					_timeoutCardAssoc = null;
				}

				//
				_modeSelected = modeSelected;

				$(_ID_MODE_NAME).hide();
				$(_ID_MODE_ID).hide();
				$(_ID_MODE_CARD_LOCAL).hide();
				$(_ID_MODE_CARD_IP).hide();
				$(_ID_MODE_CARD_ASSOC).hide();

				$(_ID_BTN_MODE_NAME).hide();
				$(_ID_BTN_MODE_ID).hide();
				$(_ID_BTN_MODE_CARD_LOCAL).hide();
				$(_ID_BTN_MODE_CARD_IP).hide();

				if (modeSelected === _view.AUTH_MODE_NAME) {

					$(_ID_MODE_NAME).show();
					$(_ID_BTN_MODE_NAME).hide();

					if (_authId) {
						$(_ID_BTN_MODE_ID).show();
					}
					if (_authCardLocal) {
						$(_ID_BTN_MODE_CARD_LOCAL).show();
					}
					if (_authCardIp) {
						$(_ID_BTN_MODE_CARD_IP).show();
					}

					$('#sp-login-user-name').focus();

				} else if (modeSelected === _view.AUTH_MODE_ID) {

					$(_ID_MODE_ID).show();
					$(_ID_BTN_MODE_ID).hide();

					if (_authName) {
						$(_ID_BTN_MODE_NAME).show();
					}
					if (_authCardLocal) {
						$(_ID_BTN_MODE_CARD_LOCAL).show();
					}
					if (_authCardIp) {
						$(_ID_BTN_MODE_CARD_IP).show();
					}

					$('#sp-login-id-number').focus();

				} else if (modeSelected === _view.AUTH_MODE_CARD_LOCAL) {

					$(_ID_MODE_CARD_LOCAL).show();
					$(_ID_BTN_MODE_CARD_LOCAL).hide();

					if (_authName) {
						$(_ID_BTN_MODE_NAME).show();
					}
					if (_authId) {
						$(_ID_BTN_MODE_ID).show();
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
					if (_authId) {
						$(_ID_BTN_MODE_ID).show();
					}
					if (_authCardLocal) {
						$(_ID_BTN_MODE_CARD_LOCAL).show();
					}
					$('#sp-login-card-ip-pin-group').hide();
					$('#sp-login-card-ip-number-group').show();

				}

				if (modeSelected === _view.AUTH_MODE_CARD_ASSOC) {

					$('.sp-login-dialog').hide();
					$('.sp-login-dialog-assoc').show();

					$(_ID_MODE_CARD_ASSOC).show();
					$('#sp-login-user-name-assoc').focus();

					$('#sp-login-card-assoc-countdown').text(_MAX_CARD_ASSOC_SECS);

					_countdownCardAssoc = _MAX_CARD_ASSOC_SECS - 1;
					_timeoutCardAssoc = window.setInterval(function() {
						$('#sp-login-card-assoc-countdown').text(_countdownCardAssoc);
						if (_countdownCardAssoc-- === 0) {
							$(_ID_BTN_LOGIN_CARD_ASSOC + '-cancel').click();
						}
					}, 1000);

				} else {
					_collectedCardNumber = null;
					$('.sp-login-dialog').show();
					$('.sp-login-dialog-assoc').hide();
				}

				if (_authName) {
					nMethods++;
				}
				if (_authId) {
					nMethods++;
				}
				if (_authCardLocal) {
					nMethods++;
				}
				if (_authCardIp) {
					nMethods++;
				}
				if (nMethods < 2) {
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
					var key = e.keyCode || e.which;
					// 13 = <Enter>, 27 = <ESC>
					if (key === 13) {
						if (_modeSelected === _view.AUTH_MODE_NAME) {
							$(_ID_BTN_LOGIN_NAME).click();
						} else if (_modeSelected === _view.AUTH_MODE_ID) {
							$(_ID_BTN_LOGIN_ID).click();
						} else if (_modeSelected === _view.AUTH_MODE_CARD_LOCAL) {
							$('#sp-login-card-local-number').val(_collectedCardNumber);
							$(_ID_BTN_LOGIN_CARD_LOCAL).click();
						} else if (_modeSelected === _view.AUTH_MODE_CARD_IP && $('#sp-login-card-ip-number').val().length > 0) {
							$(_ID_BTN_LOGIN_CARD_IP).click();
						} else if (_modeSelected === _view.AUTH_MODE_CARD_ASSOC) {
							$(_ID_BTN_LOGIN_CARD_ASSOC).click();
						}
					} else if (key === 27) {
						if (_modeSelected === _view.AUTH_MODE_CARD_ASSOC) {
							$(_ID_BTN_LOGIN_CARD_ASSOC + '-cancel').click();
						}
					} else if (_modeSelected === _view.AUTH_MODE_CARD_LOCAL && $('#sp-login-card-local-number').val().length === 0) {
						/*
						 * IMPORTANT: only look at printable chars. When doing an
						 * alt-tab
						 * to return to THIS application we do not want to
						 * collect
						 * !!!
						 */
						if (32 < key && key < 127) {
							if (_startTimeCardNumber === null) {
								_startTimeCardNumber = new Date().getTime();
								_collectedCardNumber = '';
							}
							_collectedCardNumber += String.fromCharCode(key);
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

				$('#sp-login-card-local-number-group').focusin(function() {
					_startTimeCardNumber = null;
					$('#sp-login-card-local-focusin').show();
					$('#sp-login-card-local-focusout').hide();
				});

				$('#sp-login-card-local-number-group').focusout(function() {
					_startTimeCardNumber = null;
					$('#sp-login-card-local-focusin').hide();
					// Use the fadeIn to prevent a 'flash' effect when just
					// anotherfocus is lost because another auth method is selected.
					$('#sp-login-card-local-focusout').fadeIn(700);
				});

				if (_authName) {

					$(_ID_BTN_MODE_NAME).click(function() {
						_onAuthModeSelect(_view.AUTH_MODE_NAME);
						return false;
					});

					$(_ID_BTN_LOGIN_NAME).click(function() {
						var sel = $('#sp-login-user-password'), pw = sel.val();
						sel.val('');
						_onLogin(_view.AUTH_MODE_NAME, $('#sp-login-user-name').val(), pw);
						return false;
					});

					$("#sp-login-user-name").focus(function() {
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
						var selNum = $('#sp-login-id-number'), num = selNum.val()
						//
						, selPin = $('#sp-login-id-pin'), pin = selPin.val();
						selNum.val('');
						selPin.val('');

						_onLogin(_view.AUTH_MODE_ID, num, pin);
						return false;
					});
				}

				if (_authCardLocal) {

					$(_ID_BTN_MODE_CARD_LOCAL).click(function() {
						_onAuthModeSelect(_view.AUTH_MODE_CARD_LOCAL);
						return false;
					});

					$(_ID_BTN_LOGIN_CARD_LOCAL).click(function() {
						var selCard = $('#sp-login-card-local-number'), card = selCard.val()
						//
						, selPin = $('#sp-login-card-local-pin'), pin = selPin.val()
						// Elapsed time since the first keyup in the local card
						// number field.
						, elapsedTimeCardNumber
						//
						;

						if (card.length === 0) {
							$('#sp-login-card-local-number-group').focus();
							return false;
						}

						if (_startTimeCardNumber) {
							elapsedTimeCardNumber = new Date().getTime() - _startTimeCardNumber;
							_startTimeCardNumber = null;
							if (elapsedTimeCardNumber > _MAX_CARD_NUMBER_MSECS) {
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
						// hidden field
						var selCard = $('#sp-login-card-ip-number')
						//
						, card = selCard.val()
						//
						, selPin = $('#sp-login-card-ip-pin'), pin = selPin.val()
						//
						;

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
						_onLogin(_view.AUTH_MODE_NAME, $('#sp-login-user-name-assoc').val(), $('#sp-login-user-password-assoc').val(), _collectedCardNumber);
						return false;
					});
					$(_ID_BTN_LOGIN_CARD_ASSOC + '-cancel').click(function() {
						_onAuthModeSelect(_authModeDefault);
						_self.onCardAssocCancel();
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

				_onAuthModeSelect(_authModeDefault);

				_onShow(event, ui);

				// Pick up URL parameters
				$('#sp-login-user-name').val(_util.getUrlParam('user'));
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

			var _view = this;

			/*
			 * AUTH Modes used for login. These names are reserved and are
			 * hardcoded in SavaPage server.
			 */
			this.AUTH_MODE_NAME = 'name';
			this.AUTH_MODE_ID = 'id';
			this.AUTH_MODE_CARD_LOCAL = 'nfc-local';
			this.AUTH_MODE_CARD_IP = 'nfc-network';

			// Dummy AUTH Modes to associate Card with user.
			this.AUTH_MODE_CARD_ASSOC = '_CA';

			// orange
			this.colorSheet = '#FF9900';
			// red
			this.colorPrinter = '#D14719';
			// blue
			this.colorQueue = '#597BDE';
			// green
			this.colorPDF = '#33AD33';

			this.userChartColors = [this.colorPrinter, this.colorQueue, this.colorPDF];

			// this.onDisconnected

			this.imgBase64 = false;

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
						type : "POST",
						async : false,
						dataType : "text",
						url : url,
						data : {}
					}).responseText;
				}
				return url;
			};

			/**
			 *
			 */
			this.showXyChart = function(id, xydata) {

				return $.jqplot(id, xydata.dataSeries, {
					textColor : "#ff0000",
					grid : {
						shadow : false,
						borderWidth : 1.0
					},
					seriesColors : this.userChartColors,
					axes : {
						xaxis : {
							renderer : $.jqplot.DateAxisRenderer,
							tickOptions : {
								formatString : this.jqPlotMonthDayFormat()
							},
							tickInterval : '1 week'
						},
						yaxis : {
							renderer : $.jqplot.LogAxisRenderer
						}
					},
					title : {
						//text : xydata.optionObj.title.text,
						show : false
					}
				});
			};

			/**
			 *
			 */
			this.showPieChart = function(id, piedata) {

				return $.jqplot(id, piedata.dataSeries, {
					seriesDefaults : {
						renderer : $.jqplot.PieRenderer,
						rendererOptions : {
							dataLabels : 'value',
							// Put data labels on the pie slices.
							// By default, labels show the percentage of the
							// slice.
							showDataLabels : true,
							// Turn off filling of slices.
							fill : false,
							// Add a margin to separate the slices.
							sliceMargin : 4,
							// stroke the slices with a little thicker line.
							lineWidth : 5
						}
					},
					seriesColors : this.userChartColors,
					grid : {
						shadow : false,
						borderWidth : 1.0
					},
					legend : {
						show : true,
						location : 'se'
					},
					title : {
						//text : piedata.optionObj.title.text,
						show : false
					}
				});

			};

			/**
			 *
			 */
			this.jqPlotData = function(chartType, isGlobal) {
				var res = _api.call({
					request : 'jqplot',
					chartType : chartType,
					isGlobal : ( isGlobal ? '1' : '0')
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
					changeHash : false
				});
			};

			/**
			 *
			 */
			this.initI18n = function(language) {
				this.language = language;
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

				var html;

				// Since this is a synchronous call, this does NOT show in
				// Chrome browser (at the moment of the call).
				$.mobile.loading("show");

				html = $.ajax({
					type : "POST",
					async : false,
					url : '/pages/' + page,
					dataType : 'json',
					data : data || {}
				}).responseText;

				$.mobile.loading("hide");

				if (html) {
					return html;
				}

				/*
				 * Do NOT use this.showApiMsg(), since it will NOT show, since
				 * the login window is showed as a response.
				 */
				this.message(_i18n.string('msg-disconnected'));
				this.onDisconnected();
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
			 * Gets HTML of a User page (synchronous).
			 * @param page
			 *            The Wicket class
			 */
			this.getUserPageHtml = function(page) {
				return this.getPageHtml('user/' + page);
			};

			/**
			 * Gets HTML of an Admin page (synchronous).
			 * @param page
			 *            The Wicket class
			 */
			this.getAdminPageHtml = function(page, data) {
				return this.getPageHtml('admin/' + page, data);
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

			this.showPageAsync = function(sel, page, onDone) {
				var _this = this;

				if ($(sel).children().length === 0) {

					$.mobile.loading("show");

					$.ajax({
						type : "POST",
						async : true,
						dataType : "html",
						url : '/pages/' + page
					}).done(function(html) {
						$(sel).html(html).page();
						_view.changePage($(sel));
						if (onDone) {
							onDone();
						}
					}).fail(function() {
						/*
						 * Do NOT use this.showApiMsg(), since it will NOT show,
						 * since
						 * the login window is showed as a response.
						 */
						_this.message(_i18n.string('msg-disconnected'));
						_this.onDisconnected();
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
					"z-index" : 5000,
					display : "block",
					position : "relative",
					opacity : 0.96,
					top : $(window).scrollTop() + 100,
					// 'margin-top' : '100px',
					'margin-left' : 'auto',
					'margin-right' : 'auto',
					// width : "400px"
					width : $(window).width() * 0.8
				}).prependTo($('body')).delay(2500).fadeOut(1000, function() {
					$(this).remove();
				});
			};

			this.apiResMsgTitle = function(result) {
				var title;
				if (result.code === "1") {
					title = _i18n.string('title-info');
				} else if (result.code === "2") {
					title = _i18n.string('title-warning');
				} else {
					title = _i18n.string('title-error');
				}
				return title;
			};

			this.apiResMsgCssClass = function(result) {
				var klas;
				if (result.code === "1") {
					klas = 'sp-msg-popup-info';
				} else if (result.code === "2") {
					klas = 'sp-msg-popup-warn';
				} else {
					klas = 'sp-msg-popup-error';
				}
				return klas;
			};

			this.apiResMsg = function(result) {

				var title, cssClass, txt, sel, html;

				if (result.code === "9") {
					/*
					 * Do NOT show a session expiration error, since this will be
					 * handled in the onExpire() callback.
					 */
					return;
				}

				if (result.code === "0") {
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

				// remove CR/LF, CR and replace all multiple spaces with one single space
				txt = txt.replace(/\r?\n|\r/g, "").replace(/ +(?= )/g, '');

				html = '<a href="#" data-rel="back" class="ui-btn ui-corner-all ui-shadow ui-btn-a ui-icon-delete ui-btn-icon-notext ui-btn-right">Close</a>';
				html += '<h3 class="ui-title ' + cssClass + '">' + title + '</h3>';
				html += '<p class="sp-txt-wrap ' + cssClass + '">' + txt + '</p>';
				html += '<div data-iconpos="none" data-mini="true" data-role="collapsible" data-collapsed="true">';
				html += '<h3 style="width: 50px;">. . .</h3>';
				html += '<textarea onclick="this.focus();this.select()" readonly="readonly" autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false">' + txt + '</textarea>';
				html += '</div>';

				sel.html(html);
				sel.enhanceWithin().popup("open");
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
				//@JQ-1.9.1
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
			 * Checks a radiobutton.
			 */
			this.checkRadio = function(name, id) {
				var sel = $('input[name="' + name + '"]');
				sel.prop('checked', false);
				//@JQ-1.9.1
				$('#' + id).prop('checked', true);
				//@JQ-1.9.1
				sel.checkboxradio("refresh");
			};

			/**
			 * Checks a radiobutton value.
			 */
			this.checkRadioValue = function(name, value) {
				var radio = 'input[name="' + name + '"]'
				//
				, sel = $(radio + '[value="' + value + '"]')
				//
				;
				$(radio).prop('checked', false);
				sel.prop('checked', true);
				$(radio).checkboxradio("refresh");
			};

			this.getRadioValue = function(name) {
				return $("input:radio[name='" + name + "']:checked").val();
			};

			this.isRadioIdSelected = function(name, id) {
				return ($("input:radio[name='" + name + "']:checked").attr('id') === id);
			};

			this.showApiMsg = function(res) {
				if (res && res.result) {
					this.apiResMsg(res.result);
				}
			};

			this.mobipick = function(sel) {
				return sel.mobipick({
					locale : _view.language
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
			};

			/**
			 * Enables/Disables jQuery selector.
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
			 *
			 */
			this.checkPwMatch = function(jqPassw, jqConfirm) {

				var pw_valid = false, pw1 = jqPassw.val(), pw2 = jqConfirm.val(), msg;

				if (pw1 !== pw2) {
					msg = 'msg-input-mismatch';
				} else if (!pw1) {
					msg = 'msg-input-empty';
				} else {
					pw_valid = true;
				}
				if (msg) {
					this.apiResMsg({
						result : '3',
						txt : _i18n.format(msg)
					});
				}

				return pw_valid;
			};

		};
	}(jQuery, this, this.document, this.org.savapage));
