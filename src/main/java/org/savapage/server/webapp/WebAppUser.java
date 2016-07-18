/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.UploadProgressBar;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.lang.Bytes;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.crypto.OneTimeAuthToken;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.print.server.DocContentPrintReq;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.InetUtils;
import org.savapage.core.util.NumberUtil;
import org.savapage.server.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppUser extends AbstractWebAppPage {

    private static final long serialVersionUID = 1L;

    private final static String PAGE_PARM_AUTH_TOKEN = "auth_token";
    private final static String PAGE_PARM_AUTH_TOKEN_USERID = "auth_user";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(WebAppUser.class);

    /**
     *
     */
    private static final QueueService QUEUE_SERVICE =
            ServiceContext.getServiceFactory().getQueueService();

    /**
     * "Bean" attribute used for the selected font from the Form. Note: Wicket
     * needs the getter.
     */
    private InternalFontFamilyEnum selectedUploadFont;

    /**
     *
     */
    private FileUploadField fileUploadField;

    /**
     *
     */
    private Long maxUploadMb;

    public InternalFontFamilyEnum getSelectedUploadFont() {
        return selectedUploadFont;
    }

    public void setSelectedUploadFont(
            final InternalFontFamilyEnum selectedUploadFont) {
        this.selectedUploadFont = selectedUploadFont;
    }

    @Override
    protected boolean isJqueryCoreRenderedByWicket() {
        return true;
    }

    @Override
    protected WebAppTypeEnum getWebAppType() {
        return WebAppTypeEnum.USER;
    }

    @Override
    protected void renderWebAppTypeJsFiles(final IHeaderResponse response,
            final String nocache) {
        renderJs(response, String.format("%s%s",
                JS_FILE_JQUERY_SAVAPAGE_PAGE_PRINT_DELEGATION, nocache));
        renderJs(response,
                String.format("%s%s", getSpecializedJsFileName(), nocache));
    }

    /**
     *
     */
    private class MyFileUploadForm<Leeg> extends Form<Void> {

        private static final long serialVersionUID = 1L;

        /**
         *
         * @param id
         */
        public MyFileUploadForm(String id) {
            super(id);

        }

        /**
         * @see org.apache.wicket.markup.html.form.Form#onSubmit()
         */
        @Override
        protected void onSubmit() {

            final String originatorIp =
                    ((ServletWebRequest) RequestCycle.get().getRequest())
                            .getContainerRequest().getRemoteAddr();

            if (!InetUtils
                    .isIp4AddrInCidrRanges(
                            ConfigManager.instance().getConfigValue(
                                    Key.WEB_PRINT_LIMIT_IP_ADDRESSES),
                            originatorIp)) {

                error(localized("msg-file-upload-ip-not-allowed"));
                return;
            }

            final List<FileUpload> uploads = fileUploadField.getFileUploads();

            if (uploads == null || uploads.isEmpty()) {
                /*
                 * display uploaded info
                 */
                warn(getLocalizer().getString("msg-file-upload-no-file", this));
                return;
            }

            FileUpload uploadedFile = fileUploadField.getFileUploads().get(0);

            if (uploadedFile == null) {
                /*
                 * display uploaded info
                 */
                warn(getLocalizer().getString("msg-file-upload-no-file", this));
                return;
            }

            final String fileSize = NumberUtil
                    .humanReadableByteCount(uploadedFile.getSize(), true);

            info(String.format("%s (%s)", uploadedFile.getClientFileName(),
                    fileSize));

            try {

                AdminPublisher.instance().publish(PubTopicEnum.WEBPRINT,
                        PubLevelEnum.INFO,
                        localized("msg-admin-file-upload",
                                SpSession.get().getUser().getUserId(),
                                uploadedFile.getClientFileName(), fileSize));

                // Convert file to PDF.
                handleFileUpload(originatorIp, uploadedFile);

                info(getLocalizer().getString("msg-file-process-success",
                        this));

            } catch (Exception e) {
                error(localized("msg-file-process-error", e.getMessage()));
            }
        }

        /**
         *
         * @param originatorIp
         * @param uploadedFile
         * @throws DocContentPrintException
         * @throws IOException
         */
        private void handleFileUpload(final String originatorIp,
                final FileUpload uploadedFile)
                throws DocContentPrintException, IOException {

            final User user = SpSession.get().getUser();
            final String fileName = uploadedFile.getClientFileName();

            final InternalFontFamilyEnum preferredFont =
                    ((WebAppUser) this.getParent()).getSelectedUploadFont();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("User [%s] uploaded file [%s] [%s]",
                        user.getUserId(), uploadedFile.getContentType(),
                        uploadedFile.getClientFileName()));
            }

            ServiceContext.open();

            try {

                DocContentTypeEnum contentType = DocContent
                        .getContentTypeFromMime(uploadedFile.getContentType());

                if (contentType == null) {
                    contentType = DocContent.getContentTypeFromFile(
                            uploadedFile.getClientFileName());

                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(String.format(
                                "No content type found for [%s], "
                                        + "using [%s] based on file extension.",
                                uploadedFile.getContentType(), contentType));
                    }
                }

                final DocContentPrintReq docContentPrintReq =
                        new DocContentPrintReq();

                docContentPrintReq.setContentType(contentType);
                docContentPrintReq.setFileName(fileName);
                docContentPrintReq.setOriginatorEmail(null);
                docContentPrintReq.setOriginatorIp(originatorIp);
                docContentPrintReq.setPreferredOutputFont(preferredFont);
                docContentPrintReq.setProtocol(DocLogProtocolEnum.HTTP);
                docContentPrintReq.setTitle(fileName);

                QUEUE_SERVICE.printDocContent(ReservedIppQueueEnum.WEBPRINT,
                        user, true, docContentPrintReq,
                        uploadedFile.getInputStream());

            } finally {
                ServiceContext.close();
            }

        }
    }

    /**
     * Check if a {@link OneTimeAuthToken} is present and, when valid,
     * authenticates by putting the {@link User} in the {@link SpSession}.
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    private void checkOneTimeAuthToken(final PageParameters parameters) {

        final String token =
                this.getParmValue(parameters, false, PAGE_PARM_AUTH_TOKEN);

        final String userid = this.getParmValue(parameters, false,
                PAGE_PARM_AUTH_TOKEN_USERID);

        if (userid == null || token == null) {
            return;
        }

        final User sessionUser = SpSession.get().getUser();

        if (sessionUser != null && sessionUser.getUserId().equals(userid)) {
            return;
        }

        final long msecExpiry = ConfigManager.instance()
                .getConfigLong(Key.WEB_LOGIN_TTP_TOKEN_EXPIRY_MSECS);

        if (!OneTimeAuthToken.isTokenValid(userid, token, msecExpiry)) {
            final String msg = localized("msg-authtoken-denied", userid);
            AdminPublisher.instance().publish(PubTopicEnum.USER,
                    PubLevelEnum.WARN, msg);
            LOGGER.warn(msg);
            return;
        }

        final User authUser = ServiceContext.getDaoContext().getUserDao()
                .findActiveUserByUserId(userid);

        if (authUser == null) {

            final String msg =
                    localized("msg-authtoken-user-not-found", userid);
            AdminPublisher.instance().publish(PubTopicEnum.USER,
                    PubLevelEnum.WARN, msg);
            LOGGER.warn(msg);

            return;
        }

        SpSession.get().setUser(authUser, true);

        final String msg = localized("msg-authtoken-accepted", userid);
        AdminPublisher.instance().publish(PubTopicEnum.USER, PubLevelEnum.INFO,
                msg);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("User [%s] authenticated with token: %s",
                    userid, token));
        } else if (LOGGER.isInfoEnabled()) {
            LOGGER.info(msg);
        }
    }

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppUser(final PageParameters parameters) {

        super(parameters);

        if (isWebAppCountExceeded(parameters)) {
            this.setWebAppCountExceededResponse();
            return;
        }

        checkOneTimeAuthToken(parameters);

        final String appTitle = getWebAppTitle(null);

        add(new Label("app-title", appTitle));

        addZeroPagePanel(WebAppTypeEnum.USER);

        maxUploadMb = ConfigManager.instance()
                .getConfigLong(Key.WEB_PRINT_MAX_FILE_MB);

        if (maxUploadMb == null) {
            maxUploadMb = IConfigProp.WEBPRINT_MAX_FILE_MB_V_DEFAULT;
        }

        fileUploadMarkup();

        addFileDownloadApiPanel();

        /*
         * NOTE: Since Wicket 7 a panel is needed to make <wicket:enclosure>
         * work.
         */
        final UserDashboardPanel dashboardPanel =
                new UserDashboardPanel("page-dashboard-panel");

        add(dashboardPanel);
        dashboardPanel.populate();
    }

    @Override
    protected String getSpecializedCssFileName() {
        return "jquery.savapage-user.css";
    }

    @Override
    protected String getSpecializedJsFileName() {
        return "jquery.savapage-user.js";
    }

    @Override
    protected Set<JavaScriptLibrary> getJavaScriptToRender() {
        return EnumSet.allOf(JavaScriptLibrary.class);
    }

    /**
     * Creates the markup for the Ajax File upload.
     *
     * http://www.wicket-library.com/wicket-examples-6.0.x/upload/single?1
     */
    private void fileUploadMarkup() {

        /*
         * Supported types.
         */
        add(new Label("file-upload-types-docs",
                DocContent.getSupportedDocsInfo()));

        add(new Label("file-upload-types-graphics",
                DocContent.getSupportedGraphicsInfo()));

        add(new Label("file-upload-max-size", maxUploadMb.toString() + " MB"));

        /*
         * Create the form.
         */
        Form<?> form = new MyFileUploadForm<>("fileUploadForm");

        form.setMultiPart(false);

        form.setMaxSize(Bytes.megabytes(maxUploadMb));

        add(form);

        /*
         * Create the file upload field.
         */
        fileUploadField = new FileUploadField("fileUpload");
        fileUploadField.add(new AttributeModifier("accept",
                DocContent.getHtmlAcceptString()));

        form.add(fileUploadField);

        /*
         * The progress bar.
         */
        form.add(new UploadProgressBar("upload-progress", form));

        /*
         * The feedback panel.
         */
        final Component feedback = new FeedbackPanel("fileUploadFeedback")
                .setOutputMarkupPlaceholderTag(true);
        form.add(feedback);

        /*
         *
         */
        this.setSelectedUploadFont(ConfigManager
                .getConfigFontFamily(Key.REPORTS_PDF_INTERNAL_FONT_FAMILY));

        final DropDownChoice<InternalFontFamilyEnum> fileUploadFontDropDown =
                new DropDownChoice<>("fileUploadFontSelect",
                        new PropertyModel<InternalFontFamilyEnum>(this,
                                "selectedUploadFont"),
                        new LoadableDetachableModel<List<InternalFontFamilyEnum>>() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            protected List<InternalFontFamilyEnum> load() {
                                return new ArrayList<>(Arrays.asList(
                                        InternalFontFamilyEnum.values()));
                            }
                        }, new IChoiceRenderer<InternalFontFamilyEnum>() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public Object getDisplayValue(
                                    final InternalFontFamilyEnum object) {
                                return object.uiText(getLocale());
                            }

                            @Override
                            public String getIdValue(
                                    final InternalFontFamilyEnum object,
                                    final int index) {
                                return object.toString();
                            }

                            @Override
                            public InternalFontFamilyEnum getObject(String arg0,
                                    IModel<? extends List<? extends InternalFontFamilyEnum>> arg1) {
                                return InternalFontFamilyEnum.valueOf(arg0);
                            }
                        });

        form.add(fileUploadFontDropDown);

        /*
         * Create the ajax button used to submit the form.
         */
        AjaxButton ajaxButton = new AjaxButton("ajaxSubmitFileUpload") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form<?> form) {
                // ajax-update the feedback panel
                target.add(feedback);
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form<?> form) {
                LOGGER.error("error uploading file");
                // ajax-update the feedback panel
                target.add(feedback);
            }

        };

        ajaxButton.add(new AttributeModifier("value",
                getLocalizer().getString("button-file-upload", this)));

        form.add(ajaxButton);

        /*
         *
         */
        final Label labelWrk = new Label("file-upload-reset");
        labelWrk.add(new AttributeModifier("value",
                getLocalizer().getString("button-file-upload-reset", this)));
        form.add(labelWrk);

    }
}
