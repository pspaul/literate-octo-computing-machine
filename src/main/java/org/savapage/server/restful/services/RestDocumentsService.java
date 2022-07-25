/*
+ * This file is part of the SavaPage project <https://www.savapage.org>.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.savapage.core.UnavailableException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.dao.helpers.DocLogPagerReq;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.doc.MimeTypeEnum;
import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreException;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.User;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.print.server.DocContentPrintReq;
import org.savapage.core.services.DocStoreService;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.server.pages.DocLogItem;
import org.savapage.server.restful.RestAuthException;
import org.savapage.server.restful.RestAuthFilter;
import org.savapage.server.restful.dto.RestDocumentDto;
import org.savapage.server.webapp.WebAppHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST documents API.
 * <p>
 * Implementation of Jersey extended WADL support is <i>under construction</i>.
 * <p>
 *
 * @author Rijk Ravestein
 *
 */
@Path("/" + RestDocumentsService.PATH_MAIN)
public final class RestDocumentsService implements IRestService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RestDocumentsService.class);

    /** */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final DocStoreService DOCSTORE_SERVICE =
            ServiceContext.getServiceFactory().getDocStoreService();

    /** */
    private static final DocLogDao DOCLOG_DAO =
            ServiceContext.getDaoContext().getDocLogDao();

    /** */
    private static final UserDao USER_DAO =
            ServiceContext.getDaoContext().getUserDao();

    /** */
    private static final int INPUT_STREAM_BUFFER_SIZE = 1024;

    /** */
    public static final String PATH_MAIN = "documents";

    /** */
    private static final String PATH_PARAM_ID = "id";

    /** */
    private static final String PATH_PARAM_EXT_ID = "ext_id";

    /** */
    private static final String PATH_SUB_PDF = "pdf";

    /** */
    private static final String PATH_SUB_PRINT = "print";

    /** */
    private static final String FILTER_SFX_GTE = "_gte";
    /** */
    private static final String FILTER_SFX_LTE = "_lte";

    /** */
    private static final String SORT_PFX_ASC = "+";
    /** */
    private static final String SORT_PFX_DESC = "-";

    /** */
    private static final String QUERY_PARAM_PAGE = "page";
    /** */
    private static final String QUERY_PARAM_LIMIT = "limit";
    /** */
    private static final String QUERY_PARAM_SORT = "sort";

    /** */
    private static final String FORM_PARAM_FILE = "file";

    /** */
    @SuppressWarnings("unused")
    private static final String FORM_PARAM_DESCRIPTION = "description";

    /** */
    @Context
    private HttpServletRequest servletRequest;

    // response.* annotations are from Jersey's wadl-resourcedoc-doclet

    /**
     * Gets document details.
     *
     * @response.representation.200.qname document
     * @response.representation.200.mediaType application/json
     * @response.representation.200.doc The document object.
     *
     *                                  @response.representation.200.example
     *                                  {"id" : 125, "ext_id" :
     *                                  "a4fc5e08-9f5e-11e9-81cf-406186940c49",
     *                                  "type" : "print", "title" : "Sample
     *                                  Document", "created" :
     *                                  "2019-07-04T14:15:03+0200", "store" :
     *                                  "true"}
     *
     * @param id
     *            Document key.
     * @return {@link RestDocumentDto}.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    @GET
    @Path("/{" + PATH_PARAM_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocumentById(//
            @PathParam(PATH_PARAM_ID) final Long id) {

        return this.getDocumentRsp(DOCLOG_DAO.findById(id));
    }

    /**
     * Gets document details.
     *
     * @param externalId
     *            Unique external id of Document.
     * @return {@link RestDocumentDto}.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    @GET
    @Path("/" + RestDocumentDto.FIELD_EXT_ID + "={" + PATH_PARAM_EXT_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocumentByExtId(//
            @PathParam(PATH_PARAM_EXT_ID) final String externalId) {

        return this.getDocumentRsp(DOCLOG_DAO.findByExtId(externalId));
    }

    /**
     * @param doc
     *            {@link DocLog}, can be {@code null}.
     * @return REST response.
     */
    private Response getDocumentRsp(final DocLog doc) {

        try {
            final ResponseBuilder rsp;
            if (doc == null) {
                rsp = Response.noContent();
            } else {

                DocStoreBranchEnum branch = null;
                if (doc.getDocOut() != null
                        && doc.getDocOut().getPrintOut() != null) {
                    branch = DocStoreBranchEnum.OUT_PRINT;
                }

                final boolean isStored = branch != null && (DOCSTORE_SERVICE
                        .isDocPresent(DocStoreTypeEnum.ARCHIVE, branch, doc)
                        || DOCSTORE_SERVICE.isDocPresent(
                                DocStoreTypeEnum.JOURNAL, branch, doc));

                rsp = Response.ok(RestDocumentDto.createJSON(doc, isStored));
            }
            return rsp.build();
        } catch (Exception e) {
            LOGGER.error("{}: {}", e.getClass().getName(), e.getMessage());
            throw new WebApplicationException(e.getMessage());
        }
    }

    /**
     * Gets PDF from archive.
     *
     * @param id
     *            Document key.
     * @return PDF file.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    @GET
    @Path("/{" + PATH_PARAM_ID + "}/" + PATH_SUB_PDF)
    @Produces(MimeTypeEnum.MIME_APPLICATION_PDF)
    public Response getDocumentPdfById(//
            @PathParam(PATH_PARAM_ID) final Long id) {

        return this.getDocumentPdfRsp(DOCLOG_DAO.findById(id));
    }

    /**
     * Gets PDF from archive.
     *
     * @param externalId
     *            Unique external id of Document.
     * @return PDF file.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    @GET
    @Path("/" + RestDocumentDto.FIELD_EXT_ID + "={" + PATH_PARAM_EXT_ID + "}/"
            + PATH_SUB_PDF)
    @Produces(MimeTypeEnum.MIME_APPLICATION_PDF)
    public Response getDocumentPdfByExtId(//
            @PathParam(PATH_PARAM_EXT_ID) final String externalId) {

        return this.getDocumentPdfRsp(DOCLOG_DAO.findByExtId(externalId));
    }

    /**
     * @param doc
     *            {@link DocLog}, can be {@code null}.
     * @return REST response.
     */
    private Response getDocumentPdfRsp(final DocLog doc) {

        try {
            final ResponseBuilder rsp;

            if (doc == null) {
                rsp = Response.noContent();
            } else {
                File file = null;
                try {
                    file = DOCSTORE_SERVICE
                            .retrievePdf(DocStoreTypeEnum.ARCHIVE, doc);
                } catch (DocStoreException e) {
                    file = DOCSTORE_SERVICE
                            .retrievePdf(DocStoreTypeEnum.JOURNAL, doc);
                }
                rsp = downloadPdf(file);
            }
            return rsp.build();

        } catch (Exception e) {
            LOGGER.error("{}: {}", e.getClass().getName(), e.getMessage());
            throw new WebApplicationException(e.getMessage());
        }
    }

    /**
     *
     * @param authorization
     *            Header with format "Basic base64(user:password)"
     * @param msgPfx
     *            Message prefix.
     * @param disp
     *            Meta data.
     * @return Authenticated user.
     * @throws RestAuthException
     *             If authentication failed.
     */
    private User isUserAuthenticated(final String authorization,
            final String msgPfx, final FormDataContentDisposition disp)
            throws RestAuthException {

        if (authorization == null || authorization.isEmpty()) {
            throw new RestAuthException(String.format(
                    "%s of document [%s] failed: no authorization data.",
                    msgPfx, disp.getFileName()));
        }

        // Get encoded username and password
        final String encodedUserPassword = authorization.replaceFirst(
                RestAuthFilter.HEADER_AUTHENTICATION_SCHEME + " ", "");

        // Decode username and password
        final String usernameAndPassword = new String(
                Base64.getDecoder().decode(encodedUserPassword.getBytes()));

        // Split username and password tokens
        final StringTokenizer tokenizer =
                new StringTokenizer(usernameAndPassword, ":");

        final String userid = tokenizer.nextToken();
        final String uuid = tokenizer.nextToken();

        final UUID uuidObj;
        try {
            uuidObj = UUID.fromString(uuid);
        } catch (Exception e) {
            throw new RestAuthException(String.format(
                    "%s of document [%s] for user [%s] failed: "
                            + "UUID invalid.",
                    msgPfx, disp.getFileName(), userid));
        }

        final User user = USER_DAO.findActiveUserByUserId(userid);

        if (user == null) {
            throw new RestAuthException(String.format(
                    "%s of document [%s] failed: user [%s] not found.", msgPfx,
                    disp.getFileName()));
        }

        if (!USER_SERVICE.isUserUuidPresent(user, uuidObj)) {
            throw new RestAuthException(String.format(
                    "%s of document [%s] for user [%s] failed: "
                            + "UUID not found.",
                    msgPfx, disp.getFileName(), userid));
        }

        return user;
    }

    /**
     * Prints a document.
     *
     * @param authString
     *            Basic authentication string.
     * @param istr
     *            File input stream.
     * @param disp
     *            Meta data.
     * @return {@link Response} with string message.
     * @throws Exception
     *             If error.
     */
    @POST
    @Path("/" + PATH_SUB_PRINT)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response printDocument(//
            @HeaderParam(RestAuthFilter.HEADER_AUTHORIZATION_PROPERTY) //
            final String authString, //
            @FormDataParam(FORM_PARAM_FILE) final InputStream istr,
            @FormDataParam(FORM_PARAM_FILE) //
            final FormDataContentDisposition disp) {

        final String msgPfx = CommunityDictEnum.RESTFUL_PRINT.getWord();

        String userid = "";

        try {

            final User user = isUserAuthenticated(authString, msgPfx, disp);
            userid = user.getUserId();

            if (disp.getFileName() == null) {
                return printDocumentWarn(Response.Status.NOT_MODIFIED,
                        String.format(
                                "%s for user [%s] FAILED: no document name.",
                                msgPfx, userid));
            }

            final DocContentTypeEnum contentType =
                    DocContent.getContentTypeFromFile(disp.getFileName());
            if (contentType == null) {
                return printDocumentWarn(Response.Status.NOT_MODIFIED,
                        String.format(
                                "%s of document [%s] for user [%s] failed: "
                                        + "unknown content type.",
                                msgPfx, disp.getFileName(), userid));
            }

            final DocContentPrintReq docContentPrintReq =
                    new DocContentPrintReq();

            docContentPrintReq.setContentType(contentType);
            docContentPrintReq.setFileName(disp.getFileName());
            docContentPrintReq.setOriginatorEmail(null);
            docContentPrintReq
                    .setOriginatorIp(WebAppHelper.getClientIP(servletRequest));
            docContentPrintReq.setPreferredOutputFont(null);
            docContentPrintReq.setProtocol(DocLogProtocolEnum.HTTP);
            docContentPrintReq.setTitle(disp.getFileName());

            QUEUE_SERVICE.printDocContent(ReservedIppQueueEnum.WEBSERVICE,
                    user.getUserId(), docContentPrintReq, istr);

        } catch (RestAuthException e) {
            return printDocumentWarn(Response.Status.UNAUTHORIZED,
                    e.getMessage());
        } catch (DocContentPrintException e) {
            return printDocumentWarn(Response.Status.INTERNAL_SERVER_ERROR,
                    String.format(
                            "%s of document [%s] for user [%s] failed: %s",
                            msgPfx, disp.getFileName(), userid,
                            e.getMessage()));
        } catch (UnavailableException e) {
            return printDocumentWarn(Response.Status.SERVICE_UNAVAILABLE,
                    String.format(
                            "%s of document [%s] for user [%s] failed: %s",
                            msgPfx, disp.getFileName(), userid,
                            e.getMessage()));
        }

        final String msg = String.format("%s of document [%s] for user [%s]",
                msgPfx, disp.getFileName(), userid);
        AdminPublisher.instance().publish(PubTopicEnum.WEB_SERVICE,
                PubLevelEnum.INFO, msg);
        LOGGER.debug(msg);
        return Response.ok().build();
    }

    /**
     * Creates warning response for print request.
     *
     * @param status
     *            Response status
     * @param msg
     *            Message.
     * @return {@link Response}.
     */
    private Response printDocumentWarn(final Response.Status status,
            final String msg) {
        AdminPublisher.instance().publish(PubTopicEnum.WEB_SERVICE,
                PubLevelEnum.WARN, msg);
        LOGGER.warn(msg);
        return Response.status(status).build();
    }

    /**
     * @param documentType
     *            Type of document.
     * @param createdQte
     *            Creation date-time QTE.
     * @param createdLte
     *            Creation date-time LTE.
     * @param page
     *            Zero-based offset in result set.
     * @param limit
     *            Max number of objects in result set.
     * @param sort
     *            Sorting clause.
     * @return Result set: list of {@link RestDocumentDto} objects.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ADMIN)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocuments(//
            @DefaultValue("") @QueryParam(RestDocumentDto.FIELD_TYPE) //
            final String documentType,
            @DefaultValue("") @QueryParam(RestDocumentDto.FIELD_CREATED
                    + FILTER_SFX_GTE) //
            final String createdQte,
            @DefaultValue("") @QueryParam(RestDocumentDto.FIELD_CREATED
                    + FILTER_SFX_LTE) //
            final String createdLte, //
            @DefaultValue("1") @QueryParam(QUERY_PARAM_PAGE) //
            final Integer page, //
            @DefaultValue("5") @QueryParam(QUERY_PARAM_LIMIT) //
            final Integer limit, //
            @DefaultValue(SORT_PFX_ASC + RestDocumentDto.FIELD_CREATED) //
            @QueryParam(QUERY_PARAM_SORT) final String sort) {

        try {
            // Note: the "+" prefix gets lost when using curl.
            final boolean sortAscending = !sort.startsWith(SORT_PFX_DESC);

            final DocLogDao.Type daoType;

            if (StringUtils.isBlank(documentType)) {
                daoType = DocLogDao.Type.ALL;
            } else {
                daoType = EnumUtils.getEnum(DocLogDao.Type.class,
                        documentType.toUpperCase());
            }
            //
            final DocLogPagerReq req = new DocLogPagerReq();
            req.setPage(page);
            req.setMaxResults(limit);

            final DocLogPagerReq.Sort reqSort = new DocLogPagerReq.Sort();
            reqSort.setField(DocLogPagerReq.Sort.FLD_DATE);
            reqSort.setAscending(sortAscending);
            req.setSort(reqSort);

            final DocLogPagerReq.Select reqSelect = new DocLogPagerReq.Select();
            reqSelect.setDocType(daoType);
            req.setSelect(reqSelect);

            final DocLogItem.AbstractQuery query =
                    DocLogItem.createQuery(daoType);

            final EntityManager em = DaoContextImpl.peekEntityManager();
            final Long userId = null;
            final List<DocLogItem> items =
                    query.getListChunk(em, userId, req, Locale.US);

            return Response.ok(RestDocumentDto.itemsToJSON(items)).build();

        } catch (Exception e) {
            LOGGER.error("{}: {}", e.getClass().getName(), e.getMessage());
            throw new WebApplicationException(e.getMessage());
        }
    }

    /**
     *
     * @param filePdf
     *            PDF file.
     * @return {@link ResponseBuilder}.
     */
    private ResponseBuilder downloadPdf(final File filePdf) {

        final StreamingOutput output = new StreamingOutput() {

            @Override
            public void write(final java.io.OutputStream output)
                    throws IOException, WebApplicationException {

                try (InputStream istr = new FileInputStream(filePdf)) {
                    final byte[] buffer = new byte[INPUT_STREAM_BUFFER_SIZE];
                    int nBytes;
                    while ((nBytes = istr.read(buffer)) >= 0) {
                        output.write(buffer, 0, nBytes);
                    }
                    output.flush();
                }
            }
        };

        return Response.ok(output).header("content-disposition",
                "attachment; filename = file.pdf");
    }
}
