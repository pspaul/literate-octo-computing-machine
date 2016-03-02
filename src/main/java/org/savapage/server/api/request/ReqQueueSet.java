/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
import java.util.Date;

import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 * Edits or creates a Queue.
 * <p>
 * Also, a logical delete can be applied or reversed.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqQueueSet extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final ReqQueueGet.DtoRsp dtoReq = ReqQueueGet.DtoRsp
                .create(ReqQueueGet.DtoRsp.class, getParmValue("dto"));

        final IppQueueDao ippQueueDao =
                ServiceContext.getDaoContext().getIppQueueDao();

        final boolean isNew = dtoReq.getId() == null;
        final Date now = new Date();
        final String urlPath = dtoReq.getUrlpath().trim();

        /*
         * Note: returns null when logically deleted!!
         */
        final IppQueue jpaQueueDuplicate = ippQueueDao.findByUrlPath(urlPath);

        IppQueue jpaQueue = null;

        boolean isDuplicate = true;

        if (isNew) {

            if (jpaQueueDuplicate == null) {

                jpaQueue = new IppQueue();
                jpaQueue.setCreatedBy(requestingUser);
                jpaQueue.setCreatedDate(now);

                isDuplicate = false;
            }

        } else {

            jpaQueue = ippQueueDao.findById(dtoReq.getId());

            if (jpaQueueDuplicate == null
                    || jpaQueueDuplicate.getId().equals(jpaQueue.getId())) {

                jpaQueue.setModifiedBy(requestingUser);
                jpaQueue.setModifiedDate(now);

                isDuplicate = false;
            }
        }

        if (isDuplicate) {

            setApiResult(ApiResultCodeEnum.ERROR, "msg-queue-duplicate-path",
                    urlPath);

        } else {

            jpaQueue.setUrlPath(urlPath);
            jpaQueue.setIpAllowed(dtoReq.getIpallowed());
            jpaQueue.setTrusted(dtoReq.getTrusted());
            jpaQueue.setDisabled(dtoReq.getDisabled());

            if (isNew) {

                if (QUEUE_SERVICE.isReservedQueue(urlPath)) {

                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-queue-reserved-path", urlPath);

                } else {

                    ippQueueDao.create(jpaQueue);

                    setApiResult(ApiResultCodeEnum.OK, "msg-queue-created-ok");
                }

            } else {

                final boolean isDeleted = dtoReq.getDeleted();

                if (jpaQueue.getDeleted() != isDeleted) {

                    if (isDeleted) {
                        QUEUE_SERVICE.setLogicalDeleted(jpaQueue, now,
                                requestingUser);
                    } else {
                        QUEUE_SERVICE.undoLogicalDeleted(jpaQueue);
                    }
                }

                ippQueueDao.update(jpaQueue);

                setApiResult(ApiResultCodeEnum.OK, "msg-queue-saved-ok");
            }
        }

    }

}
