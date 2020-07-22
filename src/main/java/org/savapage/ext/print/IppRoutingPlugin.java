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

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.ipp.routing.IppRoutingContext;
import org.savapage.core.ipp.routing.IppRoutingResult;
import org.savapage.core.pdf.IPdfPageProps;
import org.savapage.core.pdf.ITextPdfCreator;
import org.savapage.core.util.CupsPrinterUriHelper;
import org.savapage.core.util.JsonHelper;
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
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
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
     *
     */
    private static class RoutingDataBase {

        /** */
        protected String pdfQrCodeContent;

        /** Quiet Zone. */
        protected int pdfQrCodeQzMM;

        /** */
        protected QrCodeAnchorEnum pdfQrCodePosAnchor;
        /** */
        protected float pdfQrCodePosMarginXPt;
        /** */
        protected float pdfQrCodePosMarginYPt;
        /** */
        protected int pdfQrCodeSizeMM;
        /** */
        protected String pdfHeaderText;
        /** */
        protected Font pdfHeaderFont;
        /** */
        protected float pdfHeaderMarginTopPt;
        /** */
        protected String pdfFooterText;
        /** */
        protected Font pdfFooterFont;
        /** */
        protected float pdfFooterMarginBottomPt;
        /** */
        protected String routingId;
        /** */
        protected Map<String, String> pdfInfo;

        /**
         *
         * @param obj
         *            Object to copy from.
         */
        void copy(final RoutingDataBase obj) {

            this.pdfQrCodeContent = obj.pdfQrCodeContent;
            this.pdfQrCodeQzMM = obj.pdfQrCodeQzMM;
            this.pdfQrCodePosAnchor = obj.pdfQrCodePosAnchor;
            this.pdfQrCodePosMarginXPt = obj.pdfQrCodePosMarginXPt;
            this.pdfQrCodePosMarginYPt = obj.pdfQrCodePosMarginYPt;
            this.pdfQrCodeSizeMM = obj.pdfQrCodeSizeMM;
            this.pdfHeaderText = obj.pdfHeaderText;
            this.pdfHeaderFont = obj.pdfHeaderFont;
            this.pdfHeaderMarginTopPt = obj.pdfHeaderMarginTopPt;
            this.pdfFooterText = obj.pdfFooterText;
            this.pdfFooterFont = obj.pdfFooterFont;
            this.pdfFooterMarginBottomPt = obj.pdfFooterMarginBottomPt;
            this.routingId = obj.routingId;

            if (obj.pdfInfo != null) {
                this.pdfInfo = new HashMap<>();
                this.pdfInfo.putAll(obj.pdfInfo);
            }
        }
    }

    /**
     *
     */
    private static final class RoutingData extends RoutingDataBase {

        /** */
        private final UUID routingUUID;

        /**
         * PDF QR-code image, or {@code null} if not applicable.
         */
        private Image pdfQrCodeImage;

        /**
         * PDF QR-code Quiet Zone background image, or {@code null} if not
         * applicable.
         */
        private Image pdfQrCodeImageBg;

        /**
         *
         * @param template
         *            Data template.
         */
        RoutingData(final RoutingDataBase template) {

            this.copy(template);

            this.routingUUID = UUID.randomUUID();

            if (this.routingId != null
                    && this.routingId.equals(ROUTING_ID_PLACEHOLDER_UUID)) {
                this.routingId = this.routingUUID.toString();
            }
        }

        /**
         * Updates object with routing data response.
         *
         * @param rsp
         *            Routing data response.
         */
        void update(final IppRoutingDto rsp) {

            this.routingId = rsp.getId();

            final IppRoutingDto.PdfData pdf = rsp.getPdf();
            if (pdf == null) {
                return;
            }

            this.update(pdf.getFooter());
            this.update(pdf.getHeader());
            this.update(pdf.getInfo());
            this.update(pdf.getQrcode());
        }

        /**
         * Updates object with routing Footer response.
         *
         * @param footer
         *            Routing Footer response.
         */
        void update(final IppRoutingDto.Footer footer) {

            if (footer == null || footer.getText() == null) {
                return;
            }

            this.pdfFooterText = footer.getText();

            final IppRoutingDto.Font font = footer.getFont();
            if (font != null && font.getSize() != null) {
                this.pdfFooterFont = new Font(Font.HELVETICA,
                        Float.valueOf(font.getSize()).floatValue());
                this.pdfFooterFont.setColor(Color.GRAY);
            }

            final IppRoutingDto.MarginFooter margin = footer.getMargin();
            if (margin != null && margin.getBottom() != null) {
                this.pdfFooterMarginBottomPt =
                        QRCodeHelper.pdfMMToPoints(margin.getBottom());
            }

        }

        /**
         * Updates object with routing Header response.
         *
         * @param header
         *            Routing Header response.
         */
        void update(final IppRoutingDto.Header header) {

            if (header == null || header.getText() == null) {
                return;
            }

            this.pdfHeaderText = header.getText();

            final IppRoutingDto.Font font = header.getFont();
            if (font != null && font.getSize() != null) {
                this.pdfHeaderFont = new Font(Font.HELVETICA,
                        Float.valueOf(font.getSize()).floatValue());
                this.pdfHeaderFont.setColor(Color.GRAY);
            }

            final IppRoutingDto.MarginHeader margin = header.getMargin();
            if (margin != null && margin.getTop() != null) {
                this.pdfHeaderMarginTopPt =
                        QRCodeHelper.pdfMMToPoints(margin.getTop());
            }
        }

        /**
         * Updates pdfinfo.
         *
         * @param key
         *            pdfinfo key.
         * @param value
         *            pdfinfo value.
         */
        void updatePdfInfo(final String key, final String value) {
            if (value == null) {
                return;
            }
            this.pdfInfo.put(key, value);
        }

        /**
         * Updates object with routing PdfInfo response.
         *
         * @param info
         *            Routing PdfInfo response.
         */
        void update(final IppRoutingDto.PdfInfo info) {

            if (info == null) {
                return;
            }

            if (this.pdfInfo == null) {
                this.pdfInfo = new HashMap<>();
            }

            this.updatePdfInfo(ITextPdfCreator.PDF_INFO_KEY_TITLE,
                    info.getTitle());

            this.updatePdfInfo(ITextPdfCreator.PDF_INFO_KEY_SUBJECT,
                    info.getSubject());

            this.updatePdfInfo(ITextPdfCreator.PDF_INFO_KEY_AUTHOR,
                    info.getAuthor());

            if (info.getKeywords() != null) {

                final StringBuilder kws = new StringBuilder();

                for (final String kw : info.getKeywords()) {
                    if (StringUtils.isNotBlank(kw)) {
                        kws.append(kw).append(" ");
                    }
                }
                this.updatePdfInfo(ITextPdfCreator.PDF_INFO_KEY_KEYWORDS,
                        kws.toString().trim());
            }
        }

        /**
         * Updates object with routing QrCode response.
         *
         * @param qrcode
         *            Routing QrCode response.
         */
        void update(final IppRoutingDto.QrCode qrcode) {

            if (qrcode == null) {
                return;
            }

            if (StringUtils.isNotBlank(qrcode.getContent())) {
                this.pdfQrCodeContent = qrcode.getContent();
            }
            if (qrcode.getSize() != null) {
                this.pdfQrCodeSizeMM = qrcode.getSize().intValue();
            }
            if (qrcode.getQz() != null) {
                this.pdfQrCodeQzMM = qrcode.getQz().intValue();
            }

            final IppRoutingDto.QrCodePosition pos = qrcode.getPos();
            if (pos == null) {
                return;
            }

            if (pos.getAnchor() != null) {
                this.pdfQrCodePosAnchor = pos.getAnchor();
            }

            final IppRoutingDto.Margin margin = pos.getMargin();
            if (margin == null) {
                return;
            }

            if (margin.getX() != null) {
                this.pdfQrCodePosMarginXPt =
                        QRCodeHelper.pdfMMToPoints(margin.getX().intValue());
            }
            if (margin.getY() != null) {
                this.pdfQrCodePosMarginYPt =
                        QRCodeHelper.pdfMMToPoints(margin.getY().intValue());
            }
        }
    }

    /**
     * Property key prefix.
     */
    private static final String PROP_KEY_PFX = "routing.";

    /** */
    private static final String PROP_KEY_ROUTING_ID = PROP_KEY_PFX + "id";

    /** */
    private static final String ROUTING_ID_PLACEHOLDER_UUID = "$uuid$";

    /**
     * Property key prefix.
     */
    private static final String PROP_KEY_PFX_PDF = PROP_KEY_PFX + "pdf.";

    /** */
    private static final String PROP_KEY_PFX_ROUTING_REST =
            PROP_KEY_PFX + "rest.";

    /** */
    private static final String PROP_KEY_ROUTING_REST_URI =
            PROP_KEY_PFX_ROUTING_REST + "uri";
    /** */
    private static final String PROP_KEY_ROUTING_REST_USER =
            PROP_KEY_PFX_ROUTING_REST + "user";
    /** */
    private static final String PROP_KEY_ROUTING_REST_PW =
            PROP_KEY_PFX_ROUTING_REST + "password";

    /** */
    private static final String PROP_KEY_PFX_PDF_QRCODE_REST_POST =
            PROP_KEY_PFX_ROUTING_REST + "post.";

    /** */
    private static final String PROP_KEY_ROUTING_REST_POST_REQ_ENTITY =
            PROP_KEY_PFX_PDF_QRCODE_REST_POST + "request.entity";

    /** */
    private static final String ROUTING_REST_POST_REQ_PLACEHOLDER_UUID =
            "$uuid$";
    /** */
    private static final String ROUTING_REST_POST_REQ_PLACEHOLDER_USERID =
            "$user_id$";
    /** */
    private static final String ROUTING_REST_POST_REQ_PLACEHOLDER_CLIENT_IP =
            "$client_ip$";
    /** */
    private static final String ROUTING_REST_POST_REQ_PLACEHOLDER_QUEUE_NAME =
            "$queue_name$";
    /** */
    private static final String ROUTING_REST_POST_REQ_PLACEHOLDER_PRINTER_NAME =
            "$printer_name$";
    /** */
    private static final String ROUTING_REST_POST_REQ_PLACEHOLDER_PRINTER_URI =
            "$printer_uri$";
    /** */
    private static final String //
    ROUTING_REST_POST_REQ_PLACEHOLDER_PRINTER_URI_HOST = "$printer_uri_host$";

    /** */
    private static final String ROUTING_REST_POST_REQ_PLACEHOLDER_JOB_NAME =
            "$job_name$";
    /** */
    private static final String ROUTING_REST_POST_REQ_PLACEHOLDER_JOB_TIME =
            "$job_time$";

    /** */
    private static final String ROUTING_REST_POST_REQ_PLACEHOLDER_PAGE_WIDTH =
            "$page_width_mm$";
    /** */
    private static final String ROUTING_REST_POST_REQ_PLACEHOLDER_PAGE_HEIGHT =
            "$page_height_mm$";

    /** */
    private static final String PROP_KEY_ROUTING_REST_POST_REQ_MEDIATYPE =
            PROP_KEY_PFX_PDF_QRCODE_REST_POST + "request.mediatype";

    /** */
    private static final String PROP_KEY_ROUTING_REST_POST_RSP_MEDIATYPE =
            PROP_KEY_PFX_PDF_QRCODE_REST_POST + "response.mediatype";

    /** */
    private static final String PDF_QRCODE_CONTENT_PLACEHOLDER_ROUTING_ID =
            "$routing.id$";

    /** */
    private static final String PROP_KEY_PDF_QRCODE_PFX =
            PROP_KEY_PFX_PDF + "qrcode";
    /** */
    private static final String PROP_KEY_PDF_QRCODE_CONTENT =
            PROP_KEY_PDF_QRCODE_PFX + ".content";
    /** */
    private static final String PROP_KEY_PDF_QRCODE_SIZE =
            PROP_KEY_PDF_QRCODE_PFX + ".size";
    /** */
    private static final String PROP_KEY_PDF_QRCODE_QUIET_ZONE =
            PROP_KEY_PDF_QRCODE_PFX + ".qz";
    /** */
    private static final String PROP_KEY_PDF_QRCODE_POS =
            PROP_KEY_PDF_QRCODE_PFX + ".pos";

    /** */
    private static final String PROP_KEY_PDF_QRCODE_POS_ANCHOR =
            PROP_KEY_PDF_QRCODE_POS + ".anchor";
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
    private static final String PROP_KEY_PDF_HEADER_TEXT =
            PROP_KEY_PDF_HEADER + ".text";

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
    private static final String PROP_KEY_PDF_FOOTER_TEXT =
            PROP_KEY_PDF_FOOTER + ".text";

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
    private RestClient routingRestClient;

    /** */
    private String routingRestReqEntity;

    /** */
    private String routingRestReqMediaType;
    /** */
    private String routingRestRspMediaType;

    private RoutingDataBase routingTemplate;

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

        final RoutingDataBase tpl = new RoutingDataBase();
        this.routingTemplate = tpl;

        //
        tpl.routingId = props.getProperty(PROP_KEY_ROUTING_ID);

        //
        this.onInitRoutingREST(context, props);

        //
        tpl.pdfQrCodeContent = props.getProperty(PROP_KEY_PDF_QRCODE_CONTENT);

        tpl.pdfQrCodeSizeMM =
                Integer.valueOf(props.getProperty(PROP_KEY_PDF_QRCODE_SIZE))
                        .intValue();

        tpl.pdfQrCodeQzMM = Integer.valueOf(
                props.getProperty(PROP_KEY_PDF_QRCODE_QUIET_ZONE, "0"));

        tpl.pdfQrCodePosAnchor = EnumUtils.getEnum(QrCodeAnchorEnum.class,
                props.getProperty(PROP_KEY_PDF_QRCODE_POS_ANCHOR));

        if (tpl.pdfQrCodePosAnchor == null) {
            tpl.pdfQrCodePosAnchor = QrCodeAnchorEnum.TL;
        }

        tpl.pdfQrCodePosMarginXPt =
                QRCodeHelper.pdfMMToPoints(Integer
                        .valueOf(props.getProperty(
                                PROP_KEY_PDF_QRCODE_POS_MARGIN_X, "0"))
                        .intValue());

        tpl.pdfQrCodePosMarginYPt =
                QRCodeHelper.pdfMMToPoints(Integer
                        .valueOf(props.getProperty(
                                PROP_KEY_PDF_QRCODE_POS_MARGIN_Y, "0"))
                        .intValue());

        onInitHeaderFooter(props, tpl);
        onInitPdfInfo(props, tpl);
    }

    /**
     * @param ctx
     *            Context.
     * @param props
     *            Configuration properties.
     * @throws ServerPluginException
     *             If error.
     */
    private void onInitRoutingREST(final ServerPluginContext ctx,
            final Properties props) throws ServerPluginException {

        final String restUri = props.getProperty(PROP_KEY_ROUTING_REST_URI);

        if (restUri == null) {
            return;
        }

        final URI uri;
        try {
            uri = new URI(restUri);
        } catch (URISyntaxException e) {
            throw new ServerPluginException(e.getMessage());
        }

        final String restUser = props.getProperty(PROP_KEY_ROUTING_REST_USER);

        if (restUser == null) {
            this.routingRestClient = ctx.createRestClient(uri);
        } else {
            this.routingRestClient = ctx.createRestClient(uri, restUser,
                    props.getProperty(PROP_KEY_ROUTING_REST_PW, ""));
        }

        this.routingRestReqEntity =
                props.getProperty(PROP_KEY_ROUTING_REST_POST_REQ_ENTITY);

        this.routingRestReqMediaType =
                props.getProperty(PROP_KEY_ROUTING_REST_POST_REQ_MEDIATYPE);

        this.routingRestRspMediaType =
                props.getProperty(PROP_KEY_ROUTING_REST_POST_RSP_MEDIATYPE);

        if (this.routingRestReqEntity == null
                || this.routingRestReqMediaType == null
                || this.routingRestRspMediaType == null) {
            throw new ServerPluginException(
                    String.format("One or more %s properties are missing.",
                            PROP_KEY_PFX_PDF_QRCODE_REST_POST));
        }
    }

    /**
     * Initializes the PDF info template.
     *
     * @param props
     *            Configuration properties.
     * @param tpl
     *            Template.
     */
    private static void onInitPdfInfo(final Properties props,
            final RoutingDataBase tpl) {

        tpl.pdfInfo = new HashMap<>();

        String value;

        value = props.getProperty(PROP_KEY_PDF_INFO_AUTHOR);
        if (StringUtils.isNotBlank(value)) {
            tpl.pdfInfo.put(ITextPdfCreator.PDF_INFO_KEY_AUTHOR, value);
        }

        value = props.getProperty(PROP_KEY_PDF_INFO_TITLE);
        if (StringUtils.isNotBlank(value)) {
            tpl.pdfInfo.put(ITextPdfCreator.PDF_INFO_KEY_TITLE, value);
        }

        value = props.getProperty(PROP_KEY_PDF_INFO_SUBJECT);
        if (StringUtils.isNotBlank(value)) {
            tpl.pdfInfo.put(ITextPdfCreator.PDF_INFO_KEY_SUBJECT, value);
        }

        value = props.getProperty(PROP_KEY_PDF_INFO_KEYWORDS);
        if (StringUtils.isNotBlank(value)) {
            tpl.pdfInfo.put(ITextPdfCreator.PDF_INFO_KEY_KEYWORDS, value);
        }
    }

    /**
     * Initializes the header and Footer template.
     *
     * @param props
     *            Configuration properties.
     * @param tpl
     *            Template.
     */
    private static void onInitHeaderFooter(final Properties props,
            final RoutingDataBase tpl) {

        tpl.pdfHeaderText = props.getProperty(PROP_KEY_PDF_HEADER_TEXT);

        if (tpl.pdfHeaderText != null) {

            final float headerFontSize = Float.valueOf(
                    props.getProperty(PROP_KEY_PDF_HEADER_FONT_SIZE, "8"))
                    .floatValue();

            tpl.pdfHeaderFont = new Font(Font.HELVETICA, headerFontSize);
            tpl.pdfHeaderFont.setColor(Color.GRAY);

            tpl.pdfHeaderMarginTopPt =
                    QRCodeHelper.pdfMMToPoints(Integer
                            .valueOf(props.getProperty(
                                    PROP_KEY_PDF_HEADER_MARGIN_TOP, "10"))
                            .intValue());
        }

        tpl.pdfFooterText = props.getProperty(PROP_KEY_PDF_FOOTER_TEXT);

        if (tpl.pdfFooterText != null) {

            final float footerFontSize = Float.valueOf(
                    props.getProperty(PROP_KEY_PDF_FOOTER_FONT_SIZE, "8"))
                    .floatValue();

            tpl.pdfFooterFont = new Font(Font.HELVETICA, footerFontSize);
            tpl.pdfFooterFont.setColor(Color.GRAY);

            tpl.pdfFooterMarginBottomPt = QRCodeHelper.pdfMMToPoints(Integer
                    .valueOf(props.getProperty(
                            PROP_KEY_PDF_FOOTER_MARGIN_BOTTOM, "10"))
                    .intValue());
        }
    }

    /**
     * @param stamper
     *            {@link PdfStamper}
     * @param data
     *            Routing data.
     */
    private static void onRoutingPdfInfo(final PdfStamper stamper,
            final RoutingData data) {

        if (data.pdfInfo == null || data.pdfInfo.isEmpty()) {
            return;
        }

        @SuppressWarnings("unchecked")
        final HashMap<String, String> info = stamper.getReader().getInfo();
        info.putAll(data.pdfInfo);
        stamper.setMoreInfo(info);
    }

    /**
     * @param stamper
     *            {@link PdfStamper}
     */
    private static void onRoutingPdfPermissions(final PdfStamper stamper) {

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
     * @param result
     *            The routing result to be filled.
     */
    public void onRouting(final IppRoutingContext ctx,
            final IppRoutingResult result) {

        final IPdfPageProps pageProps = ctx.getPageProperties();

        // Rotate pages in PDF dictionary? Mantis #1139.
        final Integer pdfDictPageRotate;
        if (pageProps.isLandscape() && !pageProps.isSeenAsLandscape()) {
            /*
             * Rotate to user "perceived" landscape.
             *
             * Example: Landscape print from LibreOffice gives Width [297]
             * Height [210] Rotation Page [270] Content [0].
             */
            pdfDictPageRotate = pageProps.getRotateToOrientationSeen(true);
        } else {
            pdfDictPageRotate = null;
        }

        /*
         * Rotate page size to position items on PDF "over content"?
         *
         * Mantis #1139. Example: PostScript PDL (Windows) driver print gives
         * Width [216] Height [279] Rotation Page [90] Content [0].
         */
        final boolean rotatePageSizeToPositionContent =
                !pageProps.isLandscape() && pageProps.isSeenAsLandscape();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "Job: {}\n\tWidth [{}] Height [{}]\n\t"
                            + "Landscape Page [{}] Seen [{}]\n\t"
                            + "Rotation Page [{}] Content [{}] -> [{}]",
                    ctx.getJobName(), pageProps.getMmWidth(),
                    pageProps.getMmHeight(),
                    Boolean.valueOf(pageProps.isLandscape()).toString(),
                    Boolean.valueOf(pageProps.isSeenAsLandscape()).toString(),
                    pageProps.getRotationFirstPage(),
                    pageProps.getContentRotationFirstPage(), pdfDictPageRotate);
        }

        final RoutingData data = new RoutingData(this.routingTemplate);

        if (this.routingRestClient != null) {
            this.onRoutingREST(rotatePageSizeToPositionContent, ctx, data);
        }

        try {
            onRoutingQRCodeImage(data);
        } catch (QRCodeException e) {
            LOGGER.error(e.getMessage());
            throw new SpException(e.getMessage(), e);
        }

        //
        final Phrase phraseHeader;

        if (data.pdfHeaderText == null) {
            phraseHeader = null;
        } else {
            phraseHeader = new Phrase(data.pdfHeaderText, data.pdfHeaderFont);
        }

        final Phrase phraseFooter;

        if (data.pdfFooterText == null) {
            phraseFooter = null;
        } else {
            phraseFooter = new Phrase(data.pdfFooterText, data.pdfFooterFont);
        }

        if (data.pdfQrCodeImage == null && phraseHeader == null
                && phraseFooter == null) {
            return;
        }

        //
        final File fileIn = ctx.getPdfToPrint();
        final File fileOut = new File(
                fileIn.getAbsolutePath().concat(UUID.randomUUID().toString()));

        PdfReader reader = null;
        PdfStamper stamper = null;
        boolean processed = false;

        try (InputStream pdfIn = new FileInputStream(fileIn);
                OutputStream pdfSigned = new FileOutputStream(fileOut);) {

            reader = new PdfReader(pdfIn);
            stamper = new PdfStamper(reader, pdfSigned);

            // First thing to do.
            onRoutingPdfPermissions(stamper);

            final int nPages = reader.getNumberOfPages();
            for (int nPage = 1; nPage <= nPages; nPage++) {

                final PdfContentByte content = stamper.getOverContent(nPage);

                final int pageRotation = reader.getPageRotation(nPage);
                final int rotation;
                if (pdfDictPageRotate == null) {
                    rotation = pageRotation;
                } else {
                    rotation = pdfDictPageRotate.intValue();
                    final PdfDictionary pageDict = reader.getPageN(nPage);
                    pageDict.put(PdfName.ROTATE, new PdfNumber(rotation));
                }

                final Rectangle pageSize = reader.getPageSize(nPage);
                final Rectangle rect;
                if (rotatePageSizeToPositionContent) {
                    rect = pageSize.rotate();
                } else {
                    rect = pageSize;
                }

                if (data.pdfQrCodeImage != null) {
                    positionQrCodeImagesOnPdfPage(rect, data.pdfQrCodeImage,
                            data.pdfQrCodeImageBg, data);
                    if (data.pdfQrCodeImageBg != null) {
                        content.addImage(data.pdfQrCodeImageBg);
                    }
                    content.addImage(data.pdfQrCodeImage);
                }

                if (phraseHeader != null || phraseFooter != null) {

                    final float xHeaderFooter =
                            (rect.getRight() - rect.getLeft()) / 2;
                    final float yHeader =
                            rect.getHeight() - data.pdfHeaderMarginTopPt;

                    if (phraseHeader != null) {
                        ColumnText.showTextAligned(content,
                                Element.ALIGN_CENTER, phraseHeader,
                                xHeaderFooter, yHeader, 0);
                    }

                    if (phraseFooter != null) {
                        final float yFooter = data.pdfFooterMarginBottomPt;
                        ColumnText.showTextAligned(content,
                                Element.ALIGN_CENTER, phraseFooter,
                                xHeaderFooter, yFooter, 0);
                    }
                }
            } // end-for

            onRoutingPdfInfo(stamper, data);

            stamper.close();
            reader.close();
            reader = null;

            ctx.replacePdfToPrint(fileOut);
            processed = true;

            // Fill result.
            result.setRoutingId(data.routingId);

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
     * Retrieves routing data from REST server.
     *
     * @param rotatePageSizeToPositionContent
     *            {@code true} if page size must be rotated to position items on
     *            PDF "over content"?
     * @param ctx
     *            The routing context.
     * @param data
     *            The routing data.
     */
    private void onRoutingREST(final boolean rotatePageSizeToPositionContent,
            final IppRoutingContext ctx, final RoutingData data) {

        final IPdfPageProps pageProps = ctx.getPageProperties();
        final int pageWidth;
        final int pageHeight;
        if (rotatePageSizeToPositionContent) {
            pageWidth = pageProps.getMmHeight();
            pageHeight = pageProps.getMmWidth();
        } else {
            pageWidth = pageProps.getMmWidth();
            pageHeight = pageProps.getMmHeight();
        }

        final String[][] placeholderValues = new String[][] {
                { ROUTING_REST_POST_REQ_PLACEHOLDER_CLIENT_IP,
                        ctx.getOriginatorIp() },
                { ROUTING_REST_POST_REQ_PLACEHOLDER_JOB_NAME,
                        ctx.getJobName() },
                { ROUTING_REST_POST_REQ_PLACEHOLDER_JOB_TIME,
                        this.routingRestClient
                                .toISODateTimeZ(ctx.getTransactionDate()) },
                { ROUTING_REST_POST_REQ_PLACEHOLDER_PAGE_WIDTH,
                        String.valueOf(pageWidth) },
                { ROUTING_REST_POST_REQ_PLACEHOLDER_PAGE_HEIGHT,
                        String.valueOf(pageHeight) },
                { ROUTING_REST_POST_REQ_PLACEHOLDER_QUEUE_NAME,
                        ctx.getQueueName() },
                { ROUTING_REST_POST_REQ_PLACEHOLDER_PRINTER_NAME,
                        ctx.getPrinterName() },
                { ROUTING_REST_POST_REQ_PLACEHOLDER_PRINTER_URI,
                        ctx.getPrinterURI().toString() },
                { ROUTING_REST_POST_REQ_PLACEHOLDER_PRINTER_URI_HOST,
                        CupsPrinterUriHelper.resolveHost(ctx.getPrinterURI()) },
                { ROUTING_REST_POST_REQ_PLACEHOLDER_UUID,
                        data.routingUUID.toString() },
                { ROUTING_REST_POST_REQ_PLACEHOLDER_USERID, ctx.getUserId() } //
        };

        String entity = this.routingRestReqEntity;

        for (final String[] placeholderValue : placeholderValues) {
            entity = StringUtils.replace(entity, placeholderValue[0],
                    placeholderValue[1]);
        }

        if (MediaType.APPLICATION_JSON.equals(this.routingRestRspMediaType)) {

            final IppRoutingDto rsp = this.routingRestClient.post(entity,
                    this.routingRestReqMediaType, this.routingRestRspMediaType,
                    IppRoutingDto.class);

            data.update(rsp);

            try {
                LOGGER.debug("RESTful POST: {} -> {}", entity,
                        JsonHelper.stringifyObjectPretty(rsp));
            } catch (IOException e) {
                //
            }

        } else {
            final String routingIdWrk = this.routingRestClient.post(entity,
                    this.routingRestReqMediaType, this.routingRestRspMediaType,
                    String.class);

            data.routingId = routingIdWrk;

            LOGGER.debug("RESTful POST: {} -> {}", entity, routingIdWrk);
        }
    }

    /**
     * Creates PDF QR-code and Quiet Zone background images.
     *
     * @param data
     *            Routing data.
     * @throws QRCodeException
     *             QR-code image error
     */
    private static void onRoutingQRCodeImage(final RoutingData data)
            throws QRCodeException {

        if (StringUtils.isBlank(data.pdfQrCodeContent)) {
            return;
        }

        final String codeQRContentWrk = StringUtils.replace(
                data.pdfQrCodeContent,
                PDF_QRCODE_CONTENT_PLACEHOLDER_ROUTING_ID, data.routingId);

        data.pdfQrCodeImage = QRCodeHelper.createPdfImage(codeQRContentWrk,
                data.pdfQrCodeSizeMM);

        //
        if (data.pdfQrCodeQzMM == 0) {
            return;
        }
        data.pdfQrCodeImageBg = QRCodeHelper.createPdfImageBackground(
                data.pdfQrCodeSizeMM + 2 * data.pdfQrCodeQzMM, Color.WHITE);
    }

    /**
     * Sets the absolute position of (background) image(s) on the PDF page.
     *
     * @param pageRect
     *            Page rectangle.
     * @param image
     *            The QR image.
     * @param imageBg
     *            Background image (Quiet Zone). Can be {@code null}.
     * @param data
     *            Routing data.
     */
    private static void positionQrCodeImagesOnPdfPage(final Rectangle pageRect,
            final Image image, final Image imageBg, final RoutingData data) {

        final float imgPointsHeight = image.getScaledHeight();
        final float imgPointsWidth = image.getScaledWidth();

        final float codeQRQuiteZonePoints;

        if (data.pdfQrCodeQzMM == 0) {
            codeQRQuiteZonePoints = 0f;
        } else {
            codeQRQuiteZonePoints =
                    QRCodeHelper.pdfMMToPoints(data.pdfQrCodeQzMM);
        }

        final float xImage;
        final float yImage;

        switch (data.pdfQrCodePosAnchor) {
        case TL:
            xImage = data.pdfQrCodePosMarginXPt;
            yImage = pageRect.getTop() - data.pdfQrCodePosMarginYPt
                    - imgPointsHeight;
            break;

        case TR:
            xImage = pageRect.getRight() - data.pdfQrCodePosMarginXPt
                    - imgPointsWidth;
            yImage = pageRect.getTop() - data.pdfQrCodePosMarginYPt
                    - imgPointsHeight;
            break;

        case BL:
            xImage = data.pdfQrCodePosMarginXPt;
            yImage = pageRect.getBottom() + data.pdfQrCodePosMarginYPt;
            break;

        case BR:
            // no code intended
        default:
            xImage = pageRect.getRight() - data.pdfQrCodePosMarginXPt
                    - imgPointsWidth;
            yImage = pageRect.getBottom() + data.pdfQrCodePosMarginYPt;
            break;
        }

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
