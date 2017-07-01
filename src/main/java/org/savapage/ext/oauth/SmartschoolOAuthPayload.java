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

import org.savapage.core.json.JsonAbstractBase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SmartschoolOAuthPayload extends JsonAbstractBase {

    /**
     * Technical key. E.g. "K+ySCK04KP\/ACYTZOB3Uug=="
     */
    private String userID;

    /**
     * E.g. "John".
     */
    private String name;

    /**
     * E.g. "Brown".
     */
    private String surname;

    /**
     * E.g. "Brown John".
     */
    private String fullname;

    /**
     * E.g. "john.brown".
     */
    private String username;

    /**
     * "https://uwschool.smartschool.be".
     */
    private String platform;

    /**
     *
     * @return user id.
     */
    public String getUserID() {
        return userID;
    }

    /**
     *
     * @param id
     *            user id.
     */
    public void setUserID(final String id) {
        this.userID = id;
    }

    /**
     *
     * @return The name, e.g. "John".
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param fname
     *            The name, e.g. "John".
     */
    public void setName(final String fname) {
        this.name = fname;
    }

    /**
     *
     * @return Surname, e.g. "Brown".
     */
    public String getSurname() {
        return surname;
    }

    /**
     *
     * @param sname
     *            Surname, e.g. "Brown".
     */
    public void setSurname(final String sname) {
        this.surname = sname;
    }

    /**
     *
     * @return E.g. "john.brown".
     */
    public String getFullname() {
        return fullname;
    }

    /**
     *
     * @param fname
     *            E.g. "john.brown".
     */
    public void setFullname(final String fname) {
        this.fullname = fname;
    }

    /**
     *
     * @return User name.
     */
    public String getUsername() {
        return username;
    }

    /**
     *
     * @param uname
     *            User name.
     */
    public void setUsername(final String uname) {
        this.username = uname;
    }

    /**
     *
     * @return Like "https://uwschool.smartschool.be".
     */
    public String getPlatform() {
        return platform;
    }

    /**
     *
     * @param pform
     *            Like "https://uwschool.smartschool.be".
     */
    public void setPlatform(final String pform) {
        this.platform = pform;
    }

    /**
     * Creates an instance from a JSON string.
     *
     * @param json
     *            The JSON string.
     * @return The {@link SmartschoolOAuthPayload} instance.
     */
    public static SmartschoolOAuthPayload create(final String json) {
        return create(SmartschoolOAuthPayload.class, json);
    }

}
