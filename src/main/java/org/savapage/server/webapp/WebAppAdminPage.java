/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.lang.Bytes;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class WebAppAdminPage extends AbstractWebAppPage {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(WebAppAdminPage.class);

    /**
     * .
     */
    private static final long MAX_UPLOAD_KB = 10L;

    /**
     * .
     */
    private FileUploadField memberCardFileUpload;

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppAdminPage(final PageParameters parameters) {

        super(parameters);

        if (isWebAppCountExceeded(parameters)) {
            setResponsePage(WebAppCountExceededMsg.class);
            return;
        }

        addZeroPagePanel(WebAppTypeEnum.ADMIN);

        add(new Label("app-name", CommunityDictEnum.SAVAPAGE.getWord() + " :: "
                + getLocalizer().getString("webapp-title-suffix", this)));
        memberCardUploadMarkup();

        addFileDownloadApiPanel();
    }

    @Override
    boolean isJqueryCoreRenderedByWicket() {
        return true;
    }

    @Override
    protected WebAppTypeEnum getWebAppType() {
        return WebAppTypeEnum.ADMIN;
    }

    @Override
    protected void renderWebAppTypeJsFiles(IHeaderResponse response,
            final String nocache) {

        renderJs(response, "jquery.savapage-admin-panels.js" + nocache);
        renderJs(response, "jquery.savapage-admin-pages.js" + nocache);
        renderJs(response, "jquery.savapage-admin-page-pos.js" + nocache);

        renderJs(response, getSpecializedJsFile() + nocache);
    }

    @Override
    protected String getSpecializedCssFile() {
        return "jquery.savapage-admin.css";
    }

    @Override
    protected String getSpecializedJsFile() {
        return "jquery.savapage-admin.js";
    }

    @Override
    protected Set<JavaScriptLibrary> getJavaScriptToRender() {
        return EnumSet.allOf(JavaScriptLibrary.class);
    }

    /**
     * Creates the markup for the Membership Card file Ajax upload.
     *
     * http://www.wicket-library.com/wicket-examples-6.0.x/upload/single?1
     */
    private void memberCardUploadMarkup() {

        /*
         * create a feedback panel
         */
        final Component feedback =
                new FeedbackPanel("memberCardUploadFeedback")
                        .setOutputMarkupPlaceholderTag(true);
        add(feedback);

        /*
         * create the form
         */
        Form<?> form = new Form<Void>("memberCardUploadForm") {

            private static final long serialVersionUID = 1L;

            /**
             * @see org.apache.wicket.markup.html.form.Form#onSubmit()
             */
            @Override
            protected void onSubmit() {

                final List<FileUpload> uploads =
                        memberCardFileUpload.getFileUploads();

                if (uploads == null || uploads.isEmpty()) {
                    /*
                     * display uploaded info
                     */
                    info(getLocalizer().getString(
                            "msg-membercard-import-no-file", this));
                    return;
                }

                FileUpload uploadedFile =
                        memberCardFileUpload.getFileUploads().get(0);

                if (uploadedFile == null) {
                    /*
                     * display uploaded info
                     */
                    info(getLocalizer().getString(
                            "msg-membercard-import-no-file", this));
                    return;
                }

                boolean isValid = true;

                File finalFile = MemberCard.getMemberCardFile();

                /*
                 * write to a temporary file
                 */
                File tempFile =
                        new File(System.getProperty("java.io.tmpdir") + "/"
                                + uploadedFile.getClientFileName());

                if (tempFile.exists()) {
                    tempFile.delete();
                }

                ServiceContext.open();
                final DaoContext daoContext = ServiceContext.getDaoContext();

                daoContext.beginTransaction();

                try {

                    tempFile.createNewFile();
                    uploadedFile.writeTo(tempFile);

                    // info("saved file: " +
                    // uploadedFile.getClientFileName());

                    isValid =
                            MemberCard.instance().isMemberCardFormatValid(
                                    tempFile);

                    if (isValid) {
                        /*
                         * Rename to standard Menber Card file name
                         */
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
                        info(getLocalizer().getString(
                                "msg-membercard-import-file-invalid", this));

                        LOGGER.info(uploadedFile.getClientFileName()
                                + " is not a valid "
                                + CommunityDictEnum.MEMBER_CARD.getWord());

                        // + " File-Size: "
                        // +
                        // Bytes.bytes(uploadedFile.getSize()).toString());
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

        /*
         * Create the ajax button used to submit the form.
         */
        AjaxButton ajaxButton = new AjaxButton("ajaxSubmit") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form<?> form) {

                info(getLocalizer().getString("msg-membercard-import-busy",
                        this));

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Uploading "
                            + CommunityDictEnum.MEMBER_CARD.getWord() + " file");
                }

                /*
                 * ajax-update the feedback panel
                 */
                target.add(feedback);
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form<?> form) {
                LOGGER.error("error importing "
                        + CommunityDictEnum.MEMBER_CARD.getWord());
            }

        };

        ajaxButton.add(new AttributeModifier("value", getLocalizer().getString(
                "button-membercard-import", this)));

        form.add(ajaxButton);

        /*
         *
         */
        Label labelWrk = new Label("membercard-import-reset");
        labelWrk.add(new AttributeModifier("value", getLocalizer().getString(
                "button-membercard-import-clear", this)));
        form.add(labelWrk);

    }

}
