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
package org.savapage.server.pages.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.UserGroupAccountDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dto.SharedAccountDto;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.JobTicketNounEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.i18n.SystemModeEnum;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.Device;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrinterAccessInfo;
import org.savapage.server.api.request.ApiRequestHelper;
import org.savapage.server.dropzone.WebPrintHelper;
import org.savapage.server.helpers.CssClassEnum;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.SparklineHtml;
import org.savapage.server.pages.CommunityStatusFooterPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class Main extends AbstractUserPage {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /** */
    private static final String CSS_CLASS_MAIN_ACTIONS = "main_actions";

    /** */
    private static final String CSS_CLASS_MAIN_ACTIONS_BASE =
            "main_action_base";

    /** */
    private static final String CSS_CLASS_MAIN_ACTION_OUTBOX =
            CSS_CLASS_MAIN_ACTIONS_BASE + " sp-btn-show-outbox";

    /** */
    private static final int BUTTONS_IN_ROW = 4;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final UserGroupAccountDao USER_GROUP_ACCOUNT_DAO =
            ServiceContext.getDaoContext().getUserGroupAccountDao();

    /**
     *
     */
    private static enum NavButtonEnum {
        /** */
        ABOUT,
        /** */
        BROWSE,
        /** */
        UPLOAD,
        /** */
        PDF,
        /** */
        LETTERHEAD,
        /** */
        SORT,
        /** Regular printers only. */
        PRINT,
        /** Job Ticket printers only. */
        TICKET,
        /** Regular printers and Job Ticket printers. */
        PRINT_AND_TICKET,
        /** */
        TICKET_QUEUE,
        /** */
        HELP
    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class NavBarItem {

        private final String itemCssClass;
        private final String imgCssClass;
        private final String buttonHtmlId;
        private final String buttonText;

        public NavBarItem(final String itemCssClass, final String imgCssClass,
                final String buttonHtmlId, final String buttonText) {
            this.imgCssClass = imgCssClass;
            this.itemCssClass = itemCssClass;
            this.buttonHtmlId = buttonHtmlId;
            this.buttonText = buttonText;
        }

    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private class NavBarRow extends PropertyListView<NavBarItem> {

        /**
         *
         * @param id
         *            The Wicket id.
         * @param list
         *            The list.
         */
        NavBarRow(final String id, final List<NavBarItem> list) {
            super(id, list);
        }

        /**
        *
        */
        private static final long serialVersionUID = 1L;

        @Override
        protected void populateItem(final ListItem<NavBarItem> listItem) {

            final NavBarItem navBarItem = listItem.getModelObject();

            final WebMarkupContainer contItem = new WebMarkupContainer("item");

            contItem.add(new AttributeModifier(MarkupHelper.ATTR_CLASS,
                    navBarItem.itemCssClass));

            //
            final WebMarkupContainer contButton =
                    new WebMarkupContainer("button");

            contButton
                    .add(new AttributeAppender(MarkupHelper.ATTR_CLASS,
                            String.format(" %s", navBarItem.imgCssClass)))
                    .add(new AttributeModifier(MarkupHelper.ATTR_ID,
                            navBarItem.buttonHtmlId));

            contButton.add(new Label("button-text", navBarItem.buttonText));

            //
            contItem.add(contButton);

            listItem.add(contItem);
        }
    }

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public Main(final PageParameters parameters) {

        super(parameters);

        final ConfigManager cm = ConfigManager.instance();
        final String urlHelp = cm.getConfigValue(Key.WEBAPP_USER_HELP_URL);
        final boolean hasHelpURL = StringUtils.isNotBlank(urlHelp)
                && cm.isConfigValue(Key.WEBAPP_USER_HELP_URL_ENABLE);

        final SpSession session = SpSession.get();
        final UserIdDto userIdDto = session.getUserIdDto();

        final Set<NavButtonEnum> buttonPrivileged =
                this.getNavButtonPriv(userIdDto);

        final Set<NavButtonEnum> buttonSubstCandidates = new HashSet<>();
        final Set<NavButtonEnum> buttonSubstCandidatesFill = new HashSet<>();

        buttonSubstCandidates.add(NavButtonEnum.BROWSE);
        buttonSubstCandidatesFill.add(NavButtonEnum.ABOUT);

        if (hasHelpURL) {
            buttonSubstCandidatesFill.add(NavButtonEnum.HELP);
        }

        final boolean isUpload =
                WebPrintHelper.isWebPrintEnabled(getClientIpAddr());

        if (isUpload) {
            buttonSubstCandidates.add(NavButtonEnum.UPLOAD);
        }

        if (buttonPrivileged.contains(NavButtonEnum.TICKET)
                || buttonPrivileged.contains(NavButtonEnum.PRINT_AND_TICKET)) {
            buttonSubstCandidates.add(NavButtonEnum.TICKET_QUEUE);
        }

        this.populateNavBar(buttonPrivileged, buttonSubstCandidates,
                buttonSubstCandidatesFill);

        // final boolean isPrintDelegate = user != null &&
        // ACCESS_CONTROL_SERVICE
        // .hasAccess(user, ACLRoleEnum.PRINT_DELEGATE);
        addVisible(false, "button-print-delegation", "-");

        add(new CommunityStatusFooterPanel("community-status-footer-panel",
                false));

        final MarkupHelper helper = new MarkupHelper(this);

        addVisible(cm.isConfigValue(Key.WEBAPP_USER_GDPR_ENABLE),
                "btn-txt-gdpr", "GDPR");

        //
        final boolean isMailTicketOperatorNative =
                userIdDto != null && ConfigManager
                        .isMailPrintTicketOperator(userIdDto.getUserId());

        final WebAppTypeEnum webAppType = this.getSessionWebAppType();

        final boolean isMailTicketOperatorRole =
                userIdDto != null && webAppType == WebAppTypeEnum.MAILTICKETS
                        && ConfigManager.isMailPrintTicketingEnabled()
                        && ACCESS_CONTROL_SERVICE.hasAccess(userIdDto,
                                ACLRoleEnum.MAIL_TICKET_OPERATOR);

        if (isMailTicketOperatorNative || isMailTicketOperatorRole) {
            helper.addTransparentAppendAttr("ticket-input",
                    MarkupHelper.ATTR_PLACEHOLDER,
                    JobTicketNounEnum.TICKET.uiText(getLocale()));
        } else {
            helper.discloseLabel("ticket-input");
        }

        // Mini-buttons in footer/header bar

        helper.addTransparentWithAttrTitle("button-mini-outbox", String.format(
                "%s (%s) %s", AdjectiveEnum.HELD.uiText(getLocale()),
                PrintOutNounEnum.JOB.uiText(getLocale(), true).toLowerCase(),
                HtmlButtonEnum.DOTTED_SUFFIX));

        helper.addTransparentWithAttrTitle("button-mini-media-sources-info",
                PROXY_PRINT_SERVICE.localizePrinterOpt(getLocale(),
                        IppDictJobTemplateAttr.ATTR_MEDIA));

        helper.addTransparentWithAttrTitle("button-mini-count-overlays",
                String.format("%s (%s)",
                        AdjectiveEnum.EDITED.uiText(getLocale()),
                        NounEnum.PAGE.uiText(getLocale(), true).toLowerCase()));

        helper.addTransparentWithAttrTitle("button-mini-page-range-select",
                String.format("%s (%s)",
                        AdjectiveEnum.SELECTED.uiText(getLocale()),
                        NounEnum.RANGE.uiText(getLocale()).toLowerCase()));

        helper.addTransparentWithAttrTitle("button-mini-page-range-cut",
                String.format("%s (%s)", AdjectiveEnum.CUT.uiText(getLocale()),
                        NounEnum.RANGE.uiText(getLocale()).toLowerCase()));

        final boolean showUploadInFooter = isUpload
                && buttonSubstCandidates.contains(NavButtonEnum.UPLOAD);
        addVisible(showUploadInFooter, "button-upload",
                localized(HtmlButtonEnum.UPLOAD));
        if (showUploadInFooter) {
            helper.addTransparentWithAttrTitle("button-mini-upload",
                    HtmlButtonEnum.UPLOAD.uiText(getLocale(), true));
        }

        helper.addAppendLabelAttr("btn-about-popup-menu",
                HtmlButtonEnum.ABOUT.uiText(getLocale()),
                MarkupHelper.ATTR_CLASS, CssClassEnum.SP_BTN_ABOUT.clazz());

        // Action pop-up in sort view.
        this.add(MarkupHelper.createEncloseLabel("main-arr-action-pdf", "PDF",
                buttonPrivileged.contains(NavButtonEnum.PDF)));

        this.add(MarkupHelper.createEncloseLabel("main-arr-action-print",
                localized(HtmlButtonEnum.PRINT),
                buttonPrivileged.contains(NavButtonEnum.PRINT)
                        || buttonPrivileged
                                .contains(NavButtonEnum.PRINT_AND_TICKET)));

        this.add(MarkupHelper.createEncloseLabel("main-arr-action-ticket",
                localized("button-ticket"),
                !buttonPrivileged.contains(NavButtonEnum.PRINT)
                        && !buttonPrivileged
                                .contains(NavButtonEnum.PRINT_AND_TICKET)
                        && buttonPrivileged.contains(NavButtonEnum.TICKET)));

        // Fixed buttons in sort view.
        helper.addButton("button-cut", HtmlButtonEnum.CUT);
        helper.addButton("button-back", HtmlButtonEnum.BACK);
        helper.addButton("button-delete", HtmlButtonEnum.DELETE);
        helper.addButton("button-undo", HtmlButtonEnum.UNDO);
        helper.addButton("button-unselect-all", HtmlButtonEnum.UNSELECT_ALL);

        //
        final String userId;
        final String userName;

        if (userIdDto == null) {
            userId = "";
            userName = "";
        } else {
            userId = userIdDto.getUserId();
            userName =
                    StringUtils.defaultIfBlank(userIdDto.getFullName(), userId);
        }

        final boolean showUserBalance = ACCESS_CONTROL_SERVICE.hasPermission(
                userIdDto, ACLOidEnum.U_FINANCIAL, ACLPermissionEnum.READER);

        helper.encloseLabel("mini-user-balance", "", showUserBalance);

        if (showUserBalance) {
            helper.addTransparentWithAttrTitle("button-mini-user-balance",
                    NounEnum.BALANCE.uiText(getLocale())
                            .concat(HtmlButtonEnum.DOTTED_SUFFIX));
        }

        final UserIdDto userIdDtoDocLog = session.getUserIdDtoDocLog();
        if (userIdDtoDocLog != null) {
            helper.encloseLabel("btn-mini-user-name-doclog",
                    StringUtils.defaultIfBlank(userIdDtoDocLog.getFullName(),
                            userIdDtoDocLog.getUserId()),
                    userIdDtoDocLog != null && !userIdDto.getDbKey()
                            .equals(userIdDtoDocLog.getDbKey()));
        } else {
            helper.discloseLabel("btn-mini-user-name-doclog");
        }
        //
        final Label name = helper.addLabel("mini-user-name", userName);
        final Component nameButton =
                helper.addTransparant("btn-mini-user-name");

        final boolean showUserDetails =
                ACCESS_CONTROL_SERVICE.hasAccess(userIdDto, ACLOidEnum.U_USER);

        if (showUserDetails) {
            MarkupHelper.appendComponentAttr(nameButton,
                    MarkupHelper.ATTR_CLASS, "sp-button-user-details");
        }

        //
        final Label pie =
                helper.encloseLabel("sparkline-user-pie", "", showUserDetails);

        if (showUserDetails) {
            MarkupHelper.modifyLabelAttr(pie, SparklineHtml.ATTR_SLICE_COLORS,
                    SparklineHtml.arrayAttr(SparklineHtml.COLOR_PRINTER,
                            SparklineHtml.COLOR_QUEUE,
                            SparklineHtml.COLOR_PDF));
        }

        add(name.add(new AttributeModifier(MarkupHelper.ATTR_TITLE, userId)));

        //
        helper.encloseLabel("mini-sys-maintenance",
                SystemModeEnum.MAINTENANCE.uiText(getLocale()),
                ConfigManager.getSystemMode() == SystemModeEnum.MAINTENANCE);

        if (hasHelpURL) {

            final Label btn = helper.encloseLabel("button-mini-help",
                    HtmlButtonEnum.HELP.uiText(getLocale()), true);

            MarkupHelper.modifyLabelAttr(btn, MarkupHelper.ATTR_HREF, urlHelp);

            if (!buttonSubstCandidatesFill.contains(NavButtonEnum.HELP)) {
                // Hide: href is used by the substitute main button.
                MarkupHelper.modifyLabelAttr(btn, MarkupHelper.ATTR_STYLE,
                        "display:none;");
            }

        } else {
            helper.discloseLabel("button-mini-help");
        }

        // Job info pop-up
        helper.addLabel("header-document", NounEnum.DOCUMENT);

        helper.addLabel("prompt-job-pages",
                NounEnum.PAGE.uiText(getLocale(), true));

        // as in "Pages: 4 • 1 deleted" or "Pages: 4 • 3 deleted"
        helper.addLabel("prompt-deleted",
                AdverbEnum.DELETED.uiText(getLocale()).toLowerCase());

        helper.addButton("prompt-job-rotate", HtmlButtonEnum.ROTATE);
        helper.addButton("button-apply", HtmlButtonEnum.APPLY);
        helper.addButton("button-close", HtmlButtonEnum.CLOSE);

        helper.addLabel("button-paste-before",
                HtmlButtonEnum.PASTE.uiText(getLocale()).concat(" ::"));
        helper.addLabel("button-paste-after",
                ":: ".concat(HtmlButtonEnum.PASTE.uiText(getLocale())));

        //
        helper.addButton("button-continue", HtmlButtonEnum.CONTINUE);
    }

    /**
     * Gets the buttons that must be present on the main navigation bar.
     *
     * @param user
     *            The user.
     * @return The privileged navigation buttons.
     */
    private Set<NavButtonEnum> getNavButtonPriv(final UserIdDto user) {

        final Set<NavButtonEnum> set = new HashSet<>();

        NavButtonEnum navButtonWlk;

        //
        navButtonWlk = NavButtonEnum.PDF;

        final Integer inboxPriv =
                ACCESS_CONTROL_SERVICE.getPrivileges(user, ACLOidEnum.U_INBOX);

        if (inboxPriv == null
                || ACLPermissionEnum.DOWNLOAD.isPresent(inboxPriv.intValue())
                || ACLPermissionEnum.SEND.isPresent(inboxPriv.intValue())) {
            set.add(navButtonWlk);
        }

        //
        if (ACCESS_CONTROL_SERVICE.isAuthorized(user,
                ACLRoleEnum.PRINT_CREATOR)) {

            final boolean allowPrint;

            if (ACCESS_CONTROL_SERVICE.hasAccess(user,
                    ACLOidEnum.U_PERSONAL_PRINT)) {

                allowPrint = true;

            } else {
                final UserGroupAccountDao.ListFilter filter =
                        new UserGroupAccountDao.ListFilter();

                filter.setUserId(user.getDbKey());
                filter.setDisabled(Boolean.FALSE);

                final List<SharedAccountDto> sharedAccounts =
                        USER_GROUP_ACCOUNT_DAO.getListChunk(filter, null, null);

                allowPrint =
                        sharedAccounts != null && !sharedAccounts.isEmpty();
            }

            if (allowPrint) {
                final Device terminal = ApiRequestHelper
                        .getHostTerminal(this.getClientIpAddr());

                try {
                    final PrinterAccessInfo accessInfo =
                            PROXY_PRINT_SERVICE.getUserPrinterAccessInfo(
                                    terminal, user.getUserId());

                    final NavButtonEnum navButtonPrint;

                    if (accessInfo.isJobTicketsOnly()) {
                        navButtonPrint = NavButtonEnum.TICKET;
                    } else {
                        if (accessInfo.isJobTicketsPresent()) {
                            navButtonPrint = NavButtonEnum.PRINT_AND_TICKET;
                        } else {
                            navButtonPrint = NavButtonEnum.PRINT;
                        }
                    }

                    set.add(navButtonPrint);

                } catch (IppConnectException | IppSyntaxException e) {
                    LOGGER.error(e.getMessage());
                }
            }

        } else if (ACCESS_CONTROL_SERVICE.isAuthorized(user,
                ACLRoleEnum.JOB_TICKET_CREATOR)) {
            set.add(NavButtonEnum.TICKET);
        }

        //
        navButtonWlk = NavButtonEnum.SORT;

        if (inboxPriv == null
                || ACLPermissionEnum.EDITOR.isPresent(inboxPriv.intValue())) {
            set.add(navButtonWlk);
        }

        //
        navButtonWlk = NavButtonEnum.LETTERHEAD;
        if (ACCESS_CONTROL_SERVICE.hasAccess(user, ACLOidEnum.U_LETTERHEAD)) {
            set.add(navButtonWlk);
        }

        //
        return set;
    }

    /**
     * Uses a {@link NavBarItem} by removing it from the set of candidates.
     *
     * @param buttonCandidates
     *            The set of candidates.
     * @return The {@link NavBarItem} used.
     */
    private NavBarItem
            useButtonCandidate(final Set<NavButtonEnum> buttonCandidates) {

        NavButtonEnum candidate = null;
        NavBarItem item = null;

        /*
         * Order is important!
         */

        // #1
        if (item == null) {
            candidate = NavButtonEnum.UPLOAD;
            if (buttonCandidates.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                        "ui-icon-main-upload", "button-mini-upload",
                        localized(HtmlButtonEnum.UPLOAD));
            }
        }

        // #2
        if (item == null) {
            candidate = NavButtonEnum.TICKET_QUEUE;
            if (buttonCandidates.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTION_OUTBOX,
                        "ui-icon-main-hold-jobs", "button-main-outbox",
                        NounEnum.QUEUE.uiText(getLocale()));
            }
        }

        // #5
        if (item == null) {
            candidate = NavButtonEnum.BROWSE;
            if (buttonCandidates.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTIONS,
                        "ui-icon-main-browse", "button-browser",
                        localized(HtmlButtonEnum.BROWSE));
            }
        }

        if (item != null) {
            buttonCandidates.remove(candidate);
        }

        return item;
    }

    /**
     * Uses a {@link NavBarItem} by removing it from the set of filler
     * candidates.
     *
     * @param buttonCandidatesFill
     *            The set of candidates.
     * @return The {@link NavBarItem} used.
     */
    private NavBarItem useButtonCandidateFill(
            final Set<NavButtonEnum> buttonCandidatesFill) {

        NavButtonEnum candidate = null;
        NavBarItem item = null;

        // #1
        if (item == null) {
            candidate = NavButtonEnum.HELP;
            if (buttonCandidatesFill.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                        "ui-icon-main-help", "button-main-help",
                        localized(HtmlButtonEnum.HELP));
            }
        }

        // #2
        if (item == null) {
            candidate = NavButtonEnum.ABOUT;
            if (buttonCandidatesFill.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                        String.format("%s %s", "ui-icon-main-about",
                                CssClassEnum.SP_BTN_ABOUT.clazz()),
                        "button-about", localized(HtmlButtonEnum.ABOUT));
            }
        }

        if (item != null) {
            buttonCandidatesFill.remove(candidate);
        }

        return item;
    }

    /**
     * Populates the central navigation bar.
     *
     * @param buttonPrivileged
     * @param buttonCandidates
     *            The substitute buttons.
     * @param buttonCandidatesFill
     *            Filler buttons.
     */
    private void populateNavBar(final Set<NavButtonEnum> buttonPrivileged,
            final Set<NavButtonEnum> buttonCandidates,
            final Set<NavButtonEnum> buttonCandidatesFill) {

        // ----------------------------------------------------------
        // Row 1
        // ----------------------------------------------------------
        final List<NavBarItem> itemsRow1 = new ArrayList<>();

        // ------------
        // PDF
        // ------------
        if (buttonPrivileged.contains(NavButtonEnum.PDF)) {
            itemsRow1.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS,
                    "ui-icon-main-pdf-properties", "button-main-pdf-properties",
                    "PDF"));
        } else {
            // Immediate replacement.
            itemsRow1.add(this.useButtonCandidate(buttonCandidates));
        }

        // --------------------------
        // Print or Ticket (or none)
        // --------------------------
        final boolean isJobTicketCopier = ConfigManager.instance()
                .isConfigValue(Key.JOBTICKET_COPIER_ENABLE);

        if (buttonPrivileged.contains(NavButtonEnum.PRINT)) {

            itemsRow1.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS,
                    "ui-icon-main-print", "button-main-print",
                    localized(HtmlButtonEnum.PRINT)));

        } else if (buttonPrivileged.contains(NavButtonEnum.PRINT_AND_TICKET)) {

            final String cssClass;

            if (isJobTicketCopier) {
                cssClass = CSS_CLASS_MAIN_ACTIONS_BASE;
            } else {
                cssClass = CSS_CLASS_MAIN_ACTIONS;
            }

            itemsRow1.add(new NavBarItem(cssClass, "ui-icon-main-print",
                    "button-main-print", localized(HtmlButtonEnum.PRINT)));

        } else if (buttonPrivileged.contains(NavButtonEnum.TICKET)) {

            final String cssClass;

            if (isJobTicketCopier) {
                cssClass = CSS_CLASS_MAIN_ACTIONS_BASE;
            } else {
                cssClass = CSS_CLASS_MAIN_ACTIONS;
            }

            itemsRow1.add(new NavBarItem(cssClass, "ui-icon-main-jobticket",
                    "button-main-print", localized("button-ticket")));
        }

        // ------------
        // Letterhead
        // ------------
        if (buttonPrivileged.contains(NavButtonEnum.LETTERHEAD)) {
            itemsRow1.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                    "ui-icon-main-letterhead", "button-main-letterhead",
                    NounEnum.LETTERHEAD.uiText(getLocale(), true)));
        }

        // ------------
        // Delete
        // ------------
        itemsRow1.add(
                new NavBarItem(CSS_CLASS_MAIN_ACTIONS, "ui-icon-main-clear",
                        "button-main-clear", localized(HtmlButtonEnum.DELETE)));

        // Append filler buttons.
        for (int i = itemsRow1.size(); i < BUTTONS_IN_ROW; i++) {
            final NavBarItem nbi = this.useButtonCandidate(buttonCandidates);
            if (nbi != null) {
                itemsRow1.add(nbi);
            }
        }
        // ----------------------------------------------------------
        // Row 2
        // ----------------------------------------------------------
        final List<NavBarItem> itemsRow2 = new ArrayList<>();

        itemsRow2.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                "ui-icon-main-logout", "button-logout",
                localized(HtmlButtonEnum.LOGOUT)));

        itemsRow2.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                "ui-icon-main-refresh", "button-main-refresh",
                localized(HtmlButtonEnum.REFRESH)));

        itemsRow2.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                "ui-icon-main-doclog", "button-main-doclog",
                localized("button-doclog")));

        if (buttonPrivileged.contains(NavButtonEnum.SORT)) {
            itemsRow2.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS,
                    "ui-icon-main-arr-edit", "main-arr-edit",
                    localized(HtmlButtonEnum.SORT)));
        } else {
            final NavBarItem nbi = this.useButtonCandidate(buttonCandidates);
            if (nbi != null) {
                itemsRow2.add(nbi);
            }
        }
        // Make sure rows have same number of buttons.
        if (itemsRow1.size() != itemsRow2.size()) {
            final List<NavBarItem> itemsRowFill;
            final int nFill;
            if (itemsRow1.size() < itemsRow2.size()) {
                itemsRowFill = itemsRow1;
                nFill = itemsRow2.size() - itemsRow1.size();
            } else {
                itemsRowFill = itemsRow2;
                nFill = itemsRow1.size() - itemsRow2.size();
            }
            for (int i = 0; i < nFill; i++) {
                final NavBarItem nbi =
                        this.useButtonCandidateFill(buttonCandidatesFill);
                if (nbi != null) {
                    itemsRowFill.add(nbi);
                }
            }
        }
        //
        add(new NavBarRow("main-navbar-row-top", itemsRow1));
        add(new NavBarRow("main-navbar-row-bottom", itemsRow2));
    }

}
