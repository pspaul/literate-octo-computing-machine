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

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppCountExceededMsg extends AbstractWebAppMsg {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppCountExceededMsg(final PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected WebAppTypeEnum getDisplayInfo(final PageParameters parameters,
            final WebAppTypeEnum webAppTypeAuth,
            final WebAppTypeEnum webAppTypeRequested,
            final MutableObject<String> messageObj,
            final MutableObject<String> messageCssObj,
            final MutableObject<String> remedyObj) {

        final String message;
        final String messageCss;
        final String remedy;

        final WebAppTypeEnum webAppTypeLogin;

        if (webAppTypeAuth == null
                || webAppTypeAuth == WebAppTypeEnum.UNDEFINED) {

            webAppTypeLogin = WebAppTypeEnum.USER;
            message = localized("message-login", webAppTypeLogin);
            messageCss = MarkupHelper.CSS_TXT_VALID;
            remedy = null;

        } else if (webAppTypeRequested == WebAppTypeEnum.UNDEFINED) {

            webAppTypeLogin = null;
            message = localized("message-unknown", webAppTypeAuth.getUiText());
            messageCss = MarkupHelper.CSS_TXT_WARN;
            remedy = null;

        } else if (webAppTypeAuth == webAppTypeRequested) {

            webAppTypeLogin = webAppTypeRequested;

            message =
                    localized("message-same", webAppTypeRequested.getUiText());
            messageCss = MarkupHelper.CSS_TXT_WARN;
            remedy = localized("remedy-same");

        } else {

            webAppTypeLogin = webAppTypeRequested;
            message = localized("message-switch",
                    webAppTypeRequested.getUiText());
            messageCss = MarkupHelper.CSS_TXT_VALID;

            if (webAppTypeAuth == null
                    || webAppTypeAuth == WebAppTypeEnum.UNDEFINED) {
                remedy = null;
            } else {
                remedy = localized("remedy-switch", webAppTypeAuth.getUiText());
            }
        }

        messageObj.setValue(message);
        messageCssObj.setValue(messageCss);
        remedyObj.setValue(remedy);

        return webAppTypeLogin;
    }

}
