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

import java.util.EnumSet;
import java.util.Set;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.WebAppTypeEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppPrintSite extends AbstractWebAppPage {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final String[] CSS_REQ_FILENAMES =
            new String[] { "jquery.savapage-common-icons.css" };

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppPrintSite(final PageParameters parameters) {

        super(parameters);

        checkInternetAccess(IConfigProp.Key.WEBAPP_INTERNET_PRINTSITE_ENABLE);

        if (isWebAppCountExceeded(parameters)) {
            setWebAppCountExceededResponse();
            return;
        }

        final String appTitle = getWebAppTitle(
                getLocalizer().getString("webapp-title-suffix", this));

        addZeroPagePanel(this.getWebAppType());

        add(new Label("app-title", appTitle));

        addFileDownloadApiPanel();
    }

    @Override
    protected boolean isJqueryCoreRenderedByWicket() {
        return true;
    }

    @Override
    protected WebAppTypeEnum getWebAppType() {
        return WebAppTypeEnum.PRINTSITE;
    }

    @Override
    protected void renderWebAppTypeJsFiles(final IHeaderResponse response,
            final String nocache) {

        renderJs(response, String.format("%s%s",
                "jquery.savapage-printsite-panels.js", nocache));
        renderJs(response, String.format("%s%s",
                "jquery.savapage-printsite-pages.js", nocache));
        renderJs(response,
                String.format("%s%s", getSpecializedJsFileName(), nocache));
    }

    @Override
    protected String[] getSpecializedCssReqFileNames() {
        return CSS_REQ_FILENAMES;
    }

    @Override
    protected String getSpecializedCssFileName() {
        return "jquery.savapage-printsite.css";
    }

    @Override
    protected String getSpecializedJsFileName() {
        return "jquery.savapage-printsite.js";
    }

    @Override
    protected Set<JavaScriptLibrary> getJavaScriptToRender() {
        final EnumSet<JavaScriptLibrary> libs =
                EnumSet.allOf(JavaScriptLibrary.class);
        return libs;
    }

}
