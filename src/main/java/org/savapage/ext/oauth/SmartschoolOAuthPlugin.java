/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.ext.oauth;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.savapage.ext.ServerPluginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SmartschoolOAuthPlugin implements OAuthClientPlugin {

    /**
     * The {@link Logger}.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SmartschoolOAuthPlugin.class);

    /**
     * Property key prefix.
     */
    private static final String PROP_KEY_PFX = "smartschool.";

    /**
     * .
     */
    private static final String PROP_KEY_ACCOUNT = PROP_KEY_PFX + "account";

    /**
     * OAuth property key prefix.
     */
    private static final String PROP_KEY_OAUTH_PFX = PROP_KEY_PFX + "oauth.";

    /**
     * .
     */
    private static final String PROP_KEY_OAUTH_CLIENT_ID =
            PROP_KEY_OAUTH_PFX + "client-id";

    /**
     *
     */
    private static final String PROP_KEY_OAUTH_CLIENT_SECRET =
            PROP_KEY_OAUTH_PFX + "client-secret";

    /**
    *
    */
    private static final String PROP_KEY_OAUTH_CALLBACK_URL =
            PROP_KEY_OAUTH_PFX + "callback-url";

    /**
     * OAuth scope.
     */
    private static final String SMARTSCHOOL_OAUTH_SCOPE = "userinfo";

    /**
     *
     */
    private static final String URL_PARM_CODE = "code";

    /**
    *
    */
    private static final String PROTECTED_RESOURCE_URL =
            "https://oauth.smartschool.be/Api/V1";

    /**
     * The plug-in properties..
     */
    private Properties properties;

    /**
     *
     */
    private String id;

    /**
     *
     */
    private String name;

    /**
     * The singleton {@link OAuth20Service}.
     */
    private OAuth20Service oauthService;

    /**
     * URL of OAuth provider where users authorize SavaPage to do OAuth calls.
     */
    private URL authorizationUrl;

    /**
     * URL the OAuth provider should redirect after authorization.
     */
    private URL callbackUrl;

    @Override
    public OAuthProviderEnum getProvider() {
        return OAuthProviderEnum.SMARTSCHOOL;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public void onInit(final String pluginId, final String pluginName,
            final boolean live, final boolean online, final Properties props)
            throws ServerPluginException {

        this.id = pluginId;
        this.name = pluginName;
        this.properties = props;

        final String secretState = String.format("%s%d",
                this.getClass().getSimpleName(), new Random().nextInt(999_999));

        this.oauthService = new ServiceBuilder()
                .apiKey(props.getProperty(PROP_KEY_OAUTH_CLIENT_ID))
                .apiSecret(props.getProperty(PROP_KEY_OAUTH_CLIENT_SECRET))
                .callback(props.getProperty(PROP_KEY_OAUTH_CALLBACK_URL))
                .scope(SMARTSCHOOL_OAUTH_SCOPE)
                //
                .state(secretState)
                //
                .build(new SmartschoolOAuthApi(
                        props.getProperty(PROP_KEY_ACCOUNT)));

        try {
            this.callbackUrl =
                    new URL(props.getProperty(PROP_KEY_OAUTH_CALLBACK_URL));

            this.authorizationUrl =
                    new URL(this.oauthService.getAuthorizationUrl());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public URL getAuthorizationUrl() {
        return this.authorizationUrl;
    }

    @Override
    public URL getCallbackUrl() {
        return this.callbackUrl;
    }

    /**
     * Get content from URL.
     *
     * @param url
     *            The URL.
     * @return The content of the URL.
     * @throws IOException
     *             When IO errors.
     */
    private static String getContent(final URL url) throws IOException {

        final InputStreamReader in = new InputStreamReader(url.openStream());
        final StringWriter out = new StringWriter();

        int aChar = in.read();
        while (aChar != -1) {
            out.write(aChar);
            aChar = in.read();
        }
        return out.toString();
    }

    @Override
    public OAuthUserInfo onCallBack(final Map<String, String> parameterMap)
            throws IOException, OAuthPluginException {

        if (!parameterMap.containsKey(URL_PARM_CODE)) {
            return null;
        }

        final String code = parameterMap.get(URL_PARM_CODE);

        try {
            /*
             * Get the Access Token. Since this is just a quick peek at the
             * username, a refresh token is not relevant.
             */
            final OAuth2AccessToken token = oauthService.getAccessToken(code);
            final String accessToken = token.getAccessToken();

            /*
             * Ask for a protected resource.
             */

            final String apiUrl = String.format("%s/%s" + "?access_token=%s",
                    PROTECTED_RESOURCE_URL, SMARTSCHOOL_OAUTH_SCOPE,
                    accessToken);
            final String json = getContent(new URL(apiUrl));

            LOGGER.trace(json);

            final SmartschoolOAuthPayload payload =
                    SmartschoolOAuthPayload.create(json);

            final OAuthUserInfo userInfo = new OAuthUserInfo();
            userInfo.setUserId(payload.getUsername());

            if (userInfo.getUserId() == null) {
                LOGGER.error(String.format("No username found:\n%s", json));
                return null;
            }

            return userInfo;

        } catch (InterruptedException e) {
            LOGGER.warn(e.getMessage());
        } catch (ExecutionException e) {
            throw new OAuthPluginException(e.getMessage());
        }

        return null;
    }

}
