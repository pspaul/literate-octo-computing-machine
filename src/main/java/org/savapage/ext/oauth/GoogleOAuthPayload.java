/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.ext.oauth;

import java.util.List;

import org.savapage.core.json.JsonAbstractBase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GoogleOAuthPayload extends JsonAbstractBase {

    /**
     *
     * @author Rijk Ravestein
     *
     */
    public static final class Email extends JsonAbstractBase {

        /** */
        private String value;

        /** */
        private String type;

        /**
         *
         * @return Email value.
         */
        public String getValue() {
            return value;
        }

        /**
         *
         * @param evalue
         *            Email value.
         */
        public void setValue(final String evalue) {
            this.value = evalue;
        }

        /**
         *
         * @return Email type.
         */
        public String getType() {
            return type;
        }

        /**
         *
         * @param etype
         *            Email type.
         */
        public void setType(final String etype) {
            this.type = etype;
        }
    }

    /** */
    @JsonProperty("displayName")
    private String displayName;

    /** */
    @JsonProperty("emails")
    private List<Email> emails;

    /**
     *
     * @return The display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     *
     * @param name
     *            The display name.
     */
    public void setDisplayName(final String name) {
        this.displayName = name;
    }

    /**
     *
     * @return Email list.
     */
    public List<Email> getEmails() {
        return emails;
    }

    /**
     *
     * @param list
     *            Email list.
     */
    public void setEmails(final List<Email> list) {
        this.emails = list;
    }

    /**
     *
     * @return {@code null} when not available.
     */
    @JsonIgnore
    public String getFirstEmail() {
        if (!emails.isEmpty()) {
            return emails.get(0).getValue();
        }
        return null;
    }

    /**
     * Creates an instance from a JSON string.
     *
     * @param json
     *            The JSON string.
     * @return The {@link GoogleOAuthPayload} instance.
     */
    public static GoogleOAuthPayload create(final String json) {
        return create(GoogleOAuthPayload.class, json);
    }

}
