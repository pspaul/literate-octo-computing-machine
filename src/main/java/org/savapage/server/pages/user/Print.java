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
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.UserGroupAccountDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.dto.JobTicketDomainDto;
import org.savapage.core.dto.JobTicketLabelDomainPartDto;
import org.savapage.core.dto.JobTicketTagDto;
import org.savapage.core.dto.JobTicketUseDto;
import org.savapage.core.dto.SharedAccountDto;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.i18n.JobTicketNounEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutAdjectiveEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.i18n.PrintOutVerbEnum;
import org.savapage.core.json.JobTicketProperties;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.DocStoreService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.services.helpers.InboxSelectScopeEnum;
import org.savapage.core.util.DateUtil;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.EnumRadioPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.PageIllegalState;
import org.savapage.server.pages.QuickSearchPanel;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class Print extends AbstractUserPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER = LoggerFactory.getLogger(Print.class);

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /** */
    private static final DocStoreService DOC_STORE_SERVICE =
            ServiceContext.getServiceFactory().getDocStoreService();

    /** */
    private static final UserGroupAccountDao USER_GROUP_ACCOUNT_DAO =
            ServiceContext.getDaoContext().getUserGroupAccountDao();

    private static final String ID_BTN_JOBTICKET_DATETIME_DEFAULT =
            "btn-jobticket-datetime-default";

    /** */
    private static final String ID_BTN_PRINT_AND_CLOSE = "btn-print-and-close";
    /** */
    private static final String ID_BTN_PRINT_AND_CLOSE_POPUP =
            "btn-print-and-close-popup";

    /** */
    private static final String ID_DELETE_PAGES = "delete-pages-after-print";
    /** */
    private static final String ID_DELETE_PAGES_WARN =
            "delete-pages-after-print-warn";
    /** */
    private static final String ID_DELETE_PAGES_SCOPE =
            "delete-pages-after-print-scope";
    /** */
    private static final String HTML_NAME_DELETE_PAGES_SCOPE =
            ID_DELETE_PAGES_SCOPE;

    /** */
    private static final String CSS_CLASS_JOB_TICKET_DOMAIN =
            "sp-jobticket-domain";

    /** */
    private static final String CSS_CLASS_JOB_TICKET_DOMAIN_PFX =
            CSS_CLASS_JOB_TICKET_DOMAIN + "-";

    /** */
    public static final Long OPTION_VALUE_SELECT_PROMPT = -1L;
    /** */
    public static final Long OPTION_VALUE_SELECT_PERSONAL_ACCOUNT = 0L;

    /**
     *
     * @param parameters
     *            The page parms.
     */
    public Print(final PageParameters parameters) {

        super(parameters);

        final ConfigManager cm = ConfigManager.instance();

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("label-copy",
                PrintOutNounEnum.COPY.uiText(getLocale()));

        helper.addModifyLabelAttr("slider-print-copies", "max",
                cm.getConfigValue(Key.WEBAPP_USER_PROXY_PRINT_MAX_COPIES));

        if (cm.isConfigValue(Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_ENABLE)) {

            helper.discloseLabel(ID_DELETE_PAGES);

            if (cm.isConfigValue(
                    Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_PROMPT)) {

                final InboxSelectScopeEnum clearScope =
                        cm.getConfigEnum(InboxSelectScopeEnum.class,
                                Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_SCOPE);

                final String keyWarn;
                switch (clearScope) {
                case ALL:
                    keyWarn = "delete-pages-after-print-info-all";
                    break;
                case JOBS:
                    keyWarn = "delete-pages-after-print-info-jobs";
                    break;
                case PAGES:
                    keyWarn = "delete-pages-after-print-info-pages";
                    break;
                case NONE:
                default:
                    throw new SpException(String.format("%s is not handled.",
                            clearScope.toString()));
                }

                helper.encloseLabel(ID_DELETE_PAGES_WARN, localized(keyWarn),
                        true);
            } else {
                helper.discloseLabel(ID_DELETE_PAGES_WARN);
            }

        } else {

            helper.discloseLabel(ID_DELETE_PAGES_WARN);

            add(new Label(ID_DELETE_PAGES));

            final EnumRadioPanel clearInboxScopePanel =
                    new EnumRadioPanel(ID_DELETE_PAGES_SCOPE);

            clearInboxScopePanel.populate(
                    EnumSet.complementOf(EnumSet.of(InboxSelectScopeEnum.NONE)),
                    InboxSelectScopeEnum.ALL,
                    InboxSelectScopeEnum.uiTextMap(getLocale()),
                    HTML_NAME_DELETE_PAGES_SCOPE);

            add(clearInboxScopePanel);
        }

        final QuickSearchPanel panel =
                new QuickSearchPanel("quicksearch-printer");

        add(panel);

        panel.populate("sp-print-qs-printer", "",
                getLocalizer().getString("search-printer-placeholder", this),
                false, null, null);

        //
        if (cm.isConfigValue(Key.WEBAPP_USER_PROXY_PRINT_SEPARATE_ENABLE)) {
            final boolean isSeparate =
                    cm.isConfigValue(Key.WEBAPP_USER_PROXY_PRINT_SEPARATE);
            helper.addCheckbox("print-documents-separate-print", isSeparate);
            helper.addCheckbox("print-documents-separate-ticket", isSeparate);
        } else {
            helper.discloseLabel("print-documents-separate-print");
            helper.discloseLabel("print-documents-separate-ticket");
        }

        //
        helper.encloseLabel("print-remove-graphics-label",
                localized("print-remove-graphics"),
                cm.isConfigValue(Key.PROXY_PRINT_REMOVE_GRAPHICS_ENABLE));

        //
        helper.encloseLabel("print-ecoprint", "",
                ConfigManager.isEcoPrintEnabled());

        final Integer discount =
                cm.getConfigInt(Key.ECO_PRINT_DISCOUNT_PERC, 0);

        final String ecoPrintLabel;

        if (discount.intValue() > 0) {
            ecoPrintLabel = localized("print-ecoprint-with-discount", discount);
        } else {
            ecoPrintLabel = localized("print-ecoprint");
        }

        helper.addLabel("print-ecoprint-label", ecoPrintLabel);

        //
        final boolean isJobTicketDatetime =
                cm.isConfigValue(Key.JOBTICKET_DELIVERY_DATETIME_ENABLE);

        helper.encloseLabel("prompt-jobticket-delivery-datetime",
                localized("prompt-jobticket-delivery-datetime"),
                isJobTicketDatetime);

        if (isJobTicketDatetime) {

            helper.addLabel("prompt-jobticket-delivery-datetime-weekdays",
                    DateUtil.getWeekDayOrdinalsText(
                            JOBTICKET_SERVICE.getDeliveryDaysOfWeek(),
                            getLocale()));

            if (cm.getConfigInt(Key.JOBTICKET_DELIVERY_DAYS, 0) > 0) {
                MarkupHelper.modifyComponentAttr(
                        helper.addTransparant(
                                ID_BTN_JOBTICKET_DATETIME_DEFAULT),
                        MarkupHelper.ATTR_TITLE,
                        HtmlButtonEnum.DEFAULT.uiText(getLocale()));
            } else {
                helper.discloseLabel(ID_BTN_JOBTICKET_DATETIME_DEFAULT);
            }

            helper.encloseLabel("jobticket-delivery-time-hr", "",
                    cm.isConfigValue(Key.JOBTICKET_DELIVERY_TIME_ENABLE));
        }
        //
        boolean hasInvoicingOptions = false;

        final UserIdDto userIdDto = SpSession.get().getUserIdDto();

        final boolean isPrintDelegate = ACCESS_CONTROL_SERVICE
                .hasAccess(userIdDto, ACLRoleEnum.PRINT_DELEGATE);

        addVisible(isPrintDelegate, "button-print-delegation",
                PrintOutNounEnum.COPY.uiText(getLocale(), true));

        if (isPrintDelegate) {

            hasInvoicingOptions = true;

            if (cm.isConfigValue(
                    Key.WEBAPP_USER_PROXY_PRINT_DELEGATE_COPIES_APPLY_SWITCH)) {

                MarkupHelper.setFlipswitchOnOffText(
                        helper.addLabel("flipswitch-print-as-delegate", ""),
                        getLocale());
            } else {
                helper.discloseLabel("flipswitch-print-as-delegate");
            }
        }

        //
        final boolean isArchive;
        final boolean isArchiveSelectable;
        final String archivePrompt;

        if (DOC_STORE_SERVICE.isEnabled(DocStoreTypeEnum.ARCHIVE,
                DocStoreBranchEnum.OUT_PRINT)) {

            final Integer privsArchive = ACCESS_CONTROL_SERVICE
                    .getPrivileges(userIdDto, ACLOidEnum.U_PRINT_ARCHIVE);

            if (privsArchive == null) {
                isArchive = true;
                isArchiveSelectable = false;
            } else {
                isArchive = ACLPermissionEnum.READER
                        .isPresent(privsArchive.intValue());
                isArchiveSelectable = isArchive
                        && ACLPermissionEnum.SELECT.isPresent(privsArchive);
            }
        } else {
            isArchive = false;
            isArchiveSelectable = false;
        }

        if (isArchive && cm.isConfigValue(
                Key.WEBAPP_USER_DOC_STORE_ARCHIVE_OUT_PRINT_PROMPT)) {
            if (isArchiveSelectable) {
                archivePrompt = localized("sp-archive-select-prompt");
            } else {
                archivePrompt = localized("sp-archive-auto-prompt");
            }
        } else {
            archivePrompt = null;
        }

        helper.encloseLabel("archive-print-job-prompt", archivePrompt,
                archivePrompt != null);

        helper.encloseLabel("archive-print-job",
                HtmlButtonEnum.ARCHIVE.uiText(getLocale()),
                isArchiveSelectable);

        //
        final Integer privsLetterhead = ACCESS_CONTROL_SERVICE
                .getPrivileges(userIdDto, ACLOidEnum.U_LETTERHEAD);

        helper.encloseLabel("prompt-letterhead",
                NounEnum.LETTERHEAD.uiText(getLocale()),
                privsLetterhead == null || ACLPermissionEnum.READER
                        .isPresent(privsLetterhead.intValue()));

        //
        if (ACCESS_CONTROL_SERVICE.hasAccess(userIdDto,
                ACLRoleEnum.JOB_TICKET_CREATOR)
                && !ACCESS_CONTROL_SERVICE.hasAccess(userIdDto,
                        ACLRoleEnum.PRINT_CREATOR)) {
            add(new Label("title",
                    JobTicketNounEnum.TICKET.uiText(getLocale())));
        } else {
            add(new Label("title", HtmlButtonEnum.PRINT.uiText(getLocale())));
        }

        //
        if (cm.isConfigValue(Key.JOBTICKET_COPIER_ENABLE)) {
            helper.encloseLabel("jobticket-type",
                    NounEnum.TYPE.uiText(getLocale()), true);
            helper.addModifyLabelAttr("jobticket-type-print",
                    MarkupHelper.ATTR_VALUE, "PRINT");
            helper.addModifyLabelAttr("jobticket-type-copy",
                    MarkupHelper.ATTR_VALUE, "COPY");
        } else {
            helper.discloseLabel("jobticket-type");
        }

        //
        final boolean allowPersonalPrint =
                ACCESS_CONTROL_SERVICE.hasPermission(userIdDto,
                        ACLOidEnum.U_PERSONAL_PRINT, ACLPermissionEnum.READER);

        final UserGroupAccountDao.ListFilter filter =
                new UserGroupAccountDao.ListFilter();

        filter.setUserId(SpSession.get().getUserIdDto().getDbKey());
        filter.setDisabled(Boolean.FALSE);

        final List<SharedAccountDto> sharedAccounts =
                USER_GROUP_ACCOUNT_DAO.getListChunk(filter, null, null);

        if (sharedAccounts == null || sharedAccounts.isEmpty()) {

            if (!allowPersonalPrint) {
                LOGGER.warn(
                        "User [{}] is not authorized for Personal "
                                + "and Shared Account Print.",
                        userIdDto.getUserId());
                throw new RestartResponseException(
                        new PageIllegalState(AppLogLevelEnum.ERROR,
                                "Not authorized for Personal Print."));
            }

            helper.discloseLabel("print-account-type");

        } else {

            hasInvoicingOptions = true;

            helper.encloseLabel("print-account-type",
                    PrintOutNounEnum.ACCOUNT.uiText(getLocale()), true);

            if (allowPersonalPrint) {
                helper.discloseLabel("print-shared-account-option-only");
                helper.addModifyLabelAttr("print-account-type-personal",
                        PrintOutAdjectiveEnum.PERSONAL.uiText(getLocale()),
                        MarkupHelper.ATTR_VALUE,
                        OPTION_VALUE_SELECT_PERSONAL_ACCOUNT.toString());

            } else {
                helper.discloseLabel("print-account-type-personal");
                helper.addModifyLabelAttr("btn-select-shared-account",
                        HtmlButtonEnum.SELECT.uiTextDottedSfx(getLocale())
                                .toLowerCase(),
                        MarkupHelper.ATTR_VALUE,
                        OPTION_VALUE_SELECT_PROMPT.toString());
            }

            addSharedAccounts(sharedAccounts, allowPersonalPrint);
        }

        //
        final boolean isJobTicketDomainsEnable =
                cm.isConfigValue(Key.JOBTICKET_DOMAINS_ENABLE);

        final Collection<JobTicketDomainDto> jobTicketDomains;

        if (isJobTicketDomainsEnable) {

            jobTicketDomains = JOBTICKET_SERVICE.getTicketDomainsByName();

            if (!jobTicketDomains.isEmpty()) {

                final JobTicketProperties ticketProps;

                if (cm.isConfigValue(Key.JOBTICKET_DOMAINS_RETAIN)) {
                    ticketProps =
                            USER_SERVICE.getJobTicketPropsLatest(userIdDto);
                } else {
                    ticketProps = new JobTicketProperties();
                }

                helper.encloseLabel("label-jobticket-domain",
                        JobTicketNounEnum.DOMAIN.uiText(getLocale()), true);
                helper.addLabel("jobticket-domain-option-select",
                        HtmlButtonEnum.SELECT.uiText(getLocale(), true)
                                .toLowerCase());

                addJobTicketDomains(jobTicketDomains, ticketProps.getDomain());

                hasInvoicingOptions = true;
            }
        } else {
            jobTicketDomains = null;
        }

        if (jobTicketDomains == null || jobTicketDomains.isEmpty()) {
            helper.discloseLabel("label-jobticket-domain");
        }

        //
        final Collection<JobTicketUseDto> jobTicketUses;

        if (cm.isConfigValue(Key.JOBTICKET_USES_ENABLE)) {

            jobTicketUses = JOBTICKET_SERVICE.getTicketUsesByName();

            if (!jobTicketUses.isEmpty()) {
                helper.encloseLabel("label-jobticket-use",
                        JobTicketNounEnum.USE.uiText(getLocale()), true);

                MarkupHelper.modifyLabelAttr(
                        helper.addLabel("jobticket-use-option-select",
                                HtmlButtonEnum.SELECT.uiText(getLocale(), true)
                                        .toLowerCase()),
                        MarkupHelper.ATTR_CLASS, CSS_CLASS_JOB_TICKET_DOMAIN);

                addJobTicketUses(isJobTicketDomainsEnable, jobTicketUses);

                hasInvoicingOptions = true;
            }
        } else {
            jobTicketUses = null;
        }

        if (jobTicketUses == null || jobTicketUses.isEmpty()) {
            helper.discloseLabel("label-jobticket-use");
        }

        //
        final Collection<JobTicketTagDto> jobTicketTags;

        if (cm.isConfigValue(Key.JOBTICKET_TAGS_ENABLE)) {

            jobTicketTags = JOBTICKET_SERVICE.getTicketTagsByName();

            if (!jobTicketTags.isEmpty()) {
                helper.encloseLabel("label-jobticket-tag",
                        JobTicketNounEnum.TAG.uiText(getLocale()), true);

                MarkupHelper.modifyLabelAttr(
                        helper.addLabel("jobticket-tag-option-select",
                                HtmlButtonEnum.SELECT.uiText(getLocale(), true)
                                        .toLowerCase()),
                        MarkupHelper.ATTR_CLASS, CSS_CLASS_JOB_TICKET_DOMAIN);
                addJobTicketTags(isJobTicketDomainsEnable, jobTicketTags);

                hasInvoicingOptions = true;
            }
        } else {
            jobTicketTags = null;
        }

        if (jobTicketTags == null || jobTicketTags.isEmpty()) {
            helper.discloseLabel("label-jobticket-tag");
        }

        //
        final WebAppTypeEnum webAppType = getSessionWebAppType();
        final boolean isMailPrintTicketOperator =
                webAppType == WebAppTypeEnum.MAILTICKETS || ConfigManager
                        .isMailPrintTicketingEnabled(userIdDto.getUserId());

        helper.encloseLabel(ID_BTN_PRINT_AND_CLOSE_POPUP,
                HtmlButtonEnum.PRINT.uiText(getLocale(), true),
                isMailPrintTicketOperator);
        helper.encloseLabel(ID_BTN_PRINT_AND_CLOSE,
                this.localized("button-print-and-close"),
                !isMailPrintTicketOperator);

        helper.addLabel("sp-popup-print-and-close-title", NounEnum.COST);

        helper.addButton("btn-print-and-close-popup-cancel",
                HtmlButtonEnum.CANCEL);
        //
        helper.addButton("button-send-jobticket", HtmlButtonEnum.SEND);
        helper.addButton("button-inbox", HtmlButtonEnum.BACK);
        helper.addLabel("label-invoicing", NounEnum.INVOICING);
        helper.addLabel("header-job", PrintOutNounEnum.JOB);

        helper.addModifyLabelAttr("jobticket-copy-pages",
                MarkupHelper.ATTR_TITLE,
                localized("sp-jobticket-copy-pages-tooltip"));

        helper.addLabel("print-page-ranges-label",
                NounEnum.PAGE.uiText(getLocale(), true));

        MarkupHelper.modifyLabelAttr(
                helper.addModifyLabelAttr("print-page-ranges",
                        MarkupHelper.ATTR_PLACEHOLDER,
                        localized("sp-print-page-ranges-placeholder")),
                MarkupHelper.ATTR_TITLE,
                localized("sp-print-page-ranges-tooltip"));

        helper.encloseLabel("print-account-separator", "", hasInvoicingOptions);

        helper.addLabel("title_ticket",
                JobTicketNounEnum.TICKET.uiText(getLocale()));
        helper.addLabel("header-printer", NounEnum.PRINTER);
        helper.addLabel("print-title", NounEnum.TITLE);
        helper.addLabel("print-job-document", NounEnum.DOCUMENT);

        helper.addLabel("print-copies-1",
                PrintOutNounEnum.COPY.uiText(getLocale(), true));
        helper.addLabel("print-copies-2",
                PrintOutNounEnum.COPY.uiText(getLocale(), true));
        helper.addLabel("print-copies-3",
                PrintOutNounEnum.COPY.uiText(getLocale(), true));

        helper.addLabel("prompt-collate",
                PrintOutVerbEnum.COLLATE.uiText(getLocale()));

        helper.addButton("button-cancel", HtmlButtonEnum.CANCEL);
        helper.addButton("button-ok", HtmlButtonEnum.OK);

        helper.addLabel("prompt-jobticket-remark",
                NounEnum.REMARK.uiText(getLocale(), true));
    }

    /**
     * Adds the shared accounts as select options.
     *
     * @param sharedAccounts
     *            The shared accounts.
     * @param allowPersonalPrint
     *            If {@code true} printing on personal account is allowed.
     */
    private void addSharedAccounts(final List<SharedAccountDto> sharedAccounts,
            final boolean allowPersonalPrint) {

        final String listId;
        final String optionId;
        if (allowPersonalPrint) {
            listId = "print-shared-account-option-extra";
            optionId = "option-extra";
        } else {
            listId = "print-shared-account-option-only";
            optionId = "option-only";
        }

        add(new PropertyListView<SharedAccountDto>(listId, sharedAccounts) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<SharedAccountDto> item) {
                final SharedAccountDto dto = item.getModel().getObject();
                final Label label = new Label(optionId, dto.nameAsHtml());
                label.setEscapeModelStrings(false);
                label.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                        dto.getId()));
                item.add(label);
            }
        });
    }

    /**
     * Adds the Job Tickets domains as select options.
     *
     * @param jobTicketDomains
     *            The job ticket domains.
     * @param defaultDomain
     *            Default domain as pre-selected option.
     */
    private void addJobTicketDomains(
            final Collection<JobTicketDomainDto> jobTicketDomains,
            final String defaultDomain) {

        final List<JobTicketDomainDto> domains = new ArrayList<>();

        for (final JobTicketDomainDto domain : jobTicketDomains) {
            domains.add(domain);
        }

        add(new PropertyListView<JobTicketDomainDto>("jobticket-domain-option",
                domains) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void
                    populateItem(final ListItem<JobTicketDomainDto> item) {

                final JobTicketDomainDto dto = item.getModel().getObject();
                final Label label = new Label("option", dto.getName());

                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_VALUE,
                        dto.getId());

                if (defaultDomain != null
                        && dto.getId().equals(defaultDomain)) {
                    MarkupHelper.modifyLabelAttr(label,
                            MarkupHelper.ATTR_SELECTED,
                            MarkupHelper.ATTR_SELECTED);
                }

                item.add(label);
            }
        });
    }

    /**
     * Adds the Job Tickets uses as select options.
     *
     * @param isJobTicketDomainsEnable
     *            If {@code true} job ticket domain label is enabled.
     * @param jobTicketUses
     *            The job ticket uses.
     */
    private void addJobTicketUses(final boolean isJobTicketDomainsEnable,
            final Collection<JobTicketUseDto> jobTicketUses) {

        final List<JobTicketLabelDomainPartDto> labels = new ArrayList<>();

        for (final JobTicketUseDto dto : jobTicketUses) {
            if (isJobTicketDomainsEnable || dto.getDomainIDs().isEmpty()) {
                labels.add(dto);
            }
        }

        addJobTicketLabelDomainPart("jobticket-use-option",
                isJobTicketDomainsEnable, labels);
    }

    /**
     * Adds the Job Tickets tags as select options.
     *
     * @param isJobTicketDomainsEnable
     *            If {@code true} job ticket domain label is enabled.
     * @param jobTicketTags
     *            The job ticket tags.
     */
    private void addJobTicketTags(final boolean isJobTicketDomainsEnable,
            final Collection<JobTicketTagDto> jobTicketTags) {

        final List<JobTicketLabelDomainPartDto> labels = new ArrayList<>();

        for (final JobTicketTagDto dto : jobTicketTags) {
            if (isJobTicketDomainsEnable || dto.getDomainIDs().isEmpty()) {
                labels.add(dto);
            }
        }
        addJobTicketLabelDomainPart("jobticket-tag-option",
                isJobTicketDomainsEnable, labels);
    }

    /**
     *
     * Fills select options.
     *
     * @param wicketID
     *            Wicket ID of option list.
     * @param isJobTicketDomainsEnable
     *            If {@code true} job ticket domain label is enabled.
     * @param labels
     *            The job ticket labels.
     */
    private void addJobTicketLabelDomainPart(final String wicketID,
            final boolean isJobTicketDomainsEnable,
            final List<JobTicketLabelDomainPartDto> labels) {

        add(new PropertyListView<JobTicketLabelDomainPartDto>(wicketID,
                labels) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(
                    final ListItem<JobTicketLabelDomainPartDto> item) {
                final JobTicketLabelDomainPartDto dto =
                        item.getModel().getObject();
                final Label label = new Label("option", dto.getName());
                item.add(label);

                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_VALUE,
                        dto.getId());

                if (!isJobTicketDomainsEnable) {
                    return;
                }

                if (dto.getDomainIDs().isEmpty()) {
                    MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_CLASS,
                            CSS_CLASS_JOB_TICKET_DOMAIN);
                    return;
                }

                boolean first = true;

                for (final String id : dto.getDomainIDs()) {
                    final String cssClass =
                            CSS_CLASS_JOB_TICKET_DOMAIN_PFX.concat(id);
                    if (first) {
                        MarkupHelper.modifyLabelAttr(label,
                                MarkupHelper.ATTR_CLASS, cssClass);
                    } else {
                        MarkupHelper.appendLabelAttr(label,
                                MarkupHelper.ATTR_CLASS, cssClass);
                    }
                    first = false;
                }
            }
        });
    }

}
