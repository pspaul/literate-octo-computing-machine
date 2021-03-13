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
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.helpers.DocLogPagerReq;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchFilterMailTicketDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.QuickSearchMailTicketItemDto;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.services.helpers.MailPrintData;
import org.savapage.server.pages.DocLogItem;

/**
 * Mail Ticket Quick Search.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqMailTicketQuickSearch extends ApiRequestMixin {

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoRsp extends AbstractDto {

        private List<QuickSearchItemDto> items;

        @SuppressWarnings("unused")
        public List<QuickSearchItemDto> getItems() {
            return items;
        }

        public void setItems(List<QuickSearchItemDto> items) {
            this.items = items;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final QuickSearchFilterMailTicketDto dto = AbstractDto.create(
                QuickSearchFilterMailTicketDto.class, this.getParmValueDto());

        //
        final DocLogPagerReq req = new DocLogPagerReq();
        req.setMaxResults(dto.getMaxResults());
        req.setTicketNumberMailView(Boolean.TRUE);

        final DocLogPagerReq.Sort sort = new DocLogPagerReq.Sort();
        sort.setField(DocLogPagerReq.Sort.FLD_NAME);
        req.setSort(sort);

        final DocLogPagerReq.Select select = new DocLogPagerReq.Select();
        select.setDocType(DocLogDao.Type.IN);
        select.setTicketNumberMail(dto.getFilter());

        req.setSelect(select);

        final EntityManager em = DaoContextImpl.peekEntityManager();
        final DocLogItem.AbstractQuery query =
                DocLogItem.createQuery(DocLogDao.Type.IN);

        //
        final List<QuickSearchItemDto> resultList = new ArrayList<>();
        int nPage = 0;

        while (true) {

            nPage++;
            req.setPage(nPage);

            final List<DocLogItem> itemListWlk =
                    query.getListChunk(em, dto.getUserId(), req, getLocale());

            for (final DocLogItem log : itemListWlk) {

                final boolean hasDocStore =
                        log.isPrintArchive() || log.isPrintJournal();

                if (dto.isDocStore() && !hasDocStore) {
                    continue;
                }

                final QuickSearchMailTicketItemDto item =
                        new QuickSearchMailTicketItemDto();

                item.setKey(log.getDocLogId());
                item.setText(log.getExtId());
                item.setEmail(MailPrintData.createFromData(log.getExtData())
                        .getFromAddress());
                item.setDocStore(hasDocStore);

                if (log.getPrintOutOfDocIn() != null
                        && log.getPrintOutOfDocIn().size() > 0) {
                    item.setPrintOutJobs(log.getPrintOutOfDocIn().size());
                }
                item.setPaperSize(log.getPaperSize());
                item.setTitle(log.getTitle());
                item.setPages(String.format("%d %s", log.getTotalPages(),
                        NounEnum.PAGE.uiText(getLocale(),
                                log.getTotalPages() > 1)));
                item.setByteCount(log.getHumanReadableByteCount());

                resultList.add(item);

                if (resultList.size() == dto.getMaxResults()) {
                    break;
                }
            }

            if (resultList.size() == dto.getMaxResults()
                    || itemListWlk.size() < dto.getMaxResults()) {
                break;
            }
        }

        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(resultList);

        setResponse(rsp);
        setApiResultOk();
    }

}
