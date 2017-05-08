/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.api.request;

import java.io.IOException;

import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.ext.oauth.OAuthClientPlugin;
import org.savapage.ext.oauth.OAuthProviderEnum;
import org.savapage.server.WebApp;
import org.savapage.server.ext.ServerPluginManager;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Wrapper for {@link OAuthClientPlugin#getAuthorizationUrl()}.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqOAuthUrl extends ApiRequestMixin {

    /**
     * .
     */
    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private OAuthProviderEnum provider;

        public OAuthProviderEnum getProvider() {
            return provider;
        }

        @SuppressWarnings("unused")
        public void setProvider(OAuthProviderEnum provider) {
            this.provider = provider;
        }

    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoRsp extends AbstractDto {

        private String url;

        @SuppressWarnings("unused")
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValueDto());

        final ServerPluginManager pluginManager =
                WebApp.get().getPluginManager();

        final OAuthClientPlugin plugin =
                pluginManager.getOAuthClient(dtoReq.getProvider());

        if (plugin == null) {
            setApiResultText(ApiResultCodeEnum.ERROR,
                    String.format("%s plug-in not found.",
                            dtoReq.getProvider().uiText()));
            return;
        }

        final DtoRsp rsp = new DtoRsp();
        rsp.setUrl(plugin.getAuthorizationUrl().toString());

        setResponse(rsp);
        setApiResultOk();
    }

}
