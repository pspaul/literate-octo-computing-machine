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
import java.util.List;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.dao.AccountVoucherDao;
import org.savapage.core.dao.AccountVoucherDao.ListFilter;
import org.savapage.core.dao.helpers.AccountVoucherPagerReq;
import org.savapage.core.jpa.AccountVoucher;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountVoucherPage extends AbstractAdminListPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Maximum number of pages in the navigation bar. IMPORTANT: this must be an
     * ODD number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    /**
     *
     */
    public AccountVoucherPage(final PageParameters parameters) {

        super(parameters);

        final String data = getParmValue(POST_PARM_DATA);
        final AccountVoucherPagerReq req = AccountVoucherPagerReq.readReq(data);

        final ListFilter filter = new ListFilter();

        filter.setBatch(req.getSelect().getBatch());
        filter.setExpired(req.getSelect().getExpired());
        filter.setNumber(req.getSelect().getNumber());
        filter.setUsed(req.getSelect().getUsed());
        filter.setUserId(req.getSelect().getUserId());
        filter.setDateFrom(req.getSelect().dateFrom());
        filter.setDateTo(req.getSelect().dateTo());
        filter.setDateNow(new Date());

        // this.openServiceContext();

        //
        final AccountVoucherDao accountVoucherDao =
                ServiceContext.getDaoContext().getAccountVoucherDao();

        final long logCount = accountVoucherDao.getListCount(filter);

        /*
         * Display the requested page.
         */

        // add(new Label("applog-count", Long.toString(logCount)));

        final List<AccountVoucher> entryList = accountVoucherDao.getListChunk(
                filter, req.calcStartPosition(), req.getMaxResults(),
                req.getSort().getField(), req.getSort().getAscending());

        //
        add(new PropertyListView<AccountVoucher>("voucher-entry-view",
                entryList) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<AccountVoucher> item) {

                /*
                 * Step 1: Create panel and add to page.
                 */
                AccountVoucherItemPanel panel =
                        new AccountVoucherItemPanel("voucher-entry");

                item.add(panel);

                /*
                 * Step 2: populate the panel.
                 *
                 * Reason: “If the component is not an instance of Page then it
                 * must be a component that has already been added to a page.”
                 * otherwise it will throw the following warning message.
                 *
                 * 12:07:11,726 WARN [Localizer] Tried to retrieve a localized
                 * string for a component that has not yet been added to the
                 * page.
                 */
                panel.populate(item.getModelObject());
            }
        });

        /*
         * Display the navigation bars and write the response.
         */
        createNavBarResponse(req, logCount, MAX_PAGES_IN_NAVBAR,
                "sp-voucher-page", new String[] { "nav-bar-1", "nav-bar-2" });
    }
}
