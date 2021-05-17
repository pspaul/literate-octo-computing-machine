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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.SystemStatusEnum;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.dao.helpers.UserPagerReq;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupMember;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserGroupService;
import org.savapage.core.services.UserService;
import org.savapage.core.services.helpers.account.UserAccountContext;
import org.savapage.core.services.helpers.account.UserAccountContextEnum;
import org.savapage.core.services.helpers.account.UserAccountContextFactory;
import org.savapage.core.users.conf.UserAliasList;
import org.savapage.core.util.NumberUtil;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.SparklineHtml;
import org.savapage.server.helpers.account.UserAccountContextHtmlFactory;
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
public final class UsersPage extends AbstractAdminListPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final UserGroupService USER_GROUP_SERVICE =
            ServiceContext.getServiceFactory().getUserGroupService();

    /**
     * Important: must be odd number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    /**
     * @return {@code false} to give Admin a chance to inspect the users.
     */
    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     *
     */
    private final class UserGroupInfo {

        private String groupName;
        private ReservedUserGroupEnum reservedGroup;
        private Map<ACLRoleEnum, Boolean> roles;
        private Map<ACLOidEnum, Integer> aclUser;
        private Map<ACLOidEnum, Integer> aclAdmin;

        private boolean hasRolesOrACL() {
            return !this.roles.isEmpty() || !this.aclUser.isEmpty()
                    || !this.aclAdmin.isEmpty();
        }
    }

    /**
     *
     */
    private final class UserGroupInfoListView
            extends PropertyListView<UserGroupInfo> {

        /** */
        private static final long serialVersionUID = 1L;

        /** */
        private int iItemWlk;

        /**
         * @param id
         *            Wicket ID.
         * @param list
         *            Groups.
         */
        UserGroupInfoListView(final String id, final List<UserGroupInfo> list) {
            super(id, list);
            this.iItemWlk = 0;
        }

        @Override
        protected void populateItem(final ListItem<UserGroupInfo> item) {

            final MarkupHelper helper = new MarkupHelper(item);

            final UserGroupInfo info = item.getModelObject();

            final String pfx;
            if (this.iItemWlk == 0) {
                pfx = "";
            } else {
                pfx = ", ";
            }

            if (info.reservedGroup == null) {
                helper.addLabel("group-name", pfx.concat(info.groupName));
                this.iItemWlk++;
            } else {
                if (info.roles.isEmpty() && info.aclUser.isEmpty()
                        && info.aclAdmin.isEmpty()) {
                    helper.discloseLabel("group-name");
                } else {
                    helper.addLabel("group-name",
                            String.format("%s<i>%s</i>", pfx,
                                    info.reservedGroup.getUiName()))
                            .setEscapeModelStrings(false);
                    this.iItemWlk++;
                }
            }

            final ACLRoleSummaryPanel rolesPanel =
                    new ACLRoleSummaryPanel("group-roles");
            rolesPanel.populate(info.roles, getLocale());
            item.add(rolesPanel);

            final ACLOidSummaryPanel aclUserPanel =
                    new ACLOidUserSummaryPanel("group-acl-user");
            aclUserPanel.populate(info.aclUser, getLocale());
            item.add(aclUserPanel);

            final ACLOidSummaryPanel aclAdminPanel =
                    new ACLOidAdminSummaryPanel("group-acl-admin");
            aclAdminPanel.populate(info.aclAdmin, getLocale());
            item.add(aclAdminPanel);
        }
    }

    /**
     *
     */
    private final class UserListView extends PropertyListView<User> {

        private static final long serialVersionUID = 1L;

        /***/
        private final boolean isEditorPriv;
        /***/
        private final boolean hasAccessDoc;
        /***/
        private final boolean hasAccessTrx;
        /***/
        private final boolean isAppReady;

        /**
         * The currency symbol of the current Locale.
         */
        private final String currencySymbol;

        /** */
        private final UserAccountContext accountCtxSavaPage;

        /** */
        private final UserAccountContext accountCtxPaperCut;

        /** */
        private static final String WID_BUTTON_LOG = "button-log";
        /** */
        private static final String WID_BUTTON_TRX = "button-transaction";
        /** */
        private static final String WID_BUTTON_GDPR = "button-gdpr";

        /**
         *
         * @param id
         *            Wicket ID.
         * @param list
         *            User list.
         * @param isEditor
         *            {@code true} if requesting user has editing privilege.
         */
        UserListView(final String id, final List<User> list,
                final boolean isEditor) {

            super(id, list);

            this.currencySymbol = SpSession.getAppCurrencySymbol();
            this.isAppReady = ConfigManager.instance()
                    .getSystemStatus() != SystemStatusEnum.SETUP;

            final UserIdDto reqUserDto = SpSession.get().getUserIdDto();

            this.isEditorPriv = isEditor;

            this.hasAccessDoc = ACCESS_CONTROL_SERVICE.hasAccess(reqUserDto,
                    ACLOidEnum.A_DOCUMENTS);

            this.hasAccessTrx = ACCESS_CONTROL_SERVICE.hasAccess(reqUserDto,
                    ACLOidEnum.A_TRANSACTIONS);

            this.accountCtxSavaPage =
                    UserAccountContextFactory.getContextSavaPage();

            this.accountCtxPaperCut =
                    UserAccountContextFactory.getContextPaperCut();
        }

        /**
         *
         * @param item
         *            User item.
         * @param mapVisible
         *            Map of visible Wicket IDs (key) with value.
         */
        private void evaluateVisible(final ListItem<User> item,
                final Map<String, String> mapVisible) {

            for (Map.Entry<String, String> entry : mapVisible.entrySet()) {

                if (entry.getValue() == null) {
                    entry.setValue("");
                }

                final String cssClassWlk = null;

                item.add(createVisibleLabel(
                        StringUtils.isNotBlank(entry.getValue()),
                        entry.getKey(), entry.getValue(), cssClassWlk));
            }
        }

        /**
         * Creates user group info.
         *
         * @param user
         *            User.
         * @param rolesUser
         *            User roles.
         * @return UserGroupInfo list.
         */
        private List<UserGroupInfo> createGroupInfo(final User user,
                final Map<ACLRoleEnum, Boolean> rolesUser) {

            final List<UserGroupInfo> groupInfoList = new ArrayList<>();

            if (!user.getDeleted()) {

                final boolean isAdminUser =
                        BooleanUtils.isTrue(user.getAdmin());

                final Map<ACLRoleEnum, Boolean> rolesCumulated =
                        new HashMap<>();
                final Map<ACLOidEnum, Integer> aclUserCumulated =
                        new HashMap<>();
                final Map<ACLOidEnum, Integer> aclAdminCumulated =
                        new HashMap<>();

                /*
                 * Add user group is specific order.
                 */
                final List<UserGroup> memberGroups = new ArrayList<>();

                /*
                 * 1: Add explicit group membership.
                 */
                final List<UserGroupMember> memberships =
                        user.getGroupMembership();

                if (memberships != null) {
                    for (final UserGroupMember member : memberships) {
                        memberGroups.add(member.getGroup());
                    }
                }

                /*
                 * 2: Add implicit user source group membership.
                 */
                if (BooleanUtils.isTrue(user.getInternal())) {
                    memberGroups.add(USER_GROUP_SERVICE.getInternalUserGroup());
                } else {
                    memberGroups.add(USER_GROUP_SERVICE.getExternalUserGroup());
                }

                /*
                 * 3: Add implicit all user membership.
                 */
                memberGroups.add(USER_GROUP_SERVICE.getAllUserGroup());

                /*
                 * Process.
                 */
                for (final UserGroup group : memberGroups) {

                    final UserGroupInfo info = new UserGroupInfo();

                    info.groupName = group.getGroupName();

                    info.reservedGroup = ReservedUserGroupEnum
                            .fromDbName(group.getGroupName());

                    info.roles = this.mergeRoles(rolesCumulated, rolesUser,
                            USER_GROUP_SERVICE.getUserGroupRoles(group));

                    info.aclUser = this.mergeACL(aclUserCumulated,
                            USER_GROUP_SERVICE.getUserGroupACLUser(group));

                    if (isAdminUser) {
                        info.aclAdmin = this.mergeACL(aclAdminCumulated,
                                USER_GROUP_SERVICE.getUserGroupACLAdmin(group));
                    } else {
                        info.aclAdmin = new HashMap<>();
                    }

                    if (info.reservedGroup == null || info.hasRolesOrACL()) {
                        groupInfoList.add(info);
                    }
                }
            }

            return groupInfoList;
        }

        /**
         * Merges group roles.
         *
         * @param rolesCumulated
         *            Accumulated merges (input and output).
         * @param rolesUser
         *            User roles.
         * @param rolesGroup
         *            Group roles.
         * @return Merged roles.
         */
        private Map<ACLRoleEnum, Boolean> mergeRoles(
                final Map<ACLRoleEnum, Boolean> rolesCumulated,
                final Map<ACLRoleEnum, Boolean> rolesUser,
                final Map<ACLRoleEnum, Boolean> rolesGroup) {

            final Map<ACLRoleEnum, Boolean> rolesMerged = new HashMap<>();

            for (final Map.Entry<ACLRoleEnum, Boolean> entry : rolesGroup
                    .entrySet()) {
                final ACLRoleEnum key = entry.getKey();
                if (!rolesUser.containsKey(key)
                        && !rolesCumulated.containsKey(key)) {
                    rolesCumulated.put(key, entry.getValue());
                    rolesMerged.put(key, entry.getValue());
                }
            }
            return rolesMerged;
        }

        /**
         * Merge ACL.
         *
         * @param aclCumulated
         *            Accumulated merges (input and output).
         * @param aclSource
         *            Source ACL.
         * @return Merged ACL.
         */
        private Map<ACLOidEnum, Integer> mergeACL(
                final Map<ACLOidEnum, Integer> aclCumulated,
                final Map<ACLOidEnum, Integer> aclSource) {

            final Map<ACLOidEnum, Integer> aclMerged = new HashMap<>();

            for (final Map.Entry<ACLOidEnum, Integer> entry : aclSource
                    .entrySet()) {
                final ACLOidEnum key = entry.getKey();
                if (!aclCumulated.containsKey(key)) {
                    aclCumulated.put(key, entry.getValue());
                    aclMerged.put(key, entry.getValue());
                }
            }
            return aclMerged;
        }

        @Override
        protected void populateItem(final ListItem<User> item) {

            final MarkupHelper helper = new MarkupHelper(item);

            final Map<String, String> mapVisible = new HashMap<>();

            final User user = item.getModelObject();

            final boolean isErased = USER_SERVICE.isErased(user);

            Label labelWrk = null;

            MarkupHelper.modifyLabelAttr(
                    helper.addModifyLabelAttr("user-pie",
                            SparklineHtml.valueString(
                                    user.getNumberOfPrintOutPages().toString(),
                                    user.getNumberOfPrintInPages().toString(),
                                    user.getNumberOfPdfOutPages().toString()),
                            SparklineHtml.ATTR_SLICE_COLORS,
                            SparklineHtml.arrayAttr(SparklineHtml.COLOR_PRINTER,
                                    SparklineHtml.COLOR_QUEUE,
                                    SparklineHtml.COLOR_PDF)),
                    MarkupHelper.ATTR_CLASS, SparklineHtml.CSS_CLASS_USER);

            //
            if (isErased) {
                helper.discloseLabel("fullName");

                labelWrk = new Label("userId",
                        USER_SERVICE.getUserIdUi(user, getLocale()));
                labelWrk.add(new AttributeModifier("class",
                        MarkupHelper.CSS_TXT_ERROR));
            } else {
                item.add(new Label("fullName"));
                item.add(new Label("email",
                        USER_SERVICE.getPrimaryEmailAddress(user)));
                //
                labelWrk = new Label("userId");
                if (user.getInternal()) {
                    labelWrk.add(new AttributeModifier("class",
                            MarkupHelper.CSS_TXT_INTERNAL_USER));
                }
            }
            item.add(labelWrk);

            /*
             * Signal
             */
            String signal = "";
            String signalKey = null;
            String color = null;

            final Date onDate = new Date();

            if (isErased) {
                color = MarkupHelper.CSS_TXT_ERROR;
                signal = AdjectiveEnum.ERASED.uiText(getLocale()).toLowerCase();
            } else if (user.getDeleted()) {
                color = MarkupHelper.CSS_TXT_ERROR;
                signalKey = "signal-user-deleted";
            } else if (USER_SERVICE.isUserFullyDisabled(user, onDate)) {
                color = MarkupHelper.CSS_TXT_ERROR;
                signalKey = "signal-user-disabled";
            } else if (user.getAdmin()) {
                color = MarkupHelper.CSS_TXT_VALID;
                signalKey = "signal-user-admin";
            } else if (!user.getPerson()) {
                color = MarkupHelper.CSS_TXT_WARN;
                signalKey = "signal-user-abstract";
            }

            if (signalKey != null) {
                signal = localized(signalKey);
            }
            labelWrk = new Label("signal", signal);
            labelWrk.add(new AttributeModifier("class", color));
            item.add(labelWrk);

            /*
             * Roles
             */
            final Map<ACLRoleEnum, Boolean> userRoles =
                    USER_SERVICE.getUserRoles(user.getId());

            final ACLRoleSummaryPanel rolesPanel =
                    new ACLRoleSummaryPanel("user-roles");
            rolesPanel.populate(userRoles, getLocale());
            item.add(rolesPanel);

            /*
             * User aliases
             */
            final Set<String> aliases =
                    UserAliasList.instance().getUserAliases(user.getUserId());

            if (aliases.isEmpty()) {
                helper.discloseLabel("userAliasesPrompt");
            } else {
                final StringBuilder builder = new StringBuilder();
                for (final String alias : aliases) {
                    if (builder.length() > 0) {
                        builder.append(", ");
                    }
                    builder.append("\"").append(alias).append("\"");
                }
                helper.addLabel("userAliasesPrompt", NounEnum.ALIAS);
                helper.addLabel("userAliases", builder.toString());
            }

            /*
             * Groups
             */
            final List<UserGroupInfo> groupInfoList =
                    this.createGroupInfo(user, userRoles);

            if (groupInfoList.isEmpty()) {
                helper.discloseLabel("userGroupsPrompt");
            } else {
                helper.addLabel("userGroupsPrompt",
                        NounEnum.GROUP.uiText(getLocale()));
                item.add(new UserGroupInfoListView("user-groups-view",
                        groupInfoList));
            }

            /*
             * Balance
             */
            item.add(new Label("balance-amount",
                    this.accountCtxSavaPage.getFormattedUserBalance(user,
                            getLocale(), this.currencySymbol)));

            if (this.accountCtxPaperCut == null || isErased) {
                mapVisible.put("balance-amount-papercut", null);
            } else {
                helper.addTransparant("balance-papercut-icon")
                        .add(new AttributeModifier(MarkupHelper.ATTR_SRC,
                                UserAccountContextHtmlFactory
                                        .getContext(
                                                UserAccountContextEnum.PAPERCUT)
                                        .getImgUrl()));
                mapVisible.put("balance-amount-papercut",
                        this.accountCtxPaperCut.getFormattedUserBalance(user,
                                getLocale(), this.currencySymbol));

                helper.addTransparant("balance-savapage-icon")
                        .add(new AttributeModifier(MarkupHelper.ATTR_SRC,
                                UserAccountContextHtmlFactory
                                        .getContext(
                                                UserAccountContextEnum.SAVAPAGE)
                                        .getImgUrl()));
            }

            /*
             * Period + Totals
             */
            final StringBuilder period = new StringBuilder();
            final StringBuilder totals = new StringBuilder();

            if (user.getResetDate() == null) {
                period.append(localizedMediumDate(user.getCreatedDate()));
            } else {
                period.append(localizedMediumDate(user.getResetDate()));
            }

            period.append(" ~ ");

            if (user.getLastUserActivity() != null) {
                period.append(localizedMediumDate(user.getLastUserActivity()));

                Integer total = null;

                //
                total = user.getNumberOfPrintInJobs();
                totals.append(helper.localizedNumber(total));
                totals.append(" ").append(
                        PrintOutNounEnum.JOB.uiText(getLocale(), total > 1));

                //
                total = user.getNumberOfPrintInPages();
                totals.append(" &bull; ").append(helper.localizedNumber(total));
                totals.append(" ").append(
                        PrintOutNounEnum.PAGE.uiText(getLocale(), total > 1));

                //
                totals.append(" &bull; ")
                        .append(NumberUtil.humanReadableByteCountSI(getLocale(),
                                user.getNumberOfPrintInBytes()));
            }

            item.add(new Label("period", period.toString()));

            labelWrk = new Label("totals", totals.toString());
            labelWrk.setEscapeModelStrings(false);
            item.add(labelWrk);

            /*
             *
             */
            String homeSize = "-";
            if (user.getPerson()) {
                try {
                    final long size =
                            ConfigManager.getUserHomeDirSize(user.getUserId());
                    if (size > 0) {
                        homeSize = NumberUtil
                                .humanReadableByteCountSI(getLocale(), size);
                    }
                } catch (IOException e) {
                    homeSize = "-";
                }
            }

            item.add(new Label("home-size", homeSize));

            /*
             * Set the uid in 'data-savapage' attribute, so it can be picked up
             * in JavaScript for editing.
             */
            final boolean visible =
                    !user.getDeleted() && this.isAppReady && this.isEditorPriv;

            labelWrk = new Label("button-edit",
                    HtmlButtonEnum.EDIT.uiText(getLocale())) {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return visible;
                }

            };

            MarkupHelper.modifyLabelAttr(labelWrk, MarkupHelper.ATTR_TITLE,
                    HtmlButtonEnum.EDIT.uiText(getLocale()));

            MarkupHelper.modifyLabelAttr(labelWrk,
                    MarkupHelper.ATTR_DATA_SAVAPAGE, user.getUserId());

            item.add(labelWrk);

            if (this.isAppReady) {

                if (this.hasAccessDoc) {
                    labelWrk = new Label(WID_BUTTON_LOG,
                            NounEnum.DOCUMENT.uiText(getLocale(), true));

                    MarkupHelper.modifyLabelAttr(labelWrk,
                            MarkupHelper.ATTR_TITLE,
                            NounEnum.DOCUMENT.uiText(getLocale(), true));

                    MarkupHelper.modifyLabelAttr(labelWrk,
                            MarkupHelper.ATTR_DATA_SAVAPAGE,
                            user.getId().toString());

                    item.add(labelWrk);
                } else {
                    helper.discloseLabel(WID_BUTTON_LOG);
                }

                if (this.hasAccessTrx) {
                    labelWrk = new Label(WID_BUTTON_TRX,
                            NounEnum.TRANSACTION.uiText(getLocale(), true));

                    MarkupHelper.modifyLabelAttr(labelWrk,
                            MarkupHelper.ATTR_TITLE,
                            NounEnum.TRANSACTION.uiText(getLocale(), true));

                    MarkupHelper.modifyLabelAttr(labelWrk,
                            MarkupHelper.ATTR_DATA_SAVAPAGE,
                            user.getId().toString());

                    item.add(labelWrk);
                } else {
                    helper.discloseLabel(WID_BUTTON_TRX);
                }

                //
                if (isErased) {
                    helper.discloseLabel(WID_BUTTON_GDPR);
                } else {
                    labelWrk = new Label(WID_BUTTON_GDPR, "GDPR");

                    MarkupHelper.modifyLabelAttr(labelWrk,
                            MarkupHelper.ATTR_TITLE, "GDPR");

                    MarkupHelper.modifyLabelAttr(labelWrk,
                            MarkupHelper.ATTR_DATA_SAVAPAGE,
                            user.getId().toString());

                    item.add(labelWrk);
                }

            } else {
                helper.discloseLabel(WID_BUTTON_LOG);
                helper.discloseLabel(WID_BUTTON_TRX);
                helper.discloseLabel(WID_BUTTON_GDPR);
            }
            //
            evaluateVisible(item, mapVisible);
        }

    }

    /**
     *
     */
    public UsersPage(final PageParameters parameters) {

        super(parameters);

        /*
         * We need a transaction because of the lazy creation of UserAccount
         * instances.
         */
        ServiceContext.getDaoContext().beginTransaction();
        handlePage();
    }

    /**
     *
     */
    private void handlePage() {

        final String data = getParmValue(POST_PARM_DATA);
        final UserPagerReq req = UserPagerReq.read(data);

        final UserDao.Field sortField = req.getSort().getSortField();
        final boolean sortAscending = req.getSort().getAscending();

        final UserDao.ListFilter filter = new UserDao.ListFilter();

        //
        final Long userGroupId = req.getSelect().getUserGroupId();

        if (userGroupId != null) {

            final UserGroupDao userGroupDao =
                    ServiceContext.getDaoContext().getUserGroupDao();

            final ReservedUserGroupEnum reservedGroup =
                    userGroupDao.findReservedGroup(userGroupId);

            if (reservedGroup == null) {
                filter.setUserGroupId(req.getSelect().getUserGroupId());
            } else {
                filter.setInternal(reservedGroup.isInternalExternal());
            }
        }

        //
        filter.setContainingNameOrIdText(
                req.getSelect().getNameIdContainingText());
        filter.setContainingEmailText(req.getSelect().getEmailContainingText());
        filter.setAdmin(req.getSelect().getAdmin());
        filter.setPerson(req.getSelect().getPerson());
        filter.setDisabled(req.getSelect().getDisabled());
        filter.setDeleted(req.getSelect().getDeleted());

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final long userCount = userDao.getListCount(filter);

        /*
         * Display the requested page.
         */
        final List<User> entryList =
                userDao.getListChunk(filter, req.calcStartPosition(),
                        req.getMaxResults(), sortField, sortAscending);

        //
        add(new UserListView("users-view", entryList,
                this.probePermissionToEdit(ACLOidEnum.A_USERS)));

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, userCount, MAX_PAGES_IN_NAVBAR,
                "sp-users-page", new String[] { "nav-bar-1", "nav-bar-2" });

        final MarkupHelper helper = new MarkupHelper(this);
        helper.addButton("btn-gdpr-download", HtmlButtonEnum.DOWNLOAD);
    }
}
