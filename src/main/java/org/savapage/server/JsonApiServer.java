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
package org.savapage.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.print.attribute.standard.MediaSizeName;
import javax.servlet.http.HttpSession;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.resource.StringBufferResourceStream;
import org.apache.wicket.util.time.Duration;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.savapage.core.LetterheadNotFoundException;
import org.savapage.core.OutputProducer;
import org.savapage.core.PerformanceLogger;
import org.savapage.core.PostScriptDrmException;
import org.savapage.core.SpException;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.CometdClientMixin;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.ConfigPropertyDao;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.PrinterGroupDao;
import org.savapage.core.dao.UserAccountDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.helpers.AccountTrxTypeEnum;
import org.savapage.core.dao.helpers.DeviceAttrEnum;
import org.savapage.core.dao.helpers.DeviceTypeEnum;
import org.savapage.core.dao.helpers.DocLogProtocolEnum;
import org.savapage.core.dao.helpers.PrintModeEnum;
import org.savapage.core.dao.helpers.ProxyPrintAuthModeEnum;
import org.savapage.core.dao.helpers.ReservedIppQueueEnum;
import org.savapage.core.dao.helpers.UserAttrEnum;
import org.savapage.core.dao.helpers.UserPagerReq;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.doc.DocContent;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.AccountDisplayInfoDto;
import org.savapage.core.dto.AccountVoucherBatchDto;
import org.savapage.core.dto.AccountVoucherRedeemDto;
import org.savapage.core.dto.JrPageSizeDto;
import org.savapage.core.dto.PosDepositDto;
import org.savapage.core.dto.PosDepositReceiptDto;
import org.savapage.core.dto.PrimaryKeyDto;
import org.savapage.core.dto.ProxyPrinterCostDto;
import org.savapage.core.dto.ProxyPrinterDto;
import org.savapage.core.dto.ProxyPrinterMediaSourcesDto;
import org.savapage.core.dto.QuickSearchFilterDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.dto.QuickSearchPosPurchaseItemDto;
import org.savapage.core.dto.QuickSearchPrinterItemDto;
import org.savapage.core.dto.QuickSearchUserItemDto;
import org.savapage.core.dto.UserDto;
import org.savapage.core.dto.VoucherBatchPrintDto;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.img.ImageUrl;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.PageImages;
import org.savapage.core.inbox.PageImages.PageImage;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.jmx.JmxRemoteProperties;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.job.SpJobType;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.ConfigProperty;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.DeviceAttr;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.Entity;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.PosPurchase;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.User;
import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.json.JsonPrinter;
import org.savapage.core.json.JsonPrinterDetail;
import org.savapage.core.json.JsonPrinterList;
import org.savapage.core.json.PdfProperties;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.JsonRpcConfig;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.ResultDataBasic;
import org.savapage.core.json.rpc.impl.ResultPosDeposit;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.outbox.OutboxInfoDto;
import org.savapage.core.papercut.PaperCutDbProxy;
import org.savapage.core.papercut.PaperCutServerProxy;
import org.savapage.core.print.gcp.GcpClient;
import org.savapage.core.print.gcp.GcpPrinter;
import org.savapage.core.print.gcp.GcpRegisterPrinterRsp;
import org.savapage.core.print.imap.ImapListener;
import org.savapage.core.print.proxy.ProxyPrintAuthManager;
import org.savapage.core.print.proxy.ProxyPrintException;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.print.smartschool.SmartSchoolPrintMonitor;
import org.savapage.core.reports.AbstractJrDesign;
import org.savapage.core.reports.JrPosDepositReceipt;
import org.savapage.core.reports.JrUserDataSource;
import org.savapage.core.reports.JrVoucherPageDesign;
import org.savapage.core.rfid.RfidNumberFormat;
import org.savapage.core.services.AccountVoucherService;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.DeviceService;
import org.savapage.core.services.DeviceService.DeviceAttrLookup;
import org.savapage.core.services.DocLogService;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.services.helpers.IppLogger;
import org.savapage.core.services.helpers.PageScalingEnum;
import org.savapage.core.services.helpers.ProxyPrintCostParms;
import org.savapage.core.services.helpers.UserAuth;
import org.savapage.core.services.impl.InboxServiceImpl;
import org.savapage.core.users.IExternalUserAuthenticator;
import org.savapage.core.users.IUserSource;
import org.savapage.core.users.InternalUserAuthenticator;
import org.savapage.core.users.UserAliasList;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.LocaleHelper;
import org.savapage.core.util.MediaUtils;
import org.savapage.core.util.Messages;
import org.savapage.core.util.QuickSearchDate;
import org.savapage.server.auth.ClientAppUserAuthManager;
import org.savapage.server.auth.UserAuthToken;
import org.savapage.server.auth.WebAppUserAuthManager;
import org.savapage.server.cometd.AbstractEventService;
import org.savapage.server.cometd.UserEventService;
import org.savapage.server.pages.AbstractPage;
import org.savapage.server.pages.StatsPageTotalPanel;
import org.savapage.server.webapp.WebAppTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the private JSON interface of the Server.
 *
 * @author Datraverse B.V.
 *
 */
public final class JsonApiServer extends AbstractPage {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(JsonApiServer.class);

    /**
     *
     */
    private static final AccountingService ACCOUNTING_SERVICE = ServiceContext
            .getServiceFactory().getAccountingService();

    /**
     *
     */
    private static final AccountVoucherService ACCOUNT_VOUCHER_SERVICE =
            ServiceContext.getServiceFactory().getAccountVoucherService();

    /**
    *
    */
    private static final DeviceService DEVICE_SERVICE = ServiceContext
            .getServiceFactory().getDeviceService();

    /**
     *
     */
    private static final DocLogService DOC_LOG_SERVICE = ServiceContext
            .getServiceFactory().getDocLogService();

    /**
     *
     */
    private static final InboxService INBOX_SERVICE = ServiceContext
            .getServiceFactory().getInboxService();

    /**
     *
     */
    private static final OutboxService OUTBOX_SERVICE = ServiceContext
            .getServiceFactory().getOutboxService();

    /**
    *
    */
    private static final PrinterService PRINTER_SERVICE = ServiceContext
            .getServiceFactory().getPrinterService();

    /**
     *
     */
    private static final ProxyPrintService PROXY_PRINT_SERVICE = ServiceContext
            .getServiceFactory().getProxyPrintService();

    /**
     *
     */
    private static final QueueService QUEUE_SERVICE = ServiceContext
            .getServiceFactory().getQueueService();

    /**
     *
     */
    private static final UserService USER_SERVICE = ServiceContext
            .getServiceFactory().getUserService();

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private static final JsonApiDict API_DICTIONARY = new JsonApiDict();

    /**
     *
     */
    private static final String PARM_VALUE_FALSE = "0";
    /**
     *
     */
    private static final String PARM_VALUE_TRUE = "1";

    /**
     *
     */
    private static final String API_RESULT_CODE_OK = "0";

    /**
     *
     */
    private static final String API_RESULT_CODE_INFO = "1";
    /**
     *
     */
    private static final String API_RESULT_CODE_WARN = "2";
    /**
     *
     */
    private static final String API_RESULT_CODE_ERROR = "3";
    /**
     *
     */
    private static final String API_RESULT_CODE_UNAUTH = "9";

    /**
     *
     */
    private static final String USER_ROLE_ADMIN = "admin";

    // private static final String USER_ROLE_USER = "user";

    /**
     * Our own handler to control the release of transient files used for
     * streaming (like JPEG images to display in a webpage and PDF files to
     * download.
     */
    private static abstract class AbstractFileRequestHandler extends
            ResourceStreamRequestHandler {

        private final String user;
        private final File file;

        /**
         *
         * @param user
         *            The userId.
         * @param file
         *            The file to stream.
         */
        public AbstractFileRequestHandler(final String user, File file) {
            super(new FileResourceStream(file));
            this.file = file;
            this.user = user;
        }

        /**
         * The actual release of the file to be implemented by any concrete
         * subclass.
         *
         * @param user
         *            The userId.
         * @param file
         *            The file to stream.
         */
        protected abstract void releaseFile(final String user, File file);

        @Override
        public void detach(IRequestCycle requestCycle) {
            releaseFile(user, file);
            super.detach(requestCycle);
        }
    }

    /**
     * Wrapper for downloaded PDF file.
     */
    private static class PdfFileRequestHandler extends
            AbstractFileRequestHandler {

        /**
         *
         * @param user
         *            The userId.
         * @param file
         *            The file to stream.
         */
        public PdfFileRequestHandler(String user, File file) {
            super(user, file);
        }

        @Override
        protected void releaseFile(String user, File file) {
            OutputProducer.instance().releasePdf(file);
        }
    }

    /**
     * Wrapper for file download.
     */
    private static class DownloadRequestHandler extends
            AbstractFileRequestHandler {

        /**
         *
         * @param user
         *            The userId.
         * @param file
         *            The file to stream.
         */
        public DownloadRequestHandler(File file) {
            super(null, file);
        }

        @Override
        protected void releaseFile(String user, File file) {
            file.delete();
        }
    }

    /**
     *
     * @param parameters
     */
    public JsonApiServer(final PageParameters parameters) {

        final Date perfStartTime = PerformanceLogger.startTime();

        //
        final RequestCycle requestCycle = getRequestCycle();

        final IRequestParameters reqParms =
                requestCycle.getRequest().getPostParameters();

        /*
         * Get the request id + requesting user
         */
        String requestId =
                reqParms.getParameterValue(JsonApiDict.PARM_REQ).toString();

        final boolean isGetAction = requestId == null;

        if (isGetAction) {
            requestId = parameters.get(JsonApiDict.PARM_REQ).toString();
        }

        final String requestingUser =
                getParmValue(parameters, isGetAction, JsonApiDict.PARM_USER);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Request: " + requestId);
        }

        /*
         * Get the user from the session.
         */
        final boolean isInternalAdmin =
                (requestingUser != null)
                        && ConfigManager.isInternalAdmin(requestingUser);

        /*
         *
         */
        String jsonArray = null;
        User lockedUser = null;
        boolean commitDbTransaction = false;

        /*
         * Lock indicators
         */
        JsonApiDict.DbClaim dbClaim = JsonApiDict.DbClaim.NONE;
        JsonApiDict.LetterheadLock letterheadLock =
                JsonApiDict.LetterheadLock.NONE;

        /*
         *
         */
        ServiceContext.open();

        ServiceContext.setLocale(getSession().getLocale());

        final DaoContext daoContext = ServiceContext.getDaoContext();

        try {
            /*
             * Is the request valid and the requesting user authorized for the
             * request?
             */
            Map<String, Object> returnData =
                    checkValidAndAuthorized(requestId, requestingUser);

            final boolean isValidAndAuthorizedRequest = returnData == null;

            if (isValidAndAuthorizedRequest) {

                /*
                 * Application level locks ...
                 */
                letterheadLock =
                        API_DICTIONARY.getLetterheadLockNeeded(requestId,
                                SpSession.get().isAdmin());
                dbClaim = API_DICTIONARY.getDbClaimNeeded(requestId);

                API_DICTIONARY.lock(letterheadLock);
                API_DICTIONARY.lock(dbClaim);

                /*
                 * Database transaction.
                 */
                ServiceContext.setActor(requestingUser);
                ServiceContext.resetTransactionDate();

                if (API_DICTIONARY.isDbAccessNeeded(requestId)) {
                    daoContext.beginTransaction();
                }

                /*
                 * Do we need a database (row) lock on the requesting user?
                 */
                if (!isInternalAdmin
                        && API_DICTIONARY.isUserLockNeeded(requestId)) {

                    final UserDao userDao =
                            ServiceContext.getDaoContext().getUserDao();

                    lockedUser = null;

                    User userTmp =
                            userDao.findActiveUserByUserId(requestingUser);

                    if (userTmp != null) {
                        lockedUser = userDao.lock(userTmp.getId());
                    }

                    if (lockedUser == null) {
                        throw new SpException("user [" + requestingUser
                                + "] cannot be found");
                    }
                }

                returnData = null;

                switch (requestId) {

                case JsonApiDict.REQ_ACCOUNT_VOUCHER_BATCH_PRINT:
                    // no break intended
                case JsonApiDict.REQ_POS_RECEIPT_DOWNLOAD:
                    // no break intended
                case JsonApiDict.REQ_POS_RECEIPT_DOWNLOAD_USER:
                    // no break intended
                case JsonApiDict.REQ_REPORT:
                    handleExportPdf(requestId, parameters, requestingUser,
                            isGetAction);
                    break;

                case JsonApiDict.REQ_PRINTER_OPT_DOWNLOAD:
                    requestCycle.scheduleRequestHandlerAfterCurrent(this
                            .exportPrinterOpt(parameters.get(
                                    JsonApiDict.PARM_REQ_SUB).toLongObject()));
                    break;

                case JsonApiDict.REQ_PDF:
                    requestCycle.scheduleRequestHandlerAfterCurrent(this
                            .handleExportSafePages(lockedUser, parameters,
                                    isGetAction));
                    commitDbTransaction = true;
                    break;

                default:
                    returnData =
                            handleRequest(requestId, parameters, isGetAction,
                                    requestingUser, lockedUser);

                    commitDbTransaction =
                            (returnData != null && !isApiResultError(returnData));
                    break;
                }

            } else {
                /*
                 * Download requests are handled with a custom feedback HTML
                 * when NOT authorized (anymore).
                 */
                if (requestId != null
                        && JsonApiDict.isDownloadRequest(requestId)) {

                    String urlPath;

                    switch (getWebAppType()) {
                    case ADMIN:
                        urlPath = WebApp.MOUNT_PATH_WEBAPP_ADMIN;
                        break;
                    case POS:
                        urlPath = WebApp.MOUNT_PATH_WEBAPP_POS;
                        break;
                    default:
                        urlPath = WebApp.MOUNT_PATH_WEBAPP_USER;
                        break;
                    }

                    requestCycle
                            .scheduleRequestHandlerAfterCurrent(new TextRequestHandler(
                                    "text/html",
                                    "UTF-8",
                                    "<h2 style='color: red;'>"
                                            + CommunityDictEnum.SAVAPAGE
                                                    .getWord()
                                            + " Authorisation Error</h2>"
                                            + "<p>Please login again.</p>"
                                            + "<p><form action='"
                                            + urlPath
                                            + "'>"
                                            + "<input type='submit'value='Login'/>"
                                            + "</form></p>"));
                    /*
                     * Reset to null.
                     */
                    returnData = null;
                }
            }

            if (returnData == null) {
                jsonArray = null;
            } else if (jsonArray == null) {
                jsonArray = new ObjectMapper().writeValueAsString(returnData);
            }

        } catch (Exception t) {

            try {
                jsonArray =
                        new ObjectMapper()
                                .writeValueAsString(handleException(t));
            } catch (Exception e1) {
                LOGGER.error(e1.getMessage());
            }

        } finally {

            try {
                /*
                 * Commit/roll-back the changes: this will also unlock the user.
                 */
                if (commitDbTransaction) {
                    ServiceContext.getDaoContext().commit();
                } else {
                    /*
                     * If a previous commit() failed the trx is NOT active.
                     */
                    ServiceContext.getDaoContext().rollback();
                }

                ServiceContext.getDaoContext().close();

            } catch (Exception ex) {
                try {
                    jsonArray =
                            new ObjectMapper()
                                    .writeValueAsString(handleException(ex));
                } catch (Exception e1) {
                    LOGGER.error(e1.getMessage());
                }

            } finally {

                try {
                    ServiceContext.close();
                } finally {
                    /*
                     * Unlock application locks
                     */
                    API_DICTIONARY.unlock(letterheadLock);
                    API_DICTIONARY.unlock(dbClaim);
                }
            }
        }

        /*
         *
         */
        if (jsonArray != null) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(jsonArray);
            }

            requestCycle
                    .scheduleRequestHandlerAfterCurrent(new TextRequestHandler(
                            JsonRpcConfig.INTERNET_MEDIA_TYPE, "UTF-8",
                            jsonArray));
        }

        PerformanceLogger.log(this.getClass(), "constructor", perfStartTime,
                requestId);
    }

    /**
     *
     * @param request
     * @param parameters
     * @param requestingUser
     * @param isGetAction
     */
    private void handleExportPdf(final String request,
            final PageParameters parameters, final String requestingUser,
            final boolean isGetAction) {

        final String baseName =
                "report-" + UUID.randomUUID().toString() + ".pdf";

        final String fileName = ConfigManager.getAppTmpDir() + "/" + baseName;

        final File tempFile = new File(fileName);

        IRequestHandler requestHandler = null;

        try {

            switch (request) {

            case JsonApiDict.REQ_ACCOUNT_VOUCHER_BATCH_PRINT:
                requestHandler =
                        exportVoucherBatchPrint(tempFile,
                                parameters.get(JsonApiDict.PARM_REQ_SUB)
                                        .toString());
                break;

            case JsonApiDict.REQ_POS_RECEIPT_DOWNLOAD:
                requestHandler =
                        exportPosPurchaseReceipt(tempFile,
                                parameters.get(JsonApiDict.PARM_REQ_SUB)
                                        .toLongObject(), requestingUser, false);
                break;

            case JsonApiDict.REQ_POS_RECEIPT_DOWNLOAD_USER:
                requestHandler =
                        exportPosPurchaseReceipt(tempFile,
                                parameters.get(JsonApiDict.PARM_REQ_SUB)
                                        .toLongObject(), requestingUser, true);
                break;

            case JsonApiDict.REQ_REPORT:
                requestHandler =
                        exportReport(tempFile,
                                parameters.get(JsonApiDict.PARM_REQ_SUB)
                                        .toString(),
                                parameters.get(JsonApiDict.PARM_DATA)
                                        .toString(), requestingUser,
                                isGetAction);
                break;
            default:
                throw new SpException("request [" + request + "] not supported");
            }

        } catch (Exception e) {

            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }

            requestHandler =
                    new TextRequestHandler("text/html", "UTF-8",
                            "<h2 style='color: red;'>"
                                    + e.getClass().getSimpleName() + "</h2><p>"
                                    + e.getMessage() + "</p>");
        }

        if (requestHandler != null) {
            getRequestCycle()
                    .scheduleRequestHandlerAfterCurrent(requestHandler);
        }
    }

    /**
     * Creates a PDF file with the Receipt of a POS Deposit.
     * <p>
     * Invariant: If request is from a user (not an administrator), then the
     * requested receipt MUST be for the requesting user.
     * </p>
     *
     * @param pdfFile
     *            The PDF file to create.
     * @param accountTrxDbId
     *            The database primary key of the {@link AccountTrx}.
     * @param requestingUser
     *            The requesting userid.
     * @param isUserRequest
     *            {@code true} if this is a request from a user.
     * @return The {@link PosDepositReceiptDto} with all the deposit
     *         information.
     * @throws JRException
     *             When JasperReport error
     */
    private PosDepositReceiptDto createPosPurchaseReceipt(final File pdfFile,
            final Long accountTrxDbId, final String requestingUser,
            boolean isUserRequest) throws JRException {

        final PosDepositReceiptDto receipt =
                ACCOUNTING_SERVICE.createPosDepositReceiptDto(accountTrxDbId);

        /*
         * INVARIANT:
         */
        if (isUserRequest && !receipt.getUserId().equals(requestingUser)) {
            throw new SpException("User [" + requestingUser
                    + "] is not autherized to access receipt of user ["
                    + receipt.getUserId() + "].");
        }

        //
        final MediaSizeName mediaSizeName = MediaUtils.getDefaultMediaSize();

        JrPageSizeDto pageSizeDto;

        if (mediaSizeName == MediaSizeName.NA_LETTER) {
            pageSizeDto = JrPageSizeDto.LETTER_PORTRAIT;
        } else {
            pageSizeDto = JrPageSizeDto.A4_PORTRAIT;
        }

        final JasperReport jasperReport =
                JasperCompileManager
                        .compileReport(JrPosDepositReceipt.create(
                                pageSizeDto,
                                ConfigManager
                                        .getConfigFontFamily(Key.REPORTS_PDF_INTERNAL_FONT_FAMILY),
                                ServiceContext.getLocale()));

        //
        final JasperPrint jasperPrint =
                JasperFillManager.fillReport(jasperReport, JrPosDepositReceipt
                        .getParameters(receipt, ServiceContext.getLocale()),
                        new JREmptyDataSource());

        JasperExportManager.exportReportToPdfFile(jasperPrint,
                pdfFile.getAbsolutePath());

        return receipt;
    }

    /**
     *
     * @param accountTrxDbId
     * @param toAddress
     * @param requestingUser
     * @param isUserRequest
     * @throws JRException
     * @throws MessagingException
     * @throws IOException
     * @throws CircuitBreakerException
     * @throws InterruptedException
     */
    private void mailDepositReceipt(final Long accountTrxDbId,
            final String toAddress, final String requestingUser,
            boolean isUserRequest) throws JRException, MessagingException,
            IOException, InterruptedException, CircuitBreakerException {

        final OutputProducer outputProducer = OutputProducer.instance();

        final File tempPdfFile =
                new File(OutputProducer.createUniqueTempPdfName(requestingUser,
                        "deposit-receipt"));
        try {

            final PosDepositReceiptDto receipt =
                    createPosPurchaseReceipt(tempPdfFile, accountTrxDbId,
                            requestingUser, isUserRequest);

            final String toName = null;
            final String subject =
                    localize("msg-deposit-email-subject",
                            receipt.getReceiptNumber());
            final String body = localize("msg-deposit-email-body");

            outputProducer.sendEmail(toAddress, toName, subject, body,
                    tempPdfFile, getUserFriendlyFilename(receipt));

        } finally {
            if (tempPdfFile != null && tempPdfFile.exists()) {
                tempPdfFile.delete();
            }
        }

    }

    /**
     *
     * @param receipt
     * @return
     */
    private String getUserFriendlyFilename(final PosDepositReceiptDto receipt) {
        return CommunityDictEnum.SAVAPAGE.getWord() + "-"
                + receipt.getReceiptNumber() + ".pdf";
    }

    /**
     *
     * @param printerName
     * @return
     * @throws ResourceStreamNotFoundException
     */
    private IRequestHandler exportPrinterOpt(final Long printerId)
            throws ResourceStreamNotFoundException {

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final Printer printer = printerDao.findById(printerId);

        /*
         * INVARIANT: printer must exist in database.
         */
        if (printer == null) {
            throw new SpException("Printer [" + printerId + "] not found.");
        }

        final StringBufferResourceStream stream =
                new StringBufferResourceStream("text/text");

        stream.append(IppLogger.logIppPrinterOpt(printer, getSession()
                .getLocale()));

        final ResourceStreamRequestHandler handler =
                new ResourceStreamRequestHandler(stream);

        handler.setFileName(printer.getPrinterName() + ".txt");
        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        handler.setCacheDuration(Duration.NONE);

        return handler;
    }

    /**
     *
     * @param tempFile
     * @param accountTrxDbId
     * @param requestingUser
     * @param isUserRequest
     * @return
     * @throws JRException
     */
    private IRequestHandler exportPosPurchaseReceipt(final File tempFile,
            final Long accountTrxDbId, final String requestingUser,
            boolean isUserRequest) throws JRException {

        final PosDepositReceiptDto receipt =
                createPosPurchaseReceipt(tempFile, accountTrxDbId,
                        requestingUser, isUserRequest);

        final ResourceStreamRequestHandler handler =
                new DownloadRequestHandler(tempFile);

        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        handler.setFileName(getUserFriendlyFilename(receipt));
        handler.setCacheDuration(Duration.NONE);

        return handler;
    }

    /**
     *
     * @param batch
     * @return
     * @throws JRException
     */
    private IRequestHandler exportVoucherBatchPrint(final File tempFile,
            final String jsonDto) throws JRException {

        final VoucherBatchPrintDto dto =
                AbstractDto.create(VoucherBatchPrintDto.class, jsonDto);

        final InternalFontFamilyEnum font =
                ConfigManager
                        .getConfigFontFamily(Key.FINANCIAL_VOUCHER_CARD_FONT_FAMILY);

        final JasperDesign design =
                dto.getDesign().createDesign(font, getLocale());

        final ConfigManager cm = ConfigManager.instance();

        JasperReport jasperReport = JasperCompileManager.compileReport(design);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put(JrVoucherPageDesign.PARM_HEADER,
                cm.getConfigValue(Key.FINANCIAL_VOUCHER_CARD_HEADER));
        parameters.put(JrVoucherPageDesign.PARM_FOOTER,
                cm.getConfigValue(Key.FINANCIAL_VOUCHER_CARD_FOOTER));

        JasperPrint jasperPrint =
                JasperFillManager.fillReport(jasperReport, parameters,
                        JrVoucherPageDesign.createDataSource(dto.getBatchId(),
                                getLocale()));

        JasperExportManager.exportReportToPdfFile(jasperPrint,
                tempFile.getAbsolutePath());

        final ResourceStreamRequestHandler handler =
                new DownloadRequestHandler(tempFile);

        final String userFriendlyFilename =
                CommunityDictEnum.SAVAPAGE.getWord() + "-voucher-batch-"
                        + dto.getBatchId() + ".pdf";

        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        handler.setFileName(userFriendlyFilename);
        handler.setCacheDuration(Duration.NONE);

        return handler;
    }

    /**
     * <p>
     * Handles the {@link JsonApiDict#REQ_REPORT} request by returning the
     * {@link IRequestHandler}.
     * </p>
     *
     * @param parameters
     *            The {@link PageParameters}.
     * @param requestingUser
     * @param isGetAction
     *            {@code true} if this is a GET action, {@code false} when a
     *            POST.
     * @return
     * @throws JRException
     */
    private IRequestHandler exportReport(final File tempFile,
            final String reportId, final String jsonData,
            final String requestingUser, final boolean isGetAction)
            throws JRException {

        final String REPORT_ID_USERLIST = "UserList";

        final Locale locale = getSession().getLocale();
        final String resourceBundleBaseName =
                AbstractJrDesign.getResourceBundleBaseName();

        final InputStream istr = AbstractJrDesign.getJrxmlAsStream(reportId);

        final JasperReport jasperReport =
                JasperCompileManager.compileReport(istr);

        final String fontName =
                ConfigManager.getConfigFontFamily(
                        Key.REPORTS_PDF_INTERNAL_FONT_FAMILY).getJrName();

        jasperReport.getDefaultStyle().setFontName(fontName);

        final Map<String, Object> reportParameters = new HashMap<>();

        reportParameters.put("REPORT_LOCALE", locale);
        reportParameters.put("REPORT_RESOURCE_BUNDLE",
                ResourceBundle.getBundle(resourceBundleBaseName, locale));

        reportParameters.put("SP_APP_VERSION",
                ConfigManager.getAppNameVersion());
        reportParameters.put("SP_REPORT_ACTOR", requestingUser);
        reportParameters.put("SP_REPORT_IMAGE",
                AbstractJrDesign.getHeaderImage());

        final UserPagerReq req = UserPagerReq.read(jsonData);

        JrUserDataSource dataSource = null;

        if (reportId.equals(REPORT_ID_USERLIST)) {
            dataSource = new JrUserDataSource(req, locale);
        } else {
            throw new SpException("Report [" + reportId + "] is NOT supported");
        }

        reportParameters
                .put("SP_DATA_SELECTION", dataSource.getSelectionInfo());

        final JasperPrint jasperPrint =
                JasperFillManager.fillReport(jasperReport, reportParameters,
                        dataSource);

        JasperExportManager.exportReportToPdfFile(jasperPrint,
                tempFile.getAbsolutePath());

        final ResourceStreamRequestHandler handler =
                new DownloadRequestHandler(tempFile);

        final String userFriendlyFilename = reportId + ".pdf";

        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        handler.setFileName(userFriendlyFilename);
        handler.setCacheDuration(Duration.NONE);

        return handler;
    }

    /**
     * Handles the {@link JsonApiDict#REQ_PDF} request by returning the
     * {@link IRequestHandler}.
     *
     * @param lockedUser
     * @param parameters
     *            The {@link PageParameters}.
     * @param isGetAction
     *            {@code true} if this is a GET action, {@code false} when a
     *            POST.
     * @return
     */
    private IRequestHandler handleExportSafePages(final User lockedUser,
            final PageParameters parameters, final boolean isGetAction) {

        IRequestHandler requestHandler = null;

        if (USER_SERVICE.isUserPdfOutDisabled(lockedUser, new Date())) {

            /*
             * Stream HTML (error)
             */
            requestHandler =
                    new TextRequestHandler("text/html", "UTF-8",
                            "<h2 style='color: red;'>"
                                    + localize("msg-user-pdf-out-disabled")
                                    + "</h2>");
        } else {

            File pdfTemp = null;

            try {

                final DocLog docLog = new DocLog();

                /*
                 * (1) Generate the PDF
                 */
                pdfTemp =
                        generatePdfForExport(
                                lockedUser,
                                getParmValue(parameters, isGetAction, "ranges"),
                                getParmValue(parameters, isGetAction,
                                        "graphics").equals(PARM_VALUE_FALSE),
                                docLog, "download");

                /*
                 * (2) Write log to database.
                 */
                docLog.setDeliveryProtocol(DocLogProtocolEnum.HTTP.getDbName());
                docLog.getDocOut().setDestination(getRemoteAddr());

                DOC_LOG_SERVICE.logDocOut(lockedUser, docLog.getDocOut());

                /*
                 * Stream PDF
                 */
                ResourceStreamRequestHandler handler =
                        new PdfFileRequestHandler(lockedUser.getUserId(),
                                pdfTemp);

                handler.setContentDisposition(ContentDisposition.ATTACHMENT);
                handler.setFileName(createMeaningfullPdfFileName(docLog));
                handler.setCacheDuration(Duration.NONE);

                requestHandler = handler;

            } catch (Exception e) {

                if (pdfTemp != null && pdfTemp.exists()) {
                    pdfTemp.delete();
                }

                String msg = null;

                if (e instanceof LetterheadNotFoundException) {
                    msg = localize("exc-letterhead-not-found-login");
                } else {
                    msg = e.getMessage();
                }

                requestHandler =
                        new TextRequestHandler("text/html", "UTF-8",
                                "<h2 style='color: red;'>" + msg + "</h2>");
            }
        }

        return requestHandler;
    }

    /**
     *
     * @param request
     * @param parameters
     * @param isGetAction
     * @param requestingUser
     * @param lockedUser
     * @return
     * @throws Exception
     * @throws CircuitBreakerException
     */
    private Map<String, Object> handleRequest(final String request,
            final PageParameters parameters, final boolean isGetAction,
            final String requestingUser, final User lockedUser)
            throws Exception {

        final User mySessionUser = SpSession.get().getUser();

        switch (request) {

        case JsonApiDict.REQ_ACCOUNT_VOUCHER_BATCH_CREATE:
            return reqVoucherBatchCreate(getParmValue(parameters, isGetAction,
                    "dto"));

        case JsonApiDict.REQ_ACCOUNT_VOUCHER_BATCH_DELETE:
            return reqVoucherBatchDelete(getParmValue(parameters, isGetAction,
                    "batch"));

        case JsonApiDict.REQ_ACCOUNT_VOUCHER_BATCH_EXPIRE:
            return reqVoucherBatchExpire(getParmValue(parameters, isGetAction,
                    "batch"));

        case JsonApiDict.REQ_ACCOUNT_VOUCHER_DELETE_EXPIRED:
            return reqVoucherDeleteExpired();

        case JsonApiDict.REQ_ACCOUNT_VOUCHER_REDEEM:
            return reqVoucherRedeem(requestingUser,
                    getParmValue(parameters, isGetAction, "cardNumber"));

        case JsonApiDict.REQ_CARD_IS_REGISTERED:
            return reqCardIsRegistered(getParmValue(parameters, isGetAction,
                    "card"));

        case JsonApiDict.REQ_CONSTANTS:

            return reqConstants(getParmValue(parameters, isGetAction,
                    "authMode"));

        case JsonApiDict.REQ_CONFIG_GET_PROP:

            return reqConfigGetProp(getParmValue(parameters, isGetAction,
                    "name"));

        case JsonApiDict.REQ_CONFIG_SET_PROPS:

            return reqConfigSetProps(requestingUser,
                    getParmValue(parameters, isGetAction, "props"));

        case JsonApiDict.REQ_DEVICE_DELETE:

            return reqDeviceDelete(requestingUser,
                    getParmValue(parameters, isGetAction, "id"),
                    getParmValue(parameters, isGetAction, "deviceName"));

        case JsonApiDict.REQ_DEVICE_GET:

            return reqDeviceGet(requestingUser,
                    getParmValue(parameters, isGetAction, "id"));

        case JsonApiDict.REQ_DEVICE_SET:

            return reqDeviceSet(requestingUser,
                    getParmValue(parameters, isGetAction, "j_device"));

        case JsonApiDict.REQ_EXIT_EVENT_MONITOR:

            return reqExitEventMonitor(requestingUser);

        case JsonApiDict.REQ_LANGUAGE:

            String language = getParmValue(parameters, isGetAction, "language");

            if (language == null || language.trim().isEmpty()) {

                language = getSession().getLocale().getLanguage();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("using default language [" + language
                            + "] for user [" + requestingUser + "]");
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("language [" + language + "]");
                }
            }
            return reqLanguage(language);

        case JsonApiDict.REQ_LOGIN:

            final String role = getParmValue(parameters, isGetAction, "role");
            final boolean isAdminLogin =
                    role != null && role.equals(USER_ROLE_ADMIN);

            return reqLogin(UserAuth.mode(getParmValue(parameters, isGetAction,
                    "authMode")),
                    getParmValue(parameters, isGetAction, "authId"),
                    getParmValue(parameters, isGetAction, "authPw"),
                    getParmValue(parameters, isGetAction, "authToken"),
                    getParmValue(parameters, isGetAction, "assocCardNumber"),
                    isAdminLogin);

        case JsonApiDict.REQ_LOGOUT:

            return reqLogout(requestingUser,
                    getParmValue(parameters, isGetAction, "authToken"));

        case JsonApiDict.REQ_WEBAPP_UNLOAD:

            return reqWebAppUnload();

        case JsonApiDict.REQ_WEBAPP_CLOSE_SESSION:

            return reqWebAppCloseSession(
                    getParmValue(parameters, isGetAction, "authTokenUser"),
                    getParmValue(parameters, isGetAction, "authTokenAdmin"));

        case JsonApiDict.REQ_MAIL_TEST:

            return reqMailTest(requestingUser,
                    getParmValue(parameters, isGetAction, "mailto"));

        case JsonApiDict.REQ_IMAP_TEST:

            return reqImapTest();

        case JsonApiDict.REQ_IMAP_START:

            return reqImapStart();

        case JsonApiDict.REQ_IMAP_STOP:

            return reqImapStop();

        case JsonApiDict.REQ_PAPERCUT_TEST:

            return reqPaperCutTest();

        case JsonApiDict.REQ_SMARTSCHOOL_TEST:

            return reqSmartSchoolTest();

        case JsonApiDict.REQ_SMARTSCHOOL_START:

            return reqSmartSchoolStart(false);

        case JsonApiDict.REQ_SMARTSCHOOL_START_SIMULATE:

            return reqSmartSchoolStart(true);

        case JsonApiDict.REQ_SMARTSCHOOL_STOP:

            return reqSmartSchoolStop();

        case JsonApiDict.REQ_POS_DEPOSIT:

            return reqPosDeposit(requestingUser,
                    getParmValue(parameters, isGetAction, "dto"));

        case JsonApiDict.REQ_POS_DEPOSIT_QUICK_SEARCH:

            return reqPosDepositQuickSearch(getParmValue(parameters,
                    isGetAction, "dto"));

        case JsonApiDict.REQ_POS_RECEIPT_SENDMAIL:

            return reqPosReceiptSendMail(requestingUser,
                    getParmValue(parameters, isGetAction, "dto"));

        case JsonApiDict.REQ_RESET_ADMIN_PASSWORD:

            return reqAdminPasswordReset(getParmValue(parameters, isGetAction,
                    "password"));

        case JsonApiDict.REQ_RESET_JMX_PASSWORD:

            return reqJmxPasswordReset(getParmValue(parameters, isGetAction,
                    "password"));

        case JsonApiDict.REQ_DB_BACKUP:

            return reqDbBackup(requestingUser);

        case JsonApiDict.REQ_PAGOMETER_RESET:

            return reqPagometerReset(requestingUser,
                    getParmValue(parameters, isGetAction, "scope"));

        case JsonApiDict.REQ_RESET_USER_PASSWORD:

            return reqUserPasswordReset(requestingUser,
                    getParmValue(parameters, isGetAction, "iuser"),
                    getParmValue(parameters, isGetAction, "password"));

        case JsonApiDict.REQ_RESET_USER_PIN:

            return reqUserPinReset(requestingUser,
                    getParmValue(parameters, isGetAction, "user"),
                    getParmValue(parameters, isGetAction, "pin"));

        case JsonApiDict.REQ_JOB_DELETE:

            return reqInboxJobDelete(requestingUser,
                    Integer.valueOf(getParmValue(parameters, isGetAction,
                            "ijob")));

        case JsonApiDict.REQ_JOB_EDIT:

            return reqInboxJobEdit(requestingUser,
                    Integer.valueOf(getParmValue(parameters, isGetAction,
                            "ijob")),
                    getParmValue(parameters, isGetAction, "data"));

        case JsonApiDict.REQ_JOB_PAGES:

            return reqInboxJobPages(
                    requestingUser,
                    getParmValue(parameters, isGetAction, "first-detail-page"),
                    getParmValue(parameters, isGetAction, "unique-url-value"),
                    getParmValue(parameters, isGetAction, "base64").equals(
                            PARM_VALUE_TRUE));

        case JsonApiDict.REQ_JQPLOT:

            return reqJqPlot(
                    getParmValue(parameters, isGetAction, "chartType"),
                    getParmValue(parameters, isGetAction, "isGlobal").equals(
                            PARM_VALUE_TRUE), mySessionUser);

        case JsonApiDict.REQ_OUTBOX_CLEAR:

            return reqOutboxClear(lockedUser);

        case JsonApiDict.REQ_OUTBOX_DELETE_JOB:

            return reqOutboxRemoveJob(lockedUser,
                    getParmValue(parameters, isGetAction, "jobId"));

        case JsonApiDict.REQ_OUTBOX_EXTEND:

            return reqOutboxExtend(lockedUser);

        case JsonApiDict.REQ_PAGE_DELETE:

            return reqInboxPageDelete(requestingUser,
                    getParmValue(parameters, isGetAction, "ranges"));

        case JsonApiDict.REQ_PAGE_MOVE:

            return reqInboxPageMove(requestingUser,
                    getParmValue(parameters, isGetAction, "ranges"),
                    getParmValue(parameters, isGetAction, "position"));

        case JsonApiDict.REQ_PDF_GET_PROPERTIES:

            return reqPdfPropsGet(mySessionUser);

        case JsonApiDict.REQ_PDF_SET_PROPERTIES:

            return reqPdfPropsSet(mySessionUser,
                    getParmValue(parameters, isGetAction, "props"));

        case JsonApiDict.REQ_PRINTER_DETAIL:

            return reqPrinterDetail(requestingUser,
                    getParmValue(parameters, isGetAction, "printer"));

        case JsonApiDict.REQ_PRINTER_GET:

            return reqPrinterGet(requestingUser,
                    getParmValue(parameters, isGetAction, "id"));

        case JsonApiDict.REQ_PRINTER_LIST:

            return reqPrinterList(requestingUser);

        case JsonApiDict.REQ_PRINTER_PRINT:

            return reqPrint(
                    lockedUser,
                    getParmValue(parameters, isGetAction, "printer"),
                    getParmValue(parameters, isGetAction, "readerName"),
                    getParmValue(parameters, isGetAction, "jobName"),
                    getParmValue(parameters, isGetAction, "copies"),
                    getParmValue(parameters, isGetAction, "ranges"),
                    PageScalingEnum.valueOf(getParmValue(parameters,
                            isGetAction, "pageScaling")),
                    getParmValue(parameters, isGetAction, "graphics").equals(
                            PARM_VALUE_FALSE),
                    getParmValue(parameters, isGetAction, "clear").equals(
                            PARM_VALUE_TRUE),
                    getParmValue(parameters, isGetAction, "options"));

        case JsonApiDict.REQ_PRINT_AUTH_CANCEL:
            return reqPrintAuthCancel(Long.parseLong(getParmValue(parameters,
                    isGetAction, "idUser")),
                    getParmValue(parameters, isGetAction, "printer"));

        case JsonApiDict.REQ_PRINT_FAST_RENEW:
            return reqPrintFastRenew(requestingUser);

        case JsonApiDict.REQ_PRINTER_QUICK_SEARCH:

            return reqPrinterQuickSearch(
                    getParmValue(parameters, isGetAction, "dto"),
                    requestingUser);

        case JsonApiDict.REQ_PRINTER_RENAME:

            return reqPrinterRename(requestingUser,
                    getParmValue(parameters, isGetAction, "j_printer"));

        case JsonApiDict.REQ_PRINTER_SET:

            return reqPrinterSet(getParmValue(parameters, isGetAction,
                    "j_printer"));

        case JsonApiDict.REQ_PRINTER_SET_MEDIA_COST:

            return reqPrinterSetMediaCost(getParmValue(parameters, isGetAction,
                    "j_cost"));

        case JsonApiDict.REQ_PRINTER_SET_MEDIA_SOURCES:

            return reqPrinterSetMediaSources(getParmValue(parameters,
                    isGetAction, "j_media_sources"));

        case JsonApiDict.REQ_PRINTER_SYNC:

            return reqPrinterSync(requestingUser);

        case JsonApiDict.REQ_LETTERHEAD_LIST:

            return setApiResultOK(INBOX_SERVICE
                    .getLetterheadList(mySessionUser));

        case JsonApiDict.REQ_LETTERHEAD_ATTACH:

            INBOX_SERVICE.attachLetterhead(
                    mySessionUser,
                    getParmValue(parameters, isGetAction, "id"),
                    getParmValue(parameters, isGetAction, "pub").equals(
                            PARM_VALUE_TRUE));
            return createApiResultOK();

        case JsonApiDict.REQ_LETTERHEAD_DELETE:

            return reqLetterheadDelete(
                    mySessionUser,
                    getParmValue(parameters, isGetAction, "id"),
                    getParmValue(parameters, isGetAction, "pub").equals(
                            PARM_VALUE_TRUE));

        case JsonApiDict.REQ_LETTERHEAD_DETACH:

            INBOX_SERVICE.detachLetterhead(requestingUser);
            return createApiResultOK();

        case JsonApiDict.REQ_LETTERHEAD_NEW:

            try {
                INBOX_SERVICE.createLetterhead(mySessionUser);
                return createApiResultOK();
            } catch (PostScriptDrmException e) {
                return setApiResult(new HashMap<String, Object>(),
                        API_RESULT_CODE_ERROR, "msg-pdf-export-drm-error");
            }

        case JsonApiDict.REQ_LETTERHEAD_GET:

            return setApiResultOK(INBOX_SERVICE.getLetterheadDetails(
                    mySessionUser,
                    getParmValue(parameters, isGetAction, "id"),
                    getParmValue(parameters, isGetAction, "pub").equals(
                            PARM_VALUE_TRUE),
                    getParmValue(parameters, isGetAction, "base64").equals(
                            PARM_VALUE_TRUE)));

        case JsonApiDict.REQ_LETTERHEAD_SET:

            return reqLetterheadSet(mySessionUser,
                    getParmValue(parameters, isGetAction, "id"),
                    getParmValue(parameters, isGetAction, "data"));

        case JsonApiDict.REQ_GCP_GET_DETAILS:

            return reqGcpGetDetails();

        case JsonApiDict.REQ_GCP_ONLINE:

            return reqGcpOnline(
                    mySessionUser,
                    getParmValue(parameters, isGetAction, "online").equals(
                            PARM_VALUE_TRUE));

        case JsonApiDict.REQ_GCP_REGISTER:

            return reqGcpRegister(mySessionUser,
                    getParmValue(parameters, isGetAction, "clientId"),
                    getParmValue(parameters, isGetAction, "clientSecret"),
                    getParmValue(parameters, isGetAction, "printerName"));

        case JsonApiDict.REQ_GCP_SET_DETAILS:

            return reqGcpSetDetails(
                    mySessionUser,
                    getParmValue(parameters, isGetAction, "enabled").equals(
                            PARM_VALUE_TRUE),
                    getParmValue(parameters, isGetAction, "clientId"),
                    getParmValue(parameters, isGetAction, "clientSecret"),
                    getParmValue(parameters, isGetAction, "printerName"));

        case JsonApiDict.REQ_GCP_SET_NOTIFICATIONS:

            return reqGcpSetNotifications(
                    mySessionUser,
                    getParmValue(parameters, isGetAction, "enabled").equals(
                            PARM_VALUE_TRUE),
                    getParmValue(parameters, isGetAction, "emailSubject"),
                    getParmValue(parameters, isGetAction, "emailBody"));

        case JsonApiDict.REQ_GET_EVENT:

            return reqGetEvent(
                    requestingUser,
                    getParmValue(parameters, isGetAction, "page-offset"),
                    getParmValue(parameters, isGetAction, "unique-url-value"),
                    getParmValue(parameters, isGetAction, "base64").equals(
                            PARM_VALUE_TRUE));

        case JsonApiDict.REQ_PING:

            return createApiResultOK();

        case JsonApiDict.REQ_SEND:

            return reqSend(
                    lockedUser,
                    getParmValue(parameters, isGetAction, "mailto"),
                    getParmValue(parameters, isGetAction, "ranges"),
                    getParmValue(parameters, isGetAction, "graphics").equals(
                            PARM_VALUE_FALSE));

        case JsonApiDict.REQ_QUEUE_GET:

            return reqQueueGet(requestingUser,
                    getParmValue(parameters, isGetAction, "id"));

        case JsonApiDict.REQ_QUEUE_SET:

            return reqQueueSet(requestingUser,
                    getParmValue(parameters, isGetAction, "j_queue"));

        case JsonApiDict.REQ_USER_DELETE:

            return reqUserDelete(getParmValue(parameters, isGetAction, "id"),
                    getParmValue(parameters, isGetAction, "userid"));

        case JsonApiDict.REQ_USER_GET:

            return reqUserGet(requestingUser,
                    getParmValue(parameters, isGetAction, "s_user"));

        case JsonApiDict.REQ_USER_GET_STATS:

            return reqUserGetStats(requestingUser);

        case JsonApiDict.REQ_USER_NOTIFY_ACCOUNT_CHANGE:

            return reqUserNotifyAccountChange(getParmValue(parameters,
                    isGetAction, "dto"));

        case JsonApiDict.REQ_USER_SET:
            return reqUserSet(getParmValue(parameters, isGetAction, "userDto"));

        case JsonApiDict.REQ_USER_QUICK_SEARCH:
            return reqUserQuickSearch(getParmValue(parameters, isGetAction,
                    "dto"));

        case JsonApiDict.REQ_USER_SOURCE_GROUPS:

            return reqUserSourceGroups(requestingUser);

        case JsonApiDict.REQ_USER_SYNC:

            return reqUserSync(requestingUser,
                    getParmValue(parameters, isGetAction, "test")
                            .equalsIgnoreCase("Y"),
                    getParmValue(parameters, isGetAction, "delete-users")
                            .equalsIgnoreCase("Y"));

        default:
            throw new SpException("Request [" + request + "] NOT supported");
        }

    }

    /**
     *
     * @param user
     *            The {@link User}.
     * @param documentPageRangeFilter
     *            The page range filter. For example: '1,2,5-6'. The page
     *            numbers in page range filter refer to one-based page numbers
     *            of the integrated {@link InboxInfoDto} document. When
     *            {@code null}, then the full page range is applied.
     * @param removeGraphics
     *            If <code>true</code> graphics are removed (minified to
     *            one-pixel).
     * @param docLog
     * @param purpose
     *            A simple tag to insert into the filename (to add some
     *            meaning).
     * @return The generated PDF file.
     * @throws PostScriptDrmException
     * @throws IOException
     * @throws LetterheadNotFoundException
     * @throws Exception
     */
    private File generatePdfForExport(final User user,
            final String documentPageRangeFilter, boolean removeGraphics,
            final DocLog docLog, final String purpose)
            throws LetterheadNotFoundException, IOException,
            PostScriptDrmException {

        final String pdfFile =
                OutputProducer.createUniqueTempPdfName(user, purpose);

        return OutputProducer.instance().generatePdfForExport(user, pdfFile,
                documentPageRangeFilter, removeGraphics, docLog);
    }

    /**
     *
     * @param docLog
     * @return The PDF file name.
     */
    private String createMeaningfullPdfFileName(final DocLog docLog) {
        String title = docLog.getTitle().trim();

        if (StringUtils.isBlank(title)) {
            title = "savapage";
        }

        return sanitizeFileName(title) + "." + DocContent.FILENAME_EXT_PDF;
    }

    /**
     * Converts a candidate file name to an acceptable name for "any" OS. This
     * solution is not rock solid (I guess), but will do for now.
     * <p>
     * Characters / ? < > \ : * | ” are suspect because they are illegal as file
     * or folder names on Windows using NTFS.
     * </p>
     * <p>
     * The caret ^ is not permitted under Windows Operating Systems using the
     * FAT file system.
     * </p>
     * <p>
     * The only illegal character for file and folder names in Mac OS X is the
     * colon “:”
     * </p>
     *
     * @param name
     * @return
     */
    private static String sanitizeFileName(final String name) {

        String converted = "";
        for (char ch : name.toCharArray()) {
            switch (ch) {
            case '?':
            case '^':
            case ':':
                // no code intended
                break;
            case '*':
                converted += 'x';
                break;
            case '<':
                converted += '[';
                break;
            case '>':
                converted += ']';
                break;
            case '|':
            case '/':
            case '\\':
                converted += '-';
                break;
            default:
                converted += ch;
            }
        }
        return converted;
    }

    /**
     * Return a localized message string. IMPORTANT: The locale from the session
     * is used.
     *
     * @param key
     *            The key of the message.
     * @return The message text.
     */
    private String localize(final String key) {
        return Messages.getMessage(getClass(), getSession().getLocale(), key,
                (String[]) null);
    }

    /**
     * Return a localized message string. IMPORTANT: The locale from the session
     * is used.
     *
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    private String localize(final String key, final String... args) {
        return Messages.getMessage(getClass(), getSession().getLocale(), key,
                args);
    }

    /**
     *
     * @param data
     * @return
     */
    private boolean isApiResultOK(final Map<String, Object> data) {
        return isApiResultCode(data, API_RESULT_CODE_OK);
    }

    /**
     *
     * @param data
     * @return
     */
    private boolean isApiResultError(final Map<String, Object> data) {
        return isApiResultCode(data, API_RESULT_CODE_ERROR);
    }

    /**
     *
     * @param data
     * @return
     */
    private boolean isApiResultCode(final Map<String, Object> data,
            final String code) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) data.get("result");
        return result.get("code").equals(code);
    }

    /**
     * Creates the API result on parameter {@code out}.
     *
     * @param out
     *            The Map to put the result on.
     * @param code
     *            One of the following codes: {@link #API_RESULT_CODE_OK},
     *            {@link #API_RESULT_CODE_INFO}, {@link #API_RESULT_CODE_WARN},
     *            {@link #API_RESULT_CODE_ERROR}
     * @param msg
     *            The key of the message
     * @param txt
     *            The message text.
     * @return the {@code out} parameter.
     *
     */
    private static Map<String, Object> createApiResult(
            final Map<String, Object> out, final String code, final String msg,
            final String txt) {

        Map<String, Object> result = new HashMap<String, Object>();
        out.put("result", result);

        result.put("code", code);
        if (msg != null) {
            result.put("msg", msg);
        }
        if (txt != null) {
            result.put("txt", txt);
        }
        return out;
    }

    /**
     * Sets the {@link #API_RESULT_CODE_OK} result on parameter {@code out}.
     *
     * @param out
     * @return the {@code out} parameter.
     */
    private Map<String, Object> setApiResultOK(final Map<String, Object> out) {
        return createApiResult(out, API_RESULT_CODE_OK, null, null);
    }

    /**
     * Creates and {@link #API_RESULT_CODE_OK} API result.
     *
     * @return The API response with the OK result.
     */
    private Map<String, Object> createApiResultOK() {
        return setApiResultOK(new HashMap<String, Object>());
    }

    /**
     * Sets the API result on parameter {@code out}.
     *
     * @param out
     *            The Map to put the result on.
     * @param code
     *            One og the following codes: {@link #API_RESULT_CODE_OK},
     *            {@link #API_RESULT_CODE_INFO}, {@link #API_RESULT_CODE_WARN},
     *            {@link #API_RESULT_CODE_ERROR}
     * @param key
     *            The key of the message
     * @return the {@code out} parameter.
     *
     */
    private Map<String, Object> setApiResult(final Map<String, Object> out,
            final String code, final String key) {
        return createApiResult(out, code, key, localize(key));
    }

    /**
     * Sets the JSON {@code result} and {@code requestStatus} of a Proxy Print
     * Request on parameter {@code out}.
     *
     * @param out
     *            The Map to put the {@code result} on.
     * @param printReq
     *            The Proxy Print Request.
     * @return the {@code out} parameter.
     */
    public static Map<String, Object> setApiResultMsg(
            final Map<String, Object> out, ProxyPrintInboxReq printReq) {

        String code;
        switch (printReq.getStatus()) {
        case ERROR_PRINTER_NOT_FOUND:
            code = API_RESULT_CODE_ERROR;
            break;
        case PRINTED:
            code = API_RESULT_CODE_OK;
            break;
        case WAITING_FOR_RELEASE:
            code = API_RESULT_CODE_OK;
            break;
        default:
            code = API_RESULT_CODE_WARN;
        }

        out.put("requestStatus", printReq.getStatus().toString());

        return createApiResult(out, code, printReq.getUserMsgKey(),
                printReq.getUserMsg());
    }

    /**
     *
     * @param out
     * @param key
     * @param msg
     * @return
     */
    public static Map<String, Object> setApiResultMsgError(
            final Map<String, Object> out, final String key, final String msg) {
        return createApiResult(out, API_RESULT_CODE_ERROR, key, msg);
    }

    /**
     *
     * @param out
     * @param key
     * @param msg
     * @return
     */
    public static Map<String, Object> setApiResultMsgOK(
            final Map<String, Object> out, final String key, final String msg) {
        return createApiResult(out, API_RESULT_CODE_OK, key, msg);
    }

    /**
     * Set the API result on parameter {@code out}.
     *
     * @param out
     *            The Map to put the result on.
     * @param code
     *            One og the following codes: {@link #API_RESULT_CODE_OK},
     *            {@link #API_RESULT_CODE_INFO}, {@link #API_RESULT_CODE_WARN},
     *            {@link #API_RESULT_CODE_ERROR}
     * @param key
     *            The key of the message
     * @param args
     *            The placeholder arguments for the message.
     * @return the {@code out} parameter.
     */
    private Map<String, Object> setApiResult(final Map<String, Object> out,
            final String code, final String key, final String... args) {
        return createApiResult(out, code, key, localize(key, args));
    }

    /**
     *
     * @param e
     * @return
     */
    private Map<String, Object> handleException(final Exception exception) {

        String msg = null;
        String msgKey = null;

        if (exception instanceof LetterheadNotFoundException) {
            msgKey = "exc-letterhead-not-found";
            msg = localize(msgKey);
        }

        final boolean isShutdown = ConfigManager.isShutdownInProgress();

        if (msgKey == null) {

            msgKey = "msg-exception";
            msg =
                    exception.getClass().getSimpleName() + ": "
                            + exception.getMessage();

            if (!isShutdown) {
                LOGGER.error(exception.getMessage(), exception);
            }

        } else {

            if (!isShutdown) {
                LOGGER.warn(exception.getMessage());
            }
        }

        return createApiResult(new HashMap<String, Object>(),
                API_RESULT_CODE_ERROR, msgKey, msg);
    }

    /**
     * Checks if user of current session has admin rights.
     *
     * @param userData
     *            User data to put message in when user has NO admin rights.
     * @return <code>true</code> when user has admin rights.
     */
    private boolean hasAdminRights(Map<String, Object> userData) {

        if (!SpSession.get().isAdmin()) {
            setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-need-admin-rights");
            return false;
        }
        return true;
    }

    /**
     * Checks if the request is valid and the user is authorized to perform the
     * API.
     *
     * @param uid
     *            The user id.
     * @return {@code null} if user is authorized, otherwise a Map object
     *         containing the error message.
     */
    private Map<String, Object> checkValidAndAuthorized(final String request,
            final String uid) {

        Map<String, Object> userData = null;

        if (!API_DICTIONARY.isValidRequest(request)) {
            userData = new HashMap<String, Object>();
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-invalid-request");
        }

        if (!API_DICTIONARY.isAuthenticationNeeded(request)) {
            return null;
        }

        SpSession session = SpSession.get();

        boolean authorized = false;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Checking user in session from application ["
                    + session.getApplication().getClass().toString() + "]");
        }

        if (session.isAuthenticated()) {

            if (session.getUser().getUserId().equals(uid)) {

                if (API_DICTIONARY.isAdminAuthenticationNeeded(request)) {
                    authorized = session.getUser().getAdmin().booleanValue();
                } else {
                    authorized = true;
                }

            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("request [" + request + "]: user [" + uid
                            + "] is NOT the owner " + "of this session ["
                            + session.getId() + "]");
                }
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("request [" + request
                        + "]: NO user authenticated in session ["
                        + session.getId() + "]");
            }
        }

        //
        if (authorized) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("user [" + uid + "] is owner of session "
                        + session.getId());
            }
        } else {
            userData = new HashMap<String, Object>();
            setApiResult(userData, API_RESULT_CODE_UNAUTH,
                    "msg-user-not-authorized");
        }

        return userData;
    }

    /**
     *
     * @param user
     * @param mailto
     * @return
     * @throws IOException
     * @throws LetterheadNotFoundException
     * @throws Exception
     * @throws MessagingException
     * @throws CircuitBreakerException
     * @throws InterruptedException
     * @throws ParseException
     */
    private Map<String, Object> reqSend(final User lockedUser,
            final String mailto, final String ranges, boolean removeGraphics)
            throws LetterheadNotFoundException, IOException,
            MessagingException, InterruptedException, CircuitBreakerException,
            ParseException {

        final String user = lockedUser.getUserId();

        final Map<String, Object> userData = new HashMap<String, Object>();

        File fileAttach = null;

        try {

            final DocLog docLog = new DocLog();

            /*
             * (1) Generate with existing user lock.
             */
            fileAttach =
                    generatePdfForExport(lockedUser, ranges, removeGraphics,
                            docLog, "email");
            /*
             * INVARIANT: Since sending the mail is synchronous, file length is
             * important and MUST be less than criterion.
             */
            final int maxFileKb =
                    ConfigManager.instance().getConfigInt(
                            Key.MAIL_SMTP_MAX_FILE_KB);

            final long maxFileBytes = maxFileKb * 1024;

            if (fileAttach.length() > maxFileBytes) {

                final BigDecimal fileMb =
                        new BigDecimal((double) fileAttach.length()
                                / (1024 * 1024));

                final BigDecimal maxFileMb =
                        new BigDecimal((double) maxFileKb / 1024);

                setApiResult(
                        userData,
                        API_RESULT_CODE_WARN,
                        "msg-mail-max-file-size",
                        BigDecimalUtil.localize(fileMb, 2, this.getSession()
                                .getLocale(), true)
                                + " MB",
                        BigDecimalUtil.localize(maxFileMb, 2, this.getSession()
                                .getLocale(), true)
                                + " MB");

            } else {

                final String fileName = createMeaningfullPdfFileName(docLog);

                final String subject = fileName;
                final String body =
                        "Hi,\n\nHere are the SafePages attached from " + user
                                + ".\n\n--\n"
                                + CommunityDictEnum.SAVAPAGE.getWord() + " "
                                + ConfigManager.getAppVersion();

                /*
                 * (2) Unlock the user BEFORE sending the email.
                 */
                ServiceContext.getDaoContext().rollback();

                /*
                 * (3) Send email.
                 */
                OutputProducer.instance().sendEmail(mailto, user, subject,
                        body, fileAttach, fileName);

                /*
                 * (4) Log in database
                 */
                docLog.setDeliveryProtocol(DocLogProtocolEnum.SMTP.getDbName());
                docLog.getDocOut().setDestination(mailto);

                DOC_LOG_SERVICE.logDocOut(lockedUser, docLog.getDocOut());

                addUserStats(userData, lockedUser, this.getSession()
                        .getLocale(), SpSession.getCurrencySymbol());

                setApiResult(userData, API_RESULT_CODE_OK, "msg-mail-sent",
                        mailto);
            }

        } catch (PostScriptDrmException e) {

            setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-pdf-export-drm-error");

        } finally {

            if ((fileAttach != null) && fileAttach.exists()) {

                if (fileAttach.delete()) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("deleted temp file [" + fileAttach + "]");
                    }
                } else {
                    LOGGER.error("delete of temp file [" + fileAttach
                            + "] FAILED");
                }
            }
        }

        return userData;
    }

    /**
     *
     * @param user
     * @return
     */
    private Map<String, Object> reqOutboxClear(final User lockedUser) {
        final Map<String, Object> userData = new HashMap<String, Object>();

        final int jobCount = OUTBOX_SERVICE.clearOutbox(lockedUser.getUserId());

        final String msgKey;

        if (jobCount == 0) {
            msgKey = "msg-outbox-cleared-none";
        } else if (jobCount == 1) {
            msgKey = "msg-outbox-cleared-one";
        } else {
            msgKey = "msg-outbox-cleared-multiple";
        }

        setApiResult(userData, API_RESULT_CODE_OK, msgKey,
                String.valueOf(jobCount));

        addUserStats(userData, lockedUser, this.getSession().getLocale(),
                SpSession.getCurrencySymbol());

        return userData;
    }

    /**
     *
     * @param user
     * @return
     */
    private Map<String, Object> reqOutboxRemoveJob(final User lockedUser,
            final String jobFileName) {
        final Map<String, Object> userData = new HashMap<String, Object>();

        final boolean removedJob =
                OUTBOX_SERVICE.removeOutboxJob(lockedUser.getUserId(),
                        jobFileName);

        final String msgKey;

        if (removedJob) {
            msgKey = "msg-outbox-removed-job";
        } else {
            msgKey = "msg-outbox-removed-job-none";
        }

        setApiResult(userData, API_RESULT_CODE_OK, msgKey);

        addUserStats(userData, lockedUser, this.getSession().getLocale(),
                SpSession.getCurrencySymbol());

        return userData;
    }

    /**
     *
     * @param userId
     * @return
     */
    private Map<String, Object> reqOutboxExtend(final User lockedUser) {

        final Map<String, Object> userData = new HashMap<String, Object>();

        final int minutes = 10;

        final int jobCount =
                OUTBOX_SERVICE.extendOutboxExpiry(lockedUser.getUserId(),
                        minutes);

        final String msgKey;

        if (jobCount == 0) {
            msgKey = "msg-outbox-extended-none";
        } else if (jobCount == 1) {
            msgKey = "msg-outbox-extended-one";
        } else {
            msgKey = "msg-outbox-extended-multiple";
        }

        setApiResult(userData, API_RESULT_CODE_OK, msgKey,
                String.valueOf(jobCount), String.valueOf(minutes));

        addUserStats(userData, lockedUser, this.getSession().getLocale(),
                SpSession.getCurrencySymbol());

        return userData;
    }

    /**
     *
     * @param user
     * @param ranges
     * @return
     */
    private Map<String, Object> reqInboxPageDelete(final String user,
            final String ranges) {

        final Map<String, Object> userData = new HashMap<String, Object>();

        final int nPages = INBOX_SERVICE.deletePages(user, ranges);

        final String msgKey;

        if (nPages == 1) {
            msgKey = "msg-page-delete-one";
        } else {
            msgKey = "msg-page-delete-multiple";
        }

        return setApiResult(userData, API_RESULT_CODE_OK, msgKey,
                String.valueOf(nPages));
    }

    /**
     * Gets the PDF properties.
     *
     * @param user
     * @return
     * @throws Exception
     */
    private Map<String, Object> reqPdfPropsGet(final User user)
            throws Exception {

        final PdfProperties objProps = USER_SERVICE.getPdfProperties(user);

        Map<String, Object> userData = new HashMap<String, Object>();

        userData.put("props", objProps);
        return setApiResultOK(userData);
    }

    /**
     * Sets the PDF properties.
     *
     * @param user
     * @param json
     * @return
     * @throws Exception
     */
    private Map<String, Object> reqPdfPropsSet(final User user,
            final String json) throws Exception {

        Map<String, Object> userData = new HashMap<String, Object>();
        PdfProperties pdfProp = PdfProperties.create(json);

        String pwOwner = pdfProp.getPw().getOwner();
        String pwUser = pdfProp.getPw().getUser();

        if (StringUtils.isNotBlank(pwOwner) && StringUtils.isNotBlank(pwUser)
                && pwOwner.equals(pwUser)) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-pdf-identical-owner-user-pw");
        } else {
            USER_SERVICE.setPdfProperties(user, pdfProp);
            return setApiResultOK(userData);
        }
    }

    /**
     * Custom validates a {@link IConfigProp.Key} value.
     *
     * @param userData
     *            The user data to be returned with error message when value is
     *            NOT valid.
     * @param key
     *            The key of the configuration item.
     * @param value
     *            The value of the configuration item.
     * @return {@code null} when NO validation error, or the userData object
     *         filled with the error message when an error is encountered..
     */
    private Map<String, Object> customConfigPropValidate(
            Map<String, Object> userData, Key key, String value) {

        if (key == Key.PROXY_PRINT_NON_SECURE_PRINTER_GROUP
                && StringUtils.isNotBlank(value)) {

            final PrinterGroup jpaPrinterGroup =
                    ServiceContext.getDaoContext().getPrinterGroupDao()
                            .findByName(value);

            if (jpaPrinterGroup == null) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-device-printer-group-not-found", value);
            }
        }

        if ((key == Key.SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER || key == Key.SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER)
                && StringUtils.isNotBlank(value)) {

            final PrinterDao printerDao =
                    ServiceContext.getDaoContext().getPrinterDao();

            if (printerDao.findByName(value) == null) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-printer-not-found", value);
            }
        }

        return null;
    }

    /**
     * Updates one or more configuration properties. Valid properties are
     * updated (and eventually committed), till an invalid property is
     * encountered.
     *
     * @param user
     * @param jsonProps
     * @return
     * @throws ParseException
     */
    private Map<String, Object> reqConfigSetProps(final String requestingUser,
            final String jsonProps) throws ParseException {

        final Map<String, Object> userData = new HashMap<String, Object>();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("jsonProps: " + jsonProps);
        }

        final JsonNode list;

        try {
            list = new ObjectMapper().readTree(jsonProps);
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        final ConfigManager cm = ConfigManager.instance();

        final Iterator<String> iter = list.getFieldNames();

        boolean isValid = true;
        int nJobsRescheduled = 0;
        int nValid = 0;

        while (iter.hasNext() && isValid) {

            final String key = iter.next();

            String value = list.get(key).getTextValue();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(key + " = " + value);
            }

            final Key configKey = cm.getConfigKey(key);

            /*
             * If this value is Locale formatted, we MUST revert to locale
             * independent format.
             */
            if (cm.isConfigBigDecimal(configKey)) {
                value =
                        BigDecimalUtil.toPlainString(value, getSession()
                                .getLocale(), false, true);
            }

            /*
             *
             */
            if (customConfigPropValidate(userData, configKey, value) != null) {
                return userData;
            }

            IConfigProp.ValidationResult res = cm.validate(configKey, value);
            isValid = res.isValid();

            if (isValid) {

                switch (configKey) {

                case SYS_DEFAULT_LOCALE:
                    ConfigManager.setDefaultLocale(value);
                    break;

                case SCHEDULE_HOURLY:
                    SpJobScheduler.instance().scheduleJobs(configKey);
                    nJobsRescheduled++;
                    break;
                case SCHEDULE_DAILY:
                    SpJobScheduler.instance().scheduleJobs(configKey);
                    nJobsRescheduled++;
                    break;
                case SCHEDULE_WEEKLY:
                    SpJobScheduler.instance().scheduleJobs(configKey);
                    nJobsRescheduled++;
                    break;
                case SCHEDULE_MONTHLY:
                    SpJobScheduler.instance().scheduleJobs(configKey);
                    nJobsRescheduled++;
                    break;
                case SCHEDULE_DAILY_MAINT:
                    SpJobScheduler.instance().scheduleJobs(configKey);
                    nJobsRescheduled++;
                    break;

                default:
                    break;
                }

                ConfigManager.instance().updateConfigKey(configKey, value,
                        requestingUser);

                nValid++;

            } else {
                setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-config-props-error", value);
            }

        } // end-while

        if (nValid > 0) {
            ConfigManager.instance().calcRunnable();
        }

        if (isValid) {
            final String msg =
                    (nJobsRescheduled == 0) ? "msg-config-props-applied"
                            : "msg-config-props-applied-rescheduled";

            setApiResult(userData, API_RESULT_CODE_OK, msg);
        }

        return userData;
    }

    /**
     *
     * @param userName
     *            The requesting user.
     * @return The {@linkJsonPrinterList}.
     * @throws Exception
     */
    private JsonPrinterList getUserPrinterList(final String userName)
            throws Exception {

        final JsonPrinterList jsonPrinterList =
                PROXY_PRINT_SERVICE.getUserPrinterList(getHostTerminal(),
                        userName);

        if (jsonPrinterList.getDfault() != null) {
            PROXY_PRINT_SERVICE.localize(getSession().getLocale(),
                    jsonPrinterList.getDfault());
        }
        return jsonPrinterList;
    }

    /**
     *
     * @param userName
     *            The requesting user.
     * @return
     * @throws Exception
     */
    private Map<String, Object> reqPrinterList(final String userName)
            throws Exception {

        final Map<String, Object> data = new HashMap<String, Object>();

        if (!PROXY_PRINT_SERVICE.isConnectedToCups()) {
            return setApiResult(data, API_RESULT_CODE_ERROR,
                    "msg-printer-connection-broken");
        }

        final JsonPrinterList jsonPrinterList = getUserPrinterList(userName);

        data.put("data", jsonPrinterList);
        return setApiResultOK(data);
    }

    /**
     *
     * @param json
     *            The JSON request.
     * @param userName
     *            The requesting user.
     * @return
     */
    private Map<String, Object> reqPrinterQuickSearch(final String json,
            final String userName) throws Exception {

        final Map<String, Object> data = new HashMap<String, Object>();

        final QuickSearchFilterDto dto =
                AbstractDto.create(QuickSearchFilterDto.class, json);

        final List<QuickSearchItemDto> items = new ArrayList<>();

        final JsonPrinterList jsonPrinterList = getUserPrinterList(userName);

        final int maxItems = dto.getMaxResults().intValue();

        final String filter = dto.getFilter().toLowerCase();

        /*
         * First iteration to collect the items, fastPrintAvaliable.
         */
        final Iterator<JsonPrinter> iter = jsonPrinterList.getList().iterator();

        // Is printer with Fast Proxy Print available?
        Boolean fastPrintAvailable = null;

        while (iter.hasNext()) {

            final JsonPrinter printer = iter.next();

            if (printer.getAuthMode() != null && printer.getAuthMode().isFast()) {
                fastPrintAvailable = Boolean.TRUE;
            }

            final String location = printer.getLocation();

            if (StringUtils.isEmpty(filter)
                    || printer.getAlias().toLowerCase().contains(filter)
                    || (StringUtils.isNotBlank(location) && location
                            .toLowerCase().contains(filter))) {

                if (items.size() < maxItems) {

                    final QuickSearchPrinterItemDto itemWlk =
                            new QuickSearchPrinterItemDto();

                    itemWlk.setKey(printer.getDbKey());
                    itemWlk.setText(printer.getAlias());
                    itemWlk.setPrinter(printer);

                    items.add(itemWlk);
                } else {
                    break;
                }
            }
        }

        /*
         * We need to know if there are any "Fast Release" printers available
         * (even if not part of this search list). So, if fastPrintAvaliable was
         * NOT collected iterate the remainder of the list.
         *
         * Reason: the client may want to display a button to extend the Fast
         * Print Closing Time.
         */
        if (fastPrintAvailable == null) {

            fastPrintAvailable = Boolean.FALSE;

            while (iter.hasNext()) {

                final JsonPrinter printer = iter.next();

                if (printer.getAuthMode() != null
                        && printer.getAuthMode().isFast()) {
                    fastPrintAvailable = Boolean.TRUE;
                    break;
                }
            }
        }

        /*
         *
         */
        data.put("items", items);
        data.put("fastPrintAvailable", fastPrintAvailable);

        return setApiResultOK(data);
    }

    /**
     * {@link JsonApiDict#REQ_PRINTER_DETAIL}.
     *
     * @param user
     * @return
     */
    private Map<String, Object> reqPrinterDetail(final String user,
            final String printerName) {

        final Map<String, Object> data = new HashMap<String, Object>();

        if (!PROXY_PRINT_SERVICE.isConnectedToCups()) {
            return setApiResult(data, API_RESULT_CODE_ERROR,
                    "msg-printer-connection-broken");
        }

        final JsonPrinterDetail jsonPrinter =
                PROXY_PRINT_SERVICE.getPrinterDetailUserCopy(getSession()
                        .getLocale(), printerName);

        if (jsonPrinter == null) {
            setApiResult(data, API_RESULT_CODE_ERROR, "msg-printer-out-of-date");
        } else {
            data.put("printer", jsonPrinter);
            setApiResultOK(data);
        }
        return data;
    }

    /**
     *
     * @param requestingUser
     * @param printerName
     * @return
     * @throws InterruptedException
     */
    private Map<String, Object> reqPrintAuthCancel(final Long idUser,
            final String printerName) throws InterruptedException {

        final Map<String, Object> data = new HashMap<String, Object>();

        if (ProxyPrintAuthManager.cancelRequest(idUser, printerName)) {
            return setApiResult(data, API_RESULT_CODE_OK,
                    "msg-print-auth-cancel-ok");
        } else {
            return setApiResult(data, API_RESULT_CODE_WARN,
                    "msg-print-auth-cancel-not-found");
        }
    }

    /**
     *
     * @param requestingUser
     * @return
     */
    private Map<String, Object> reqPrintFastRenew(final String requestingUser) {

        final Map<String, Object> data = new HashMap<String, Object>();

        final InboxInfoDto dto =
                INBOX_SERVICE.touchLastPreviewTime(requestingUser);

        final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

        final String expiry =
                timeFormat.format(new Date(dto.getLastPreviewTime().longValue()
                        + ConfigManager.instance().getConfigLong(
                                Key.PROXY_PRINT_FAST_EXPIRY_MINS) * 60 * 1000));

        data.put("expiry", expiry);

        return setApiResult(data, API_RESULT_CODE_OK,
                "msg-print-fast-renew-ok", expiry);
    }

    /**
     * Gets the localized string for a BigDecimal.
     *
     * @param vlaue
     *            The {@link BigDecimal}.
     * @param currencySymbol
     *            {@code null} when not available.
     * @return The localized string.
     * @throws ParseException
     */
    private final String localizedPrinterCost(final BigDecimal decimal,
            final String currencySymbol) throws ParseException {

        BigDecimal value = decimal;

        if (value == null) {
            value = BigDecimal.ZERO;
        }

        String cost =
                BigDecimalUtil.localize(value, ConfigManager
                        .getPrinterCostDecimals(), this.getSession()
                        .getLocale(), true);

        if (StringUtils.isBlank(currencySymbol)) {
            return cost;
        }
        return currencySymbol + cost;
    }

    /**
     * Handles a Proxy Print request.
     *
     * IMPORTANT: The printing transaction MUST be guarded by
     * {@link ConfigManager#readPrintOutLock()}. This is managed via
     * {@link JsonApiDict#getPrintOutLockNeeded(String)}.
     *
     * @param lockedUser
     * @param printerName
     * @param readerName
     * @param jobName
     * @param copies
     * @param rangesRaw
     * @param fitToPage
     * @param removeGraphics
     * @param clearInbox
     * @param jsonOptions
     * @return
     * @throws Exception
     */
    private Map<String, Object> reqPrint(final User lockedUser,
            final String printerName, final String readerName,
            final String jobName, final String copies, final String rangesRaw,
            final PageScalingEnum pageScaling, final boolean removeGraphics,
            final boolean clearInbox, final String jsonOptions)
            throws Exception {

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final Map<String, Object> data = new HashMap<String, Object>();

        final Printer printer;

        try {
            printer =
                    PROXY_PRINT_SERVICE.getValidateProxyPrinterAccess(
                            lockedUser, printerName,
                            ServiceContext.getTransactionDate());
        } catch (ProxyPrintException e) {
            return setApiResult(data, API_RESULT_CODE_ERROR, e.getMessage());
        }

        // Example: {"media.type":"Plain","Resolution":"300x300dpi"}

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("printer [" + printerName + "] reader [" + readerName
                    + "] copies [" + copies + "] ranges [" + rangesRaw
                    + "] clear [" + clearInbox + "] JSON options: "
                    + jsonOptions);
        }

        final InboxInfoDto jobs =
                INBOX_SERVICE.getInboxInfo(lockedUser.getUserId());

        final int nPagesTot = INBOX_SERVICE.calcNumberOfPagesInJobs(jobs);
        int nPagesPrinted = nPagesTot;

        /*
         * Validate the ranges.
         */
        String ranges = rangesRaw.trim();

        if (!ranges.isEmpty()) {
            /*
             * remove spaces
             */
            ranges = ranges.replace(" ", "");
            try {
                final List<RangeAtom> rangeAtoms =
                        INBOX_SERVICE.createSortedRangeArray(ranges);

                nPagesPrinted =
                        InboxServiceImpl.calcSelectedDocPages(rangeAtoms,
                                nPagesTot);
                /*
                 * This gives the SORTED ranges as string: CUPS cannot handle
                 * ranges like '7-8,5,2' but needs '2,5,7-8'
                 */
                ranges = RangeAtom.asText(rangeAtoms);
            } catch (Exception e) {
                return setApiResult(data, API_RESULT_CODE_ERROR,
                        "msg-clear-range-syntax-error", ranges);
            }
        }

        /*
         * INVARIANT: number of printed pages can NOT exceed total number of
         * pages.
         */
        if (nPagesPrinted > nPagesTot) {
            return setApiResult(data, API_RESULT_CODE_ERROR,
                    "msg-print-out-of-range-error", ranges,
                    String.valueOf(nPagesTot));
        }

        /*
         * Collect the user printer options.
         */
        final JsonNode list = new ObjectMapper().readTree(jsonOptions);
        final Map<String, String> options = new HashMap<String, String>();

        String key = null;

        final Iterator<String> iter = list.getFieldNames();

        while (iter.hasNext()) {
            key = iter.next();
            options.put(key, list.get(key).getTextValue());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(key + " = " + list.get(key).getTextValue());
            }
        }

        /*
         * Create the proxy print request, and chunk it.
         */
        final ProxyPrintInboxReq printReq = new ProxyPrintInboxReq();

        printReq.setClearPages(clearInbox);
        printReq.setJobName(jobName);
        printReq.setPageRanges(ranges);
        printReq.setNumberOfCopies(Integer.parseInt(copies));
        printReq.setNumberOfPages(nPagesPrinted);
        printReq.setOptionValues(options);
        printReq.setPrinterName(printerName);
        printReq.setRemoveGraphics(removeGraphics);
        printReq.setLocale(getSession().getLocale());
        printReq.setIdUser(lockedUser.getId());

        PROXY_PRINT_SERVICE.chunkProxyPrintRequest(lockedUser, printReq,
                pageScaling);

        /*
         * Calculate the printing cost.
         */
        final String currencySymbol = SpSession.getCurrencySymbol();

        final BigDecimal cost;

        try {

            final ProxyPrintCostParms costParms = new ProxyPrintCostParms();

            /*
             * Set the common parameters for all print job chunks, and calculate
             * the cost.
             */
            costParms.setDuplex(printReq.isDuplex());
            costParms.setGrayscale(printReq.isGrayscale());
            costParms.setNumberOfCopies(printReq.getNumberOfCopies());

            cost =
                    ACCOUNTING_SERVICE.calcProxyPrintCost(
                            ServiceContext.getLocale(), currencySymbol,
                            lockedUser, printer, costParms,
                            printReq.getJobChunkInfo());

        } catch (ProxyPrintException e) {
            return createApiResult(data, API_RESULT_CODE_WARN, "",
                    e.getMessage());
        }

        printReq.setCost(cost);

        final String localizedCost = localizedPrinterCost(cost, null);

        /*
         * Direct Proxy Print?
         */
        if (readerName == null) {

            printReq.setPrintMode(PrintModeEnum.PUSH);

            PROXY_PRINT_SERVICE.proxyPrintInbox(lockedUser, printReq);

            setApiResultMsg(data, printReq);

            if (printReq.getStatus() == ProxyPrintInboxReq.Status.PRINTED) {

                addUserStats(data, lockedUser, getSession().getLocale(),
                        currencySymbol);
            }

            return data;
        }

        /*
         * Proxy Print Authentication is needed (secure printing).
         */
        final Device device = deviceDao.findByName(readerName);

        /*
         * INVARIANT: Device MUST exits.
         */
        if (device == null) {
            throw new SpException("Reader Device [" + readerName
                    + "] NOT found");
        }
        /*
         * INVARIANT: Device MUST be enabled.
         */
        if (device.getDisabled()) {
            throw new SpException("Device [" + readerName + "] is disabled");
        }

        /*
         * INVARIANT: Device MUST be a reader.
         */
        if (!deviceDao.isCardReader(device)) {
            throw new SpException("Device [" + readerName
                    + "] is NOT a Card Reader");
        }

        /*
         * INVARIANT: Reader MUST have Printer restriction.
         */
        if (!deviceDao.hasPrinterRestriction(device)) {
            throw new SpException("Reader [" + readerName
                    + "] does not have associated Printer(s).");
        }

        /*
         * INVARIANT: Reader MUST have Printer restriction.
         */
        if (device.getPrinter() == null) {

            if (!PRINTER_SERVICE.checkDeviceSecurity(printer,
                    DeviceTypeEnum.CARD_READER, device)) {

                throw new SpException("Reader [" + readerName
                        + "] does not have associated Printer(s).");
            }
        }

        /*
         * Accounting.
         */
        if (StringUtils.isNotBlank(localizedCost)) {
            data.put("formattedCost", localizedCost);

            if (StringUtils.isNotBlank(localizedCost)) {
                data.put("currencySymbol", currencySymbol);
            }
        }

        /*
         * Fast proxy Print via outbox?
         */
        final ProxyPrintAuthModeEnum authModeEnum =
                DEVICE_SERVICE.getProxyPrintAuthMode(device.getId());

        if (authModeEnum.isHoldRelease()) {

            printReq.setPrintMode(PrintModeEnum.HOLD);

            OUTBOX_SERVICE.proxyPrintInbox(lockedUser, printReq);

            setApiResultMsg(data, printReq);

            addUserStats(data, lockedUser, getSession().getLocale(),
                    currencySymbol);

            /*
             * Since the job is present in the outbox we can honor the clearIbox
             * request.
             */
            if (clearInbox) {
                INBOX_SERVICE.deleteAllPages(lockedUser.getUserId());
            }

            return data;
        }

        /*
         * User WebApp Authenticated Proxy Print.
         */
        printReq.setPrintMode(PrintModeEnum.AUTH);
        printReq.setStatus(ProxyPrintInboxReq.Status.NEEDS_AUTH);

        if (ProxyPrintAuthManager.submitRequest(printerName,
                device.getHostname(), printReq)) {
            /*
             * Signal NEEDS_AUTH
             */
            data.put("requestStatus", printReq.getStatus().toString());
            data.put("printAuthExpirySecs", ConfigManager.instance()
                    .getConfigInt(Key.PROXY_PRINT_DIRECT_EXPIRY_SECS));

            setApiResultOK(data);

        } else {

            setApiResult(data, API_RESULT_CODE_WARN, "msg-print-auth-pending");
        }

        return data;
    }

    /**
     *
     * @param user
     * @param ranges
     * @param position
     * @return
     * @throws NumberFormatException
     */
    private Map<String, Object> reqInboxPageMove(final String user,
            final String ranges, final String position)
            throws NumberFormatException {

        final Map<String, Object> userData = new HashMap<String, Object>();

        final int nPages =
                INBOX_SERVICE
                        .movePages(user, ranges, Integer.valueOf(position));

        final String msgKey;

        if (nPages == 1) {
            msgKey = "msg-page-move-one";
        } else {
            msgKey = "msg-page-move-multiple";
        }

        return setApiResult(userData, API_RESULT_CODE_OK, msgKey,
                String.valueOf(nPages));
    }

    /**
     *
     * @param user
     * @param jsonScope
     * @return
     * @throws Exception
     */
    private Map<String, Object> reqPagometerReset(final String resetBy,
            final String jsonScope) throws Exception {

        final Map<String, Object> userData = new HashMap<String, Object>();

        final JsonNode list = new ObjectMapper().readTree(jsonScope);

        final Iterator<JsonNode> iter = list.getElements();

        boolean resetDashboard = false;
        boolean resetQueues = false;
        boolean resetPrinters = false;
        boolean resetUsers = false;

        while (iter.hasNext()) {

            final String scope = iter.next().asText();

            switch (scope) {
            case "dashboard":
                resetDashboard = true;
                break;
            case "queues":
                resetQueues = true;
                break;
            case "printers":
                resetPrinters = true;
                break;
            case "users":
                resetUsers = true;
                break;
            default:
                throw new SpException("unknown scope [" + scope + "]");
            }
        }

        resetPagometers(resetBy, resetDashboard, resetQueues, resetPrinters,
                resetUsers);

        setApiResult(userData, API_RESULT_CODE_OK, "msg-pagometer-reset-ok");

        return userData;
    }

    /**
     * @param user
     * @return
     */
    private Map<String, Object> reqDbBackup(final String user) {

        Map<String, Object> userData = new HashMap<String, Object>();

        try {
            SpJobScheduler.instance().scheduleOneShotJob(SpJobType.DB_BACKUP,
                    1L);

            setApiResult(userData, API_RESULT_CODE_OK, "msg-db-backup-busy");

        } catch (Exception e) {
            setApiResult(userData, API_RESULT_CODE_ERROR, "msg-tech-error",
                    e.getMessage());
        }

        return userData;
    }

    /**
     * @param user
     * @return
     */
    private Map<String, Object> reqUserSync(final String user,
            final boolean isTest, final boolean deleteUsers) {

        Map<String, Object> data = new HashMap<String, Object>();

        try {
            SpJobScheduler.instance().scheduleOneShotUserSync(isTest,
                    deleteUsers);

            setApiResult(data, API_RESULT_CODE_OK, "msg-user-sync-busy");

        } catch (Exception e) {
            setApiResult(data, API_RESULT_CODE_ERROR, "msg-tech-error",
                    e.getMessage());
        }

        return data;
    }

    /**
     *
     * @param json
     * @return
     * @throws ParseException
     */
    private Map<String, Object> reqPosDepositQuickSearch(String json)
            throws ParseException {

        final Map<String, Object> data = new HashMap<String, Object>();

        final QuickSearchFilterDto dto =
                AbstractDto.create(QuickSearchFilterDto.class, json);

        final AccountTrxDao accountTrxDao =
                ServiceContext.getDaoContext().getAccountTrxDao();

        final UserAccountDao userAccountDao =
                ServiceContext.getDaoContext().getUserAccountDao();

        final AccountTrxDao.ListFilter filter = new AccountTrxDao.ListFilter();
        filter.setAccountType(AccountTypeEnum.USER);
        filter.setTrxType(AccountTrxTypeEnum.DEPOSIT);

        final List<QuickSearchItemDto> list = new ArrayList<>();

        //
        Date dateFrom = null;
        try {
            dateFrom = QuickSearchDate.toDate(dto.getFilter());
        } catch (ParseException e) {
            dateFrom = null;
        }
        filter.setDateFrom(dateFrom);

        //
        final LocaleHelper localeHelper =
                new LocaleHelper(SpSession.get().getLocale());

        final String currencySymbol = SpSession.getCurrencySymbol();
        final int balanceDecimals = ConfigManager.getUserBalanceDecimals();

        QuickSearchPosPurchaseItemDto itemWlk;

        for (final AccountTrx accountTrx : accountTrxDao.getListChunk(filter,
                0, dto.getMaxResults(), AccountTrxDao.Field.TRX_DATE, false)) {

            final PosPurchase purchase = accountTrx.getPosPurchase();
            final User user =
                    userAccountDao.findByAccountId(
                            accountTrx.getAccount().getId()).getUser();

            itemWlk = new QuickSearchPosPurchaseItemDto();

            itemWlk.setKey(accountTrx.getId());

            itemWlk.setComment(purchase.getComment());
            itemWlk.setPaymentType(purchase.getPaymentType());
            itemWlk.setReceiptNumber(purchase.getReceiptNumber());

            itemWlk.setDateTime(localeHelper.getShortDateTime(accountTrx
                    .getTransactionDate()));

            itemWlk.setTotalCost(localeHelper.getCurrencyDecimal(
                    purchase.getTotalCost(), balanceDecimals, currencySymbol));

            itemWlk.setUserId(user.getUserId());
            itemWlk.setUserEmail(USER_SERVICE.getPrimaryEmailAddress(user));

            list.add(itemWlk);

        }
        //
        data.put("items", list);

        setApiResultOK(data);
        return data;
    }

    /**
     *
     * @param json
     * @return
     */
    private Map<String, Object> reqUserQuickSearch(String json) {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final Map<String, Object> data = new HashMap<String, Object>();

        final QuickSearchFilterDto dto =
                AbstractDto.create(QuickSearchFilterDto.class, json);

        final String currencySymbol = SpSession.getCurrencySymbol();

        final UserDao.ListFilter filter = new UserDao.ListFilter();

        filter.setContainingIdText(dto.getFilter());
        filter.setDeleted(Boolean.FALSE);
        filter.setPerson(Boolean.TRUE);

        final List<QuickSearchItemDto> list = new ArrayList<>();

        QuickSearchUserItemDto itemWlk;

        for (final User user : userDao.getListChunk(filter, 0,
                dto.getMaxResults(), UserDao.Field.USERID, true)) {

            itemWlk = new QuickSearchUserItemDto();

            itemWlk.setKey(user.getId());
            itemWlk.setText(user.getUserId());
            itemWlk.setEmail(USER_SERVICE.getPrimaryEmailAddress(user));

            itemWlk.setBalance(ACCOUNTING_SERVICE.getFormattedUserBalance(
                    user.getUserId(), ServiceContext.getLocale(),
                    currencySymbol));

            list.add(itemWlk);
        }

        data.put("items", list);

        setApiResultOK(data);

        return data;
    }

    /**
     * Sends a test mail message to mailto address.
     *
     * @param requestingUser
     * @param mailto
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    private Map<String, Object> reqMailTest(final String requestingUser,
            final String mailto) {

        Map<String, Object> userData = new HashMap<String, Object>();

        final String subject = localize("mail-test-subject");
        final String body =
                localize("mail-test-body", requestingUser,
                        CommunityDictEnum.SAVAPAGE.getWord() + " "
                                + ConfigManager.getAppVersion());

        try {
            OutputProducer.instance().sendEmail(mailto, null, subject, body,
                    null, null);

            setApiResult(userData, API_RESULT_CODE_OK, "msg-mail-sent", mailto);

        } catch (MessagingException | IOException | InterruptedException
                | CircuitBreakerException e) {

            String msg = e.getMessage();

            if (e.getCause() != null) {
                msg += " (" + e.getCause().getMessage() + ")";
            }
            setApiResult(userData, API_RESULT_CODE_ERROR, "msg-error", msg);
        }

        return userData;
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqImapTest() {

        Map<String, Object> userData = new HashMap<String, Object>();

        try {
            MutableInt nMessagesInbox = new MutableInt();
            MutableInt nMessagesTrash = new MutableInt();

            ImapListener.test(nMessagesInbox, nMessagesTrash);

            setApiResult(userData, API_RESULT_CODE_INFO,
                    "msg-imap-test-passed", nMessagesInbox.toString(),
                    nMessagesTrash.toString());

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = "connection error.";
            }
            setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-imap-test-failed", msg);
        }

        return userData;
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqImapStart() {

        final Map<String, Object> userData = new HashMap<String, Object>();

        if (ConfigManager.isPrintImapEnabled()) {
            SpJobScheduler.instance().scheduleOneShotImapListener(1L);
        }
        return setApiResult(userData, API_RESULT_CODE_OK, "msg-imap-started");
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqImapStop() {

        final Map<String, Object> userData = new HashMap<String, Object>();

        SpJobScheduler.interruptImapListener();

        return setApiResult(userData, API_RESULT_CODE_OK, "msg-imap-stopped");
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqPaperCutTest() {

        final Map<String, Object> userData = new HashMap<String, Object>();

        final ConfigManager cm = ConfigManager.instance();

        String error =
                PaperCutServerProxy.create(
                        cm.getConfigValue(Key.PAPERCUT_SERVER_HOST),
                        cm.getConfigInt(Key.PAPERCUT_SERVER_PORT),
                        cm.getConfigValue(Key.PAPERCUT_XMLRPC_URL_PATH),
                        cm.getConfigValue(Key.PAPERCUT_SERVER_AUTH_TOKEN),
                        false).testConnection();

        if (error == null) {
            error =
                    PaperCutDbProxy.create(
                            cm.getConfigValue(Key.PAPERCUT_DB_JDBC_URL),
                            cm.getConfigValue(Key.PAPERCUT_DB_USER),
                            cm.getConfigValue(Key.PAPERCUT_DB_PASSWORD), false)
                            .testConnection();
        }

        if (error == null) {
            setApiResult(userData, API_RESULT_CODE_INFO,
                    "msg-papercut-test-passed");
        } else {
            setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-papercut-test-failed", error);
        }

        return userData;
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqSmartSchoolTest() {

        final Map<String, Object> userData = new HashMap<String, Object>();

        try {
            final MutableInt nMessagesInbox = new MutableInt();

            SmartSchoolPrintMonitor.testConnection(nMessagesInbox);

            setApiResult(userData, API_RESULT_CODE_INFO,
                    "msg-smartschool-test-passed", nMessagesInbox.toString());

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = "connection error.";
            }
            setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-smartschool-test-failed", msg);
        }

        return userData;
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqSmartSchoolStart(final boolean simulate) {

        final Map<String, Object> userData = new HashMap<String, Object>();

        if (ConfigManager.isSmartSchoolPrintActiveAndEnabled()) {

            if (simulate) {
                SmartSchoolPrintMonitor.resetJobTickerCounter();
            }

            SpJobScheduler.instance().scheduleOneShotSmartSchoolPrintMonitor(
                    simulate, 1L);
        }

        final String msgKey;

        if (simulate) {
            msgKey = "msg-smartschool-started-simulation";
        } else {
            msgKey = "msg-smartschool-started";
        }

        return setApiResult(userData, API_RESULT_CODE_OK, msgKey);
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqSmartSchoolStop() {

        final Map<String, Object> userData = new HashMap<String, Object>();

        SpJobScheduler.interruptSmartSchoolPoller();

        return setApiResult(userData, API_RESULT_CODE_OK,
                "msg-smartschool-stopped");
    }

    /**
     * Checks the password length satisfies
     * {@link Key#INTERNAL_USERS_PW_LENGTH_MIN} .
     *
     * @param userData
     * @param password
     * @return {@code true} is valid.
     */
    private boolean validatePassword(Map<String, Object> userData,
            final String password) {

        final int minPwLength =
                ConfigManager.instance().getConfigInt(
                        Key.INTERNAL_USERS_PW_LENGTH_MIN);

        if (password.length() < minPwLength) {
            setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-password-length-error", String.valueOf(minPwLength));
            return false;
        }

        return true;
    }

    /**
     *
     * @param password
     * @return
     */
    private Map<String, Object> reqJmxPasswordReset(final String password) {

        final Map<String, Object> userData = new HashMap<String, Object>();

        if (!validatePassword(userData, password)) {
            return userData;
        }

        JmxRemoteProperties.setAdminPassword(password);

        setApiResult(userData, API_RESULT_CODE_OK, "msg-password-reset-ok");

        return userData;
    }

    /**
     *
     * @param password
     * @return
     */
    private Map<String, Object> reqAdminPasswordReset(final String password) {

        final Map<String, Object> userData = new HashMap<String, Object>();

        if (!validatePassword(userData, password)) {
            return userData;
        }

        ConfigManager.instance().setInternalAdminPassword(password);
        setApiResult(userData, API_RESULT_CODE_OK, "msg-password-reset-ok");

        return userData;
    }

    /**
     * An Internal User can set its own password. An administrator can set
     * password of every user.
     *
     * @param requestingUser
     * @param iuser
     * @param password
     * @return
     */
    private Map<String, Object> reqUserPasswordReset(
            final String requestingUser, final String iuser,
            final String password) {

        final Map<String, Object> userData = new HashMap<String, Object>();

        /*
         * INVARIANT: internal Admin is NOT a database User.
         */
        if (ConfigManager.isInternalAdmin(iuser)) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-password-reset-not-allowed", iuser);
        }

        if (!validatePassword(userData, password)) {
            return userData;
        }

        if (!requestingUser.equals(iuser)) {
            if (!hasAdminRights(userData)) {
                return userData;
            }
        }

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final User jpaUser = userDao.findActiveUserByUserId(iuser);

        if (!jpaUser.getInternal()) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-password-reset-not-allowed", iuser);
        }

        final String encryptedPw = encryptUserPassword(iuser, password);

        USER_SERVICE.setUserAttrValue(jpaUser, UserAttrEnum.INTERNAL_PASSWORD,
                encryptedPw);

        jpaUser.setModifiedBy(requestingUser);
        jpaUser.setModifiedDate(new Date());

        userDao.update(jpaUser);

        setApiResult(userData, API_RESULT_CODE_OK, "msg-password-reset-ok");

        return userData;
    }

    /**
     * An User can set its own PIN.
     *
     * @param requestingUser
     * @param user
     * @param pin
     * @return
     */
    private Map<String, Object> reqUserPinReset(final String requestingUser,
            final String user, final String pin) {

        final Map<String, Object> userData = new HashMap<String, Object>();

        /*
         * INVARIANT: Internal Admin can NOT have a PIN.
         */
        if (ConfigManager.isInternalAdmin(user)) {
            throw new SpException("No PIN allowed for Internal Admin.");
        }

        if (requestingUser.equals(user)) {
            /*
             * INVARIANT: Users must be allowed to change their PIN.
             */
            if (!ConfigManager.instance()
                    .isConfigValue(Key.USER_CAN_CHANGE_PIN)) {
                throw new SpException(
                        "Users are NOT allowed to change their PIN.");
            }

        } else {
            /*
             * INVARIANT: Only an Administrator can set PIN for another User.
             */
            if (!hasAdminRights(userData)) {
                return userData;
            }
        }

        /*
         * INVARIANT: PIN must be valid.
         */
        if (!validateUserPin(userData, pin)) {
            return userData;
        }

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final User jpaUser = userDao.findActiveUserByUserId(user);

        USER_SERVICE.setUserAttrValue(jpaUser, UserAttrEnum.PIN,
                CryptoUser.encryptUserPin(jpaUser.getId(), pin));

        jpaUser.setModifiedBy(requestingUser);
        jpaUser.setModifiedDate(new Date());

        userDao.update(jpaUser);

        setApiResult(userData, API_RESULT_CODE_OK, "msg-pin-reset-ok");

        return userData;
    }

    /**
     *
     * @param user
     * @param urlPath
     * @return
     */
    private Map<String, Object>
            reqQueueGet(final String userId, final String id) {

        final IppQueueDao ippQueueDao =
                ServiceContext.getDaoContext().getIppQueueDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        final IppQueue queue = ippQueueDao.findById(Long.valueOf(id));

        if (queue == null) {

            setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-queue-not-found", id);

        } else {

            final Map<String, Object> userObj = new HashMap<String, Object>();

            userObj.put("id", queue.getId());
            userObj.put("urlpath", queue.getUrlPath());
            userObj.put("ipallowed", queue.getIpAllowed());
            userObj.put("trusted", queue.getTrusted());
            userObj.put("disabled", queue.getDisabled());
            userObj.put("deleted", queue.getDeleted());

            String reserved = null;
            String uiText;

            final ReservedIppQueueEnum reservedQueue =
                    QUEUE_SERVICE.getReservedQueue(queue.getUrlPath());

            if (reservedQueue != null) {
                reserved = reservedQueue.getUiText();
                uiText = reservedQueue.getUiText();
                if (reservedQueue == ReservedIppQueueEnum.RAW_PRINT) {
                    uiText += " Port " + ConfigManager.getRawPrinterPort();
                }
            } else {
                uiText = ReservedIppQueueEnum.IPP_PRINT.getUiText();
            }

            userObj.put("reserved", reserved);
            userObj.put("uiText", uiText);

            userData.put("j_queue", userObj);

            setApiResultOK(userData);
        }

        return userData;
    }

    /**
     * Edits or creates a Queue.
     * <p>
     * Also, a logical delete can be applied or reversed.
     * </p>
     *
     * @param requestingUser
     * @param jsonQueue
     * @return
     */
    private Map<String, Object> reqQueueSet(final String requestingUser,
            final String jsonQueue) {

        final IppQueueDao ippQueueDao =
                ServiceContext.getDaoContext().getIppQueueDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("jsonQueue: " + jsonQueue);
        }

        final JsonNode list;

        try {
            list = new ObjectMapper().readTree(jsonQueue);
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        final JsonNode id = list.get("id");
        final boolean isNew = id.isNull();
        final Date now = new Date();
        final String urlPath = list.get("urlpath").getTextValue().trim();

        /*
         * Note: returns null when logically deleted!!
         */
        final IppQueue jpaQueueDuplicate = ippQueueDao.findByUrlPath(urlPath);

        IppQueue jpaQueue = null;

        boolean isDuplicate = true;

        if (isNew) {

            if (jpaQueueDuplicate == null) {

                jpaQueue = new IppQueue();
                jpaQueue.setCreatedBy(requestingUser);
                jpaQueue.setCreatedDate(now);

                isDuplicate = false;
            }

        } else {

            jpaQueue = ippQueueDao.findById(id.asLong());

            if (jpaQueueDuplicate == null
                    || jpaQueueDuplicate.getId().equals(jpaQueue.getId())) {

                jpaQueue.setModifiedBy(requestingUser);
                jpaQueue.setModifiedDate(now);

                isDuplicate = false;
            }
        }

        if (isDuplicate) {

            setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-queue-duplicate-path", urlPath);

        } else {

            jpaQueue.setUrlPath(urlPath);
            jpaQueue.setIpAllowed(list.get("ipallowed").getTextValue());
            jpaQueue.setTrusted(list.get("trusted").getBooleanValue());
            jpaQueue.setDisabled(list.get("disabled").getBooleanValue());

            if (isNew) {

                if (QUEUE_SERVICE.isReservedQueue(urlPath)) {

                    setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-queue-reserved-path", urlPath);

                } else {

                    ippQueueDao.create(jpaQueue);

                    setApiResult(userData, API_RESULT_CODE_OK,
                            "msg-queue-created-ok");
                }

            } else {

                final boolean isDeleted = list.get("deleted").getBooleanValue();

                if (jpaQueue.getDeleted() != isDeleted) {

                    if (isDeleted) {
                        QUEUE_SERVICE.setLogicalDeleted(jpaQueue, now,
                                requestingUser);
                    } else {
                        QUEUE_SERVICE.undoLogicalDeleted(jpaQueue);
                    }
                }

                ippQueueDao.update(jpaQueue);

                setApiResult(userData, API_RESULT_CODE_OK, "msg-queue-saved-ok");
            }
        }
        return userData;
    }

    /**
     *
     * @param requestingUser
     * @param userid
     * @param id
     * @return
     */
    private Map<String, Object> reqDeviceDelete(final String requestingUser,
            final String id, final String deviceName) {

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        final Device device = deviceDao.findById(Long.valueOf(id));

        if (device == null) {
            throw new SpException("Device [" + deviceName + "] is not found");
        }

        if (device.getCardReaderTerminal() != null) {
            return setApiResult(userData, API_RESULT_CODE_INFO,
                    "msg-device-delete-reader-in-use", device.getDisplayName(),
                    device.getCardReaderTerminal().getDisplayName());
        }

        deviceDao.delete(device);

        return setApiResult(userData, API_RESULT_CODE_OK,
                "msg-device-deleted-ok");
    }

    /**
     *
     * @param card
     * @return
     */
    private Map<String, Object> reqCardIsRegistered(final String cardNumber) {

        final Map<String, Object> userData = new HashMap<String, Object>();

        userData.put("registered",
                Boolean.valueOf(USER_SERVICE.isCardRegistered(cardNumber)));

        setApiResultOK(userData);

        return setApiResultOK(userData);
    }

    /**
     *
     * @param user
     * @param id
     * @return
     */
    private Map<String, Object> reqDeviceGet(final String userId,
            final String id) {

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        final Device device = deviceDao.findById(Long.valueOf(id));

        if (device == null) {
            setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-device-not-found", id);
        } else {
            Map<String, Object> userObj = new HashMap<String, Object>();

            userObj.put("id", device.getId());
            userObj.put("deviceType", device.getDeviceType());
            userObj.put("deviceName", device.getDeviceName());
            userObj.put("displayName", device.getDisplayName());
            userObj.put("location", device.getLocation());
            userObj.put("notes", device.getNotes());
            userObj.put("hostname", device.getHostname());
            userObj.put("port", device.getPort());
            userObj.put("disabled", device.getDisabled());

            if (device.getPrinter() != null) {
                userObj.put("printerName", device.getPrinter().getPrinterName());
            }

            if (device.getPrinterGroup() != null) {
                userObj.put("printerGroup", device.getPrinterGroup()
                        .getDisplayName());
            }

            if (device.getCardReader() != null) {
                userObj.put("readerName", device.getCardReader()
                        .getDeviceName());
            }

            if (device.getCardReaderTerminal() != null) {
                userObj.put("terminalName", device.getCardReaderTerminal()
                        .getDeviceName());
            }

            /*
             *
             */
            if (device.getAttributes() != null) {

                Map<String, Object> attrMap = new HashMap<String, Object>();

                for (DeviceAttr attr : device.getAttributes()) {

                    final String value = attr.getValue();

                    if (value.equals(IConfigProp.V_YES)
                            || value.equals(IConfigProp.V_NO)) {

                        attrMap.put(attr.getName(), Boolean.valueOf(value
                                .equals(IConfigProp.V_YES)));
                    } else {
                        attrMap.put(attr.getName(), attr.getValue());
                    }

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("get [" + attr.getName() + "] : ["
                                + attrMap.get(attr.getName()) + "]");
                    }
                }

                userObj.put("attr", attrMap);
            }

            /*
             *
             */
            userData.put("j_device", userObj);
            setApiResultOK(userData);
        }
        return userData;
    }

    /**
     * Edits or creates a Device.
     *
     * @param requestingUser
     * @param jsonDevice
     * @return
     */
    private Map<String, Object> reqDeviceSet(final String requestingUser,
            final String jsonDevice) {

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        final JsonNode list;

        try {
            list = new ObjectMapper().readTree(jsonDevice);
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        final JsonNode id = list.get("id");
        final boolean isNew = id.isNull();
        final Date now = new Date();
        final String deviceName = list.get("deviceName").getTextValue();

        final PrinterGroupDao printerGroupDao =
                ServiceContext.getDaoContext().getPrinterGroupDao();

        /*
         * Note: returns null when not found.
         */
        final Device jpaDeviceDuplicate = deviceDao.findByName(deviceName);

        Device jpaDevice = null;

        boolean isDuplicate = true;

        if (isNew) {

            if (jpaDeviceDuplicate == null) {

                jpaDevice = new Device();
                jpaDevice.setCreatedBy(requestingUser);
                jpaDevice.setCreatedDate(now);

                isDuplicate = false;
            }

        } else {

            jpaDevice = deviceDao.findById(id.asLong());

            if (jpaDeviceDuplicate == null
                    || jpaDeviceDuplicate.getId().equals(jpaDevice.getId())) {

                jpaDevice.setModifiedBy(requestingUser);
                jpaDevice.setModifiedDate(now);

                isDuplicate = false;
            }
        }

        if (isDuplicate) {

            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-device-duplicate-name", deviceName);

        }

        jpaDevice.setDeviceType(list.get("deviceType").getTextValue());
        jpaDevice.setDeviceName(deviceName);
        jpaDevice.setDisplayName(list.get("displayName").getTextValue());
        jpaDevice.setLocation(list.get("location").getTextValue());
        jpaDevice.setNotes(list.get("notes").getTextValue());
        jpaDevice.setHostname(list.get("hostname").getTextValue());
        jpaDevice.setDisabled(list.get("disabled").getBooleanValue());

        Integer iPort = null;

        if (deviceDao.isCardReader(jpaDevice)) {
            JsonNode jsonPort = list.get("port");
            if (jsonPort == null || jsonPort.getIntValue() <= 0) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-device-port-error");
            }
            iPort = jsonPort.getIntValue();
        }
        jpaDevice.setPort(iPort);

        /*
         * Get JSON Attributes
         */
        final JsonNode jsonAttributes = list.get("attr");

        /*
         * Reference to Card Reader.
         */
        Device jpaCardReader = null;

        final JsonNode jsonCardReaderName = list.get("readerName");
        String cardReaderName = null;

        if (jsonCardReaderName != null) {
            cardReaderName = jsonCardReaderName.getTextValue();
        }

        final JsonNode jsonAuthModeCardIp;

        if (jsonAttributes == null) {
            jsonAuthModeCardIp = null;
        } else {
            jsonAuthModeCardIp =
                    jsonAttributes.findValue(DeviceAttrEnum.AUTH_MODE_CARD_IP
                            .getDbName());
        }

        final boolean linkCardReader =
                jsonAuthModeCardIp != null
                        && jsonAuthModeCardIp.getBooleanValue();

        if (linkCardReader) {

            /*
             * INVARIANT: Card Reader must be specified.
             */
            if (StringUtils.isBlank(cardReaderName)) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-device-card-reader-missing");
            }

            jpaCardReader = deviceDao.findByName(cardReaderName);

            /*
             * INVARIANT: Card Reader must exist.
             */
            if (jpaCardReader == null) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-device-card-reader-not-found", cardReaderName);
            }

            /*
             * INVARIANT: Card Reader must not already be a Login Authenticator.
             */
            Device jpaCardReaderTerminal =
                    jpaCardReader.getCardReaderTerminal();

            if (jpaCardReaderTerminal != null) {
                if (isNew
                        || !jpaDevice.getId().equals(
                                jpaCardReaderTerminal.getId())) {
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-device-card-reader-reserved-for-terminal",
                            cardReaderName,
                            jpaCardReaderTerminal.getDisplayName());
                }
            }

            /*
             * INVARIANT: Card Reader must not already be a Proxy Print
             * Authenticator.
             */
            if (jpaCardReader.getPrinter() != null) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-device-card-reader-reserved-for-printer",
                        cardReaderName, jpaCardReader.getPrinter()
                                .getPrinterName());
            }
            if (jpaCardReader.getPrinterGroup() != null) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-device-card-reader-reserved-for-printer-group",
                        cardReaderName, jpaCardReader.getPrinterGroup()
                                .getDisplayName());
            }

        }

        jpaDevice.setCardReader(jpaCardReader);

        /*
         * Screening and determination of attribute values.
         */
        ProxyPrintAuthModeEnum printAuthMode = null;

        if (jsonAttributes != null) {

            final Iterator<Entry<String, JsonNode>> iter =
                    jsonAttributes.getFields();

            while (iter.hasNext()) {

                final Entry<String, JsonNode> entry = iter.next();

                final String attrKey = entry.getKey();
                final String attrValue = entry.getValue().getTextValue();

                /*
                 * INVARIANT: USER_MAX_IDLE_SECS must be numeric.
                 */
                if (attrKey.equals(DeviceAttrEnum.WEBAPP_USER_MAX_IDLE_SECS
                        .getDbName()) && !StringUtils.isNumeric(attrValue)) {

                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-device-idle-seconds-error");
                }

                /*
                 * PROXY_PRINT_AUTH_MODE
                 */
                if (attrKey.equals(DeviceAttrEnum.PROXY_PRINT_AUTH_MODE
                        .getDbName())) {
                    printAuthMode = ProxyPrintAuthModeEnum.valueOf(attrValue);
                }

            }
        }

        /*
         * Authenticated proxy print for single printer or printer group.
         */
        final PrinterGroup jpaPrinterGroup;
        final Printer jpaPrinter;

        if (printAuthMode == null) {
            /*
             * No authenticated proxy print applicable.
             */
            jpaPrinter = null;
            jpaPrinterGroup = null;

        } else {

            final JsonNode jsonPrinterName = list.get("printerName");
            final JsonNode jsonPrinterGroup = list.get("printerGroup");

            final String printerName;
            final String printerGroup;

            if (jsonPrinterName == null
                    || StringUtils.isBlank(jsonPrinterName.getTextValue())) {
                printerName = null;
            } else {
                printerName = jsonPrinterName.getTextValue();
            }

            if (jsonPrinterGroup == null
                    || StringUtils.isBlank(jsonPrinterGroup.getTextValue())) {
                printerGroup = null;
            } else {
                printerGroup = jsonPrinterGroup.getTextValue();
            }

            if (printerGroup == null && printerName == null) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-device-proxy-printer-or-group-needed");
            }

            if (printAuthMode == ProxyPrintAuthModeEnum.FAST) {

                /*
                 * INVARIANT: FAST proxy print MUST target a SINGLE printer. A
                 * printer group is irrelevant for FAST proxy print.
                 */
                if (printerName == null || printerGroup != null) {
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-device-single-proxy-printer-needed");
                }

            } else if (printAuthMode == ProxyPrintAuthModeEnum.FAST_DIRECT
                    || printAuthMode == ProxyPrintAuthModeEnum.FAST_HOLD) {

                /*
                 * INVARIANT: FAST proxy print MUST target a SINGLE printer.
                 */
                if (printerGroup != null && printerName == null) {
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-device-single-proxy-printer-needed");
                }
            } else if (printerGroup != null && printerName != null) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-device-proxy-printer-or-group-needed");
            }

            /*
             * Single Printer.
             */
            if (printerName == null) {

                jpaPrinter = null;

            } else {

                Printer jpaPrinterWlk = jpaDevice.getPrinter();

                if (jpaPrinterWlk == null
                        || !jpaPrinterWlk.getPrinterName().equals(printerName)) {

                    final PrinterDao printerDao =
                            ServiceContext.getDaoContext().getPrinterDao();

                    jpaPrinterWlk = printerDao.findByName(printerName);

                    if (jpaPrinterWlk == null) {
                        return setApiResult(userData, API_RESULT_CODE_ERROR,
                                "msg-device-printer-not-found", printerName);
                    }
                }

                jpaPrinter = jpaPrinterWlk;
            }

            /*
             * Printer Group.
             */
            if (printerGroup == null) {

                jpaPrinterGroup = null;

            } else {

                PrinterGroup jpaPrinterGroupWlk = jpaDevice.getPrinterGroup();

                if (jpaPrinterGroupWlk == null
                        || !jpaPrinterGroupWlk.getGroupName().equals(
                                printerGroup)) {

                    jpaPrinterGroupWlk =
                            printerGroupDao.findByName(printerGroup);

                    if (jpaPrinterGroupWlk == null) {
                        return setApiResult(userData, API_RESULT_CODE_ERROR,
                                "msg-device-printer-group-not-found",
                                printerGroup);
                    }
                }
                jpaPrinterGroup = jpaPrinterGroupWlk;
            }
        }

        jpaDevice.setPrinter(jpaPrinter);
        jpaDevice.setPrinterGroup(jpaPrinterGroup);

        /*
         * Device: Persist | Merge
         */
        String resultMsgKey;

        if (isNew) {
            deviceDao.create(jpaDevice);
            resultMsgKey = "msg-device-created-ok";

        } else {
            deviceDao.update(jpaDevice);
            resultMsgKey = "msg-device-saved-ok";
        }

        /*
         * Write Attributes.
         */
        if (jsonAttributes != null) {

            final Iterator<Entry<String, JsonNode>> iter =
                    jsonAttributes.getFields();

            while (iter.hasNext()) {

                final Entry<String, JsonNode> entry = iter.next();

                final String value;

                if (entry.getValue().isBoolean()) {

                    if (entry.getValue().asBoolean()) {
                        value = IConfigProp.V_YES;
                    } else {
                        value = IConfigProp.V_NO;
                    }
                } else {
                    value = entry.getValue().asText();
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("write [" + entry.getKey() + "] : [" + value
                            + "]");
                }

                deviceDao.writeAttribute(jpaDevice, entry.getKey(), value);
            }

        }

        /*
         *
         */
        setApiResult(userData, API_RESULT_CODE_OK, resultMsgKey);
        return userData;
    }

    /**
     *
     * @param user
     * @param urlPath
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqPrinterGet(final String userId,
            final String id) throws IOException {

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        final Printer printer = printerDao.findById(Long.valueOf(id));

        if (printer == null) {
            setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-printer-not-found", id);
        } else {
            final ProxyPrinterDto dto =
                    PROXY_PRINT_SERVICE.getProxyPrinterDto(printer);
            userData.put("j_printer", dto.asMap());
            setApiResultOK(userData);
        }
        return userData;
    }

    /**
     *
     * @param requestingUser
     * @param jsonRename
     * @return
     * @throws Exception
     */
    private Map<String, Object> reqPrinterRename(final String requestingUser,
            final String jsonRename) throws Exception {

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        final JsonNode list;

        try {
            list = new ObjectMapper().readTree(jsonRename);
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        final long id = list.get("id").getLongValue();
        final String newName = list.get("name").getTextValue();
        final Boolean replaceDuplicate = list.get("replace").getBooleanValue();

        if (StringUtils.isBlank(newName)) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-printer-rename-new-name-missing");
        }

        /*
         * Read printer.
         */
        final Printer printer = printerDao.findById(id);
        final String oldName = printer.getPrinterName();

        /*
         * Read duplicate and check PrintOut documents.
         */
        final Printer printerDuplicate = printerDao.findByName(newName);

        if (printerDuplicate != null) {

            if (!replaceDuplicate
                    && printerDao.countPrintOuts(printerDuplicate.getId()) > 0) {

                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-printer-rename-duplicate", oldName, newName);
            }

            printerDao.delete(printerDuplicate);

            /*
             * Flushing isn't committing, flushing is only issuing the pending
             * changes to the database so that it is visible for the current
             * transaction.
             *
             * NOTE: this is needed because the printerName is unique. If we did
             * not flush a violation constraint would happens because we update
             * the printer with the printerName of the removed printer.
             *
             * TODO: 2013-10-02: is this really true?
             *
             * http://www.developerfusion.com/article/84945/flush-and-clear-or-
             * mapping-antipatterns/
             *
             * "A manual call of flush() should be prevented by a clear design
             * of the application and is similar to a manual call of System.gc()
             * which requests a manual garbage collection. In both cases, a
             * normal, optimized operation of the technologies is prevented."
             */
            DaoContextImpl.peekEntityManager().flush();
        }

        /*
         * Read printer and rename.
         */
        printer.setPrinterName(newName);
        printerDao.update(printer);

        /*
         * Re-initialize the CUPS printer cache.
         */
        PROXY_PRINT_SERVICE.initPrinterCache();

        /*
         * Feedback message.
         */
        String msgKey = null;

        if (printerDuplicate == null) {
            msgKey = "msg-printer-renamed-ok";
        } else {
            msgKey = "msg-printer-renamed-delete-ok";
        }

        return setApiResult(userData, API_RESULT_CODE_OK, msgKey, oldName,
                newName);
    }

    /**
     * Sets the Proxy Printer (basic) properties.
     * <p>
     * Also, a logical delete can be applied or reversed.
     * </p>
     *
     * @param jsonPrinter
     * @return
     */
    private Map<String, Object> reqPrinterSet(final String jsonPrinter) {

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        final ProxyPrinterDto dto =
                JsonAbstractBase.create(ProxyPrinterDto.class, jsonPrinter);

        final long id = dto.getId();

        final Printer jpaPrinter = printerDao.findById(id);

        /*
         * INVARIANT: printer MUST exist.
         */
        if (jpaPrinter == null) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-printer-not-found", String.valueOf(id));
        }

        PROXY_PRINT_SERVICE.setProxyPrinterProps(jpaPrinter, dto);

        setApiResult(userData, API_RESULT_CODE_OK, "msg-printer-saved-ok");

        return userData;
    }

    /**
     * Sets the Proxy Printer media cost properties.
     * <p>
     * Also, a logical delete can be applied or reversed.
     * </p>
     *
     * @param jsonCost
     * @return
     */
    private Map<String, Object> reqPrinterSetMediaCost(final String jsonCost) {

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        final ProxyPrinterCostDto dto =
                JsonAbstractBase.create(ProxyPrinterCostDto.class, jsonCost);

        final long id = dto.getId();

        final Printer jpaPrinter = printerDao.findById(id);

        /*
         * INVARIANT: printer MUST exist.
         */
        if (jpaPrinter == null) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-printer-not-found", String.valueOf(id));
        }

        /*
         *
         */
        final AbstractJsonRpcMethodResponse rpcResponse =
                PROXY_PRINT_SERVICE.setProxyPrinterCostMedia(jpaPrinter, dto);

        if (rpcResponse.isResult()) {
            setApiResult(userData, API_RESULT_CODE_OK, "msg-printer-saved-ok");
        } else {
            setApiResultMsgError(userData, "", rpcResponse.asError().getError()
                    .data(ErrorDataBasic.class).getReason());
        }

        return userData;
    }

    /**
     * Sets the Proxy Printer media cost properties.
     * <p>
     * Also, a logical delete can be applied or reversed.
     * </p>
     *
     * @param json
     * @return
     */
    private Map<String, Object> reqPrinterSetMediaSources(final String json) {

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        final ProxyPrinterMediaSourcesDto dto =
                JsonAbstractBase
                        .create(ProxyPrinterMediaSourcesDto.class, json);

        final long id = dto.getId();

        final Printer jpaPrinter = printerDao.findById(id);

        /*
         * INVARIANT: printer MUST exist.
         */
        if (jpaPrinter == null) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-printer-not-found", String.valueOf(id));
        }

        /*
         *
         */
        final AbstractJsonRpcMethodResponse rpcResponse =
                PROXY_PRINT_SERVICE.setProxyPrinterCostMediaSources(jpaPrinter,
                        dto);

        if (rpcResponse.isResult()) {
            setApiResult(userData, API_RESULT_CODE_OK, "msg-printer-saved-ok");
        } else {
            setApiResultMsgError(userData, "", rpcResponse.asError().getError()
                    .data(ErrorDataBasic.class).getReason());
        }

        return userData;
    }

    /**
     *
     * @param user
     * @return
     * @throws Exception
     */
    private Map<String, Object> reqPrinterSync(final String user)
            throws Exception {

        final Map<String, Object> data = new HashMap<String, Object>();

        if (!PROXY_PRINT_SERVICE.isConnectedToCups()) {
            return setApiResult(data, API_RESULT_CODE_ERROR,
                    "msg-printer-connection-broken");
        }

        /*
         * Re-initialize the CUPS printer cache.
         */
        PROXY_PRINT_SERVICE.initPrinterCache();

        return setApiResult(data, API_RESULT_CODE_OK, "msg-printer-sync-ok");
    }

    /**
     *
     * @param user
     * @param userSubject
     * @return
     * @throws ParseException
     */
    private Map<String, Object> reqConfigGetProp(final String name)
            throws ParseException {

        final ConfigPropertyDao dao =
                ServiceContext.getDaoContext().getConfigPropertyDao();

        final ConfigManager cm = ConfigManager.instance();

        Map<String, Object> userData = new HashMap<String, Object>();

        /*
         * INVARIANT: property MUST exist in database.
         */
        final ConfigProperty prop = dao.findByName(name);

        if (prop == null) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-config-prop-not-found", name);
        }

        /*
         * INVARIANT: property MUST exist in cache.
         */
        final Key key = cm.getConfigKey(name);
        if (key == null) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-config-prop-not-found", name);
        }

        /*
         * If display of this value is Locale sensitive, we MUST revert to
         * locale format.
         */
        String value = prop.getValue();

        if (cm.isConfigBigDecimal(key)) {
            value =
                    BigDecimalUtil.localize(BigDecimalUtil.valueOf(value),
                            getSession().getLocale(), true);
        }

        Map<String, Object> obj = new HashMap<String, Object>();

        obj.put("name", name);
        obj.put("value", value);
        obj.put("multiline", cm.isConfigMultiline(key));

        /*
         *
         */
        userData.put("j_prop", obj);
        setApiResultOK(userData);

        return userData;
    }

    /**
     *
     * @param user
     * @param userSubject
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqUserGet(final String userId,
            final String userSubject) throws IOException {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        final User user = userDao.findActiveUserByUserId(userSubject);

        if (user == null) {

            setApiResult(userData, API_RESULT_CODE_ERROR, "msg-user-not-found",
                    userId);

        } else {

            final UserDto dto = USER_SERVICE.createUserDto(user);
            userData.put("userDto", dto.asMap());
            setApiResultOK(userData);
        }
        return userData;
    }

    /**
     *
     * @param user
     * @param userSubject
     * @return
     */
    private Map<String, Object> reqUserGetStats(final String userId) {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        final User user = userDao.findActiveUserByUserId(userId);

        if (user == null) {
            setApiResult(userData, API_RESULT_CODE_ERROR, "msg-user-not-found",
                    userId);
        } else {
            addUserStats(userData, user, this.getSession().getLocale(),
                    SpSession.getCurrencySymbol());
            setApiResultOK(userData);
        }
        return userData;
    }

    /**
     * Encrypts the user password.
     *
     * @param userid
     * @param password
     * @return
     */
    private String encryptUserPassword(final String userid,
            final String password) {
        return CryptoUser.getHashedUserPassword(userid, password);
    }

    /**
     * Check if User PIN is valid.
     *
     * @param userData
     *            Filled with the proper error code and message when not valid.
     * @param pin
     * @return {@code true} if valid.
     */
    private boolean validateUserPin(final Map<String, Object> userData,
            final String pin) {

        boolean isValid = false;

        final int lengthMin =
                ConfigManager.instance().getConfigInt(Key.USER_PIN_LENGTH_MIN);

        final int lengthMax =
                ConfigManager.instance().getConfigInt(Key.USER_PIN_LENGTH_MAX);

        if (!StringUtils.isNumeric(pin)) {

            setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-user-pin-not-numeric");

        } else if (pin.length() < lengthMin
                || (lengthMax > 0 && pin.length() > lengthMax)) {

            if (lengthMin == lengthMax) {
                setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-user-pin-length-error", String.valueOf(lengthMin));
            } else if (lengthMax == 0) {
                setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-user-pin-length-error-min",
                        String.valueOf(lengthMin));
            } else {
                setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-user-pin-length-error-min-max",
                        String.valueOf(lengthMin), String.valueOf(lengthMax));
            }

        } else {
            isValid = true;
        }

        return isValid;
    }

    /**
     *
     * @param jsonDto
     * @return
     * @throws IOException
     * @throws MessagingException
     * @throws JRException
     */
    private Map<String, Object> reqPosDeposit(final String requestingUser,
            final String jsonDto) throws Exception {

        Map<String, Object> userData = new HashMap<String, Object>();

        final PosDepositDto dto =
                JsonAbstractBase.create(PosDepositDto.class, jsonDto);

        AbstractJsonRpcMethodResponse rpcResponse =
                ACCOUNTING_SERVICE.depositFunds(dto);

        if (rpcResponse.isResult()) {

            switch (dto.getReceiptDelivery()) {
            case EMAIL:
                final ResultPosDeposit data =
                        rpcResponse.asResult().getResult()
                                .data(ResultPosDeposit.class);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("AccountTrxDbId [" + data.getAccountTrxDbId()
                            + "]");
                }
                /*
                 * Commit the result before sending the email.
                 */
                ServiceContext.getDaoContext().commit();

                setApiResult(userData, API_RESULT_CODE_OK,
                        "msg-deposit-funds-receipt-email-ok",
                        dto.getUserEmail());

                mailDepositReceipt(data.getAccountTrxDbId(),
                        dto.getUserEmail(), requestingUser, false);

                break;

            case NONE:
                // no break intended
            default:
                setApiResult(userData, API_RESULT_CODE_OK,
                        "msg-deposit-funds-ok");
                break;
            }

        } else {
            setApiResultMsgError(userData, "", rpcResponse.asError().getError()
                    .data(ErrorDataBasic.class).getReason());
        }

        return userData;
    }

    /**
     *
     * @param json
     * @return
     * @throws ParseException
     * @throws IOException
     * @throws MessagingException
     * @throws JRException
     */
    private Map<String, Object> reqPosReceiptSendMail(
            final String requestingUser, String json) throws Exception {

        Map<String, Object> data = new HashMap<String, Object>();

        final AccountTrxDao accountTrxDao =
                ServiceContext.getDaoContext().getAccountTrxDao();

        final PrimaryKeyDto dto = AbstractDto.create(PrimaryKeyDto.class, json);
        final AccountTrx accountTrx = accountTrxDao.findById(dto.getKey());

        final UserAccountDao userAccountDao =
                ServiceContext.getDaoContext().getUserAccountDao();

        final User user =
                userAccountDao.findByAccountId(accountTrx.getAccount().getId())
                        .getUser();

        final String email = USER_SERVICE.getPrimaryEmailAddress(user);

        mailDepositReceipt(dto.getKey(), email, requestingUser, false);

        setApiResult(data, API_RESULT_CODE_OK, "msg-pos-receipt-sendmail-ok",
                email);

        return data;
    }

    /**
     *
     * @param jsonDto
     * @return
     * @throws JRException
     */
    private Map<String, Object> reqVoucherBatchCreate(final String jsonDto)
            throws JRException {

        Map<String, Object> userData = new HashMap<String, Object>();

        final AccountVoucherBatchDto dto =
                JsonAbstractBase.create(AccountVoucherBatchDto.class, jsonDto);

        final AbstractJsonRpcMethodResponse rpcResponse =
                ACCOUNT_VOUCHER_SERVICE.createBatch(dto);

        if (rpcResponse.isResult()) {
            setApiResult(userData, API_RESULT_CODE_OK,
                    "msg-voucher-batch-created-ok", dto.getNumber().toString(),
                    dto.getBatchId());
        } else {
            setApiResultMsgError(userData, "", rpcResponse.asError().getError()
                    .data(ErrorDataBasic.class).getReason());
        }

        return userData;

    }

    /**
     *
     * @param batch
     * @return
     */
    private Map<String, Object> reqVoucherBatchDelete(final String batch) {
        Map<String, Object> userData = new HashMap<String, Object>();

        final Integer nDeleted =
                ServiceContext.getDaoContext().getAccountVoucherDao()
                        .deleteBatch(batch);

        if (nDeleted == 0) {
            setApiResult(userData, API_RESULT_CODE_OK,
                    "msg-voucher-batch-deleted-zero", batch);
        } else if (nDeleted == 1) {
            setApiResult(userData, API_RESULT_CODE_OK,
                    "msg-voucher-batch-deleted-one", batch);
        } else {
            setApiResult(userData, API_RESULT_CODE_OK,
                    "msg-voucher-batch-deleted-many", nDeleted.toString(),
                    batch);
        }
        return userData;
    }

    /**
     *
     * @param batch
     * @return
     */
    private Map<String, Object> reqVoucherBatchExpire(final String batch) {
        Map<String, Object> userData = new HashMap<String, Object>();

        final Date expiryToday =
                DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

        final Integer nExpired =
                ServiceContext.getDaoContext().getAccountVoucherDao()
                        .expireBatch(batch, expiryToday);

        if (nExpired == 0) {
            setApiResult(userData, API_RESULT_CODE_OK,
                    "msg-voucher-batch-expired-zero", batch);
        } else if (nExpired == 1) {
            setApiResult(userData, API_RESULT_CODE_OK,
                    "msg-voucher-batch-expired-one", batch);
        } else {
            setApiResult(userData, API_RESULT_CODE_OK,
                    "msg-voucher-batch-expired-many", nExpired.toString(),
                    batch);
        }
        return userData;
    }

    /**
     *
     * @param number
     * @return
     */
    private Map<String, Object> reqVoucherRedeem(final String requestingUser,
            final String cardNumber) {

        Map<String, Object> userData = new HashMap<String, Object>();

        /*
         * A blank card number is not considered a trial.
         */
        if (StringUtils.isBlank(cardNumber)) {
            return setApiResult(userData, API_RESULT_CODE_WARN,
                    "msg-voucher-redeem-void");
        }

        final AccountVoucherRedeemDto dto = new AccountVoucherRedeemDto();

        dto.setCardNumber(cardNumber);
        dto.setRedeemDate(new Date().getTime());
        dto.setUserId(requestingUser);

        final AbstractJsonRpcMethodResponse rpcResponse =
                ACCOUNTING_SERVICE.redeemVoucher(dto);

        if (rpcResponse.isResult()) {

            setApiResult(userData, API_RESULT_CODE_OK, "msg-voucher-redeem-ok");

        } else {

            final JsonRpcError error = rpcResponse.asError().getError();
            final ErrorDataBasic errorData = error.data(ErrorDataBasic.class);

            setApiResultMsgError(userData, error.getMessage(),
                    errorData.getReason());

            if (error.getMessage().equals(
                    AccountingService.MSG_KEY_VOUCHER_REDEEM_NUMBER_INVALID)) {

                final String msg =
                        AppLogHelper.logWarning(getClass(),
                                "msg-voucher-redeem-invalid", requestingUser,
                                cardNumber);

                AdminPublisher.instance().publish(PubTopicEnum.USER,
                        PubLevelEnum.WARN, msg);
            }
        }

        return userData;
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqVoucherDeleteExpired() {

        Map<String, Object> userData = new HashMap<String, Object>();

        final Date expiryToday =
                DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

        final Integer nDeleted =
                ServiceContext.getDaoContext().getAccountVoucherDao()
                        .deleteExpired(expiryToday);

        if (nDeleted == 0) {
            setApiResult(userData, API_RESULT_CODE_OK,
                    "msg-voucher-deleted-expired-zero");
        } else if (nDeleted == 1) {
            setApiResult(userData, API_RESULT_CODE_OK,
                    "msg-voucher-deleted-expired-one");
        } else {
            setApiResult(userData, API_RESULT_CODE_OK,
                    "msg-voucher-deleted-expired-many", nDeleted.toString());
        }
        return userData;
    }

    /**
     *
     * @param userId
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqUserNotifyAccountChange(
            final String primaryKeyDto) throws IOException {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final PrimaryKeyDto dto =
                AbstractDto.create(PrimaryKeyDto.class, primaryKeyDto);

        final User user = userDao.findById(dto.getKey());

        if (UserMsgIndicator.isSafePagesDirPresent(user.getUserId())) {
            UserMsgIndicator.write(user.getUserId(),
                    ServiceContext.getTransactionDate(),
                    UserMsgIndicator.Msg.ACCOUNT_INFO, null);
        }
        return createApiResultOK();
    }

    /**
     * Edits or creates a User.
     * <p>
     * Delete is not handled here, see
     * {@link #reqUserDelete(EntityManager, String, String, String)}.
     * </p>
     *
     * @param user
     * @param jsonUser
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqUserSet(final String jsonUser)
            throws IOException {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final Map<String, Object> userData = new HashMap<String, Object>();

        final UserDto userDto =
                JsonAbstractBase.create(UserDto.class, jsonUser);

        final boolean isNew = userDto.getDatabaseId() == null;

        AbstractJsonRpcMethodResponse rpcResponse =
                USER_SERVICE.setUser(userDto, isNew);

        if (rpcResponse.isResult()) {
            String msgKeyOk;

            if (isNew) {
                msgKeyOk = "msg-user-created-ok";
            } else {
                msgKeyOk = "msg-user-saved-ok";
            }

            setApiResult(userData, API_RESULT_CODE_OK, msgKeyOk);

        } else {
            setApiResultMsgError(userData, "", rpcResponse.asError().getError()
                    .data(ErrorDataBasic.class).getReason());
        }

        return userData;
    }

    /**
     *
     * @param requestingUser
     * @param userid
     * @param id
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqUserDelete(final String id,
            final String userid) throws IOException {

        final Map<String, Object> userData = new HashMap<String, Object>();

        final AbstractJsonRpcMethodResponse rpcResponse =
                USER_SERVICE.deleteUser(userid);

        if (rpcResponse.isResult()) {
            setApiResultMsgOK(userData, null, rpcResponse.asResult()
                    .getResult().data(ResultDataBasic.class).getMessage());
        } else {
            setApiResultMsgError(userData, "", rpcResponse.asError().getError()
                    .data(ErrorDataBasic.class).getReason());
        }
        return userData;
    }

    /**
     *
     * @param user
     * @return
     */
    private Map<String, Object> reqUserSourceGroups(final String user) {

        Map<String, Object> userData = new HashMap<String, Object>();

        userData.put(
                "groups",
                Arrays.asList(ConfigManager.instance().getUserSource()
                        .getGroups().toArray()));
        return setApiResultOK(userData);
    }

    /**
     * Gets the {@link Device.DeviceTypeEnum#TERMINAL} definition of the remote
     * client.
     *
     * @return {@code null} when no device definition is found.
     */
    private Device getHostTerminal() {

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        return deviceDao.findByHostDeviceType(getRemoteAddr(),
                DeviceTypeEnum.TERMINAL);
    }

    /**
     * Handles a new login request for both the User and Admin WebApp.
     * <p>
     * When an assocCardNumber (not null) is passed, the User is authenticated
     * according to the authMode. A login is NOT granted, just the card is
     * associated.
     * </p>
     * <ul>
     * <li>Internal admin is NOT allowed to login to the User WebApp.</li>
     * <li>User MUST exist to login to Admin WebApp (no lazy user insert allowed
     * in this case).</li>
     * <li>User MUST exist to login when NO external user source (no lazy user
     * insert allowed in this case).</li>
     * <li>User MUST exist to login when lazy user insert is disabled.</li>
     * <li>User MUST have admin rights to login to Admin WebApp.</li>
     * <li>User MUST be a Person to login.</li>
     * <li>User MUST be active (enabled) at moment of login.</li>
     * </ul>
     *
     * @param userData
     *            The map which can be converted to a json string by the caller.
     * @param authMode
     *            The authentication mode.
     * @param authId
     *            Offered user name (handled as user alias), ID Number or Card
     *            Number.
     * @param authPw
     *            The password to be validated against the user source or user
     *            PIN.
     * @param assocCardNumber
     *            The card number to associate with this user account. When
     *            {@code null} NO card will be associated.
     * @param isAdminOnlyLogin
     *            <code>true</code> if this is a login for admin only.
     * @return Same object as userData param.
     * @throws IOException
     */
    private Map<String, Object> reqLoginNew(final Map<String, Object> userData,
            final UserAuth.Mode authMode, final String authId,
            final String authPw, final String assocCardNumber,
            final boolean isAdminOnlyLogin) throws IOException {

        /*
         * INVARIANT: Password can NOT be empty in Name authentication.
         */
        if (authMode == UserAuth.Mode.NAME) {
            if (StringUtils.isBlank(authPw)) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-login-no-password-given");
            }
        }

        /*
         *
         */
        final Device terminal = getHostTerminal();

        final UserAuth theUserAuth =
                new UserAuth(terminal, null, isAdminOnlyLogin);

        if (!theUserAuth.isAuthModeAllowed(authMode)) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-auth-mode-not-available", authMode.toString());
        }

        /*
         *
         */
        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final SpSession session = SpSession.get();
        final ConfigManager cm = ConfigManager.instance();

        final IExternalUserAuthenticator userAuthenticator =
                cm.getUserAuthenticator();

        /*
         * This is the place to set the WebAppType session attribute.
         */
        WebAppTypeEnum webAppType;

        if (isAdminOnlyLogin) {
            webAppType = WebAppTypeEnum.ADMIN;
        } else {
            webAppType = WebAppTypeEnum.USER;
        }
        session.setWebAppType(webAppType);

        /*
         * Initialize pessimistic.
         */
        session.setUser(null);

        /*
         * The facts.
         */
        final boolean allowInternalUsersOnly = (userAuthenticator == null);

        final boolean isInternalAdmin =
                authMode == UserAuth.Mode.NAME
                        && ConfigManager.isInternalAdmin(authId);

        final boolean isLazyUserInsert =
                cm.isUserInsertLazyLogin() && !isAdminOnlyLogin
                        && !allowInternalUsersOnly;

        /*
         * To find out.
         */
        boolean isAuthenticated = false;
        boolean isInternalUser = false;
        String uid = null;
        User userDb = null;

        /*
         *
         */
        RfidNumberFormat rfidNumberFormat = null;

        if (authMode == UserAuth.Mode.CARD_LOCAL || assocCardNumber != null) {
            if (terminal == null) {
                rfidNumberFormat = new RfidNumberFormat();
            } else {
                final DeviceAttrLookup lookup = new DeviceAttrLookup(terminal);
                rfidNumberFormat =
                        DEVICE_SERVICE.createRfidNumberFormat(terminal, lookup);
            }
        }

        /*
         *
         */
        if (isInternalAdmin) {

            /*
             * Internal admin
             */

            if (isAdminOnlyLogin) {

                User userAuth =
                        ConfigManager.instance().isInternalAdminValid(authId,
                                authPw);

                isAuthenticated = (userAuth != null);

                if (isAuthenticated) {

                    uid = authId;
                    userDb = userAuth;

                } else {
                    /*
                     * INVARIANT: internal admin password must be correct.
                     */
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-login-invalid");
                }

            } else {
                /*
                 * INVARIANT: internal admin is NOT allowed to login to the User
                 * WebApp.
                 */
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-login-denied");
            }

        } else {

            if (authMode == UserAuth.Mode.NAME) {

                /*
                 * Get the "real" username from the alias.
                 */
                uid =
                        UserAliasList.instance().getUserName(
                                userAuthenticator.asDbUserId(authId));
                /*
                 * Read real user from database.
                 */
                uid = userAuthenticator.asDbUserId(uid);

                userDb = userDao.findActiveUserByUserId(uid);

            } else if (authMode == UserAuth.Mode.ID) {

                userDb = USER_SERVICE.findUserByNumber(authId);

                /*
                 * INVARIANT: User MUST be present in database.
                 */
                if (userDb == null) {
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-login-invalid-number");
                }
                uid = userDb.getUserId();

            } else if (authMode == UserAuth.Mode.CARD_IP
                    || authMode == UserAuth.Mode.CARD_LOCAL) {

                String normalizedCardNumber = authId;

                if (authMode == UserAuth.Mode.CARD_LOCAL) {
                    normalizedCardNumber =
                            rfidNumberFormat.getNormalizedNumber(authId);
                }

                userDb =
                        USER_SERVICE.findUserByCardNumber(normalizedCardNumber);

                /*
                 * INVARIANT: User MUST be present.
                 */
                if (userDb == null) {

                    userData.put("authCardSelfAssoc",
                            Boolean.valueOf(theUserAuth.isAuthCardSelfAssoc()));

                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-login-unregistered-card");
                }
                uid = userDb.getUserId();
            }

            /*
             * Check invariants based on user presence in database.
             */
            if (userDb == null) {

                if (isAdminOnlyLogin) {
                    /*
                     * INVARIANT: User MUST exist to login to Admin WebApp (no
                     * lazy user insert allowed in this case)
                     */
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-login-admin-not-present");
                }

                if (allowInternalUsersOnly) {
                    /*
                     * INVARIANT: User MUST exist to login when NO external user
                     * source (no lazy user insert allowed in this case).
                     */
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-login-user-not-present");
                }

                if (!isLazyUserInsert) {
                    /*
                     * INVARIANT: User MUST exist to login when lazy user insert
                     * is disabled.
                     */
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-login-user-not-present");
                }

            } else {

                if (isAdminOnlyLogin && !userDb.getAdmin()) {
                    /*
                     * INVARIANT: User MUST have admin rights to login to Admin
                     * WebApp.
                     */
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-login-no-admin-rights");
                }

                if (!userDb.getPerson()) {
                    /*
                     * INVARIANT: User MUST be a Person to login.
                     */
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-login-no-person");
                }

                final Date onDate = new Date();

                if (USER_SERVICE.isUserFullyDisabled(userDb, onDate)) {
                    /*
                     * INVARIANT: User MUST be active (enabled) at moment of
                     * login.
                     */
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-login-disabled");
                }

                /*
                 * Identify internal user.
                 */
                isInternalUser = userDb.getInternal();

            }

            /*
             * Authenticate
             */
            if (authMode == UserAuth.Mode.NAME) {

                if (isInternalUser) {

                    isAuthenticated =
                            InternalUserAuthenticator.authenticate(userDb,
                                    authPw);

                    if (!isAuthenticated) {
                        /*
                         * INVARIANT: Password of Internal User must be correct.
                         */
                        return setApiResult(userData, API_RESULT_CODE_ERROR,
                                "msg-login-invalid");
                    }

                    // No lazy insert for internal user.

                } else {

                    User userAuth = userAuthenticator.authenticate(uid, authPw);
                    isAuthenticated = (userAuth != null);

                    if (!isAuthenticated) {
                        /*
                         * INVARIANT: Password of External User must be correct.
                         */
                        return setApiResult(userData, API_RESULT_CODE_ERROR,
                                "msg-login-invalid");
                    }

                    /**
                     * Lazy user insert
                     */
                    if (userDb == null) {

                        boolean lazyInsert = false;

                        final String group =
                                cm.getConfigValue(Key.USER_SOURCE_GROUP).trim();

                        if (group.isEmpty()) {
                            lazyInsert = true;
                        } else {
                            IUserSource userSource = cm.getUserSource();
                            lazyInsert = userSource.isUserInGroup(uid, group);
                        }

                        if (lazyInsert) {
                            userDb =
                                    userDao.findActiveUserByUserIdInsert(
                                            userAuth, new Date(),
                                            Entity.ACTOR_SYSTEM);
                            /*
                             * IMPORTANT: ad-hoc commit + begin transaction
                             */
                            if (userDb != null) {
                                ServiceContext.getDaoContext().commit();
                                ServiceContext.getDaoContext()
                                        .beginTransaction();
                            }
                        }
                    }

                }

            } else {

                /*
                 * Check PIN for both ID Number, Local and Network Card.
                 */
                isAuthenticated =
                        (authMode == UserAuth.Mode.ID && !theUserAuth
                                .isAuthIdPinReq())
                                || (authMode == UserAuth.Mode.CARD_IP && !theUserAuth
                                        .isAuthCardPinReq())
                                || (authMode == UserAuth.Mode.CARD_LOCAL && !theUserAuth
                                        .isAuthCardPinReq());

                if (!isAuthenticated) {

                    if (StringUtils.isBlank(authPw)) {
                        /*
                         * INVARIANT: PIN can NOT be empty.
                         */
                        return setApiResult(userData, API_RESULT_CODE_ERROR,
                                "msg-login-no-pin-available");
                    }

                    final String encryptedPin =
                            USER_SERVICE.findUserAttrValue(userDb,
                                    UserAttrEnum.PIN);
                    String pin = "";
                    if (encryptedPin != null) {
                        pin =
                                CryptoUser.decryptUserPin(userDb.getId(),
                                        encryptedPin);
                    }
                    isAuthenticated = pin.equals(authPw);
                }

                if (!isAuthenticated) {
                    /*
                     * INVARIANT: PIN must be correct.
                     */
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-login-invalid-pin");
                }
            }

            /*
             * Lazy create user home directory
             */
            if (!isAdminOnlyLogin && userDb != null) {

                /*
                 * Ad-hoc user lock
                 */
                userDb = userDao.lock(userDb.getId());

                try {
                    USER_SERVICE.lazyUserHomeDir(uid);
                } catch (IOException e) {
                    return setApiResult(userData, API_RESULT_CODE_ERROR,
                            "msg-user-home-dir-create-error");
                }
            }

        }

        /*
         * Associate Card Number to User?
         */
        if (assocCardNumber != null && userDb != null) {

            USER_SERVICE.assocPrimaryCardNumber(userDb,
                    rfidNumberFormat.getNormalizedNumber(assocCardNumber));

            /*
             * Do NOT grant a login, just associate the card.
             */
            return setApiResult(userData, API_RESULT_CODE_INFO,
                    "msg-card-registered-ok");
        }

        /*
         * Warnings for Admin WebApp.
         */
        if (isAdminOnlyLogin) {
            if (!cm.isSetupCompleted()) {
                setApiResult(userData, API_RESULT_CODE_WARN,
                        "msg-setup-is-needed");
            } else if (cm.doesInternalAdminHasDefaultPassword()) {
                setApiResult(userData, API_RESULT_CODE_WARN,
                        "msg-change-internal-admin-password");
            } else {
                setApiResultMembershipMsg(userData);
            }

        } else {
            setApiResultOK(userData);
        }

        /*
         * Deny access, when the user is still not found in the database.
         */
        if (userDb == null) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-login-user-not-present");
        }

        final UserAuthToken authToken =
                reqLoginLazyCreateAuthToken(uid, isAdminOnlyLogin);

        onUserLoginGranted(userData, session, isAdminOnlyLogin, uid, userDb,
                authToken);

        /*
         * Update session.
         */
        session.setUser(userDb);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Setting user in session of application ["
                    + SpSession.get().getApplication().getClass().toString()
                    + "] isAuthenticated [" + SpSession.get().isAuthenticated()
                    + "]");
        }

        return userData;
    }

    /**
     * Handles the login request for both the User and Admin WebApp.
     * <p>
     * NOTE: If {@link #AUTH_MODE_USER} and authPw is {@code null} the user is
     * validated against the authToken.
     * </p>
     * <p>
     * When an assocCardNumber (not null) is passed, the User is authenticated
     * according to the authMode. A login is NOT granted, just the card is
     * associated.
     * </p>
     * Invariants:
     * <ul>
     * <li>The application SHOULD be initialized.</li>
     * <li>If set-up is not completed then the only login possible is as
     * internal admin in the Admin WebApp.</li>
     * <li>If Application is NOT ready-to-use the only login possible is as
     * admin in the admin application.</li>
     * <li>See
     * {@link #reqLoginNew(Map, EntityManager, org.savapage.core.services.helpers.UserAuth.Mode, String, String, boolean)}
     * and
     * {@link #reqLoginAuthToken(Map, EntityManager, String, String, boolean)}.</li>
     * </ul>
     *
     * @param authMode
     *            The authentication mode.
     * @param authId
     *            Offered use name (handled as user alias), ID Number or Card
     *            Number.
     * @param authPw
     *            The password or PIN. When {@code null} AND
     *            {@link #AUTH_MODE_USER}, the authToken is used to validate.
     * @param authToken
     *            The authentication token.
     * @param assocCardNumber
     *            The card number to associate with this user account. When
     *            {@code null} NO card will be associated.
     * @param isAdminOnlyLogin
     *            <code>true</code> if this is a login for admin only.
     * @return The map which can be converted to a json string by the caller.
     * @throws IOException
     */
    private Map<String, Object> reqLogin(final UserAuth.Mode authMode,
            final String authId, final String authPw, final String authToken,
            final String assocCardNumber, final boolean isAdminOnlyLogin)
            throws IOException {

        /*
         * Browser diagnostics.
         */
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Browser detection for [" + authId + "] Mobile ["
                    + isMobileBrowser() + "] Mobile Safari ["
                    + isMobileSafariBrowser() + "] UserAgent ["
                    + getUserAgent() + "]");
        }

        /*
         *
         */
        final Map<String, Object> userData = new HashMap<String, Object>();

        final ConfigManager cm = ConfigManager.instance();

        if (LOGGER.isTraceEnabled()) {
            final SpSession session = SpSession.get();
            String testLog = "Session [" + session.getId() + "]";
            testLog += " WebAppCount [" + session.getAuthWebAppCount() + "]";
            LOGGER.trace(testLog);
        }

        /*
         * INVARIANT: Only one (1) authenticated session allowed for desktop
         * computers.
         */
        final boolean isMobile = isMobileBrowser();

        if (!isMobile && SpSession.get().getAuthWebAppCount() != 0) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-login-another-session-active");
        }

        /*
         * INVARIANT: The application SHOULD be initialized.
         */
        if (!cm.isInitialized()) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-login-not-possible");
        }

        /*
         * INVARIANT: If set-up is NOT completed the only login possible is mode
         * {@link #AUTH_MODE_USER} as INTERNAL admin in the admin application.
         */
        if (!cm.isSetupCompleted()) {
            if (!isAdminOnlyLogin || authMode != UserAuth.Mode.NAME) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-login-install-mode");
            }
            if (!ConfigManager.isInternalAdmin(authId)) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-login-as-internal-admin");
            }
        }

        /*
         * INVARIANT: If Application is NOT ready-to-use the only login possible
         * is as admin in the admin application.
         */
        if (!cm.isAppReadyToUse()) {
            if (!isAdminOnlyLogin) {
                return setApiResult(userData, API_RESULT_CODE_ERROR,
                        "msg-login-app-config");
            }
        }

        final boolean isCliAppAuthApplied;

        if (authMode == UserAuth.Mode.NAME && StringUtils.isBlank(authPw)) {
            isCliAppAuthApplied =
                    this.reqLoginAuthTokenCliApp(userData, authId,
                            this.getClientIpAddr(), isAdminOnlyLogin);
        } else {
            isCliAppAuthApplied = false;
        }

        if (!isCliAppAuthApplied) {

            if (authMode == UserAuth.Mode.NAME && StringUtils.isBlank(authPw)
                    && StringUtils.isNotBlank(authToken)) {

                reqLoginAuthTokenWebApp(userData, authId, authToken,
                        isAdminOnlyLogin);

            } else {
                reqLoginNew(userData, authMode, authId, authPw,
                        assocCardNumber, isAdminOnlyLogin);
            }
        }

        userData.put("sessionid", SpSession.get().getId());

        if (isApiResultOK(userData)) {
            setSessionTimeoutSeconds(isAdminOnlyLogin);
            SpSession.get().incrementAuthWebApp();
        }

        return userData;
    }

    /**
     *
     * @param isAdminSession
     */
    private void setSessionTimeoutSeconds(boolean isAdminSession) {

        Request request = RequestCycle.get().getRequest();

        if (request instanceof WebRequest) {

            ServletWebRequest wr = (ServletWebRequest) request;
            HttpSession session = wr.getContainerRequest().getSession();

            if (session != null) {

                IConfigProp.Key configKey;

                if (isAdminSession) {
                    configKey =
                            IConfigProp.Key.WEB_LOGIN_ADMIN_SESSION_TIMOUT_MINS;
                } else {
                    configKey =
                            IConfigProp.Key.WEB_LOGIN_USER_SESSION_TIMEOUT_MINS;
                }

                int minutes = ConfigManager.instance().getConfigInt(configKey);

                if (minutes >= 0) {
                    session.setMaxInactiveInterval(minutes * 60);
                }
            }
        }
    }

    /**
     * Uses the existing {@link UserAuthToken} (if found) or creates a new one.
     *
     * @param userId
     *            The user id.
     * @param isAdminOnlyLogin
     * @return The {@link UserAuthToken}.
     */
    private UserAuthToken reqLoginLazyCreateAuthToken(final String userId,
            final boolean isAdminOnlyLogin) {

        UserAuthToken authToken =
                WebAppUserAuthManager.instance().getAuthTokenOfUser(userId,
                        isAdminOnlyLogin);

        if (authToken == null || authToken.isAdminOnly() != isAdminOnlyLogin) {
            authToken = new UserAuthToken(userId, isAdminOnlyLogin);
            WebAppUserAuthManager.instance().putUserAuthToken(authToken,
                    isAdminOnlyLogin);

        }

        return authToken;
    }

    /**
     * Tries to login with Client App authentication token.
     *
     * @param userData
     *            The user data to be filled after applying the authentication
     *            method. If method is NOT applied, the userData in not touched.
     * @param userId
     *            The unique user id.
     * @param clientIpAddress
     *            The remote IP address.
     * @return {@code false} when Client App Authentication was NOT applied.
     * @throws IOException
     */
    private boolean reqLoginAuthTokenCliApp(final Map<String, Object> userData,
            final String userId, final String clientIpAddress,
            final boolean isAdminOnly) throws IOException {

        /*
         * INVARIANT: do NOT authenticate for Admin Web App.
         */
        if (isAdminOnly) {
            return false;
        }

        /*
         * INVARIANT: Trust between User Web App and User Client App MUST be
         * enabled.
         */
        if (!ConfigManager.instance().isConfigValue(
                Key.WEBAPP_USER_AUTH_TRUST_CLIAPP_AUTH)) {
            return false;
        }

        /*
         * INVARIANT: authentication token MUST be present for IP address.
         */
        final UserAuthToken authTokenCliApp =
                ClientAppUserAuthManager.getIpAuthToken(clientIpAddress);

        if (authTokenCliApp == null) {
            return false;
        }

        /*
         * INVARIANT: authentication token MUST match requesting user.
         */
        final String userIdToken = authTokenCliApp.getUser();

        if (userIdToken == null || !userIdToken.equalsIgnoreCase(userId)) {
            return false;
        }

        /*
         * INVARIANT: authentication token MUST not be older than 2 times the
         * max time it takes a long poll to finish.
         */
        if (ServiceContext.getTransactionDate().getTime()
                - authTokenCliApp.getCreateTime() > 2 * UserEventService
                .getMaxMonitorMsec()) {
            return false;
        }

        /*
         * INVARIANT: User must exist in database.
         */
        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();
        final User userDb = userDao.findActiveUserByUserId(userId);

        if (userDb != null) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("CliApp AuthToken Login [" + userId + "] granted.");
            }

            final SpSession session = SpSession.get();

            session.setUser(userDb);

            final UserAuthToken authTokenWebApp =
                    reqLoginLazyCreateAuthToken(userId, isAdminOnly);

            onUserLoginGranted(userData, session, isAdminOnly, userId,
                    session.getUser(), authTokenWebApp);

            setApiResultOK(userData);

        } else {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("CliApp AuthToken Login [" + userId
                        + "] denied: user NOT found.");
            }

            setApiResult(userData, API_RESULT_CODE_ERROR, "msg-login-invalid");
        }

        return true;
    }

    /**
     * Tries to login with WebApp authentication token.
     *
     * @param userData
     * @param uid
     * @param authtoken
     * @param isAdminOnly
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqLoginAuthTokenWebApp(
            final Map<String, Object> userData, final String uid,
            final String authtoken, final boolean isAdminOnly)
            throws IOException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Login [" + uid + "] with WebApp AuthToken.");
        }

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        User userDb = null;

        final WebAppUserAuthManager userAuthManager =
                WebAppUserAuthManager.instance();

        final UserAuthToken authTokenObj =
                userAuthManager.getUserAuthToken(authtoken, isAdminOnly);

        if (authTokenObj != null && uid.equals(authTokenObj.getUser())
                && authTokenObj.isAdminOnly() == isAdminOnly) {

            if (isAdminOnly && ConfigManager.isInternalAdmin(uid)) {
                userDb = ConfigManager.createInternalAdminUser();
            } else {
                userDb = userDao.findActiveUserByUserId(uid);
            }

        }

        if (userDb != null) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("WebApp AuthToken Login [" + uid + "] granted.");
            }

            final SpSession session = SpSession.get();
            session.setUser(userDb);
            onUserLoginGranted(userData, session, isAdminOnly, uid,
                    session.getUser(), authTokenObj);
            setApiResultOK(userData);

        } else {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("WebApp AuthToken Login [" + uid
                        + "] denied: user NOT found.");
            }

            setApiResult(userData, API_RESULT_CODE_ERROR, "msg-login-invalid");
        }

        return userData;
    }

    /**
     * Adds user statistics at the {@code stats} key of jsonData.
     * <p>
     * The User Statistics are used by the Client WebApp to update the Pie-chart
     * graphics.
     * </p>
     *
     * @param jsonData
     *            The JSON data to add the {@code stats} key to.
     * @param user
     *            The User to get the statistics from.
     */
    public static void addUserStats(Map<String, Object> jsonData,
            final User user, Locale locale, String currencySymbol) {

        final Map<String, Object> stats = new HashMap<>();

        stats.put("pagesPrintIn", user.getNumberOfPrintInPages());
        stats.put("pagesPrintOut", user.getNumberOfPrintOutPages());
        stats.put("pagesPdfOut", user.getNumberOfPdfOutPages());

        final AccountDisplayInfoDto dto =
                ACCOUNTING_SERVICE.getAccountDisplayInfo(user, locale,
                        currencySymbol);

        stats.put("accountInfo", dto);

        final OutboxInfoDto outbox =
                OUTBOX_SERVICE.pruneOutboxInfo(user.getUserId(),
                        ServiceContext.getTransactionDate());

        OUTBOX_SERVICE.applyLocaleInfo(outbox, locale, currencySymbol);

        stats.put("outbox", outbox);

        jsonData.put("stats", stats);
    }

    /**
     * Sets the user data for login request and notifies the authenticated user
     * to the Admin WebApp.
     *
     * @param userData
     * @param uid
     * @param userDb
     * @throws IOException
     */
    private void onUserLoginGranted(final Map<String, Object> userData,
            final SpSession session, final boolean isAdminOnlyLogin,
            final String uid, final User userDb, final UserAuthToken authToken)
            throws IOException {

        userData.put("id", uid);
        userData.put("key_id", userDb.getId());
        userData.put("fullname", userDb.getFullName());
        userData.put("admin", userDb.getAdmin());
        userData.put("internal", userDb.getInternal());
        userData.put("systime", Long.valueOf(new Date().getTime()));
        userData.put("language", getSession().getLocale().getLanguage());
        userData.put("mail", USER_SERVICE.getPrimaryEmailAddress(userDb));

        if (authToken != null) {
            userData.put("authtoken", authToken.getToken());
        }

        final String cometdToken;

        if (userDb.getAdmin()) {
            cometdToken = CometdClientMixin.SHARED_USER_ADMIN_TOKEN;
            userData.put("role", "admin"); // TODO
        } else {
            cometdToken = CometdClientMixin.SHARED_USER_TOKEN;
            userData.put("role", "editor"); // TODO
        }
        userData.put("cometdToken", cometdToken);

        // role (editor|admin|reader)

        WebApp.get().onAuthenticatedUser(session.getId(), getRemoteAddr(), uid,
                userDb.getAdmin());

        if (!isAdminOnlyLogin) {

            addUserStats(userData, userDb, this.getSession().getLocale(),
                    SpSession.getCurrencySymbol());

            /*
             * Make sure that any User Web App long poll for this user is
             * interrupted.
             */
            interruptPendingLongPolls(userDb.getUserId());

            INBOX_SERVICE.pruneOrphanJobs(ConfigManager.getUserHomeDir(uid),
                    userDb);
        }

    }

    /**
     * Returns the Internet Protocol (IP) address of the client or last proxy
     * that sent the request. For HTTP servlets, same as the value of the CGI
     * variable <code>REMOTE_ADDR</code>.
     *
     * @return a <code>String</code> containing the IP address of the client
     *         that sent the request
     *
     */
    private String getRemoteAddr() {
        return ((ServletWebRequest) RequestCycle.get().getRequest())
                .getContainerRequest().getRemoteAddr();
    }

    /**
     * Logs out by replacing the underlying (Web)Session, invalidating the
     * current one and creating a new one. Also sends the
     * {@link UserMsgIndicator.Msg#STOP_POLL_REQ} message.
     * <p>
     * After the invalidate() the Wicket framework calls
     * {@link WebApp#sessionUnbound(String)} : this method publishes the logout
     * message.
     * </p>
     *
     * @param lockedUser
     * @param authToken
     *            The authorization token (can be {@code null}).
     * @return The OK message.
     * @throws IOException
     */
    private Map<String, Object> reqLogout(final String userId,
            final String authToken) throws IOException {

        ClientAppUserAuthManager.removeUserAuthToken(this.getClientIpAddr());

        WebAppUserAuthManager.instance().removeUserAuthToken(authToken,
                this.isAdminRoleContext());

        final SpSession session = SpSession.get();

        /*
         * Save the critical session attribute.
         */
        final WebAppTypeEnum savedWebAppType = session.getWebAppType();

        /*
         * IMPORTANT: Logout to remove the user associated with this session.
         */
        session.logout();

        /*
         * Replaces the underlying (Web)Session, invalidating the current one
         * and creating a new one. NOTE: the data of the current session is
         * copied.
         */
        session.replaceSession();

        /*
         * Restore the critical session attribute.
         */
        session.setWebAppType(savedWebAppType);

        /*
         * Make sure that all User Web App long polls for this user are
         * interrupted.
         */
        if (this.getWebAppType() == WebAppTypeEnum.USER) {
            interruptPendingLongPolls(userId);
        }

        /*
         * We are OK.
         */
        return createApiResultOK();
    }

    /**
     * @return The OK message.
     */
    private Map<String, Object> reqWebAppUnload() {
        SpSession.get().decrementAuthWebApp();
        return createApiResultOK();
    }

    /**
     * This method acts as a {@link #reqLogout(User, String)} for the
     * {@link User} in the current {@link SpSession}.
     *
     * @param authTokenUser
     *            The authentication token of the User WebApp.
     * @param authTokenAdmin
     *            The authentication token of the Admin WebApp.
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqWebAppCloseSession(
            final String authTokenUser, final String authTokenAdmin)
            throws IOException {

        final UserAuthToken removedTokenUser =
                WebAppUserAuthManager.instance().removeUserAuthToken(
                        authTokenUser, false);

        final UserAuthToken removedTokenAdmin =
                WebAppUserAuthManager.instance().removeUserAuthToken(
                        authTokenUser, true);

        final SpSession session = SpSession.get();

        if (LOGGER.isTraceEnabled()) {
            String testLog =
                    "reqWebAppCloseSession Session [" + session.getId() + "]";
            testLog += " WebAppCount [" + session.getAuthWebAppCount() + "]";
            testLog += " authTokenUser [" + authTokenUser + "]";
            if (removedTokenUser == null) {
                testLog += "  [NOT found]";
            } else {
                testLog += "  [REMOVED]";
            }
            testLog += " authTokenAdmin [" + authTokenAdmin + "]";
            if (removedTokenAdmin == null) {
                testLog += "  [NOT found]";
            } else {
                testLog += "  [REMOVED]";
            }
            LOGGER.trace(testLog);
        }

        final String userId;

        if (session.getUser() == null) {
            userId = null;
        } else {
            userId = session.getUser().getUserId();
        }

        /*
         * Save the critical session attribute.
         */
        final WebAppTypeEnum savedWebAppType = session.getWebAppType();

        /*
         * IMPORTANT: Logout to remove the user associated with this session.
         */
        session.logout();

        /*
         * Replaces the underlying (Web)Session, invalidating the current one
         * and creating a new one. NOTE: the data of the current session is
         * copied.
         */
        session.replaceSession();

        /*
         * Restore the critical session attribute.
         */
        session.setWebAppType(savedWebAppType);

        /*
         * Make sure that all User Web App long polls for this user are
         * interrupted.
         */
        if (savedWebAppType == WebAppTypeEnum.USER && userId != null) {
            interruptPendingLongPolls(userId);
        }

        /*
         * We are OK.
         */
        return createApiResultOK();
    }

    /**
     * Interrupts all current User Web App long polls for this user.
     * <p>
     * If the user id is the reserved 'admin', the interrupt is NOT applied.
     * </p>
     *
     * @param userId
     *            The user id.
     * @throws IOException
     *             When the interrupt message could not be written (to the
     *             message file).
     */
    private void interruptPendingLongPolls(final String userId)
            throws IOException {

        if (!ConfigManager.isInternalAdmin(userId)) {

            UserMsgIndicator.write(userId, new Date(),
                    UserMsgIndicator.Msg.STOP_POLL_REQ, getRemoteAddr());
        }
    }

    /**
     *
     * @param lockedUser
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqExitEventMonitor(final String userId)
            throws IOException {
        interruptPendingLongPolls(userId);
        return createApiResultOK();
    }

    /**
     * Sets the userData message with an error or warning depending on the
     * Membership position. If the membership is ok, the OK message is applied.
     *
     * @param userData
     *            The data the message is to be applied on.
     * @throws NumberFormatException
     */
    private void setApiResultMembershipMsg(final Map<String, Object> userData)
            throws NumberFormatException {

        final MemberCard memberCard = MemberCard.instance();

        Long daysLeft =
                memberCard.getDaysLeftInVisitorPeriod(ServiceContext
                        .getTransactionDate());

        switch (memberCard.getStatus()) {
        case EXCEEDED:
            setApiResult(userData, API_RESULT_CODE_WARN,
                    "msg-membership-exceeded-user-limit",
                    CommunityDictEnum.MEMBERSHIP.getWord(),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                    CommunityDictEnum.MEMBER_CARD.getWord());
            break;
        case EXPIRED:
            setApiResult(userData, API_RESULT_CODE_WARN,
                    "msg-membership-expired",
                    CommunityDictEnum.MEMBERSHIP.getWord(),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                    CommunityDictEnum.MEMBER_CARD.getWord());

            break;
        case VISITOR:
            setApiResult(userData, API_RESULT_CODE_WARN,
                    "msg-membership-visit", daysLeft.toString(),
                    CommunityDictEnum.VISITOR.getWord());
            break;
        case VISITOR_EXPIRED:
            setApiResult(userData, API_RESULT_CODE_WARN,
                    "msg-membership-visit-expired",
                    CommunityDictEnum.VISITOR.getWord(),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                    CommunityDictEnum.MEMBER_CARD.getWord());
            break;
        case WRONG_MODULE:
        case WRONG_COMMUNITY:
            setApiResult(userData, API_RESULT_CODE_WARN,
                    "msg-membership-wrong-product",
                    CommunityDictEnum.MEMBERSHIP.getWord(),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                    CommunityDictEnum.MEMBER_CARD.getWord());
            break;
        case WRONG_VERSION:
            setApiResult(userData, API_RESULT_CODE_WARN,
                    "msg-membership-version",
                    CommunityDictEnum.MEMBERSHIP.getWord(),
                    CommunityDictEnum.SAVAPAGE_SUPPORT.getWord(),
                    CommunityDictEnum.MEMBER_CARD.getWord());
            break;
        case WRONG_VERSION_WITH_GRACE:
            setApiResult(userData, API_RESULT_CODE_WARN,
                    "msg-membership-version-grace",
                    CommunityDictEnum.MEMBERSHIP.getWord(),
                    daysLeft.toString(),
                    CommunityDictEnum.MEMBER_CARD.getWord());
            break;
        case VISITOR_EDITION:
        case VALID:
        default:
            setApiResultOK(userData);
            break;
        }
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqGetEvent(final String user,
            final String pageOffset, final String uniqueUrlValue,
            final boolean base64) {

        int nPageOffset = Integer.parseInt(pageOffset);

        final PageImages pages =
                INBOX_SERVICE.getPageChunks(user, null, uniqueUrlValue, base64);

        int totPages = 0;

        for (final PageImage image : pages.getPages()) {
            totPages += image.getPages();
        }

        final Map<String, Object> userData = new HashMap<String, Object>();

        if (totPages != nPageOffset) {

            userData.put("jobs", pages.getJobs());
            userData.put("pages", pages.getPages());
            userData.put("event", "print");

        } else {
            userData.put("event", "");
        }

        return userData;
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqInboxJobDelete(final String user, int iJob) {

        INBOX_SERVICE.deleteJob(user, iJob);

        final Map<String, Object> userData = new HashMap<String, Object>();

        return setApiResult(userData, API_RESULT_CODE_OK, "msg-job-deleted");
    }

    /**
     *
     * @param user
     * @param online
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqGcpOnline(final User user,
            final boolean online) throws IOException {

        Map<String, Object> userData = new HashMap<String, Object>();

        final ConfigManager cm = ConfigManager.instance();

        /*
         * Google Cloud Print MUST be enabled.
         */
        if (!cm.isConfigValue(Key.GCP_ENABLE)) {
            /*
             * INVARIANT: GCP printer MUST be present.
             */
            throw new SpException(
                    "Operation not allowed: Google Cloud Print is disabled.");
        }

        /*
         * INVARIANT: GCP printer MUST be present.
         */
        if (!GcpPrinter.isPresent()) {
            return setApiResult(userData, API_RESULT_CODE_ERROR,
                    "msg-gcp-state-mismatch", GcpPrinter.getState().toString());
        }

        String msgKey;

        if (online == GcpPrinter.isOnline()) {

            if (online) {
                msgKey = "msg-gcp-already-online";
            } else {
                msgKey = "msg-gcp-already-offline";
            }

        } else {

            GcpPrinter.setOnline(online);

            if (online) {
                SpJobScheduler.instance().scheduleOneShotGcpListener(0L);
                msgKey = "msg-gcp-online";
            } else {
                SpJobScheduler.interruptGcpListener();
                msgKey = "msg-gcp-offline";
            }
        }

        /*
         * Anticipate to the new state...
         */
        GcpPrinter.State newState;

        if (online) {
            newState = GcpPrinter.State.ON_LINE;
        } else {
            newState = GcpPrinter.State.OFF_LINE;
        }

        userData.put("state", newState.toString());
        userData.put("displayState",
                GcpPrinter.localized(ConfigManager.isGcpEnabled(), newState));

        /*
         *
         */
        return setApiResult(userData, API_RESULT_CODE_OK, msgKey);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqGcpGetDetails() {

        Map<String, Object> userData = new HashMap<String, Object>();

        userData.put("clientId", GcpPrinter.getOAuthClientId());
        userData.put("clientSecret", GcpPrinter.getOAuthClientSecret());
        userData.put("printerName", GcpPrinter.getPrinterName());
        userData.put("ownerId", GcpPrinter.getOwnerId());

        final GcpPrinter.State state = GcpPrinter.getState();

        userData.put("state", state.toString());
        userData.put("displayState",
                GcpPrinter.localized(ConfigManager.isGcpEnabled(), state));

        return setApiResultOK(userData);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqGcpSetNotifications(final User user,
            boolean enabled, final String emailSubject, final String emailBody) {

        Map<String, Object> userData = new HashMap<String, Object>();

        final ConfigManager cm = ConfigManager.instance();

        cm.updateConfigKey(Key.GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_ENABLE,
                enabled, user.getUserId());

        cm.updateConfigKey(Key.GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_SUBJECT,
                emailSubject, user.getUserId());

        cm.updateConfigKey(Key.GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_BODY,
                emailBody, user.getUserId());

        return setApiResult(userData, API_RESULT_CODE_OK,
                "msg-config-props-applied");
    }

    /**
     *
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqGcpSetDetails(final User user,
            boolean enabled, final String clientId, final String clientSecret,
            final String printerName) {

        Map<String, Object> userData = new HashMap<String, Object>();

        /*
         * Update the printer singleton.
         */
        GcpPrinter.setPrinterName(printerName);
        GcpPrinter.storeOauthProps(clientId, clientSecret);

        /*
         * Currently enabled?
         */
        final ConfigManager cm = ConfigManager.instance();
        final Key key = Key.GCP_ENABLE;
        final boolean enabledCurrent = cm.isConfigValue(key);

        /*
         * Anticipate to the new state...
         */
        final GcpPrinter.State stateCurrent = GcpPrinter.getState();
        GcpPrinter.State stateNext = stateCurrent;

        /*
         *
         */
        if (enabled && stateCurrent == GcpPrinter.State.NOT_FOUND) {
            SpJobScheduler.instance().scheduleOneShotGcpListener(0L);
        }

        if (enabledCurrent != enabled) {

            cm.updateConfigKey(key, enabled, user.getUserId());

            if (!enabled) {
                SpJobScheduler.interruptGcpListener();
                stateNext = GcpPrinter.State.OFF_LINE;
            }
        }

        /*
         * Anticipate to the new state...
         */
        userData.put("state", stateNext.toString());
        userData.put("displayState",
                GcpPrinter.localized(ConfigManager.isGcpEnabled(), stateNext));

        return setApiResult(userData, API_RESULT_CODE_OK,
                "msg-config-props-applied");
    }

    /**
     *
     * @param user
     * @param printerName
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqGcpRegister(final User user,
            final String clientId, final String clientSecret,
            final String printerName) throws IOException {

        Map<String, Object> userData = new HashMap<String, Object>();

        /*
         * Update the printer singleton.
         */
        GcpPrinter.setPrinterName(printerName);
        GcpPrinter.storeOauthProps(clientId, clientSecret);

        /*
         * Register...
         */
        GcpRegisterPrinterRsp rsp = GcpClient.instance().registerPrinter();

        if (rsp.isSuccess()) {

            SpJobScheduler.instance().scheduleOneShotGcpPollForAuthCode(
                    rsp.getPollingUrl(), rsp.getTokenDuration(), 0L);

            userData.put("registration_token", rsp.getRegistrationToken());
            userData.put("invite_url", rsp.getInviteUrl());
            userData.put("invite_page_url", rsp.getInvitePageUrl());
            userData.put("complete_invite_url", rsp.getCompleteInviteUrl());

            setApiResultOK(userData);

        } else {
            throw new SpException("Registration failed");
        }

        return userData;
    }

    /**
     *
     * @return
     * @throws Exception
     */
    private Map<String, Object> reqLetterheadSet(final User user,
            final String letterheadId, final String jsonData) throws Exception {

        final JsonNode list;

        try {
            list = new ObjectMapper().readTree(jsonData);
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        INBOX_SERVICE.setLetterhead(user, letterheadId, list.get("name")
                .getTextValue(), list.get("foreground").getBooleanValue(), list
                .get("pub").getBooleanValue(), list.get("pub-new")
                .getBooleanValue());

        final Map<String, Object> userData = new HashMap<String, Object>();

        return setApiResult(userData, API_RESULT_CODE_OK,
                "msg-letterhead-edited");
    }

    /**
     *
     * @param user
     * @param letterheadId
     * @param isPublic
     * @return
     */
    private Map<String, Object> reqLetterheadDelete(final User user,
            final String letterheadId, final boolean isPublic) {

        INBOX_SERVICE.deleteLetterhead(user, letterheadId, isPublic);
        return createApiResultOK();
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqInboxJobEdit(final String user, int iJob,
            final String jsonData) {

        final JsonNode list;

        try {
            list = new ObjectMapper().readTree(jsonData);
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        INBOX_SERVICE.editJob(user, iJob, list.get("rotate").getBooleanValue(),
                list.get("undelete").getBooleanValue());

        final Map<String, Object> userData = new HashMap<String, Object>();
        return setApiResult(userData, API_RESULT_CODE_OK, "msg-job-edited");
    }

    /**
     * @param chartType
     *            The unique id of the chart type.
     * @param isGlobal
     *            {@code true} if global chart.
     * @param sessionUser
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqJqPlot(final String chartType,
            boolean isGlobal, User sessionUser) throws IOException {

        Map<String, Object> chartData = null;

        boolean isAdminChart = isGlobal;

        if (isAdminChart) {

            if (chartType.equalsIgnoreCase("dashboard-piechart")) {
                chartData = StatsPageTotalPanel.jqplotPieChart();
            } else if (chartType.equalsIgnoreCase("dashboard-xychart")) {
                chartData = StatsPageTotalPanel.jqplotXYLineChart();
            }

        } else {

            final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

            final User chartUser = userDao.findById(sessionUser.getId());

            if (chartType.equalsIgnoreCase("dashboard-piechart")) {
                chartData = StatsPageTotalPanel.jqplotPieChart(chartUser);
            } else if (chartType.equalsIgnoreCase("dashboard-xychart")) {
                chartData = StatsPageTotalPanel.jqplotXYLineChart(chartUser);
            }
        }

        Map<String, Object> userData = new HashMap<String, Object>();

        if (isAdminChart && !hasAdminRights(userData)) {
            return userData;
        } else if (chartData == null) {
            throw new SpException("unknown chartType [" + chartType + "]");
        } else {
            setApiResultOK(userData);
            userData.put("chartData", chartData);
        }

        return userData;
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqInboxJobPages(final String user,
            final String firstDetailPage, final String uniqueUrlValue,
            boolean base64) {

        int nFirstDetailPage = Integer.parseInt(firstDetailPage);

        Map<String, Object> userData = new HashMap<String, Object>();

        setApiResultOK(userData);

        PageImages pages =
                INBOX_SERVICE.getPageChunks(user, nFirstDetailPage,
                        uniqueUrlValue, base64);

        userData.put("jobs", pages.getJobs());
        userData.put("pages", pages.getPages());
        userData.put("url_template",
                ImageUrl.composeDetailPageTemplate(user, base64));

        return userData;
    }

    /**
     *
     * @param authModeReq
     *            The requested authentication mode.
     * @return
     */
    private Map<String, Object> reqConstants(final String authModeReq) {

        final Map<String, Object> userData = new HashMap<String, Object>();

        /*
         * Straight values
         */
        setApiResultOK(userData);

        userData.put("thumbnail-width", ImageUrl.THUMBNAIL_WIDTH);
        userData.put("pdf-prop-default", PdfProperties.createDefault());

        /*
         * The authenticated user of the current session, or null if not
         * present.
         */
        String authenticatedUser = null;

        if (SpSession.get() != null && SpSession.get().getUser() != null) {

            authenticatedUser = SpSession.get().getUser().getUserId();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("User [" + authenticatedUser
                        + "] is authenticated in current session");
            }
        }

        /*
         * See Mantis #320. No BASE64 images for Mobile Safari Browser anymore.
         */
        userData.put("img_base64", false);

        /*
         * Setting values for existing i18n keys. These keys must be present in
         * the i18n_<language>.properties files.
         */
        Map<String, Object> i18nValues = new HashMap<String, Object>();

        // deprecated
        userData.put("i18n_values", i18nValues);

        /*
         *
         */
        final UserAuth userAuth =
                new UserAuth(getHostTerminal(), authModeReq, getWebAppType()
                        .equals(WebAppTypeEnum.ADMIN));

        userData.put("authName", userAuth.isVisibleAuthName());
        userData.put("authId", userAuth.isVisibleAuthId());
        userData.put("authCardLocal", userAuth.isVisibleAuthCardLocal());
        userData.put("authCardIp", userAuth.isVisibleAuthCardIp());

        userData.put("authModeDefault",
                UserAuth.mode(userAuth.getAuthModeDefault()));

        userData.put("authCardPinReq", userAuth.isAuthCardPinReq());
        userData.put("authCardSelfAssoc", userAuth.isAuthCardSelfAssoc());

        final Integer maxIdleSeconds = userAuth.getMaxIdleSeconds();

        if (maxIdleSeconds != null && maxIdleSeconds > 0) {
            userData.put("maxIdleSeconds", maxIdleSeconds);
        }

        /*
         *
         */
        ConfigManager cm = ConfigManager.instance();

        userData.put("cardLocalMaxMsecs",
                cm.getConfigInt(Key.WEBAPP_CARD_LOCAL_KEYSTROKES_MAX_MSECS));

        userData.put("cardAssocMaxSecs",
                cm.getConfigInt(Key.WEBAPP_CARD_ASSOC_DIALOG_MAX_SECS));

        userData.put("intUserIdPfx",
                cm.getConfigValue(Key.INTERNAL_USERS_NAME_PREFIX));

        userData.put("watchdogHeartbeatSecs",
                cm.getConfigInt(Key.WEBAPP_WATCHDOG_HEARTBEAT_SECS));

        userData.put("watchdogTimeoutSecs",
                cm.getConfigInt(Key.WEBAPP_WATCHDOG_TIMEOUT_SECS));

        /*
         *
         */
        userData.put("cometdToken", CometdClientMixin.SHARED_DEVICE_TOKEN);

        userData.put("cometdMaxNetworkDelay",
                AbstractEventService.getMaxNetworkDelay());

        /*
         *
         */
        userData.put("locale", getSession().getLocale().toLanguageTag());
        userData.put("systime", Long.valueOf(new Date().getTime()));

        return userData;
    }

    /**
     * Sets the language of the Web App, and the {@link Locale} in the
     * {@link SpSession}.
     * <p>
     * For the session {@link Locale} we adopt the country/region code from the
     * Browser, if the language setting of the Browser is the same as the
     * language of the Web App.
     * </p>
     * <p>
     * NOTE: The country/region in our session locale is used (as default) when
     * determining the Currency Symbol.
     * </p>
     *
     * @param language
     *            The selected language for the Web App. This value is also used
     *            for the language setting in the session {@link Locale}.
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqLanguage(final String language)
            throws IOException {

        Map<String, Object> userData = new HashMap<String, Object>();

        /*
         * Straight values.
         */
        setApiResultOK(userData);

        Map<String, Object> i18n = new HashMap<String, Object>();

        /*
         * Set the new locale, for the strings to return AND for the current
         * session.
         */
        Locale.Builder localeBuilder = new Locale.Builder();
        localeBuilder.setLanguage(language);

        /*
         * We adopt the country/region code from the Browser, if the language
         * setting of the Browser is the same as the SELECTED language in the
         * SavaPage Web App.
         *
         * The country/region in our session locale is used (as default) when
         * determining the Currency Symbol. If country/region in missing in the
         * session locale
         */
        final Locale localeReq = getRequestCycle().getRequest().getLocale();
        if (localeReq.getLanguage().equalsIgnoreCase(language)) {
            localeBuilder.setRegion(localeReq.getCountry());
        }

        //
        Locale locale = localeBuilder.build();
        SpSession.get().setLocale(locale);

        /*
         *
         */
        ResourceBundle rcBundle =
                Messages.loadResource(getClass(), "i18n", locale);

        Set<String> keySet = rcBundle.keySet();
        for (final String key : keySet) {
            i18n.put(key, rcBundle.getString(key));
        }

        userData.put("i18n", i18n);
        userData.put("language", locale.getLanguage());

        return userData;
    }

    /**
     *
     * @param resetBy
     * @param resetDashboard
     * @param resetQueues
     * @param resetPrinters
     * @param resetUsers
     */
    private void resetPagometers(final String resetBy,
            final boolean resetDashboard, final boolean resetQueues,
            final boolean resetPrinters, final boolean resetUsers) {
        DOC_LOG_SERVICE.resetPagometers(resetBy, resetDashboard, resetQueues,
                resetPrinters, resetUsers);
    }

}
