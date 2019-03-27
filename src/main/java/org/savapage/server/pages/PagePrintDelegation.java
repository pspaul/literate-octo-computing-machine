/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dto.PrintDelegationDto;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.session.SpSession;

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

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

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

            helper.addModifyLabelAttr("radio-account-group",
                    MarkupHelper.ATTR_VALUE,
                    PrintDelegationDto.DelegatorAccountEnum.GROUP.toString());

            helper.addModifyLabelAttr("sp-label-invoicing-group",
                    localized("sp-label-invoicing-group"),
                    MarkupHelper.ATTR_TITLE,
                    localized("sp-label-invoicing-group-tooltip"));

        } else {
            helper.discloseLabel("radio-account-group");
        }

        if (cm.isConfigValue(Key.PROXY_PRINT_DELEGATE_ACCOUNT_USER_ENABLE)) {

            helper.addModifyLabelAttr("radio-account-user",
                    MarkupHelper.ATTR_VALUE,
                    PrintDelegationDto.DelegatorAccountEnum.USER.toString());

            helper.addModifyLabelAttr("sp-label-invoicing-user",
                    localized("sp-label-invoicing-user"),
                    MarkupHelper.ATTR_TITLE,
                    localized("sp-label-invoicing-user-tooltip"));
        } else {
            helper.discloseLabel("radio-account-user");
        }

        helper.encloseLabel("member-copies", txtCopies, cm.isConfigValue(
                Key.PROXY_PRINT_DELEGATE_MULTIPLE_MEMBER_COPIES_ENABLE));

        if (cm.isConfigValue(Key.PROXY_PRINT_DELEGATE_ACCOUNT_SHARED_ENABLE)
                && ACCESS_CONTROL_SERVICE
                        .hasSharedAccountAccess(SpSession.get().getUser())) {

            helper.addModifyLabelAttr("radio-account-shared",
                    MarkupHelper.ATTR_VALUE,
                    PrintDelegationDto.DelegatorAccountEnum.SHARED.toString());

            helper.addModifyLabelAttr("sp-label-invoicing-shared",
                    localized("sp-label-invoicing-shared"),
                    MarkupHelper.ATTR_TITLE,
                    localized("sp-label-invoicing-shared-tooltip"));

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
        helper.encloseLabel("button-add-popup", HtmlButtonEnum.DOTTED_SUFFIX,
                cm.isConfigValue(Key.PROXY_PRINT_DELEGATE_GROUP_COPIES_ENABLE));

        helper.addButton("button-popup-cancel", HtmlButtonEnum.CANCEL);

        //
        final DaoContext ctx = ServiceContext.getDaoContext();
        User dbUser = null;
        String scopeHtmlId, scopeHtmlTitle;

        final QuickSearchPanel panel =
                new QuickSearchPanel("quicksearch-user-groups");
        add(panel);

        if (cm.isConfigValue(
                Key.PROXY_PRINT_DELEGATE_GROUPS_PREFERRED_ENABLE)) {

            scopeHtmlId = "sp-print-delegation-groups-select-to-add-scope";
            scopeHtmlTitle = NounEnum.PREFERRED_LIST.uiText(getLocale());

            if (dbUser == null) {
                dbUser = startUserTrx(ctx);
            }
            USER_SERVICE.prunePreferredDelegateGroups(dbUser);

        } else {
            scopeHtmlId = null;
            scopeHtmlTitle = null;
        }

        panel.populate("sp-print-delegation-groups-select-to-add",
                getLocalizer().getString("label-groups", this), "", true,
                scopeHtmlId, scopeHtmlTitle);

        //
        final QuickSearchPanel panelUsers =
                new QuickSearchPanel("quicksearch-users");
        add(panelUsers);

        scopeHtmlId = "sp-print-delegation-users-select-to-add-all";
        scopeHtmlTitle = localized("button-select-all");

        panelUsers.populate("sp-print-delegation-users-select-to-add",
                getLocalizer().getString("label-users", this), "", true,
                scopeHtmlId, scopeHtmlTitle);

        //
        final QuickSearchPanel panelAccounts =
                new QuickSearchPanel("quicksearch-shared-account");
        add(panelAccounts);

        if (cm.isConfigValue(
                Key.PROXY_PRINT_DELEGATE_ACCOUNTS_PREFERRED_ENABLE)) {

            scopeHtmlId = "sp-print-delegation-select-shared-account-scope";
            scopeHtmlTitle = NounEnum.PREFERRED_LIST.uiText(getLocale());

            if (dbUser == null) {
                dbUser = startUserTrx(ctx);
            }
            USER_SERVICE.prunePreferredDelegateAccounts(dbUser);

        } else {
            scopeHtmlId = null;
            scopeHtmlTitle = null;
        }

        if (ctx.isTransactionActive()) {
            ctx.commit();
        }

        panelAccounts.populate("sp-print-delegation-select-shared-account",
                PrintOutNounEnum.ACCOUNT.uiText(getLocale()), "", true,
                scopeHtmlId, scopeHtmlTitle);
    }

    /**
     * Starts a transaction and find the user.
     *
     * @param ctx
     *            The trx context.
     * @return The user
     */
    private User startUserTrx(final DaoContext ctx) {
        final User dbUser = ServiceContext.getDaoContext().getUserDao()
                .findById(SpSession.get().getUser().getId());
        ctx.beginTransaction();
        return dbUser;
    }
}
