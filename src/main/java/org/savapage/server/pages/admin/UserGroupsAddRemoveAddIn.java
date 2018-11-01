/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.server.pages.admin;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.users.CommonUserGroup;
import org.savapage.core.users.conf.InternalGroupList;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserGroupsAddRemoveAddIn extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private class RemoveGroupsView extends PropertyListView<UserGroup> {

        /** */
        private static final long serialVersionUID = 1L;

        /** */
        private final String optionId;

        /**
         * @param id
         *            Wicket id of the view.
         * @param list
         *            The user group list.
         * @param optionId
         *            Wicket id op the HTML option.
         */
        RemoveGroupsView(final String id, final List<UserGroup> list,
                final String optionId) {
            super(id, list);
            this.optionId = optionId;
        }

        @Override
        protected void populateItem(final ListItem<UserGroup> item) {

            final UserGroup group = item.getModelObject();

            final StringBuilder disp = new StringBuilder();
            disp.append(group.getGroupName());

            if (group.getFullName() != null
                    && !StringUtils.defaultString(group.getFullName())
                            .equals(group.getGroupName())) {
                disp.append(" • ").append(group.getFullName());
            }

            final Label label = new Label(this.optionId, disp.toString());

            label.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                    group.getId()));
            item.add(label);
        }

    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private class AddGroupsView extends PropertyListView<Object> {

        /** */
        private static final long serialVersionUID = 1L;

        /** */
        private final String optionId;

        /**
         * @param id
         *            Wicket id of the view.
         * @param list
         *            The user group list.
         * @param optionId
         *            Wicket id op the HTML option.
         */
        AddGroupsView(final String id, final List<Object> list,
                final String optionId) {
            super(id, list);
            this.optionId = optionId;
        }

        @Override
        protected void populateItem(final ListItem<Object> item) {

            final CommonUserGroup commonGroup =
                    (CommonUserGroup) item.getModelObject();

            final StringBuilder disp = new StringBuilder();
            disp.append(commonGroup.getGroupName());

            if (StringUtils.isNotBlank(commonGroup.getFullName())
                    && !commonGroup.getFullName()
                            .equals(commonGroup.getGroupName())) {
                disp.append(" • ").append(commonGroup.getFullName());
            }

            final Label label = new Label(this.optionId, disp.toString());

            label.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                    commonGroup.getGroupName()));

            item.add(label);
        }

    }

    /**
     * @param parameters
     *            The page parameters.
     */
    public UserGroupsAddRemoveAddIn(final PageParameters parameters) {

        super(parameters, ACLOidEnum.A_USER_GROUPS, RequiredPermission.EDIT);

        /*
         * User Source and Internal Groups are candidates to Add.
         */
        final SortedSet<CommonUserGroup> setAdd =
                ConfigManager.instance().getUserSource().getGroups();

        try {
            for (final String grp : InternalGroupList.getGroups()) {
                setAdd.add(new CommonUserGroup(grp));
            }
        } catch (IOException e) {
            throw new SpException(String.format(
                    "Error reading internal groups file: %s", e.getMessage()));
        }

        /*
         * Groups already present are the groups to be Removed.
         */
        final UserGroupDao.ListFilter filter = new UserGroupDao.ListFilter();

        final UserGroupDao userGroupDao =
                ServiceContext.getDaoContext().getUserGroupDao();

        final List<UserGroup> listRemove = userGroupDao.getListChunk(filter,
                null, null, UserGroupDao.Field.ID, true);

        /*
         * Update Add and Remove List.
         *
         * (1) Remove Reserved Groups from the Remove List.
         *
         * (2) Remove Remove List entries from the Add list.
         */
        final Iterator<UserGroup> iter = listRemove.iterator();

        while (iter.hasNext()) {

            final UserGroup group = iter.next();
            final String name = group.getGroupName();

            if (ReservedUserGroupEnum.fromDbName(name) != null) {
                iter.remove();
            } else {
                setAdd.remove(new CommonUserGroup(name));
            }
        }

        //
        add(new AddGroupsView("groups-add", Arrays.asList(setAdd.toArray()),
                "option-add"));
        add(new RemoveGroupsView("groups-remove", listRemove, "option-remove"));
    }
}
