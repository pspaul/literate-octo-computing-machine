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
package org.savapage.server.restful.dto;

import java.util.ArrayList;
import java.util.List;

import org.savapage.core.dao.DocLogDao;
import org.savapage.core.jpa.DocLog;
import org.savapage.server.pages.DocLogItem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({ RestDocumentDto.FIELD_ID, RestDocumentDto.FIELD_EXT_ID,
        RestDocumentDto.FIELD_TYPE, RestDocumentDto.FIELD_TITLE,
        RestDocumentDto.FIELD_CREATED, RestDocumentDto.FIELD_STORE })
public final class RestDocumentDto extends AbstractRestDto {

    /** */
    public static final String FIELD_ID = "id";
    /** */
    public static final String FIELD_EXT_ID = "ext_id";
    /** */
    public static final String FIELD_TYPE = "type";
    /** */
    public static final String FIELD_TITLE = "title";
    /** */
    public static final String FIELD_CREATED = "created";
    /** */
    public static final String FIELD_STORE = "store";

    @JsonProperty(FIELD_ID)
    private Long id;

    @JsonProperty(FIELD_TITLE)
    private String title;

    @JsonProperty(FIELD_CREATED)
    private String created;

    @JsonProperty(FIELD_TYPE)
    private String type;

    @JsonProperty(FIELD_EXT_ID)
    private String extId;

    @JsonProperty(FIELD_STORE)
    private boolean store;

    //
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public boolean isStore() {
        return store;
    }

    public void setStore(boolean store) {
        this.store = store;
    }

    /**
     *
     * @param doc
     *            {@link DocLog}.
     * @return {@link RestDocumentDto}.
     */
    private static RestDocumentDto createObj(final DocLog doc,
            final boolean isStored) {

        final RestDocumentDto dto = new RestDocumentDto();

        dto.setId(doc.getId());
        dto.setTitle(doc.getTitle());
        dto.setCreated(toISODateTimeZ(doc.getCreatedDate()));
        dto.setExtId(doc.getExternalId());
        dto.setStore(isStored);

        DocLogDao.Type docType = null;

        if (doc.getDocIn() != null) {
            docType = DocLogDao.Type.IN;
        } else if (doc.getDocOut() != null) {
            if (doc.getDocOut().getPdfOut() != null) {
                docType = DocLogDao.Type.PDF;
            } else if (doc.getDocOut().getPrintOut() != null) {
                docType = DocLogDao.Type.PRINT;
            }
        }

        if (docType != null) {
            dto.setType(docType.toString().toLowerCase());
        }

        return dto;
    }

    /**
     *
     * @param item
     *            {@link DocLogItem}.
     * @return {@link RestDocumentDto}.
     */
    private static RestDocumentDto createObj(final DocLogItem item) {

        final RestDocumentDto dto = new RestDocumentDto();

        dto.setId(item.getDocLogId());
        dto.setTitle(item.getTitle());
        dto.setCreated(toISODateTimeZ(item.getCreatedDate()));
        dto.setType(item.getDocType().toString().toLowerCase());
        dto.setExtId(item.getExtId());
        dto.setStore(item.isPrintArchive() || item.isPrintJournal());

        return dto;
    }

    /**
     *
     * @param doc
     *            {@link DocLog}.
     * @return Pretty-printed JSON.
     */
    public static String createJSON(final DocLog doc, final boolean isStored) {
        return toJSON(createObj(doc, isStored));
    }

    /**
     *
     * @param items
     *            List of {@link DocLogItem} objects.
     * @return Pretty-printed JSON.
     */
    public static String itemsToJSON(final List<DocLogItem> items) {
        final List<RestDocumentDto> list = new ArrayList<>();
        for (final DocLogItem item : items) {
            list.add(createObj(item));
        }
        return toJSON(list);
    }

}
