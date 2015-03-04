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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Singleton manager of Client App User IP Address Authentications Tokens.
 *
 * @author Datraverse B.V.
 *
 */
public final class ClientAppUserAuthManager {

    /**
     * Client App dictionary of {@link UserAuthToken} objects with IP address
     * key.
     */
    private final ConcurrentMap<String, UserAuthToken> authTokenByIpAddr =
            new ConcurrentHashMap<>();

    /**
     *
     */
    private ClientAppUserAuthManager() {

    }

    /**
     * The SingletonHolder.
     */
    private static class SingletonHolder {
        /**
         * The singleton.
         */
        public static final ClientAppUserAuthManager INSTANCE =
                new ClientAppUserAuthManager();
    }

    /**
     * @return the singleton instance.
     */
    private static ClientAppUserAuthManager instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Gets the Client App authentication token object of the client IP address.
     *
     * @param ipAddress
     *            The IP address.
     * @return {@code null} when token is NOT found.
     */
    public static UserAuthToken getIpAuthToken(final String ipAddress) {
        return instance().authTokenByIpAddr.get(ipAddress);
    }

    /**
     * Adds a new token to the cache, replacing an old token object of the same
     * IP address.
     *
     * @param ipAddress
     *            The IP address.
     * @param token
     *            The {@link UserAuthToken}.
     * @return the old token or {@code null} when no old token was replaced.
     */
    public static UserAuthToken putIpAuthToken(final String ipAddress,
            final UserAuthToken token) {
        return instance().authTokenByIpAddr.put(ipAddress, token);
    }

    /**
     * Replaces an existing token in the cache. If the token does not exist it
     * is not added.
     *
     * @param ipAddress
     *            The IP address.
     * @param token
     *            The {@link UserAuthToken}.
     * @return the old token or {@code null} when no old token was present.
     */
    public static UserAuthToken replaceIpAuthToken(final String ipAddress,
            final UserAuthToken token) {
        return instance().authTokenByIpAddr.replace(ipAddress, token);
    }

    /**
     * Removes a token from the cache.
     *
     * @param ipAddress
     *            The IP address.
     * @return the removed token or {@code null} when not found.
     */
    public static UserAuthToken removeUserAuthToken(final String ipAddress) {
        return instance().authTokenByIpAddr.remove(ipAddress);
    }

}
