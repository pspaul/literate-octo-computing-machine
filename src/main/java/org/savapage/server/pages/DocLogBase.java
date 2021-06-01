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
package org.savapage.server.pages;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.dao.helpers.DocLogPagerReq;
import org.savapage.core.dao.helpers.IppQueueHelper;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.JobTicketNounEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.i18n.PrepositionEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.DocLogScopeEnum;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocLogBase extends AbstractAuthPage {

    private static final long serialVersionUID = 1L;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();

    /** */
    private static final ConfigManager CONFIG_MNGR = ConfigManager.instance();

    /** */
    private static final String WICKET_ID_PROMPT_LETTERHEAD =
            "prompt-letterhead";
    /** */
    private static final String WICKET_ID_SELECT_AND_SORT_USER =
            "select-and-sort-user";
    /** */
    private static final String WICKET_ID_SELECT_AND_SORT_ACCOUNT =
            "select-and-sort-account";

    /**
     * @param parameters
     *            The page parameters.
     */
    public DocLogBase(final PageParameters parameters) {

        super(parameters);

        final WebAppTypeEnum webAppType = this.getSessionWebAppType();

        if (webAppType == WebAppTypeEnum.JOBTICKETS) {

            if (!ACCESS_CONTROL_SERVICE.hasAccess(
                    SpSession.get().getUserIdDto(),
                    ACLRoleEnum.JOB_TICKET_OPERATOR)) {
                throw new RestartResponseException(NotAuthorized.class);
            }

        } else if (webAppType == WebAppTypeEnum.MAILTICKETS) {

            if (!ACCESS_CONTROL_SERVICE.hasAccess(
                    SpSession.get().getUserIdDto(),
                    ACLRoleEnum.MAIL_TICKET_OPERATOR)) {
                throw new RestartResponseException(NotAuthorized.class);
            }

        } else if (webAppType == WebAppTypeEnum.PRINTSITE) {

            if (!ACCESS_CONTROL_SERVICE.hasAccess(
                    SpSession.get().getUserIdDto(),
                    ACLRoleEnum.PRINT_SITE_OPERATOR)) {
                throw new RestartResponseException(NotAuthorized.class);
            }
        }

        handlePage(webAppType);
    }

    /**
     *
     * @param webAppType
     *            The Web App Type this page is part of.
     */
    private void handlePage(final WebAppTypeEnum webAppType) {

        final String data = getParmValue(POST_PARM_DATA);

        final DocLogPagerReq req = DocLogPagerReq.read(data);

        final Long userId;
        Long accountId = null;

        final boolean userNameVisible;
        final boolean accountNameVisible;

        if (webAppType == WebAppTypeEnum.ADMIN
                || webAppType == WebAppTypeEnum.PRINTSITE) {

            userId = req.getSelect().getUserId();
            userNameVisible = (userId != null);

            accountId = req.getSelect().getAccountId();
            accountNameVisible = (accountId != null);

        } else if (webAppType == WebAppTypeEnum.JOBTICKETS) {

            userId = null;
            userNameVisible = false;
            accountNameVisible = false;
        } else {
            /*
             * If we are called in a User WebApp context we ALWAYS use the user
             * of the current session.
             */
            userId = SpSession.get().getUserDbKeyDocLog();
            userNameVisible = false;
            accountNameVisible = false;
        }

        //
        String userName = null;
        String accountName = null;

        final String userNameCheck;

        if (userNameVisible) {

            final User user = ServiceContext.getDaoContext().getUserDao()
                    .findById(userId);
            userName = user.getUserId();
            userNameCheck = userName;

        } else if (accountNameVisible) {

            final Account account = ServiceContext.getDaoContext()
                    .getAccountDao().findById(accountId);
            accountName = account.getName();
            userNameCheck = null;

        } else {
            userNameCheck = SpSession.get().getUserIdDocLog();
        }

        final boolean isMailPrintTicketOperator = userNameCheck != null
                && ConfigManager.isMailPrintTicketOperator(userNameCheck);

        //
        Label hiddenLabel = new Label("hidden-user-id");
        final String hiddenUserId;
        if (userId == null) {
            hiddenUserId = "";
        } else {
            hiddenUserId = userId.toString();
        }
        hiddenLabel.add(
                new AttributeModifier(MarkupHelper.ATTR_VALUE, hiddenUserId));
        add(hiddenLabel);

        //
        hiddenLabel = new Label("hidden-account-id");
        final String hiddenAccountId;
        if (accountId == null) {
            hiddenAccountId = "";
        } else {
            hiddenAccountId = accountId.toString();
        }
        hiddenLabel.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                hiddenAccountId));
        add(hiddenLabel);

        //
        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("select-and-sort", PhraseEnum.SELECT_AND_SORT);

        helper.addLabel("prompt-type", NounEnum.TYPE);
        helper.addLabel("prompt-document-name", NounEnum.DOCUMENT);

        helper.encloseLabel("prompt-ticket-number-mail",
                JobTicketNounEnum.TICKET.uiText(getLocale()),
                isMailPrintTicketOperator);

        helper.addLabel("prompt-period", NounEnum.PERIOD);
        helper.addLabel("prompt-period-from", PrepositionEnum.FROM_TIME);
        helper.addLabel("prompt-period-to", PrepositionEnum.TO_TIME);

        helper.addLabel("prompt-author", NounEnum.AUTHOR);
        helper.addLabel("prompt-subject", NounEnum.SUBJECT);
        helper.addLabel("prompt-keywords",
                NounEnum.KEYWORD.uiText(getLocale(), true));
        helper.addLabel("prompt-encryption", NounEnum.ENCRYPTION);
        helper.addLabel("prompt-layout", NounEnum.LAYOUT);

        helper.addLabel("encryp-option-yes",
                HtmlButtonEnum.YES.uiText(getLocale()));
        helper.addLabel("encryp-option-no",
                HtmlButtonEnum.NO.uiText(getLocale()));

        helper.addLabel("cat-queue", NounEnum.QUEUE);

        helper.encloseLabel("prompt-destination",
                NounEnum.DESTINATION.uiText(getLocale()),
                !isMailPrintTicketOperator
                        && (webAppType == WebAppTypeEnum.ADMIN
                                || webAppType == WebAppTypeEnum.PRINTSITE));

        helper.addLabel("option-simplex", PrintOutNounEnum.SIMPLEX);
        helper.addLabel("option-duplex", PrintOutNounEnum.DUPLEX);

        helper.addLabel("prompt-sort-by", NounEnum.SORTING);
        helper.addLabel("sort-by-name", NounEnum.NAME);
        helper.addLabel("sort-by-date", NounEnum.DATE);

        helper.addLabel("sort-asc", AdjectiveEnum.ASCENDING);
        helper.addLabel("sort-desc", AdjectiveEnum.DESCENDING);

        helper.addButton("button-apply", HtmlButtonEnum.APPLY);
        helper.addButton("button-default", HtmlButtonEnum.DEFAULT);

        //
        final boolean btnVisiblePdf;
        final boolean btnVisiblePrint;
        final boolean btnVisibleTicket;
        final boolean visibleLetterhead;

        DocLogScopeEnum defaultScope = DocLogScopeEnum.ALL;

        if (isMailPrintTicketOperator) {
            defaultScope = DocLogScopeEnum.IN;
        }

        final List<Printer> printerList = getPrinterList(webAppType);

        if (webAppType == WebAppTypeEnum.ADMIN) {

            btnVisiblePdf = !isMailPrintTicketOperator;
            btnVisiblePrint = true;
            btnVisibleTicket = !isMailPrintTicketOperator;
            visibleLetterhead = !isMailPrintTicketOperator;

        } else if (webAppType == WebAppTypeEnum.JOBTICKETS) {

            defaultScope = DocLogScopeEnum.TICKET;

            btnVisiblePdf = false;
            btnVisiblePrint = false;
            btnVisibleTicket = true;
            visibleLetterhead = false;

        } else if (webAppType == WebAppTypeEnum.PRINTSITE) {

            defaultScope = DocLogScopeEnum.PRINT;

            btnVisiblePdf = false;
            btnVisiblePrint = true;
            btnVisibleTicket = false;
            visibleLetterhead = false;

        } else {

            final UserIdDto userIdDto = SpSession.get().getUserIdDto();

            final List<ACLPermissionEnum> permissions = ACCESS_CONTROL_SERVICE
                    .getPermission(userIdDto, ACLOidEnum.U_INBOX);

            visibleLetterhead = userIdDto != null
                    && (permissions == null || ACCESS_CONTROL_SERVICE
                            .hasAccess(userIdDto, ACLOidEnum.U_LETTERHEAD));

            btnVisiblePdf = userIdDto != null && (permissions == null
                    || ACCESS_CONTROL_SERVICE.hasPermission(permissions,
                            ACLPermissionEnum.DOWNLOAD)
                    || ACCESS_CONTROL_SERVICE.hasPermission(permissions,
                            ACLPermissionEnum.SEND));

            btnVisiblePrint = !printerList.isEmpty() && userIdDto != null
                    && ACCESS_CONTROL_SERVICE.hasAccess(userIdDto,
                            ACLRoleEnum.PRINT_CREATOR);

            btnVisibleTicket = userIdDto != null && ACCESS_CONTROL_SERVICE
                    .hasAccess(userIdDto, ACLRoleEnum.JOB_TICKET_CREATOR);

            final List<DocLogScopeEnum> typeDefaultOrder =
                    CONFIG_MNGR.getConfigEnumList(DocLogScopeEnum.class,
                            Key.WEBAPP_USER_DOCLOG_SELECT_TYPE_DEFAULT_ORDER);

            DocLogScopeEnum scopeSecondChoice = null;

            for (final DocLogScopeEnum scope : typeDefaultOrder) {
                if ((scope == DocLogScopeEnum.PDF && btnVisiblePdf)
                        || (scope == DocLogScopeEnum.PRINT && btnVisiblePrint)
                        || (scope == DocLogScopeEnum.TICKET
                                && btnVisibleTicket)) {
                    defaultScope = scope;
                    break;
                }
                if (scopeSecondChoice == null && (scope == DocLogScopeEnum.IN
                        || scope == DocLogScopeEnum.OUT)) {
                    scopeSecondChoice = scope;
                }
            }

            if (defaultScope == DocLogScopeEnum.ALL
                    && scopeSecondChoice != null) {
                defaultScope = scopeSecondChoice;
            }
        }

        Label selectTypeToCheck = helper.addModifyLabelAttr("select-type-all",
                MarkupHelper.ATTR_VALUE, DocLogDao.Type.ALL.toString());

        Label labelWlk;

        labelWlk = helper.addModifyLabelAttr("select-type-in",
                MarkupHelper.ATTR_VALUE, DocLogDao.Type.IN.toString());
        if (defaultScope == DocLogScopeEnum.IN) {
            selectTypeToCheck = labelWlk;
        }

        if (isMailPrintTicketOperator) {
            helper.discloseLabel("select-type-out");
        } else {
            labelWlk = helper.addModifyLabelAttr("select-type-out",
                    MarkupHelper.ATTR_VALUE, DocLogDao.Type.OUT.toString());
            if (defaultScope == DocLogScopeEnum.OUT) {
                selectTypeToCheck = labelWlk;
            }
        }

        if (btnVisiblePdf) {
            labelWlk = helper.addModifyLabelAttr("select-type-pdf",
                    MarkupHelper.ATTR_VALUE, DocLogDao.Type.PDF.toString());
            if (defaultScope == DocLogScopeEnum.PDF) {
                selectTypeToCheck = labelWlk;
            }
        } else {
            helper.discloseLabel("select-type-pdf");
        }

        if (btnVisiblePrint) {
            labelWlk = helper.addModifyLabelAttr("select-type-print",
                    MarkupHelper.ATTR_VALUE, DocLogDao.Type.PRINT.toString());
            if (defaultScope == DocLogScopeEnum.PRINT) {
                selectTypeToCheck = labelWlk;
            }
        } else {
            helper.discloseLabel("select-type-print");
        }

        if (btnVisibleTicket) {
            labelWlk = helper.addModifyLabelAttr("select-type-ticket",
                    MarkupHelper.ATTR_VALUE, DocLogDao.Type.TICKET.toString());
            if (defaultScope == DocLogScopeEnum.TICKET) {
                selectTypeToCheck = labelWlk;
            }
        } else {
            helper.discloseLabel("select-type-ticket");
        }

        MarkupHelper.modifyLabelAttr(selectTypeToCheck,
                MarkupHelper.ATTR_CHECKED, MarkupHelper.ATTR_CHECKED);

        //
        helper.encloseLabel(WICKET_ID_PROMPT_LETTERHEAD,
                NounEnum.LETTERHEAD.uiText(getLocale()), visibleLetterhead);

        if (visibleLetterhead) {
            helper.addLabel("lh-option-yes",
                    HtmlButtonEnum.YES.uiText(getLocale()));
            helper.addLabel("lh-option-no",
                    HtmlButtonEnum.NO.uiText(getLocale()));
        }

        //
        helper.encloseLabel(WICKET_ID_SELECT_AND_SORT_USER, userName,
                userNameVisible);
        if (userNameVisible) {
            helper.addLabel("select-and-sort-user-prompt", NounEnum.USER);
        }

        helper.encloseLabel(WICKET_ID_SELECT_AND_SORT_ACCOUNT, accountName,
                accountNameVisible);
        if (accountNameVisible) {
            helper.addLabel("select-and-sort-account-prompt",
                    PrintOutNounEnum.ACCOUNT.uiText(getLocale()));
        }

        /*
         * Option list: Queues
         */
        final List<IppQueue> queueList =
                getQueueList(webAppType, isMailPrintTicketOperator);

        helper.encloseLabel("option-queue-all", localized("option-all"),
                queueList.size() > 1);
        helper.encloseLabel("sort-by-queue", NounEnum.QUEUE.uiText(getLocale()),
                queueList.size() > 1);

        add(new PropertyListView<IppQueue>("option-list-queues", queueList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<IppQueue> item) {
                final IppQueue queue = item.getModel().getObject();
                final Label label =
                        new Label("option-queue", IppQueueHelper.uiPath(queue));
                label.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                        queue.getId()));
                item.add(label);
            }
        });

        /*
         * Option list: Printers
         */
        helper.encloseLabel("option-printer-all", localized("option-all"),
                printerList.size() > 1);
        helper.encloseLabel("sort-by-printer",
                NounEnum.PRINTER.uiText(getLocale()), printerList.size() > 1);

        add(new PropertyListView<Printer>("option-list-printers", printerList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<Printer> item) {
                final Printer printer = item.getModel().getObject();
                final Label label =
                        new Label("option-printer", printer.getDisplayName());
                label.add(new AttributeModifier(MarkupHelper.ATTR_VALUE,
                        printer.getId()));
                item.add(label);
            }
        });

        /*
         *
         */
        if (req.getSelect() != null) {

            final Long printerId = req.getSelect().getPrinterId();
            if (printerId != null) {

            }

            final Long queueId = req.getSelect().getQueueId();
            if (queueId != null) {

            }
        }

    }

    /**
     * Gets the Queue list.
     *
     * @param webAppType
     *            The Web App Type.
     * @param isMailPrintTicketOperator
     *            {{@code true} if list is for Mail Print Ticket Operator.
     * @return The list.
     */
    private static List<IppQueue> getQueueList(final WebAppTypeEnum webAppType,
            final boolean isMailPrintTicketOperator) {

        final IppQueueDao dao = ServiceContext.getDaoContext().getIppQueueDao();

        final IppQueueDao.ListFilter filter = new IppQueueDao.ListFilter();

        if (webAppType == WebAppTypeEnum.USER
                || webAppType == WebAppTypeEnum.PRINTSITE) {
            filter.setDeleted(Boolean.FALSE);
            filter.setDisabled(Boolean.FALSE);
        }
        if (isMailPrintTicketOperator) {
            filter.setContainingText(
                    ReservedIppQueueEnum.MAILPRINT.getUrlPath());
        }

        final List<IppQueue> list = dao.getListChunk(filter, null, null,
                IppQueueDao.Field.URL_PATH, true);

        if (webAppType == WebAppTypeEnum.USER
                || webAppType == WebAppTypeEnum.PRINTSITE) {
            final Iterator<IppQueue> iter = list.iterator();
            while (iter.hasNext()) {
                if (!QUEUE_SERVICE.isActiveQueue(iter.next())) {
                    iter.remove();
                }
            }
        }
        return list;
    }

    /**
     * Gets the Printer list.
     *
     * @param webAppType
     *            The Web App Type.
     * @return The list.
     */
    private static List<Printer>
            getPrinterList(final WebAppTypeEnum webAppType) {

        final PrinterDao dao = ServiceContext.getDaoContext().getPrinterDao();

        final PrinterDao.ListFilter filter = new PrinterDao.ListFilter();

        filter.setJobTicket(Boolean.FALSE);

        if (webAppType == WebAppTypeEnum.USER
                || webAppType == WebAppTypeEnum.PRINTSITE) {
            filter.setDeleted(Boolean.FALSE);
            filter.setDisabled(Boolean.FALSE);
            filter.setInternal(Boolean.FALSE);
        }

        return dao.getListChunk(filter, null, null,
                PrinterDao.Field.DISPLAY_NAME, true);
    }

    @Override
    protected boolean needMembership() {
        return false;
    }
}
