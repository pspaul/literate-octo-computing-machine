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
package org.savapage.server.restful.dto;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
@JsonPropertyOrder({ RestHttpRequestDto.REMOTE_ADDR,
        RestHttpRequestDto.REMOTE_HOST, RestHttpRequestDto.REMOTE_PORT,
        RestHttpRequestDto.REMOTE_USER, RestHttpRequestDto.LOCAL_ADDR,
        RestHttpRequestDto.SERVER_NAME, RestHttpRequestDto.SERVER_PORT,
        RestHttpRequestDto.HEADERS })
public final class RestHttpRequestDto extends AbstractRestDto {

    /** */
    public static final String REMOTE_ADDR = "remoteAddr";
    /** */
    public static final String REMOTE_HOST = "remoteHost";
    /** */
    public static final String REMOTE_PORT = "remotePort";
    /** */
    public static final String REMOTE_USER = "remoteUser";

    /** */
    public static final String LOCAL_ADDR = "localAddr";

    /** */
    public static final String SERVER_NAME = "serverName";
    /** */
    public static final String SERVER_PORT = "serverPort";

    /** */
    public static final String HEADERS = "headers";

    /** */
    @JsonProperty(REMOTE_ADDR)
    private String remoteAddr;

    /** */
    @JsonProperty(REMOTE_HOST)
    private String remoteHost;

    /** */
    @JsonProperty(REMOTE_PORT)
    private int remotePort;

    /** */
    @JsonProperty(REMOTE_USER)
    private String remoteUser;

    /** */
    @JsonProperty(LOCAL_ADDR)
    private String localAddr;

    /** */
    @JsonProperty(SERVER_NAME)
    private String serverName;

    /** */
    @JsonProperty(SERVER_PORT)
    private int serverPort;

    /** */
    @JsonProperty(HEADERS)
    private Map<String, String> headers = new HashMap<>();

    //
    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    public String getLocalAddr() {
        return localAddr;
    }

    public void setLocalAddr(String localAddr) {
        this.localAddr = localAddr;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Creates instance.
     *
     * @param req
     *            {@link HttpServletRequest}.
     * @return {@link RestHttpRequestDto}.
     */
    private static RestHttpRequestDto create(final HttpServletRequest req) {

        final RestHttpRequestDto dto = new RestHttpRequestDto();

        dto.remoteAddr = req.getRemoteAddr();
        dto.remoteHost = req.getRemoteHost();
        dto.remotePort = req.getRemotePort();
        dto.remoteUser = req.getRemoteUser();

        dto.localAddr = req.getLocalAddr();

        dto.serverName = req.getServerName();
        dto.serverPort = req.getServerPort();

        final Enumeration<String> headerNames = req.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            final String key = headerNames.nextElement();
            dto.headers.put(key, req.getHeader(key));
        }
        return dto;
    }

    /**
     * @param req
     *            {@link HttpServletRequest}.
     * @return Pretty-printed JSON.
     */
    public static String createJSON(final HttpServletRequest req) {
        return toJSON(create(req));
    }
}
