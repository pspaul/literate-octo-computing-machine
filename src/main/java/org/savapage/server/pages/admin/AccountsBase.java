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
package org.savapage.server.pages.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.SystemStatusEnum;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.i18n.PrintOutAdjectiveEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountsBase extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final String WICKET_ID_BUTTON_NEW = "button-new";

    /** */
    private static final String WICKET_ID_TXT_NOT_READY =
            "warn-not-ready-to-use";

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public AccountsBase(final PageParameters parameters) {

        super(parameters, ACLOidEnum.A_ACCOUNTS, RequiredPermission.READ);

        final MarkupHelper helper = new MarkupHelper(this);

        //
        helper.addLabel("title", ACLOidEnum.A_ACCOUNTS.uiText(getLocale()));
        helper.addLabel("select-and-sort", PhraseEnum.SELECT_AND_SORT);
        helper.addLabel("prompt-name-containing-text", NounEnum.NAME);

        //
        helper.addLabel("prompt-account-deleted", AdverbEnum.DELETED);
        helper.addLabel("account-deleted-n", AdjectiveEnum.ACTIVE);
        helper.addLabel("account-deleted-y", AdverbEnum.DELETED);

        helper.addLabel("prompt-sort-by", NounEnum.SORTING);
        helper.addLabel("sort-asc", AdjectiveEnum.ASCENDING);
        helper.addLabel("sort-desc", AdjectiveEnum.DESCENDING);

        helper.addButton("button-apply", HtmlButtonEnum.APPLY);
        helper.addButton("button-default", HtmlButtonEnum.DEFAULT);

        //
        helper.addLabel("prompt-account-type", NounEnum.TYPE);
        helper.addLabel("account-type-group-label", NounEnum.GROUP);
        helper.addLabel("account-type-shared-label",
                PrintOutAdjectiveEnum.SHARED.uiText(getLocale()));

        helper.addModifyLabelAttr("account-type-group", MarkupHelper.ATTR_VALUE,
                AccountTypeEnum.GROUP.toString());

        helper.addModifyLabelAttr("account-type-shared",
                MarkupHelper.ATTR_VALUE, AccountTypeEnum.SHARED.toString());

        if (ConfigManager.instance()
                .getSystemStatus() == SystemStatusEnum.SETUP) {

            helper.discloseLabel(WICKET_ID_BUTTON_NEW);
            helper.encloseLabel(WICKET_ID_TXT_NOT_READY,
                    localized("warn-not-ready-to-use"), true);

        } else {
            helper.encloseLabel(WICKET_ID_BUTTON_NEW,
                    HtmlButtonEnum.ADD.uiText(getLocale()),
                    ACCESS_CONTROL_SERVICE.hasPermission(
                            SpSession.get().getUserIdDto(),
                            ACLOidEnum.A_ACCOUNTS, ACLPermissionEnum.EDITOR));
            helper.discloseLabel(WICKET_ID_TXT_NOT_READY);
        }

        /*
         * Reserved for future use.
         */
        // add(new JrExportFileExtButtonPanel("report-button-panel",
        // "sp-btn-accounts-report"));
    }
}
