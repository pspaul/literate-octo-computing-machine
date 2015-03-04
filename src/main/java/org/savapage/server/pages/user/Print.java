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
package org.savapage.server.pages.user;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.QuickSearchPanel;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class Print extends AbstractUserPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public Print() {

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addModifyLabelAttr(
                "slider-print-copies",
                "max",
                ConfigManager.instance().getConfigValue(
                        Key.WEBAPP_USER_PROXY_PRINT_MAX_COPIES));

        final Label label = new Label("delete-pages-after-print");

        if (ConfigManager.instance().isConfigValue(
                Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX)) {
            label.add(new AttributeModifier("checked", "checked"));
            label.add(new AttributeModifier("disabled", "disabled"));
        }

        add(label);

        final QuickSearchPanel panel =
                new QuickSearchPanel("quicksearch-printer");

        add(panel);

        panel.populate("sp-print-qs-printer", "",
        // getLocalizer().getString("search-printer-prompt", this),
                getLocalizer().getString("search-printer-placeholder", this));
    }

}
