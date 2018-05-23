/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.webapp;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.ext.oauth.OAuthProviderEnum;
import org.savapage.server.WebAppParmEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 * Notifies an OAuth failure with warning message, remedy and Login (retry)
 * button).
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppOAuthMsg extends AbstractWebAppMsg {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppOAuthMsg(final PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected WebAppTypeEnum getDisplayInfo(final PageParameters parameters,
            final WebAppTypeEnum webAppTypeAuth,
            final WebAppTypeEnum webAppTypeRequested,
            final MutableObject<String> messageObj,
            final MutableObject<String> messageCssObj,
            final MutableObject<String> remedyObj) {

        final StringValue oauthProviderValue =
                parameters.get(WebAppParmEnum.SP_LOGIN_OAUTH.parm());

        String providerUI = "???";

        if (!oauthProviderValue.isEmpty()) {
            final OAuthProviderEnum oauthProvider =
                    EnumUtils.getEnum(OAuthProviderEnum.class,
                            oauthProviderValue.toString().toUpperCase());
            if (oauthProvider != null) {
                providerUI = oauthProvider.uiText();
            }
        }

        messageObj.setValue(localized("warning", providerUI));
        messageCssObj.setValue(MarkupHelper.CSS_TXT_WARN);
        remedyObj.setValue(localized("remedy"));

        return webAppTypeRequested;
    }

}
