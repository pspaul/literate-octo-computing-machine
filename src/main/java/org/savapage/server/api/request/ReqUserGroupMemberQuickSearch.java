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
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchUserGroupMemberFilterDto;
import org.savapage.core.dto.QuickSearchUserGroupMemberItemDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 * User Group Members quick search.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserGroupMemberQuickSearch extends ReqQuickSearchMixin {

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoRsp extends DtoQuickSearchRsp {

        private List<QuickSearchUserGroupMemberItemDto> items;

        @SuppressWarnings("unused")
        public List<QuickSearchUserGroupMemberItemDto> getItems() {
            return items;
        }

        public void setItems(List<QuickSearchUserGroupMemberItemDto> items) {
            this.items = items;
        }
    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final QuickSearchUserGroupMemberFilterDto dto =
                AbstractDto.create(QuickSearchUserGroupMemberFilterDto.class,
                        this.getParmValueDto());

        final List<QuickSearchUserGroupMemberItemDto> items = new ArrayList<>();

        final UserDao.ListFilter userFilter = new UserDao.ListFilter();

        userFilter.setUserGroupId(dto.getGroupId());

        final UserDao.Field sortField;

        switch (dto.getUserDetail()) {
        case FULL:
            userFilter.setContainingNameOrIdText(dto.getFilter());
            sortField = UserDao.Field.NAME;
            break;
        case ID:
            userFilter.setContainingIdText(dto.getFilter());
            sortField = UserDao.Field.USERID;
            break;
        case NAME:
            userFilter.setContainingNameText(dto.getFilter());
            sortField = UserDao.Field.NAME;
            break;
        default:
            throw new RuntimeException(dto.getUserDetail() + " NOT handled.");
        }

        userFilter.setDeleted(Boolean.FALSE);
        userFilter.setDisabled(Boolean.FALSE);

        if (dto.getAclRole() != null) {

            final Long aclRoleUserExt;
            if (dto.getAclRole() == ACLRoleEnum.PRINT_DELEGATOR) {
                // Add session as delegator.
                aclRoleUserExt = this.getSessionUserDbKey();
            } else {
                aclRoleUserExt = null;
            }

            userFilter.setAclFilter(createACLFilter(dto.getAclRole(),
                    dto.getGroupId() != null, aclRoleUserExt));
        }

        final int totalResults;

        if (dto.getTotalResults() == null) {
            totalResults = (int) userDao.getListCount(userFilter);
        } else {
            totalResults = dto.getTotalResults().intValue();
        }

        final List<User> userListChunkFirst = userDao.getListChunk(userFilter,
                dto.getStartPosition(), dto.getMaxResults(), sortField, true);

        for (final User user : userListChunkFirst) {

            final QuickSearchUserGroupMemberItemDto itemWlk =
                    new QuickSearchUserGroupMemberItemDto();

            itemWlk.setKey(user.getId());
            itemWlk.setUserId(user.getUserId());
            itemWlk.setFullName(user.getFullName());

            items.add(itemWlk);
        }

        /*
         * Response.
         */
        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(items);
        rsp.calcNavPositions(dto.getMaxResults().intValue(),
                dto.getStartPosition().intValue(), totalResults);

        setResponse(rsp);
        setApiResultOk();
    }

    /**
     * Creates an {@link UserDao.ACLFilter} for a {@link ACLRoleEnum}.
     *
     * @param role
     *            The role.
     * @param withinGroup
     *            If {@code true}, a filter is created that will work within
     *            User Group scope only.
     * @param aclRoleUserExt
     *            Optional database key of requesting user who is assigned
     *            ACLRoleEnum ad-hoc. This attribute is relevant when role is
     *            not null.
     * @return The filter.
     */
    private static UserDao.ACLFilter createACLFilter(final ACLRoleEnum role,
            final boolean withinGroup, final Long aclRoleUserExt) {

        final UserDao.ACLFilter filter = new UserDao.ACLFilter();
        filter.setAclRole(role);

        if (role != null && aclRoleUserExt != null) {
            final List<Long> lst = new ArrayList<>();
            lst.add(aclRoleUserExt);
            filter.setAclRoleUsersExt(lst);
        }

        if (withinGroup) {
            filter.setAclUserExternal(true);
            filter.setAclUserInternal(true);
            return filter;
        }

        final Boolean authExt = ACCESSCONTROL_SERVICE.isGroupAuthorized(
                USER_GROUP_SERVICE.getExternalUserGroup(), role);

        final Boolean authInt = ACCESSCONTROL_SERVICE.isGroupAuthorized(
                USER_GROUP_SERVICE.getInternalUserGroup(), role);

        if (authExt != null && authInt != null) {

            filter.setAclUserExternal(authExt.booleanValue());
            filter.setAclUserInternal(authInt.booleanValue());

        } else {

            final Boolean authAll =
                    BooleanUtils.isTrue(ACCESSCONTROL_SERVICE.isGroupAuthorized(
                            USER_GROUP_SERVICE.getAllUserGroup(), role));

            if (authExt == null) {
                filter.setAclUserExternal(authAll.booleanValue());
            } else {
                filter.setAclUserExternal(authExt.booleanValue());
            }

            if (authInt == null) {
                filter.setAclUserInternal(authAll.booleanValue());
            } else {
                filter.setAclUserInternal(authInt.booleanValue());
            }
        }

        return filter;
    }
}
