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
package org.savapage.server.pages;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.savapage.core.dao.helpers.AccountTrxPagerReq;
import org.savapage.core.dao.helpers.AccountTrxTypeEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.SpSession;

/**
 *
 * @author Datraverse B.V.
 */
public final class AccountTrxBase extends AbstractAuthPage {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public AccountTrxBase() {

        if (isAuthErrorHandled()) {
            return;
        }

        /**
         * If this page is displayed in the Admin WebApp context, we check the
         * admin authentication (including the need for a valid Membership).
         */
        if (isAdminRoleContext()) {
            checkAdminAuthorization();
        }

        handlePage();

    }

    @Override
    protected boolean needMembership() {
        return isAdminRoleContext();
    }

    /**
     *
     * @param em
     */
    private void handlePage() {

        final String data = getParmValue(POST_PARM_DATA);

        AccountTrxPagerReq req = AccountTrxPagerReq.read(data);

        Long userId = null;

        /*
         * isAdminWebAppContext() sometimes returns null. Why !?
         */
        final boolean adminWebApp = isAdminRoleContext();

        boolean userNameVisible = false;

        if (adminWebApp) {

            userId = req.getSelect().getUserId();
            userNameVisible = (userId != null);

        } else {
            /*
             * If we are called in a User WebApp context we ALWAYS use the user
             * of the current session.
             */
            userId = SpSession.get().getUser().getId();
        }

        //
        String userName = null;

        if (userNameVisible) {
            final User user =
                    ServiceContext.getDaoContext().getUserDao()
                            .findById(userId);
            userName = user.getUserId();
        }

        //
        Label hiddenLabel = new Label("hidden-user-id");
        String hiddenValue = "";

        if (userId != null) {
            hiddenValue = userId.toString();
        }
        hiddenLabel.add(new AttributeModifier("value", hiddenValue));
        add(hiddenLabel);

        //
        MarkupHelper helper = new MarkupHelper(this);
        helper.addModifyLabelAttr("accounttrx-select-type-initial", "value",
                AccountTrxTypeEnum.INITIAL.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-adjust", "value",
                AccountTrxTypeEnum.ADJUST.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-deposit", "value",
                AccountTrxTypeEnum.DEPOSIT.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-voucher", "value",
                AccountTrxTypeEnum.VOUCHER.toString());
        helper.addModifyLabelAttr("accounttrx-select-type-printout", "value",
                AccountTrxTypeEnum.PRINT_OUT.toString());

        /*
         *
         */
        final boolean isVisible = userNameVisible;
        add(new Label("select-and-sort-user", userName) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isVisible;
            }
        });
    }

}
