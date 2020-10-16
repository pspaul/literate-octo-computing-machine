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

import org.savapage.core.dto.UserDto;
import org.savapage.core.jpa.User;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.ErrorDataBasic;

/**
 * Edits or creates a User (a logical delete is not handled).
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserSet extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final UserDto userDto =
                UserDto.create(UserDto.class, this.getParmValueDto());

        final boolean isNew = userDto.getDatabaseId() == null;

        AbstractJsonRpcMethodResponse rpcResponse =
                USER_SERVICE.setUser(userDto, isNew);

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
