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
package org.savapage.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.protocol.http.WebApplication;
import org.savapage.core.config.ConfigManager;

/**
 * Delivers static documentation, like the User Manual (DocBook) and the Third
 * Party License Information.
 *
 * @author Datraverse B.V.
 *
 */
@WebServlet(name = "DocumentationServlet", urlPatterns = { "/docs/*" })
public final class DocumentationServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    private static final int BUFFER_SIZE = 1014;

    /**
     *
     */
    private static final String CONTENT_HOME = ConfigManager.getServerHome()
            + "/data";

    @Override
    protected void doGet(final HttpServletRequest req,
            final HttpServletResponse resp) throws ServletException,
            IOException {

        String reqURI = req.getRequestURI();

        if (reqURI.endsWith("manual") || reqURI.endsWith("licenses")) {
            return;
        }

        if (reqURI.endsWith("manual/") || reqURI.endsWith("licenses/")) {
            reqURI += "index.html";
        }

        InputStream istr = null;

        try {

            final OutputStream ostr = resp.getOutputStream();

            final File file =
                    new File(String.format("%s/%s", CONTENT_HOME, reqURI));

            if (file.exists()) {

                resp.setContentType(WebApplication.get().getMimeType(reqURI));
                resp.setContentLength((int) file.length());

                istr = new FileInputStream(file);
                final byte[] aByte = new byte[BUFFER_SIZE];

                int nBytes = istr.read(aByte);

                while (-1 < nBytes) {
                    ostr.write(aByte, 0, nBytes);
                    nBytes = istr.read(aByte);
                }
            } else {

                final byte[] msg =
                        String.format("%s: not found", reqURI).getBytes();

                resp.setContentType(WebApplication.get().getMimeType("x.txt"));
                resp.setContentLength(msg.length);

                ostr.write(msg);
            }

            resp.setStatus(HttpServletResponse.SC_OK);

        } finally {
            IOUtils.closeQuietly(istr);
        }
    }
}
