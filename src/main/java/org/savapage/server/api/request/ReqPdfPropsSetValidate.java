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

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.jpa.User;
import org.savapage.core.json.PdfProperties;
import org.savapage.core.services.helpers.PageRangeException;

/**
 * Sets PDF properties and optionally validates page ranges.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPdfPropsSetValidate extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final String jobIndex = this.getParmValue("jobIndex");

        if (StringUtils.isNotBlank(jobIndex)) {
            try {
                INBOX_SERVICE.calcPagesInRanges(
                        INBOX_SERVICE
                                .getInboxInfo(getInboxContext(requestingUser)),
                        Integer.valueOf(jobIndex),
                        StringUtils.defaultString(getParmValue("ranges")),
                        null);
            } catch (PageRangeException e) {
                setApiResultText(ApiResultCodeEnum.ERROR,
                        e.getMessage(getLocale()));
                return;
            }
        }

        final String json = this.getParmValue("props");
        final PdfProperties pdfProp;

        try {
            pdfProp = PdfProperties.create(json);
        } catch (Exception e) {
            setApiResultText(ApiResultCodeEnum.ERROR, e.getMessage());
            return;
        }

        final String pwOwner = pdfProp.getPw().getOwner();
        final String pwUser = pdfProp.getPw().getUser();

        if (StringUtils.isNotBlank(pwOwner) && StringUtils.isNotBlank(pwUser)
                && pwOwner.equals(pwUser)) {
            setApiResult(ApiResultCodeEnum.ERROR,
                    "msg-pdf-identical-owner-user-pw");
            return;
        }

        final User jpaUser;
        if (lockedUser == null) {
            jpaUser = USER_DAO.findById(this.getSessionUserDbKey());
        } else {
            jpaUser = lockedUser;
        }
        USER_SERVICE.setPdfProperties(jpaUser, pdfProp);
        this.setApiResultOk();
    }

}
