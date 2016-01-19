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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.savapage.core.dao.UserGroupAttrDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.UserGroupAttrEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupAttr;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.JsonHelper;

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
        private List<ACLRoleEnum> aclRoles;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<ACLRoleEnum> getAclRoles() {
            return aclRoles;
        }

        public void setAclRoles(List<ACLRoleEnum> aclRoles) {
            this.aclRoles = aclRoles;
        }

    }

    @Override
    protected void
            onRequest(final String requestingUser, final User lockedUser)
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

        //
        final DtoRsp dtoRsp = new DtoRsp();

        dtoRsp.setId(userGroup.getId());
        dtoRsp.setName(userGroup.getGroupName());

        // ACL
        final UserGroupAttrDao attrDao =
                ServiceContext.getDaoContext().getUserGroupAttrDao();

        final UserGroupAttr aclAttr =
                attrDao.findByName(userGroup, UserGroupAttrEnum.ACL_ROLES);

        if (aclAttr != null) {

            final EnumSet<ACLRoleEnum> enumSet =
                    JsonHelper.deserializeEnumSet(ACLRoleEnum.class,
                            aclAttr.getValue());

            final ArrayList<ACLRoleEnum> list = new ArrayList<>();
            final Iterator<ACLRoleEnum> iter = enumSet.iterator();
            while (iter.hasNext()) {
                list.add(iter.next());
            }
            dtoRsp.setAclRoles(list);
        }

        this.setResponse(dtoRsp);

        setApiResultOk();
    }
}
