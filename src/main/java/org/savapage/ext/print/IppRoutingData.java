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

import java.util.List;

import org.savapage.core.dto.AbstractDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class IppRoutingData extends AbstractDto {

    /** */
    @JsonInclude(Include.NON_NULL)
    public static class Font {

        private Integer size;

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

    }

    /** */
    @JsonInclude(Include.NON_NULL)
    public static class Margin {

        private Integer x;
        private Integer y;

        public Integer getX() {
            return x;
        }

        public void setX(Integer x) {
            this.x = x;
        }

        public Integer getY() {
            return y;
        }

        public void setY(Integer y) {
            this.y = y;
        }

    }

    /** */
    @JsonInclude(Include.NON_NULL)
    public static class MarginHeader {

        private Integer top;

        public Integer getTop() {
            return top;
        }

        public void setTop(Integer top) {
            this.top = top;
        }

    }

    /** */
    @JsonInclude(Include.NON_NULL)
    public static class MarginFooter {

        private Integer bottom;

        public Integer getBottom() {
            return bottom;
        }

        public void setBottom(Integer bottom) {
            this.bottom = bottom;
        }

    }

    /** */
    @JsonInclude(Include.NON_NULL)
    public static class Text {

        private String text;
        private Font font;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Font getFont() {
            return font;
        }

        public void setFont(Font font) {
            this.font = font;
        }

    }

    /** */
    @JsonInclude(Include.NON_NULL)
    public static class Header extends Text {

        private MarginHeader margin;

        public MarginHeader getMargin() {
            return margin;
        }

        public void setMargin(MarginHeader margin) {
            this.margin = margin;
        }

    }

    /** */
    @JsonInclude(Include.NON_NULL)
    public static class Footer extends Text {

        private MarginFooter margin;

        public MarginFooter getMargin() {
            return margin;
        }

        public void setMargin(MarginFooter margin) {
            this.margin = margin;
        }

    }

    /** */
    @JsonInclude(Include.NON_NULL)
    public static class QrCodePosition {

        private QrCodeAnchorEnum anchor;
        private Margin margin;

        public QrCodeAnchorEnum getAnchor() {
            return anchor;
        }

        public void setAnchor(QrCodeAnchorEnum anchor) {
            this.anchor = anchor;
        }

        public Margin getMargin() {
            return margin;
        }

        public void setMargin(Margin margin) {
            this.margin = margin;
        }

    }

    /** */
    @JsonInclude(Include.NON_NULL)
    public static class QrCode {

        private String content;
        private Integer size;
        private Integer qz;
        private QrCodePosition pos;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Integer getQz() {
            return qz;
        }

        public void setQz(Integer qz) {
            this.qz = qz;
        }

        public QrCodePosition getPos() {
            return pos;
        }

        public void setPos(QrCodePosition pos) {
            this.pos = pos;
        }

    }

    /** */
    @JsonInclude(Include.NON_NULL)
    public static class PdfInfo {

        private String title;
        private String subject;
        private String author;
        private List<String> keywords;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

    }

    /** */
    @JsonInclude(Include.NON_NULL)
    public static class PdfData {

        private QrCode qrcode;
        private Header header;
        private Footer footer;
        private PdfInfo info;

        public QrCode getQrcode() {
            return qrcode;
        }

        public void setQrcode(QrCode qrcode) {
            this.qrcode = qrcode;
        }

        public Header getHeader() {
            return header;
        }

        public void setHeader(Header header) {
            this.header = header;
        }

        public Footer getFooter() {
            return footer;
        }

        public void setFooter(Footer footer) {
            this.footer = footer;
        }

        public PdfInfo getInfo() {
            return info;
        }

        public void setInfo(PdfInfo info) {
            this.info = info;
        }

    }

    private String id;
    private PdfData pdf;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PdfData getPdf() {
        return pdf;
    }

    public void setPdf(PdfData pdf) {
        this.pdf = pdf;
    }

}
