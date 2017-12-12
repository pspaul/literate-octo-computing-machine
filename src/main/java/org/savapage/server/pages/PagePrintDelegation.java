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
package org.savapage.server.pages;

import org.apache.wicket.markup.html.basic.Label;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dto.PrintDelegationDto;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.server.helpers.HtmlButtonEnum;

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

        final String txtCopies =
                PrintOutNounEnum.COPY.uiText(getLocale(), true);
        //
        add(new Label("header-copies-1", txtCopies));
        add(new Label("header-copies-2", txtCopies));

        helper.addButton("btn-next", HtmlButtonEnum.NEXT);
        helper.addButton("button-inbox", HtmlButtonEnum.BACK);

        //
        final ConfigManager cm = ConfigManager.instance();

        if (cm.isConfigValue(Key.PROXY_PRINT_DELEGATE_ACCOUNT_GROUP_ENABLE)) {
            helper.addModifyLabelAttr("radio-account-group", "value",
                    PrintDelegationDto.DelegatorAccountEnum.GROUP.toString());
        } else {
            helper.discloseLabel("radio-account-group");
        }

        helper.addModifyLabelAttr("radio-account-user", "value",
                PrintDelegationDto.DelegatorAccountEnum.USER.toString());

        helper.encloseLabel("member-copies", txtCopies, cm.isConfigValue(
                Key.PROXY_PRINT_DELEGATE_MULTIPLE_MEMBER_COPIES_ENABLE));

        if (cm.isConfigValue(Key.PROXY_PRINT_DELEGATE_ACCOUNT_SHARED_ENABLE)) {

            helper.addModifyLabelAttr("radio-account-shared", "value",
                    PrintDelegationDto.DelegatorAccountEnum.SHARED.toString());

            helper.encloseLabel("radio-add-extra", localized("label-add-extra"),
                    true);

            helper.encloseLabel("extra-to-add", "", true);
            helper.addLabel("member-copies-2", txtCopies);

        } else {
            helper.discloseLabel("radio-account-shared");
            helper.discloseLabel("radio-add-extra");
            helper.discloseLabel("extra-to-add");
        }

        //
        final QuickSearchPanel panel =
                new QuickSearchPanel("quicksearch-user-groups");
        add(panel);
        panel.populate("sp-print-delegation-groups-select-to-add",
                getLocalizer().getString("label-groups", this), "", true);

        //
        final QuickSearchPanel panelUsers =
                new QuickSearchPanel("quicksearch-users");
        add(panelUsers);

        panelUsers.populate("sp-print-delegation-users-select-to-add",
                getLocalizer().getString("label-users", this), "", true);

        //
        final QuickSearchPanel panelAccounts =
                new QuickSearchPanel("quicksearch-shared-account");
        add(panelAccounts);

        panelAccounts.populate("sp-print-delegation-select-shared-account",
                PrintOutNounEnum.ACCOUNT.uiText(getLocale()), "", true);
    }

}
