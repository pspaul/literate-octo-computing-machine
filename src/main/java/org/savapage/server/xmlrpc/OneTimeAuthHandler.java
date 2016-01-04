/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.server.xmlrpc;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.crypto.OneTimeAuthToken;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class OneTimeAuthHandler {

    /**
     * @param ttpKey
     *            The Trusted Third-Party key.
     * @param userid
     *            The user id.
     * @return The one-time user authentication token or {@code null} when
     *         access is denied (because TTP Web Login is disabled or TTP Key is
     *         invalid).
     */
    public String createToken(final String ttpKey, final String userid) {

        final ConfigManager cm = ConfigManager.instance();

        if (!cm.isConfigValue(Key.WEB_LOGIN_TTP_ENABLE)) {
            return null;
        }

        final String ttpKeyConfig =
                cm.getConfigValue(Key.WEB_LOGIN_TTP_API_KEY);

        if (StringUtils.isBlank(ttpKeyConfig) || !ttpKeyConfig.equals(ttpKey)) {
            return null;
        }

        return OneTimeAuthToken.createToken(userid);
    }

}
