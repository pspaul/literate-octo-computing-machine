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
package org.savapage.server.restful.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.savapage.core.SpInfo;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.restful.RestApplication;
import org.savapage.server.restful.RestAuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
@Path("/" + RestPrintService.PATH_MAIN)

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class RestPrintService implements IRestService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RestPrintService.class);

    /** */
    public static final String PATH_MAIN = "print";

    /** */
    private static final String PATH_SUB_TEST = "test";

    /** */
    private static final String PATH_SUB_PDF = "pdf";

    /** */
    private static final String FORM_PARAM_FILE = "file";

    /**
     *
     * @param fos
     *            File input stream.
     * @param disp
     *            Meta data.
     * @return String message.
     * @throws Exception
     *             If error.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ALLOWED)
    @POST
    @Path(PATH_SUB_TEST)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response printTest(//
            @FormDataParam(FORM_PARAM_FILE) final InputStream fos,
            @FormDataParam(FORM_PARAM_FILE) //
            final FormDataContentDisposition disp) {

        final String msg;
        boolean error = true;

        if (disp.getFileName() == null) {
            msg = String.format("Test upload of file [%s] " + "FAILED.\n",
                    disp.getFileName());
        } else {
            /*
             * NOTE: disp.getSize() == -1 when invoked from `curl`. Why?
             */
            long nBytes = 0;

            try {
                while (fos.read() >= 0) {
                    nBytes++;
                }
                error = false;
            } catch (IOException e) {
                // no code intended.
            }

            if (error) {
                msg = String.format(
                        "Test upload of file [%s] FAILED after [%d] bytes.\n",
                        disp.getFileName(), nBytes);
            } else if (nBytes == 0) {
                msg = String.format(
                        "Test upload of file [%s] FAILED: file is empty.\n",
                        disp.getFileName());
            } else {
                msg = String.format(
                        "Test upload of file [%s] [%d] bytes "
                                + "successfully completed.",
                        disp.getFileName(), nBytes);
            }
        }

        SpInfo.instance().log(msg);

        if (error) {
            return Response.notModified(msg).build();
        }

        return Response.ok(msg).build();
    }

    /**
     * Tests the service by calling a simple POST of this RESTful service. The
     * result is logged.
     */
    public static void test() {

        final ConfigManager cm = ConfigManager.instance();

        final Client client = ServiceContext.getServiceFactory()
                .getRestClientService().createClientAuth(
                        cm.getConfigValue(
                                IConfigProp.Key.API_RESTFUL_AUTH_USERNAME),
                        cm.getConfigValue(
                                IConfigProp.Key.API_RESTFUL_AUTH_PASSWORD));

        final WebTarget webTarget = client
                .target("https://localhost:" + ConfigManager.getServerSslPort())
                .path(RestApplication.RESTFUL_URL_PATH).path(PATH_MAIN)
                .path(PATH_SUB_TEST);

        File file = null;

        try {
            file = File.createTempFile("temp-", ".txt");

            try (BufferedWriter writer =
                    new BufferedWriter(new FileWriter(file))) {
                writer.write("test");
            }

            final FileDataBodyPart filePart =
                    new FileDataBodyPart(FORM_PARAM_FILE, file);

            try (//
                    FormDataMultiPart formDataMultiPart =
                            new FormDataMultiPart();
                    MultiPart entity = formDataMultiPart.bodyPart(filePart);
                    Response response = webTarget.request().post(
                            Entity.entity(entity, entity.getMediaType()));) {

                SpInfo.instance()
                        .log(String.format("%s test: POST %s -> %s [%s]",
                                RestPrintService.class.getSimpleName(),
                                webTarget.getUri().toString(),
                                response.getStatus(),
                                response.getStatusInfo()));
            }

        } catch (javax.ws.rs.ProcessingException | IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }
}
