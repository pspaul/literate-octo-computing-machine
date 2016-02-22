/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.helpers.DocLogPagerReq;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.SpSession;
import org.savapage.server.webapp.WebAppTypeEnum;

/**
 *
 * @author Rijk Ravestein
 */
public class DocLogBase extends AbstractAuthPage {

    private static final long serialVersionUID = 1L;

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

        handlePage(webAppType == WebAppTypeEnum.ADMIN);
    }

    /**
     *
     * @param em
     */
    private void handlePage(final boolean adminWebApp) {

        final String data = getParmValue(POST_PARM_DATA);

        DocLogPagerReq req = DocLogPagerReq.read(data);

        Long userId = null;
        Long accountId = null;

        boolean userNameVisible = false;
        boolean accountNameVisible = false;

        if (adminWebApp) {

            userId = req.getSelect().getUserId();
            userNameVisible = (userId != null);

            accountId = req.getSelect().getAccountId();
            accountNameVisible = (accountId != null);

        } else {
            /*
             * If we are called in a User WebApp context we ALWAYS use the user
             * of the current session.
             */
            userId = SpSession.get().getUser().getId();
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

        final String attribute = "value";

        helper.addModifyLabelAttr("select-type-all", attribute,
                DocLogDao.Type.ALL.toString());
        helper.addModifyLabelAttr("select-type-in", attribute,
                DocLogDao.Type.IN.toString());
        helper.addModifyLabelAttr("select-type-out", attribute,
                DocLogDao.Type.OUT.toString());
        helper.addModifyLabelAttr("select-type-pdf", attribute,
                DocLogDao.Type.PDF.toString());
        helper.addModifyLabelAttr("select-type-print", attribute,
                DocLogDao.Type.PRINT.toString());

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
        final Long printerId = req.getSelect().getPrinterId();
        if (printerId != null) {

        }

        /*
         *
         */
        final Long queueId = req.getSelect().getQueueId();
        if (queueId != null) {

        }

    }

    @Override
    protected boolean needMembership() {
        // return isAdminRoleContext();
        return false;
    }
}
