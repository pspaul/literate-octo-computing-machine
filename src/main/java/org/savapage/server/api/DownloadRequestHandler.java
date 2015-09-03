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

/**
 * Wrapper for file download.
 *
 * @author Datraverse B.V.
 *
 */
public final class DownloadRequestHandler extends AbstractFileRequestHandler {

    /**
     *
     * @param file
     *            The file to stream.
     */
    public DownloadRequestHandler(final File file) {
        super(null, file);
    }

    @Override
    protected void releaseFile(final String user, final File file) {
        file.delete();
    }
}
