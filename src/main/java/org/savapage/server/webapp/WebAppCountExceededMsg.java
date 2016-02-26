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
package org.savapage.server.webapp;

import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.server.SpSession;
import org.savapage.server.WebApp;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class WebAppCountExceededMsg extends AbstractWebAppPage {

    /**
     * .
     */
    public static final String PARM_WEBAPPTYPE = "sp-app";

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

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("app-title", getWebAppTitle(null));
        helper.addLabel("title", CommunityDictEnum.SAVAPAGE.getWord());

        final WebAppTypeEnum webAppTypeAuth = SpSession.get().getWebAppType();

        final WebAppTypeEnum webAppTypeRequested = EnumUtils
                .getEnum(WebAppTypeEnum.class, parameters.get(PARM_WEBAPPTYPE)
                        .toString(WebAppTypeEnum.UNDEFINED.toString()));

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

            webAppTypeLogin = null;
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

        helper.addAppendLabelAttr("message", message, "class", messageCss);

        if (remedy == null) {
            helper.discloseLabel("remedy");
        } else {
            helper.encloseLabel("remedy", remedy, true);
        }

        if (webAppTypeLogin == null) {
            helper.discloseLabel("button-login");
        } else {
            helper.encloseLabel("button-login", localized("button-login"),
                    true);
            helper.addModifyLabelAttr("sp-webapp-mountpath", "value",
                    this.getMountPathRequested(webAppTypeLogin));
        }
    }

    /**
     *
     * @param webAppTypeRequested
     *            The requested web app.
     * @return The mount path.
     */
    private String
            getMountPathRequested(final WebAppTypeEnum webAppTypeRequested) {
        switch (webAppTypeRequested) {
        case ADMIN:
            return WebApp.MOUNT_PATH_WEBAPP_ADMIN;
        case JOBTICKETS:
            return WebApp.MOUNT_PATH_WEBAPP_JOBTICKETS;
        case POS:
            return WebApp.MOUNT_PATH_WEBAPP_POS;
        case USER:
        case UNDEFINED:
        default:
            return WebApp.MOUNT_PATH_WEBAPP_USER;
        }
    }

    @Override
    protected boolean isJqueryCoreRenderedByWicket() {
        return false;
    }

    @Override
    protected WebAppTypeEnum getWebAppType() {
        // Meaningless in our case.
        return null;
    }

    @Override
    protected String getSpecializedCssFileName() {
        return null;
    }

    @Override
    protected String getSpecializedJsFileName() {
        return "jquery.savapage-exceeded.js";
    }

    @Override
    protected Set<JavaScriptLibrary> getJavaScriptToRender() {
        return EnumSet.noneOf(JavaScriptLibrary.class);
    }

    @Override
    protected void renderWebAppTypeJsFiles(final IHeaderResponse response,
            final String nocache) {
        renderJs(response,
                String.format("%s%s", getSpecializedJsFileName(), nocache));
    }

}
