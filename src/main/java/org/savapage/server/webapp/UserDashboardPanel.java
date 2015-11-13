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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class UserDashboardPanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The id.
     */
    public UserDashboardPanel(final String id) {
        super(id);
    }

    /**
     *
     */
    public void populate() {

        final MarkupHelper helper = new MarkupHelper(this);

        helper.encloseLabel("button-user-pw-dialog", this.getLocalizer()
                .getString("button-password", this), ConfigManager.instance()
                .isConfigValue(Key.INTERNAL_USERS_CAN_CHANGE_PW));

        helper.encloseLabel("button-user-pin-dialog", this.getLocalizer()
                .getString("button-pin", this), ConfigManager.instance()
                .isConfigValue(Key.USER_CAN_CHANGE_PIN));

        final boolean hasUriBase =
                StringUtils.isNotBlank(ConfigManager.instance().getConfigValue(
                        Key.IPP_INTERNET_PRINTER_URI_BASE));

        helper.encloseLabel("button-user-internet-printer-dialog", this
                .getLocalizer().getString("button-internet-printer", this),
                hasUriBase);

    }
}
