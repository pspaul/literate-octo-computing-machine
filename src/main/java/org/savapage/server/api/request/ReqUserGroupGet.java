/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.savapage.core.dao.UserGroupAttrDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserGroupGet extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private Long id;

        public Long getId() {
            return id;
        }

        @SuppressWarnings("unused")
        public void setId(Long id) {
            this.id = id;
        }

    }

    /**
     * The response.
     */
    private static class DtoRsp extends AbstractDto {

        private Long id;
        private String name;

        /**
         * The user roles.
         */
        private Map<ACLRoleEnum, Boolean> aclRoles;

        /**
         * OIDS with the main role permission for Role "User". When a
         * {@link ACLOidEnum} key is not present the permission is
         * indeterminate. A {@code null} value implies no privileges.
         */
        @JsonProperty("aclOidsUser")
        private Map<ACLOidEnum, ACLPermissionEnum> aclOidsUser;

        /**
         * OIDS with extra permissions for main {@link ACLPermissionEnum#READER}
         * role for Role "User". When a {@link ACLOidEnum} key is not present in
         * the map extra permissions are not applicable. An empty
         * {@link ACLPermissionEnum} list implies no privileges.
         */
        @JsonProperty("aclOidsUserReader")
        private Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsUserReader;

        /**
         * OIDS with extra permissions for main {@link ACLPermissionEnum#EDITOR}
         * role for Role "User". When a {@link ACLOidEnum} key is not present in
         * the map extra permissions are not applicable. An empty
         * {@link ACLPermissionEnum} list implies no privileges.
         */
        @JsonProperty("aclOidsUserEditor")
        private Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsUserEditor;

        /**
         * OIDS with the main role permission for Role "Admin". When a
         * {@link ACLOidEnum} key is not present the permission is
         * indeterminate. A {@code null} value implies no privileges.
         */
        @JsonProperty("aclOidsAdmin")
        private Map<ACLOidEnum, ACLPermissionEnum> aclOidsAdmin;

        /**
         * OIDS with extra permissions for main {@link ACLPermissionEnum#READER}
         * role for Role "Admin". When a {@link ACLOidEnum} key is not present
         * in the map extra permissions are not applicable. An empty
         * {@link ACLPermissionEnum} list implies no privileges.
         */
        @JsonProperty("aclOidsAdminReader")
        private Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsAdminReader;

        /**
         * OIDS with extra permissions for main {@link ACLPermissionEnum#EDITOR}
         * role for Role "Admin". When a {@link ACLOidEnum} key is not present
         * in the map extra permissions are not applicable. An empty
         * {@link ACLPermissionEnum} list implies no privileges.
         */
        @JsonProperty("aclOidsAdminEditor")
        private Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsAdminEditor;

        //
        private Boolean builtInGroup;
        private Boolean allUsersGroup;
        private Boolean accountingEnabled;
        private UserAccountingDto accounting;

        @SuppressWarnings("unused")
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @SuppressWarnings("unused")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @SuppressWarnings("unused")
        public Map<ACLRoleEnum, Boolean> getAclRoles() {
            return aclRoles;
        }

        public void setAclRoles(Map<ACLRoleEnum, Boolean> aclRoles) {
            this.aclRoles = aclRoles;
        }

        public Map<ACLOidEnum, ACLPermissionEnum> getAclOidsUser() {
            return aclOidsUser;
        }

        public void
                setAclOidsUser(Map<ACLOidEnum, ACLPermissionEnum> aclOidsUser) {
            this.aclOidsUser = aclOidsUser;
        }

        public Map<ACLOidEnum, List<ACLPermissionEnum>> getAclOidsUserReader() {
            return aclOidsUserReader;
        }

        public void setAclOidsUserReader(
                Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsUserReader) {
            this.aclOidsUserReader = aclOidsUserReader;
        }

        public Map<ACLOidEnum, List<ACLPermissionEnum>> getAclOidsUserEditor() {
            return aclOidsUserEditor;
        }

        public void setAclOidsUserEditor(
                Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsUserEditor) {
            this.aclOidsUserEditor = aclOidsUserEditor;
        }

        public Map<ACLOidEnum, ACLPermissionEnum> getAclOidsAdmin() {
            return aclOidsAdmin;
        }

        public void setAclOidsAdmin(
                Map<ACLOidEnum, ACLPermissionEnum> aclOidsAdmin) {
            this.aclOidsAdmin = aclOidsAdmin;
        }

        public Map<ACLOidEnum, List<ACLPermissionEnum>>
                getAclOidsAdminReader() {
            return aclOidsAdminReader;
        }

        public void setAclOidsAdminReader(
                Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsAdminReader) {
            this.aclOidsAdminReader = aclOidsAdminReader;
        }

        public Map<ACLOidEnum, List<ACLPermissionEnum>>
                getAclOidsAdminEditor() {
            return aclOidsAdminEditor;
        }

        public void setAclOidsAdminEditor(
                Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsAdminEditor) {
            this.aclOidsAdminEditor = aclOidsAdminEditor;
        }

        @SuppressWarnings("unused")
        public Boolean getBuiltInGroup() {
            return builtInGroup;
        }

        public void setBuiltInGroup(Boolean builtInGroup) {
            this.builtInGroup = builtInGroup;
        }

        @SuppressWarnings("unused")
        public Boolean getAllUsersGroup() {
            return allUsersGroup;
        }

        public void setAllUsersGroup(Boolean allUsersGroup) {
            this.allUsersGroup = allUsersGroup;
        }

        @SuppressWarnings("unused")
        public Boolean getAccountingEnabled() {
            return accountingEnabled;
        }

        public void setAccountingEnabled(Boolean accountingEnabled) {
            this.accountingEnabled = accountingEnabled;
        }

        @SuppressWarnings("unused")
        public UserAccountingDto getAccounting() {
            return accounting;
        }

        public void setAccounting(UserAccountingDto accounting) {
            this.accounting = accounting;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        final UserGroupDao dao =
                ServiceContext.getDaoContext().getUserGroupDao();

        final UserGroup userGroup = dao.findById(dtoReq.getId());

        if (userGroup == null) {

            setApiResult(ApiResultCodeEnum.ERROR, "msg-usergroup-not-found",
                    dtoReq.getId().toString());
            return;
        }

        final ReservedUserGroupEnum reservedUserGroup =
                ReservedUserGroupEnum.fromDbName(userGroup.getGroupName());

        //
        final DtoRsp dtoRsp = new DtoRsp();

        dtoRsp.setId(userGroup.getId());

        if (reservedUserGroup == null) {
            dtoRsp.setName(userGroup.getGroupName());
        } else {
            dtoRsp.setName(reservedUserGroup.getUiName());
        }

        dtoRsp.setBuiltInGroup(reservedUserGroup != null);
        dtoRsp.setAllUsersGroup(reservedUserGroup == ReservedUserGroupEnum.ALL);
        dtoRsp.setAccountingEnabled(userGroup.getInitialSettingsEnabled());

        // ACL
        final UserGroupAttrDao attrDao =
                ServiceContext.getDaoContext().getUserGroupAttrDao();

        final UserGroupAttr aclAttr =
                attrDao.findByName(userGroup, UserGroupAttrEnum.ACL_ROLES);

        Map<ACLRoleEnum, Boolean> aclRoles;

        if (aclAttr == null) {
            aclRoles = null;
        } else {
            aclRoles = JsonHelper.createEnumBooleanMapOrNull(ACLRoleEnum.class,
                    aclAttr.getValue());
        }

        if (aclRoles == null) {
            aclRoles = new HashMap<ACLRoleEnum, Boolean>();
        }

        dtoRsp.setAclRoles(aclRoles);

        // ACL OID User
        Map<ACLOidEnum, Integer> mapOid = getAclOidMap(attrDao, userGroup,
                UserGroupAttrEnum.ACL_OIDS_USER);

        dtoRsp.setAclOidsUser(ACLOidEnum.asMapRole(mapOid));
        dtoRsp.setAclOidsUserReader(ACLOidEnum.asMapPermsReader(mapOid));
        dtoRsp.setAclOidsUserEditor(ACLOidEnum.asMapPermsEditor(mapOid));

        // ACL OID Admin
        mapOid = getAclOidMap(attrDao, userGroup,
                UserGroupAttrEnum.ACL_OIDS_ADMIN);

        dtoRsp.setAclOidsAdmin(ACLOidEnum.asMapRole(mapOid));
        dtoRsp.setAclOidsAdminReader(ACLOidEnum.asMapPermsReader(mapOid));
        dtoRsp.setAclOidsAdminEditor(ACLOidEnum.asMapPermsEditor(mapOid));

        //
        dtoRsp.setAccounting(
                ACCOUNTING_SERVICE.getInitialUserAccounting(userGroup));

        //
        this.setResponse(dtoRsp);
        setApiResultOk();
    }

    /**
     *
     * @param attrDao
     * @param userGroup
     * @param attrEnum
     * @return
     */
    private Map<ACLOidEnum, Integer> getAclOidMap(
            final UserGroupAttrDao attrDao, final UserGroup userGroup,
            final UserGroupAttrEnum attrEnum) {

        UserGroupAttr aclAttr = attrDao.findByName(userGroup, attrEnum);

        Map<ACLOidEnum, Integer> aclOids;

        if (aclAttr == null) {
            aclOids = null;
        } else {
            aclOids = JsonHelper.createEnumIntegerMapOrNull(ACLOidEnum.class,
                    aclAttr.getValue());
        }

        if (aclOids == null) {
            aclOids = new HashMap<ACLOidEnum, Integer>();
        }

        return aclOids;
    }

}
