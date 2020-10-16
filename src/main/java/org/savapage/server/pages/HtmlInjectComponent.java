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
package org.savapage.server.pages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.request.Response;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.util.LocaleHelper;
import org.savapage.server.WebApp;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class HtmlInjectComponent extends WebComponent {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private final WebAppTypeEnum webAppType;

    /**
     *
     */
    private final HtmlInjectEnum htmlInject;

    /**
     *
     */
    private final File htmlFile;

    /**
     * @return The HTML {@link File}, or {@code null} when not applicable.
     */
    private String getHtmlFileName() {

        final IConfigProp.Key configKey;

        switch (this.webAppType) {
        case ADMIN:
            switch (this.htmlInject) {
            case ABOUT:
                configKey = Key.WEBAPP_HTML_ADMIN_ABOUT;
                break;
            case LOGIN:
                configKey = Key.WEBAPP_HTML_ADMIN_LOGIN;
                break;
            default:
                configKey = null;
                break;
            }
            break;
        case PRINTSITE:
            switch (this.htmlInject) {
            case ABOUT:
                configKey = Key.WEBAPP_HTML_PRINTSITE_ABOUT;
                break;
            case LOGIN:
                configKey = Key.WEBAPP_HTML_PRINTSITE_LOGIN;
                break;
            default:
                configKey = null;
                break;
            }
            break;
        case JOBTICKETS:
            switch (this.htmlInject) {
            case ABOUT:
                configKey = Key.WEBAPP_HTML_JOBTICKETS_ABOUT;
                break;
            case LOGIN:
                configKey = Key.WEBAPP_HTML_JOBTICKETS_LOGIN;
                break;
            default:
                configKey = null;
                break;
            }
            break;
        case POS:
            switch (this.htmlInject) {
            case ABOUT:
                configKey = Key.WEBAPP_HTML_POS_ABOUT;
                break;
            case LOGIN:
                configKey = Key.WEBAPP_HTML_POS_LOGIN;
                break;
            default:
                configKey = null;
                break;
            }
            break;
        case USER:
            switch (this.htmlInject) {
            case ABOUT:
                configKey = Key.WEBAPP_HTML_USER_ABOUT;
                break;
            case LOGIN:
                configKey = Key.WEBAPP_HTML_USER_LOGIN;
                break;
            default:
                configKey = null;
                break;
            }
            break;
        default:
            configKey = null;
            break;
        }

        if (configKey != null) {
            final String fileName =
                    ConfigManager.instance().getConfigValue(configKey);
            if (StringUtils.isNotBlank(fileName)) {
                return fileName;
            }
        }

        final StringBuilder key = new StringBuilder();

        key.append(Key.WEBAPP_HTML_PFX)
                .append(this.webAppType.toString().toLowerCase()).append(".")
                .append(this.htmlInject.toString().toLowerCase());

        final String fileName = WebApp.getWebProperty(key.toString());

        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        return fileName;
    }

    /**
     *
     * @param id
     *            The Wicket id.
     * @param webApp
     *            The Web App type.
     * @param inject
     *            The HTMP injection type.
     */
    public HtmlInjectComponent(final String id, final WebAppTypeEnum webApp,
            final HtmlInjectEnum inject) {
        super(id);
        this.webAppType = webApp;
        this.htmlInject = inject;

        final String htmlFileName = getHtmlFileName();

        if (htmlFileName == null) {
            this.htmlFile = null;
        } else {
            this.htmlFile = LocaleHelper.getLocaleFile(
                    Paths.get(ConfigManager.getServerCustomHtmlHome()
                            .getAbsolutePath(), htmlFileName).toFile(),
                    getLocale());
        }
    }

    /**
     *
     * @return {@code true} when inject HTML is available.
     */
    public boolean isInjectAvailable() {
        return htmlFile != null;
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream,
            final ComponentTag openTag) {

        if (this.htmlFile == null) {
            return;
        }

        final Response response = getRequestCycle().getResponse();

        try (BufferedReader br =
                new BufferedReader(new FileReader(htmlFile));) {

            String line = null;

            while ((line = br.readLine()) != null) {
                response.write(line);
            }

        } catch (IOException e) {
            response.write("<span class=\"");
            response.write(MarkupHelper.CSS_TXT_ERROR);
            response.write("\">");
            response.write(e.getMessage());
            response.write("<span>");
        }
    }

}
