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

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
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
public final class UsersBase extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public UsersBase(final PageParameters parameters) {

        super(parameters);

        addVisible(ConfigManager.isInternalUsersEnabled(), "button-new",
                localized("button-new"));

        //
        final UserGroupDao userGroupDao =
                ServiceContext.getDaoContext().getUserGroupDao();

        final List<UserGroup> groupList =
                userGroupDao.getListChunk(new UserGroupDao.ListFilter(), null,
                        null, UserGroupDao.Field.NAME, true);

        add(new PropertyListView<UserGroup>("option-list-groups", groupList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<UserGroup> item) {

                final UserGroup group = item.getModel().getObject();

                final ReservedUserGroupEnum reservedGroup =
                        ReservedUserGroupEnum.fromDbName(group.getGroupName());

                final String groupName;

                if (reservedGroup == null) {
                    groupName = group.getGroupName();
                } else {
                    groupName = reservedGroup.getUiName();
                }

                final Label label = new Label("option-group", groupName);
                label.add(new AttributeModifier("value", group.getId()));
                item.add(label);
            }

        });

    }
}
