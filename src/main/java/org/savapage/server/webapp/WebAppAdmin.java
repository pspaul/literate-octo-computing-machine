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
package org.savapage.server.webapp;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.lang.Bytes;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppAdmin extends AbstractWebAppPage {

    /** */
    private static final long serialVersionUID = 1L;

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(WebAppAdmin.class);

    /** */
    private static final long MAX_UPLOAD_KB = 10L;

    /** */
    private static final String[] CSS_REQ_FILENAMES =
            new String[] { "jquery.savapage-common-icons.css" };

    /**
     * .
     */
    private FileUploadField memberCardFileUpload;

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppAdmin(final PageParameters parameters) {

        super(parameters);

        checkInternetAccess(IConfigProp.Key.WEBAPP_INTERNET_ADMIN_ENABLE);

        if (isWebAppCountExceeded(parameters)) {
            setWebAppCountExceededResponse();
            return;
        }

        final String appTitle = getWebAppTitle(
                getLocalizer().getString("webapp-title-suffix", this));

        addZeroPagePanel(WebAppTypeEnum.ADMIN);

        add(new Label("app-title", appTitle));

        memberCardUploadMarkup();

        addFileDownloadApiPanel();
    }

    @Override
    protected boolean isJqueryCoreRenderedByWicket() {
        return true;
    }

    @Override
    protected WebAppTypeEnum getWebAppType() {
        return WebAppTypeEnum.ADMIN;
    }

    @Override
    protected void renderWebAppTypeJsFiles(final IHeaderResponse response,
            final String nocache) {

        renderJs(response, String.format("%s%s",
                "jquery.savapage-admin-panels.js", nocache));
        renderJs(response, String.format("%s%s",
                "jquery.savapage-admin-pages.js", nocache));
        renderJs(response,
                String.format("%s%s", getSpecializedJsFileName(), nocache));
    }

    @Override
    protected String[] getSpecializedCssReqFileNames() {
        return CSS_REQ_FILENAMES;
    }

    @Override
    protected String getSpecializedCssFileName() {
        return "jquery.savapage-admin.css";
    }

    @Override
    protected String getSpecializedJsFileName() {
        return "jquery.savapage-admin.js";
    }

    @Override
    protected Set<JavaScriptLibrary> getJavaScriptToRender() {
        final EnumSet<JavaScriptLibrary> libs =
                EnumSet.allOf(JavaScriptLibrary.class);
        return libs;
    }

    /**
     * Creates the markup for the Membership Card file Ajax upload.
     *
     * http://www.wicket-library.com/wicket-examples-6.0.x/upload/single?1
     */
    private void memberCardUploadMarkup() {

        final Component feedback = new FeedbackPanel("memberCardUploadFeedback")
                .setOutputMarkupPlaceholderTag(true);
        add(feedback);

        final StatelessForm<?> form =
                new StatelessForm<Void>("memberCardUploadForm") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit() {

                        final List<FileUpload> uploads =
                                memberCardFileUpload.getFileUploads();

                        if (uploads == null || uploads.isEmpty()) {
                            // display uploaded info
                            warn(getLocalizer().getString(
                                    "msg-membercard-import-no-file", this));
                            return;
                        }

                        final FileUpload uploadedFile =
                                memberCardFileUpload.getFileUploads().get(0);

                        if (uploadedFile == null) {
                            // display uploaded info
                            warn(getLocalizer().getString(
                                    "msg-membercard-import-no-file", this));
                            return;
                        }

                        final File finalFile = MemberCard.getMemberCardFile();

                        /*
                         * Write to a temporary file in SAME directory as final
                         * member card. Do NOT use AppTmpDir(), since its
                         * location may be configured to be on another
                         * partition, making the ATOMIC_MOVE fail with
                         * java.nio.file.AtomicMoveNotSupportedException.
                         */
                        final File tempFile = new File(String.format("%s.%s",
                                finalFile.getAbsolutePath(),
                                UUID.randomUUID().toString()));

                        if (tempFile.exists()) {
                            tempFile.delete();
                        }

                        ServiceContext.open();
                        final DaoContext daoContext =
                                ServiceContext.getDaoContext();

                        daoContext.beginTransaction();

                        try {

                            tempFile.createNewFile();
                            uploadedFile.writeTo(tempFile);

                            final boolean isValid = MemberCard.instance()
                                    .isMemberCardFormatValid(tempFile);

                            if (isValid) {
                                // Rename to standard Member Card file name
                                java.nio.file.Files.move(
                                        FileSystems.getDefault().getPath(
                                                tempFile.getAbsolutePath()),
                                        FileSystems.getDefault().getPath(
                                                finalFile.getAbsolutePath()),
                                        StandardCopyOption.ATOMIC_MOVE,
                                        StandardCopyOption.REPLACE_EXISTING);

                                MemberCard.instance().init();

                                info(getLocalizer().getString(
                                        "msg-membercard-import-success", this));

                            } else {
                                error(getLocalizer().getString(
                                        "msg-membercard-import-file-invalid",
                                        this));

                                if (LOGGER.isInfoEnabled()) {
                                    LOGGER.info(String.format(
                                            "%s is not a valid %s.",
                                            uploadedFile.getClientFileName(),
                                            CommunityDictEnum.MEMBER_CARD
                                                    .getWord()));
                                }
                            }

                            daoContext.commit();

                        } catch (Exception e) {

                            daoContext.rollback();

                            if (finalFile.exists()) {
                                finalFile.delete();
                            }
                            throw new IllegalStateException(e);

                        } finally {
                            ServiceContext.close();

                            if (tempFile.exists()) {
                                tempFile.delete();
                            }

                            uploadedFile.closeStreams();
                            uploadedFile.delete();
                        }
                    }
                };

        form.setMultiPart(false);
        form.setMaxSize(Bytes.kilobytes(MAX_UPLOAD_KB));
        add(form);

        /*
         * create the file upload field
         */
        memberCardFileUpload = new FileUploadField("memberCardUpload");

        form.add(memberCardFileUpload);

        final AjaxButton ajaxButton = new AjaxButton("ajaxSubmit") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Uploading %s file.",
                            CommunityDictEnum.MEMBER_CARD.getWord()));
                }

                // ajax-update the feedback panel
                target.add(feedback);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {

                LOGGER.error(String.format("Error importing %s",
                        CommunityDictEnum.MEMBER_CARD.getWord()));

                // ajax-update the feedback panel
                target.add(feedback);
            }
        };

        form.add(ajaxButton);
    }

}
