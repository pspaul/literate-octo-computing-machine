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
package org.savapage.server.pages.admin;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.helpers.UserPagerReq;
import org.savapage.core.jpa.User;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.NumberUtil;
import org.savapage.server.SpSession;
import org.savapage.server.pages.MarkupHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class UsersPage extends AbstractAdminListPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(UsersPage.class);

    /**
    *
    */
    private static final AccountingService ACCOUNTING_SERVICE = ServiceContext
            .getServiceFactory().getAccountingService();
    /**
     *
     */
    private static final UserService USER_SERVICE = ServiceContext
            .getServiceFactory().getUserService();

    /**
     *
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5; // must be odd number

    /**
     * The currency symbol of the current Locale.
     */
    private final String currencySymbol;

    /**
     * Number of currency decimals to display.
     */
    private final int currencyDecimals;

    /**
     * @return {@code false} to give Admin a chance to inspect the users.
     */
    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     *
     */
    public UsersPage() {

        // this.openServiceContext();

        this.currencySymbol = SpSession.getAppCurrencySymbol();
        this.currencyDecimals = ConfigManager.getUserBalanceDecimals();

        /*
         * We need a transaction because of the lazy creation of UserAccount
         * instances.
         */
        ServiceContext.getDaoContext().beginTransaction();
        handlePage();
    }

    /**
     *
     */
    private void handlePage() {

        final String data = getParmValue(POST_PARM_DATA);
        final UserPagerReq req = UserPagerReq.read(data);

        final UserDao.Field sortField = req.getSort().getSortField();
        final boolean sortAscending = req.getSort().getAscending();

        final UserDao.ListFilter filter = new UserDao.ListFilter();

        filter.setContainingIdText(req.getSelect().getIdContainingText());
        filter.setContainingEmailText(req.getSelect().getEmailContainingText());
        filter.setInternal(req.getSelect().getInternal());
        filter.setAdmin(req.getSelect().getAdmin());
        filter.setPerson(req.getSelect().getPerson());
        filter.setDisabled(req.getSelect().getDisabled());
        filter.setDeleted(req.getSelect().getDeleted());

        final UserService userService =
                ServiceContext.getServiceFactory().getUserService();

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final long userCount = userDao.getListCount(filter);

        /*
         * Display the requested page.
         */
        final List<User> entryList =
                userDao.getListChunk(filter, req.calcStartPosition(),
                        req.getMaxResults(), sortField, sortAscending);

        final String myCurrencySymbol = this.currencySymbol;
        final int myCurrencyDecimals = this.currencyDecimals;

        final Locale locale = getSession().getLocale();

        //
        add(new PropertyListView<User>("users-view", entryList) {

            private static final long serialVersionUID = 1L;

            /**
             * Decodes an encoded decimal to a localized string.
             *
             * @param value
             * @return
             */
            private final String localizedDecimal(final BigDecimal value) {
                try {
                    return BigDecimalUtil.localize(value, myCurrencyDecimals,
                            locale, true);
                } catch (ParseException e) {
                    throw new SpException(e);
                }
            }

            /**
             *
             * @param item
             * @param mapVisible
             */
            private void evaluateVisible(final ListItem<User> item,
                    final Map<String, String> mapVisible) {

                for (Map.Entry<String, String> entry : mapVisible.entrySet()) {

                    if (entry.getValue() == null) {
                        entry.setValue("");
                    }

                    final String cssClassWlk = null;

                    item.add(createVisibleLabel(
                            StringUtils.isNotBlank(entry.getValue()),
                            entry.getKey(), entry.getValue(), cssClassWlk));
                }

            }

            @Override
            protected void populateItem(final ListItem<User> item) {

                /*
                 *
                 */
                final Map<String, String> mapVisible = new HashMap<>();
                mapVisible.put("balance-currency", null);

                /*
                 *
                 */
                final User user = item.getModelObject();

                Label labelWrk = null;

                final String sparklineData =
                        user.getNumberOfPrintOutPages().toString() + ","
                                + user.getNumberOfPrintInPages() + ","
                                + user.getNumberOfPdfOutPages();

                item.add(new Label("user-pie", sparklineData));

                /*
                 *
                 */
                labelWrk = new Label("userId");
                if (user.getInternal()) {
                    labelWrk.add(new AttributeModifier("class",
                            MarkupHelper.CSS_TXT_INTERNAL_USER));
                }
                item.add(labelWrk);

                /*
                 *
                 */
                item.add(new Label("fullName"));
                item.add(new Label("email", userService
                        .getPrimaryEmailAddress(user)));

                /*
                 * Signal
                 */
                String signalKey = null;
                String color = null;

                final Date onDate = new Date();

                if (user.getDeleted()) {
                    color = MarkupHelper.CSS_TXT_ERROR;
                    signalKey = "signal-user-deleted";
                } else if (USER_SERVICE.isUserFullyDisabled(user, onDate)) {
                    color = MarkupHelper.CSS_TXT_ERROR;
                    signalKey = "signal-user-disabled";
                } else if (user.getAdmin()) {
                    color = MarkupHelper.CSS_TXT_VALID;
                    signalKey = "signal-user-admin";
                } else if (!user.getPerson()) {
                    color = MarkupHelper.CSS_TXT_WARN;
                    signalKey = "signal-user-abstract";
                }

                String signal = "";
                if (signalKey != null) {
                    signal = localized(signalKey);
                }
                labelWrk = new Label("signal", signal);
                labelWrk.add(new AttributeModifier("class", color));
                item.add(labelWrk);

                /*
                 * Balance
                 */
                mapVisible.put("balance-currency", myCurrencySymbol);

                item.add(new Label("balance-amount", ACCOUNTING_SERVICE
                        .getFormattedUserBalance(user, locale, null)));

                /*
                 * Period + Totals
                 */
                String period = ""; // localized("period") + ": ";
                String totals = "";

                if (user.getResetDate() == null) {
                    period += localizedMediumDate(user.getCreatedDate());
                } else {
                    period += localizedMediumDate(user.getResetDate());
                }

                period += " ~ ";

                if (user.getLastUserActivity() != null) {
                    period += localizedMediumDate(user.getLastUserActivity());

                    //
                    String key = null;
                    Integer total = null;

                    //
                    total = user.getNumberOfPrintInJobs();
                    totals += localizedNumber(total);
                    key = (total == 1) ? "job" : "jobs";
                    totals += " " + localized(key);

                    //
                    total = user.getNumberOfPrintInPages();
                    totals += ", " + localizedNumber(total);
                    key = (total == 1) ? "page" : "pages";
                    totals += " " + localized(key);

                    //
                    totals +=
                            ", "
                                    + NumberUtil.humanReadableByteCount(
                                            user.getNumberOfPrintInBytes(),
                                            true);
                }

                item.add(new Label("period", period));
                item.add(new Label("totals", totals));

                /*
                 *
                 */
                String homeSize = "-";
                if (user.getPerson()) {
                    try {
                        final long size =
                                ConfigManager.getUserHomeDirSize(user
                                        .getUserId());
                        if (size > 0) {
                            // homeSize =
                            // FileUtils.byteCountToDisplaySize(size);
                            homeSize =
                                    NumberUtil.humanReadableByteCount(size,
                                            true);
                        }
                    } catch (IOException e) {
                        homeSize = "-";
                    }
                }

                item.add(new Label("home-size", homeSize));

                /*
                 * Set the uid in 'data-savapage' attribute, so it can be picked
                 * up in JavaScript for editing.
                 */
                final boolean visible = !user.getDeleted();

                labelWrk =
                        new Label("button-edit", getLocalizer().getString(
                                "button-edit", this)) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public boolean isVisible() {
                                return visible;
                            }

                        };

                labelWrk.add(new AttributeModifier("data-savapage", user
                        .getUserId()));

                item.add(labelWrk);

                /*
                 *
                 */
                labelWrk =
                        new Label("button-log", getLocalizer().getString(
                                "button-log", this));
                labelWrk.add(new AttributeModifier("data-savapage", user
                        .getId()));
                item.add(labelWrk);

                /*
                 *
                 */
                labelWrk =
                        new Label("button-transaction", getLocalizer()
                                .getString("button-transaction", this));
                labelWrk.add(new AttributeModifier("data-savapage", user
                        .getId()));
                item.add(labelWrk);

                /*
                 *
                 */
                evaluateVisible(item, mapVisible);

            }
        });

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, userCount, MAX_PAGES_IN_NAVBAR,
                "sp-users-page", new String[] { "nav-bar-1", "nav-bar-2" });
    }

}
