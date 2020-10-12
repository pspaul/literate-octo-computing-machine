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
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.pages.LibreJsLicenseEnum;
import org.savapage.server.pages.MessageContent;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppJobTickets extends AbstractWebAppPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppJobTickets(final PageParameters parameters) {

        super(parameters);

        checkInternetAccess(IConfigProp.Key.WEBAPP_INTERNET_JOBTICKETS_ENABLE);

        if (isWebAppCountExceeded(parameters)) {
            this.setWebAppCountExceededResponse();
            return;
        }

        /*
         * We need the printer cache.
         */
        try {
            PROXYPRINT_SERVICE.lazyInitPrinterCache();
        } catch (IppConnectException | IppSyntaxException e) {
            setResponsePage(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));
            return;
        }

        final String appTitle = getWebAppTitle(
                getLocalizer().getString("webapp-title-suffix", this));

        add(new Label("app-title", appTitle));

        addZeroPagePanel(WebAppTypeEnum.JOBTICKETS);
        addFileDownloadApiPanel();
        //
        this.addLibreJsLicensePanel("librjs-license-page");
    }

    @Override
    protected boolean isJqueryCoreRenderedByWicket() {
        return true;
    }

    @Override
    protected WebAppTypeEnum getWebAppType() {
        return WebAppTypeEnum.JOBTICKETS;
    }

    @Override
    protected void appendWebAppTypeJsFiles(
            final List<Pair<String, LibreJsLicenseEnum>> list,
            final String nocache) {

        list.add(
                new ImmutablePair<>(
                        String.format("%s%s",
                                "jquery.savapage-page-jobtickets.js", nocache),
                        SAVAPAGE_JS_LICENSE));

        list.add(new ImmutablePair<>(
                String.format("%s%s", getSpecializedJsFileName(), nocache),
                SAVAPAGE_JS_LICENSE));
    }

    @Override
    protected String getSpecializedCssFileName() {
        return "jquery.savapage-jobtickets.css";
    }

    @Override
    protected String getSpecializedJsFileName() {
        return "jquery.savapage-jobtickets.js";
    }

    @Override
    protected Set<JavaScriptLibrary> getJavaScriptToRender() {
        final EnumSet<JavaScriptLibrary> libs = EnumSet
                .of(JavaScriptLibrary.MOBIPICK, JavaScriptLibrary.SPARKLINE);
        return libs;
    }

}
