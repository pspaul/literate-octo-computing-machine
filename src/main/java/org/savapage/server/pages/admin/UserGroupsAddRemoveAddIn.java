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
package org.savapage.server.pages.admin;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.services.ServiceContext;

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

        /**
        *
        */
        private static final long serialVersionUID = 1L;

        private final String optionId;

        /**
         *
         * @param id
         * @param list
         */
        public RemoveGroupsView(String id, List<UserGroup> list,
                final String optionId) {
            super(id, list);
            this.optionId = optionId;
        }

        @Override
        protected void populateItem(final ListItem<UserGroup> item) {
            final UserGroup group = item.getModelObject();
            final Label label = new Label(this.optionId, group.getGroupName());
            label.add(new AttributeModifier("value", group.getId()));
            item.add(label);
        }

    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private class AddGroupsView extends PropertyListView<Object> {

        /**
       *
       */
        private static final long serialVersionUID = 1L;

        private final String optionId;

        /**
         *
         * @param id
         * @param list
         */
        public AddGroupsView(String id, List<Object> list, final String optionId) {
            super(id, list);
            this.optionId = optionId;
        }

        @Override
        protected void populateItem(final ListItem<Object> item) {
            final Object group = item.getModelObject();
            final Label label = new Label(this.optionId, group.toString());
            label.add(new AttributeModifier("value", group.toString()));
            item.add(label);
        }

    }

    /**
     *
     */
    public UserGroupsAddRemoveAddIn() {

        final UserGroupDao.ListFilter filter = new UserGroupDao.ListFilter();

        final UserGroupDao userGroupDao =
                ServiceContext.getDaoContext().getUserGroupDao();

        final List<UserGroup> entryListRemove =
                userGroupDao.getListChunk(filter, null, null,
                        UserGroupDao.Field.NAME, true);

        final SortedSet<String> setAdd =
                ConfigManager.instance().getUserSource().getGroups();

        /*
         * Remove reserved groups and groups which are already added from the
         * remove list.
         */
        final Iterator<UserGroup> iter = entryListRemove.iterator();

        while (iter.hasNext()) {

            final UserGroup group = iter.next();
            final String name = group.getGroupName();

            if (ReservedUserGroupEnum.fromDbName(name) != null) {
                iter.remove();
            } else {
                setAdd.remove(name);
            }
        }

        //
        add(new AddGroupsView("groups-add", Arrays.asList(setAdd.toArray()),
                "option-add"));
        add(new RemoveGroupsView("groups-remove", entryListRemove,
                "option-remove"));

    }
}
