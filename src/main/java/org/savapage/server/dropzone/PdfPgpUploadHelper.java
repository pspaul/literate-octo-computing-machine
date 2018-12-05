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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.util.lang.Bytes;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.doc.DocContent;
import org.savapage.core.util.NumberUtil;
import org.savapage.lib.pgp.PGPKeyInfo;
import org.savapage.lib.pgp.PGPSecretKeyInfo;
import org.savapage.lib.pgp.PGPSignatureInfo;
import org.savapage.lib.pgp.pdf.PdfPgpHelper;
import org.savapage.server.pages.MarkupHelper;
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
     *            The HTML feedback message to append on.
     */
    public static void handleFileUpload(final String originatorIp,
            final FileUpload uploadedFile, final StringBuilder feedbackMsg) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("[{}] uploaded file [{}] [{}]", originatorIp,
                    uploadedFile.getContentType(),
                    uploadedFile.getClientFileName());
        }

        try {

            final PGPSecretKeyInfo secKeyInfo =
                    ConfigManager.instance().getPGPSecretKeyInfo();

            final PGPSignatureInfo sigInfo = PdfPgpHelper.instance().verify(
                    uploadedFile.getInputStream(), secKeyInfo.getPublicKey());

            final boolean isSigValid = sigInfo.isValid();
            final boolean isSigTrusted = sigInfo.getSignature()
                    .getKeyID() == secKeyInfo.getPrivateKey().getKeyID();

            /*
             * Messages are modeled after GPG CLI output.
             */
            feedbackMsg.append("<div class=\"sp-pdfpgp-verify-entry")
                    .append(" ").append(MarkupHelper.CSS_TXT_WRAP).append(" ");

            final String imgSrc;

            if (isSigValid) {
                feedbackMsg.append(MarkupHelper.CSS_TXT_VALID).append(" ")
                        .append("sp-pdfpgp-verify-entry-valid");
                imgSrc = "/famfamfam-silk/tick.png";
            } else {
                feedbackMsg.append(MarkupHelper.CSS_TXT_WARN).append(" ")
                        .append("sp-pdfpgp-verify-entry-warn");
                imgSrc = "/famfamfam-silk/error.png";
            }
            feedbackMsg.append("\">");

            appendFileInfo(uploadedFile, feedbackMsg, imgSrc);

            if (isSigValid || isSigTrusted) {

                if (isSigValid) {
                    feedbackMsg.append("Good");
                } else {
                    feedbackMsg.append("BAD");
                }

                feedbackMsg.append(" signature from ")
                        .append(secKeyInfo.getUids().get(0).toString())
                        .append(" &lt;")
                        .append(secKeyInfo.getUids().get(0).getAddress())
                        .append("&gt;");

            } else {
                feedbackMsg.append(String
                        .format("Can't check signature: public key not found"));
            }

            feedbackMsg.append("<br><br><small>");
            feedbackMsg.append(String.format(
                    "Signature made %s (clock of signer's computer)",
                    sigInfo.getSignature().getCreationTime()));

            feedbackMsg.append("<br>");
            feedbackMsg.append(String.format("Key ID: %s", PGPKeyInfo
                    .formattedKeyID(sigInfo.getSignature().getKeyID())));

            if (isSigTrusted) {
                feedbackMsg.append("<br>");
                feedbackMsg.append(String.format("Key fingerprint: %s",
                        secKeyInfo.formattedFingerPrint()));
            }

            feedbackMsg.append("</small></div>");

        } catch (Exception e) {

            feedbackMsg.append("<div class=\"sp-pdfpgp-verify-entry")
                    .append(" ").append(MarkupHelper.CSS_TXT_WRAP).append(" ")
                    .append(MarkupHelper.CSS_TXT_ERROR).append(" ")
                    .append("sp-pdfpgp-verify-entry-error").append("\">");
            appendFileInfo(uploadedFile, feedbackMsg,
                    "/famfamfam-silk/cross.png");

            feedbackMsg.append("The signature could not be verified");
            feedbackMsg.append("<br><br>");
            feedbackMsg.append("<small>").append(e.getMessage())
                    .append("</small>");
            feedbackMsg.append("</div>");

        } finally {
            // Don't wait for garbage collect: delete now.
            uploadedFile.delete();
        }
    }

    /**
     *
     * @param uploadedFile
     *            Uploaded file
     * @param feedbackMsg
     *            Message to append on.
     * @param imgSrc
     *            image URL path.
     */
    private static void appendFileInfo(final FileUpload uploadedFile,
            final StringBuilder feedbackMsg, final String imgSrc) {
        feedbackMsg.append("<span class=\"sp-pdfpgp-file\">").append(
                "<img class=\"sp-pdfpgp-status-img\" height=\"20\" src=\"")
                .append(imgSrc).append("\">").append("&nbsp;&nbsp;")
                .append(uploadedFile.getClientFileName()).append(" &bull; ")
                .append(NumberUtil
                        .humanReadableByteCount(uploadedFile.getSize(), true))
                .append("</span>");
        feedbackMsg.append("<br><br>");
    }
}
