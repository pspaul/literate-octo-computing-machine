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

import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.server.WebApp;
import org.savapage.server.WebAppParmEnum;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractWebAppMsg extends AbstractWebAppPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public AbstractWebAppMsg(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("app-title", getWebAppTitle(null));
        helper.addLabel("title", CommunityDictEnum.SAVAPAGE.getWord());

        final MutableObject<String> messageObj = new MutableObject<>();
        final MutableObject<String> messageCssObj = new MutableObject<>();
        final MutableObject<String> remedyObj = new MutableObject<>();

        final WebAppTypeEnum webAppTypeAuth = SpSession.get().getWebAppType();

        final WebAppTypeEnum webAppTypeRequested =
                EnumUtils.getEnum(WebAppTypeEnum.class,
                        parameters.get(WebAppParmEnum.SP_APP.parm())
                                .toString(WebAppTypeEnum.UNDEFINED.toString()));

        final WebAppTypeEnum webAppTypeLogin =
                getDisplayInfo(parameters, webAppTypeAuth, webAppTypeRequested,
                        messageObj, messageCssObj, remedyObj);

        helper.addAppendLabelAttr("message", messageObj.getValue(), "class",
                messageCssObj.getValue());

        if (remedyObj.getValue() == null) {
            helper.discloseLabel("remedy");
        } else {
            helper.encloseLabel("remedy", remedyObj.getValue(), true);
        }

        if (webAppTypeLogin == null) {
            helper.discloseLabel("button-login");
        } else {
            helper.encloseLabel("button-login",
                    HtmlButtonEnum.LOGIN.uiText(getLocale()), true);
            helper.addModifyLabelAttr("sp-webapp-mountpath", "value",
                    this.getMountPathRequested(webAppTypeLogin));
        }
    }

    /**
     * Creates display info on mutable strings.
     *
     * @param parameters
     *            The page parameters.
     * @param webAppTypeAuth
     *            The current authenticated web app.
     * @param webAppTypeRequested
     *            The requested web app.
     * @param messageObj
     *            The message.
     * @param messageCssObj
     *            The CSS class of the message.
     * @param remedyObj
     *            The remedy.
     *
     * @return The WebAppTypeEnum to login again. When {@code null}, the Login
     *         button is not displayed.
     */
    protected abstract WebAppTypeEnum getDisplayInfo(PageParameters parameters,
            WebAppTypeEnum webAppTypeAuth, WebAppTypeEnum webAppTypeRequested,
            MutableObject<String> messageObj,
            MutableObject<String> messageCssObj,
            MutableObject<String> remedyObj);

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
        case PRINTSITE:
            return WebApp.MOUNT_PATH_WEBAPP_PRINTSITE;
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
    protected final boolean isJqueryCoreRenderedByWicket() {
        return false;
    }

    @Override
    protected final WebAppTypeEnum getWebAppType() {
        // Meaningless in our case.
        return null;
    }

    @Override
    protected final String getSpecializedCssFileName() {
        return null;
    }

    @Override
    protected final String getSpecializedJsFileName() {
        return "jquery.savapage-msg.js";
    }

    @Override
    protected final Set<JavaScriptLibrary> getJavaScriptToRender() {
        return EnumSet.noneOf(JavaScriptLibrary.class);
    }

    @Override
    protected final void renderWebAppTypeJsFiles(final IHeaderResponse response,
            final String nocache) {
        renderJs(response,
                String.format("%s%s", getSpecializedJsFileName(), nocache));
    }

}
