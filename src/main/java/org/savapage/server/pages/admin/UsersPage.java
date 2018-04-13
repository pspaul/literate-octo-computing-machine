/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.dao.helpers.UserPagerReq;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroupMember;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.util.NumberUtil;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.SparklineHtml;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UsersPage extends AbstractAdminListPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();
    /**
     *
     */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /**
     * Important: must be odd number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    /**
     * @return {@code false} to give Admin a chance to inspect the users.
     */
    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private final class UserListView extends PropertyListView<User> {

        private static final long serialVersionUID = 1L;

        /***/
        private final boolean isEditor;
        /***/
        private final boolean hasAccessDoc;
        /***/
        private final boolean hasAccessTrx;
        /***/
        private final boolean isAppReady;

        /**
         * The currency symbol of the current Locale.
         */
        private final String currencySymbol;

        /** */
        private static final String WID_BUTTON_LOG = "button-log";
        /** */
        private static final String WID_BUTTON_TRX = "button-transaction";
        /** */
        private static final String WID_BUTTON_GDPR = "button-gdpr";

        /**
         *
         * @param id
         * @param list
         * @param isEditor
         */
        public UserListView(final String id, final List<User> list,
                final boolean isEditor) {

            super(id, list);

            this.currencySymbol = SpSession.getAppCurrencySymbol();
            this.isAppReady = ConfigManager.instance().isAppReadyToUse();

            final User reqUser = SpSession.get().getUser();

            this.isEditor = isEditor;

            this.hasAccessDoc = ACCESS_CONTROL_SERVICE.hasAccess(reqUser,
                    ACLOidEnum.A_DOCUMENTS);

            this.hasAccessTrx = ACCESS_CONTROL_SERVICE.hasAccess(reqUser,
                    ACLOidEnum.A_TRANSACTIONS);
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

            final MarkupHelper helper = new MarkupHelper(item);

            final Map<String, String> mapVisible = new HashMap<>();
            mapVisible.put("balance-currency", null);

            final User user = item.getModelObject();

            final boolean isErased = USER_SERVICE.isErased(user);

            Label labelWrk = null;

            MarkupHelper.modifyLabelAttr(
                    helper.addModifyLabelAttr("user-pie",
                            SparklineHtml.valueString(
                                    user.getNumberOfPrintOutPages().toString(),
                                    user.getNumberOfPrintInPages().toString(),
                                    user.getNumberOfPdfOutPages().toString()),
                            SparklineHtml.ATTR_SLICE_COLORS,
                            SparklineHtml.arrayAttr(SparklineHtml.COLOR_PRINTER,
                                    SparklineHtml.COLOR_QUEUE,
                                    SparklineHtml.COLOR_PDF)),
                    MarkupHelper.ATTR_CLASS, SparklineHtml.CSS_CLASS_USER);

            //
            if (isErased) {
                helper.discloseLabel("fullName");

                labelWrk = new Label("userId",
                        USER_SERVICE.getUserIdUi(user, getLocale()));
                labelWrk.add(new AttributeModifier("class",
                        MarkupHelper.CSS_TXT_ERROR));
            } else {
                item.add(new Label("fullName"));
                item.add(new Label("email",
                        USER_SERVICE.getPrimaryEmailAddress(user)));
                //
                labelWrk = new Label("userId");
                if (user.getInternal()) {
                    labelWrk.add(new AttributeModifier("class",
                            MarkupHelper.CSS_TXT_INTERNAL_USER));
                }
            }
            item.add(labelWrk);

            /*
             * Signal
             */
            String signal = "";
            String signalKey = null;
            String color = null;

            final Date onDate = new Date();

            if (isErased) {
                color = MarkupHelper.CSS_TXT_ERROR;
                signal = AdjectiveEnum.ERASED.uiText(getLocale()).toLowerCase();
            } else if (user.getDeleted()) {
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

            if (signalKey != null) {
                signal = localized(signalKey);
            }
            labelWrk = new Label("signal", signal);
            labelWrk.add(new AttributeModifier("class", color));
            item.add(labelWrk);

            /*
             *
             */
            final StringBuilder groups = new StringBuilder();

            if (!user.getDeleted()) {

                final List<UserGroupMember> memberships =
                        user.getGroupMembership();

                if (memberships != null) {
                    int i = 0;
                    for (final UserGroupMember member : memberships) {
                        if (i++ > 0) {
                            groups.append(", ");
                        }
                        groups.append(member.getGroup().getGroupName());
                    }
                }
                if (groups.length() > 0) {
                    helper.addLabel("userGroupsPrompt",
                            NounEnum.GROUP.uiText(getLocale()));
                }
            }
            helper.encloseLabel("userGroups", groups.toString(),
                    groups.length() > 0);

            /*
             * Balance
             */
            mapVisible.put("balance-currency", this.currencySymbol);

            item.add(new Label("balance-amount", ACCOUNTING_SERVICE
                    .getFormattedUserBalance(user, getLocale(), null)));

            /*
             * Period + Totals
             */
            final StringBuilder period = new StringBuilder();
            final StringBuilder totals = new StringBuilder();

            if (user.getResetDate() == null) {
                period.append(localizedMediumDate(user.getCreatedDate()));
            } else {
                period.append(localizedMediumDate(user.getResetDate()));
            }

            period.append(" ~ ");

            if (user.getLastUserActivity() != null) {
                period.append(localizedMediumDate(user.getLastUserActivity()));

                //
                String key = null;
                Integer total = null;

                //
                total = user.getNumberOfPrintInJobs();
                totals.append(helper.localizedNumber(total));
                key = (total == 1) ? "job" : "jobs";
                totals.append(" ").append(localized(key));

                //
                total = user.getNumberOfPrintInPages();
                totals.append(", ").append(helper.localizedNumber(total));
                key = (total == 1) ? "page" : "pages";
                totals.append(" ").append(localized(key));

                //
                totals.append(", ").append(NumberUtil.humanReadableByteCount(
                        user.getNumberOfPrintInBytes(), true));
            }

            item.add(new Label("period", period.toString()));
            item.add(new Label("totals", totals.toString()));

            /*
             *
             */
            String homeSize = "-";
            if (user.getPerson()) {
                try {
                    final long size =
                            ConfigManager.getUserHomeDirSize(user.getUserId());
                    if (size > 0) {
                        // homeSize =
                        // FileUtils.byteCountToDisplaySize(size);
                        homeSize =
                                NumberUtil.humanReadableByteCount(size, true);
                    }
                } catch (IOException e) {
                    homeSize = "-";
                }
            }

            item.add(new Label("home-size", homeSize));

            /*
             * Set the uid in 'data-savapage' attribute, so it can be picked up
             * in JavaScript for editing.
             */
            final boolean visible =
                    !user.getDeleted() && this.isAppReady && this.isEditor;

            labelWrk = new Label("button-edit",
                    HtmlButtonEnum.EDIT.uiText(getLocale())) {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return visible;
                }

            };

            labelWrk.add(new AttributeModifier(MarkupHelper.ATTR_DATA_SAVAPAGE,
                    user.getUserId()));

            item.add(labelWrk);

            if (this.isAppReady) {

                if (this.hasAccessDoc) {
                    labelWrk = new Label(WID_BUTTON_LOG,
                            getLocalizer().getString("button-log", this));
                    labelWrk.add(new AttributeModifier(
                            MarkupHelper.ATTR_DATA_SAVAPAGE, user.getId()));
                    item.add(labelWrk);
                } else {
                    helper.discloseLabel(WID_BUTTON_LOG);
                }

                if (this.hasAccessTrx) {
                    labelWrk = new Label(WID_BUTTON_TRX, getLocalizer()
                            .getString("button-transaction", this));
                    labelWrk.add(new AttributeModifier(
                            MarkupHelper.ATTR_DATA_SAVAPAGE, user.getId()));
                    item.add(labelWrk);
                } else {
                    helper.discloseLabel(WID_BUTTON_TRX);
                }

                //
                if (isErased) {
                    helper.discloseLabel(WID_BUTTON_GDPR);
                } else {
                    labelWrk = new Label(WID_BUTTON_GDPR, "GDPR");
                    labelWrk.add(new AttributeModifier(
                            MarkupHelper.ATTR_DATA_SAVAPAGE, user.getId()));
                    item.add(labelWrk);
                }

            } else {
                helper.discloseLabel(WID_BUTTON_LOG);
                helper.discloseLabel(WID_BUTTON_TRX);
                helper.discloseLabel(WID_BUTTON_GDPR);
            }
            //
            evaluateVisible(item, mapVisible);
        }

    }

    /**
     *
     */
    public UsersPage(final PageParameters parameters) {

        super(parameters);

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

        //
        final Long userGroupId = req.getSelect().getUserGroupId();

        if (userGroupId != null) {

            final UserGroupDao userGroupDao =
                    ServiceContext.getDaoContext().getUserGroupDao();

            final ReservedUserGroupEnum reservedGroup =
                    userGroupDao.findReservedGroup(userGroupId);

            if (reservedGroup == null) {
                filter.setUserGroupId(req.getSelect().getUserGroupId());
            } else {
                filter.setInternal(reservedGroup.isInternalExternal());
            }
        }

        //
        filter.setContainingIdText(req.getSelect().getIdContainingText());
        filter.setContainingEmailText(req.getSelect().getEmailContainingText());
        filter.setAdmin(req.getSelect().getAdmin());
        filter.setPerson(req.getSelect().getPerson());
        filter.setDisabled(req.getSelect().getDisabled());
        filter.setDeleted(req.getSelect().getDeleted());

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final long userCount = userDao.getListCount(filter);

        /*
         * Display the requested page.
         */
        final List<User> entryList =
                userDao.getListChunk(filter, req.calcStartPosition(),
                        req.getMaxResults(), sortField, sortAscending);

        //
        add(new UserListView("users-view", entryList,
                this.probePermissionToEdit(ACLOidEnum.A_USERS)));

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, userCount, MAX_PAGES_IN_NAVBAR,
                "sp-users-page", new String[] { "nav-bar-1", "nav-bar-2" });

        final MarkupHelper helper = new MarkupHelper(this);
        helper.addButton("btn-gdpr-download", HtmlButtonEnum.DOWNLOAD);
    }
}
