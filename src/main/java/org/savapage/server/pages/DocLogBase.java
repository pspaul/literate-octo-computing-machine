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
package org.savapage.server.pages;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.helpers.DocLogPagerReq;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.SpSession;
import org.savapage.server.webapp.WebAppTypeEnum;

/**
 *
 * @author Rijk Ravestein
 */
public class DocLogBase extends AbstractAuthPage {

    private static final long serialVersionUID = 1L;

    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /**
     *
     */
    public DocLogBase(final PageParameters parameters) {

        super(parameters);

        if (isAuthErrorHandled()) {
            return;
        }

        final WebAppTypeEnum webAppType = this.getSessionWebAppType();

        /**
         * If this page is displayed in the Admin WebApp context, we check the
         * admin authentication (including the need for a valid Membership).
         */
        if (webAppType == WebAppTypeEnum.ADMIN) {
            checkAdminAuthorization();
        }

        if (webAppType == WebAppTypeEnum.JOBTICKETS) {

            final User user = SpSession.get().getUser();

            if (user == null || !ACCESS_CONTROL_SERVICE.hasAccess(user,
                    ACLRoleEnum.JOB_TICKET_OPERATOR)) {

                this.setResponsePage(NotAuthorized.class);
                setAuthErrorHandled(true);
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

        if (webAppType == WebAppTypeEnum.ADMIN) {

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
            userId = SpSession.get().getUser().getId();
            userNameVisible = false;
            accountNameVisible = false;
        }

        //
        String userName = null;
        String accountName = null;

        if (userNameVisible) {
            final User user = ServiceContext.getDaoContext().getUserDao()
                    .findById(userId);
            userName = user.getUserId();
        } else if (accountNameVisible) {
            final Account account = ServiceContext.getDaoContext()
                    .getAccountDao().findById(accountId);
            accountName = account.getName();
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
        hiddenLabel = new Label("hidden-account-id");
        hiddenValue = "";

        if (accountId != null) {
            hiddenValue = accountId.toString();
        }
        hiddenLabel.add(new AttributeModifier("value", hiddenValue));
        add(hiddenLabel);

        //
        final MarkupHelper helper = new MarkupHelper(this);

        final String htmlAttrValue = "value";

        helper.addModifyLabelAttr("select-type-all", htmlAttrValue,
                DocLogDao.Type.ALL.toString());
        helper.addModifyLabelAttr("select-type-in", htmlAttrValue,
                DocLogDao.Type.IN.toString());
        helper.addModifyLabelAttr("select-type-out", htmlAttrValue,
                DocLogDao.Type.OUT.toString());
        helper.addModifyLabelAttr("select-type-pdf", htmlAttrValue,
                DocLogDao.Type.PDF.toString());
        helper.addModifyLabelAttr("select-type-print", htmlAttrValue,
                DocLogDao.Type.PRINT.toString());
        //
        final boolean ticketButtonVisible;

        if (webAppType == WebAppTypeEnum.ADMIN
                || webAppType == WebAppTypeEnum.JOBTICKETS) {
            ticketButtonVisible = true;
        } else {
            final User user = SpSession.get().getUser();
            ticketButtonVisible = user != null && ACCESS_CONTROL_SERVICE
                    .hasAccess(user, ACLRoleEnum.JOB_TICKET_CREATOR);
        }

        if (ticketButtonVisible) {
            helper.addModifyLabelAttr("select-type-ticket", htmlAttrValue,
                    DocLogDao.Type.TICKET.toString());
        } else {
            helper.discloseLabel("select-type-ticket");
        }

        //
        helper.encloseLabel("select-and-sort-user", userName, userNameVisible);
        helper.encloseLabel("select-and-sort-account", accountName,
                accountNameVisible);

        /*
         * Option list: Queues
         */
        final IppQueueDao queueDao =
                ServiceContext.getDaoContext().getIppQueueDao();

        final List<IppQueue> queueList =
                queueDao.getListChunk(new IppQueueDao.ListFilter(), null, null,
                        IppQueueDao.Field.URL_PATH, true);

        add(new PropertyListView<IppQueue>("option-list-queues", queueList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<IppQueue> item) {
                final IppQueue queue = item.getModel().getObject();
                final Label label = new Label("option-queue",
                        String.format("/%s", queue.getUrlPath()));
                label.add(new AttributeModifier("value", queue.getId()));
                item.add(label);
            }

        });

        /*
         * Option list: Printers
         */
        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final List<Printer> printerList =
                printerDao.getListChunk(new PrinterDao.ListFilter(), null, null,
                        PrinterDao.Field.DISPLAY_NAME, true);

        add(new PropertyListView<Printer>("option-list-printers", printerList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<Printer> item) {
                final Printer printer = item.getModel().getObject();
                final Label label =
                        new Label("option-printer", printer.getDisplayName());
                label.add(new AttributeModifier("value", printer.getId()));
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

    @Override
    protected boolean needMembership() {
        // return isAdminRoleContext();
        return false;
    }
}
