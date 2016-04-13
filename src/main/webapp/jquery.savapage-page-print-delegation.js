/*! SavaPage jQuery Mobile Print Delegation Page | (c) 2011-2016 Datraverse B.V. | GNU Affero General Public License */

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

		_ns.PagePrintDelegation = function(_i18n, _view, _model, _api) {

			var _util = _ns.Utils, _this = this
			//
			, _ACCOUNT_ENUM_GROUP = 'GROUP', _ACCOUNT_ENUM_USER = 'USER', _ACCOUNT_ENUM_SHARED = 'SHARED'
			//
			, _RADIO_ACCOUNT_NAME = 'sp-print-delegation-select-account-radio'
			//
			, _RADIO_ACCOUNT_ID_GROUP = 'sp-print-delegation-select-account-group'
			//
			, _RADIO_ACCOUNT_ID_USER = 'sp-print-delegation-select-account-user'
			//
			, _RADIO_ACCOUNT_ID_SHARED = 'sp-print-delegation-select-account-shared'
			//
			, _IND_SELECTED = '+', _IND_DESELECTED = '&nbsp;', _QS_MAX_RESULTS = 5
			//
			, _CLASS_GROUP = '_sp_group', _CLASS_USER = '_sp_user'
			//
			, _CLASS_SELECTED_IND = '_sp_selected', _CLASS_SELECTED_TXT = '_sp_selected_txt'
			//
			, _quickUserGroupCache, _quickUserCache, _quickAccountCache
			//
			, _quickUserGroupSelected, _quickUserSelected, _quickAccountSelected
			//
			, _nSelectedGroups, _nSelectedUsers
			//
			, _delegatorGroups = {}, _delegatorUsers = {}
			//
			, _nDelegatorGroups = 0, _nDelegatorUsers = 0, _nDelegatorGroupMembers = 0
			//
			, _delegatorGroupsSelected, _delegatorUsersSelected
			//
			, _nSelectedDelegatorGroups, _nSelectedDelegatorUsers

			//----------------------------------------------------------------
			, _delegationModel = function() {
				return {
					name : 'a name',
					groups : _delegatorGroups,
					users : _delegatorUsers
				};
			}
			//----------------------------------------------------------------
			, _createModelAccount = function(account, userCount) {
				// Java: PrintDelegationDto.DelegatorAccount
				return {
					type : account.accountType,
					id : account.id,
					userCount : userCount
				};
			}
			//----------------------------------------------------------------
			, _onSaveModel = function(selection) {
				// Java: PrintDelegationDto
				var res = _api.call({
					request : "print-delegation-set",
					dto : JSON.stringify(_delegationModel())
				});

				if (res.result.code !== '0') {
					_view.showApiMsg(res);
				}
			}
			//
			, _revertQuickUserGroupsSelected = function() {
				_quickUserGroupSelected = {};
				_nSelectedGroups = 0;

				$.each($('#sp-print-delegation-groups-select-to-add-filter .' + _CLASS_SELECTED_TXT), function() {
					$(this).html(_IND_DESELECTED);
				});
			}
			//
			, _revertQuickUserSelected = function() {
				_quickUserSelected = {};
				_nSelectedUsers = 0;

				$.each($('#sp-print-delegation-users-select-to-add-filter .' + _CLASS_SELECTED_TXT), function() {
					$(this).html(_IND_DESELECTED);
				});
			}
			//
			, _isAccountSharedSelected = function() {
				return _view.isRadioIdSelected(_RADIO_ACCOUNT_NAME, _RADIO_ACCOUNT_ID_SHARED);
			}
			//
			, _setVisibilityDelegatorsEdit = function() {
				var selectedRows = _nSelectedDelegatorGroups + _nSelectedDelegatorUsers > 0;

				/*
				*  For future use: when #sp-print-delegation-edit-radio-edit is present.
				*/
				//_view.enableCheckboxRadio($("#sp-print-delegation-edit-radio-edit"), selectedRows);

				_view.enable($('.sp-print-delegator-rows'), _nDelegatorGroups + _nDelegatorUsers > 0);
				_view.enable($('.sp-print-delegator-rows-selected'), selectedRows);
				_view.enable($('.sp-print-delegation-mode-edit'), selectedRows);
				_view.enable($('#sp-print-delegation-button-remove-selected'), selectedRows);
			}
			//----------------------------------------------------------------
			, _initDelegatorSelection = function() {
				_nSelectedDelegatorGroups = 0;
				_nSelectedDelegatorUsers = 0;
				_delegatorGroupsSelected = {};
				_delegatorUsersSelected = {};
				_setVisibilityDelegatorsEdit();
			}
			//----------------------------------------------------------------
			, _isModeAddGroups = function() {
				return $('#sp-print-delegation-edit-radio-add-groups').is(':checked');
			}
			//----------------------------------------------------------------
			, _isModeAddUsers = function() {
				return $('#sp-print-delegation-edit-radio-add-users').is(':checked');
			}
			//----------------------------------------------------------------
			, _setVisibility = function() {
				var showButtonAdd, enableButtonAdd, showAddGroups, showAddUsers, showEditPanel;

				if (_isModeAddGroups()) {
					showAddGroups = true;
					showButtonAdd = true;
					enableButtonAdd = _nSelectedGroups > 0;

				} else if (_isModeAddUsers()) {
					showAddGroups = true;
					showAddUsers = true;
					showButtonAdd = true;
					enableButtonAdd = _nSelectedUsers > 0;
				} else {
					showEditPanel = _nSelectedDelegatorGroups + _nSelectedDelegatorUsers > 0;
				}

				_view.visible($('.sp-print-delegation-mode-add'), showAddGroups || showAddUsers);

				_view.visible($('.sp-print-delegation-mode-add-groups'), showAddGroups);
				_view.visible($('.sp-print-delegation-mode-add-users'), showAddUsers);
				_view.visible($('.sp-print-delegation-mode-edit'), showEditPanel);

				_view.visible($('#sp-print-delegation-button-add'), showButtonAdd);
				_view.enable($('#sp-print-delegation-button-add'), enableButtonAdd);

				_view.visible($('.sp-print-delegation-select-shared-account'), _isAccountSharedSelected());

				_setVisibilityDelegatorsEdit();
			}
			//----------------------------------------------------------------
			, _getAccount = function() {
				var account = {
					accountType : _view.getRadioValue(_RADIO_ACCOUNT_NAME)
				};

				if (_isAccountSharedSelected()) {
					account.id = _quickAccountSelected.key;
					account.text = _quickAccountSelected.text;
				}

				return account;
			}
			//----------------------------------------------------------------
			, _onGroupQuickSearch = function(target, filter) {
				/* QuickSearchFilterUserGroupDto */
				var res, html = "";

				_quickUserGroupCache = {};
				_quickUserGroupSelected = {};
				_nSelectedGroups = 0;

				res = _api.call({
					request : "usergroup-quick-search",
					dto : JSON.stringify({
						filter : filter,
						maxResults : _QS_MAX_RESULTS,
						aclRole : 'PRINT_DELEGATOR'
					})
				});

				if (res.result.code === '0') {

					$.each(res.dto.items, function(key, item) {

						_quickUserGroupCache[item.key] = item;

						html += "<li class=\"ui-mini ui-li-has-icon\" data-icon=\"false\" data-savapage=\"" + item.key + "\">";
						html += "<a href=\"#\">";
						html += "<span class=\"" + _CLASS_SELECTED_TXT + "\" style=\"font-family: monospace;\">&nbsp;</span>&nbsp;";
						html += item.text + "<span class=\"ui-li-count\">" + item.userCount + "</span></a></li>";
					});
				} else {
					_view.showApiMsg(res);
				}

				target.html(html).filterable("refresh");
			}
			//----------------------------------------------------------------
			, _onUserQuickSearch = function(target, groupId, filter) {
				/* QuickSearchUserGroupMemberFilterDto */
				var res, html = "";

				_quickUserCache = {};
				_quickUserSelected = {};
				_nSelectedUsers = 0;

				res = _api.call({
					request : "usergroup-member-quick-search",
					dto : JSON.stringify({
						groupId : groupId,
						filter : filter,
						maxResults : _QS_MAX_RESULTS,
						aclRole : 'PRINT_DELEGATOR'
					})
				});

				if (res.result.code === '0') {

					$.each(res.dto.items, function(key, item) {

						_quickUserCache[item.key] = item;

						html += "<li class=\"ui-mini ui-li-has-icon\" data-icon=\"false\" data-savapage=\"" + item.key + "\">";
						html += "<a href=\"#\">";
						html += "<span class=\"" + _CLASS_SELECTED_TXT + "\" style=\"font-family: monospace;\">&nbsp;</span>&nbsp;";
						html += item.fullName;
						if (item.userId) {
							html += ' &bull; ' + item.userId;
						}
						html += "</a></li>";
					});
				} else {
					_view.showApiMsg(res);
				}

				target.html(html).filterable("refresh");
			}
			//----------------------------------------------------------------
			, _onAccountQuickSearch = function(target, filter) {
				/* QuickSearchFilterDto */
				var res, html = "";

				_quickAccountCache = {};
				_quickAccountSelected = undefined;

				res = _api.call({
					request : "shared-account-quick-search",
					dto : JSON.stringify({
						filter : filter,
						maxResults : _QS_MAX_RESULTS
					})
				});

				if (res.result.code === '0') {

					$.each(res.dto.items, function(key, item) {

						_quickAccountCache[item.key] = item;

						html += "<li class=\"ui-mini ui-li-has-icon\" data-icon=\"false\" data-savapage=\"" + item.key + "\">";
						html += "<a href=\"#\">";
						html += "<span class=\"" + _CLASS_SELECTED_TXT + "\" style=\"font-family: monospace;\">&nbsp;</span>&nbsp;";
						html += item.text + "</a></li>";
					});
				} else {
					_view.showApiMsg(res);
				}

				target.html(html).filterable("refresh");
			}
			//----------------------------------------------------------------
			, _appendDelegatorRow = function(tbody, item, account, isGroup) {
				var html;

				html = '<tr class="';
				html += isGroup ? _CLASS_GROUP : _CLASS_USER;
				html += '" data-savapage="' + item.key + '">';
				html += '<th><span class="' + _CLASS_SELECTED_TXT + '">&nbsp;</span>&nbsp;';
				html += '<img src="/famfamfam-silk/';
				html += isGroup ? 'group.png' : 'user_gray.png';
				html += '"/>&nbsp;&nbsp;&nbsp;';

				if (isGroup) {
					html += item.text;
				} else {
					html += item.fullName;
					if (item.userId) {
						html += ' &bull; ' + item.userId;
					}
				}
				html += '</th>';
				html += '<td>' + ( isGroup ? item.userCount : 1) + '</td>';
				html += '<td><img src="/famfamfam-silk/';

				if (account.accountType === _ACCOUNT_ENUM_GROUP) {
					html += 'group.png';
				} else if (account.accountType === _ACCOUNT_ENUM_USER) {
					html += 'user_gray.png';
				} else {
					html += "tag_green.png";
				}
				html += '"/>';

				if (account.accountType === _ACCOUNT_ENUM_SHARED) {
					html += '&nbsp;&nbsp;&nbsp;' + account.text;
				}

				html += '</td></tr>';
				tbody.append(html);
			}
			//----------------------------------------------------------------
			, _getDelegatorRowBody = function() {
				return $('#sp-selected-print-delegation tbody');
			}
			//----------------------------------------------------------------
			, _onAddGroups = function() {
				var tbody = _getDelegatorRowBody(), account = _getAccount();

				$.each(_quickUserGroupSelected, function(key, item) {

					if (!_delegatorGroups[item.key]) {

						_delegatorGroups[item.key] = _createModelAccount(account, item.userCount);
						_appendDelegatorRow(tbody, item, account, true);
						_nDelegatorGroups++;
						_nDelegatorGroupMembers += item.userCount;
					}
				});

				_revertQuickUserGroupsSelected();
				_setVisibilityDelegatorsEdit();
			}
			//----------------------------------------------------------------
			, _onAddUsers = function() {
				var tbody = _getDelegatorRowBody(), account = _getAccount();

				$.each(_quickUserSelected, function(key, item) {

					if (!_delegatorUsers[item.key]) {

						_delegatorUsers[item.key] = _createModelAccount(account, 1);
						_appendDelegatorRow(tbody, item, account, false);
						_nDelegatorUsers++;
					}
				});

				_revertQuickUserSelected();
				_setVisibilityDelegatorsEdit();
			}
			//----------------------------------------------------------------
			, _onGroupSelected = function(selection) {
				var span = selection.find('.' + _CLASS_SELECTED_TXT)
				//
				, dbkey = selection.attr('data-savapage'), dbKeySelected
				//
				;

				if (_isModeAddGroups()) {

					if (_quickUserGroupSelected[dbkey]) {
						_nSelectedGroups--;
						delete _quickUserGroupSelected[dbkey];
						span.html(_IND_DESELECTED);
					} else {
						_nSelectedGroups++;
						_quickUserGroupSelected[dbkey] = _quickUserGroupCache[dbkey];
						span.html(_IND_SELECTED);
					}
					_view.enable($('#sp-print-delegation-button-add'), _nSelectedGroups > 0);

				} else {

					dbKeySelected = _util.getFirstProp(_quickUserGroupSelected);

					if (dbKeySelected) {
						_nSelectedGroups--;
						delete _quickUserGroupSelected[dbKeySelected];
						selection.closest('ul').find('.' + _CLASS_SELECTED_IND).html(_IND_DESELECTED).removeClass(_CLASS_SELECTED_IND);
					}

					if (dbKeySelected === dbkey) {
						dbkey = null;
					} else {
						_nSelectedGroups++;
						_quickUserGroupSelected[dbkey] = _quickUserGroupCache[dbkey];
						span.html(_IND_SELECTED).addClass(_CLASS_SELECTED_IND);
					}
					_onUserQuickSearch($("#sp-print-delegation-users-select-to-add-filter"), dbkey, $('#sp-print-delegation-users-select-to-add').val());
				}
			}
			//----------------------------------------------------------------
			, _onUserSelected = function(selection) {
				var span = selection.find('.' + _CLASS_SELECTED_TXT)
				//
				, dbkey = selection.attr('data-savapage')
				//
				;

				if (_quickUserSelected[dbkey]) {
					_nSelectedUsers--;
					delete _quickUserSelected[dbkey];
					span.html(_IND_DESELECTED);
				} else {
					_nSelectedUsers++;
					_quickUserSelected[dbkey] = _quickUserCache[dbkey];
					span.html(_IND_SELECTED);
				}
				_view.enable($('#sp-print-delegation-button-add'), _nSelectedUsers > 0);
			}
			//----------------------------------------------------------------
			, _onAccountSelected = function(selection) {
				var span = selection.find('.' + _CLASS_SELECTED_TXT)
				//
				, dbkey = selection.attr('data-savapage')
				//
				, dbKeySelected = _quickAccountSelected ? _quickAccountSelected.key : null
				//
				;

				if (dbKeySelected) {
					if (dbKeySelected === dbkey) {
						return;
					}
					selection.closest('ul').find('.' + _CLASS_SELECTED_IND).html(_IND_DESELECTED).removeClass(_CLASS_SELECTED_IND);
				}
				_quickAccountSelected = _quickAccountCache[dbkey];
				span.html(_IND_SELECTED).addClass(_CLASS_SELECTED_IND);
			}
			//----------------------------------------------------------------
			, _onDelegatorSelectRow = function(selection) {
				var span = selection.find('.' + _CLASS_SELECTED_TXT)
				//
				, isGroup = selection.hasClass(_CLASS_GROUP)
				//
				, dbkey = selection.attr('data-savapage')
				//
				;
				if (isGroup) {
					if (_delegatorGroupsSelected[dbkey]) {
						_nSelectedDelegatorGroups--;
						delete _delegatorGroupsSelected[dbkey];
						span.html(_IND_DESELECTED);
					} else {
						_nSelectedDelegatorGroups++;
						_delegatorGroupsSelected[dbkey] = _delegatorGroups[dbkey];
						span.html(_IND_SELECTED);
					}
				} else {
					if (_delegatorUsersSelected[dbkey]) {
						_nSelectedDelegatorUsers--;
						delete _delegatorUsersSelected[dbkey];
						span.html(_IND_DESELECTED);
					} else {
						_nSelectedDelegatorUsers++;
						_delegatorUsersSelected[dbkey] = _delegatorUsers[dbkey];
						span.html(_IND_SELECTED);
					}
				}
				_setVisibilityDelegatorsEdit();
			}
			//----------------------------------------------------------------
			, _onDelegatorSelectAll = function() {
				_initDelegatorSelection();
				$.each($('#sp-selected-print-delegation-rows .' + _CLASS_SELECTED_TXT), function() {
					_onDelegatorSelectRow($(this).closest('tr'));
				});
			}
			//----------------------------------------------------------------
			, _onDelegatorDeselectAll = function() {
				_initDelegatorSelection();
				$.each($('#sp-selected-print-delegation-rows .' + _CLASS_SELECTED_TXT), function() {
					$(this).html(_IND_DESELECTED);
				});
			}
			//----------------------------------------------------------------
			, _onDelegatorRemoveSelected = function() {

				$.each($('.' + _CLASS_SELECTED_TXT).closest('tr'), function() {
					var dbkey = $(this).attr('data-savapage');

					if (_delegatorGroupsSelected[dbkey]) {
						_nDelegatorGroups--;
						_nDelegatorGroupMembers -= _delegatorGroups[dbkey].userCount;
						delete _delegatorGroups[dbkey];
						$(this).remove();
					} else if (_delegatorUsersSelected[dbkey]) {
						_nDelegatorUsers--;
						delete _delegatorUsers[dbkey];
						$(this).remove();
					}

				});
				_initDelegatorSelection();
			}
			//
			, _onChangeEditMode = function(id) {

				if (id === 'sp-print-delegation-edit-radio-add-groups') {

					_view.checkRadio(_RADIO_ACCOUNT_NAME, _RADIO_ACCOUNT_ID_USER);

					_view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_GROUP), true);
					_view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_SHARED), true);

				} else if (id === 'sp-print-delegation-edit-radio-add-users') {

					_view.checkRadio(_RADIO_ACCOUNT_NAME, _RADIO_ACCOUNT_ID_USER);
					_view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_GROUP), false);
					_view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_SHARED), false);

					// Deselect all groups
					_revertQuickUserGroupsSelected();

					// Clear users
					_quickUserCache = {};
					_quickUserSelected = {};
					_nSelectedUsers = 0;

					_onUserQuickSearch($("#sp-print-delegation-users-select-to-add-filter"), null, $('#sp-print-delegation-users-select-to-add').val());

					_view.enable($('#sp-print-delegation-button-add'), false);

				} else {
					_view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_GROUP), false);
					_view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_USER), true);
					_view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_SHARED), true);
				}
			}
			//
			;

			//----------------------------------------------------------------
			$('#page-print-delegation').on('pagecreate', function(event) {

				var filterableGroups = $("#sp-print-delegation-groups-select-to-add-filter")
				//
				, filterableUsers = $("#sp-print-delegation-users-select-to-add-filter")
				//
				, filterableAccounts = $("#sp-print-delegation-select-shared-account-filter")
				//
				;
				_initDelegatorSelection();

				$('#sp-print-delegation-button-select-all').click(function() {
					_onDelegatorSelectAll();
				});

				$('#sp-print-delegation-button-deselect-all').click(function() {
					_onDelegatorDeselectAll();
				});

				$('#sp-print-delegation-button-remove-selected').click(function() {
					_onDelegatorRemoveSelected();
				});

				$('#sp-print-delegation-button-save').click(function() {
					_onSaveModel();
				});

				$('#sp-print-delegation-button-add').click(function() {
					if (_isModeAddGroups()) {
						_onAddGroups();
					} else {
						_onAddUsers();
					}
					return false;
				});

				$('input[name="sp-print-delegation-edit-radio"]').click(function() {
					_onChangeEditMode($(this).attr('id'));
					_setVisibility();
				});

				$('input[name="' + _RADIO_ACCOUNT_NAME + '"]').click(function() {
					_setVisibility();
				});

				//
				$(this).on('click', '#sp-selected-print-delegation-rows tr', null, function() {
					_onDelegatorSelectRow($(this));
				});

				// Start with Add Groups (and trigger visibility actions).
				$('#sp-print-delegation-edit-radio-add-groups').click();

				//---------------------
				// Group selection
				//---------------------
				filterableGroups.on("filterablebeforefilter", function(e, data) {
					e.preventDefault();
					_onGroupQuickSearch($(this), data.input.get(0).value);
				});

				$(this).on('click', '#sp-print-delegation-groups-select-to-add-filter li', null, function() {
					_onGroupSelected($(this));
				});

				// Show available groups on first open
				_onGroupQuickSearch(filterableGroups, "");

				//---------------------
				// User selection
				//---------------------
				filterableUsers.on("filterablebeforefilter", function(e, data) {
					var dbkey = _util.getFirstProp(_quickUserGroupSelected);
					e.preventDefault();
					_onUserQuickSearch($(this), dbkey, data.input.get(0).value);
				});

				$(this).on('click', '#sp-print-delegation-users-select-to-add-filter li', null, function() {
					_onUserSelected($(this));
				});

				//---------------------
				// Account selection
				//---------------------
				filterableAccounts.on("filterablebeforefilter", function(e, data) {
					e.preventDefault();
					_onAccountQuickSearch($(this), data.input.get(0).value);
				});

				$(this).on('click', '#sp-print-delegation-select-shared-account-filter li', null, function() {
					_onAccountSelected($(this));
				});

				// Show available accounts on first open
				_onAccountQuickSearch(filterableAccounts, "");

			}).on("pagebeforeshow", function(event, ui) {
				_setVisibility();
			}).on("pagebeforehide", function(event, ui) {
				_model.printDelegation = _delegationModel();
				_model.printDelegationCopies = _nDelegatorGroupMembers + _nDelegatorUsers;
				_this.onBeforeHide();
			});
		};

	}(jQuery, this, this.document, JSON, this.org.savapage));
