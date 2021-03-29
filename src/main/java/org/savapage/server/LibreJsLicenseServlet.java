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
package org.savapage.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.EnumUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.protocol.http.WebApplication;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.ServerBasePath;
import org.savapage.core.util.IOHelper;
import org.savapage.core.util.InetUtils;
import org.savapage.server.pages.LibreJsHelper;
import org.savapage.server.pages.LibreJsLicenseEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds GNU LibreJS license tags to external (thirdparty) Javascript files.
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "LibreJsLicenseServlet",
        urlPatterns = { LibreJsLicenseServlet.SERVLET_URL_PATTERN })
public final class LibreJsLicenseServlet extends HttpServlet {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(LibreJsLicenseServlet.class);

    /** */
    private static final SSLContext SSLCONTEXT_TRUST_SELFSIGNED =
            InetUtils.createSslContextTrustSelfSigned();
    /**
     * .
     */
    private static final int BUFFER_SIZE = 1014;

    /**
     * Base path of the files (without leading or trailing '/').
     */
    private static final String PATH_BASE = ServerBasePath.LIBREJS;

    /**
     * Base path of the files with leading '/'.
     */
    public static final String SLASH_PATH_BASE = "/" + PATH_BASE;

    /**
     * .
     */
    public static final String SERVLET_URL_PATTERN = SLASH_PATH_BASE + "/*";

    /**
     * The full file path of the content.
     */
    public static final String CONTENT_HOME = ConfigManager.getServerHome();

    @Override
    protected void doGet(final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, IOException {

        final String reqURL = req.getRequestURL().toString();

        final StringBuilder unwrappedURL = new StringBuilder();

        final LibreJsLicenseEnum libreLicense =
                unwrapLibreJsURL(reqURL, unwrappedURL);

        if (libreLicense == null) {
            LOGGER.error("{} : GNU LibreJS license missing.", reqURL);
            throw new IOException(
                    String.format("%s : GNU LibreJS license missing.", reqURL));
        }

        final byte[] licenceStart =
                LibreJsHelper.getJsLicenseStartTag(libreLicense).getBytes();
        final byte[] licenceEnd = LibreJsHelper.getJsLicenseEndTag().getBytes();

        resp.setContentType(WebApplication.get().getMimeType(reqURL));

        final URL urlUnwrapped = new URL(unwrappedURL.toString());

        final OutputStream ostr = resp.getOutputStream();

        if (this.isWrappedWicketJs(req)) {

            this.writeFromLocalHost(resp, urlUnwrapped.getPath(), ostr,
                    licenceStart, licenceEnd);

        } else {
            final InputStream istr = this.getServletContext()
                    .getResourceAsStream(urlUnwrapped.getPath());
            this.write(istr, ostr, licenceStart, licenceEnd);
            /*
             * Do not set contentlength. Rely on close of OutputStream. When no
             * Content-Length is received, the client keeps reading until the
             * server closes the connection.
             */
        }

        resp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Writes GNU LibreJS wrapped JavaScript from input to output stream.
     *
     * @param istr
     *            Input stream.
     * @param ostr
     *            Output stream.
     * @param licenceStart
     *            GNU LibreJS License start tag.
     * @param licenceEnd
     *            GNU LibreJS License end tag.
     * @throws IOException
     *             If IO error.
     */
    private void write(final InputStream istr, final OutputStream ostr,
            final byte[] licenceStart, final byte[] licenceEnd)
            throws IOException {

        try {
            ostr.write(licenceStart);

            final byte[] aByte = new byte[BUFFER_SIZE];

            int nBytes = istr.read(aByte);
            while (-1 < nBytes) {
                ostr.write(aByte, 0, nBytes);
                nBytes = istr.read(aByte);
            }

            ostr.write(licenceEnd);
        } finally {
            IOHelper.closeQuietly(istr);
        }
    }

    /**
     * @param req
     *            Servlet request.
     * @return {@code true} if this is a GNU LibreJS wrapped Wicket JavaScript
     *         resource.
     */
    private boolean isWrappedWicketJs(final HttpServletRequest req) {
        return req.getRequestURI().contains(WebApp.WICKET_PATH_FULL_SLASHES);
    }

    /**
     * Writes unwrapped GNU LibreJS JavaScript from localhost URL to output
     * stream.
     *
     * @param resp
     *            HTTP response.
     * @param unwrappedPath
     *            Unwrapped Path.
     * @param ostr
     *            Output stream.
     * @param licenceStart
     *            GNU LibreJS License start tag.
     * @param licenceEnd
     *            GNU LibreJS License end tag.
     * @throws IOException
     *             If IO error.
     */
    private void writeFromLocalHost(final HttpServletResponse resp,
            final String unwrappedPath, final OutputStream ostr,
            final byte[] licenceStart, final byte[] licenceEnd)
            throws IOException {

        final HttpClientBuilder builder = HttpClientBuilder.create();
        final URL localhostURL;

        if (WebServer.isSSLRedirect()) {
            // Shortcut to SSL and set trust precautions.
            localhostURL = new URL("https", InetUtils.LOCAL_HOST,
                    Integer.parseInt(ConfigManager.getServerSslPort()),
                    unwrappedPath);

            builder.setSSLContext(SSLCONTEXT_TRUST_SELFSIGNED)
                    .setSSLHostnameVerifier(
                            InetUtils.getHostnameVerifierTrustAll());
        } else {
            localhostURL = new URL("http", InetUtils.LOCAL_HOST,
                    Integer.parseInt(ConfigManager.getServerPort()),
                    unwrappedPath);
        }

        try (CloseableHttpClient client = builder.build();) {

            final HttpGet request = new HttpGet(localhostURL.toString());

            try {
                final HttpResponse response = client.execute(request);
                final InputStream istr = response.getEntity().getContent();
                this.write(istr, ostr, licenceStart, licenceEnd);

            } finally {
                // Mantis #487: release the connection.
                if (request != null) {
                    request.reset();
                }
            }
        }
    }

    /**
     * Unwraps original URL path into GNU LibreJS license path.
     *
     * @param wrappedURL
     *            Wrapped URL.
     * @param unwrappedURL
     *            Unwrapped URL.
     * @return License.
     */
    private static LibreJsLicenseEnum unwrapLibreJsURL(final String wrappedURL,
            final StringBuilder unwrappedURL) {

        final int iPathBase = wrappedURL.indexOf(SLASH_PATH_BASE);

        if (iPathBase > 0) {
            final int iLicense = iPathBase + SLASH_PATH_BASE.length() + 1;
            if (iLicense > 0) {
                final int iUnwrappedPath = wrappedURL.indexOf("/", iLicense);
                if (iUnwrappedPath > 0) {

                    final String license =
                            wrappedURL.substring(iLicense, iUnwrappedPath);

                    final LibreJsLicenseEnum licenseEnum = EnumUtils
                            .getEnum(LibreJsLicenseEnum.class, license);

                    if (licenseEnum != null) {
                        unwrappedURL.append(wrappedURL.substring(0, iPathBase));
                        unwrappedURL.append("/");
                        unwrappedURL.append(
                                wrappedURL.substring(iUnwrappedPath + 1));
                        return licenseEnum;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Wraps original URL path into GNU LibreJS license path.
     *
     * @param lic
     *            License
     * @param orgPath
     *            The original URL path.
     * @return Wrapped URL path.
     */
    public static String wrapLibreJsPath(final LibreJsLicenseEnum lic,
            final String orgPath) {
        return String.format("%s/%s/%s", SLASH_PATH_BASE, lic.toString(),
                orgPath);
    }

}
