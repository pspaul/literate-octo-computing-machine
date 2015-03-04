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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton manager of WebApp User Authentications Tokens.
 * <p>
 * Separate cache dictionaries for User and Admin context are maintained.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public final class WebAppUserAuthManager {

    private static final int IDX_CONTEXT_USER = 0;
    private static final int IDX_CONTEXT_ADMIN = 1;

    /**
     * WebApp User/Admin Context Dictionary of {@link UserAuthToken} objects
     * with 'token' key.
     */
    private final List<Map<String, UserAuthToken>> dictTokenAuthToken =
            new ArrayList<Map<String, UserAuthToken>>(2);

    /**
     * WebApp User Context Dictionary of {@link UserAuthToken} objects with
     * 'user' key.
     */
    private final List<Map<String, UserAuthToken>> dictUserAuthToken =
            new ArrayList<Map<String, UserAuthToken>>(2);

    /**
     *
     */
    private WebAppUserAuthManager() {

        dictTokenAuthToken.add(new HashMap<String, UserAuthToken>());
        dictTokenAuthToken.add(new HashMap<String, UserAuthToken>());

        dictUserAuthToken.add(new HashMap<String, UserAuthToken>());
        dictUserAuthToken.add(new HashMap<String, UserAuthToken>());
    }

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link WebAppUserAuthManager#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final WebAppUserAuthManager INSTANCE = new WebAppUserAuthManager();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static WebAppUserAuthManager instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     * @param isAdminContext
     * @return
     */
    private int getDictIndex(final boolean isAdminContext) {
        if (isAdminContext) {
            return IDX_CONTEXT_ADMIN;
        }
        return IDX_CONTEXT_USER;
    }

    /**
     * Gets the authentication token object from the token string.
     *
     * @param token
     *            The token string.
     * @param isAdminContext
     *            {@code true} if the token is needed in a "admin-only" context,
     *            i.e. an Admin WebApp session.
     * @return {@code null} when token is NOT found.
     */
    public synchronized UserAuthToken getUserAuthToken(final String token,
            final boolean isAdminContext) {
        return dictTokenAuthToken.get(getDictIndex(isAdminContext)).get(token);
    }

    /**
     * Gets the authentication token object of the user.
     *
     * @param user
     *            The user id.
     * @param isAdminContext
     *            {@code true} if the token is needed in a "admin-only" context,
     *            i.e. an Admin WebApp session.
     * @return {@code null} when token is NOT found.
     */
    public synchronized UserAuthToken getAuthTokenOfUser(final String user,
            final boolean isAdminContext) {
        return dictUserAuthToken.get(getDictIndex(isAdminContext)).get(user);
    }

    /**
     * Adds a new token to the cache, replacing an old token object of the same
     * user.
     *
     * @param token
     *            The token to add.
     * @param isAdminContext
     *            {@code true} if the token is needed in a "admin-only" context,
     *            i.e. an Admin WebApp session.
     * @return the old token or {@code null} when no old token was replaced.
     */
    public synchronized UserAuthToken putUserAuthToken(
            final UserAuthToken token, final boolean isAdminContext) {

        final int i = getDictIndex(isAdminContext);

        UserAuthToken oldToken = dictUserAuthToken.get(i).get(token.getUser());

        if (oldToken != null) {
            dictTokenAuthToken.get(i).remove(oldToken.getToken());
            dictUserAuthToken.get(i).remove(oldToken.getUser());
        }

        dictTokenAuthToken.get(i).put(token.getToken(), token);
        dictUserAuthToken.get(i).put(token.getUser(), token);

        return oldToken;
    }

    /**
     * Removes a token from the cache.
     *
     * @param token
     *            The token.
     * @param isAdminContext
     *            {@code true} if the token is needed in a "admin-only" context,
     *            i.e. an Admin WebApp session.
     * @return the removed token or {@code null} when not found.
     */
    public synchronized UserAuthToken removeUserAuthToken(final String token,
            final boolean isAdminContext) {

        final int i = getDictIndex(isAdminContext);

        final UserAuthToken oldToken = dictTokenAuthToken.get(i).remove(token);

        if (oldToken != null) {
            dictTokenAuthToken.get(i).remove(oldToken.getToken());
            dictUserAuthToken.get(i).remove(oldToken.getUser());
        }

        return oldToken;
    }

}
