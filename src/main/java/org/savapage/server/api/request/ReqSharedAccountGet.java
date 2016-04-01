/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.api.request;

import java.io.IOException;

import org.savapage.core.dao.AccountDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.User;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqSharedAccountGet extends ApiRequestMixin {

    /**
     *
     * The request.
     *
     */
    private static class DtoReq extends AbstractDto {

        private Long id;

        public Long getId() {
            return id;
        }

        @SuppressWarnings("unused")
        public void setId(Long id) {
            this.id = id;
        }
    }

    @Override
    protected void
            onRequest(final String requestingUser, final User lockedUser)
                    throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        final AccountDao accountDao =
                ServiceContext.getDaoContext().getAccountDao();

        final Account account = accountDao.findById(dtoReq.getId());

        if (account == null) {

            setApiResult(ApiResultCodeEnum.ERROR, "msg-account-not-found",
                    dtoReq.getId().toString());
            return;
        }

        final AccountingService service =
                ServiceContext.getServiceFactory().getAccountingService();

        final String currencySymbol = "";
        this.setResponse(service.getSharedAccountDisplayInfo(account,
                this.getLocale(), currencySymbol));

        setApiResultOk();
    }

}
