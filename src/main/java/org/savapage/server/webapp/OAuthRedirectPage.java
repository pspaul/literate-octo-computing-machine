/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.webapp;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.pages.RedirectPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.ext.oauth.OAuthClientPlugin;
import org.savapage.ext.oauth.OAuthProviderEnum;
import org.savapage.server.WebApp;
import org.savapage.server.ext.ServerPluginManager;

/**
 * Page that redirects to OAuth provider.
 *
 * @author Rijk Ravestein
 *
 */
public final class OAuthRedirectPage extends RedirectPage {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * @param url
     *            The redirect URL.
     */
    private OAuthRedirectPage(final CharSequence url) {
        super(url);
    }

    /**
     * @param parms
     *            The page parameters.
     */
    public OAuthRedirectPage(final PageParameters parms) {
        this(getURL(parms));
    }

    /**
     *
     * @param parms
     *            The page parameters.
     * @return The redirect URL.
     */
    private static CharSequence getURL(final PageParameters parms) {

        String redirectUrl = null;

        final ServerPluginManager pluginManager =
                WebApp.get().getPluginManager();

        final OAuthProviderEnum provider =
                EnumUtils.getEnum(OAuthProviderEnum.class, StringUtils
                        .defaultString(parms.get(0).toString()).toUpperCase());

        if (provider != null) {
            final OAuthClientPlugin plugin =
                    pluginManager.getOAuthClient(provider);
            if (plugin != null) {
                redirectUrl = plugin.getAuthorizationUrl().toString();
            }
        }
        if (redirectUrl == null) {
            return WebApp.MOUNT_PATH_WEBAPP_USER;
        }
        return redirectUrl;
    }

}
