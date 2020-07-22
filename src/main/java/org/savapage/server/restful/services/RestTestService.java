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
package org.savapage.server.restful.services;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.savapage.core.SpInfo;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.doc.DocContent;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.print.IppRoutingDto;
import org.savapage.ext.print.QrCodeAnchorEnum;
import org.savapage.server.restful.RestApplication;
import org.savapage.server.restful.RestAuthFilter;
import org.savapage.server.restful.dto.RestHttpRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

/**
 * REST tests API.
 * <p>
 * Implementation of Jersey extended WADL support is <i>under construction</i>.
 * <p>
 *
 * @author Rijk Ravestein
 *
 */
@Path("/" + RestTestService.PATH_MAIN)
public final class RestTestService implements IRestService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RestTestService.class);

    /** */
    public static final String PATH_MAIN = "tests";

    /** */
    private static final String PATH_SUB_ECHO = "echo";

    /** */
    private static final String PATH_SUB_IPP_ROUTING_DATA = "ipp-routing-data";

    /** */
    private static final String PATH_SUB_HTTP_REQUEST = "http/request";

    /** */
    private static final String PATH_SUB_UPLOAD = "upload";

    /** */
    private static final String PATH_SUB_DOWNLOAD_PDF = "download/pdf";

    /** */
    private static final String PATH_SUB_ARCHIVES_PDF = "archives";

    /** */
    private static final String PATH_SUB_ID = "id";

    /** */
    private static final String FORM_PARAM_FILE = "file";

    /** */
    private static final String MEDIA_TYPE_PDF = DocContent.MIMETYPE_PDF;

    /** */
    @Context
    private HttpServletRequest servletRequest;

    /**
     *
     * @author Rijk Ravestein
     *
     */
    public static class ArchiveDto extends AbstractDto {

        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    /**
     * @param id
     *            File ID.
     * @return {@link Response}.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    @GET
    @Path(PATH_SUB_ARCHIVES_PDF + "/{" + PATH_SUB_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testArchives(@PathParam(PATH_SUB_ID) final String id) {

        if (StringUtils.isBlank(id)) {
            return Response.noContent().build();
        }

        final ArchiveDto dto = new ArchiveDto();
        dto.setId(id);

        try {
            return Response.ok(dto.stringifyPrettyPrinted()).build();
        } catch (IOException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    /**
     * @param text
     *            Text to echo.
     * @return Application version.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    @POST
    @Path(PATH_SUB_ECHO)
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response echoTextPlain(final String text) {
        return Response.status(Status.CREATED).entity(text).build();
    }

    /**
     * @param dummy
     *            Dummy text.
     * @return {@link IppRoutingDto}.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    @POST
    @Path(PATH_SUB_IPP_ROUTING_DATA)
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testIppRoutingData(final String dummy) {

        final String[] tokens = StringUtils.splitPreserveAllTokens(dummy, ';');

        int width = 0;
        int height = 0;

        for (final String token : tokens) {

            final String[] keyValue =
                    StringUtils.splitPreserveAllTokens(token, "=");
            if (keyValue.length != 2) {
                continue;
            }
            final String key = keyValue[0];
            final String value = keyValue[1];

            switch (key) {
            case "page-width":
                if (NumberUtils.isDigits(value)) {
                    width = Integer.parseInt(value);
                }
                break;

            case "page-height":
                if (NumberUtils.isDigits(value)) {
                    height = Integer.parseInt(value);
                }
                break;

            default:
                break;
            }
        }

        final IppRoutingDto data = new IppRoutingDto();

        data.setId(UUID.randomUUID().toString());

        final IppRoutingDto.PdfData pdf = new IppRoutingDto.PdfData();
        data.setPdf(pdf);

        final IppRoutingDto.QrCode qrcode = new IppRoutingDto.QrCode();
        pdf.setQrcode(qrcode);

        final int qz = 4;
        final int qrSize = 40;

        qrcode.setQz(qz);
        qrcode.setSize(qrSize);

        final IppRoutingDto.QrCodePosition pos =
                new IppRoutingDto.QrCodePosition();
        qrcode.setPos(pos);
        pos.setAnchor(QrCodeAnchorEnum.BR);

        final IppRoutingDto.Margin margin = new IppRoutingDto.Margin();
        pos.setMargin(margin);

        if (width == 0 || height == 0) {
            margin.setX(100);
            margin.setY(150);
        } else {
            final boolean center = false;
            if (center) {
                // center
                final int offset = (qz + qrSize) / 2;
                margin.setX(width / 2 - offset);
                margin.setY(height / 2 - offset);
            } else {
                margin.setX(10);
                margin.setY(10);
            }
        }

        return Response.status(Status.CREATED).entity(data).build();
    }

    /**
     * @return HTTP request data.
     */
    @GET
    @Path(PATH_SUB_HTTP_REQUEST)
    @Produces(MediaType.TEXT_PLAIN)
    public Response testHeaders() {
        return Response.ok(RestHttpRequestDto.createJSON(servletRequest))
                .build();
    }

    /**
     * @param id
     *            File ID.
     * @return {@link Response}.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    @GET
    @Path(PATH_SUB_DOWNLOAD_PDF + "/{" + PATH_SUB_ID + "}")
    @Produces(MEDIA_TYPE_PDF)
    public Response testDownloadPdf(@PathParam(PATH_SUB_ID) final String id) {

        if (StringUtils.isBlank(id)) {
            return Response.noContent().build();
        }

        final StreamingOutput output = new StreamingOutput() {
            @Override
            public void write(final java.io.OutputStream output)
                    throws IOException, WebApplicationException {

                try {
                    final ByteArrayOutputStream bos =
                            new ByteArrayOutputStream();

                    final Document doc = new Document();
                    final PdfWriter writer = PdfWriter.getInstance(doc, bos);
                    doc.open();
                    doc.add(new Paragraph(String
                            .format("Content of file with ID [%s].\n", id)));

                    doc.close();
                    writer.close();

                    output.write(bos.toByteArray());

                    output.flush();

                } catch (Exception e) {
                    throw new WebApplicationException(e.getMessage());
                }
            }
        };

        return Response.ok(output)
                .header("content-disposition",
                        "attachment; filename = file.pdf") //
                .build();
    }

    /**
     *
     * @param fos
     *            File input stream.
     * @param disp
     *            Meta data.
     * @return {@link Response} with string message.
     * @throws Exception
     *             If error.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    @POST
    @Path(PATH_SUB_UPLOAD)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testUpload(//
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
                .path(PATH_SUB_UPLOAD);

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
                                RestTestService.class.getSimpleName(),
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
