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
package org.savapage.server.pages.admin;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.dao.helpers.UserGroupPagerReq;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserGroupService;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.ACLOidAdminSummaryPanel;
import org.savapage.server.pages.ACLOidSummaryPanel;
import org.savapage.server.pages.ACLOidUserSummaryPanel;
import org.savapage.server.pages.ACLRoleSummaryPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserGroupsPage extends AbstractAdminListPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Note: must be odd number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /** */
    private static final UserGroupService USERGROUP_SERVICE =
            ServiceContext.getServiceFactory().getUserGroupService();

    /**
     * @return {@code false} to give Admin a chance to inspect the User Groups.
     */
    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private final class UserGroupListView extends PropertyListView<UserGroup> {

        private static final long serialVersionUID = 1L;

        /** */
        private static final String WID_BUTTON_EDIT = "button-edit";
        /** */
        private static final String WID_BUTTON_USERS = "button-users";
        /** */
        private static final String WID_BUTTON_ACCOUNT = "button-account";

        /** */
        private final UserGroupMemberDao groupMemberDao =
                ServiceContext.getDaoContext().getUserGroupMemberDao();

        /** */
        private final UserDao userDao =
                ServiceContext.getDaoContext().getUserDao();

        /** */
        private final UserGroupMemberDao.GroupFilter groupFilter =
                new UserGroupMemberDao.GroupFilter();

        /***/
        private final boolean isEditor;
        /***/
        private final boolean hasAccessAcc;
        /***/
        private final boolean hasAccessUsers;

        /**
         *
         * @param id
         * @param list
         * @param isEditor
         */
        public UserGroupListView(String id, List<UserGroup> list,
                final boolean isEditor) {

            super(id, list);

            this.isEditor = isEditor;

            final UserIdDto reqUserDto = SpSession.get().getUserIdDto();

            this.hasAccessAcc = ACCESS_CONTROL_SERVICE.hasAccess(reqUserDto,
                    ACLOidEnum.A_ACCOUNTS);

            this.hasAccessUsers = ACCESS_CONTROL_SERVICE.hasAccess(reqUserDto,
                    ACLOidEnum.A_USERS);
        }

        @Override
        protected void populateItem(final ListItem<UserGroup> item) {

            final MarkupHelper helper = new MarkupHelper(item);

            final UserGroup userGroup = item.getModelObject();

            Label labelWrk = null;

            helper.encloseLabel("fullName", userGroup.getFullName(),
                    !StringUtils.defaultString(userGroup.getFullName())
                            .equals(userGroup.getGroupName()));
            //
            final ReservedUserGroupEnum reservedGroup =
                    ReservedUserGroupEnum.fromDbName(userGroup.getGroupName());

            final String groupName;
            final long userCount;

            if (reservedGroup == null) {
                groupName = userGroup.getGroupName();
                groupFilter.setGroupId(userGroup.getId());
                userCount = groupMemberDao.getUserCount(groupFilter);
            } else {
                groupName = reservedGroup.getUiName();
                userCount = userDao.countActiveUsers(reservedGroup);
            }

            labelWrk = helper.addLabel("groupName", groupName);

            if (reservedGroup != null) {
                MarkupHelper.appendLabelAttr(labelWrk, "class",
                        MarkupHelper.CSS_TXT_WARN);
            }

            helper.addLabel("signal", String.valueOf(userCount));

            /*
             * Shared accounts users of this group are allowed to use.
             */
            final SharedAccountSummaryPanel sharedAccPanel =
                    new SharedAccountSummaryPanel("auth-shared-accounts");
            sharedAccPanel.populate(userGroup);
            item.add(sharedAccPanel);

            /*
             * Roles
             */
            final ACLRoleSummaryPanel rolesPanel =
                    new ACLRoleSummaryPanel("user-roles");
            rolesPanel.populate(USERGROUP_SERVICE.getUserGroupRoles(userGroup),
                    getLocale());
            item.add(rolesPanel);

            /*
             * ACL
             */
            final ACLOidSummaryPanel userAclPanel =
                    new ACLOidUserSummaryPanel("user-acl");
            userAclPanel.populate(
                    USERGROUP_SERVICE.getUserGroupACLUser(userGroup),
                    getLocale());
            item.add(userAclPanel);

            final ACLOidSummaryPanel adminAclPanel =
                    new ACLOidAdminSummaryPanel("admin-acl");
            adminAclPanel.populate(
                    USERGROUP_SERVICE.getUserGroupACLAdmin(userGroup),
                    getLocale());
            item.add(adminAclPanel);

            /*
             * Set the uid in 'data-savapage' attribute, so it can be picked up
             * in JavaScript for editing.
             */
            if (this.isEditor) {
                MarkupHelper.modifyLabelAttr(
                        MarkupHelper.modifyLabelAttr(
                                helper.encloseLabel(WID_BUTTON_EDIT,
                                        getLocalizer().getString("button-edit",
                                                this),
                                        true),
                                MarkupHelper.ATTR_DATA_SAVAPAGE,
                                userGroup.getId().toString()),
                        MarkupHelper.ATTR_TITLE,
                        HtmlButtonEnum.EDIT.uiText(getLocale()));
            } else {
                helper.discloseLabel(WID_BUTTON_EDIT);
            }

            if (this.hasAccessUsers && userCount > 0) {
                MarkupHelper.modifyLabelAttr(
                        MarkupHelper.modifyLabelAttr(
                                helper.encloseLabel(WID_BUTTON_USERS,
                                        getLocalizer().getString("button-users",
                                                this),
                                        true),
                                MarkupHelper.ATTR_DATA_SAVAPAGE,
                                userGroup.getId().toString()),
                        MarkupHelper.ATTR_TITLE,
                        NounEnum.USER.uiText(getLocale(), userCount > 1));
            } else {
                helper.discloseLabel(WID_BUTTON_USERS);
            }

            if (this.hasAccessAcc
                    && ACCOUNTING_SERVICE.getActiveUserGroupAccount(
                            userGroup.getGroupName()) != null) {

                labelWrk = helper.encloseLabel(WID_BUTTON_ACCOUNT,
                        getLocalizer().getString("button-account", this), true);

                MarkupHelper.modifyLabelAttr(labelWrk,
                        MarkupHelper.ATTR_DATA_SAVAPAGE,
                        userGroup.getGroupName());

                MarkupHelper.modifyLabelAttr(labelWrk,
                        MarkupHelper.ATTR_DATA_SAVAPAGE_TYPE,
                        AccountTypeEnum.GROUP.toString());

                MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_TITLE,
                        PrintOutNounEnum.ACCOUNT.uiText(getLocale()));

            } else {
                helper.discloseLabel(WID_BUTTON_ACCOUNT);
            }

            helper.addTransparantInvisible("sect-buttons", !(this.isEditor
                    || this.hasAccessAcc || this.hasAccessUsers));
        }
    }

    /**
     * @param parameters
     *            The page parameters.
     */
    public UserGroupsPage(final PageParameters parameters) {

        super(parameters);

        final String data = getParmValue(POST_PARM_DATA);
        final UserGroupPagerReq req = UserGroupPagerReq.read(data);

        final UserGroupDao.Field sortField = req.getSort().getSortField();
        final boolean sortAscending = req.getSort().getAscending();

        final UserGroupDao.ListFilter filter = new UserGroupDao.ListFilter();
        filter.setContainingNameOrIdText(
                req.getSelect().getNameContainingText());

        final UserGroupDao userGroupDao =
                ServiceContext.getDaoContext().getUserGroupDao();

        final long groupCount = userGroupDao.getListCount(filter);

        /*
         * Display the requested page.
         */
        final List<UserGroup> entryList =
                userGroupDao.getListChunk(filter, req.calcStartPosition(),
                        req.getMaxResults(), sortField, sortAscending);

        add(new UserGroupListView("user-groups-view", entryList,
                this.probePermissionToEdit(ACLOidEnum.A_USER_GROUPS)));

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, groupCount, MAX_PAGES_IN_NAVBAR,
                "sp-user-groups-page",
                new String[] { "nav-bar-1", "nav-bar-2" });
    }

}
