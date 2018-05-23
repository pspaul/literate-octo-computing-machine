/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Author: Rijk Ravestein.
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
package org.savapage.server.auth;

import java.util.Date;

import org.savapage.core.config.WebAppTypeEnum;

/**
 * User Authentication token.
 *
 * @author Datraverse B.V.
 *
 */
public final class UserAuthToken {

    private final String user;
    private final String token;
    private final WebAppTypeEnum webAppType;
    private final long createTime;

    @SuppressWarnings("unused")
    private UserAuthToken() {
        this.user = null;
        this.webAppType = WebAppTypeEnum.UNDEFINED;
        this.token = null;
        this.createTime = 0;
    }

    /**
     * Creates a {@link UserAuthToken} for a {@link WebAppTypeEnum#USER}.
     *
     * @param user
     *            The user id.
     */
    public UserAuthToken(final String user) {
        this(user, WebAppTypeEnum.USER);
    }

    /**
     * Creates an {@link UserAuthToken}.
     *
     * @param user
     *            The user id.
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     */
    public UserAuthToken(final String user, final WebAppTypeEnum webAppType) {
        this.user = user;
        this.webAppType = webAppType;
        this.token = java.util.UUID.randomUUID().toString();
        this.createTime = System.currentTimeMillis();
    }

    public String getToken() {
        return token;
    }

    public String getUser() {
        return user;
    }

    public WebAppTypeEnum getWebAppType() {
        return this.webAppType;
    }

    /**
     * @return The time of creation as in {@link Date#getTime()}.
     */
    public long getCreateTime() {
        return createTime;
    }

}
