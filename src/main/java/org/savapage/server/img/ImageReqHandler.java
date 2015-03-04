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

import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.util.resource.FileResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Our own handler to service the request for a generated image file.
 *
 * @author Datraverse B.V.
 */
public class ImageReqHandler extends ResourceStreamRequestHandler {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ImageReqHandler.class);

    private final File file;

    /**
     *
     * @param file
     */
    public ImageReqHandler(File file) {
        super(new FileResourceStream(file));
        this.file = file;
        LOGGER.trace("handle image file [" + file.getAbsolutePath() + "]");
    }

    /**
     * The actual release of the file.
     *
     * @param file
     */
    protected void releaseFile(File image) {
        if (image.exists()) {
            if (image.delete()) {
                LOGGER.trace("deleted image file [" + image.getAbsolutePath()
                        + "]");
            } else {
                LOGGER.warn("delete of image file [" + image.getAbsolutePath()
                        + "] FAILED");
            }
        } else {
            LOGGER.warn("image file to be deleted [" + image.getAbsolutePath()
                    + "] does NOT exist");
        }

    }

    @Override
    public void detach(IRequestCycle requestCycle) {
        if (file != null) {
            releaseFile(file);
        } else {
            LOGGER.warn("no image file to delete");
        }
        super.detach(requestCycle);
    }

}
