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
package org.savapage.ext.print;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.UUID;

import org.savapage.core.SpException;
import org.savapage.core.ipp.routing.IppRoutingContext;
import org.savapage.core.util.QRCodeException;
import org.savapage.core.util.QRCodeHelper;
import org.savapage.ext.ServerPlugin;
import org.savapage.ext.ServerPluginContext;
import org.savapage.ext.ServerPluginException;
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
     * Property key prefix.
     */
    private static final String PROP_KEY_PFX = "routing.";

    /**
     * Property key prefix.
     */
    private static final String PROP_KEY_PFX_PDF = PROP_KEY_PFX + "pdf.";

    /** */
    private static final String PROP_KEY_PDF_QRCODE =
            PROP_KEY_PFX_PDF + "qrcode";

    /** */
    private static final String PROP_KEY_PDF_QRCODE_SIZE =
            PROP_KEY_PDF_QRCODE + ".size";

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
    private String id;

    /** */
    private String name;

    /** */
    private String codeQR;

    /** */
    private int codeQRSize;

    /** */
    private String header;

    /** */
    private Font headerFont;

    /** */
    private int headerMarginTop;

    /** */
    private String footer;

    /** */
    private Font footerFont;

    /** */
    private int footerMarginBottom;

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

        this.codeQR = props.getProperty(PROP_KEY_PDF_QRCODE);
        this.codeQRSize = Integer
                .valueOf(props.getProperty(PROP_KEY_PDF_QRCODE_SIZE, "100"))
                .intValue();

        this.header = props.getProperty(PROP_KEY_PDF_HEADER);

        if (this.header != null) {

            final float headerFontSize = Float.valueOf(
                    props.getProperty(PROP_KEY_PDF_HEADER_FONT_SIZE, "8"))
                    .floatValue();

            this.headerFont = new Font(Font.HELVETICA, headerFontSize);
            this.headerFont.setColor(Color.GRAY);

            this.headerMarginTop = Integer.valueOf(
                    props.getProperty(PROP_KEY_PDF_HEADER_MARGIN_TOP, "20"))
                    .intValue();
        }

        this.footer = props.getProperty(PROP_KEY_PDF_FOOTER);

        if (this.footer != null) {

            final float footerFontSize = Float.valueOf(
                    props.getProperty(PROP_KEY_PDF_FOOTER_FONT_SIZE, "8"))
                    .floatValue();

            this.footerFont = new Font(Font.HELVETICA, footerFontSize);
            this.footerFont.setColor(Color.GRAY);

            this.footerMarginBottom = Integer
                    .valueOf(props.getProperty(
                            PROP_KEY_PDF_FOOTER_MARGIN_BOTTOM, "20"))
                    .intValue();
        }
    }

    /**
     *
     * @param ctx
     *            The routing context.
     */
    public void onRouting(final IppRoutingContext ctx) {

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

            //
            final int imgHeight = this.codeQRSize;

            final Image image;
            if (this.codeQR == null) {
                image = null;
            } else {
                final BufferedImage bufferImage =
                        QRCodeHelper.createImage(this.codeQR, imgHeight);
                image = Image.getInstance(bufferImage, null);
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
            final int nPages = reader.getNumberOfPages();
            for (int nPage = 1; nPage <= nPages; nPage++) {

                final PdfContentByte content = stamper.getOverContent(nPage);

                final Rectangle rect = reader.getPageSize(1);
                final int rotation = reader.getPageRotation(nPage);

                if (image != null) {

                    final float xImage = 0;
                    final float yImage;
                    if (rotation == 0) {
                        yImage = rect.getHeight() - imgHeight;
                    } else {
                        yImage = rect.getWidth() - imgHeight;
                    }

                    image.setAbsolutePosition(xImage, yImage);

                    content.addImage(image);
                }

                if (phraseHeader != null || phraseFooter != null) {

                    final float xHeaderFooter;
                    final float yHeader;

                    if (rotation == 0) {
                        xHeaderFooter = (rect.getRight() - rect.getLeft()) / 2;
                        yHeader = rect.getHeight() - this.headerMarginTop;
                    } else {
                        xHeaderFooter = (rect.getTop() - rect.getBottom()) / 2;
                        yHeader = rect.getWidth() - this.headerMarginTop;
                    }

                    if (phraseHeader != null) {
                        ColumnText.showTextAligned(content,
                                Element.ALIGN_CENTER, phraseHeader,
                                xHeaderFooter, yHeader, 0);
                    }

                    if (phraseFooter != null) {
                        final float yFooter = this.footerMarginBottom;
                        ColumnText.showTextAligned(content,
                                Element.ALIGN_CENTER, phraseFooter,
                                xHeaderFooter, yFooter, 0);
                    }
                }
            } // end-for

            stamper.close();
            reader.close();
            reader = null;

            ctx.replacePdfToPrint(fileOut);
            processed = true;

        } catch (IOException | DocumentException | QRCodeException e) {
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
