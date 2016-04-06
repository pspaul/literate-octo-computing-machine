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
package org.savapage.server.pages.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.dao.helpers.UserGroupPagerReq;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.pages.MarkupHelper;

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
     *
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5; // must be odd number

    /**
     * @return {@code false} to give Admin a chance to inspect the User Groups.
     */
    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     *
     */
    public UserGroupsPage(final PageParameters parameters) {

        super(parameters);

        final String data = getParmValue(POST_PARM_DATA);
        final UserGroupPagerReq req = UserGroupPagerReq.read(data);

        final UserGroupDao.Field sortField = req.getSort().getSortField();
        final boolean sortAscending = req.getSort().getAscending();

        final UserGroupDao.ListFilter filter = new UserGroupDao.ListFilter();

        final UserGroupMemberDao.GroupFilter groupFilter =
                new UserGroupMemberDao.GroupFilter();

        final UserGroupMemberDao groupMemberDao =
                ServiceContext.getDaoContext().getUserGroupMemberDao();

        filter.setContainingText(req.getSelect().getNameContainingText());

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final UserGroupDao userGroupDao =
                ServiceContext.getDaoContext().getUserGroupDao();

        final long groupCount = userGroupDao.getListCount(filter);

        /*
         * Display the requested page.
         */
        final List<UserGroup> entryList =
                userGroupDao.getListChunk(filter, req.calcStartPosition(),
                        req.getMaxResults(), sortField, sortAscending);

        //
        add(new PropertyListView<UserGroup>("user-groups-view", entryList) {

            private static final long serialVersionUID = 1L;

            /**
             *
             * @param item
             * @param mapVisible
             */
            private void evaluateVisible(final ListItem<UserGroup> item,
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

            @Override
            protected void populateItem(final ListItem<UserGroup> item) {

                final Map<String, String> mapVisible = new HashMap<>();

                final UserGroup userGroup = item.getModelObject();

                Label labelWrk = null;

                //
                final ReservedUserGroupEnum reservedGroup =
                        ReservedUserGroupEnum
                                .fromDbName(userGroup.getGroupName());

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
                labelWrk = new Label("groupName", groupName);

                if (reservedGroup != null) {
                    MarkupHelper.appendLabelAttr(labelWrk, "class",
                            MarkupHelper.CSS_TXT_WARN);
                }

                item.add(labelWrk);

                labelWrk = new Label("signal", String.valueOf(userCount));
                item.add(labelWrk);

                /*
                 * Set the uid in 'data-savapage' attribute, so it can be picked
                 * up in JavaScript for editing.
                 */
                labelWrk = new Label("button-edit",
                        getLocalizer().getString("button-edit", this));

                labelWrk.add(new AttributeModifier(
                        MarkupHelper.ATTR_DATA_SAVAPAGE, userGroup.getId()));

                item.add(labelWrk);

                //
                labelWrk = new Label("button-users",
                        getLocalizer().getString("button-users", this));
                labelWrk.add(new AttributeModifier(
                        MarkupHelper.ATTR_DATA_SAVAPAGE, userGroup.getId()));
                item.add(labelWrk);

                //
                labelWrk = new Label("button-account",
                        getLocalizer().getString("button-account", this));
                labelWrk.add(
                        new AttributeModifier(MarkupHelper.ATTR_DATA_SAVAPAGE,
                                userGroup.getGroupName()));
                labelWrk.add(new AttributeModifier(
                        MarkupHelper.ATTR_DATA_SAVAPAGE_TYPE,
                        AccountTypeEnum.GROUP.toString()));
                item.add(labelWrk);

                //
                evaluateVisible(item, mapVisible);

            }
        });

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, groupCount, MAX_PAGES_IN_NAVBAR,
                "sp-user-groups-page",
                new String[] { "nav-bar-1", "nav-bar-2" });
    }

}
