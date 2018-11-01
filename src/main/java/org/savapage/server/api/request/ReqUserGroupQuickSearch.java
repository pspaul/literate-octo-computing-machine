/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchFilterUserGroupDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.QuickSearchUserGroupItemDto;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.services.ServiceContext;

/**
 * User Group Quicksearch.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserGroupQuickSearch extends ReqQuickSearchMixin {

    /** */
    private static final UserGroupDao USER_GROUP_DAO =
            ServiceContext.getDaoContext().getUserGroupDao();

    /** */
    private static final UserGroupMemberDao USER_GROUP_MEMBER_DAO =
            ServiceContext.getDaoContext().getUserGroupMemberDao();

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoRsp extends DtoQuickSearchRsp {

        private List<QuickSearchItemDto> items;

        @SuppressWarnings("unused")
        public List<QuickSearchItemDto> getItems() {
            return items;
        }

        public void setItems(List<QuickSearchItemDto> items) {
            this.items = items;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final QuickSearchFilterUserGroupDto dto = AbstractDto.create(
                QuickSearchFilterUserGroupDto.class, this.getParmValueDto());

        final int maxResult = dto.getMaxResults().intValue();
        final int currPosition = dto.getStartPosition();

        final User dbUser = ServiceContext.getDaoContext().getUserDao()
                .findById(this.getSessionUser().getId());

        final Set<Long> preferredGroups =
                USER_SERVICE.getPreferredDelegateGroups(dbUser);

        final UserGroupDao.ListFilter groupFilter =
                new UserGroupDao.ListFilter();

        if (BooleanUtils.isTrue(dto.getHideId())) {
            groupFilter.setContainingNameText(dto.getFilter());
        } else {
            groupFilter.setContainingNameOrIdText(dto.getFilter());
        }
        groupFilter.setAclRole(dto.getAclRole());

        if (dto.isPreferred()) {
            groupFilter.setGroupIds(preferredGroups);
        }

        final int totalResults;
        final List<UserGroup> userGroupList;

        if (dto.isPreferred() && preferredGroups == null) {

            userGroupList = new ArrayList<>();
            totalResults = 0;

        } else {

            if (dto.getTotalResults() == null) {
                totalResults = (int) USER_GROUP_DAO.getListCount(groupFilter);
            } else {
                totalResults = dto.getTotalResults().intValue();
            }

            userGroupList = USER_GROUP_DAO.getListChunk(groupFilter,
                    Integer.valueOf(currPosition), dto.getMaxResults(),
                    UserGroupDao.Field.NAME, true);
        }

        final List<QuickSearchItemDto> items = new ArrayList<>();

        if (!userGroupList.isEmpty()) {

            final UserGroupMemberDao.GroupFilter groupMemberFilter =
                    new UserGroupMemberDao.GroupFilter();

            groupMemberFilter.setAclRoleNotFalse(dto.getAclRole());

            if (dto.getAclRole() == ACLRoleEnum.PRINT_DELEGATOR) {
                groupMemberFilter.setDisabledPrintOut(Boolean.FALSE);
            }

            for (final UserGroup group : userGroupList) {

                groupMemberFilter.setGroupId(group.getId());
                final long userCount =
                        USER_GROUP_MEMBER_DAO.getUserCount(groupMemberFilter);

                final QuickSearchUserGroupItemDto itemWlk =
                        new QuickSearchUserGroupItemDto();

                itemWlk.setKey(group.getId());
                itemWlk.setText(group.getGroupName());
                itemWlk.setFullName(group.getFullName());
                itemWlk.setPreferred(preferredGroups != null
                        && preferredGroups.contains(group.getId()));

                itemWlk.setUserCount(userCount);
                items.add(itemWlk);
            }
        }

        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(items);
        rsp.calcNavPositions(maxResult, currPosition, totalResults);

        setResponse(rsp);
        setApiResultOk();
    }

}
