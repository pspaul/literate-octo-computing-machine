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
package org.savapage.server.ext;

import org.savapage.core.SpException;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.ext.oauth.OAuthProviderEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ServerPluginHelper {

    /**
     * Hide constructor because utility class.
     */
    private ServerPluginHelper() {

    }

    /**
     * Maps external supplier to an OAuth provider.
     *
     * @param provider
     *            The {@link OAuthProviderEnum}.
     * @return The {@link ExternalSupplierEnum}.
     */
    public static ExternalSupplierEnum
            getEnum(final OAuthProviderEnum provider) {

        switch (provider) {
        case AZURE:
            return ExternalSupplierEnum.AZURE;
        case GOOGLE:
            return ExternalSupplierEnum.GOOGLE;
        case KEYCLOAK:
            return ExternalSupplierEnum.KEYCLOAK;
        case SMARTSCHOOL:
            return ExternalSupplierEnum.SMARTSCHOOL;
        default:
            throw new SpException(
                    String.format("No mapping for %s", provider.toString()));
        }
    }

}
