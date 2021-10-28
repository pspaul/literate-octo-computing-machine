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
package org.savapage.server;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.server.webapp.ZeroPagePanel;

/**
 * URL parameters on Main Web App.
 *
 * @author Rijk Ravestein
 *
 */
public enum WebAppParmEnum {

    /**
     * The Web App Type.
     */
    SP_APP(WebAppParmEnum.PFX + "app"),

    /**
     * Country (locale).
     */
    SP_CTRY(WebAppParmEnum.PFX + "ctry"),

    /**
     * Language (locale).
     */
    SP_LANG(WebAppParmEnum.PFX + "lang"),

    /**
     * JavaScript log level.
     */
    SP_LOG(WebAppParmEnum.PFX + "log"),

    /**
     * .
     */
    SP_LOGIN(WebAppParmEnum.PFX + "login"),

    /**
     * .
     */
    SP_LOGIN_LOCAL(WebAppParmEnum.PFX + "login-local"),

    /**
     * Internal indicator that login with OAuth occurred.
     */
    SP_LOGIN_OAUTH(WebAppParmEnum.PFX + "login-oauth"),

    /**
     * Used in callback from OAuth provider.
     */
    SP_OAUTH(WebAppParmEnum.PFX + "oauth"),

    /**
     * Used in callback from OAuth provider.
     */
    SP_OAUTH_ID(WebAppParmEnum.PFX + "oauth-id"),

    /**
     * The initial view : main | print | pdf.
     */
    SP_SHOW(WebAppParmEnum.PFX + "show"),

    /**
     * .
     */
    SP_USER(WebAppParmEnum.PFX + "user"),

    /**
     * A {@link PageParameters} value indicating that a submit was encountered
     * on the {@link ZeroPagePanel}.
     */
    SP_ZERO(WebAppParmEnum.PFX + "zero"),

    /**
     * General purpose parameter #1.
     */
    SP_PARM_1(WebAppParmEnum.PFX + "p1");

    /**
     * The prefix for SavaPage URL parameters.
     */
    private static final String PFX = "sp-";

    /**
     * .
     */
    private final String pageParm;

    /** */
    public static final String URL_PARM_SHOW_PDF = "pdf";
    /** */
    public static final String URL_PARM_SHOW_PRINT = "print";
    /** */
    public static final String URL_PARM_SHOW_USER = "user";

    /**
     * Constructor.
     *
     * @param parm
     *            The unique URL page parameter.
     */
    WebAppParmEnum(final String parm) {
        this.pageParm = parm;
    }

    /**
     * @return The URL parameter name.
     */
    public String parm() {
        return this.pageParm;
    }

    /**
     * @return The prefix for SavaPage URL parameters.
     */
    public static String parmPrefix() {
        return PFX;
    }

}
