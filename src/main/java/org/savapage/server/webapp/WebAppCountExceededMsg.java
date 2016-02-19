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
    public static final String PARM_WEBAPPTYPE = "app";

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

        //
        final WebAppTypeEnum requestedWebAppType =
                EnumUtils.getEnum(WebAppTypeEnum.class,
                        this.getPageParameters().get(PARM_WEBAPPTYPE)
                                .toString(WebAppTypeEnum.UNDEFINED.toString()));

        final String mountPath;

        switch (requestedWebAppType) {
        case ADMIN:
            mountPath = WebApp.MOUNT_PATH_WEBAPP_ADMIN;
            break;
        case JOB_TICKETS:
            mountPath = WebApp.MOUNT_PATH_WEBAPP_JOBTICKETS;
            break;
        case POS:
            mountPath = WebApp.MOUNT_PATH_WEBAPP_POS;
            break;
        case USER:
        case UNDEFINED:
        default:
            mountPath = WebApp.MOUNT_PATH_WEBAPP_USER;
            break;
        }

        helper.addModifyLabelAttr("sp-webapp-mountpath", "value", mountPath);
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
