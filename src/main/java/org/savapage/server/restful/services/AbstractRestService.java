/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2023 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2023 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.server.restful.services;

import java.util.Base64;
import java.util.StringTokenizer;
import java.util.UUID;

import org.savapage.core.dao.UserDao;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.server.restful.RestAuthException;
import org.savapage.server.restful.RestAuthFilter;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractRestService implements IRestService {

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    protected static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();

    /**
     * Gets userid and password from authorization.
     *
     * @param authorization
     *            Header with format "Basic base64(user:password)"
     * @return Array with userid (index 0) and password (index 1).
     */
    protected String[] getUserPasswordFromAuth(final String authorization) {
        final String[] array = new String[2];

        // Get encoded username and password
        final String encodedUserPassword = authorization.replaceFirst(
                RestAuthFilter.HEADER_AUTHENTICATION_SCHEME + " ", "");

        // Decode username and password
        final String usernameAndPassword = new String(
                Base64.getDecoder().decode(encodedUserPassword.getBytes()));

        // Split username and password tokens
        final StringTokenizer tokenizer =
                new StringTokenizer(usernameAndPassword, ":");

        array[0] = tokenizer.nextToken();
        array[1] = tokenizer.nextToken();

        return array;
    }

    /**
     *
     * @param authorization
     *            Header with format "Basic base64(user:password)"
     * @param msgPfx
     *            Message prefix.
     * @return Authenticated user.
     * @throws RestAuthException
     *             If authentication failed.
     */
    protected User isUserAuthenticated(final String authorization,
            final String msgPfx) throws RestAuthException {

        if (authorization == null || authorization.isEmpty()) {
            throw new RestAuthException(
                    String.format("%s : no authorization data.", msgPfx));
        }

        final String[] arrayUidPw = this.getUserPasswordFromAuth(authorization);

        final String userid = arrayUidPw[0];
        final String uuid = arrayUidPw[1];

        final UUID uuidObj;
        try {
            uuidObj = UUID.fromString(uuid);
        } catch (Exception e) {
            throw new RestAuthException(String
                    .format("%s : user [%s] UUID invalid.", msgPfx, userid));
        }

        final User user = USER_DAO.findActiveUserByUserId(userid);

        if (user == null) {
            throw new RestAuthException(
                    String.format("%s : user [%s] not found.", msgPfx, userid));
        }

        if (!USER_SERVICE.isUserUuidPresent(user, uuidObj)) {
            throw new RestAuthException(String
                    .format("%s : user [%s] UUID not found.", msgPfx, userid));
        }

        return user;
    }

}
