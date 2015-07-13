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
package org.savapage.server.img;

import java.io.File;
import java.io.IOException;

import net.iharder.Base64;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.savapage.core.SpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Our own handler to service the request for a generated image file as BASE64
 * encoded output.
 *
 * @author Datraverse B.V.
 */
public class ImageReqHandlerBase64 extends TextRequestHandler {

    private static final String CONTENT_TYPE_BASE64 = "text/plain";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ImageReqHandlerBase64.class);

    private final File file;

    /**
     *
     * @param file
     */
    public ImageReqHandlerBase64(File file) {

        super(CONTENT_TYPE_BASE64, "UTF-8", getBase64(file));

        this.file = file;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("handle image file [" + file.getAbsolutePath() + "]");
        }
    }

    /**
     *
     * @param file
     * @return
     */
    private static String getBase64(File file) {

        byte[] iconBytes;
        try {
            iconBytes = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            throw new SpException(e);
        }

        return Base64.encodeBytes(iconBytes);
    }

    /**
     * The actual release of the file.
     *
     * @param file
     */
    protected void releaseFile(File image) {
        if (image.exists()) {
            if (image.delete()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("deleted image file ["
                            + image.getAbsolutePath() + "]");
                }
            } else {
                LOGGER.error("delete of image file [" + image.getAbsolutePath()
                        + "] FAILED");
            }
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("image file to be deleted ["
                        + image.getAbsolutePath() + "] does NOT exist");
            }
        }
    }

    @Override
    public void detach(IRequestCycle requestCycle) {
        if (file != null) {
            releaseFile(file);
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("no image file to delete");
            }
        }
        super.detach(requestCycle);
    }

}
