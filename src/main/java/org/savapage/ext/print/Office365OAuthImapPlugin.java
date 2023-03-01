/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2023 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2023 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.ext.print;

import java.io.IOException;
import java.util.Properties;

import org.savapage.ext.Office365Util;
import org.savapage.ext.ServerPluginContext;
import org.savapage.ext.ServerPluginException;

/**
 * Office365 IMAP authentication Plug-in.
 *
 * @author Rijk Ravestein
 *
 */
public final class Office365OAuthImapPlugin extends OAuthImapPlugin {

    /**
     * Property key prefix.
     */
    private static final String PROP_KEY_PFX = "office365.";

    /**
     * Tenant id.
     */
    private static final String PROP_KEY_TENANT_ID = PROP_KEY_PFX + "tenant-id";

    /**
     * Mail address.
     */
    private static final String PROP_KEY_MAIL_ADDRESS =
            PROP_KEY_PFX + "mail-address";

    /**
     * Property key prefix imap.
     */
    private static final String PROP_KEY_PFX_IMAP = PROP_KEY_PFX + "imap.";

    /**
     * OAuth property key prefix oauth.
     */
    private static final String PROP_KEY_OAUTH_PFX =
            PROP_KEY_PFX_IMAP + "oauth.";

    /** */
    private static final String PROP_KEY_OAUTH_CLIENT_ID =
            PROP_KEY_OAUTH_PFX + "client-id";

    /** */
    private static final String PROP_KEY_OAUTH_CLIENT_SECRET_VALUE =
            PROP_KEY_OAUTH_PFX + "client-secret.value";

    /**
     *
     */
    private String id;

    /**
     *
     */
    private String name;

    /**
     *
     */
    private String tenantId;

    /**
    *
    */
    private String mailAddress;

    /**
     *
     */
    private String oauthClientId;

    /**
     *
     */
    private String oauthClientSecretValue;

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void onStart() throws ServerPluginException {
    }

    @Override
    public void onStop() throws ServerPluginException {
    }

    @Override
    public void onInit(final String pluginId, final String pluginName,
            final boolean live, final boolean online, final Properties props,
            final ServerPluginContext context) throws ServerPluginException {

        this.id = pluginId;
        this.name = pluginName;
        this.tenantId = props.getProperty(PROP_KEY_TENANT_ID);
        this.mailAddress = props.getProperty(PROP_KEY_MAIL_ADDRESS);
        this.oauthClientId = props.getProperty(PROP_KEY_OAUTH_CLIENT_ID);
        this.oauthClientSecretValue =
                props.getProperty(PROP_KEY_OAUTH_CLIENT_SECRET_VALUE);
    }

    @Override
    public String getMailAddress() {
        return this.mailAddress;
    }

    @Override
    public String retrieveToken() throws IOException {
        return Office365Util.retrieveOAuthTokenImap(this.tenantId,
                this.oauthClientId, this.oauthClientSecretValue);
    }

}
