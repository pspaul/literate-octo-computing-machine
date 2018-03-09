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
package org.savapage.server.pages.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.UserGroupAccountDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dto.JobTicketTagDto;
import org.savapage.core.dto.SharedAccountDto;
import org.savapage.core.i18n.JobTicketNounEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.InboxSelectScopeEnum;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.EnumRadioPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.QuickSearchPanel;
import org.savapage.server.session.SpSession;

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
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /** */
    private static final UserGroupAccountDao USER_GROUP_ACCOUNT_DAO =
            ServiceContext.getDaoContext().getUserGroupAccountDao();

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
                false);

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
        helper.encloseLabel("prompt-jobticket-delivery-datetime",
                localized("prompt-jobticket-delivery-datetime"),
                cm.isConfigValue(Key.JOBTICKET_DELIVERY_DATETIME_ENABLE));

        //
        final org.savapage.core.jpa.User user = SpSession.get().getUser();

        final boolean isPrintDelegate = ACCESS_CONTROL_SERVICE.hasAccess(user,
                ACLRoleEnum.PRINT_DELEGATE);

        addVisible(isPrintDelegate, "button-print-delegation",
                PrintOutNounEnum.COPY.uiText(getLocale(), true));

        if (isPrintDelegate) {

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
        final Integer privsLetterhead = ACCESS_CONTROL_SERVICE
                .getPrivileges(user, ACLOidEnum.U_LETTERHEAD);

        helper.encloseLabel("prompt-letterhead", localized("prompt-letterhead"),
                privsLetterhead == null || ACLPermissionEnum.READER
                        .isPresent(privsLetterhead.intValue()));

        //
        if (ACCESS_CONTROL_SERVICE.hasAccess(user,
                ACLRoleEnum.JOB_TICKET_CREATOR)
                && !ACCESS_CONTROL_SERVICE.hasAccess(user,
                        ACLRoleEnum.PRINT_CREATOR)) {
            add(new Label("title", localized("title_ticket")));
        } else {
            add(new Label("title", localized("title_print")));
        }

        //
        if (cm.isConfigValue(Key.JOBTICKET_COPIER_ENABLE)) {
            helper.encloseLabel("jobticket-type",
                    localized("jobticket-type-prompt"), true);
            helper.addModifyLabelAttr("jobticket-type-print", "value", "PRINT");
            helper.addModifyLabelAttr("jobticket-type-copy", "value", "COPY");
        } else {
            helper.discloseLabel("jobticket-type");
        }

        //
        final UserGroupAccountDao.ListFilter filter =
                new UserGroupAccountDao.ListFilter();

        filter.setUserId(SpSession.get().getUser().getId());
        filter.setDisabled(Boolean.FALSE);

        final List<SharedAccountDto> sharedAccounts =
                USER_GROUP_ACCOUNT_DAO.getListChunk(filter, null, null);

        if (sharedAccounts == null || sharedAccounts.isEmpty()) {
            helper.discloseLabel("print-account-type");
        } else {
            helper.encloseLabel("print-account-type",
                    localized("account-type-prompt"), true);
            addSharedAccounts(sharedAccounts);
        }

        //
        final Collection<JobTicketTagDto> jobTicketTags;

        if (cm.isConfigValue(Key.JOBTICKET_TAGS_ENABLE)) {

            jobTicketTags = JOBTICKET_SERVICE.getTicketTagsByWord();

            if (!jobTicketTags.isEmpty()) {
                helper.encloseLabel("label-jobticket-tag",
                        JobTicketNounEnum.TAG.uiText(getLocale()), true);
                helper.addLabel("jobticket-tag-option-select",
                        HtmlButtonEnum.SELECT.uiText(getLocale(), true)
                                .toLowerCase());
                addJobTicketTags(jobTicketTags);
            }
        } else {
            jobTicketTags = null;
        }

        if (jobTicketTags == null || jobTicketTags.isEmpty()) {
            helper.discloseLabel("label-jobticket-tag");
        }
        //
        helper.addButton("button-send-jobticket", HtmlButtonEnum.SEND);
        helper.addButton("button-inbox", HtmlButtonEnum.BACK);
        helper.addLabel("label-invoicing", NounEnum.INVOICING);
        helper.addLabel("header-job", PrintOutNounEnum.JOB);

        helper.addModifyLabelAttr("jobticket-copy-pages",
                MarkupHelper.ATTR_TITLE,
                localized("sp-jobticket-copy-pages-tooltip"));
    }

    /**
     * Adds the shared accounts as select options.
     *
     * @param sharedAccounts
     *            The shared accounts.
     */
    private void
            addSharedAccounts(final List<SharedAccountDto> sharedAccounts) {

        add(new PropertyListView<SharedAccountDto>(
                "print-shared-account-option", sharedAccounts) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<SharedAccountDto> item) {
                final SharedAccountDto dto = item.getModel().getObject();
                final Label label = new Label("option", dto.nameAsHtml());
                label.setEscapeModelStrings(false);
                label.add(new AttributeModifier("value", dto.getId()));
                item.add(label);
            }
        });
    }

    /**
     * Adds the Job Tickets tags as select options.
     *
     * @param jobTicketTags
     *            The job ticket tags.
     */
    private void
            addJobTicketTags(final Collection<JobTicketTagDto> jobTicketTags) {

        final List<JobTicketTagDto> tags = new ArrayList<>();

        for (final JobTicketTagDto tag : jobTicketTags) {
            tags.add(tag);
        }

        add(new PropertyListView<JobTicketTagDto>("jobticket-tag-option",
                tags) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<JobTicketTagDto> item) {
                final JobTicketTagDto dto = item.getModel().getObject();
                final Label label = new Label("option", dto.getWord());
                label.add(new AttributeModifier("value", dto.getId()));
                item.add(label);
            }
        });
    }
}
