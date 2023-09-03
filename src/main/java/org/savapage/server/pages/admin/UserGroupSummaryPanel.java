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

import java.util.Iterator;
import java.util.List;

import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.UserGroupAccount;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.PageItemRelatedPanel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserGroupSummaryPanel extends PageItemRelatedPanel {

    /** */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The non-null id of this component.
     */
    public UserGroupSummaryPanel(final String id) {
        super(id);
    }

    /**
     * @param account
     */
    public void populate(final Account account) {

        final StringBuilder accString = new StringBuilder();
        int groupCount = 0;

        final List<UserGroupAccount> accList = account.getMemberGroups();
        for (Iterator<UserGroupAccount> iterator = accList.iterator(); iterator
                .hasNext();) {
            final UserGroupAccount userGroupAccount = iterator.next();
            groupCount++;
            if (accString.length() > 0) {
                accString.append(", ");
            }
            accString.append(userGroupAccount.getUserGroup().getGroupName());
        }

        this.populate(groupCount, accString.toString(),
                MarkupHelper.IMG_PATH_USER_PRIVILEGES);
    }

}
