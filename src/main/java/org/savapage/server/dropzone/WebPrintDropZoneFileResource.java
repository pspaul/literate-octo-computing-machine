/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.server.dropzone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.EnumUtils;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.apache.wicket.request.resource.AbstractResource;
import org.savapage.core.SpException;
import org.savapage.core.UnavailableException;
import org.savapage.core.UnavailableException.State;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.JsonHelper;
import org.savapage.core.util.NumberUtil;
import org.savapage.server.api.request.ApiRequestMixin;
import org.savapage.server.api.request.ApiResultCodeEnum;
import org.savapage.server.session.SpSession;
import org.savapage.server.webapp.WebAppHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The resource that handles DropZone file uploads. It reads the file items from
 * the request parameters and prints them to the user's inbox.
 * <p>
 * Additionally it writes the response's content type and body.
 * </p>
 * <p>
 * Checks for max upload size and supported file type are also done at the
 * client (JavaScript) side, before sending the file(s).
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class WebPrintDropZoneFileResource extends AbstractResource {

    /**
     * As in: {@code <input type="file">} .
     */
    public static final String UPLOAD_PARAM_NAME_FILE = "file";

    /**
     * As in: {@code ?font=CJK} .
     */
    public static final String UPLOAD_PARAM_NAME_FONT = "font";

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(WebPrintDropZoneFileResource.class);

    /**
     * .
     */
    public WebPrintDropZoneFileResource() {
        ServiceContext.setLocale(SpSession.get().getLocale());
    }

    @Override
    protected ResourceResponse
            newResourceResponse(final Attributes attributes) {

        final ResourceResponse resourceResponse = new ResourceResponse();

        final ServletWebRequest webRequest =
                (ServletWebRequest) attributes.getRequest();

        final SpSession session = SpSession.get();
        final String userId;

        if (session == null) {
            userId = null;
        } else {
            userId = session.getUserId();
        }

        if (userId == null) {
            final String msg = "No authenticated user.";
            LOGGER.warn(msg);
            throw new AbortWithHttpErrorCodeException(
                    HttpServletResponse.SC_UNAUTHORIZED, msg);
        }

        final InternalFontFamilyEnum defaultFont = ConfigManager
                .getConfigFontFamily(Key.REPORTS_PDF_INTERNAL_FONT_FAMILY);

        final String originatorIp =
                WebAppHelper.getClientIP(attributes.getRequest());

        ApiResultCodeEnum resultCode = ApiResultCodeEnum.OK;
        String resultText = "";

        final Map<String, Boolean> filesStatus = new HashMap<>();

        final Map<String, FileItem> fileItemsToHandle = new HashMap<>();

        ServiceContext.open();

        try {

            if (!WebPrintHelper.isWebPrintEnabled(originatorIp)) {
                throw new UnavailableException(State.PERMANENT,
                        "Service is not available.");
            }

            final MultipartServletWebRequest multiPartRequest =
                    webRequest.newMultipartWebRequest(
                            WebPrintHelper.getMaxUploadSize(), "ignored");

            final InternalFontFamilyEnum selectedFont = EnumUtils.getEnum(
                    InternalFontFamilyEnum.class,
                    multiPartRequest.getUrl()
                            .getQueryParameterValue(UPLOAD_PARAM_NAME_FONT)
                            .toString(defaultFont.toString()));

            /*
             * CRUCIAL: parse the file parts first, before getting the files :-)
             */
            multiPartRequest.parseFileParts();

            final Map<String, List<FileItem>> files =
                    multiPartRequest.getFiles();

            final List<FileItem> fileItemsAll = new ArrayList<>();

            for (final List<FileItem> list : files.values()) {
                for (final FileItem fileItem : list) {
                    fileItemsToHandle.put(fileItem.getName(), fileItem);
                    fileItemsAll.add(fileItem);
                }
            }

            if (files.get(UPLOAD_PARAM_NAME_FILE) == null) {
                LOGGER.debug("WebPrint [{}]: no files for key [{}]", userId,
                        UPLOAD_PARAM_NAME_FILE);
            }

            final int totFiles = fileItemsAll.size();

            if (totFiles == 0) {
                throw new DocContentPrintException("No files uploaded.");
            }

            int nFileWlk = 0;

            for (final FileItem fileItem : fileItemsAll) {

                final String fileKey = fileItem.getName();
                filesStatus.put(fileKey, Boolean.FALSE);

                nFileWlk++;

                final long start = System.currentTimeMillis();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("WebPrint [{}] {}/{} [{}] uploading... [{}]",
                            userId, nFileWlk, totFiles, fileItem.getName(),
                            NumberUtil.humanReadableByteCountSI(
                                    Locale.getDefault(), fileItem.getSize()));
                }
                WebPrintHelper.handleFileUpload(originatorIp, userId,
                        new FileUpload(fileItem), selectedFont);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("WebPrint [{}] {}/{} [{}] ....uploaded [{}].",
                            userId, nFileWlk, totFiles, fileItem.getName(),
                            DateUtil.formatDuration(
                                    System.currentTimeMillis() - start));
                }
                filesStatus.put(fileKey, Boolean.TRUE);
                fileItemsToHandle.remove(fileKey);
            }

        } catch (UnavailableException | DocContentPrintException e) {

            resultCode = ApiResultCodeEnum.INFO;
            resultText = e.getMessage();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("WebPrint [{}] [{}]: {}", userId,
                        e.getClass().getSimpleName(), e.getMessage());
            }

        } catch (FileUploadException e) {

            resultCode = ApiResultCodeEnum.INFO;
            resultText = e.getMessage();

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        String.format("WebPrint [%s] [%s]: %s", userId,
                                e.getClass().getSimpleName(), e.getMessage()),
                        e);
            }

        } catch (IOException e) {

            resultCode = ApiResultCodeEnum.WARN;
            resultText = e.getMessage();

            LOGGER.warn(String.format("WebPrint [%s] [%s]: %s", userId,
                    e.getClass().getSimpleName(), e.getMessage()), e);

        } catch (Exception e) {

            resultCode = ApiResultCodeEnum.ERROR;
            resultText = e.getMessage();

            LOGGER.error(String.format("WebPrint [%s] [%s]: %s", userId,
                    e.getClass().getSimpleName(), e.getMessage()), e);

        } finally {

            // Clean up any file items not handled.
            for (final FileItem fileItem : fileItemsToHandle.values()) {
                fileItem.delete();
            }

            ServiceContext.close();
        }

        writeResponse(resourceResponse, resultCode, resultText, filesStatus);

        return resourceResponse;
    }

    /**
     * Sets the response's content type and body.
     *
     * @param response
     *            The {@link ResourceResponse}.
     * @param code
     *            The result code.
     * @param text
     *            The result text.
     * @param fileStatus
     *            The status of each file uploaded.
     */
    private void writeResponse(final ResourceResponse response,
            final ApiResultCodeEnum code, final String text,
            final Map<String, Boolean> fileStatus) {

        response.setContentType("application/json");

        final String responseContent;

        try {
            final Map<String, Object> result =
                    ApiRequestMixin.createApiResultText(code, text);

            result.put("filesStatus", fileStatus);
            responseContent = JsonHelper.objectMapAsString(result);

        } catch (IOException e) {
            throw new SpException(e);
        }

        response.setWriteCallback(new WriteCallback() {
            @Override
            public void writeData(final Attributes attributes)
                    throws IOException {
                attributes.getResponse().write(responseContent);
            }
        });
    }

}
