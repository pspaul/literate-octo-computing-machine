/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.server.api;

import java.io.File;

import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.util.resource.FileResourceStream;

/**
 * Our own Wicket handler to control the release of transient files used for
 * streaming (like JPEG images to display in a webpage and PDF files to
 * download.
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractFileRequestHandler extends
        ResourceStreamRequestHandler {

    private final String user;
    private final File file;

    /**
     *
     * @param user
     *            The userId.
     * @param file
     *            The file to stream.
     */
    public AbstractFileRequestHandler(final String user, File file) {
        super(new FileResourceStream(file));
        this.file = file;
        this.user = user;
    }

    /**
     * The actual release of the file to be implemented by any concrete
     * subclass.
     *
     * @param userId
     *            The userId.
     * @param file
     *            The file to stream.
     */
    protected abstract void releaseFile(String user, File file);

    @Override
    public void detach(IRequestCycle requestCycle) {
        releaseFile(user, file);
        super.detach(requestCycle);
    }
}
