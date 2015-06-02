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
package org.savapage.server.pages.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.outbox.OutboxInfoDto;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJob;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.SpSession;
import org.savapage.server.pages.MarkupHelper;

/**
 * A page showing the HOLD proxy print jobs for a user.
 *
 * @author Datraverse B.V.
 *
 */
public class OutboxAddin extends AbstractUserPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private class OutboxJobView extends PropertyListView<OutboxJob> {

        /**
        *
        */
        private static final long serialVersionUID = 1L;

        /**
         *
         * @param id
         * @param list
         */
        public OutboxJobView(String id, List<? extends OutboxJob> list) {
            super(id, list);
        }

        @Override
        protected void populateItem(ListItem<OutboxJob> item) {

            final OutboxJob job = item.getModelObject();

            final MarkupHelper helper = new MarkupHelper(item);

            Label labelWlk;

            /*
             * Fixed attributes.
             */
            item.add(new Label("printer", job.getPrinterName()));
            item.add(new Label("timeSubmit", job.getLocaleInfo()
                    .getSubmitTime()));
            item.add(new Label("timeExpiry", job.getLocaleInfo()
                    .getExpiryTime()));
            //
            /*
             * Totals
             */
            StringBuilder totals = new StringBuilder();

            //
            String key = null;
            int total = job.getPages();
            int copies = job.getCopies();

            //
            totals.append(localizedNumber(total));
            key = (total == 1) ? "page" : "pages";
            totals.append(" ").append(localized(key));

            //
            if (copies > 1) {
                totals.append(", ").append(copies).append(" ")
                        .append(localized("copies"));
            }

            //
            total = job.getSheets();
            if (total > 0) {
                key = (total == 1) ? "sheet" : "sheets";
                totals.append(" (").append(total).append(" ")
                        .append(localized(key)).append(")");
            }

            item.add(new Label("totals", totals.toString()));

            //
            final String sparklineData =
                    String.format("%d,%d", job.getSheets(), job.getPages()
                            * job.getCopies() - job.getSheets());
            item.add(new Label("printout-pie", sparklineData));

            //
            labelWlk =
                    new Label("button-remove", getLocalizer().getString(
                            "button-remove", this));
            labelWlk.add(new AttributeModifier("data-savapage", job.getFile()));
            item.add(labelWlk);

            /*
             * Variable attributes.
             */
            final Map<String, String> mapVisible = new HashMap<>();

            mapVisible.put("title", null);
            mapVisible.put("papersize", null);
            mapVisible.put("letterhead", null);
            mapVisible.put("duplex", null);
            mapVisible.put("singlex", null);
            mapVisible.put("color", null);
            mapVisible.put("grayscale", null);
            mapVisible.put("cost", null);

            /*
             *
             */
            mapVisible.put("title", job.getJobName());

            if (ProxyPrintInboxReq.isDuplex(job.getOptionValues())) {
                mapVisible.put("duplex", localized("duplex"));
            } else {
                mapVisible.put("singlex", localized("singlex"));
            }

            if (ProxyPrintInboxReq.isGrayscale(job.getOptionValues())) {
                mapVisible.put("grayscale", localized("grayscale"));
            } else {
                mapVisible.put("color", localized("color"));
            }

            // mapVisible.put("papersize", obj.get);

            mapVisible.put("cost", job.getLocaleInfo().getCost());

            /*
             * Hide/Show
             */
            for (Map.Entry<String, String> entry : mapVisible.entrySet()) {

                if (entry.getValue() == null) {
                    entry.setValue("");
                }

                helper.encloseLabel(entry.getKey(), entry.getValue(),
                        StringUtils.isNotBlank(entry.getValue()));
            }
        }

    }

    /**
     *
     */
    public OutboxAddin() {

        //this.openServiceContext();

        final OutboxService outboxService =
                ServiceContext.getServiceFactory().getOutboxService();

        final DaoContext daoContext = ServiceContext.getDaoContext();

        final SpSession session = SpSession.get();

        /*
         * Lock user while getting the OutboxInfo.
         */
        daoContext.beginTransaction();

        final org.savapage.core.jpa.User lockedUser =
                daoContext.getUserDao().lock(session.getUser().getId());

        final OutboxInfoDto outboxInfo =
                outboxService.pruneOutboxInfo(lockedUser.getUserId(),
                        ServiceContext.getTransactionDate());

        // unlock
        daoContext.rollback();

        /*
         * Show the OutboxInfo.
         */
        outboxService.applyLocaleInfo(outboxInfo, session.getLocale(),
                SpSession.getAppCurrencySymbol());

        final List<OutboxJob> entryList = new ArrayList<>();

        for (final Entry<String, OutboxJob> entry : outboxInfo.getJobs()
                .entrySet()) {
            entryList.add(entry.getValue());
        }

        add(new OutboxJobView("job-entry", entryList));

    }

}
