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

/**
 * User Authentication token.
 *
 * @author Datraverse B.V.
 *
 */
public final class UserAuthToken {

    private final String user;
    private final String token;
    private final boolean adminOnly;
    private final long createTime;

    @SuppressWarnings("unused")
    private UserAuthToken() {
        this.user = null;
        this.adminOnly = false;
        this.token = null;
        this.createTime = 0;
    }

    /**
     * Creates a non-admin {@link UserAuthToken}.
     *
     * @param user
     *            The user id.
     */
    public UserAuthToken(final String user) {
        this(user, false);
    }

    /**
     * Creates an {@link UserAuthToken}.
     *
     * @param user
     *            The user id.
     * @param adminOnly
     *            If {@code true} the token is for an "admin-only" context, i.e.
     *            an Admin WebApp session. If {@code false} the token is used in
     *            a regular user context, i.e. a User WebApp session.
     */
    public UserAuthToken(final String user, boolean adminOnly) {
        this.user = user;
        this.adminOnly = adminOnly;
        this.token = java.util.UUID.randomUUID().toString();
        this.createTime = new Date().getTime();
    }

    public String getToken() {
        return token;
    }

    public String getUser() {
        return user;
    }

    public boolean isAdminOnly() {
        return adminOnly;
    }

    /**
     * @return The time of creation as in {@link Date#getTime()}.
     */
    public long getCreateTime() {
        return createTime;
    }

}
