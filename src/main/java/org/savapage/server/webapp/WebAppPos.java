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
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.server.pages.LibreJsLicenseEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppPos extends AbstractWebAppPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppPos(final PageParameters parameters) {

        super(parameters);

        checkInternetAccess(IConfigProp.Key.WEBAPP_INTERNET_POS_ENABLE);

        if (isWebAppCountExceeded(parameters)) {
            this.setWebAppCountExceededResponse();
            return;
        }

        final String appTitle = getWebAppTitle(
                getLocalizer().getString("webapp-title-suffix", this));

        addZeroPagePanel(WebAppTypeEnum.POS);

        add(new Label("app-title", appTitle));

        addFileDownloadApiPanel();
        //
        this.addLibreJsLicensePanel("librjs-license-page");
    }

    @Override
    boolean isJqueryCoreRenderedByWicket() {
        return true;
    }

    @Override
    protected WebAppTypeEnum getWebAppType() {
        return WebAppTypeEnum.POS;
    }

    @Override
    protected void appendWebAppTypeJsFiles(
            final List<Pair<String, LibreJsLicenseEnum>> list,
            final String nocache) {

        list.add(new ImmutablePair<>(
                String.format("%s%s", "jquery.savapage-page-pos.js", nocache),
                SAVAPAGE_JS_LICENSE));

        list.add(new ImmutablePair<>(
                String.format("%s%s", getSpecializedJsFileName(), nocache),
                SAVAPAGE_JS_LICENSE));
    }

    @Override
    protected String getSpecializedCssFileName() {
        return "jquery.savapage-pos.css";
    }

    @Override
    protected String getSpecializedJsFileName() {
        return "jquery.savapage-pos.js";
    }

    @Override
    protected Set<JavaScriptLibrary> getJavaScriptToRender() {
        final EnumSet<JavaScriptLibrary> libs =
                EnumSet.noneOf(JavaScriptLibrary.class);
        return libs;
    }

}
