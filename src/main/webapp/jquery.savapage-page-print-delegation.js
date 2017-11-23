/*! SavaPage jQuery Mobile Print Delegation Page | (c) 2011-2017 Datraverse B.V.
 * | GNU Affero General Public License */

/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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

( function($, window, document, JSON, _ns) {
        "use strict";

        _ns.PagePrintDelegation = function(_i18n, _view, _model, _api) {

            var _util = _ns.Utils,
                _this = this
            //
            ,
                _ACCOUNT_ENUM_GROUP = 'GROUP',
                _ACCOUNT_ENUM_USER = 'USER',
                _ACCOUNT_ENUM_SHARED = 'SHARED'
            //
            ,
                _RADIO_ACCOUNT_NAME = 'sp-print-delegation-select-account-radio'
            //
            ,
                _RADIO_ACCOUNT_ID_GROUP = 'sp-print-delegation-select-account-group'
            //
            ,
                _RADIO_ACCOUNT_ID_USER = 'sp-print-delegation-select-account-user'
            //
            ,
                _RADIO_ACCOUNT_ID_SHARED = 'sp-print-delegation-select-account-shared'
            //
            ,
                _IND_SELECTED = '+',
                _IND_DESELECTED = '&nbsp;',
                _QS_MAX_RESULTS = 5
            //
            ,
                _CLASS_GROUP = '_sp_group',
                _CLASS_USER = '_sp_user',
                _CLASS_COPIES = '_sp_copies'
            //
            ,
                _CLASS_SELECTED_IND = '_sp_selected',
                _CLASS_SELECTED_TXT = '_sp_selected_txt'
            //
            ,
                _quickUserGroupFilter,
                _quickUserFilter,
                _quickAccountFilter
            //
            ,
                _quickUserGroupCache,
                _quickUserCache,
                _quickAccountCache
            //
            ,
                _quickUserGroupSelected,
                _quickUserSelected,
                _quickAccountSelected
            //
            ,
                _nSelectedGroups,
                _nSelectedUsers
            //
            ,
                _delegatorGroups = {},
                _delegatorUsers = {},
                _delegatorCopies = {}
            //
            ,
                _nDelegatorGroups = 0,
                _nDelegatorUsers = 0,
                _nDelegatorGroupMembers = 0,
                _nDelegatorCopies = 0,

                _nDelegatorUsersCopies = 0,
                _nDelegatorGroupMembersCopies = 0
            //
            ,
                _delegatorGroupsSelected,
                _delegatorUsersSelected,
                _delegatorCopiesSelected
            //
            ,
                _nSelectedDelegatorGroups,
                _nSelectedDelegatorUsers,
                _nSelectedDelegatorCopies

            //----------------------------------------------------------------
            ,
                _delegationModel = function() {
                return {
                    name : 'a name',
                    groups : _delegatorGroups,
                    users : _delegatorUsers,
                    copies : _delegatorCopies
                };
            }
            //----------------------------------------------------------------
            ,
                _createModelAccount = function(account, userCount, userCopies) {
                // Java: PrintDelegationDto.DelegatorAccount
                return {
                    type : account.accountType,
                    id : account.id,
                    userCount : userCount,
                    userCopies : userCopies
                };
            }
            //----------------------------------------------------------------
            ,
                _onSaveModel = function(selection) {
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
            ,
                _revertQuickUserGroupsSelected = function() {
                _quickUserGroupSelected = {};
                _nSelectedGroups = 0;

                $.each($('#sp-print-delegation-groups-select-to-add-filter .' + _CLASS_SELECTED_TXT), function() {
                    $(this).html(_IND_DESELECTED);
                });
            }
            //
            ,
                _revertQuickUserSelected = function() {
                _quickUserSelected = {};
                _nSelectedUsers = 0;

                $.each($('#sp-print-delegation-users-select-to-add-filter .' + _CLASS_SELECTED_TXT), function() {
                    $(this).html(_IND_DESELECTED);
                });
            }
            //
            ,
                _isAccountSharedSelected = function() {
                return _view.isRadioIdSelected(_RADIO_ACCOUNT_NAME, _RADIO_ACCOUNT_ID_SHARED);
            }
            //
            ,
                _setVisibilityDelegatorsEdit = function() {
                var selectedRows = _nSelectedDelegatorGroups + _nSelectedDelegatorUsers + _nSelectedDelegatorCopies > 0;

                _view.enable($('.sp-print-delegator-rows'), _nDelegatorGroups + _nDelegatorUsers + _nDelegatorCopies > 0);
                _view.enable($('.sp-print-delegator-rows-selected'), selectedRows);
                _view.enable($('#sp-print-delegation-button-remove-selected'), selectedRows);
            }
            //----------------------------------------------------------------
            ,
                _initDelegatorSelection = function() {
                _nSelectedDelegatorGroups = 0;
                _nSelectedDelegatorUsers = 0;
                _nSelectedDelegatorCopies = 0;

                _delegatorGroupsSelected = {};
                _delegatorUsersSelected = {};
                _delegatorCopiesSelected = {};

                _setVisibilityDelegatorsEdit();
            }
            //----------------------------------------------------------------
            ,
                _isModeAddGroups = function() {
                return $('#sp-print-delegation-edit-radio-add-groups').is(':checked');
            }
            //----------------------------------------------------------------
            ,
                _isModeAddUsers = function() {
                return $('#sp-print-delegation-edit-radio-add-users').is(':checked');
            }
            //----------------------------------------------------------------
            ,
                _isModeAddCopies = function() {
                return $('#sp-print-delegation-edit-radio-add-copies').is(':checked');
            }
            //----------------------------------------------------------------
            ,
                _setVisibility = function() {
                var showButtonAdd,
                    enableButtonAdd,
                    showAddGroups,
                    showAddUsers;

                if (_isModeAddGroups()) {
                    showAddGroups = true;
                    showButtonAdd = true;
                    enableButtonAdd = _nSelectedGroups > 0;
                } else if (_isModeAddUsers()) {
                    showAddGroups = true;
                    showAddUsers = true;
                    showButtonAdd = true;
                    enableButtonAdd = _nSelectedUsers > 0;
                } else if (_isModeAddCopies()) {
                    showAddGroups = false;
                    showAddUsers = false;
                    showButtonAdd = true;
                    enableButtonAdd = true;
                }

                _view.visible($('.sp-print-delegation-mode-add'), showAddGroups || showAddUsers);

                _view.visible($('.sp-print-delegation-mode-add-groups'), showAddGroups);
                _view.visible($('.sp-print-delegation-mode-add-users'), showAddUsers);

                _view.visible($('#sp-print-delegation-button-add'), showButtonAdd);
                _view.enable($('#sp-print-delegation-button-add'), enableButtonAdd);

                _view.visible($('.sp-print-delegation-select-shared-account'), _isAccountSharedSelected());

                _setVisibilityDelegatorsEdit();
            }
            //----------------------------------------------------------------
            ,
                _getMemberCopies = function() {
                return parseInt($('#sp-print-delegation-member-copies').val(), 10) || 1;
            }
            //----------------------------------------------------------------
            ,
                _resetMemberCopies = function() {
                $('#sp-print-delegation-member-copies').val(1);
            }
            //----------------------------------------------------------------
            ,
                _clearUserList = function() {
                $("#sp-print-delegation-quicksearch-users .sp-quicksearch-buttons").hide();
                $("#sp-print-delegation-users-select-to-add-filter").html("");
            }
            //----------------------------------------------------------------
            ,
                _getAccount = function() {
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
            ,
                _setQuickSearchNavigation = function(qsButtons, res) {
                var selWlk;
                if (qsButtons) {
                    selWlk = qsButtons.find('.sp-button-prev');
                    selWlk.attr('data-savapage', res.dto.prevPosition).show();

                    selWlk = qsButtons.find('.sp-button-curr');
                    selWlk.attr('data-savapage', res.dto.currPosition).show();
                    selWlk.html(res.dto.currPage);

                    selWlk = qsButtons.find('.sp-button-next');
                    selWlk.attr('data-savapage', res.dto.nextPosition).show();

                    selWlk = qsButtons.find('.sp-button-last');
                    selWlk.attr('data-savapage', res.dto.lastPosition).show();
                    selWlk.html(res.dto.lastPage);

                    _view.visible(qsButtons, res.dto.lastPosition > 0);
                }
            }
            //----------------------------------------------------------------
            ,
                _onGroupQuickSearch = function(target, filter, startPosition, paging) {
                /* QuickSearchFilterUserGroupDto */
                var res,
                    selWlk,
                    html = "",
                    qsButtons = $("#sp-print-delegation-quicksearch-groups .sp-quicksearch-buttons");

                if (!paging && _quickUserGroupFilter === filter) {
                    return;
                }
                _quickUserGroupFilter = filter;
                _quickUserGroupCache = {};
                _quickUserGroupSelected = {};
                _nSelectedGroups = 0;

                res = _api.call({
                    request : "usergroup-quick-search",
                    dto : JSON.stringify({
                        filter : filter,
                        maxResults : _QS_MAX_RESULTS,
                        startPosition : startPosition,
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

                    _setQuickSearchNavigation(qsButtons, res);

                } else {
                    _view.showApiMsg(res);
                }

                target.html(html).filterable("refresh");
            }
            //----------------------------------------------------------------
            ,
                _onUserQuickSearch = function(target, groupId, filter, startPosition, paging) {
                /* QuickSearchUserGroupMemberFilterDto */
                var res,
                    selWlk,
                    html = "",
                    qsButtons = $("#sp-print-delegation-quicksearch-users .sp-quicksearch-buttons");

                if (!paging && _quickUserFilter === filter) {
                    return;
                }
                _quickUserFilter = filter;
                _quickUserCache = {};
                _quickUserSelected = {};
                _nSelectedUsers = 0;

                res = _api.call({
                    request : "usergroup-member-quick-search",
                    dto : JSON.stringify({
                        groupId : groupId,
                        filter : filter,
                        startPosition : startPosition,
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

                    _setQuickSearchNavigation(qsButtons, res);

                } else {
                    _view.showApiMsg(res);
                }

                target.html(html).filterable("refresh");
            }
            //----------------------------------------------------------------
            ,
                _onAccountQuickSearch = function(target, filter) {
                /* QuickSearchFilterDto */
                var res,
                    html = "";

                if (_quickAccountFilter === filter) {
                    return;
                }
                _quickAccountFilter = filter;
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
            ,
                _appendDelegatorRow = function(tbody, item, account, cssClass, nCopies) {
                var html,
                    isGroup = cssClass === _CLASS_GROUP,
                    isCopies = cssClass === _CLASS_COPIES,
                    nMembers = isGroup || isCopies ? item.userCount : 1;

                html = '<tr class="';
                html += cssClass;
                html += '" data-savapage="' + item.key + '">';
                html += '<th><span class="' + _CLASS_SELECTED_TXT + '">&nbsp;</span>&nbsp;';
                html += '<img src="/famfamfam-silk/';
                html += isGroup ? 'group.png' : isCopies ? 'tag_green.png' : 'user_gray.png';
                html += '"/>&nbsp;&nbsp;&nbsp;';

                if (isCopies) {
                    html += '';
                } else if (isGroup) {
                    html += item.text;
                } else {
                    html += item.fullName;
                    if (item.userId) {
                        html += ' &bull; ' + item.userId;
                    }
                }
                html += '</th>';

                html += '<td>' + ( isCopies ? '' : nMembers) + '</td>';
                html += '<td>' + nCopies + '</td>';
                html += '<td><img src="/famfamfam-silk/';

                if (account.accountType === _ACCOUNT_ENUM_GROUP) {
                    html += 'group.png';
                } else if (account.accountType === _ACCOUNT_ENUM_USER) {
                    html += 'user_gray.png';
                } else {
                    html += 'tag_green.png';
                }
                html += '"/>';

                if (account.accountType === _ACCOUNT_ENUM_SHARED) {
                    html += '&nbsp;&nbsp;&nbsp;' + account.text;
                }

                html += '</td></tr>';
                tbody.append(html);
            }
            //----------------------------------------------------------------
            ,
                _getDelegatorRowBody = function() {
                return $('#sp-selected-print-delegation tbody');
            }
            //
            ,
                _moveCopiesButtons = function(selTarget) {
                var src = $('#sp-print-delegation-table-copies-add'),
                    html;
                html = src.html();
                if (!html || html.length == 0) {
                    src = $("#sp-print-delegation-quicksearch-groups .sp-quicksearch-button-group-inject");
                    html = src.html();
                    if (!html || html.length == 0) {
                        src = $("#sp-print-delegation-quicksearch-users .sp-quicksearch-button-group-inject");
                        html = src.html();
                        if (!html || html.length == 0) {
                            src = $("#sp-print-delegation-edit-radio-inject");
                            html = src.html();
                        }
                    }
                }

                // Remove the current handler ...
                $('#sp-print-delegation-button-add').off('click');

                // ... move the html ...
                src.empty();
                selTarget.html(html);

                // .. and add new handler.
                $('#sp-print-delegation-button-add').on('click', function() {
                    if (_isModeAddGroups()) {
                        _onAddGroups();
                    } else if (_isModeAddCopies()) {
                        _onAddCopies();
                    } else {
                        _onAddUsers();
                    }
                    return false;
                });

            }
            //
            ,
                _moveCopiesButtons2Groups = function() {
                _moveCopiesButtons($("#sp-print-delegation-quicksearch-groups .sp-quicksearch-button-group-inject"));
            }
            //
            ,
                _moveCopiesButtons2Users = function() {
                _moveCopiesButtons($("#sp-print-delegation-quicksearch-users .sp-quicksearch-button-group-inject"));
            }
            //
            ,
                _moveCopiesButtons2Extra = function() {
                _moveCopiesButtons($("#sp-print-delegation-edit-radio-inject"));
            }
            //----------------------------------------------------------------
            ,
                _onAddGroups = function() {
                var tbody = _getDelegatorRowBody(),
                    account = _getAccount(),
                    memberCopies = _getMemberCopies();

                $.each(_quickUserGroupSelected, function(key, item) {
                    var copiesWlk;
                    if (!_delegatorGroups[item.key]) {
                        copiesWlk = item.userCount * memberCopies;
                        _delegatorGroups[item.key] = _createModelAccount(account, item.userCount, memberCopies);
                        _appendDelegatorRow(tbody, item, account, _CLASS_GROUP, copiesWlk);
                        _nDelegatorGroups++;
                        _nDelegatorGroupMembers += item.userCount;
                        _nDelegatorGroupMembersCopies += copiesWlk;
                    }
                });

                _revertQuickUserGroupsSelected();
                _resetMemberCopies();
                _setVisibilityDelegatorsEdit();
            }
            //----------------------------------------------------------------
            ,
                _onAddUsers = function() {
                var tbody = _getDelegatorRowBody(),
                    account = _getAccount(),
                    memberCopies = _getMemberCopies();

                $.each(_quickUserSelected, function(key, item) {

                    if (!_delegatorUsers[item.key]) {
                        _delegatorUsers[item.key] = _createModelAccount(account, 1, memberCopies);
                        _appendDelegatorRow(tbody, item, account, _CLASS_USER, memberCopies);
                        _nDelegatorUsers++;
                        _nDelegatorUsersCopies += memberCopies;
                    }
                });

                _revertQuickUserSelected();
                _resetMemberCopies();
                _setVisibilityDelegatorsEdit();
            }
            //----------------------------------------------------------------
            ,
                _onAddCopies = function() {
                var selCopies = $('#sp-print-delegation-copies-to-add')
                //
                ,
                    userCount = parseInt(selCopies.val(), 10)
                //
                ,
                    tbody = _getDelegatorRowBody(),
                    account = _getAccount()
                //
                ,
                    item = {
                    key : account.id,
                    text : $('#sp-print-delegation-edit-radio-add-copies-label').text(),
                    userCount : userCount
                };

                if (!_delegatorCopies[item.key]) {
                    _delegatorCopies[item.key] = _createModelAccount(account, userCount, userCount);
                    _appendDelegatorRow(tbody, item, account, _CLASS_COPIES, userCount);
                    _nDelegatorCopies += userCount;
                }

                selCopies.val(1);

                _setVisibilityDelegatorsEdit();
            }
            //----------------------------------------------------------------
            ,
                _onGroupSelected = function(selection) {
                var span = selection.find('.' + _CLASS_SELECTED_TXT)
                //
                ,
                    dbkey = selection.attr('data-savapage'),
                    dbKeySelected
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
                    _quickUserFilter = undefined;

                    _onUserQuickSearch($("#sp-print-delegation-users-select-to-add-filter"), dbkey, $('#sp-print-delegation-users-select-to-add').val(), 0);

                }
            }
            //----------------------------------------------------------------
            ,
                _onUserSelected = function(selection) {
                var span = selection.find('.' + _CLASS_SELECTED_TXT)
                //
                ,
                    dbkey = selection.attr('data-savapage')
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
            ,
                _onAccountSelected = function(selection) {
                var span = selection.find('.' + _CLASS_SELECTED_TXT)
                //
                ,
                    dbkey = selection.attr('data-savapage')
                //
                ,
                    dbKeySelected = _quickAccountSelected ? _quickAccountSelected.key : null
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
            ,
                _onDelegatorSelectRow = function(selection) {
                var span = selection.find('.' + _CLASS_SELECTED_TXT)
                //
                ,
                    isGroup = selection.hasClass(_CLASS_GROUP)
                //
                ,
                    isCopies = selection.hasClass(_CLASS_COPIES)
                //
                ,
                    dbkey = selection.attr('data-savapage')
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
                } else if (isCopies) {
                    if (_delegatorCopiesSelected[dbkey]) {
                        _nSelectedDelegatorCopies--;
                        delete _delegatorCopiesSelected[dbkey];
                        span.html(_IND_DESELECTED);
                    } else {
                        _nSelectedDelegatorCopies++;
                        _delegatorCopiesSelected[dbkey] = _delegatorCopies[dbkey];
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
            ,
                _onDelegatorSelectAll = function() {
                _initDelegatorSelection();
                $.each($('#sp-selected-print-delegation-rows .' + _CLASS_SELECTED_TXT), function() {
                    _onDelegatorSelectRow($(this).closest('tr'));
                });
            }
            //----------------------------------------------------------------
            ,
                _onDelegatorDeselectAll = function() {
                _initDelegatorSelection();
                $.each($('#sp-selected-print-delegation-rows .' + _CLASS_SELECTED_TXT), function() {
                    $(this).html(_IND_DESELECTED);
                });
            }
            //----------------------------------------------------------------
            ,
                _onDelegatorRemoveSelected = function() {

                $.each($('.' + _CLASS_SELECTED_TXT).closest('tr'), function() {
                    var dbkey = $(this).attr('data-savapage');

                    if (_delegatorGroupsSelected[dbkey]) {
                        _nDelegatorGroups--;
                        _nDelegatorGroupMembers -= _delegatorGroups[dbkey].userCount;
                        _nDelegatorGroupMembersCopies -= _delegatorGroups[dbkey].userCount * _delegatorGroups[dbkey].userCopies;
                        delete _delegatorGroups[dbkey];
                        $(this).remove();
                    } else if (_delegatorCopiesSelected[dbkey]) {
                        _nDelegatorCopies -= _delegatorCopies[dbkey].userCount;
                        delete _delegatorCopies[dbkey];
                        $(this).remove();
                    } else if (_delegatorUsersSelected[dbkey]) {
                        _nDelegatorUsers--;
                        _nDelegatorUsersCopies -= _delegatorUsers[dbkey].userCopies;
                        delete _delegatorUsers[dbkey];
                        $(this).remove();
                    }

                });
                _initDelegatorSelection();
            }
            //
            ,
                _onChangeEditMode = function(id) {
                var isCopies = id === 'sp-print-delegation-edit-radio-add-copies';

                if (isCopies) {

                    _view.checkRadio(_RADIO_ACCOUNT_NAME, _RADIO_ACCOUNT_ID_SHARED);
                    _view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_SHARED), true);
                    _view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_GROUP), false);
                    _view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_USER), false);

                    _moveCopiesButtons2Extra();

                } else if (id === 'sp-print-delegation-edit-radio-add-groups') {

                    _view.checkRadio(_RADIO_ACCOUNT_NAME, _RADIO_ACCOUNT_ID_USER);

                    _view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_USER), true);
                    _view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_GROUP), true);
                    _view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_SHARED), true);

                    _moveCopiesButtons2Groups();

                } else if (id === 'sp-print-delegation-edit-radio-add-users') {

                    _view.checkRadio(_RADIO_ACCOUNT_NAME, _RADIO_ACCOUNT_ID_USER);
                    _view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_USER), true);
                    _view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_GROUP), false);
                    _view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_SHARED), false);

                    _moveCopiesButtons2Users();

                    // Deselect all groups
                    _revertQuickUserGroupsSelected();

                    // Clear users
                    _quickUserFilter = undefined;
                    _quickUserCache = {};
                    _quickUserSelected = {};
                    _nSelectedUsers = 0;

                    _onUserQuickSearch($("#sp-print-delegation-users-select-to-add-filter"), null, $('#sp-print-delegation-users-select-to-add').val(), 0);

                    _view.enable($('#sp-print-delegation-button-add'), false);

                } else {
                    _view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_GROUP), false);
                    _view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_USER), true);
                    _view.enableCheckboxRadio($('#' + _RADIO_ACCOUNT_ID_SHARED), true);
                }

                _view.visible($('#sp-print-delegation-copies-to-add').parent(), isCopies);
                _view.visible($('#sp-print-delegation-member-copies').parent(), !isCopies);
            }
            //
            ,
                _updateModel = function() {
                _model.printDelegation = _delegationModel();
                _model.printDelegationCopies = _nDelegatorGroupMembersCopies + _nDelegatorUsersCopies + _nDelegatorCopies;
            }
            //
            ;

            //--------
            this.clear = function() {
                _resetMemberCopies();
                $('#sp-print-delegation-copies-to-add').val(1);
                _onDelegatorSelectAll();
                _onDelegatorRemoveSelected();
                _updateModel();
                _this.onBeforeHide();
            };

            //----------------------------------------------------------------
            $('#page-print-delegation').on('pagecreate', function(event) {

                var filterableGroups = $("#sp-print-delegation-groups-select-to-add-filter")
                //
                ,
                    filterableUsers = $("#sp-print-delegation-users-select-to-add-filter")
                //
                ,
                    filterableAccounts = $("#sp-print-delegation-select-shared-account-filter")
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
                    _onGroupQuickSearch($(this), data.input.get(0).value, 0);
                });

                $(this).on('click', '#sp-print-delegation-groups-select-to-add-filter li', null, function() {
                    _onGroupSelected($(this));
                });

                // Show available groups on first open
                _onGroupQuickSearch(filterableGroups, "", 0);

                //---------------------
                // User selection
                //---------------------
                filterableUsers.on("filterablebeforefilter", function(e, data) {
                    var dbkey = _util.getFirstProp(_quickUserGroupSelected);
                    e.preventDefault();
                    _onUserQuickSearch($(this), dbkey, data.input.get(0).value, 0);
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

                //-----------------------
                // Group list navigation
                //-----------------------
                $("#sp-print-delegation-quicksearch-groups .sp-quicksearch-buttons .ui-btn").click(function() {
                    _onGroupQuickSearch($("#sp-print-delegation-groups-select-to-add-filter"), $('#sp-print-delegation-groups-select-to-add').val(), $(this).attr('data-savapage'), true);
                });

                //-----------------------
                // User list navigation
                //-----------------------
                $("#sp-print-delegation-quicksearch-users .sp-quicksearch-buttons .ui-btn").click(function() {
                    var dbkey = _util.getFirstProp(_quickUserGroupSelected),
                        nextPosition = $(this).attr('data-savapage');
                    _onUserQuickSearch($("#sp-print-delegation-users-select-to-add-filter"), dbkey, $('#sp-print-delegation-users-select-to-add').val(), nextPosition, true);
                });

                // Show available accounts on first open
                _onAccountQuickSearch(filterableAccounts, "");

            }).on("pagebeforeshow", function(event, ui) {
                _setVisibility();
            }).on("pagebeforehide", function(event, ui) {
                _updateModel();
                _this.onBeforeHide();
            });
        };

    }(jQuery, this, this.document, JSON, this.org.savapage));
