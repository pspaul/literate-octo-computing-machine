/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchUserGroupMemberFilterDto;
import org.savapage.core.dto.QuickSearchUserGroupMemberItemDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 * User Group Members Quicksearch.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserGroupMemberQuickSearch extends ApiRequestMixin {

    /**
     * The number of rows to retrieve in the Quick Search result set as multiple
     * of requested rows. This factor is needed since some retrieved items don't
     * qualify for the ACL role.
     */
    private static final int MAX_RESULTS_FACTOR = 2;

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoRsp extends AbstractDto {

        private List<QuickSearchUserGroupMemberItemDto> items;

        private String searchMsg;

        @SuppressWarnings("unused")
        public List<QuickSearchUserGroupMemberItemDto> getItems() {
            return items;
        }

        public void setItems(List<QuickSearchUserGroupMemberItemDto> items) {
            this.items = items;
        }

        @SuppressWarnings("unused")
        public String getSearchMsg() {
            return searchMsg;
        }

        public void setSearchMsg(String searchMsg) {
            this.searchMsg = searchMsg;
        }
    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final QuickSearchUserGroupMemberFilterDto dto =
                AbstractDto.create(QuickSearchUserGroupMemberFilterDto.class,
                        this.getParmValue("dto"));

        final List<QuickSearchUserGroupMemberItemDto> items = new ArrayList<>();

        //
        final UserDao.ListFilter filter = new UserDao.ListFilter();

        filter.setUserGroupId(dto.getGroupId());
        filter.setContainingNameText(dto.getFilter());
        filter.setDeleted(Boolean.FALSE);

        final int nUsersMax = dto.getMaxResults();

        int nUsersConsumed = 0;
        int nUsersSelected = 0;
        int iStartPosition = 0;

        // The number of rows to retrieve in the Quick Search result set.
        final int maxListChunkResults = MAX_RESULTS_FACTOR * nUsersMax;

        final List<User> userListChunkFirst =
                userDao.getListChunk(filter, iStartPosition,
                        maxListChunkResults, UserDao.Field.USERID, true);

        for (final User user : userListChunkFirst) {

            nUsersConsumed++;

            final boolean selectUser;

            if (dto.getAclRole() == null) {
                // Not restricted by role.
                selectUser = true;
            } else if (dto.getAclRole() == ACLRoleEnum.PRINT_DELEGATOR
                    && user.getUserId().equals(requestingUser)) {
                // Requesting user can be its own print delegator.
                selectUser = true;
            } else {
                selectUser = ACCESSCONTROL_SERVICE.isAuthorized(user,
                        dto.getAclRole());
            }

            if (!selectUser) {
                continue;
            }

            final QuickSearchUserGroupMemberItemDto itemWlk =
                    new QuickSearchUserGroupMemberItemDto();

            itemWlk.setKey(user.getId());
            itemWlk.setUserId(user.getUserId());
            itemWlk.setFullName(user.getFullName());

            items.add(itemWlk);

            nUsersSelected++;

            if (nUsersSelected == nUsersMax) {
                break;
            }
        }

        /*
         * Response.
         */
        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(items);

        // Assume there are next candidates.
        String msg = ". . .";

        if (nUsersConsumed == maxListChunkResults) {
            // Is next chunk available?
            iStartPosition += maxListChunkResults;
            final List<User> userListChunkNext = userDao.getListChunk(filter,
                    iStartPosition, 1, UserDao.Field.USERID, true);

            if (userListChunkNext.isEmpty()) {
                msg = null;
            }
        } else if (nUsersConsumed != nUsersMax) {
            msg = null;
        }

        rsp.setSearchMsg(msg);

        setResponse(rsp);
        setApiResultOk();
    }
}
