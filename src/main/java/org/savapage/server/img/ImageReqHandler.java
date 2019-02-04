/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * @author Rijk Ravestein
 *
 */
public final class ImageReqHandler extends ResourceStreamRequestHandler {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageReqHandler.class);

    /** Image file. */
    private final File file;

    /**
     *
     * @param image
     *            The image file.
     */
    public ImageReqHandler(final File image) {

        super(new FileResourceStream(image));

        this.file = image;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("handle image file [{}]", image.getAbsolutePath());
        }
    }

    /**
     * The actual release of the file.
     *
     * @param image
     *            Image file.
     */
    protected void releaseFile(final File image) {
        if (image.exists()) {
            if (image.delete()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Deleted image file [{}]",
                            image.getAbsolutePath());
                }
            } else {
                LOGGER.warn("Delete of image file [{}] FAILED.",
                        image.getAbsolutePath());
            }
        } else {
            LOGGER.warn("Image file to be deleted [{}] does NOT exist.",
                    image.getAbsolutePath());
        }
    }

    @Override
    public void detach(final IRequestCycle requestCycle) {
        if (this.file != null) {
            releaseFile(this.file);
        } else {
            LOGGER.warn("No image file to delete");
        }
        super.detach(requestCycle);
    }

}
