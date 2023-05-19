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
package org.savapage.server.restful.dto;

import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.enums.AccountTrxTypeEnum;
import org.savapage.core.dto.DateYmdzDto;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.reports.JrExportFileExtEnum;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public class AccountTrxReportReqDto extends AbstractRestDto {
    /** */
    @JsonProperty("media_type")
    private JrExportFileExtEnum mediaType = JrExportFileExtEnum.CSV;
    /** */
    private Select select;
    /** */
    private Sort sort;

    @JsonInclude(Include.NON_NULL)
    public static class Select {

        @JsonProperty("account_type")
        private AccountTypeEnum accountType;

        @JsonProperty("account_name")
        private String accountName = null;

        @JsonProperty("text")
        private String containingText = null;

        @JsonProperty("trx_type")
        private AccountTrxTypeEnum trxType = null;

        @JsonProperty("date_from")
        private DateYmdzDto dateFrom = null;

        @JsonProperty("date_to")
        private DateYmdzDto dateTo = null;

        public AccountTypeEnum getAccountType() {
            return accountType;
        }

        public void setAccountType(AccountTypeEnum accountType) {
            this.accountType = accountType;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public AccountTrxTypeEnum getTrxType() {
            return trxType;
        }

        public void setTrxType(AccountTrxTypeEnum trxType) {
            this.trxType = trxType;
        }

        public DateYmdzDto getDateFrom() {
            return dateFrom;
        }

        public void setDateFrom(DateYmdzDto dateFrom) {
            this.dateFrom = dateFrom;
        }

        public DateYmdzDto getDateTo() {
            return dateTo;
        }

        public void setDateTo(DateYmdzDto dateTo) {
            this.dateTo = dateTo;
        }

        public String getContainingText() {
            return containingText;
        }

        public void setContainingText(String containingText) {
            this.containingText = containingText;
        }

    }

    @JsonInclude(Include.NON_NULL)
    public static class Sort {

        /** */
        private AccountTrxDao.Field field = AccountTrxDao.Field.TRX_DATE;
        /** */
        private Boolean ascending = true;

        public AccountTrxDao.Field getField() {
            return field;
        }

        public void setField(AccountTrxDao.Field field) {
            this.field = field;
        }

        public Boolean getAscending() {
            return ascending;
        }

        public void setAscending(Boolean ascending) {
            this.ascending = ascending;
        }

    }

    public JrExportFileExtEnum getMediaType() {
        return mediaType;
    }

    public void setMediaType(JrExportFileExtEnum mediaType) {
        this.mediaType = mediaType;
    }

    public Select getSelect() {
        return select;
    }

    public void setSelect(Select select) {
        this.select = select;
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    /**
     * Creates the Select part.
     *
     * @return {@link AccountTrxReportReqDto.Select}.
     */
    public Select createSelect() {
        this.select = new AccountTrxReportReqDto.Select();
        return this.select;
    }

    /**
     * Creates the Sort part.
     *
     * @return {@link AccountTrxReportReqDto.Sort}.
     */
    public Sort createSort() {
        this.sort = new AccountTrxReportReqDto.Sort();
        return this.sort;
    }
}
