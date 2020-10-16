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

import javax.mail.MessagingException;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.services.EmailService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.email.EmailMsgParms;
import org.savapage.core.util.EmailValidator;
import org.savapage.lib.pgp.PGPBaseException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Sends a test email message.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqMailTest extends ApiRequestMixin {

    /** */
    private static final EmailService EMAIL_SERVICE =
            ServiceContext.getServiceFactory().getEmailService();

    /**
     * .
     */
    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private String emailAddress;

        public String getEmailAddress() {
            return emailAddress;
        }

        @SuppressWarnings("unused")
        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq =
                DtoReq.create(DtoReq.class, this.getParmValueDto());

        final String mailto = dtoReq.getEmailAddress();

        if (StringUtils.isBlank(mailto) || !EmailValidator.validate(mailto)) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-email-invalid",
                    StringUtils.defaultString(mailto));
            return;
        }

        final String subject = localize("mail-test-subject");
        final String body = localize("mail-test-body", requestingUser,
                CommunityDictEnum.SAVAPAGE.getWord() + " "
                        + ConfigManager.getAppVersion());

        try {

            final EmailMsgParms emailParms = new EmailMsgParms();

            emailParms.setToAddress(mailto);
            emailParms.setSubject(subject);
            emailParms.setBodyInStationary(subject, body, getLocale(), true);

            EMAIL_SERVICE.sendEmail(emailParms);

            setApiResult(ApiResultCodeEnum.OK, "msg-mail-sent", mailto);

        } catch (MessagingException | IOException | InterruptedException
                | CircuitBreakerException | PGPBaseException e) {

            String msg = e.getMessage();

            if (e.getCause() != null) {
                msg += " (" + e.getCause().getMessage() + ")";
            }
            setApiResult(ApiResultCodeEnum.ERROR, "msg-single-parm", msg);
        }
    }

}
