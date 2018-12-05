/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.server.dropzone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.lang.Bytes;
import org.bouncycastle.openpgp.PGPSignature;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.doc.DocContent;
import org.savapage.core.util.NumberUtil;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPSecretKeyInfo;
import org.savapage.lib.pgp.pdf.PdfPgpHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfPgpUploadHelper {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PdfPgpDropZoneFileResource.class);

    /**
     * .
     */
    private PdfPgpUploadHelper() {
    }

    /**
     *
     * @return The max upload size.
     */
    public static Bytes getMaxUploadSize() {
        // Note: Bytes.megabytes() is MiB (not MB).
        return Bytes.bytes(ConfigManager.instance().getConfigLong(
                Key.WEBAPP_PDFPGP_MAX_UPLOAD_FILE_MB,
                IConfigProp.WEBAPP_PDFPGP_MAX_UPLOAD_FILE_MB_V_DEFAULT)
                * NumberUtil.INT_THOUSAND * NumberUtil.INT_THOUSAND);
    }

    /**
     *
     * @param dotPfx
     *            If {@code true}, a '.' is prepended to each extension.
     * @return The list of supported Web Print file extensions.
     */
    public static List<String>
            getSupportedFileExtensions(final boolean dotPfx) {

        final List<String> list = new ArrayList<>();

        if (dotPfx) {
            list.add(String.format(".%s", DocContent.FILENAME_EXT_PDF));
        } else {
            list.add(DocContent.FILENAME_EXT_PDF);
        }
        return list;
    }

    /**
     * @param originatorIp
     *            The client IP address.
     * @param uploadedFile
     *            The uploaded file.
     * @param feedbackMsg
     *            The feeback message.
     * @throws PGPBaseException
     *             When PGP error.
     * @throws IOException
     *             When IO error.
     */
    public static void handleFileUpload(final String originatorIp,
            final FileUpload uploadedFile, final StringBuilder feedbackMsg)
            throws PGPBaseException, IOException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("[{}] uploaded file [{}] [{}]", originatorIp,
                    uploadedFile.getContentType(),
                    uploadedFile.getClientFileName());
        }

        try {

            final PGPSecretKeyInfo secKeyInfo =
                    ConfigManager.instance().getPGPSecretKeyInfo();

            final PGPSignature signature = PdfPgpHelper.instance().verify(
                    uploadedFile.getInputStream(), secKeyInfo.getPublicKey());

            if (signature == null) {
                throw new PGPBaseException("Signature is INVALID.");
            }

            feedbackMsg.append(String.format("Signed by %s",
                    secKeyInfo.getUids().get(0).toString()));

            feedbackMsg.append("<br>");
            feedbackMsg.append(
                    String.format("Signed on %s (clock of signer's computer)",
                            signature.getCreationTime()));

            feedbackMsg.append("<br>");
            feedbackMsg.append(
                    String.format("Key ID: %s", secKeyInfo.formattedKeyID()));

            feedbackMsg.append("<br>");
            feedbackMsg.append(String.format("Key fingerprint: %s",
                    secKeyInfo.formattedFingerPrint()));

        } catch (PGPBaseException e) {
            throw e;
        } catch (Exception e) {
            throw new PGPBaseException(e.getMessage());
        } finally {
            // Don't wait for garbage collect: delete now.
            uploadedFile.delete();
        }
    }

}
