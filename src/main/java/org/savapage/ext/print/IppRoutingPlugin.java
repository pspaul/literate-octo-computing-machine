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
package org.savapage.ext.print;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.ipp.routing.IppRoutingContext;
import org.savapage.core.ipp.routing.IppRoutingResult;
import org.savapage.core.pdf.ITextPdfCreator;
import org.savapage.core.util.QRCodeException;
import org.savapage.core.util.QRCodeHelper;
import org.savapage.ext.ServerPlugin;
import org.savapage.ext.ServerPluginContext;
import org.savapage.ext.ServerPluginException;
import org.savapage.ext.rest.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppRoutingPlugin implements ServerPlugin {

    /**
     * The {@link Logger}.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppRoutingPlugin.class);

    /**
     * QR-code position.
     */
    private enum QrPos {
        /** top-left. */
        TL,
        /** top-right. */
        TR,
        /** bottom-left. */
        BL,
        /** bottom-right. */
        BR
    }

    /**
     * Property key prefix.
     */
    private static final String PROP_KEY_PFX = "routing.";

    /** */
    private static final String PROP_KEY_ROUTING_ID = PROP_KEY_PFX + "id";

    /** */
    private static final String ROUTING_ID_PLACEHOLDER_QRCODE = "$pdf.qrcode$";

    /**
     * Property key prefix.
     */
    private static final String PROP_KEY_PFX_PDF = PROP_KEY_PFX + "pdf.";

    /** */
    private static final String PROP_KEY_PDF_QRCODE =
            PROP_KEY_PFX_PDF + "qrcode";

    /** */
    private static final String PROP_KEY_PFX_PDF_QRCODE_REST =
            PROP_KEY_PFX_PDF + "qrcode.rest.";

    /** */
    private static final String PROP_KEY_PDF_QRCODE_REST_URI =
            PROP_KEY_PFX_PDF_QRCODE_REST + "uri";
    /** */
    private static final String PROP_KEY_PDF_QRCODE_REST_USER =
            PROP_KEY_PFX_PDF_QRCODE_REST + "user";
    /** */
    private static final String PROP_KEY_PDF_QRCODE_REST_PW =
            PROP_KEY_PFX_PDF_QRCODE_REST + "password";

    /** */
    private static final String PROP_KEY_PFX_PDF_QRCODE_REST_POST =
            PROP_KEY_PFX_PDF_QRCODE_REST + "post.";

    /** */
    private static final String PROP_KEY_PDF_QRCODE_REST_POST_REQ_ENTITY =
            PROP_KEY_PFX_PDF_QRCODE_REST_POST + "request.entity";

    /** */
    private static final String PDF_QRCODE_REQ_PLACEHOLDER_USERID = "$user_id$";
    /** */
    private static final String PDF_QRCODE_REQ_PLACEHOLDER_CLIENT_IP =
            "$client_ip$";
    /** */
    private static final String PDF_QRCODE_REQ_PLACEHOLDER_PRINTER_NAME =
            "$printer_name$";
    /** */
    private static final String PDF_QRCODE_REQ_PLACEHOLDER_JOB_NAME =
            "$job_name$";
    /** */
    private static final String PDF_QRCODE_REQ_PLACEHOLDER_JOB_TIME =
            "$job_time$";

    /** */
    private static final String PROP_KEY_PDF_QRCODE_REST_POST_REQ_MEDIATYPE =
            PROP_KEY_PFX_PDF_QRCODE_REST_POST + "request.mediatype";

    /** */
    private static final String PROP_KEY_PDF_QRCODE_REST_POST_RSP_MEDIATYPE =
            PROP_KEY_PFX_PDF_QRCODE_REST_POST + "response.mediatype";

    /** */
    private static final String PDF_QRCODE_PLACEHOLDER_UUID = "$uuid$";

    /** */
    private static final String PROP_KEY_PDF_QRCODE_SIZE =
            PROP_KEY_PDF_QRCODE + ".size";

    /** */
    private static final String PROP_KEY_PDF_QRCODE_QUIET_ZONE =
            PROP_KEY_PDF_QRCODE + ".qz";

    /** */
    private static final String PROP_KEY_PDF_QRCODE_POS =
            PROP_KEY_PDF_QRCODE + ".pos";

    /** */
    private static final String PROP_KEY_PDF_QRCODE_POS_MARGIN_X =
            PROP_KEY_PDF_QRCODE_POS + ".margin.x";

    /** */
    private static final String PROP_KEY_PDF_QRCODE_POS_MARGIN_Y =
            PROP_KEY_PDF_QRCODE_POS + ".margin.y";

    /** */
    private static final String PROP_KEY_PDF_HEADER =
            PROP_KEY_PFX_PDF + "header";

    /** */
    private static final String PROP_KEY_PDF_HEADER_FONT_SIZE =
            PROP_KEY_PDF_HEADER + ".font.size";

    /** */
    private static final String PROP_KEY_PDF_HEADER_MARGIN_TOP =
            PROP_KEY_PDF_HEADER + ".margin.top";

    /** */
    private static final String PROP_KEY_PDF_FOOTER =
            PROP_KEY_PFX_PDF + "footer";

    /** */
    private static final String PROP_KEY_PDF_FOOTER_FONT_SIZE =
            PROP_KEY_PDF_FOOTER + ".font.size";

    /** */
    private static final String PROP_KEY_PDF_FOOTER_MARGIN_BOTTOM =
            PROP_KEY_PDF_FOOTER + ".margin.bottom";

    /** */
    private static final String PROP_KEY_PFX_PDF_INFO =
            PROP_KEY_PFX_PDF + "info";
    /** */
    private static final String PROP_KEY_PDF_INFO_TITLE =
            PROP_KEY_PFX_PDF_INFO + ".title";
    /** */
    private static final String PROP_KEY_PDF_INFO_SUBJECT =
            PROP_KEY_PFX_PDF_INFO + ".subject";
    /** */
    private static final String PROP_KEY_PDF_INFO_AUTHOR =
            PROP_KEY_PFX_PDF_INFO + ".author";
    /** */
    private static final String PROP_KEY_PDF_INFO_KEYWORDS =
            PROP_KEY_PFX_PDF_INFO + ".keywords";

    /** */
    private String id;

    /** */
    private String name;

    /** */
    private RestClient codeQRRestClient;

    /** */
    private String codeQRRestReqEntity;

    /** */
    private String codeQRRestReqMediaType;
    /** */
    private String codeQRRestRspMediaType;

    /** */
    private String codeQR;

    /** */
    private boolean codeQRUUID;

    /** */
    private int codeQRQuiteZoneMM;

    /** */
    private QrPos codeQRPos;
    /** */
    private float codeQRPosMarginPointsX;
    /** */
    private float codeQRPosMarginPointsY;

    /** */
    private int codeQRSizeMM;

    /** */
    private String header;

    /** */
    private Font headerFont;

    /** */
    private float headerMarginPointsTop;

    /** */
    private String footer;

    /** */
    private Font footerFont;

    /** */
    private float footerMarginPointsBottom;

    /** */
    private String routingId;

    /** */
    Map<String, String> pdfInfo;

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void onStart() throws ServerPluginException {
    }

    @Override
    public void onStop() throws ServerPluginException {
    }

    @Override
    public void onInit(final String pluginId, final String pluginName,
            final boolean live, final boolean online, final Properties props,
            final ServerPluginContext context) throws ServerPluginException {

        this.id = pluginId;
        this.name = pluginName;

        //
        this.routingId = props.getProperty(PROP_KEY_ROUTING_ID);

        //
        this.onInitCodeQRRetrieval(context, props);

        this.codeQRSizeMM =
                Integer.valueOf(props.getProperty(PROP_KEY_PDF_QRCODE_SIZE))
                        .intValue();

        this.codeQRQuiteZoneMM = Integer.valueOf(
                props.getProperty(PROP_KEY_PDF_QRCODE_QUIET_ZONE, "0"));

        this.codeQRPos = EnumUtils.getEnum(QrPos.class,
                props.getProperty(PROP_KEY_PDF_QRCODE_POS));

        if (this.codeQRPos == null) {
            this.codeQRPos = QrPos.TL;
        }

        this.codeQRPosMarginPointsX =
                QRCodeHelper.pdfMMToPoints(Integer
                        .valueOf(props.getProperty(
                                PROP_KEY_PDF_QRCODE_POS_MARGIN_X, "0"))
                        .intValue());

        this.codeQRPosMarginPointsY =
                QRCodeHelper.pdfMMToPoints(Integer
                        .valueOf(props.getProperty(
                                PROP_KEY_PDF_QRCODE_POS_MARGIN_Y, "0"))
                        .intValue());

        this.onInitHeaderFooter(props);
        this.onInitPdfInfo(props);
    }

    /**
     * @param ctx
     *            Context.
     * @param props
     *            Configuration properties.
     * @throws ServerPluginException
     *             If error.
     */
    private void onInitCodeQRRetrieval(final ServerPluginContext ctx,
            final Properties props) throws ServerPluginException {

        final String restUri = props.getProperty(PROP_KEY_PDF_QRCODE_REST_URI);

        if (restUri == null) {
            this.codeQR = props.getProperty(PROP_KEY_PDF_QRCODE);
            this.codeQRUUID = this.codeQR.equals(PDF_QRCODE_PLACEHOLDER_UUID);
            return;
        }

        final URI uri;
        try {
            uri = new URI(restUri);
        } catch (URISyntaxException e) {
            throw new ServerPluginException(e.getMessage());
        }

        final String restUser =
                props.getProperty(PROP_KEY_PDF_QRCODE_REST_USER);

        if (restUser == null) {
            this.codeQRRestClient = ctx.createRestClient(uri);
        } else {
            this.codeQRRestClient = ctx.createRestClient(uri, restUser,
                    props.getProperty(PROP_KEY_PDF_QRCODE_REST_PW, ""));
        }

        this.codeQRRestReqEntity =
                props.getProperty(PROP_KEY_PDF_QRCODE_REST_POST_REQ_ENTITY);

        this.codeQRRestReqMediaType =
                props.getProperty(PROP_KEY_PDF_QRCODE_REST_POST_REQ_MEDIATYPE);

        this.codeQRRestRspMediaType =
                props.getProperty(PROP_KEY_PDF_QRCODE_REST_POST_RSP_MEDIATYPE);

        if (this.codeQRRestReqEntity == null
                || this.codeQRRestReqMediaType == null
                || this.codeQRRestRspMediaType == null) {
            throw new ServerPluginException(
                    String.format("One or more %s properties are missing.",
                            PROP_KEY_PFX_PDF_QRCODE_REST_POST));
        }
    }

    /**
     *
     * @param props
     *            Configuration properties.
     */
    private void onInitPdfInfo(final Properties props) {

        this.pdfInfo = new HashMap<>();

        String value;

        value = props.getProperty(PROP_KEY_PDF_INFO_AUTHOR);
        if (StringUtils.isNotBlank(value)) {
            this.pdfInfo.put(ITextPdfCreator.PDF_INFO_KEY_AUTHOR, value);
        }

        value = props.getProperty(PROP_KEY_PDF_INFO_TITLE);
        if (StringUtils.isNotBlank(value)) {
            this.pdfInfo.put(ITextPdfCreator.PDF_INFO_KEY_TITLE, value);
        }

        value = props.getProperty(PROP_KEY_PDF_INFO_SUBJECT);
        if (StringUtils.isNotBlank(value)) {
            this.pdfInfo.put(ITextPdfCreator.PDF_INFO_KEY_SUBJECT, value);
        }

        value = props.getProperty(PROP_KEY_PDF_INFO_KEYWORDS);
        if (StringUtils.isNotBlank(value)) {
            this.pdfInfo.put(ITextPdfCreator.PDF_INFO_KEY_KEYWORDS, value);
        }
    }

    /**
     *
     * @param props
     *            Configuration properties.
     */
    private void onInitHeaderFooter(final Properties props) {

        this.header = props.getProperty(PROP_KEY_PDF_HEADER);

        if (this.header != null) {

            final float headerFontSize = Float.valueOf(
                    props.getProperty(PROP_KEY_PDF_HEADER_FONT_SIZE, "8"))
                    .floatValue();

            this.headerFont = new Font(Font.HELVETICA, headerFontSize);
            this.headerFont.setColor(Color.GRAY);

            this.headerMarginPointsTop =
                    QRCodeHelper.pdfMMToPoints(Integer
                            .valueOf(props.getProperty(
                                    PROP_KEY_PDF_HEADER_MARGIN_TOP, "10"))
                            .intValue());
        }

        this.footer = props.getProperty(PROP_KEY_PDF_FOOTER);

        if (this.footer != null) {

            final float footerFontSize = Float.valueOf(
                    props.getProperty(PROP_KEY_PDF_FOOTER_FONT_SIZE, "8"))
                    .floatValue();

            this.footerFont = new Font(Font.HELVETICA, footerFontSize);
            this.footerFont.setColor(Color.GRAY);

            this.footerMarginPointsBottom = QRCodeHelper.pdfMMToPoints(Integer
                    .valueOf(props.getProperty(
                            PROP_KEY_PDF_FOOTER_MARGIN_BOTTOM, "10"))
                    .intValue());
        }
    }

    /**
     * @param stamper
     *            {@link PdfStamper}
     */
    private void onRoutingPdfInfo(final PdfStamper stamper) {
        if (!this.pdfInfo.isEmpty()) {
            @SuppressWarnings("unchecked")
            final HashMap<String, String> info = stamper.getReader().getInfo();
            info.putAll(this.pdfInfo);
            stamper.setMoreInfo(info);
        }
    }

    /**
     * @param stamper
     *            {@link PdfStamper}
     */
    private void onRoutingPdfPermissions(final PdfStamper stamper) {
        int iPermissions = 0;
        boolean bStrength = true; // 128 bit: TODO

        iPermissions |= PdfWriter.ALLOW_COPY;
        iPermissions |= PdfWriter.ALLOW_SCREENREADERS;
        iPermissions |= PdfWriter.ALLOW_PRINTING;
        iPermissions |= PdfWriter.ALLOW_DEGRADED_PRINTING;

        // Don't allow ...

        // iPermissions |= PdfWriter.ALLOW_FILL_IN;
        // iPermissions |= PdfWriter.ALLOW_ASSEMBLY;
        // iPermissions |= PdfWriter.ALLOW_MODIFY_CONTENTS;
        // iPermissions |= PdfWriter.ALLOW_MODIFY_ANNOTATIONS;

        try {
            stamper.setEncryption(bStrength, null, null, iPermissions);
        } catch (DocumentException e) {
            throw new SpException(e);
        }
    }

    /**
     * @param ctx
     *            The routing context.
     * @param res
     *            The routing result to be filled.
     */
    public void onRouting(final IppRoutingContext ctx,
            final IppRoutingResult res) {

        if (this.routingId != null
                && !this.routingId.equals(ROUTING_ID_PLACEHOLDER_QRCODE)) {
            res.setRoutingId(this.routingId);
        }

        //
        final Image codeQRImage;
        final Image codeQRImageBg;
        try {
            codeQRImage = this.onRoutingQRCodeImage(ctx, res);
            if (codeQRImage == null) {
                codeQRImageBg = null;
            } else {
                codeQRImageBg = onRoutingQRCodeImageBg();
            }
        } catch (QRCodeException | DocumentException | IOException e) {
            LOGGER.error(e.getMessage());
            throw new SpException(e.getMessage(), e);
        }

        //
        final Phrase phraseHeader;

        if (this.header == null) {
            phraseHeader = null;
        } else {
            phraseHeader = new Phrase(this.header, this.headerFont);
        }

        final Phrase phraseFooter;

        if (this.footer == null) {
            phraseFooter = null;
        } else {
            phraseFooter = new Phrase(this.footer, this.footerFont);
        }

        //
        if (codeQRImage == null && phraseHeader == null
                && phraseFooter == null) {
            return;
        }

        //
        final File fileIn = ctx.getPdfToPrint();
        final File fileOut = new File(
                fileIn.getAbsolutePath() + UUID.randomUUID().toString());

        PdfReader reader = null;
        PdfStamper stamper = null;
        boolean processed = false;

        try (InputStream pdfIn = new FileInputStream(fileIn);
                OutputStream pdfSigned = new FileOutputStream(fileOut);) {

            reader = new PdfReader(pdfIn);
            stamper = new PdfStamper(reader, pdfSigned);

            // First thing to do.
            this.onRoutingPdfPermissions(stamper);

            final int nPages = reader.getNumberOfPages();
            for (int nPage = 1; nPage <= nPages; nPage++) {

                final PdfContentByte content = stamper.getOverContent(nPage);

                final Rectangle rect = reader.getPageSize(nPage);
                final int rotation = reader.getPageRotation(nPage);

                if (codeQRImage != null) {
                    this.setImagePositionOnPage(rect, rotation, codeQRImage,
                            codeQRImageBg);
                    if (codeQRImageBg != null) {
                        content.addImage(codeQRImageBg);
                    }
                    content.addImage(codeQRImage);
                }

                if (phraseHeader != null || phraseFooter != null) {

                    final float xHeaderFooter;
                    final float yHeader;

                    if (rotation == 0) {
                        xHeaderFooter = (rect.getRight() - rect.getLeft()) / 2;
                        yHeader = rect.getHeight() - this.headerMarginPointsTop;
                    } else {
                        xHeaderFooter = (rect.getTop() - rect.getBottom()) / 2;
                        yHeader = rect.getWidth() - this.headerMarginPointsTop;
                    }

                    if (phraseHeader != null) {
                        ColumnText.showTextAligned(content,
                                Element.ALIGN_CENTER, phraseHeader,
                                xHeaderFooter, yHeader, 0);
                    }

                    if (phraseFooter != null) {
                        final float yFooter = this.footerMarginPointsBottom;
                        ColumnText.showTextAligned(content,
                                Element.ALIGN_CENTER, phraseFooter,
                                xHeaderFooter, yFooter, 0);
                    }
                }
            } // end-for

            this.onRoutingPdfInfo(stamper);

            stamper.close();
            reader.close();
            reader = null;

            ctx.replacePdfToPrint(fileOut);
            processed = true;

        } catch (IOException | DocumentException e) {
            LOGGER.error(e.getMessage());
            throw new SpException(e.getMessage(), e);
        } finally {
            closeQuietly(stamper);
            if (reader != null) {
                reader.close();
            }
            if (!processed) {
                fileOut.delete();
            }
        }
    }

    /**
     * @param ctx
     *            The routing context.
     * @param res
     *            The routing result to be filled.
     * @return PDF QR-code image, or {@code null} if not applicable.
     * @throws QRCodeException
     *             QR-code image error
     * @throws DocumentException
     *             PDF image error.
     * @throws IOException
     *             PDF image error.
     */
    private Image onRoutingQRCodeImage(final IppRoutingContext ctx,
            final IppRoutingResult res)
            throws QRCodeException, DocumentException, IOException {

        final String codeQRWrk;

        if (this.codeQRRestClient != null) {

            final String[][] placeholderValues = new String[][] {
                    { PDF_QRCODE_REQ_PLACEHOLDER_CLIENT_IP,
                            ctx.getOriginatorIp() },
                    { PDF_QRCODE_REQ_PLACEHOLDER_JOB_NAME, ctx.getJobName() },
                    { PDF_QRCODE_REQ_PLACEHOLDER_JOB_TIME,
                            this.codeQRRestClient
                                    .toISODateTimeZ(ctx.getTransactionDate()) },
                    { PDF_QRCODE_REQ_PLACEHOLDER_PRINTER_NAME,
                            ctx.getPrinterName() },
                    { PDF_QRCODE_PLACEHOLDER_UUID,
                            UUID.randomUUID().toString() },
                    { PDF_QRCODE_REQ_PLACEHOLDER_USERID, ctx.getUserId() } //
            };

            String entity = this.codeQRRestReqEntity;

            for (final String[] placeholderValue : placeholderValues) {
                entity = StringUtils.replace(entity, placeholderValue[0],
                        placeholderValue[1]);
            }

            codeQRWrk = this.codeQRRestClient.post(entity,
                    this.codeQRRestReqMediaType, this.codeQRRestRspMediaType);
            LOGGER.debug("RESTful POST: {} -> {}", entity, codeQRWrk);

        } else if (this.codeQR == null) {
            codeQRWrk = null;
        } else {
            if (this.codeQRUUID) {
                codeQRWrk = UUID.randomUUID().toString();
            } else {
                codeQRWrk = this.codeQR;
            }
        }

        if (codeQRWrk != null && this.routingId != null
                && this.routingId.equals(ROUTING_ID_PLACEHOLDER_QRCODE)) {
            res.setRoutingId(codeQRWrk);
        }

        final Image codeQRImage;

        if (codeQRWrk == null) {
            codeQRImage = null;
        } else {
            codeQRImage =
                    QRCodeHelper.createPdfImage(codeQRWrk, this.codeQRSizeMM);
        }
        return codeQRImage;
    }

    /**
     * @return PDF QR-code Quiet Zone background image, or {@code null} if not
     *         applicable.
     * @throws QRCodeException
     *             QR-code image error
     */
    private Image onRoutingQRCodeImageBg() throws QRCodeException {
        if (this.codeQRQuiteZoneMM == 0) {
            return null;
        }
        return QRCodeHelper.createPdfImageBackground(
                this.codeQRSizeMM + 2 * this.codeQRQuiteZoneMM, Color.WHITE);
    }

    /**
     * Sets the absolute position of (background) image.
     *
     * @param pageRect
     *            Page rectangle.
     * @param pageRotation
     *            Page rotation
     * @param image
     *            The QR image.
     * @param imageBg
     *            Background image (Quiet Zone). Can be {@code null}.
     */
    private void setImagePositionOnPage(final Rectangle pageRect,
            final int pageRotation, final Image image, final Image imageBg) {

        final float imgPointsHeight = image.getScaledHeight();
        final float imgPointsWidth = image.getScaledWidth();

        final float codeQRQuiteZonePoints;

        if (this.codeQRQuiteZoneMM == 0) {
            codeQRQuiteZonePoints = 0f;
        } else {
            codeQRQuiteZonePoints =
                    QRCodeHelper.pdfMMToPoints(this.codeQRQuiteZoneMM);
        }

        final float xImage;
        final float yImage;

        switch (this.codeQRPos) {
        case TL:
            xImage = this.codeQRPosMarginPointsX;
            yImage = pageRect.getTop() - this.codeQRPosMarginPointsY
                    - imgPointsHeight;
            break;

        case TR:
            xImage = pageRect.getRight() - this.codeQRPosMarginPointsX
                    - imgPointsWidth;
            yImage = pageRect.getTop() - this.codeQRPosMarginPointsY
                    - imgPointsHeight;
            break;

        case BL:
            xImage = this.codeQRPosMarginPointsX;
            yImage = pageRect.getBottom() + this.codeQRPosMarginPointsY;
            break;

        case BR:
            // no code intended
        default:
            xImage = pageRect.getRight() - this.codeQRPosMarginPointsX
                    - imgPointsWidth;
            yImage = pageRect.getBottom() + this.codeQRPosMarginPointsY;
            break;
        }

        // TODO pageRotation.

        if (imageBg != null) {
            imageBg.setAbsolutePosition(xImage - codeQRQuiteZonePoints,
                    yImage - codeQRQuiteZonePoints);
        }
        image.setAbsolutePosition(xImage, yImage);
    }

    /**
     * Closes a Stamper ignoring exceptions.
     *
     * @param stamper
     *            The stamper.
     */
    private static void closeQuietly(final PdfStamper stamper) {
        if (stamper != null) {
            try {
                stamper.close();
            } catch (DocumentException | IOException e) {
                // no code intended.
            }
        }
    }
}
