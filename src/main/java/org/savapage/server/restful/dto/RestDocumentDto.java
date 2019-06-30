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

import org.savapage.core.jpa.DocLog;
import org.savapage.server.pages.DocLogItem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class RestDocumentDto extends AbstractRestDto {

    private Long id;
    private String title;
    private String created;
    private String type;

    @JsonProperty("ext_id")
    private String extId;

    private String uuid;

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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     *
     * @param doc
     *            {@link DocLog}.
     * @return {@link RestDocumentDto}.
     */
    private static RestDocumentDto createObj(final DocLog doc) {

        final RestDocumentDto dto = new RestDocumentDto();

        dto.setId(doc.getId());
        dto.setTitle(doc.getTitle());
        dto.setCreated(toISODateTimeZ(doc.getCreatedDate()));
        dto.setExtId(doc.getExternalId());
        //dto.setUuid(doc.getUuid());

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

        return dto;
    }

    /**
     *
     * @param doc
     *            {@link DocLog}.
     * @return Pretty-printed JSON.
     */
    public static String createJSON(final DocLog doc) {
        return toJSON(createObj(doc));
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
