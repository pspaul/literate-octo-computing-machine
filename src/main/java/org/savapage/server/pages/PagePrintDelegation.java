/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.server.pages;

import org.savapage.core.dto.PrintDelegationDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PagePrintDelegation extends AbstractPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public PagePrintDelegation() {

        //
        final MarkupHelper helper = new MarkupHelper(this);

        //
        helper.addModifyLabelAttr("radio-account-group",
                "value",
                PrintDelegationDto.DelegatorAccountEnum.GROUP.toString());

        helper.addModifyLabelAttr("radio-account-user",
                "value",
                PrintDelegationDto.DelegatorAccountEnum.USER.toString());

        helper.addModifyLabelAttr("radio-account-shared",
                "value",
                PrintDelegationDto.DelegatorAccountEnum.SHARED.toString());

        //
        final QuickSearchPanel panel =
                new QuickSearchPanel("quicksearch-user-groups");
        add(panel);
        panel.populate("sp-print-delegation-groups-select-to-add",
                getLocalizer().getString("label-groups", this), "");

        //
        final QuickSearchPanel panelUsers =
                new QuickSearchPanel("quicksearch-users");
        add(panelUsers);

        panelUsers.populate("sp-print-delegation-users-select-to-add",
                getLocalizer().getString("label-users", this), "");

        //
        final QuickSearchPanel panelAccounts =
                new QuickSearchPanel("quicksearch-shared-account");
        add(panelAccounts);

        panelAccounts.populate("sp-print-delegation-select-shared-account", "",
                "");
    }

}
