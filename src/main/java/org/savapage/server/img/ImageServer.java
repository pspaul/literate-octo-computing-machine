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
import java.util.List;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.OutputProducer;
import org.savapage.core.img.ImageUrl;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.server.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class ImageServer extends WebPage implements ServiceEntryPoint {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logegr.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ImageServer.class);

    /**
     * Delivers the a page image.
     * <p>
     * Invariants:
     * <ul>
     * <li>Authenticated user session.</li>
     * <li>Requesting user equals session user.</li>
     * </ul>
     * </p>
     * <p>
     * When an invariant is violated, an image with an error message is
     * delivered.
     * </p>
     *
     * @param parameters
     *            The page parameters.
     */
    public ImageServer(final PageParameters parameters) {

        final ImageUrl url = getImageUrl(parameters);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(url.composeImageUrl());
        }

        File file = null;

        ServiceContext.open();

        try {
            if (SpSession.get().isAuthenticated()
                    && url.getUser() != null
                    && SpSession.get().getUser().getUserId()
                            .equals(url.getUser())) {
                file = getImageFile(url);
            } else {
                file = getErrorImageFile(url);
            }

        } finally {
            ServiceContext.close();
        }

        /*
         *
         */
        final IRequestHandler handler;

        if (url.isBase64()) {
            handler = new ImageReqHandlerBase64(file);
        } else {
            handler = new ImageReqHandler(file);
        }

        getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
    }

    /**
     * TODO: localize the error message.
     *
     * @param url
     * @return
     */
    private static File getErrorImageFile(final ImageUrl url) {
        File file =
                OutputProducer.instance().allocateWarningPageImage(
                        url.getUser(), url.getJob(), url.getPage(),
                        "Session Expired", null);
        return file;
    }

    /**
     *
     * @param parameters
     * @return
     */
    private static ImageUrl getImageUrl(final PageParameters parameters) {

        final ImageUrl url = new ImageUrl();

        final List<NamedPair> list = parameters.getAllNamed();

        for (NamedPair pair : list) {
            url.setParm(pair.getKey(), pair.getValue());
        }
        return url;
    }

    /**
     *
     * @param url
     * @return
     */
    private File getImageFile(ImageUrl url) {

        return OutputProducer.instance().allocatePageImage(url.getUser(),
                url.getJob(), url.getPage(), url.getRotate(),
                url.isThumbnail(), url.isLetterhead(),
                url.isLetterheadPublic(), getSession().getId());

    }

}
