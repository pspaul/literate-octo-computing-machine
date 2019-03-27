/*! SavaPage jQuery Mobile Print Delegation Page | (c) 2011-2019 Datraverse B.V.
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

( function($, window, document, JSON, _ns) {
        "use strict";

        _ns.PagePrintDelegation = function(_i18n, _view, _model, _api) {

            var _util = _ns.Utils,
                _this = this,
                _previousPageId,

                _ACCOUNT_ENUM_GROUP = 'GROUP',
                _ACCOUNT_ENUM_USER = 'USER',
                _ACCOUNT_ENUM_SHARED = 'SHARED',

                _RADIO_ACCOUNT_NAME = 'sp-print-delegation-select-account-radio',
                _RADIO_ACCOUNT_ID_GROUP = 'sp-print-delegation-select-account-group',
                _RADIO_ACCOUNT_ID_USER = 'sp-print-delegation-select-account-user',
                _RADIO_ACCOUNT_ID_SHARED = 'sp-print-delegation-select-account-shared',

                _RADIO_EDIT_NAME = 'sp-print-delegation-edit-radio',
                _RADIO_EDIT_ID_GROUPS = _RADIO_EDIT_NAME + '-add-groups',
                _RADIO_EDIT_ID_USERS = _RADIO_EDIT_NAME + '-add-users',
                _RADIO_EDIT_ID_COPIES = _RADIO_EDIT_NAME + '-add-copies',

                _IND_SELECTED_ON = '<img height="12" src="/images/selected-green-16x16.png">&nbsp;',
                _IND_SELECTED_OFF = '<img height="12" src="/images/unselected-16x16.png">&nbsp;',

                _IND_PREFERRED_ON = '&nbsp;<img height="10" src="/famfamfam-silk/star.png">&nbsp;',
                _IND_PREFERRED_OFF = '&nbsp;<img height="10" src="/famfamfam-silk/star_gray.png">&nbsp;',

                _DATA_PREFERRED_ON = 'on',
                _DATA_PREFERRED_OFF = 'off',

                _QS_MAX_RESULTS = 5,

                _CLASS_GROUP = '_sp_group',
                _CLASS_USER = '_sp_user',
                _CLASS_COPIES = '_sp_copies',

                _CLASS_SELECTED_IND = '_sp_selected',
                _CLASS_SELECTED_TXT = '_sp_selected_txt',
                _CLASS_PREFERRED_IMG = '_sp_preferred_img',

                _quickUserGroupFilter,
                _quickUserFilter,
                _quickAccountFilter,

                _quickUserGroupCache,
                _quickUserCache,
                _quickAccountCache,

                _quickUserGroupTotalResults = null,
                _quickUserTotalResults = null,
                _quickAccountTotalResults = null,

                _quickUserGroupSelected,
                _quickUserSelected,
                _quickAccountSelected,

                _nSelectedGroups,
                _nSelectedUsers,

                _delegatorGroups = {},
                _delegatorUsers = {},
                _delegatorCopies = {},

                _nDelegatorGroups = 0,
                _nDelegatorUsers = 0,
                _nDelegatorCopies = 0,

                _nDelegatorUsersCopies = 0,
                _nDelegatorGroupMembersCopies = 0,

                _delegatorGroupsSelected,
                _delegatorUsersSelected,
                _delegatorCopiesSelected,

                _nSelectedDelegatorGroups,
                _nSelectedDelegatorUsers,
                _nSelectedDelegatorCopies,

                _delegationModel = function() {
                return {
                    name : 'a name',
                    groups : _delegatorGroups,
                    users : _delegatorUsers,
                    copies : _delegatorCopies
                };
            },
                _isUiStateDisabled = function(sel) {
                return sel.hasClass('ui-state-disabled');
            },
                _setUiStateDisabled = function(sel) {
                return sel.addClass('ui-state-disabled');
            },
                _removeUiStateDisabled = function(sel) {
                return sel.removeClass('ui-state-disabled');
            },
                _isUserSelectAllSwitchOn = function() {
                return _ns.SelectAllButtonSwitch.isOn($('#sp-print-delegation-users-select-to-add-all'));
            },
            //
                _createModelAccount = function(account, userCount, userCopies, totalCopies) {
                // Java: PrintDelegationDto.DelegatorAccount
                return {
                    type : account.accountType,
                    id : account.id,
                    userCount : userCount,
                    userCopies : userCopies,
                    totalCopies : totalCopies || (userCount * userCopies)
                };
            },

                _revertQuickUserGroupsSelected = function() {
                _quickUserGroupSelected = {};
                _nSelectedGroups = 0;

                $.each($('#sp-print-delegation-groups-select-to-add-filter .' + _CLASS_SELECTED_TXT), function() {
                    $(this).html(_IND_SELECTED_OFF);
                });
            },

                _switchQuickUserGroupToSingleSelect = function() {
                $('#sp-print-delegation-groups-select-to-add-filter .' + _CLASS_SELECTED_TXT).addClass(_CLASS_SELECTED_IND);
            },

                _revertQuickUserSelected = function() {
                _quickUserSelected = {};
                _nSelectedUsers = 0;

                $.each($('#sp-print-delegation-users-select-to-add-filter .' + _CLASS_SELECTED_TXT), function() {
                    $(this).html(_IND_SELECTED_OFF);
                });
            },
                _onGroupPreferred = function(selection) {
                _onObjectPreferred(selection, 'user-delegate-groups-preferred');
            },
                _isGroupSelectScopePreferred = function() {
                return _ns.PreferredButtonSwitch.isOn($('#sp-print-delegation-groups-select-to-add-scope'));
            },
                _isGroupPreferredEnabled = function() {
                return $('#sp-print-delegation-groups-select-to-add-scope').length > 0;
            }
            //
            ,
                _isAccountGroupSelected = function() {
                return _view.isRadioIdSelected(_RADIO_ACCOUNT_NAME, _RADIO_ACCOUNT_ID_GROUP);
            }
            //
            ,
                _isAccountUserSelected = function() {
                return _view.isRadioIdSelected(_RADIO_ACCOUNT_NAME, _RADIO_ACCOUNT_ID_USER);
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
                    showAddGroups,
                    showAddUsers;

                if (_isModeAddGroups()) {
                    showAddGroups = true;
                    showButtonAdd = true;
                } else if (_isModeAddUsers()) {
                    showAddGroups = true;
                    showAddUsers = true;
                    showButtonAdd = true;
                } else if (_isModeAddCopies()) {
                    showAddGroups = false;
                    showAddUsers = false;
                    showButtonAdd = true;
                }

                _view.visible($('.sp-print-delegation-mode-add'), showAddGroups || showAddUsers);

                _view.visible($('.sp-print-delegation-mode-add-groups'), showAddGroups);
                _view.visible($('.sp-print-delegation-mode-add-users'), showAddUsers);

                _view.visible($('#sp-print-delegation-button-add'), showButtonAdd);
                _view.visible($('#sp-print-delegation-button-add-popup'), showButtonAdd && showAddGroups && !showAddUsers);

                _view.visible($('.sp-print-delegation-select-shared-account'), _isAccountSharedSelected());

                _setVisibilityDelegatorsEdit();
            }
            //----------------------------------------------------------------
            ,
                _htmlDelegatorName = function(item) {
                var html = '',
                    sessionUser = item.userId === _model.user.id;

                if (sessionUser) {
                    html += '<span class=\"sp-txt-warn\">';
                }
                html += item.fullName;
                if (!_model.DELEGATOR_USER_HIDE_ID && item.fullName.indexOf(item.userId) < 0) {
                    html += ' &bull; ';
                    html += item.userId;
                }
                if (sessionUser) {
                    html += '</span>';
                }
                return html;
            }
            //----------------------------------------------------------------
            ,
                _htmlGroupName = function(item) {
                var fullName,
                    html;
                fullName = item.fullName || '';
                html = fullName;
                if (!_model.DELEGATOR_GROUP_HIDE_ID && fullName.toLowerCase() !== item.text.toLowerCase()) {
                    if (html.length > 0) {
                        html += ' &bull; ';
                    }
                    html += item.text;
                }
                return html;
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
                _getPopupGroupCopies = function() {
                return parseInt($('#sp-print-delegation-popup-copies-to-add').val(), 10) || 1;
            }
            //----------------------------------------------------------------
            ,
                _resetPopupGroupCopies = function() {
                $('#sp-print-delegation-popup-copies-to-add').val(1);
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
                _selectQuickSearchSingleResult = function(selList) {
                var li = selList.find('li');
                if (li.length === 1) {
                    li.click();
                }
            }
            //----------------------------------------------------------------
            ,
                _onQuickSearchAccountVisibility = function(refId, filter, totResults, preferredScope) {
                var visible = preferredScope || filter.length !== 0 || totResults > 1;
                _view.visible($('#' + refId + ' .sp-quicksearch-input'), visible);
                _view.visible($('#' + refId + ' ul'), visible);
            },
            //----------------------------------------------------------------
                _showQuickSearchGroupsAdded = function() {
                $.each($('#sp-print-delegation-groups-select-to-add-filter li'), function() {
                    var addedAsDelegator = _delegatorGroups[$(this).attr('data-savapage')];
                    if (addedAsDelegator) {
                        _setUiStateDisabled($(this));
                    } else {
                        _removeUiStateDisabled($(this));
                    }
                });
            },
                _onQuickSearchGroup = function(target, filter, startPosition, paging, rescope) {
                /* QuickSearchFilterUserGroupDto */
                var res,
                    html = "",
                    qsButtons = $("#sp-print-delegation-quicksearch-groups .sp-quicksearch-buttons"),
                    preferredEnabled = _isGroupPreferredEnabled();

                if (!rescope && !paging && _quickUserGroupFilter === filter) {
                    return;
                }
                if (!paging || rescope) {
                    _quickUserGroupTotalResults = null;
                }

                _quickUserGroupFilter = filter;
                _quickUserGroupCache = {};
                _quickUserGroupSelected = {};
                _nSelectedGroups = 0;

                res = _api.call({
                    request : "usergroup-quick-search",
                    dto : JSON.stringify({
                        filter : filter,
                        totalResults : _quickUserGroupTotalResults,
                        maxResults : _QS_MAX_RESULTS,
                        startPosition : startPosition,
                        aclRole : 'PRINT_DELEGATOR',
                        preferred : _isGroupSelectScopePreferred(),
                        hideId : _model.DELEGATOR_GROUP_HIDE_ID
                    })
                });

                if (res.result.code === '0') {

                    $.each(res.dto.items, function(key, item) {

                        _quickUserGroupCache[item.key] = item;

                        html += "<li class=\"ui-mini ui-li-has-icon\" data-icon=\"false\" data-savapage=\"" + item.key + "\">";
                        html += "<a href=\"#\">";
                        html += "<span class=\"" + _CLASS_SELECTED_TXT + "\">" + _IND_SELECTED_OFF + "</span>&nbsp;";
                        html += _htmlGroupName(item) + "<span class=\"ui-li-count\">";
                        if (preferredEnabled) {
                            html += "<span class=\"" + _CLASS_PREFERRED_IMG + "\" data-savapage=\"";
                            html += item.preferred ? _DATA_PREFERRED_ON : _DATA_PREFERRED_OFF;
                            html += "\">";
                            html += item.preferred ? _IND_PREFERRED_ON : _IND_PREFERRED_OFF;
                            html += "</span>&nbsp;";
                        }
                        html += item.userCount + "</span></a></li>";
                    });

                    _setQuickSearchNavigation(qsButtons, res);
                    _quickUserGroupTotalResults = res.dto.totalResults;

                } else {
                    _view.showApiMsg(res);
                }

                target.html(html).filterable("refresh");
                _showQuickSearchGroupsAdded();

                _selectQuickSearchSingleResult(target);
                _onButtonAddVisibility();
            }
            //----------------------------------------------------------------
            ,
                _showQuickSearchUsersAdded = function() {
                $.each($('#sp-print-delegation-users-select-to-add-filter li'), function() {
                    var addedAsDelegator = _delegatorUsers[$(this).attr('data-savapage')];
                    if (addedAsDelegator) {
                        _setUiStateDisabled($(this));
                    } else {
                        _removeUiStateDisabled($(this));
                    }
                });
            },
                _onQuickSearchUser = function(target, groupId, filter, startPosition, paging) {
                /* QuickSearchUserGroupMemberFilterDto */
                var res,
                    html = "",
                    qsButtons = $("#sp-print-delegation-quicksearch-users .sp-quicksearch-buttons");

                if (!paging && _quickUserFilter === filter) {
                    return;
                }
                if (!paging) {
                    _quickUserTotalResults = null;
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
                        totalResults : _quickUserTotalResults,
                        maxResults : _QS_MAX_RESULTS,
                        aclRole : 'PRINT_DELEGATOR',
                        hideId : _model.DELEGATOR_USER_HIDE_ID
                    })
                });

                if (res.result.code === '0') {

                    $.each(res.dto.items, function(key, item) {

                        _quickUserCache[item.key] = item;

                        html += "<li class=\"ui-mini ui-li-has-icon";
                        html += "\" data-icon=\"false\" data-savapage=\"" + item.key;
                        html += "\">";
                        html += "<a href=\"#\">";
                        html += "<span class=\"" + _CLASS_SELECTED_TXT + "\">" + _IND_SELECTED_OFF + "</span>&nbsp;";
                        html += _htmlDelegatorName(item);
                        html += "</a>";
                        html += "</li>";
                    });

                    _setQuickSearchNavigation(qsButtons, res);
                    _quickUserTotalResults = res.dto.totalResults;

                } else {
                    _view.showApiMsg(res);
                }

                target.html(html).filterable("refresh");
                _showQuickSearchUsersAdded();

                if (_isUserSelectAllSwitchOn()) {
                    _usersSelectAll(true);
                } else {
                    _selectQuickSearchSingleResult(target);
                }
                _onButtonAddVisibility();
            }
            //----------------------------------------------------------------
            ,
                _onQuickSearchAccount = function(target, filter, startPosition, paging, rescope) {
                /* QuickSearchFilterDto */
                var res,
                    html = "",
                    refId = 'sp-print-delegation-quicksearch-accounts',
                    qsButtons = $('#' + refId + ' .sp-quicksearch-buttons'),
                    preferredEnabled = _isAccountPreferredEnabled(),
                    preferredScope = _isAccountSelectScopePreferred();

                if (!rescope && !paging && _quickAccountFilter === filter) {
                    return;
                }
                if (!paging || rescope) {
                    _quickAccountTotalResults = null;
                }

                _quickAccountFilter = filter;
                _quickAccountCache = {};
                _quickAccountSelected = undefined;

                res = _api.call({
                    request : "shared-account-quick-search",
                    dto : JSON.stringify({
                        filter : filter,
                        startPosition : startPosition,
                        totalResults : _quickAccountTotalResults,
                        maxResults : _QS_MAX_RESULTS,
                        preferred : preferredScope
                    })
                });

                if (res.result.code === '0') {

                    $.each(res.dto.items, function(key, item) {

                        _quickAccountCache[item.key] = item;

                        html += "<li class=\"ui-mini ui-li-has-icon\" data-icon=\"false\" data-savapage=\"" + item.key + "\">";
                        html += "<a href=\"#\">";
                        html += "<span class=\"" + _CLASS_SELECTED_TXT + "\">" + _IND_SELECTED_OFF + "</span>&nbsp;";
                        html += item.text;
                        if (preferredEnabled) {
                            html += "<span class=\"ui-li-count\">";
                            html += "<span class=\"" + _CLASS_PREFERRED_IMG + "\" data-savapage=\"";
                            html += item.preferred ? _DATA_PREFERRED_ON : _DATA_PREFERRED_OFF;
                            html += "\">";
                            html += item.preferred ? _IND_PREFERRED_ON : _IND_PREFERRED_OFF;
                            html += "</span></span>";
                        }
                        html += "</a></li>";
                    });

                    _setQuickSearchNavigation(qsButtons, res);
                    _quickAccountTotalResults = res.dto.totalResults;

                } else {
                    _view.showApiMsg(res);
                }

                target.html(html).filterable("refresh");
                _selectQuickSearchSingleResult(target);
                _onButtonAddVisibility();
                _onQuickSearchAccountVisibility(refId, filter, _quickAccountTotalResults, preferredScope);
            }
            //----------------------------------------------------------------
            ,
                _updateDelegatorRow = function(tbody, item, cssClass) {
                var row = tbody.find('[data-savapage=' + item.id + ']');
                if (cssClass !== _CLASS_COPIES) {
                    row.find(':nth-child(2)').html(item.userCount);
                }
                row.find(':nth-child(3)').html(item.totalCopies);
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
                html += '<th>';
                html += "<span class=\"" + _CLASS_SELECTED_TXT + "\">" + _IND_SELECTED_OFF + "</span>&nbsp;";
                html += '<img src="/famfamfam-silk/';
                html += isGroup ? 'group.png' : isCopies ? 'tag_green.png' : 'user_gray.png';
                html += '"/>&nbsp;&nbsp;&nbsp;';

                if (isCopies) {
                    html += '';
                } else if (isGroup) {
                    html += _htmlGroupName(item);
                } else {
                    html += _htmlDelegatorName(item);
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

                html += '&nbsp;&nbsp;&nbsp;';
                if (account.accountType === _ACCOUNT_ENUM_SHARED) {
                    html += account.text;
                } else if (account.accountType === _ACCOUNT_ENUM_GROUP) {
                    html += item.text;
                } else if (account.accountType === _ACCOUNT_ENUM_USER) {
                    html += _i18n.string('label-personal-account');
                }

                html += '</td></tr>';
                tbody.append(html);
            }
            //----------------------------------------------------------------
            ,
                _getDelegatorRowBody = function() {
                return $('#sp-selected-print-delegation tbody');
            }
            //----------------------------------------------------------------
            ,
                _onAddGroups = function(groupCopies) {
                var tbody = _getDelegatorRowBody(),
                    account = _getAccount(),
                    memberCopies = groupCopies ? 1 : _getMemberCopies();

                $.each(_quickUserGroupSelected, function(key, item) {
                    var modelAccount;
                    if (!_delegatorGroups[item.key]) {
                        modelAccount = _createModelAccount(account, item.userCount, memberCopies, groupCopies);
                        _delegatorGroups[item.key] = modelAccount;
                        _appendDelegatorRow(tbody, item, account, _CLASS_GROUP, modelAccount.totalCopies);
                        _nDelegatorGroups++;
                        _nDelegatorGroupMembersCopies += modelAccount.totalCopies;
                    }
                });

                _revertQuickUserGroupsSelected();
                _resetMemberCopies();

                if (groupCopies) {
                    _resetPopupGroupCopies();
                }
                _showQuickSearchGroupsAdded();

                _setVisibilityDelegatorsEdit();
                _onButtonAddVisibility();
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
                _resetMemberCopies();
                _revertQuickUserSelected();
                _showQuickSearchUsersAdded();

                _setVisibilityDelegatorsEdit();
                _onButtonAddVisibility();
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

                if (userCount < 1) {
                    userCount = 1;
                }

                if (_delegatorCopies[item.key]) {
                    _delegatorCopies[item.key].userCount += userCount;
                    _delegatorCopies[item.key].userCopies += 1;
                    _delegatorCopies[item.key].totalCopies += userCount;
                    _updateDelegatorRow(tbody, _delegatorCopies[item.key], _CLASS_COPIES);
                } else {
                    _delegatorCopies[item.key] = _createModelAccount(account, userCount, 1);
                    _appendDelegatorRow(tbody, item, account, _CLASS_COPIES, userCount);
                }

                _nDelegatorCopies += userCount;

                selCopies.val(1);

                _setVisibilityDelegatorsEdit();
                _onButtonAddVisibility();
            }
            //
            ,
                _moveCopiesButtons = function(selTarget) {
                var src = $('#sp-print-delegation-table-copies-add'),
                    html;
                html = src.html();
                if (!html || html.length === 0) {
                    src = $("#sp-print-delegation-quicksearch-groups .sp-quicksearch-button-group-inject");
                    html = src.html();
                    if (!html || html.length === 0) {
                        src = $("#sp-print-delegation-quicksearch-users .sp-quicksearch-button-group-inject");
                        html = src.html();
                        if (!html || html.length === 0) {
                            src = $("#sp-print-delegation-quicksearch-accounts .sp-quicksearch-button-group-inject");
                            html = src.html();
                        }
                    }
                }

                // Remove the current handlers ...
                $('#sp-print-delegation-button-add').off('click');
                $('#sp-print-delegation-button-add-popup').off('click');

                // ... move the html ...
                src.empty();
                selTarget.html(html);

                // .. and add new handlers.
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

                $('#sp-print-delegation-button-add-popup').on('click', function() {
                    var sel = $('#sp-print-delegation-popup-add-group-copies');
                    sel.popup('open', {
                        positionTo : $(this)
                    });
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
                _moveCopiesButtons2Accounts = function() {
                _moveCopiesButtons($("#sp-print-delegation-quicksearch-accounts .sp-quicksearch-button-group-inject"));
            }
            //----------------------------------------------------------------
            ,
                _onButtonAddVisibility = function() {
                var enable,
                    modeAddGroups = _isModeAddGroups();

                if (modeAddGroups) {
                    enable = _nSelectedGroups > 0 && (_isAccountSharedSelected() ? _quickAccountSelected : true);
                } else if (_isModeAddUsers()) {
                    enable = _nSelectedUsers > 0;
                } else if (_isModeAddCopies()) {
                    enable = _quickAccountSelected;
                }
                _view.enable($('#sp-print-delegation-button-add'), enable);
                _view.visible($('#sp-print-delegation-button-add-popup'), modeAddGroups);
                if (modeAddGroups) {
                    _view.enable($('#sp-print-delegation-button-add-popup'), enable);
                }
            }
            //----------------------------------------------------------------
            ,
                _onObjectPreferred = function(selection, request) {
                var span = selection.find('.' + _CLASS_PREFERRED_IMG),
                    dbKey = span.closest('li').attr('data-savapage'),
                    preferredNew = span.attr('data-savapage') !== _DATA_PREFERRED_ON,
                    res;

                span.html( preferredNew ? _IND_PREFERRED_ON : _IND_PREFERRED_OFF);
                span.attr('data-savapage', preferredNew ? _DATA_PREFERRED_ON : _DATA_PREFERRED_OFF);

                res = _api.call({
                    request : request,
                    dto : JSON.stringify({
                        dbKeys : [dbKey],
                        add : preferredNew
                    })
                });

                if (res.result.code !== '0') {
                    _view.showApiMsg(res);
                    // reverse
                    span.html( preferredNew ? _IND_PREFERRED_OFF : _IND_PREFERRED_ON);
                    span.attr('data-savapage', preferredNew ? _DATA_PREFERRED_OFF : _DATA_PREFERRED_ON);
                }
            }
            //----------------------------------------------------------------
            ,
                _usersSelectAll = function(select) {
                $.each($('#sp-print-delegation-users-select-to-add-filter li'), function() {
                    _onUserSelectIdempotent($(this), select);
                });
                _onButtonAddVisibility();
            }
            //----------------------------------------------------------------
            ,
                _onUsersSelectAllToggle = function(selection) {
                _usersSelectAll(_ns.SelectAllButtonSwitch.toggle(selection));
            }
            //----------------------------------------------------------------
            ,
                _onGroupSelectScope = function(selection) {
                var selected = _ns.PreferredButtonSwitch.toggle(selection);
                _api.call({
                    request : 'user-set-delegate-groups-preferred-select',
                    dto : JSON.stringify({
                        select : selected
                    })
                });

                _onQuickSearchGroup($("#sp-print-delegation-groups-select-to-add-filter"), $('#sp-print-delegation-groups-select-to-add').val(), 0, true, true);
            }
            //----------------------------------------------------------------
            ,
                _onGroupSelected = function(selection) {
                var span,
                    dbkey,
                    dbKeySelected;

                if (_isUiStateDisabled(selection)) {
                    return;
                }

                span = selection.find('.' + _CLASS_SELECTED_TXT);
                dbkey = selection.attr('data-savapage');

                if (_isModeAddGroups()) {

                    if (_quickUserGroupSelected[dbkey]) {
                        _nSelectedGroups--;
                        delete _quickUserGroupSelected[dbkey];
                        span.html(_IND_SELECTED_OFF);
                    } else {
                        _nSelectedGroups++;
                        _quickUserGroupSelected[dbkey] = _quickUserGroupCache[dbkey];
                        span.html(_IND_SELECTED_ON);
                    }

                } else {

                    dbKeySelected = _util.getFirstProp(_quickUserGroupSelected);

                    if (dbKeySelected) {
                        _nSelectedGroups--;
                        delete _quickUserGroupSelected[dbKeySelected];
                        selection.closest('ul').find('.' + _CLASS_SELECTED_IND).html(_IND_SELECTED_OFF).removeClass(_CLASS_SELECTED_IND);
                    }

                    if (dbKeySelected === dbkey) {
                        dbkey = null;
                    } else {
                        _nSelectedGroups++;
                        _quickUserGroupSelected[dbkey] = _quickUserGroupCache[dbkey];
                        span.html(_IND_SELECTED_ON).addClass(_CLASS_SELECTED_IND);
                    }
                    _quickUserFilter = undefined;

                    _onQuickSearchUser($("#sp-print-delegation-users-select-to-add-filter"), dbkey, $('#sp-print-delegation-users-select-to-add').val(), 0);

                }
                _onButtonAddVisibility();
            }
            //----------------------------------------------------------------
            ,
                _onUserSelect = function(selection, toggle, select) {
                var span,
                    dbkey;

                if (_isUiStateDisabled(selection)) {
                    return;
                }

                span = selection.find('.' + _CLASS_SELECTED_TXT);
                dbkey = selection.attr('data-savapage');

                if (_quickUserSelected[dbkey]) {
                    if (toggle || !select) {
                        _nSelectedUsers--;
                        delete _quickUserSelected[dbkey];
                        span.html(_IND_SELECTED_OFF);
                    }
                } else {
                    if ((toggle || select)) {
                        _nSelectedUsers++;
                        _quickUserSelected[dbkey] = _quickUserCache[dbkey];
                        span.html(_IND_SELECTED_ON);
                    }
                }
            },
                _onUserSelectIdempotent = function(selection, select) {
                _onUserSelect(selection, false, select);
            },
                _onUserSelected = function(selection) {
                _onUserSelect(selection, true);
                _onButtonAddVisibility();
            }
            //----------------------------------------------------------------
            ,
                _isAccountSelectScopePreferred = function() {
                return _ns.PreferredButtonSwitch.isOn($('#sp-print-delegation-select-shared-account-scope'));
            }
            //----------------------------------------------------------------
            ,
                _isAccountPreferredEnabled = function() {
                return $('#sp-print-delegation-select-shared-account-scope').length > 0;
            }
            //----------------------------------------------------------------
            ,
                _onAccountPreferred = function(selection) {
                _onObjectPreferred(selection, 'user-delegate-accounts-preferred');
            }
            //----------------------------------------------------------------
            ,
                _onAccountSelectScope = function(selection) {
                var selected = _ns.PreferredButtonSwitch.toggle(selection);
                _api.call({
                    request : 'user-set-delegate-accounts-preferred-select',
                    dto : JSON.stringify({
                        select : selected
                    })
                });

                _onQuickSearchAccount($("#sp-print-delegation-select-shared-account-filter"), $('#sp-print-delegation-select-shared-account').val(), 0, true, true);
            }
            //----------------------------------------------------------------
            ,
                _onAccountSelected = function(selection) {
                var span = selection.find('.' + _CLASS_SELECTED_TXT),
                    dbkey = selection.attr('data-savapage'),
                    dbKeySelected = _quickAccountSelected ? _quickAccountSelected.key : null;

                if (dbKeySelected) {
                    if (dbKeySelected === dbkey) {
                        return;
                    }
                    selection.closest('ul').find('.' + _CLASS_SELECTED_IND).html(_IND_SELECTED_OFF).removeClass(_CLASS_SELECTED_IND);
                }
                _quickAccountSelected = _quickAccountCache[dbkey];
                span.html(_IND_SELECTED_ON).addClass(_CLASS_SELECTED_IND);

                _onButtonAddVisibility();
            }
            //----------------------------------------------------------------
            ,
                _onDelegatorSelectRow = function(selection) {
                var span = selection.find('.' + _CLASS_SELECTED_TXT),
                    isGroup = selection.hasClass(_CLASS_GROUP),
                    isCopies = selection.hasClass(_CLASS_COPIES),
                    dbkey = selection.attr('data-savapage');

                if (isGroup) {
                    if (_delegatorGroupsSelected[dbkey]) {
                        _nSelectedDelegatorGroups--;
                        delete _delegatorGroupsSelected[dbkey];
                        span.html(_IND_SELECTED_OFF);
                    } else {
                        _nSelectedDelegatorGroups++;
                        _delegatorGroupsSelected[dbkey] = _delegatorGroups[dbkey];
                        span.html(_IND_SELECTED_ON);
                    }
                } else if (isCopies) {
                    if (_delegatorCopiesSelected[dbkey]) {
                        _nSelectedDelegatorCopies--;
                        delete _delegatorCopiesSelected[dbkey];
                        span.html(_IND_SELECTED_OFF);
                    } else {
                        _nSelectedDelegatorCopies++;
                        _delegatorCopiesSelected[dbkey] = _delegatorCopies[dbkey];
                        span.html(_IND_SELECTED_ON);
                    }
                } else {
                    if (_delegatorUsersSelected[dbkey]) {
                        _nSelectedDelegatorUsers--;
                        delete _delegatorUsersSelected[dbkey];
                        span.html(_IND_SELECTED_OFF);
                    } else {
                        _nSelectedDelegatorUsers++;
                        _delegatorUsersSelected[dbkey] = _delegatorUsers[dbkey];
                        span.html(_IND_SELECTED_ON);
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
                    $(this).html(_IND_SELECTED_OFF);
                });
            }
            //----------------------------------------------------------------
            ,
                _onDelegatorRemoveSelected = function() {
                var nUsersBefore = _nDelegatorUsers,
                    nGroupsBefore =
                    _nDelegatorGroups;

                $.each($('.' + _CLASS_SELECTED_TXT).closest('tr'), function() {
                    var dbkey = $(this).attr('data-savapage');

                    if (_delegatorGroupsSelected[dbkey]) {
                        _nDelegatorGroups--;
                        _nDelegatorGroupMembersCopies -= _delegatorGroups[dbkey].totalCopies;
                        delete _delegatorGroups[dbkey];
                        $(this).remove();
                    } else if (_delegatorCopiesSelected[dbkey]) {
                        _nDelegatorCopies -= _delegatorCopies[dbkey].totalCopies;
                        delete _delegatorCopies[dbkey];
                        $(this).remove();
                    } else if (_delegatorUsersSelected[dbkey]) {
                        _nDelegatorUsers--;
                        _nDelegatorUsersCopies -= _delegatorUsers[dbkey].totalCopies;
                        delete _delegatorUsers[dbkey];
                        $(this).remove();
                    }

                });

                _initDelegatorSelection();

                if (nGroupsBefore > _nDelegatorGroups) {
                    _showQuickSearchGroupsAdded();
                }

                if (nUsersBefore > _nDelegatorUsers) {
                    _showQuickSearchUsersAdded();
                    if (_isUserSelectAllSwitchOn()) {
                        _usersSelectAll(true);
                    }
                }

                _onButtonAddVisibility();
            }
            //----------------------------------------------------------------
            ,
                _visibleClickCheckboxRadio = function(name, id, visible) {
                _view.visible($('#' + id), visible);
                if (visible) {
                    _view.checkRadio(name, id);
                }
            }
            //
            ,
                _onChangeEditMode = function(id) {
                var dbkey,
                    filterWrk,
                    selWlk,
                    isCopies = id === _RADIO_EDIT_ID_COPIES;

                if (isCopies) {

                    _moveCopiesButtons2Accounts();

                } else if (id === 'sp-print-delegation-edit-radio-add-groups') {

                    _moveCopiesButtons2Groups();

                } else if (id === 'sp-print-delegation-edit-radio-add-users') {

                    _moveCopiesButtons2Users();

                    // Deselect all groups when multiple groups are selected
                    if (_nSelectedGroups > 1) {
                        dbkey = null;
                        _revertQuickUserGroupsSelected();
                    } else {
                        _switchQuickUserGroupToSingleSelect();
                        dbkey = _util.getFirstProp(_quickUserGroupSelected);
                    }

                    // Clear users
                    _quickUserFilter = undefined;
                    _quickUserCache = {};
                    _quickUserSelected = {};
                    _nSelectedUsers = 0;

                    _onQuickSearchUser($("#sp-print-delegation-users-select-to-add-filter"), dbkey, $('#sp-print-delegation-users-select-to-add').val(), 0);

                    _view.enable($('#sp-print-delegation-button-add'), false);

                }

                if (_isAccountSharedSelected()) {
                    selWlk = $('#sp-print-delegation-select-shared-account-filter');
                    filterWrk = $('#sp-print-delegation-select-shared-account').val();
                    _onQuickSearchAccount(selWlk, filterWrk, 0, false);
                    _onQuickSearchAccountVisibility('sp-print-delegation-quicksearch-accounts', filterWrk, _quickAccountTotalResults, _isAccountSelectScopePreferred());
                }

                selWlk = $('#sp-print-delegation-member-copies');
                _view.visible(selWlk.closest('.sp-print-delegation-copies-div'), !isCopies);

                selWlk = $('#sp-print-delegation-copies-to-add');
                _view.visible(selWlk.closest('.sp-print-delegation-copies-div'), isCopies);

                _onButtonAddVisibility();
            }
            //----------------------------------------------------------------
            ,
                _onChangeAccountType = function() {
                var enableGroups,
                    enableUsers,
                    enableCopies,
                    editMode;

                // Note: AccountGroup and/or AccountShared might not be present,
                // due to customization (config items).
                if (_isAccountGroupSelected()) {
                    enableGroups = true;
                    editMode = _RADIO_EDIT_ID_GROUPS;
                } else if (_isAccountUserSelected()) {
                    enableGroups = true;
                    enableUsers = true;
                    editMode = _RADIO_EDIT_ID_GROUPS;
                } else if (_isAccountSharedSelected()) {
                    enableGroups = _model.DELEGATE_ACCOUNT_SHARED_GROUP;
                    enableCopies = true;
                    editMode = enableGroups ? _RADIO_EDIT_ID_GROUPS : _RADIO_EDIT_ID_COPIES;
                }

                _view.visibleCheckboxRadio($('#' + _RADIO_EDIT_ID_GROUPS), enableGroups);
                _view.visibleCheckboxRadio($('#' + _RADIO_EDIT_ID_COPIES), enableCopies);

                if (enableGroups) {
                    _view.visibleCheckboxRadio($('#' + _RADIO_EDIT_ID_USERS), enableUsers);
                    _visibleClickCheckboxRadio(_RADIO_EDIT_NAME, _RADIO_EDIT_ID_GROUPS, enableGroups);
                } else {
                    if (enableUsers) {
                        _visibleClickCheckboxRadio(_RADIO_EDIT_NAME, _RADIO_EDIT_ID_USERS, true);
                    } else if (enableCopies) {
                        _view.visibleCheckboxRadio($('#' + _RADIO_EDIT_ID_USERS), false);
                        _visibleClickCheckboxRadio(_RADIO_EDIT_NAME, _RADIO_EDIT_ID_COPIES, true);
                    }
                }

                _onChangeEditMode(editMode);
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

                var filterableGroups = $("#sp-print-delegation-groups-select-to-add-filter"),
                    filterableUsers = $("#sp-print-delegation-users-select-to-add-filter"),
                    filterableAccounts = $("#sp-print-delegation-select-shared-account-filter"),
                    selBtnGroupsSelectScope = $('#sp-print-delegation-groups-select-to-add-scope'),
                    selBtnAccountSelectScope = $('#sp-print-delegation-select-shared-account-scope'),
                    selBtnUsersSelectAll = $('#sp-print-delegation-users-select-to-add-all'),
                    res;

                _initDelegatorSelection();

                $('#sp-print-delegation-button-back').click(function() {
                    _this.onButtonBack();
                });

                $('#sp-print-delegation-button-next').click(function() {
                    _this.onButtonNext(_previousPageId);
                });

                $('#sp-print-delegation-button-select-all').click(function() {
                    _onDelegatorSelectAll();
                });

                $('#sp-print-delegation-button-deselect-all').click(function() {
                    _onDelegatorDeselectAll();
                });

                $('#sp-print-delegation-button-remove-selected').click(function() {
                    _onDelegatorRemoveSelected();
                });

                $('input[name="' + _RADIO_EDIT_NAME + '"]').click(function() {
                    _onChangeEditMode($(this).attr('id'));
                    _setVisibility();
                });

                $('input[name="' + _RADIO_ACCOUNT_NAME + '"]').click(function() {
                    _onChangeAccountType();
                    _setVisibility();
                });

                //
                $(this).on('click', '#sp-selected-print-delegation-rows tr', null, function() {
                    _onDelegatorSelectRow($(this));
                });

                // Start with first account option (and trigger visibility
                // actions).
                _view.checkRadioFirst(_RADIO_ACCOUNT_NAME);
                _onChangeAccountType();

                //---------------------
                // Group selection
                //---------------------
                filterableGroups.on("filterablebeforefilter", function(e, data) {
                    _onQuickSearchGroup($(this), data.input.get(0).value, 0);
                });

                $(this).on('click', '#sp-print-delegation-groups-select-to-add-filter li', null, function() {
                    _onGroupSelected($(this));
                });

                if (_isGroupPreferredEnabled()) {
                    $(this).on('click', '#sp-print-delegation-groups-select-to-add-filter li .ui-li-count', null, function() {
                        _onGroupPreferred($(this));
                        return false;
                    });
                }

                //
                selBtnGroupsSelectScope.click(function() {
                    _onGroupSelectScope($(this));
                });

                res = _api.call({
                    request : 'user-get-delegate-groups-preferred-select'
                });

                _ns.PreferredButtonSwitch.setState(selBtnGroupsSelectScope, res.dto.select);
                selBtnGroupsSelectScope.addClass('ui-btn-icon-notext');

                // Show available groups on first open
                _onQuickSearchGroup(filterableGroups, "", 0);

                //---------------------
                // User selection
                //---------------------
                filterableUsers.on("filterablebeforefilter", function(e, data) {
                    var dbkey = _util.getFirstProp(_quickUserGroupSelected);
                    _onQuickSearchUser($(this), dbkey, data.input.get(0).value, 0);
                });

                $(this).on('click', '#sp-print-delegation-users-select-to-add-filter li', null, function() {
                    _onUserSelected($(this));
                });

                selBtnUsersSelectAll.click(function() {
                    _onUsersSelectAllToggle($(this));
                });

                _ns.SelectAllButtonSwitch.setState(selBtnUsersSelectAll, false);
                selBtnUsersSelectAll.addClass('ui-btn-icon-notext');

                //---------------------
                // Account selection
                //---------------------
                filterableAccounts.on("filterablebeforefilter", function(e, data) {
                    _onQuickSearchAccount($(this), data.input.get(0).value, 0, false);
                });

                $(this).on('click', '#sp-print-delegation-select-shared-account-filter li', null, function() {
                    _onAccountSelected($(this));
                });

                if (_isAccountPreferredEnabled()) {
                    $(this).on('click', '#sp-print-delegation-select-shared-account-filter li .ui-li-count', null, function() {
                        _onAccountPreferred($(this));
                        return false;
                    });
                }

                //
                selBtnAccountSelectScope.click(function() {
                    _onAccountSelectScope($(this));
                });

                res = _api.call({
                    request : 'user-get-delegate-accounts-preferred-select'
                });

                _ns.PreferredButtonSwitch.setState(selBtnAccountSelectScope, res.dto.select);
                selBtnAccountSelectScope.addClass('ui-btn-icon-notext');

                //-----------------------------------
                // User | Group | Account selection
                //-----------------------------------
                $(this).on('click', '#sp-print-delegation-mode-add-table img', null, function(event) {
                    event.preventDefault();
                });

                //-----------------------
                // Group list navigation
                //-----------------------
                $("#sp-print-delegation-quicksearch-groups .sp-quicksearch-buttons .ui-btn").click(function() {
                    _onQuickSearchGroup($("#sp-print-delegation-groups-select-to-add-filter"), $('#sp-print-delegation-groups-select-to-add').val(), $(this).attr('data-savapage'), true);
                });

                //-----------------------
                // User list navigation
                //-----------------------
                $("#sp-print-delegation-quicksearch-users .sp-quicksearch-buttons .ui-btn").click(function() {
                    var dbkey = _util.getFirstProp(_quickUserGroupSelected),
                        nextPosition = $(this).attr('data-savapage');
                    _onQuickSearchUser($("#sp-print-delegation-users-select-to-add-filter"), dbkey, $('#sp-print-delegation-users-select-to-add').val(), nextPosition, true);
                });

                //-----------------------
                // Account list navigation
                //-----------------------
                $("#sp-print-delegation-quicksearch-accounts .sp-quicksearch-buttons .ui-btn").click(function() {
                    _onQuickSearchAccount($("#sp-print-delegation-select-shared-account-filter"), $('#sp-print-delegation-select-shared-account').val(), $(this).attr('data-savapage'), true);
                });

                //-----------------------
                // Group copies popup
                //-----------------------
                $('#sp-print-delegation-btn-add-group-copies').on('click', function() {
                    _onAddGroups(_getPopupGroupCopies());
                    $('#sp-print-delegation-popup-add-group-copies').popup('close');
                    return false;
                });

                // Show available accounts on first open
                _onQuickSearchAccount(filterableAccounts, "", 0, false);

            }).on("pagebeforeshow", function(event, ui) {
                _previousPageId = ui.prevPage.attr('id');
                _setVisibility();
            }).on("pagebeforehide", function(event, ui) {
                _updateModel();
                _this.onBeforeHide();
            });
        };

    }(jQuery, this, this.document, JSON, this.org.savapage));
