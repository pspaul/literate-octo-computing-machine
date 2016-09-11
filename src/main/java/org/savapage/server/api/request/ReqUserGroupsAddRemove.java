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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.users.conf.InternalGroupList;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserGroupsAddRemove extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        /**
         * List of group names to add.
         */
        private List<String> groupsAdded;

        /**
         * List of {@link UserGroup#getId()} values to remove.
         */
        private List<Long> groupsRemoved;

        public List<String> getGroupsAdded() {
            return groupsAdded;
        }

        @SuppressWarnings("unused")
        public void setGroupsAdded(List<String> groupsAdded) {
            this.groupsAdded = groupsAdded;
        }

        public List<Long> getGroupsRemoved() {
            return groupsRemoved;
        }

        @SuppressWarnings("unused")
        public void setGroupsRemoved(List<Long> groupsRemoved) {
            this.groupsRemoved = groupsRemoved;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        //
        int nAdded = 0;
        int nRemoved = 0;

        if (dtoReq.getGroupsAdded() != null
                && !dtoReq.getGroupsAdded().isEmpty()) {

            final Set<String> internalGroups = InternalGroupList.getGroups();

            final Set<String> internalGroupsCollected = new HashSet<>();

            final DaoBatchCommitter batchCommitter =
                    ServiceContext.getDaoContext().createBatchCommitter(
                            ConfigManager.getDaoBatchChunkSize());

            batchCommitter.open();

            for (final String groupName : dtoReq.getGroupsAdded()) {

                /*
                 * Internal group names take precedence.
                 */
                if (internalGroups.contains(groupName)) {
                    internalGroupsCollected.add(groupName);
                    continue;
                }

                final AbstractJsonRpcMethodResponse rpcResponse =
                        USER_GROUP_SERVICE.addUserGroup(batchCommitter,
                                groupName);

                if (!isApiResultOk(rpcResponse)) {
                    this.setApiResultText(rpcResponse);
                    return;
                }

                batchCommitter.commit();
                nAdded++;
            }

            for (final String groupName : internalGroupsCollected) {

                USER_GROUP_SERVICE.addInternalUserGroup(batchCommitter,
                        groupName);
                batchCommitter.commit();
                nAdded++;
            }

            batchCommitter.close();
        }

        if (dtoReq.getGroupsRemoved() != null
                && !dtoReq.getGroupsRemoved().isEmpty()) {

            final DaoContext daoCtx = ServiceContext.getDaoContext();

            for (final Long groupId : dtoReq.getGroupsRemoved()) {

                if (!daoCtx.isTransactionActive()) {
                    daoCtx.beginTransaction();
                }

                final AbstractJsonRpcMethodResponse rpcResponse =
                        USER_GROUP_SERVICE.deleteUserGroup(groupId);

                if (!isApiResultOk(rpcResponse)) {
                    this.setApiResultText(rpcResponse);
                    return;
                }

                daoCtx.commit();
                nRemoved++;
            }
        }

        if (nAdded == 0 && nRemoved == 0) {

            this.setApiResult(ApiResultCodeEnum.WARN, "msg-no-groups");

        } else {
            final StringBuilder msg = new StringBuilder();

            if (nAdded > 0) {
                msg.append(this.localize("msg-groups-added",
                        String.valueOf(nAdded)));
            }

            if (nRemoved > 0) {
                if (nAdded > 0) {
                    msg.append(" ");
                }
                msg.append(this.localize("msg-groups-removed",
                        String.valueOf(nRemoved)));
            }
            setApiResultText(ApiResultCodeEnum.OK, msg.toString());
        }
    }

}
