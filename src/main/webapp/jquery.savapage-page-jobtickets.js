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
			, _self = _ns.derive(_page)
			//
			, _countdownCounter, _countdownTimer
			//
			, _MODE_PRINT = '0', _MODE_CANCEL = '1'
			//
			, _userKey, _expiryAsc = true
			//
			, _quickUserSearch = new _ns.QuickUserSearch(_view, _api)
			//
			, _refresh = function() {

				var html = _view.getUserPageHtml('OutboxAddin', {
					jobTickets : true,
					userKey : _userKey,
					expiryAsc : _expiryAsc
				});

				if (html) {
					$('#jobticket-list').html(html).enhanceWithin();
					$('.sparkline-printout-pie').sparkline('html', {
						type : 'pie',
						sliceColors : [_view.colorPrinter, _view.colorSheet]
					});
				}

				_view.enable($('.sp-jobtickets-all'), $('.sp-outbox-job-entry').length > 0);

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
			, _onSelectUser = function(quickUserSelected) {
				$("#sp-jobticket-userid").val(quickUserSelected.text);
				_userKey = quickUserSelected.key;
				_refresh();
			}
			//
			, _onClearUser = function() {
				$("#sp-jobticket-userid").val('');
				_userKey = null;
				_refresh();
			}
			//
			, _onPrintPopup = function(jobFileName, positionTo) {
				var html = _view.getPageHtml('JobTicketPrintAddIn', {
					jobFileName : jobFileName
				}) || 'error';

				$('#sp-jobticket-popup-addin').html(html);
				$('#sp-jobticket-popup').enhanceWithin().popup('open', {
					positionTo : positionTo
				});
			}
			//
			, _cancelJob = function(jobFileName) {
				return _api.call({
					request : 'jobticket-delete',
					dto : JSON.stringify({
						jobFileName : jobFileName
					})
				});
			}
			//
			, _printJob = function(jobFileName, printerId) {
				return _api.call({
					request : 'jobticket-print',
					dto : JSON.stringify({
						jobFileName : jobFileName,
						printerId : printerId
					})
				});
			}
			//
			, _onProcessAll = function(mode) {

				var logPfx = (mode === _MODE_PRINT ? 'Print' : 'Cancel')
				//
				, popup = (mode === _MODE_PRINT ? $('#sp-jobticket-popup-print-all') : $('#sp-jobticket-popup-cancel-all'))
				//
				, tickets = $('.sp-outbox-cancel-jobticket');

				popup.find('.ui-content:eq(0)').hide();
				popup.find('.ui-content:eq(1)').show();

				// Stalled async execution: long enough to show the busy message to user.
				window.setTimeout(function() {
					tickets.each(function() {

						var res, msg;

						if (mode === _MODE_PRINT) {
							res = _printJob($(this).attr('data-savapage'));
						} else {
							res = _cancelJob($(this).attr('data-savapage'));
						}

						if (res && res.result.code !== "0") {
							msg = res.result.txt || 'unknown';
							_ns.logger.warn(logPfx + ' Job Ticket error: ' + msg);
							popup.find('.sp-progress-all-error').append(msg + '</br>');
						}
					});
					_refresh();
					popup.popup('close');
				}, 1500);
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

				$(this).on('click', '.sp-outbox-cancel-jobticket', null, function() {
					var res = _cancelJob($(this).attr('data-savapage'));
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
					_onPrintPopup($(this).attr('data-savapage'), $(this));

				}).on('change', "input[name='sp-jobticket-sort-dir']", null, function() {
					_expiryAsc = $(this).attr('id') === 'sp-jobticket-sort-dir-asc';
					_refresh();
					return false;

				}).on('click', '#btn-jobtickets-cancel-all', null, function() {
					var dlg = $('#sp-jobticket-popup-cancel-all');

					dlg.find('.ui-content:eq(1)').hide();
					dlg.find('.ui-content:eq(0)').show();

					dlg.popup('open', {
						positionTo : $(this)
					});
					$("#sp-jobticket-popup-cancel-all-btn-no").focus();

				}).on('click', '#btn-jobtickets-print-all', null, function() {
					var dlg = $('#sp-jobticket-popup-print-all');

					dlg.find('.ui-content:eq(1)').hide();
					dlg.find('.ui-content:eq(0)').show();

					dlg.popup('open', {
						positionTo : $(this)
					});
					$("#sp-jobticket-popup-print-all-btn-no").focus();

				}).on('click', "#btn-jobtickets-close", null, function() {
					if (_self.onClose) {
						return _self.onClose();
					}
					return true;

				}).on('click', "#sp-jobticket-popup-btn-print", null, function() {
					var res = _printJob($(this).attr('data-savapage'), _view.getRadioValue('sp-jobticket-redirect-printer'));
					if (res.result.code === "0") {
						$('#sp-jobticket-popup').popup('close');
						_refresh();
					}
					_view.showApiMsg(res);
				}).on('click', '#sp-jobticket-popup-cancel-all-btn-yes', null, function() {
					_onProcessAll(_MODE_CANCEL);
				}).on('click', '#sp-jobticket-popup-print-all-btn-yes', null, function() {
					_onProcessAll(_MODE_PRINT);
				});

				_quickUserSearch.onCreate($(this), 'sp-jobticket-userid-filter', _onSelectUser, _onClearUser);

			}).on("pageshow", function(event, ui) {
				_initCountdownTimer();
				_refresh();
			}).on("pagehide", function(event, ui) {
				_clearCountdownTimer();
			});

			return _self;
		};

	}(jQuery, this, this.document, JSON, this.org.savapage));
