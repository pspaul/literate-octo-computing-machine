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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.codehaus.jackson.map.ObjectMapper;
import org.savapage.core.SpException;
import org.savapage.core.dao.helpers.AbstractPagerReq;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractListPage extends AbstractAuthPage {

    private static final long serialVersionUID = 1L;

    public AbstractListPage(final PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * Get the ordinal of last page in the navigation bar.
     *
     * @param totItems
     *            Total number of items.
     * @param maxItemsOnPage
     *            Max number of items on a page.
     * @return Number of pages.
     */
    protected final long getNavBarLastPage(final long totItems,
            final int maxItemsOnPage) {
        long totPages = totItems / maxItemsOnPage;
        if (totItems % maxItemsOnPage > 0) {
            totPages++;
        }
        return totPages;
    }

    /**
     * Gets the page ordinals in the navigation bar.
     *
     * @param totItems
     *            Total number of items.
     * @param maxItemsOnPage
     *            Max number of items on a page.
     * @param nPage
     *            Ordinal of current age in view.
     * @param maxPagesInBar
     *            The maximum number of pages in the navigation bar.
     * @return List with ordinal numbers of the pages in view.
     */
    protected final List<Long> getNavBarPages(final long totItems,
            final int maxItemsOnPage, final long nPage,
            final int maxPagesInBar) {

        /*
         * Constants
         */
        final int iPageCenter = maxPagesInBar / 2;

        /*
         * Calculating the page view
         */
        long totPages = getNavBarLastPage(totItems, maxItemsOnPage);

        long nPageLast = nPage + iPageCenter;
        if (nPageLast > totPages) {
            nPageLast = totPages;
        }

        long nPageFirst = nPageLast - maxPagesInBar + 1;
        if (nPageFirst <= 0) {
            nPageFirst = 1;
            nPageLast = maxPagesInBar;
            if (nPageLast > totPages) {
                nPageLast = totPages;
            }
        }

        List<Long> pages = new ArrayList<>();
        for (long i = nPageFirst; i <= nPageLast; i++) {
            pages.add(i);
        }

        return pages;
    }

    /**
     * Writes JSON response to a hidden HTML tag.
     *
     * @param nPrevPage
     *            Ordinal of the previous page.
     * @param nNextPage
     *            Ordinal of the next page.
     * @param nLastPage
     *            Ordinal of the last page.
     */
    protected final void writePagerRsp(final long nPrevPage,
            final long nNextPage, final long nLastPage) {
        String jsonRsp = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            PagerRsp rsp = new PagerRsp();
            rsp.setLastPage(nLastPage);
            rsp.setNextPage(nNextPage);
            rsp.setPrevPage(nPrevPage);

            jsonRsp = mapper.writeValueAsString(rsp);
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }
        add(new Label("json-rsp", jsonRsp));
    }

    /**
     * Displays the navigation bar(s) and writes the page response.
     *
     * @param req
     *            The requested page.
     * @param totItems
     *            Total number of items.
     * @param maxPagesInBar
     *            The maximum number of pages in the navigation bar.
     * @param cssClassBase
     *            The CSS class name used for the navigation buttons. See
     *            {@linkplain PageNavPanel}.
     * @param navBarIds
     *            Array with wicket:id's of the {@linkplain PageNavPanel}
     *            components.
     */
    protected final void createNavBarResponse(AbstractPagerReq req,
            final long totItems, final int maxPagesInBar,
            final String cssClassBase, String[] navBarIds) {

        List<Long> list = getNavBarPages(totItems, req.getMaxResults(),
                req.getPage(), maxPagesInBar);

        final Long nPage = req.getPage().longValue();

        final long nLastPage = getNavBarLastPage(totItems, req.getMaxResults());

        long nNextPage = nPage;
        if (nNextPage < nLastPage) {
            nNextPage++;
        }
        long nPrevPage = nPage - 1;
        if (nPrevPage < 1) {
            nPrevPage = 1;
        }

        for (final String id : navBarIds) {
            add(new PageNavPanel(id, list, cssClassBase, nPage, nLastPage));
        }

        /*
         *
         */
        writePagerRsp(nPrevPage, nNextPage, nLastPage);

    }
}
