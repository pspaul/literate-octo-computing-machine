/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.server.pages.admin;

import java.util.Date;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.dao.UserDao;
import org.savapage.core.jpa.AccountVoucher;
import org.savapage.core.jpa.User;
import org.savapage.core.services.AccountVoucherService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.LocaleHelper;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountVoucherItemPanel extends Panel {

    final AccountVoucherService accountVoucherService = ServiceContext
            .getServiceFactory().getAccountVoucherService();

    final Date now = new Date();

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     * @param model
     */
    public AccountVoucherItemPanel(String id) {
        super(id);
    }

    /**
     *
     * @param voucher
     */
    public void populate(final AccountVoucher voucher) {

        add(new Label("message", voucher.getCardNumber()));

        final String cssClass;
        final String statusKey;

        if (voucher.getRedeemedDate() != null) {
            statusKey = "voucher-used";
            cssClass = MarkupHelper.CSS_TXT_WARN;
        } else if (accountVoucherService.isVoucherExpired(voucher, now)) {
            statusKey = "voucher-expired";
            cssClass = MarkupHelper.CSS_TXT_ERROR;
        } else {
            statusKey = "voucher-valid";
            cssClass = MarkupHelper.CSS_TXT_VALID;
        }

        Label labelWlk = new Label("status", localized(statusKey));
        labelWlk.add(new AttributeAppender("class", cssClass));
        add(labelWlk);

        //
        final String redeemDate;
        final String redeemUser;

        if (voucher.getRedeemedDate() == null) {

            redeemDate = null;
            redeemUser = null;

        } else {

            final LocaleHelper localeHelper = new LocaleHelper(getLocale());

            final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

            final User user =
                    userDao.findByAccount(voucher.getAccountTrx().getAccount()
                            .getId());

            redeemDate =
                    localeHelper.getLongMediumDateTime(voucher.getAccountTrx()
                            .getTransactionDate());

            redeemUser = user.getUserId();
        }

        final MarkupHelper helper = new MarkupHelper(this);

        helper.encloseLabel("redeemDate", redeemDate, redeemDate != null);
        helper.encloseLabel("redeemUser", redeemUser, redeemUser != null);

    }

    /**
     * Gives the localized string for a key.
     *
     * @param key
     *            The key from the XML resource file
     * @return The localized string.
     */
    protected final String localized(final String key) {
        return getLocalizer().getString(key, this);
    }
}
