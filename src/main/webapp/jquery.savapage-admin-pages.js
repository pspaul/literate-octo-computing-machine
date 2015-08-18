/*! SavaPage jQuery Mobile Admin Pages | (c) 2011-2015 Datraverse B.V. | GNU Affero General Public License */

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

/*
 * SavaPage jQuery Mobile Admin Pages
 */
( function($, window, document, JSON, _ns) {"use strict";

		/* jslint browser: true */
		/* global $, jQuery, alert */

		// =========================================================================
		/**
		 * Constructor
		 */
		_ns.PageUser = function(_i18n, _view, _model) {

			var _page = new _ns.Page(_i18n, _view, '#page-user', 'admin.PageUser')
			//
			, _self = _ns.derive(_page)
			//
			, _onChangeCreditLimit = function(creditLimit) {
				_view.visible($('#user-account-credit-limit-amount'), creditLimit === "INDIVIDUAL");
			}
			//
			;

			// -------------------------------------------------------------
			// DOCUMENTATION for Callback Functions
			// Do NOT remove the commented documentation block below
			// -------------------------------------------------------------
			// _self.onSaveUser = null;
			// _self.DeleteUser = null;

			/**
			 *
			 */
			$(_self.id()).on('pagecreate', function(event) {

				$(this).on('click', '#button-save-user', null, function() {
					_self.onSaveUser();
					return false;
				});

				$(this).on('click', '#button-delete-user', null, function() {
					_self.onDeleteUser();
					return false;
				});

				$(this).on('click', '#button-user-pw-dialog', null, function() {
					_view.showPageAsync('#page-user-pw-reset', 'UserPasswordReset', function() {
						$('#user-pw-reset-title').html(_model.editUser.userName);
					});
					return false;
				});

				$(this).on('change', "input:radio[name='user-account-credit-limit-type']", null, function(e) {
					_onChangeCreditLimit($(this).val());
				});

			}).on("pagebeforeshow", function(event, ui) {
				// See Java org.savapage.dto.UserDto

				var emailOther
				//
				, accounting = _model.editUser.accounting
				//
				;

				if (_model.editUser.emailOther) {
					$.each(_model.editUser.emailOther, function(key, val) {
						if (emailOther) {
							emailOther += "\n";
						} else {
							emailOther = "";
						}
						emailOther += val.address;
					});
				}
				$('#user-email-other').val(emailOther);

				//
				$('#user-account-balance').val(accounting.balance);
				$('#user-account-credit-limit-amount').val(accounting.creditLimitAmount);
				_view.checkRadioValue("user-account-credit-limit-type", accounting.creditLimit);
				_onChangeCreditLimit(accounting.creditLimit);

				//
				$('#user-email').val(_model.editUser.email);
				$('#user-fullname').val(_model.editUser.fullName);
				$('#user-userid').val(_model.editUser.userName);
				$('#user-card-number').val(_model.editUser.card);
				$('#user-id-number').val(_model.editUser.id);
				$('#user-pin').val(_model.editUser.pin);

				$('#user-user-pw').val('');
				$('#user-user-pw-confirm').val('');

				_view.checkCb('#user-isadmin', _model.editUser.admin);
				_view.checkCb('#user-isperson', _model.editUser.person);
				_view.checkCb('#user-disabled', _model.editUser.disabled);

				_view.visible($('.sp-user-edit'), _model.editUser.dbId);
				_view.visible($('.sp-user-create'), !_model.editUser.dbId);
				_view.visible($('.sp-internal-user'), _model.editUser.internal);
				_view.visible($('.sp-internal-user-edit'), _model.editUser.internal && _model.editUser.dbId);

				if (_model.editUser.dbId) {
					$('#user-fullname').focus();
				} else {
					$('#user-userid').focus();
				}

			});
			return _self;
			// IMPORTANT
		};

		// =========================================================================
		/**
		 * Constructor
		 */
		_ns.PageAccountVoucherCreate = function(_i18n, _view, _model) {

			var _page = new _ns.Page(_i18n, _view, '#page-voucher-create', 'admin.PageAccountVoucherCreate')
			//
			, _self = _ns.derive(_page);

			// -------------------------------------------------------------
			// DOCUMENTATION for Callback Functions
			// Do NOT remove the commented documentation block below
			// -------------------------------------------------------------
			// _self.onCreateBatch = null;
			// _self.onCreateBatchExit = null;

			/**
			 *
			 */
			$(_self.id()).on('pagecreate', function(event) {

				_view.mobipick($("#sp-voucher-create-expiration"));

				$(this).on('click', '#button-voucher-batch-create-print', null, function() {

					var sel = $("#sp-voucher-create-expiration")
					//
					, date = _view.mobipickGetDate(sel)
					//
					, present = (sel.val().length > 0)
					//
					;
					date = ( present ? date.getTime() : null);

					_self.onCreateBatch({
						batchId : $("#sp-voucher-create-batch-id").val(),
						number : $("#sp-voucher-create-number").val(),
						value : $("#sp-voucher-create-value").val(),
						expiryDate : date
					}, $("#sp-voucher-card-create-print-format").val());
					return false;
				});

			}).on("pagebeforeshow", function(event, ui) {
				_view.mobipickSetDate($('#sp-voucher-create-expiration'), null);
				$("#sp-voucher-create-batch-id").val('');
				$("#sp-voucher-create-number").val('');
				$("#sp-voucher-create-value").val('');
			}).on('pagebeforehide', function(event, ui) {
				_self.onCreateBatchExit();
			});
			/*
			 * IMPORTANT
			 */
			return _self;
		};

		// =========================================================================
		/**
		 * Constructor
		 */
		_ns.PageConfigProp = function(_i18n, _view, _model) {

			var _page = new _ns.Page(_i18n, _view, '#page-config-prop', 'admin.PageConfigProp')
			//
			, _self = _ns.derive(_page);

			// -------------------------------------------------------------
			// DOCUMENTATION for Callback Functions
			// Do NOT remove the commented documentation block below
			// -------------------------------------------------------------
			// _self.onSave = null;

			/**
			 *
			 */
			$(_self.id()).on('pagecreate', function(event) {

				$(this).on('click', '#button-save-configprop', null, function() {
					_self.onSave();
					return false;
				});

			}).on("pagebeforeshow", function(event, ui) {
				var
				//
				val = _model.editConfigProp.value
				//
				, single = $('#config-prop-value')
				//
				, multi = $('#config-prop-value-multiline')
				//
				;

				if (_model.editConfigProp.multiline) {
					multi.val(val).show();
					single.hide();
				} else {
					multi.hide();
					single.val(val).show();
				}

			});
			/*
			 * IMPORTANT
			 */
			return _self;
		};

		// =========================================================================
		/**
		 * Constructor
		 */
		_ns.PageDevice = function(_i18n, _view, _model) {

			var _m2v, _v2m, _m2vAuthDefault, _m2vCardFormat
			//
			, _m2vProxyPrintAuth, _v2mProxyPrintAuth
			//
			, _page = new _ns.Page(_i18n, _view, '#page-device', 'admin.PageDevice')
			//
			, _self = _ns.derive(_page)
			//
			//,                                                       _this = this
			//
			, _onAuthModeEnabled, _onProxyPrintEnabled, _onCustomAuthEnabled
			//
			, _enableCardFormat
			//
			, _ATTR_PFX = 'auth-mode'
			//
			, _AUTH_MODE_DEFAULT = _ATTR_PFX + '-default'
			//
			, _AUTH_MODE_CUSTOM = _ATTR_PFX + '-is-custom'
			//
			, _AUTH_MODE_CARD_LOCAL = _ATTR_PFX + '.card-local'
			//
			, _PROXY_PRINT_AUTH_MODE = 'proxy-print.auth-mode'
			//
			, _WEBAPP_USER_IDLE_SECS = 'webapp.user.max-idle-secs'
			// boolean authentication attributes
			, _AUTH_ATTR_BOOLS = ['.name', '.id', '.id.pin-required', '.id.is-masked', '.card-local', '.card-ip', '.card.pin-required', '.card.self-association']
			// string authentication attributes
			, _AUTH_ATTR_STRINGS = []
			//
			, _CARD_FORMAT_ATTR = ['.format', '.first-byte']
			//
			;

			// -------------------------------------------------------------
			// DOCUMENTATION for Callback Functions
			// Do NOT remove the commented documentation block below
			// -------------------------------------------------------------
			// _self.onSaveDevice = null;

			_onAuthModeEnabled = function() {
				var authUser = _view.isCbChecked($("#auth-mode\\.name"))
				//
				, authId = _view.isCbChecked($("#auth-mode\\.id"))
				//
				, authCardLocal = _view.isCbChecked($("#auth-mode\\.card-local"))
				//
				, authCardIp = _view.isCbChecked($("#auth-mode\\.card-ip"))
				//
				, sel
				//
				, nMode = 0
				//
				;

				$('#auth-mode-default-user').checkboxradio( authUser ? 'enable' : 'disable');
				$('#auth-mode-default-id').checkboxradio( authId ? 'enable' : 'disable');
				$('#auth-mode-default-card-local').checkboxradio( authCardLocal ? 'enable' : 'disable');
				$('#auth-mode-default-card-network').checkboxradio( authCardIp ? 'enable' : 'disable');

				if (authUser) {
					nMode++;
				}

				sel = $('#sp-group-device-auth-mode-id');
				if (authId) {
					nMode++;
					sel.show();
				} else {
					sel.hide();
				}

				if (authCardLocal) {
					nMode++;
				}

				_view.visible($('#sp-group-device-auth-mode-card'), authCardLocal || authCardIp);

				sel = $('#sp-group-device-auth-mode-card-network');
				if (authCardIp) {
					nMode++;
					sel.show();
				} else {
					sel.hide();
				}

				_view.visible($('#sp-group-device-auth-mode-default'), nMode > 1);
				_enableCardFormat(authCardLocal);

			};

			_onProxyPrintEnabled = function(cb) {
				_view.visible($('#sp-device-proxy-print-group-fields'), _view.isCbChecked(cb));
				_view.visible($('#sp-device-proxy-print-auth-fields'), _view.isCbChecked(cb) && _model.editDevice.deviceType === 'CARD_READER');
			};

			_onCustomAuthEnabled = function(cb) {
				var customAuth = _view.isCbChecked(cb)
				//
				, authCardLocal = _view.isCbChecked($("#auth-mode\\.card-local"))
				//
				;
				_view.visible($('.sp-device-auth-mode-custom-group'), customAuth);

				if (customAuth) {
					_onAuthModeEnabled();
				}
				_enableCardFormat(customAuth && authCardLocal);
			};

			_enableCardFormat = function(enable) {
				_view.visible($('#sp-group-device-card-format'), enable);
			};

			_v2mProxyPrintAuth = function(attr) {
				var name = _PROXY_PRINT_AUTH_MODE
				//
				, id = name.replace(/\./g, '\\.')
				//
				;
				attr[name] = _view.getRadioValue(id);
			};

			_m2vProxyPrintAuth = function() {
				var attr = _PROXY_PRINT_AUTH_MODE
				//
				, id = attr.replace(/\./g, '\\.')
				//
				, val = _model.editDevice.attr[attr]
				//
				;
				if (val) {
					_view.checkRadio(attr, id + '-' + val.toLowerCase());
				}
			};

			_m2vAuthDefault = function() {
				_view.checkRadioValue(_AUTH_MODE_DEFAULT, _model.editDevice.attr[_AUTH_MODE_DEFAULT]);
			};

			_m2vCardFormat = function(attr) {
				var val = _model.editDevice.attr[attr]
				//
				, radio, id
				//
				;
				if (val === 'HEX') {
					radio = 'hex';
				} else if (val === 'DEC') {
					radio = 'dec';
				} else if (val === 'LSB') {
					radio = 'lsb';
				} else if (val === 'MSB') {
					radio = 'msb';
				}
				id = attr.replace(/\./g, '\\.');
				_view.checkRadio(attr, id + '-' + radio);
			};

			/**
			 * Model to View.
			 */
			_m2v = function() {
				var device = _model.editDevice
				//
				, customAuth, showCardFormat
				//
				, attrBool = [], attrString = [], attrFullString = []
				// force evaluation to boolean
				, isProxyPrint = (undefined !== (device.printerName || device.printerGroup))
				//
				;

				$('#sp-device-name').val(device.deviceName);
				$('#sp-device-hostname').val(device.hostname);
				$('#sp-device-port').val(device.port);
				$('#sp-device-location').val(device.location);
				$('#sp-device-notes').val(device.notes);
				_view.checkCb('#sp-device-disabled', device.disabled);

				$('#sp-device-proxy-print-printer').val(device.printerName);
				$('#sp-device-proxy-print-printer-group').val(device.printerGroup);

				_view.checkCb('#sp-device-function-proxy-print', isProxyPrint);

				//
				if (device.deviceType === 'TERMINAL') {

					customAuth = device.attr[_AUTH_MODE_CUSTOM] || false;

					_view.checkCb('#' + _AUTH_MODE_CUSTOM, customAuth);

					if (customAuth) {
						attrBool = _AUTH_ATTR_BOOLS;
						attrString = _AUTH_ATTR_STRINGS;
						$('#sp-device-assoc-card-reader').val(device.readerName);
						_m2vAuthDefault();

						showCardFormat = device.attr[_AUTH_MODE_CARD_LOCAL];
					}

					attrFullString = [_WEBAPP_USER_IDLE_SECS];

				} else {
					showCardFormat = true;
					_m2vProxyPrintAuth();
				}

				if (showCardFormat) {
					_m2vCardFormat('card.number.format');
					_m2vCardFormat('card.number.first-byte');
				}

				attrBool.forEach(function(sfx) {
					var name = _ATTR_PFX + sfx
					// escape ALL the points '.'
					, id = name.replace(/\./g, '\\.')
					//
					;
					_view.checkCb('#' + id, device.attr[name]);
					//console.log(name + ' : ' + attr[name]);
				});
				attrString.forEach(function(sfx) {
					var name = _ATTR_PFX + sfx
					// escape ALL the points '.'
					, id = name.replace(/\./g, '\\.')
					//
					;
					$('#' + id).val(device.attr[name]);
				});
				attrFullString.forEach(function(name) {
					// escape ALL the points '.'
					var id = name.replace(/\./g, '\\.')
					//
					;
					$('#' + id).val(device.attr[name]);
				});

				//
				_view.visible($('.sp-device-edit'), device.id);
				_view.visible($('.sp-device-terminal'), device.deviceType === 'TERMINAL');
				_view.visible($('.sp-device-card-reader'), device.deviceType === 'CARD_READER');
				_view.visible($('#sp-group-device-card-format'), device.deviceType === 'CARD_READER');

				_onProxyPrintEnabled($("#sp-device-function-proxy-print"));
				_onCustomAuthEnabled($('#' + _AUTH_MODE_CUSTOM));

				_view.visible($('#sp-device-function-proxy-print-group'), !device.terminalName);
				_view.visible($('#sp-device-card-reader-user-auth'), device.terminalName);

				$('#sp-device-card-reader-terminal').text(device.terminalName);

			};

			/**
			 * View to Model.
			 */
			_v2m = function() {
				var device = _model.editDevice
				//
				, attr = {}, attrBool = [], attrString = []
				//
				, attrCardNumberFormat = [], attrFullString = []
				//
				, customAuth, proxyPrint
				//
				;

				device.deviceName = $('#sp-device-name').val();
				device.displayName = $('#sp-device-name').val();
				device.hostname = $('#sp-device-hostname').val();
				device.port = parseInt($('#sp-device-port').val(), 10);
				device.location = $('#sp-device-location').val();
				device.notes = $('#sp-device-notes').val();
				device.disabled = _view.isCbChecked($('#sp-device-disabled'));

				device.printerGroup = undefined;
				device.printerName = undefined;
				device.readerName = undefined;

				proxyPrint = _view.isCbChecked($('#sp-device-function-proxy-print'));

				if (proxyPrint) {
					device.printerName = $('#sp-device-proxy-print-printer').val();
					device.printerGroup = $('#sp-device-proxy-print-printer-group').val();
				}

				if (device.deviceType === 'TERMINAL') {

					customAuth = _view.isCbChecked($('#' + _AUTH_MODE_CUSTOM));
					attr[_AUTH_MODE_CUSTOM] = customAuth;

					if (customAuth) {
						attrBool = _AUTH_ATTR_BOOLS;
						attrString = _AUTH_ATTR_STRINGS;
						device.readerName = $('#sp-device-assoc-card-reader').val();
						attr[_AUTH_MODE_DEFAULT] = _view.getRadioValue(_AUTH_MODE_DEFAULT);
					}
					attrFullString = [_WEBAPP_USER_IDLE_SECS];
				} else {
					if (proxyPrint) {
						_v2mProxyPrintAuth(attr);
					}
				}

				attrBool.forEach(function(sfx) {
					var name = _ATTR_PFX + sfx
					// escape ALL the points '.'
					, id = name.replace(/\./g, '\\.')
					//
					;
					attr[name] = _view.isCbChecked($('#' + id));
					//console.log(name + ' : ' + attr[name]);
				});

				attrString.forEach(function(sfx) {
					var name = _ATTR_PFX + sfx
					// escape ALL the points '.'
					, id = name.replace(/\./g, '\\.')
					//
					;
					attr[name] = $('#' + id).val();
					//console.log(name + ' : ' + attr[name]);
				});
				attrFullString.forEach(function(name) {
					// escape ALL the points '.'
					var id = name.replace(/\./g, '\\.')
					//
					;
					attr[name] = $('#' + id).val();
				});

				if (device.deviceType === 'CARD_READER' || (customAuth && attr[_AUTH_MODE_CARD_LOCAL])) {
					attrCardNumberFormat = _CARD_FORMAT_ATTR;
				}

				attrCardNumberFormat.forEach(function(sfx) {
					var name = 'card.number' + sfx
					// escape ALL the points '.'
					, id = name.replace(/\./g, '\\.')
					//
					;
					attr[name] = _view.getRadioValue(id);
					//console.log(name + ' : ' + attr[name]);
				});

				device.attr = attr;
			};

			/**
			 *
			 */
			$(_self.id()).on('pagecreate', function(event) {

				$(this).on('click', '#button-save-device', null, function() {
					_v2m();
					_self.onSaveDevice(_model.editDevice);
					return false;
				});

				$(this).on('click', '#button-delete-device', null, function() {
					_self.onDeleteDevice(_model.editDevice);
					return false;
				});

				$(this).on('change', "#sp-group-device-auth-mode input:checkbox", null, function(e) {
					_onAuthModeEnabled();
				});

				$(this).on('change', "#sp-device-function-proxy-print", null, function(e) {
					_onProxyPrintEnabled($(e.target));
				});

				$(this).on('change', "#auth-mode-is-custom", null, function(e) {
					_onCustomAuthEnabled($(e.target));
				});

			}).on("pagebeforeshow", function(event, ui) {
				_m2v();
			});
			/*
			 * IMPORTANT
			 */
			return _self;
		};

		// =========================================================================
		/**
		 * Constructor
		 */
		_ns.PageQueue = function(_i18n, _view, _model) {

			var _page = new _ns.Page(_i18n, _view, '#page-queue', 'admin.PageQueue')
			//
			, _self = _ns.derive(_page)
			//
			//, _this = this
			//
			;

			// -------------------------------------------------------------
			// DOCUMENTATION for Callback Functions
			// Do NOT remove the commented documentation block below
			// -------------------------------------------------------------
			// _self.onSaveQueue = null;

			/**
			 *
			 */
			$(_self.id()).on('pagecreate', function(event) {

				$(this).on('click', '#button-save-queue', null, function() {
					_self.onSaveQueue();
					return false;
				});

			}).on("pagebeforeshow", function(event, ui) {
				var reserved = _model.editQueue.reserved
				//
				, sect = $('#queue_reserved_section')
				//
				;
				$('#queue-url-path').val(_model.editQueue.urlpath);
				$('#queue-ip-allowed').val(_model.editQueue.ipallowed);
				_view.checkCb('#queue-trusted', _model.editQueue.trusted);
				_view.checkCb('#queue-disabled', _model.editQueue.disabled);
				_view.checkCb('#queue-deleted', _model.editQueue.deleted);

				if (reserved) {
					sect.show();
					$('#queue_reserved').html(reserved);
				} else {
					sect.hide();
				}

				$('#queue-header').text(_model.editQueue.uiText);

				_view.visible($('.queue_user-defined-section'), !reserved);
				_view.enable($('#queue-url-path'), !reserved);

			});
			/*
			 * IMPORTANT
			 */
			return _self;
		};

		// =========================================================================
		/**
		 * Constructor
		 */
		_ns.PagePrinter = function(_i18n, _view, _model) {

			var _page = new _ns.Page(_i18n, _view, '#page-printer', 'admin.PagePrinter')
			//
			, _self = _ns.derive(_page)
			//
			, _onChangeChargeType, _showAllMediaRows;

			// -------------------------------------------------------------
			// DOCUMENTATION for Callback Functions
			// Do NOT remove the commented documentation block below
			// -------------------------------------------------------------
			// _self.onSavePrinter = null;
			// _self.onRenamePrinter = null;

			_onChangeChargeType = function(chargeType) {
				var isSimple = (chargeType === 'SIMPLE');
				if (!isSimple) {
					_showAllMediaRows(false);
				}
				_view.visible($('.sp-printer-charge-simple'), isSimple);
				_view.visible($('.sp-printer-charge-media'), !isSimple);
			};

			/**
			 *
			 */
			_showAllMediaRows = function(showAll) {
				var buttonLess = $('#sp-printer-cost-media-less')
				//
				, buttonMore = $('#sp-printer-cost-media-more')
				//
				;

				$(".sp-printer-cost-media-row").each(function() {
					if (showAll) {
						$(this).show();
						buttonLess.show();
						buttonMore.hide();
					} else {
						if ($(this).find("input:checkbox").is(':checked')) {
							$(this).show();
						} else {
							$(this).hide();
						}
						buttonMore.show();
						buttonLess.hide();
					}
				});
			};

			/**
			 *
			 */
			$(_self.id()).on('pagecreate', function(event) {

				$(this).on('click', '#button-save-printer', null, function() {
					_self.onSavePrinter();
					return false;
				});

				$(this).on('click', '#sp-printer-cost-media-less', null, function() {
					_showAllMediaRows(false);
					return false;
				});

				$(this).on('click', '#sp-printer-cost-media-more', null, function() {
					_showAllMediaRows(true);
					return false;
				});

				$(this).on('click', '#button-save-printer-cost', null, function() {
					_self.onSavePrinterCost();
					return false;
				});

				$(this).on('click', '#button-save-printer-media-sources', null, function() {
					_self.onSavePrinterMediaSources();
					return false;
				});

				$(this).on('click', '#button-rename-printer', null, function() {
					_self.onRenamePrinter(_model.editPrinter.id, $('#printer-newname').val(), $('#printer-rename-replace').is(':checked'));
					return false;
				});

				$(this).on('change', "input:radio[name='sp-printer-charge-type']", null, function(e) {
					_onChangeChargeType($(this).val());
				});

				$(this).on('change', ".sp-printer-cost-media", null, function(e) {
					var input = $(this).closest("tr").find("input");
					if ($(this).is(':checked')) {
						input.show();
					} else {
						input.hide();
					}
				});

			}).on("pagebeforeshow", function(event, ui) {

				var accounting = $('#sp-printer-accounting-addin')
				//
				, mediaSource = $('#sp-printer-media-source-addin')
				//
				, data = {}
				//
				;

				$('#printer-displayname').val(_model.editPrinter.displayName);
				$('#printer-location').val(_model.editPrinter.location);
				$('#printer-printergroups').val(_model.editPrinter.printerGroups);

				_view.checkCb('#printer-disabled', _model.editPrinter.disabled);
				_view.checkCb('#printer-deleted', _model.editPrinter.deleted);

				$('#printer-newname').val('');
				_view.checkCb('#printer-rename-replace', false);

				_view.visible($('.printer-not-present'), !_model.editPrinter.present);

				/*
				 * Accounting.
				 */

				data.id = _model.editPrinter.id;

				accounting.html(_view.getAdminPageHtml('PrinterAccountingAddin', data)).enhanceWithin();

				/*
				 * Media sources.
				 */
				mediaSource.html(_view.getAdminPageHtml('PrinterMediaSourceAddin', data)).enhanceWithin();

				//
				_onChangeChargeType(_view.getRadioValue('sp-printer-charge-type'));
			});
			/*
			 * IMPORTANT
			 */
			return _self;
		};

		// =========================================================================
		/**
		 * Upload Membership Card
		 */
		_ns.PageMemberCardUpload = function(_i18n, _view, _model) {

			var
			// Page is pre-loaded, so no _class needed
			_page = new _ns.Page(_i18n, _view, '#page-membercard-upload')
			//
			, _self = _ns.derive(_page)
			//
			, _submitted = false
			//
			;

			/**
			 *
			 */
			$(_self.id()).on('pagecreate', function(event) {
				$('#membercard-import-feedback').hide();
				// initial hide
				$('#button-membercard-import-submit').on('click', null, null, function() {
					_submitted = true;
					$('#membercard-import-feedback').show();
					return true;
				});
			}).on("pagebeforeshow", function(event, ui) {
				_submitted = false;
			}).on('pagebeforehide', function(event, ui) {
				/*
				 * Clear and Hide content
				 */
				$('#membercard-import-feedback').hide();
				$('#membercard-import-feedback').html('');
				$('#button-membercard-import-reset').click();

				if (_submitted) {
					_view.pages.admin.reloadPanel();
					/*
					 * Expand the collapsed section
					 */
					$('#cat-membership').click();
				}
			});
			return _self;
			// IMPORTANT
		};
		/**
		 * Constructor
		 */
		_ns.PageAdmin = function(_i18n, _view, _model) {

			var _page = new _ns.Page(_i18n, _view, '#page-admin', 'admin.Main')
			//
			, _self = _ns.derive(_page)
			//
			, _panel
			//
			, _refreshPanel, _refreshPanelByUrl, _refreshPanelCommon
			//
			, _loadListPage, _loadListPageByUrl
			//
			, _panelCur = null, _panelCurClass = null, _loadPanel
			//
			;

			//_self.onDisconnected()
			//_self.onClose()

			_self.reloadPanel = function() {
				_loadPanel(_panelCurClass);
			};

			_self.initView = function() {
				_loadPanel('Dashboard');
				// _loadPanel('ConfigPropBase');
				// _loadPanel("AccountVoucherBase");
			};

			// -------------------------------------------------------------
			// DOCUMENTATION for Callback Functions
			// Do NOT remove the commented documentation block below
			// -------------------------------------------------------------
			// _self.onApplyAuth = null;
			// _self.onApplySmtp = null;
			// _self.onApplyMail = null;
			// _self.onApplyUserCreate = null;
			// _self.onApplyLocale = null;
			// _self.onApplyBackupAuto = null;
			// _self.onAdminPwReset = null;
			// _self.onBackupNow = null;
			// _self.onEditUser = null;
			// _self.onEditConfigProp = null;
			// _self.onCreateQueue = null;
			// _self.onEditQueue = null;
			// _self.onEditPrinter = null;
			// _self.onPrinterSynchr = null;
			// _self.onUserSourceGroupAll = null;
			// _self.onUserSourceGroupApply = null;
			// _self.onUserSourceGroupCancel = null;
			// _self.onUserSourceGroupEdit = null;
			// _self.onUserSyncApply = null;
			// _self.onUserSyncNow = null;
			// _self.onUserSyncTest = null;
			// _self.onPagometerReset = null;
			// _self.onVoucherDeleteBatch
			// _self.onVoucherExpireBatch
			// _self.onVoucherDeleteExpired
			// _self.onDownload

			/*
			 * The map of panels. The key is the Java Wicket class. Each key has
			 * the
			 * following attributes.
			 *
			 * getInput : function(my) {}
			 *
			 * onOutput : function(my, output) {}
			 */
			_panel = {

				/*
				 * About
				 */
				About : {},

				/*
				 * AccountTrx (common for Admin and User WebApp)
				 */
				AccountTrxBase : _ns.PanelAccountTrxBase,

				/*
				 * AccountVoucher
				 */
				AccountVoucherBase : _ns.PanelAccountVoucherBase,

				/*
				 * AppLog
				 */
				AppLogBase : _ns.PanelAppLogBase,

				/*
				 * ConfigProp
				 */
				ConfigPropBase : _ns.PanelConfigPropBase,

				/*
				 * Dashboard
				 */
				Dashboard : _ns.PanelDashboard,

				/*
				 * Devices
				 */
				DevicesBase : _ns.PanelDevicesBase,

				/*
				 * DocLog (common for Admin and User WebApp)
				 */
				DocLogBase : _ns.PanelDocLogBase,

				/*
				 * Options
				 */
				Options : _ns.PanelOptions,

				/*
				 * Printers
				 */
				PrintersBase : _ns.PanelPrintersBase,

				/*
				 *
				 */
				QueuesBase : _ns.PanelQueuesBase,

				/*
				 *
				 */
				Reports : {},

				/*
				 *
				 */
				UsersBase : _ns.PanelUsersBase

			};

			_self.refreshUsers = function() {
				var pnl = _panel.UsersBase;
				pnl.refresh(pnl);
			};
			_self.refreshConfigProps = function() {
				var pnl = _panel.ConfigPropBase;
				pnl.refresh(pnl);
			};
			_self.refreshQueues = function() {
				var pnl = _panel.QueuesBase;
				pnl.refresh(pnl);
			};
			_self.refreshDevices = function() {
				var pnl = _panel.DevicesBase;
				pnl.refresh(pnl);
			};
			_self.refreshPrinters = function() {
				var pnl = _panel.PrintersBase;
				pnl.refresh(pnl);
			};
			_self.refreshAccountVouchers = function() {
				var pnl = _panel.AccountVoucherBase;
				pnl.refresh(pnl);
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
			 * call.
			 * Remember: we always get HTML back to display the panel.
			 */
			_refreshPanelByUrl = function(url, wClass, skipBeforeLoad) {

				var
				//
				panel = _panel[wClass]
				//
				, data = null
				//
				, jsonData = null
				//
				;

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
				 * +
				 * wClass
				 */
				$.mobile.loading("show");
				$.ajax({
					type : "POST",
					async : true,
					url : '/pages/' + url + wClass,
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
						//panel.onOutput(panel, $.parseJSON($('.content-primary
						// .json-rsp').text()));
						panel.onOutput(panel, undefined);
					}

					/*
					 * IMPORTANT: afterload() SHOULD take care of hiding the
					 * spinner. This is (usually) achieved by retrieving the
					 * first
					 * page of the list.
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
					//}).always(function() {
					//$.mobile.loading( "hide" );
				});

			};

			/**
			 * Load an admin panel: defaults are applied.
			 */
			_loadPanel = function(wClass) {
				var panel = _panel[wClass];

				if (panel.applyDefaults) {
					panel.applyDefaults(panel);
				}

				if (_panelCur && _panelCur.onUnload) {
					_panelCur.onUnload(_panelCur);
				}
				if (wClass === 'DocLogBase' || wClass === 'AccountTrxBase') {
					_refreshPanelCommon(wClass);
				} else {
					_refreshPanel(wClass);
				}

				/*
				 * IMPORTANT for MOBILE Devices: this scrolls the primary content
				 * into view.
				 */
				$("#sp-a-content-button").click();
			};

			/**
			 * Loads a page of a list.
			 */
			_loadListPage = function(wClass, wClassPage, jqId) {
				_loadListPageByUrl(wClass, 'admin/' + wClassPage, jqId);
			};

			/**
			 * Loads a page of a list.
			 */
			_loadListPageByUrl = function(wClass, wClassPage, jqId) {

				var
				//
				panel = _panel[wClass]
				//
				, data = null
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
						user : _model.user.id,
						data : jsonData
					}
				}).done(function(html) {
					$(jqId).html(html).enhanceWithin();
					if (panel.onOutput) {
						panel.onOutput(panel, $.parseJSON($(jqId + ' .json-rsp').text()));
					}
				}).fail(function() {
					_self.onDisconnected();
				}).always(function() {
					$.mobile.loading("hide");
				});
			};

			/*
			 * Common Panel parameters to be set be the client.
			 */
			_ns.PanelCommon.view = _view;
			_ns.PanelCommon.userId = _model.user.id;
			_ns.PanelCommon.refreshPanelCommon = _refreshPanelCommon;
			_ns.PanelCommon.refreshPanelAdmin = _refreshPanel;

			/**
			 *
			 */
			$(_self.id()).on('pagebeforeshow', function(event, ui) {
				/*
				 * Wow, this took some time to find out. Since the 'popup' is
				 * created
				 * ad-hoc to display the message, timing is important here... We
				 * cannot expect the popup is created all right at this stage.
				 *
				 * (1) 'pagebeforeshow' is the place to be ('pagecreate' is too
				 * early) even
				 * for the setTimeout construct...
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

				/*
				 * The 'throttleresize' event is a special event that
				 * prevents browsers from running continuous callbacks
				 * on resize (like Internet Explorer). throttleresize
				 * ensures that a held event will execute after the
				 * timeout so logic that depends on the final conditions
				 * after a resize is complete will still execute properly.
				 */
				$(window).on('throttledresize', function() {
					if (_panelCur && _panelCur.onResize) {
						_panelCur.onResize(_panelCur);
					}
				});

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

				/*
				 * Generic loading
				 */
				$('.savapage-detail').click(function() {
					_loadPanel($(this).attr('name'));
					return false;
				});

				$(this).on('click', '.savapage-detail-prev', null, function(ev) {
					var clazz = $(this).attr('name')
					//
					, pnl = _panel[clazz];

					$(this).closest('[data-role="popup"]').popup("close");

					//if (pnl.output.lastPage) {
					pnl.refresh(pnl, true);
					//} else {
					//    _loadPanel(clazz);
					//}
					return false;
				});

				$('#button-logout').click(function() {
					$('.content-primary').empty();
					_self.onLogout();
					return false;
				});

				$('#button-point-of-sale').click(function() {
					_view.pages.pointOfSale.loadShowAsync();
					return false;
				});

				/*
				 * One-time binding the click to a function. We don't want
				 * to bind each time the panel is loaded.
				 *
				 * Even if #id doesn't exist yet (because the panel is not
				 * loaded) this code is executed.
				 */
				$('body').on('click', '#sp-button-continue-after-expire', null, function() {
					_ns.restartWebApp();
					return false;
				});

				/*
				 * One-time binding the click to a function. We don't want
				 * to bind each time the panel is loaded.
				 *
				 * Even if #id doesn't exist yet (because the panel is not
				 * loaded) this code is executed.
				 */

				/*
				 * AccountTrx Panel
				 */
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

				$(this).on('click', '#button-accounttrx-report', null, function() {
					var pnl = _panel.AccountTrxBase;
					pnl.v2m(pnl);
					_self.onDownload("report", pnl.input, "AccountTrxList");
					return true;
				});

				$(this).on('click', ".sp-download-receipt", null, function() {
					_self.onDownload("pos-receipt-download", null, $(this).attr('data-savapage'));
					return false;
				});

				/*
				 * AccountVoucher Panel
				 */
				$(this).on('click', '#button-voucher-apply', null, function() {
					var pnl = _panel.AccountVoucherBase;
					pnl.page(pnl, 1);
					return false;
				});

				$(this).on('click', '#button-voucher-default', null, function() {
					var pnl = _panel.AccountVoucherBase;
					pnl.applyDefaults(pnl);
					pnl.m2v(pnl);
					return false;
				});

				$(this).on('click', '#button-voucher-new-batch', null, function() {
					_view.pages.voucherCreate.loadShowAsync();
					return false;
				});

				$(this).on('click', '#button-voucher-expire-batch', null, function() {
					var pnl = _panel.AccountVoucherBase;
					pnl.v2m(pnl);
					_self.onVoucherExpireBatch(_panel.AccountVoucherBase.getBatch());
					return false;
				});

				$(this).on('click', '#button-voucher-print-batch', null, function() {
					var pnl = _panel.AccountVoucherBase;
					pnl.v2m(pnl);
					// VoucherBatchPrintDto
					_self.onDownload("account-voucher-batch-print", null, JSON.stringify({
						batchId : pnl.getBatch(),
						design : pnl.getCardDesign()
					}));

					return false;
				});

				$(this).on('click', '#button-voucher-delete-batch', null, function() {
					var pnl = _panel.AccountVoucherBase
					//
					, batch = pnl.getBatch()
					//
					;
					pnl.applyDefaults(pnl);
					_self.onVoucherDeleteBatch(batch);
					return false;
				});

				$(this).on('click', '#button-voucher-delete-expired', null, function() {
					var pnl = _panel.AccountVoucherBase;
					pnl.v2m(pnl);
					_self.onVoucherDeleteExpired();
					return false;
				});

				$(this).on('change', "#sp-voucher-batch", null, function(e) {
					_panel.AccountVoucherBase.onVoucherSelectBatch($(this).val());
				});

				/*
				 * AppLog Panel
				 */
				$(this).on('click', '#button-applog-apply', null, function() {
					var pnl = _panel.AppLogBase;
					pnl.page(pnl, 1);
					return false;
				});

				$(this).on('click', '#button-applog-default', null, function() {
					var pnl = _panel.AppLogBase;
					pnl.applyDefaults(pnl);
					pnl.m2v(pnl);
					return false;
				});

				/*
				 * ConfigProp Panel
				 */
				$(this).on('click', '#button-configprop-apply', null, function() {
					var pnl = _panel.ConfigPropBase;
					pnl.page(pnl, 1);
					return false;
				});

				$(this).on('click', '#button-configprop-default', null, function() {
					var pnl = _panel.ConfigPropBase;
					pnl.applyDefaults(pnl);
					pnl.m2v(pnl);
					return false;
				});

				$(this).on('click', '.sp-edit-config', null, function() {
					_self.onEditConfigProp($(this).attr('data-savapage'));
					return false;
				});

				/*
				 * Devices Panel
				 */
				$(this).on('click', '.sp-edit-device', null, function() {
					_self.onEditDevice($(this).attr('data-savapage'));
					return false;
				});

				$(this).on('click', '#button-devices-apply', null, function() {
					var pnl = _panel.DevicesBase;
					pnl.page(pnl, 1);
					return false;
				});

				$(this).on('click', '#button-devices-default', null, function() {
					var pnl = _panel.DevicesBase;
					pnl.applyDefaults(pnl);
					pnl.m2v(pnl);
					return false;
				});

				$(this).on('click', "#button-create-device-terminal", null, function() {
					_self.onCreateDeviceTerminal();
					return false;
				});
				$(this).on('click', "#button-create-device-card-reader", null, function() {
					_self.onCreateDeviceCardReader();
					return false;
				});

				/*
				 * DocLog Panel
				 */
				$(this).on('click', '#button-doclog-apply', null, function() {
					var pnl = _panel.DocLogBase;
					pnl.page(pnl, 1);
					return false;
				});

				$(this).on('click', '#button-doclog-default', null, function() {
					var pnl = _panel.DocLogBase;
					pnl.clearHiddenUserid();
					pnl.applyDefaults(pnl);
					pnl.m2v(pnl);
					return false;
				});

				$(this).on('change', "input[name='sp-doclog-select-type']", null, function() {
					_panel.DocLogBase.setVisibility(_panel.DocLogBase);
					return false;
				});

				/*
				 * Users Panel
				 */
				$(this).on('click', '.sp-edit-user', null, function() {
					_self.onEditUser($(this).attr('data-savapage'));
					return false;
				});

				$(this).on('click', '.sp-user-log', null, function() {
					var pnl = _panel.DocLogBase;
					pnl.applyDefaults(_panel.DocLogBase);
					pnl.input.select.user_id = $(this).attr('data-savapage');
					// skipBeforeLoad
					pnl.refresh(pnl, true);
					return false;
				});

				$(this).on('click', '.sp-user-transaction', null, function() {
					var pnl = _panel.AccountTrxBase;
					pnl.applyDefaults(pnl);
					pnl.input.select.user_id = $(this).attr('data-savapage');
					// skipBeforeLoad
					pnl.refresh(pnl, true);
					return false;
				});

				$(this).on('click', "#button-create-user", null, function() {
					_self.onCreateUser();
					return false;
				});

				$(this).on('click', '#button-users-apply', null, function() {
					var pnl = _panel.UsersBase;
					pnl.page(pnl, 1);
					return false;
				});

				$(this).on('click', '#button-users-default', null, function() {
					var pnl = _panel.UsersBase;
					pnl.applyDefaults(pnl);
					pnl.m2v(pnl);
					return false;
				});

				$(this).on('click', '#button-users-report', null, function() {
					var pnl = _panel.UsersBase;
					pnl.v2m(pnl);
					_self.onDownload("report", pnl.input, "UserList");
					return true;
				});

				/*
				 * Queues Panel
				 */
				$(this).on('click', '.sp-edit-queue', null, function() {
					_self.onEditQueue($(this).attr('data-savapage'));
					return false;
				});

				$(this).on('click', '.sp-queue-log', null, function() {
					var pnl = _panel.DocLogBase;
					pnl.applyDefaults(pnl);
					pnl.input.select.queue_id = $(this).attr('data-savapage');
					// skipBeforeLoad
					pnl.refresh(pnl, true);
					return false;
				});

				$(this).on('click', "#button-create-queue", null, function() {
					_self.onCreateQueue();
					return false;
				});

				$(this).on('click', '#button-queues-apply', null, function() {
					var pnl = _panel.QueuesBase;
					pnl.page(pnl, 1);
					return false;
				});

				$(this).on('click', '#button-queues-default', null, function() {
					var pnl = _panel.QueuesBase;
					pnl.applyDefaults(pnl);
					pnl.m2v(pnl);
					return false;
				});

				/*
				 * Printers Panel
				 */
				$(this).on('click', '.sp-edit-printer', null, function() {
					_self.onEditPrinter($(this).attr('data-savapage'));
					return false;
				});

				$(this).on('dblclick', '.sp-printer-opt-download', null, function() {
					_self.onDownload("printer-opt-download", null, $(this).attr('data-savapage'));
					return false;
				});

				$(this).on('click', '.sp-printer-log', null, function() {
					var pnl = _panel.DocLogBase;
					pnl.applyDefaults(pnl);
					pnl.input.select.printer_id = $(this).attr('data-savapage');
					// skipBeforeLoad
					pnl.refresh(pnl, true);
					return false;
				});

				$(this).on('click', '#button-printers-apply', null, function() {
					var pnl = _panel.PrintersBase;
					pnl.page(pnl, 1);
					return false;
				});

				$(this).on('click', '#button-printers-default', null, function() {
					var pnl = _panel.PrintersBase;
					pnl.applyDefaults(pnl);
					pnl.m2v(pnl);
					return false;
				});

				$(this).on('click', '#button-printer-sync', null, function() {
					_self.onPrinterSynchr();
					return false;
				});

				/*
				 * Options Panel
				 */
				$(this).on('click', '#apply-auth', null, function() {
					_self.onApplyAuth(_view.getRadioValue('auth.method'));
					return false;
				});
				$(this).on('click', '#apply-smtp', null, function() {
					_self.onApplySmtp(_view.getRadioValue('mail.smtp.security'));
					return false;
				});

				$(this).on('click', '#apply-internal-users', null, function() {
					_self.onApplyInternalUsers();
					return false;
				});

				$(this).on('change', "input:checkbox[id='internal-users.enable']", null, function(e) {
					_panel.Options.onInternalUsersEnabled($(this).is(':checked'));
				});

				$(this).on('click', '#apply-mail', null, function() {
					_self.onApplyMail();
					return false;
				});
				$(this).on('click', '#test-mail', null, function() {
					_self.onTestMail();
					return false;
				});

				$(this).on('change', "input:checkbox[id='print.imap.enable']", null, function(e) {
					_panel.Options.onPrintImapEnabled($(this).is(':checked'));
				});

				$(this).on('change', '.flipswitch-mailprint-online', null, function(e) {
					_self.onFlatRequest($(this).is(':checked') ? 'imap-start' : 'imap-stop');
				});

				$(this).on('change', "input:checkbox[id='flipswitch-payment-plugin-bitcoin-online']", null, function(e) {
					_self.onPaymentGatewayOnline(true, $(this).is(':checked'));
				});
				
				$(this).on('change', "input:checkbox[id='flipswitch-payment-plugin-generic-online']", null, function(e) {
					_self.onPaymentGatewayOnline(false, $(this).is(':checked'));
				});

				$(this).on('click', '#apply-imap', null, function() {
					_self.onApplyImap(_view.isCbChecked($('#print\\.imap\\.enable')), _view.getRadioValue('print.imap.security'));
					return false;
				});

				$(this).on('click', '#test-imap', null, function() {
					_self.onFlatRequest('imap-test');
					return false;
				});

				$(this).on('click', '#test-papercut', null, function() {
					_self.onFlatRequest('papercut-test');
					return false;
				});
				$(this).on('click', '#test-smartschool', null, function() {
					_self.onFlatRequest('smartschool-test');
					return false;
				});

				$(this).on('click', '#start-smartschool-simulate', null, function() {
					_self.onFlatRequest('smartschool-start-simulate');
					return false;
				});

				$(this).on('change', '.flipswitch-smartschool-online', null, function(e) {
					var isChecked = $(this).is(':checked'), res = _self.onFlatRequest( isChecked ? 'smartschool-start' : 'smartschool-stop');
					if (!res || res.result.code !== '0') {
						$(this).prop("checked", !isChecked);
						// This triggers 'change' event again :-)	
						$(this).flipswitch( "refresh" );						
					}
				});

				$(this).on('change', "input:checkbox[id='flipswitch-webprint-online']", null, function(e) {
					_self.onFlipswitchWebPrint($(this).is(':checked'));
				});

				$(this).on('change', "input:checkbox[id='web-print.enable']", null, function(e) {
					_panel.Options.onWebPrintEnabled($(this).is(':checked'));
				});

				$(this).on('change', "input:checkbox[id='smartschool.1.enable']", null, function(e) {
					_panel.Options.onSmartSchoolEnabled(_view.isCbChecked($(this)), _view.isCbChecked($('#smartschool\\.2\\.enable')));
				});

				$(this).on('change', "input:checkbox[id='smartschool.2.enable']", null, function(e) {
					_panel.Options.onSmartSchoolEnabled(_view.isCbChecked($('#smartschool\\.1\\.enable')), _view.isCbChecked($(this)));
				});

				$(this).on('change', "input:checkbox[id='smartschool.papercut.enable']", null, function(e) {
					_panel.Options.onSmartSchoolPaperCutEnabled($(this).is(':checked'));
				});

				$(this).on('change', "input:checkbox[id='gcp.enable']", null, function(e) {
					_panel.Options.onGcpRefresh(_panel.Options);
				});

				$(this).on('change', "input:checkbox[id='gcp-mail-after-cancel-enable']", null, function(e) {
					_panel.Options.onGcpRefresh(_panel.Options);
				});

				$(this).on('change', "input:checkbox[id='proxy-print.non-secure']", null, function(e) {
					_panel.Options.onProxyPrintEnabled($(this).is(':checked'));
				});

				$(this).on('change', "#group-user-auth-mode-local input:checkbox", null, function(e) {
					_panel.Options.onAuthModeLocalEnabled();
				});

				$(this).on('click', '#apply-webprint', null, function() {
					_self.onApplyWebPrint(_view.isCbChecked($('#web-print\\.enable')));
					return false;
				});

				$(this).on('click', '#apply-smartschool', null, function() {
					_self.onApplySmartSchool(_view.isCbChecked($('#smartschool\\.1\\.enable')), _view.isCbChecked($('#smartschool\\.2\\.enable')));
					return false;
				});

				$(this).on('click', '#apply-smartschool-papercut', null, function() {
					_self.onApplySmartSchoolPaperCut(_view.isCbChecked($('#smartschool\\.papercut\\.enable')));
					return false;
				});

				$(this).on('change', "input:checkbox[id='flipswitch-gcp-online']", null, function(e) {
					_self.onApplyGcpOnline(_panel, $(this).is(':checked'));
				});

				$(this).on('click', '#gcp-set-online', null, function() {
					return _self.onApplyGcpOnline(_panel, true);
				});
				$(this).on('click', '#gcp-set-offline', null, function() {
					return _self.onApplyGcpOnline(_panel, false);
				});

				$(this).on('click', '#gcp-apply-enable', null, function() {
					_self.onApplyGcpEnable(_panel, _view.isCbChecked($('#gcp\\.enable')));
					return false;
				});

				$(this).on('click', '#gcp-apply-notification', null, function() {
					return _self.onApplyGcpNotification();
				});

				$(this).on('click', '#gcp-button-refresh', null, function() {
					_self.onRefreshGcp(_panel);
					return false;
				});

				$(this).on('click', '#gcp-edit-show', null, function() {
					// toggles visibility on/off
					$('#gcp-register-section').toggle();
					return false;
				});

				$(this).on('click', '#gcp-edit-hide', null, function() {
					$('#gcp-register-section').hide();
					return false;
				});

				$(this).on('click', '#register-gcp', null, function() {
					_self.onRegisterGcp();
					return false;
				});

				$(this).on('click', '#apply-proxyprint', null, function() {
					_self.onApplyProxyPrint();
					return false;
				});

				$(this).on('click', "#apply-financial-general", null, function() {
					_self.onApplyFinancialGeneral();
					return false;
				});

				$(this).on('click', "#change-financial-currency-code", null, function() {
					_self.onChangeFinancialCurrencyCode();
					return false;
				});

				$(this).on('click', "#apply-financial-pos", null, function() {
					_self.onApplyFinancialPos();
					return false;
				});

				$(this).on('click', "#apply-financial-vouchers", null, function() {
					_self.onApplyFinancialVouchers($("#sp-financial-voucher-card-fontfamily").val());
					return false;
				});

				$(this).on('click', "#apply-financial-user-transfers", null, function() {
					_self.onApplyFinancialUserTransfers();
					return false;
				});

				$(this).on('change', "input:checkbox[id='financial.user.transfers.enable']", null, function(e) {
					_panel.Options.onFinancialUserTransfersEnabled(_view.isCbChecked($(this)));
				});

				$(this).on('click', '#apply-user-create', null, function() {
					_self.onApplyUserCreate();
					return false;
				});

				$(this).on('click', '#apply-user-auth-mode-local', null, function() {
					_self.onApplyUserAuthModeLocal();
					return false;
				});

				$(this).on('click', '#apply-locale', null, function() {
					_self.onApplyLocale();
					return false;
				});

				$(this).on('click', '#apply-papersize', null, function() {
					_self.onApplyPaperSize();
					return false;
				});

				$(this).on('click', '#apply-report-fontfamily', null, function() {
					_self.onApplyReportsPdfFontFamily($("#sp-report-fontfamily").val());
					return false;
				});

				$(this).on('click', '#apply-converters', null, function() {
					_self.onApplyConverters();
					return false;
				});

				$(this).on('click', '#apply-print-in', null, function() {
					_self.onApplyPrintIn();
					return false;
				});

				$(this).on('click', '#apply-cliapp-auth', null, function() {
					_self.onApplyCliAppAuth();
					return false;
				});

				$(this).on('click', '#admin-pw-reset', null, function() {
					_self.onAdminPwReset();
					return false;
				});

				$(this).on('click', '#jmx-pw-reset', null, function() {
					_self.onJmxPwReset();
					return false;
				});

				$(this).on('click', '#backup-now', null, function() {
					_self.onBackupNow();
					return false;
				});
				$(this).on('click', '#apply-backup-automatic', null, function() {
					_self.onApplyBackupAuto();
					return false;
				});

				$(this).on('change', "input:radio[name='auth.method']", null, function(e) {
					_panel.Options.onAuthMethodSelect($(this).val());
				});

				$(this).on('click', '#user-source-change-group', null, function() {
					_self.onUserSourceGroupEdit();
					return false;
				});

				$(this).on('click', '#user-source-group-apply', null, function() {
					_self.onUserSourceGroupApply();
					return false;
				});
				$(this).on('click', '#user-source-group-cancel', null, function() {
					_self.onUserSourceGroupCancel();
					return false;
				});
				$(this).on('click', '#user-source-group-all', null, function() {
					_self.onUserSourceGroupAll();
					return false;
				});

				$(this).on('click', '#user-sync-apply', null, function() {
					_self.onUserSyncApply();
					return false;
				});

				$(this).on('click', '#user-sync-test', null, function() {
					_self.onUserSyncTest();
					return false;
				});
				$(this).on('click', '#user-sync-now', null, function() {
					_self.onUserSyncNow();
					return false;
				});

				$(this).on('click', '#sync-messages-clear', null, function() {
					$('#user-sync-messages').html('');
					return false;
				});

				$(this).on('click', '#button-config-editor', null, function() {
					var pnl = _panel.ConfigPropBase;
					pnl.refresh(pnl);
					return false;
				});

				$(this).on('click', '#pagometer-reset', null, function() {
					_self.onPagometerReset();
					return false;
				});

				/*
				 * Dashboard Panel
				 */
				$(this).on('click', "#live-messages-clear", null, function() {
					_model.pubMsgStack = [];
					$('#live-messages').html('');
					return false;
				});

				$(this).on('click', '#button-bitcoin-wallet-refresh', null, function() {
					_self.onBitcoinWalletRefresh();
					_panelCur.refreshSysStatus();
					return false;
				});

			});
			/*
			 * IMPORTANT
			 */
			return _self;
		};

	}(jQuery, this, this.document, JSON, this.org.savapage));
