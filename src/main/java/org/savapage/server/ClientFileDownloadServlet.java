/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.protocol.http.WebApplication;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;

/**
 * Delivers public client files for downloading. Single files are delivered as
 * such. Directory are delivered as zip file.
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "ClientFileDownloadServlet",
        urlPatterns = { ClientFileDownloadServlet.URL_PATH + "*" })
public final class ClientFileDownloadServlet extends HttpServlet {

    /**
     * .
     */
    private static final String CLIENT_DIR_NAME = "client";

    /**
     * .
     */
    public static final String URL_PATH = "/" + CLIENT_DIR_NAME + "/";

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     * .
     */
    private static final int BUFFER_SIZE = 1014;

    @Override
    protected void doGet(final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, IOException {

        final Path pathDownload =
                FileSystems.getDefault().getPath(ConfigManager.getClientHome(),
                        StringUtils.removeStart(req.getRequestURI(), URL_PATH));

        final File file = pathDownload.toFile();

        if (!file.exists()) {

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);

        } else if (file.isDirectory()) {

            File tmpFile = null;

            try {
                tmpFile = onDownloadZippedDir(file, resp);
            } finally {
                if (tmpFile != null) {
                    tmpFile.delete();
                    tmpFile = null;
                }
            }
        } else {
            onDownloadFile(file, resp, file.getName());
        }
    }

    /**
     *
     * @param file
     *            The {@link File}.
     * @return The HTML content type of the file.
     */
    private static String getContentType(final File file) {
        return WebApplication.get().getMimeType(file.getName());
    }

    /**
     * Handles download request of a single file by streaming {@link File} to
     * {@link HttpServletResponse#getOutputStream()}.
     *
     * @param file
     *            The {@link File}.
     * @param resp
     *            The {@link HttpServletResponse}.
     * @param attachmentFilename
     *            The file attachment name.
     * @throws IOException
     *             When IO error.
     */
    private static void onDownloadFile(final File file,
            final HttpServletResponse resp, final String attachmentFilename)
            throws IOException {

        /*
         * Before streaming content, set content characteristics.
         */
        resp.setHeader("Content-Disposition", String
                .format("attachment; filename=\"%s\"", attachmentFilename));

        resp.setContentType(getContentType(file));
        resp.setContentLength((int) file.length());

        //
        try (InputStream istr = new FileInputStream(file);) {

            final OutputStream ostr = resp.getOutputStream();

            final byte[] aByte = new byte[BUFFER_SIZE];

            int nBytes = istr.read(aByte);

            while (-1 < nBytes) {
                ostr.write(aByte, 0, nBytes);
                nBytes = istr.read(aByte);
            }

            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    /**
     * Handles download request of a directory by streaming zip {@link File}
     * with directory content to {@link HttpServletResponse#getOutputStream()}.
     *
     * @param dir
     *            The directory {@link File} to download as zip file.
     * @param resp
     *            The {@link HttpServletResponse}.
     * @return The zip {@link File}, fully streamed to the
     *         {@link HttpServletResponse#getOutputStream()}.
     * @throws IOException
     *             When IO error.
     */
    private static File onDownloadZippedDir(final File dir,
            final HttpServletResponse resp) throws IOException {

        final File zipFile =
                FileSystems.getDefault().getPath(ConfigManager.getAppTmpDir(),
                        UUID.randomUUID().toString()).toFile();

        final File clientDir = new File(ConfigManager.getClientHome());

        try (ZipOutputStream zostr =
                new ZipOutputStream(new FileOutputStream(zipFile));) {
            addDir(clientDir, dir, zostr);
        }

        final StringBuilder attachmentFilename = new StringBuilder();

        attachmentFilename
                .append(CommunityDictEnum.SAVAPAGE.getWord().toLowerCase())
                .append("-").append(CLIENT_DIR_NAME);

        if (!clientDir.getAbsolutePath().equals(dir.getAbsolutePath())) {
            attachmentFilename.append("-").append(dir.getName());
        }

        attachmentFilename.append(".zip");

        onDownloadFile(zipFile, resp, attachmentFilename.toString());

        return zipFile;
    }

    /**
     * Adds a directory to {@link ZipOutputStream}.
     *
     * @param clientDir
     *            The client directory.
     * @param dirObj
     *            The directory to add to the zip.
     * @param zostr
     *            The {@link ZipOutputStream}.
     * @throws IOException
     *             When IO error.
     */
    private static void addDir(final File clientDir, final File dirObj,
            final ZipOutputStream zostr) throws IOException {

        final File[] files = dirObj.listFiles();

        if (files == null) {
            return;
        }

        byte[] aByte = new byte[BUFFER_SIZE];

        for (int i = 0; i < files.length; i++) {

            if (files[i].isDirectory()) {
                addDir(clientDir, files[i], zostr); // recurse
                continue;
            }

            try (InputStream istr = new FileInputStream(files[i]);) {

                final StringBuilder relativePath = new StringBuilder();

                relativePath
                        .append(CommunityDictEnum.SAVAPAGE.getWord()
                                .toLowerCase())
                        .append("/").append(CLIENT_DIR_NAME);

                relativePath.append(
                        StringUtils.removeStart(files[i].getCanonicalPath(),
                                clientDir.getCanonicalPath()));

                zostr.putNextEntry(new ZipEntry(relativePath.toString()));

                int nBytes = istr.read(aByte);

                while (-1 < nBytes) {
                    zostr.write(aByte, 0, nBytes);
                    nBytes = istr.read(aByte);
                }

                zostr.closeEntry();
            }
        }
    }
}
