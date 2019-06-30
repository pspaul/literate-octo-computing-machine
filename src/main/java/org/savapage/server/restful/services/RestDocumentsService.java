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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.persistence.EntityManager;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.EnumUtils;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.helpers.DocLogPagerReq;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.store.DocStoreException;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.services.DocStoreService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.pages.DocLogItem;
import org.savapage.server.restful.RestAuthFilter;
import org.savapage.server.restful.dto.RestDocumentDto;

/** */
@Path("/" + RestDocumentsService.PATH_MAIN)

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class RestDocumentsService implements IRestService {

    /** */
    private static final DocStoreService DOCSTORE_SERVICE =
            ServiceContext.getServiceFactory().getDocStoreService();

    /** */
    private static final DocLogDao DOCLOG_DAO =
            ServiceContext.getDaoContext().getDocLogDao();

    /** */
    private static final int INPUT_STREAM_BUFFER_SIZE = 1024;

    /** */
    public static final String PATH_MAIN = "documents";
    /** */
    private static final String PATH_PARAM_ID = "id";
    /** */
    private static final String PATH_SUB_PDF = "pdf";

    /** */
    private static final String FILTER_SFX_GTE = "_gte";
    /** */
    private static final String FILTER_SFX_LTE = "_lte";

    /** */
    private static final String SORT_PFX_ASC = "+";
    /** */
    private static final String SORT_PFX_DESC = "-";
    /** */
    private static final String FIELD_CREATED = "created";

    /**
     * Gets document details.
     *
     * @param id
     *            Document key.
     * @return {@link RestDocumentDto}.
     */
    @RolesAllowed(RestAuthFilter.ROLE_ALLOWED)
    @GET
    @Path("/{" + PATH_PARAM_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocument(//
            @PathParam(PATH_PARAM_ID) final String id) {

        try {
            final ResponseBuilder rsp;

            final Long dbKey = Long.valueOf(id);

            final DocLog doc = DOCLOG_DAO.findById(dbKey);
            if (doc == null) {
                rsp = Response.noContent();
            } else {
                rsp = Response.ok(RestDocumentDto.createJSON(doc));
            }
            return rsp.build();

        } catch (Exception e) {
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
    @RolesAllowed(RestAuthFilter.ROLE_ALLOWED)
    @GET
    @Path("/{" + PATH_PARAM_ID + "}/" + PATH_SUB_PDF)
    @Produces(DocContent.MIMETYPE_PDF)
    public Response getDocumentPdf(//
            @PathParam(PATH_PARAM_ID) final String id) {

        try {
            final ResponseBuilder rsp;

            final Long dbKey = Long.valueOf(id);
            final DocLog doc = DOCLOG_DAO.findById(dbKey);

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
            throw new WebApplicationException(e.getMessage());
        }
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
    @RolesAllowed(RestAuthFilter.ROLE_ALLOWED)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocuments(//
            @DefaultValue("") @QueryParam("type") //
            final String documentType,
            @DefaultValue("") @QueryParam(FIELD_CREATED + FILTER_SFX_GTE) //
            final String createdQte,
            @DefaultValue("") @QueryParam(FIELD_CREATED + FILTER_SFX_LTE) //
            final String createdLte,
            @DefaultValue("0") @QueryParam("page") final Integer page,
            @DefaultValue("5") @QueryParam("limit") final Integer limit,
            @DefaultValue(SORT_PFX_ASC
                    + FIELD_CREATED) @QueryParam("sort") final String sort) {

        // Note: the "+" prefix gets lost when using curl.
        final boolean sortAscending = !sort.startsWith(SORT_PFX_DESC);

        final DocLogDao.Type daoType = EnumUtils.getEnum(DocLogDao.Type.class,
                documentType.toUpperCase());

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

        final DocLogItem.AbstractQuery query = DocLogItem.createQuery(daoType);

        final EntityManager em = DaoContextImpl.peekEntityManager();
        final Long userId = null;
        final List<DocLogItem> items = query.getListChunk(em, userId, req);

        return Response.ok(RestDocumentDto.itemsToJSON(items)).build();
    }

    /**
     *
     * @param filePdf
     *            PDF file.
     * @return {@link ResponseBuilder}.
     */
    public ResponseBuilder downloadPdf(final File filePdf) {

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
