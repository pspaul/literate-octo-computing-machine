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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

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

        private String fullName;

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
        private Boolean accountingEnabled;
        private UserAccountingDto accounting;

        public Long getId() {
            return id;
        }

        @SuppressWarnings("unused")
        public void setId(Long id) {
            this.id = id;
        }

        public String getFullName() {
            return fullName;
        }

        @SuppressWarnings("unused")
        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public Map<ACLRoleEnum, Boolean> getAclRoles() {
            return aclRoles;
        }

        @SuppressWarnings("unused")
        public void setAclRoles(Map<ACLRoleEnum, Boolean> aclRoles) {
            this.aclRoles = aclRoles;
        }

        public Map<ACLOidEnum, ACLPermissionEnum> getAclOidsUser() {
            return aclOidsUser;
        }

        @SuppressWarnings("unused")
        public void
                setAclOidsUser(Map<ACLOidEnum, ACLPermissionEnum> aclOidsUser) {
            this.aclOidsUser = aclOidsUser;
        }

        public Map<ACLOidEnum, List<ACLPermissionEnum>> getAclOidsUserReader() {
            return aclOidsUserReader;
        }

        @SuppressWarnings("unused")
        public void setAclOidsUserReader(
                Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsUserReader) {
            this.aclOidsUserReader = aclOidsUserReader;
        }

        public Map<ACLOidEnum, List<ACLPermissionEnum>> getAclOidsUserEditor() {
            return aclOidsUserEditor;
        }

        @SuppressWarnings("unused")
        public void setAclOidsUserEditor(
                Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsUserEditor) {
            this.aclOidsUserEditor = aclOidsUserEditor;
        }

        public Map<ACLOidEnum, ACLPermissionEnum> getAclOidsAdmin() {
            return aclOidsAdmin;
        }

        @SuppressWarnings("unused")
        public void setAclOidsAdmin(
                Map<ACLOidEnum, ACLPermissionEnum> aclOidsAdmin) {
            this.aclOidsAdmin = aclOidsAdmin;
        }

        public Map<ACLOidEnum, List<ACLPermissionEnum>>
                getAclOidsAdminReader() {
            return aclOidsAdminReader;
        }

        @SuppressWarnings("unused")
        public void setAclOidsAdminReader(
                Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsAdminReader) {
            this.aclOidsAdminReader = aclOidsAdminReader;
        }

        public Map<ACLOidEnum, List<ACLPermissionEnum>>
                getAclOidsAdminEditor() {
            return aclOidsAdminEditor;
        }

        @SuppressWarnings("unused")
        public void setAclOidsAdminEditor(
                Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsAdminEditor) {
            this.aclOidsAdminEditor = aclOidsAdminEditor;
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

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final UserGroupDao dao =
                ServiceContext.getDaoContext().getUserGroupDao();

        final UserGroup userGroup = dao.findById(dtoReq.getId());

        if (userGroup == null) {

            setApiResult(ApiResultCodeEnum.ERROR, "msg-usergroup-not-found",
                    dtoReq.getId().toString());
            return;
        }

        this.setAclRoles(dtoReq, userGroup);

        this.setAclOids(dtoReq.getAclOidsUser(), dtoReq.getAclOidsUserReader(),
                dtoReq.getAclOidsUserEditor(), userGroup,
                UserGroupAttrEnum.ACL_OIDS_USER);

        this.setAclOids(dtoReq.getAclOidsAdmin(),
                dtoReq.getAclOidsAdminReader(), dtoReq.getAclOidsAdminEditor(),
                userGroup, UserGroupAttrEnum.ACL_OIDS_ADMIN);

        final ReservedUserGroupEnum reservedGroup =
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

        if (StringUtils.isBlank(dtoReq.getFullName())) {
            userGroup.setFullName(null);
        } else {
            userGroup.setFullName(dtoReq.getFullName().trim());
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
     * Creates, updates or deletes a {@link UserGroupAttr}.
     *
     * @param daoAttr
     *            The {@link UserGroupAttrDao}.
     * @param userGroup
     *            The user group.
     * @param attrEnum
     *            The attribute key.
     * @param attrValue
     *            The attribute value. When {@code null} an existing attribute
     *            is deleted.
     */
    private static void crudUserGroupAttr(final UserGroupAttrDao daoAttr,
            final UserGroup userGroup, final UserGroupAttrEnum attrEnum,
            final String attrValue) {

        UserGroupAttr attr = daoAttr.findByName(userGroup, attrEnum);

        if (attr == null) {
            if (attrValue != null) {
                attr = new UserGroupAttr();
                attr.setUserGroup(userGroup);
                attr.setName(attrEnum.getName());
                attr.setValue(attrValue);
                daoAttr.create(attr);
            }
        } else if (attrValue == null) {
            daoAttr.delete(attr);
        } else if (!attr.getValue().equals(attrValue)) {
            attr.setValue(attrValue);
            daoAttr.update(attr);
        }
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

        crudUserGroupAttr(daoAttr, userGroup, UserGroupAttrEnum.ACL_ROLES,
                jsonRoles);
    }

    /**
     * Creates, updates or deletes the ACL Privileges.
     *
     * @param aclOids
     *            The map of {@link ACLOidEnum} keys with their
     *            {@link ACLPermissionEnum}.
     * @param aclOidsReader
     * @param aclOidsEditor
     * @param userGroup
     *            The user group to update.
     * @param attrEnum
     *            The attribute type.
     * @throws IOException
     *             When JSON errors.
     */
    private void setAclOids(final Map<ACLOidEnum, ACLPermissionEnum> aclOids,
            final Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsReader,
            final Map<ACLOidEnum, List<ACLPermissionEnum>> aclOidsEditor,
            final UserGroup userGroup, final UserGroupAttrEnum attrEnum)
            throws IOException {

        /*
         * Consolidate.
         */
        final Map<ACLOidEnum, List<ACLPermissionEnum>> mapAll = new HashMap<>();

        for (final Entry<ACLOidEnum, ACLPermissionEnum> entry : aclOids
                .entrySet()) {

            final ACLOidEnum oid = entry.getKey();
            final ACLPermissionEnum value = entry.getValue();

            final List<ACLPermissionEnum> perms = new ArrayList<>();
            mapAll.put(oid, perms);

            if (value == null) {
                continue;
            }

            perms.add(value);

            if (aclOidsReader.containsKey(oid)) {
                perms.addAll(aclOidsReader.get(oid));
            }

            if (aclOidsEditor.containsKey(oid)) {
                perms.addAll(aclOidsEditor.get(oid));
            }
        }

        //
        final Map<ACLOidEnum, Integer> aclOidsPriv = asMapPrivilege(mapAll);

        final String jsonOids;

        if (aclOidsPriv.isEmpty()) {
            jsonOids = null;
        } else {
            jsonOids = JsonHelper.stringifyObject(aclOidsPriv);
        }

        final UserGroupAttrDao daoAttr =
                ServiceContext.getDaoContext().getUserGroupAttrDao();

        crudUserGroupAttr(daoAttr, userGroup, attrEnum, jsonOids);
    }

    /**
     *
     * @param mapIn
     * @return
     */
    public static Map<ACLOidEnum, Integer> asMapPrivilege(
            final Map<ACLOidEnum, List<ACLPermissionEnum>> mapIn) {

        final Map<ACLOidEnum, Integer> mapOut = new HashMap<>();

        for (final Entry<ACLOidEnum, List<ACLPermissionEnum>> entry : mapIn
                .entrySet()) {
            mapOut.put(entry.getKey(), Integer
                    .valueOf(ACLPermissionEnum.asPrivilege(entry.getValue())));
        }
        return mapOut;
    }

}
