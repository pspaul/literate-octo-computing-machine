/*
 * This file is part of the SavaPage project <http://savapage.org>.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import org.savapage.core.dao.UserGroupAttrDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.dao.enums.UserGroupAttrEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.UserAccountingDto;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupAttr;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.JsonHelper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserGroupSet extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private Long id;

        private Map<ACLRoleEnum, Boolean> aclRoles;
        private Boolean accountingEnabled;
        private UserAccountingDto accounting;

        public Long getId() {
            return id;
        }

        @SuppressWarnings("unused")
        public void setId(Long id) {
            this.id = id;
        }

        public Map<ACLRoleEnum, Boolean> getAclRoles() {
            return aclRoles;
        }

        @SuppressWarnings("unused")
        public void setAclRoles(Map<ACLRoleEnum, Boolean> aclRoles) {
            this.aclRoles = aclRoles;
        }

        public Boolean getAccountingEnabled() {
            return accountingEnabled;
        }

        @SuppressWarnings("unused")
        public void setAccountingEnabled(Boolean accountingEnabled) {
            this.accountingEnabled = accountingEnabled;
        }

        public UserAccountingDto getAccounting() {
            return accounting;
        }

        @SuppressWarnings("unused")
        public void setAccounting(UserAccountingDto accounting) {
            this.accounting = accounting;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        //
        final UserGroupDao dao =
                ServiceContext.getDaoContext().getUserGroupDao();

        final UserGroup userGroup = dao.findById(dtoReq.getId());

        if (userGroup == null) {

            setApiResult(ApiResultCodeEnum.ERROR, "msg-usergroup-not-found",
                    dtoReq.getId().toString());
            return;
        }

        this.setAclRoles(dtoReq, userGroup);

        ReservedUserGroupEnum reservedGroup =
                ReservedUserGroupEnum.fromDbName(userGroup.getGroupName());

        if (reservedGroup == ReservedUserGroupEnum.ALL) {
            userGroup.setInitialSettingsEnabled(Boolean.FALSE);
        } else {
            try {
                ACCOUNTING_SERVICE.setInitialUserAccounting(userGroup,
                        dtoReq.getAccounting());
            } catch (ParseException e) {
                setApiResultText(ApiResultCodeEnum.ERROR, e.getMessage());
                return;
            }
            userGroup.setInitialSettingsEnabled(dtoReq.getAccountingEnabled());
        }

        userGroup.setModifiedBy(ServiceContext.getActor());
        userGroup.setModifiedDate(ServiceContext.getTransactionDate());

        // Update
        ServiceContext.getDaoContext().getUserGroupDao().update(userGroup);

        // Message
        final String groupName;

        if (reservedGroup == null) {
            groupName = userGroup.getGroupName();
        } else {
            groupName = reservedGroup.getUiName();
        }
        setApiResult(ApiResultCodeEnum.OK, "msg-usergroup-updated", groupName);
    }

    /**
     * Sets the ACL roles.
     *
     * @param dtoReq
     *            The request.
     * @param userGroup
     *            The user group.
     * @throws IOException
     *             When JSON errors.
     */
    private void setAclRoles(final DtoReq dtoReq, final UserGroup userGroup)
            throws IOException {

        final UserGroupAttrDao daoAttr =
                ServiceContext.getDaoContext().getUserGroupAttrDao();

        final String jsonRoles;

        if (dtoReq.getAclRoles().isEmpty()) {
            jsonRoles = null;
        } else {
            jsonRoles = JsonHelper.stringifyObject(dtoReq.getAclRoles());
        }

        final UserGroupAttrEnum attrEnum = UserGroupAttrEnum.ACL_ROLES;

        UserGroupAttr attr = daoAttr.findByName(userGroup, attrEnum);

        if (attr == null) {
            if (jsonRoles != null) {
                attr = new UserGroupAttr();
                attr.setUserGroup(userGroup);
                attr.setName(attrEnum.getName());
                attr.setValue(jsonRoles);
                daoAttr.create(attr);
            }
        } else if (jsonRoles == null) {
            daoAttr.delete(attr);
        } else if (!attr.getValue().equals(jsonRoles)) {
            attr.setValue(jsonRoles);
            daoAttr.update(attr);
        }
    }

}
