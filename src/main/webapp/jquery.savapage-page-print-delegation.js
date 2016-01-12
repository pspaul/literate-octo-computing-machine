/*! SavaPage jQuery Mobile Exceeded WebApp | (c) 2011-2016 Datraverse B.V. | GNU Affero General Public License */

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

/*
 *
 */
( function($, window, document, JSON, _ns) {"use strict";

		_ns.PagePrintDelegation = function(_i18n, _view, _model, _api) {

			var _this = this
			//
			, _IND_SELECTED = '+', _IND_DESELECTED = '&nbsp;'
			//
			, _quickUserGroupCache, _quickUserGroupSelected
			//
			, _nSelectedGroups, _nSelectedUsers, _nSelectedAccounts
			//
			, _delegatorGroups = {}, _delegatorUsers = {}
			//
			, _nDelegatorGroups = 0, _nDelegatorUsers = 0
			//
			, _delegatorGroupsSelected, _delegatorUsersSelected
			//
			, _nSelectedDelegatorGroups, _nSelectedDelegatorUsers
			//
			, _quickAccountCache, _quickAccountSelected

			//----------------------------------------------------------------
			, _setVisibilityDelegatorsEdit = function() {
				var selectedRows = _nSelectedDelegatorGroups + _nSelectedDelegatorUsers > 0;
				_view.enableCheckboxRadio($("#sp-print-delegation-edit-radio-edit"), selectedRows);
				_view.enable($('.sp-print-delegator-rows'), _nDelegatorGroups > 0);
				_view.enable($('.sp-print-delegator-rows-selected'), selectedRows);
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
				var showButtonAdd, showAddGroups, showAddUsers, showEditPanel
				//
				, isSharedAccountSelected = _view.isRadioIdSelected('sp-print-delegation-select-account-radio', 'sp-print-delegation-select-account-shared')
				//
				, showSharedAccountSelect = isSharedAccountSelected
				//
				;

				if (_isModeAddGroups()) {
					showAddGroups = true;
					showButtonAdd = true;
				} else if (_isModeAddUsers()) {
					showAddGroups = true;
					showAddUsers = true;
					showButtonAdd = true;
				} else {
					showEditPanel = true;
					showSharedAccountSelect = false;
				}

				_view.visible($('.sp-print-delegation-mode-add'), showAddGroups || showAddUsers);

				_view.visible($('.sp-print-delegation-mode-add-groups'), showAddGroups);
				_view.visible($('.sp-print-delegation-mode-add-users'), showAddUsers);
				_view.visible($('.sp-print-delegation-mode-edit'), showEditPanel);

				_view.visible($('#sp-print-delegation-button-add'), showButtonAdd);
				_view.visible($('.sp-print-delegation-select-shared-account'), showSharedAccountSelect);

				_setVisibilityDelegatorsEdit();
			}
			//----------------------------------------------------------------
			, _onGroupQuickSearch = function(target, filter) {
				/* QuickSearchFilterDto */
				var res, html = "";

				_quickUserGroupCache = {};
				_quickUserGroupSelected = {};
				_nSelectedGroups = 0;

				res = _api.call({
					request : "usergroup-quick-search",
					dto : JSON.stringify({
						filter : filter,
						maxResults : 5
					})
				});

				if (res.result.code === '0') {

					$.each(res.dto.items, function(key, item) {

						_quickUserGroupCache[item.key] = item;

						html += "<li class=\"ui-mini ui-li-has-icon\" data-icon=\"false\" data-savapage=\"" + item.key + "\">";
						html += "<a href=\"#\">";
						html += "<span class=\"sp-print-delegation-add-group-ind\" style=\"font-family: monospace;\">&nbsp;</span>&nbsp;";
						html += item.text + "<span class=\"ui-li-count\">" + item.userCount + "</span></a></li>";
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
						maxResults : 5
					})
				});

				if (res.result.code === '0') {

					$.each(res.dto.items, function(key, item) {

						_quickAccountCache[item.key] = item;

						html += "<li class=\"ui-mini ui-li-has-icon\" data-icon=\"false\" data-savapage=\"" + item.key + "\">";
						html += "<a href=\"#\">";
						html += item.text + "</a></li>";
					});
				} else {
					_view.showApiMsg(res);
				}

				target.html(html).filterable("refresh");
			}
			//----------------------------------------------------------------
			, _onAddGroups = function(account) {
				var html = $('#sp-selected-print-delegation tbody');
				$.each(_quickUserGroupSelected, function(key, item) {
					if (!_delegatorGroups[item.key]) {
						_delegatorGroups[item.key] = {
							item : item,
							account : account
						};
						html.append('<tr data-savapage="' + item.key + '"><th><span class="sp-print-delegation-selected-delegator-ind">&nbsp;</span>&nbsp;' + item.text + '</th><td>' + account.name + '</td></tr>');
						_nDelegatorGroups++;
					}
				});

				_setVisibilityDelegatorsEdit();
			}
			//----------------------------------------------------------------
			, _onAddUsers = function() {
			}
			//----------------------------------------------------------------
			, _onGroupSelected = function(selection) {
				var span = selection.find('.sp-print-delegation-add-group-ind')
				//
				, dbkey = selection.attr('data-savapage')
				//
				;

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
			}
			//----------------------------------------------------------------
			, _onDelegatorSelectRow = function(selection) {
				var span = selection.find('.sp-print-delegation-selected-delegator-ind'), selected
				//
				, dbkey = selection.attr('data-savapage')
				//
				;
				if (_delegatorGroupsSelected[dbkey]) {
					_nSelectedDelegatorGroups--;
					delete _delegatorGroupsSelected[dbkey];
					span.html(_IND_DESELECTED);
				} else {
					_nSelectedDelegatorGroups++;
					_delegatorGroupsSelected[dbkey] = _delegatorGroups[dbkey];
					span.html(_IND_SELECTED);
				}

				_setVisibilityDelegatorsEdit();

			}
			//----------------------------------------------------------------
			, _onDelegatorSelectAll = function() {
				_initDelegatorSelection();
				$.each($('.sp-print-delegation-selected-delegator-ind'), function() {
					_onDelegatorSelectRow($(this).closest('tr'));
				});
			}
			//----------------------------------------------------------------
			, _onDelegatorDeselectAll = function() {
				_initDelegatorSelection();
				$.each($('.sp-print-delegation-selected-delegator-ind'), function() {
					$(this).html(_IND_DESELECTED);
				});
			}
			//----------------------------------------------------------------
			, _onDelegatorRemoveSelected = function() {

				$.each($('.sp-print-delegation-selected-delegator-ind').closest('tr'), function() {
					var dbkey = $(this).attr('data-savapage');

					if (_delegatorGroupsSelected[dbkey]) {
						delete _delegatorGroups[dbkey];
						_nDelegatorGroups--;
						$(this).remove();
					} else if (_delegatorUsersSelected[dbkey]) {
						delete _delegatorUsers[dbkey];
						$(this).remove();
						_nDelegatorUsers--;
					}

				});
				_initDelegatorSelection();
			}
			//
			;

			//----------------------------------------------------------------
			$('#page-print-delegation').on('pagecreate', function(event) {

				var filterableGroups = $("#sp-print-delegation-groups-select-to-add-filter")
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

				$('#sp-print-delegation-button-add').click(function() {
					if (_isModeAddGroups()) {
						_onAddGroups({
							id : null,
							name : 'accountName'
						});
					} else {
						_onAddUsers();
					}
					return false;
				});

				$('input[name="sp-print-delegation-edit-radio"]').click(function() {
					_setVisibility();
				});

				$('input[name="sp-print-delegation-select-account-radio"]').click(function() {
					_setVisibility();
				});

				//
				$(this).on('click', '#sp-selected-print-delegation-rows tr', null, function() {
					_onDelegatorSelectRow($(this));
				});

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
				// Account selection
				//---------------------
				filterableAccounts.on("filterablebeforefilter", function(e, data) {
					e.preventDefault();
					_onAccountQuickSearch($(this), data.input.get(0).value);
				});

				$(this).on('click', '#sp-print-delegation-select-shared-account-filter li', null, function() {
					//_onAccountSelected($(this));
				});

				// Show available accounts on first open
				_onAccountQuickSearch(filterableAccounts, "");

			}).on("pagebeforeshow", function(event, ui) {
				_setVisibility();
			});
		};

	}(jQuery, this, this.document, JSON, this.org.savapage));
