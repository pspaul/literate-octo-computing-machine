/*! SavaPage jQuery Mobile Job Tickets Page | (c) 2011-2016 Datraverse B.V. | GNU Affero General Public License */

/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

( function($, window, document, JSON, _ns) {"use strict";

		// =========================================================================
		/**
		 * Constructor
		 */
		_ns.PageJobTickets = function(_i18n, _view, _model, _api) {

			var _page = new _ns.Page(_i18n, _view, "#page-jobtickets", "PageJobTickets")
			//
			, _self = _ns.derive(_page), _shown
			//
			, _countdownCounter, _countdownTimer
			//
			, _userKey, _expiryAsc = true
			//
			, _refresh = function() {

				var html = _view.getUserPageHtml('OutboxAddin', {
					jobTickets : true,
					userKey : _userKey,
					expiryAsc : _expiryAsc
				});

				if (html) {
					$('#jobticket-list').html(html).enhanceWithin();
					;
					$('.sparkline-printout-pie').sparkline('html', {
						type : 'pie',
						sliceColors : [_view.colorPrinter, _view.colorSheet]
					});
				}

				_countdownCounter = 1;

				return false;
			}
			//
			, _clearCountdownTimer = function() {
				if (_countdownTimer) {
					window.clearTimeout(_countdownTimer);
					_countdownTimer = null;
				}
			}
			//
			, _initCountdownTimer = function() {
				_clearCountdownTimer();
				_countdownCounter = 1;
				_countdownTimer = window.setInterval(function() {
					var width = 100 * _countdownCounter / 600;
					$('#sp-jobticket-countdown').width(width + '%');
					if (_countdownCounter++ === 600) {
						_countdownCounter = 1;
						_refresh();
					}
				}, 100);
			}
			//
			, _quickUserSearch = new _ns.QuickUserSearch(_view, _api)
			//
			, _onSelectUser = function(key, text) {
				$("#sp-jobticket-userid").val(text);
				_userKey = key;
				_refresh();
			}
			//
			, _onClearUser = function() {
				$("#sp-jobticket-userid").val('');
				_userKey = null;
				_refresh();
			}
			//
			;

			$(_self.id()).on('pagecreate', function(event) {

				var id = 'sp-jobticket-sort-dir';
				_view.checkRadio(id, _expiryAsc ? id + '-asc' : id + '-desc');

				$('#btn-jobtickets-refresh').click(function() {
					_refresh();
					return false;
				});

				$(this).on('click', '.sp-outbox-remove-jobticket', null, function() {
					var res = _api.call({
						request : 'jobticket-delete',
						dto : JSON.stringify({
							jobFileName : $(this).attr('data-savapage'),
						})
					});

					if (res.result.code === "0") {
						_refresh();
					}
					_view.showApiMsg(res);

				}).on('click', '.sp-outbox-preview-job', null, function() {

					_api.download("pdf-outbox", null, $(this).attr('data-savapage'));
					return false;

				}).on('click', '.sp-outbox-preview-jobticket', null, function() {

					_api.download("pdf-jobticket", null, $(this).attr('data-savapage'));
					return false;

				}).on('click', '.sp-jobticket-print', null, function() {

					var res = _api.call({
						request : 'jobticket-print',
						dto : JSON.stringify({
							jobFileName : $(this).attr('data-savapage'),
						})
					});

					if (res.result.code === "0") {
						_refresh();
					}
					_view.showApiMsg(res);

				}).on('change', "input[name='sp-jobticket-sort-dir']", null, function() {

					_expiryAsc = $(this).attr('id') === 'sp-jobticket-sort-dir-asc';
					_refresh();
					return false;

				}).on('click', "#btn-jobtickets-back", null, function() {
					/*
					 *  onBack() is injected by the Job Ticket Web App, but not by the Admin Web App host of this page.
					 */
					if (_self.onBack) {
						return _self.onBack();
					}
					return true;
				});
				
				_quickUserSearch.onCreate($(this), 'sp-jobticket-userid-filter', _onSelectUser, _onClearUser);

			}).on("pageshow", function(event, ui) {
				if (!_shown) {
					_initCountdownTimer();
					_shown = true;
					_refresh();
				}
			}).on("pagehide", function(event, ui) {
				_clearCountdownTimer();
			});

			return _self;
		};

	}(jQuery, this, this.document, JSON, this.org.savapage));
