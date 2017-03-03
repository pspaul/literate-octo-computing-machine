/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.server.pages.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class AccountsBase extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    private static final String WICKET_ID_BUTTON_NEW = "button-new";

    /**
     * .
     */
    private static final String WICKET_ID_TXT_NOT_READY =
            "warn-not-ready-to-use";

    /**
     *
     */
    public AccountsBase(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addModifyLabelAttr("account-type-group", "value",
                AccountTypeEnum.GROUP.toString());

        helper.addModifyLabelAttr("account-type-shared", "value",
                AccountTypeEnum.SHARED.toString());

        if (ConfigManager.instance().isAppReadyToUse()) {
            addVisible(ConfigManager.isInternalUsersEnabled(),
                    WICKET_ID_BUTTON_NEW, localized("button-new"));
            helper.discloseLabel(WICKET_ID_TXT_NOT_READY);
        } else {
            helper.discloseLabel(WICKET_ID_BUTTON_NEW);
            helper.encloseLabel(WICKET_ID_TXT_NOT_READY,
                    localized("warn-not-ready-to-use"), true);
        }

    }
}
