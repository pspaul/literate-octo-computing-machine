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

        private String value;
        private String type;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("emails")
    private List<Email> emails;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<Email> getEmails() {
        return emails;
    }

    public void setEmails(List<Email> emails) {
        this.emails = emails;
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
