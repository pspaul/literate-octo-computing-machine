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

import org.savapage.core.dao.UserDao;
import org.savapage.core.dto.UserDto;
import org.savapage.core.jpa.User;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.services.ServiceContext;

/**
 * Edits or creates a User (a logical delete is not handled).
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrintSiteUserSet extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final UserDto dto =
                UserDto.create(UserDto.class, this.getParmValueDto());

        final boolean isNew = dto.getDatabaseId() == null;

        if (!isNew) {
            /*
             * Since not all attributes are editable in Print Site, we retrieve
             * the missing ones, so we can use the generic setUser() service.
             */
            final UserDao userDao = ServiceContext.getDaoContext().getUserDao();
            final User user = userDao.findActiveUserById(dto.getDatabaseId());
            final UserDto dtoDb = USER_SERVICE.createUserDto(user);

            dto.setId(dtoDb.getId());
            dto.setPin(dtoDb.getPin());
            dto.setUuid(dtoDb.getUuid());
            dto.setPassword(dtoDb.getPassword());
            dto.setYubiKeyPubId(dtoDb.getYubiKeyPubId());
            dto.setPgpPubKeyId(dtoDb.getPgpPubKeyId());
        }

        AbstractJsonRpcMethodResponse rpcResponse =
                USER_SERVICE.setUser(dto, isNew);

        if (rpcResponse.isResult()) {

            final String msgKeyOk;

            if (isNew) {
                msgKeyOk = "msg-user-created-ok";
            } else {
                msgKeyOk = "msg-user-saved-ok";
            }

            setApiResult(ApiResultCodeEnum.OK, msgKeyOk);

        } else {
            setApiResultText(ApiResultCodeEnum.ERROR, rpcResponse.asError()
                    .getError().data(ErrorDataBasic.class).getReason());
        }
    }

}
