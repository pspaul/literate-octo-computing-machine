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

import java.util.Locale;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.server.helpers.HtmlButtonEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AppAbout extends AbstractPage {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public AppAbout(final PageParameters parameters) {

        super(parameters);

        //
        final HtmlInjectComponent inject = new HtmlInjectComponent("inject",
                this.getWebAppTypeEnum(parameters), HtmlInjectEnum.ABOUT);

        add(inject);

        final MarkupHelper helper = new MarkupHelper(this);

        final String appName =
                CommunityDictEnum.SAVAPAGE.getWord(Locale.getDefault());

        if (inject.isInjectAvailable()) {
            helper.discloseLabel("app-version");
            add(new AppAboutPanel("savapage-info-after-inject"));
            add(new Label("app-product-name", appName));
            add(new Label("app-product-slogan",
                    CommunityDictEnum.SAVAPAGE_SLOGAN.getWord()));
            add(new Label("app-product-version",
                    ConfigManager.getAppVersion()));
        } else {
            helper.discloseLabel("savapage-info-after-inject");
            helper.encloseLabel("app-version",
                    ConfigManager.getAppNameVersion(), true);
            add(new Label("app-slogan",
                    CommunityDictEnum.SAVAPAGE_SLOGAN.getWord()));
            add(new AppAboutPanel("savapage-info"));
        }

        helper.addButton("button-back", HtmlButtonEnum.BACK);

    }

}
