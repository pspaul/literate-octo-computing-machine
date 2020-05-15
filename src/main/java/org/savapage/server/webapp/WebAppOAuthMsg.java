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

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    public static final WebAppParmEnum PARM_STATUS = WebAppParmEnum.SP_PARM_1;

    /** */
    public static final String PARM_STATUS_WARNING = "w";

    /** */
    public static final String PARM_STATUS_ERROR = "e";

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

        final StringValue status = parameters.get(PARM_STATUS.parm());

        if (!status.isEmpty() && status.toString().equals(PARM_STATUS_ERROR)) {
            messageCssObj.setValue(MarkupHelper.CSS_TXT_ERROR);
            messageObj.setValue(localized("error-header", providerUI));
            remedyObj.setValue(localized("error-msg"));
        } else {
            messageCssObj.setValue(MarkupHelper.CSS_TXT_WARN);
            messageObj.setValue(localized("warning-header", providerUI));
            remedyObj.setValue(localized("warning-msg"));
        }

        return webAppTypeRequested;
    }

}
