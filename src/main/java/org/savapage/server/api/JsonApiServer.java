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
package org.savapage.server.api;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.persistence.PessimisticLockException;
import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.time.Duration;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.exception.LockAcquisitionException;
import org.savapage.core.LetterheadNotFoundException;
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
import org.savapage.core.concurrent.ReadLockObtainFailedException;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.OnOffEnum;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.UserAccountDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.DeviceAttrEnum;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.AccountVoucherBatchDto;
import org.savapage.core.dto.AccountVoucherRedeemDto;
import org.savapage.core.dto.JrPageSizeDto;
import org.savapage.core.dto.PosDepositDto;
import org.savapage.core.dto.PosDepositReceiptDto;
import org.savapage.core.dto.PrimaryKeyDto;
import org.savapage.core.dto.ProxyPrinterCostDto;
import org.savapage.core.dto.UserCreditTransferDto;
import org.savapage.core.dto.VoucherBatchPrintDto;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.imaging.ImageUrl;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.OutputProducer;
import org.savapage.core.inbox.PageImages;
import org.savapage.core.inbox.PageImages.PageImage;
import org.savapage.core.jmx.JmxRemoteProperties;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.job.SpJobType;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.json.JsonPrinterDetail;
import org.savapage.core.json.PdfProperties;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.JsonRpcConfig;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.ResultDataBasic;
import org.savapage.core.json.rpc.impl.ResultPosDeposit;
import org.savapage.core.pdf.PdfCreateRequest;
import org.savapage.core.print.gcp.GcpClient;
import org.savapage.core.print.gcp.GcpPrinter;
import org.savapage.core.print.gcp.GcpRegisterPrinterRsp;
import org.savapage.core.print.imap.ImapListener;
import org.savapage.core.print.imap.ImapPrinter;
import org.savapage.core.print.proxy.ProxyPrintAuthManager;
import org.savapage.core.reports.JrExportFileExtEnum;
import org.savapage.core.reports.JrPosDepositReceipt;
import org.savapage.core.reports.JrVoucherPageDesign;
import org.savapage.core.reports.impl.AccountTrxListReport;
import org.savapage.core.reports.impl.ReportCreator;
import org.savapage.core.reports.impl.UserListReport;
import org.savapage.core.services.AccountVoucherService;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.DocLogService;
import org.savapage.core.services.EmailService;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.services.helpers.PageRangeException;
import org.savapage.core.services.helpers.UserAuth;
import org.savapage.core.services.helpers.email.EmailMsgParms;
import org.savapage.core.users.CommonUserGroup;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.InetUtils;
import org.savapage.core.util.LocaleHelper;
import org.savapage.core.util.MediaUtils;
import org.savapage.core.util.Messages;
import org.savapage.ext.papercut.DelegatedPrintPeriodDto;
import org.savapage.ext.papercut.PaperCutDbProxy;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.ext.payment.PaymentGatewayPlugin.PaymentRequest;
import org.savapage.ext.payment.PaymentGatewayTrx;
import org.savapage.ext.smartschool.SmartschoolPrintMonitor;
import org.savapage.ext.smartschool.SmartschoolPrinter;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.pdf.PdfPgpVerifyUrl;
import org.savapage.server.WebApp;
import org.savapage.server.api.request.ApiRequestHandler;
import org.savapage.server.api.request.ApiRequestHelper;
import org.savapage.server.api.request.ApiRequestMixin;
import org.savapage.server.api.request.ApiResultCodeEnum;
import org.savapage.server.api.request.export.ReqExportDocStorePdf;
import org.savapage.server.api.request.export.ReqExportOutboxPdf;
import org.savapage.server.api.request.export.ReqExportPrinterOpt;
import org.savapage.server.api.request.export.ReqExportPrinterPpd;
import org.savapage.server.api.request.export.ReqExportUserDataHistory;
import org.savapage.server.cometd.AbstractEventService;
import org.savapage.server.dropzone.PdfPgpDropZoneFileResource;
import org.savapage.server.dropzone.PdfPgpUploadHelper;
import org.savapage.server.dropzone.WebPrintDropZoneFileResource;
import org.savapage.server.dropzone.WebPrintHelper;
import org.savapage.server.dto.MoneyTransferDto;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.SparklineHtml;
import org.savapage.server.pages.AbstractPage;
import org.savapage.server.pages.StatsPageTotalPanel;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;

/**
 * Implements the private JSON interface of the Server.
 *
 * @author Rijk Ravestein
 *
 */
public final class JsonApiServer extends AbstractPage {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(JsonApiServer.class);

    /** */
    private static final AccountingService ACCOUNTING_SERVICE =
            ServiceContext.getServiceFactory().getAccountingService();

    /** */
    private static final AccountVoucherService ACCOUNT_VOUCHER_SERVICE =
            ServiceContext.getServiceFactory().getAccountVoucherService();

    /** */
    private static final EmailService EMAIL_SERVICE =
            ServiceContext.getServiceFactory().getEmailService();

    /**
     *
     */
    private static final DocLogService DOC_LOG_SERVICE =
            ServiceContext.getServiceFactory().getDocLogService();

    /** */
    private static final InboxService INBOX_SERVICE =
            ServiceContext.getServiceFactory().getInboxService();

    /** */
    private static final JobTicketService JOBTICKET_SERVICE =
            ServiceContext.getServiceFactory().getJobTicketService();

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private static final JsonApiDict API_DICTIONARY = new JsonApiDict();

    /**
     * Applies the requested locale to the session locale. When the request does
     * not match an available language the {@link Locale#US} is applied..
     */
    private void applyLocaleToSession() {

        final String currentLanguage = getSession().getLocale().getLanguage();
        for (final Locale locale : LocaleHelper.getAvailableLanguages()) {
            if (locale.getLanguage().equals(currentLanguage)) {
                return;
            }
        }
        getSession().setLocale(Locale.US);
    }

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
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

        final WebAppTypeEnum requestingWebAppType =
                EnumUtils.getEnum(WebAppTypeEnum.class, getParmValue(parameters,
                        isGetAction, JsonApiDict.PARM_WEBAPP_TYPE));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Request: " + requestId);
        }

        /*
         * Get the user from the session.
         */
        final boolean isInternalAdmin = (requestingUser != null)
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
        boolean dbClaimLockDone = false;

        JsonApiDict.LetterheadLock letterheadLock =
                JsonApiDict.LetterheadLock.NONE;

        /*
         *
         */
        applyLocaleToSession();

        ServiceContext.open();

        ServiceContext.setLocale(getSession().getLocale());

        final DaoContext daoContext = ServiceContext.getDaoContext();

        try {
            /*
             * Is the request valid and the requesting user authorized for the
             * request?
             */
            Map<String, Object> returnData = checkValidAndAuthorized(requestId,
                    requestingUser, requestingWebAppType);

            final boolean isValidAndAuthorizedRequest = returnData == null;

            if (isValidAndAuthorizedRequest) {

                /*
                 * Application level locks ...
                 */
                letterheadLock = API_DICTIONARY.getLetterheadLockNeeded(
                        requestId, SpSession.get().isAdmin());

                dbClaim = API_DICTIONARY.getDbClaimNeeded(requestId,
                        requestingWebAppType);

                API_DICTIONARY.lock(letterheadLock, requestId, requestingUser);

                API_DICTIONARY.lock(dbClaim, requestId, requestingUser);
                dbClaimLockDone = true;

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
                case JsonApiDict.REQ_PDF_JOBTICKET:
                    // no break intended
                case JsonApiDict.REQ_PDF_OUTBOX:
                    // no break intended
                case JsonApiDict.REQ_PDF_DOCSTORE_ARCHIVE:
                    // no break intended
                case JsonApiDict.REQ_PDF_DOCSTORE_JOURNAL:
                    // no break intended
                case JsonApiDict.REQ_POS_RECEIPT_DOWNLOAD:
                    // no break intended
                case JsonApiDict.REQ_POS_RECEIPT_DOWNLOAD_USER:
                    // no break intended
                case JsonApiDict.REQ_REPORT:
                    // no break intended
                case JsonApiDict.REQ_REPORT_USER:
                    // no break intended
                case JsonApiDict.REQ_PAPERCUT_DELEGATOR_COST_CSV:
                    // no break intended
                case JsonApiDict.REQ_PRINTER_OPT_DOWNLOAD:
                    // no break intended
                case JsonApiDict.REQ_PRINTER_PPD_DOWNLOAD:
                    // no break intended
                case JsonApiDict.REQ_SMARTSCHOOL_PAPERCUT_STUDENT_COST_CSV:
                    // no break intended
                case JsonApiDict.REQ_USER_EXPORT_DATA_HISTORY:
                    handleExportFile(requestId, parameters, requestingUser,
                            isGetAction);
                    break;

                case JsonApiDict.REQ_PDF:
                    requestCycle.scheduleRequestHandlerAfterCurrent(
                            this.handleExportSafePages(lockedUser, parameters,
                                    isGetAction));
                    commitDbTransaction = true;
                    break;

                default:
                    returnData = handleRequest(requestId, parameters,
                            isGetAction, requestingUser, lockedUser);

                    commitDbTransaction = (returnData != null
                            && !isApiResultError(returnData));
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

                    switch (getSessionWebAppType()) {
                    case ADMIN:
                        urlPath = WebApp.MOUNT_PATH_WEBAPP_ADMIN;
                        break;
                    case PRINTSITE:
                        urlPath = WebApp.MOUNT_PATH_WEBAPP_PRINTSITE;
                        break;
                    case JOBTICKETS:
                        urlPath = WebApp.MOUNT_PATH_WEBAPP_JOBTICKETS;
                        break;
                    case POS:
                        urlPath = WebApp.MOUNT_PATH_WEBAPP_POS;
                        break;
                    default:
                        urlPath = WebApp.MOUNT_PATH_WEBAPP_USER;
                        break;
                    }

                    final StringBuilder html = new StringBuilder();

                    html.append("<h2 style='color: red;'>")
                            .append(CommunityDictEnum.SAVAPAGE.getWord())
                            .append(" Authorisation Error</h2>"
                                    + "<p>Please login again.</p>"
                                    + "<p><form action='")
                            .append(urlPath)
                            .append("'>" + "<input type='submit'value='Login'/>"
                                    + "</form></p>");

                    requestCycle.scheduleRequestHandlerAfterCurrent(
                            new TextRequestHandler("text/html", "UTF-8",
                                    html.toString()));
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

                final Map<String, Object> apiRes;

                if (t instanceof ReadLockObtainFailedException) {

                    apiRes = setApiResultTxt(new HashMap<String, Object>(),
                            ApiResultCodeEnum.UNAVAILABLE,
                            PhraseEnum.SYS_TEMP_UNAVAILABLE
                                    .uiText(ServiceContext.getLocale()));

                    if (!dbClaimLockDone) {
                        // dbClaim locking failed or was not executed: reset to
                        // NONE to prevent error when unlocking later on.
                        dbClaim = JsonApiDict.DbClaim.NONE;
                    }

                } else {
                    apiRes = handleException(requestId, requestingUser, t);
                }

                jsonArray = new ObjectMapper().writeValueAsString(apiRes);

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
                    jsonArray = new ObjectMapper().writeValueAsString(
                            handleException(requestId, requestingUser, ex));
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

            requestCycle.scheduleRequestHandlerAfterCurrent(
                    new TextRequestHandler(JsonRpcConfig.INTERNET_MEDIA_TYPE,
                            JsonRpcConfig.CHAR_ENCODING, jsonArray));
        }

        PerformanceLogger.log(this.getClass(), "constructor", perfStartTime,
                requestId);
    }

    /**
     * Handles export request.
     *
     * @param request
     * @param parameters
     * @param requestingUser
     * @param isGetAction
     */
    private void handleExportFile(final String request,
            final PageParameters parameters, final String requestingUser,
            final boolean isGetAction) {

        final String baseName =
                "export-" + UUID.randomUUID().toString() + ".tmp";

        final String fileName = ConfigManager.getAppTmpDir() + "/" + baseName;

        final File tempExportFile = new File(fileName);

        IRequestHandler requestHandler = null;

        try {

            switch (request) {

            case JsonApiDict.REQ_ACCOUNT_VOUCHER_BATCH_PRINT:
                requestHandler = exportVoucherBatchPrint(tempExportFile,
                        parameters.get(JsonApiDict.PARM_REQ_SUB).toString());
                break;

            case JsonApiDict.REQ_PDF_JOBTICKET:
            case JsonApiDict.REQ_PDF_OUTBOX:
                requestHandler = new ReqExportOutboxPdf(
                        request.equals(JsonApiDict.REQ_PDF_JOBTICKET)).export(
                                getSessionWebAppType(), getRequestCycle(),
                                parameters, isGetAction, requestingUser, null);
                break;

            case JsonApiDict.REQ_PDF_DOCSTORE_ARCHIVE:
                requestHandler =
                        new ReqExportDocStorePdf(DocStoreTypeEnum.ARCHIVE)
                                .export(getSessionWebAppType(),
                                        getRequestCycle(), parameters,
                                        isGetAction, requestingUser, null);
                break;

            case JsonApiDict.REQ_PDF_DOCSTORE_JOURNAL:
                requestHandler =
                        new ReqExportDocStorePdf(DocStoreTypeEnum.JOURNAL)
                                .export(getSessionWebAppType(),
                                        getRequestCycle(), parameters,
                                        isGetAction, requestingUser, null);
                break;

            case JsonApiDict.REQ_POS_RECEIPT_DOWNLOAD:
            case JsonApiDict.REQ_POS_RECEIPT_DOWNLOAD_USER:
                requestHandler = exportPosPurchaseReceipt(tempExportFile,
                        parameters.get(JsonApiDict.PARM_REQ_SUB).toLongObject(),
                        requestingUser,
                        request.equals(JsonApiDict.REQ_POS_RECEIPT_DOWNLOAD));
                break;

            case JsonApiDict.REQ_REPORT:
            case JsonApiDict.REQ_REPORT_USER:

                requestHandler = exportReport(tempExportFile,
                        EnumUtils.getEnum(JrExportFileExtEnum.class,
                                StringUtils.defaultString(
                                        StringUtils.stripToNull(parameters
                                                .get(JsonApiDict.PARM_REQ_PARM)
                                                .toString()),
                                        JrExportFileExtEnum.PDF.toString())),
                        parameters.get(JsonApiDict.PARM_REQ_SUB).toString(),
                        parameters.get(JsonApiDict.PARM_DATA).toString(),
                        requestingUser, request.equals(JsonApiDict.REQ_REPORT));
                break;

            case JsonApiDict.REQ_PAPERCUT_DELEGATOR_COST_CSV:
                requestHandler = exportPapercutDelegatorCost(
                        ConfigManager.instance().getConfigValue(
                                Key.PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_PERSONAL_TYPE),
                        "papercut-delegator-cost.csv", tempExportFile,
                        parameters.get(JsonApiDict.PARM_REQ_SUB).toString(),
                        requestingUser);
                break;

            case JsonApiDict.REQ_PRINTER_OPT_DOWNLOAD:
                requestHandler = new ReqExportPrinterOpt(
                        parameters.get(JsonApiDict.PARM_REQ_SUB).toLongObject())
                                .export(getSessionWebAppType(),
                                        getRequestCycle(), parameters,
                                        isGetAction, requestingUser, null);
                break;

            case JsonApiDict.REQ_PRINTER_PPD_DOWNLOAD:
                requestHandler = new ReqExportPrinterPpd(
                        parameters.get(JsonApiDict.PARM_REQ_SUB).toLongObject())
                                .export(getSessionWebAppType(),
                                        getRequestCycle(), parameters,
                                        isGetAction, requestingUser, null);
                break;

            case JsonApiDict.REQ_SMARTSCHOOL_PAPERCUT_STUDENT_COST_CSV:
                requestHandler = exportPapercutDelegatorCost(
                        ConfigManager.instance().getConfigValue(
                                Key.SMARTSCHOOL_PAPERCUT_ACCOUNT_PERSONAL_TYPE),
                        "smartschool-papercut-student-cost.csv", tempExportFile,
                        parameters.get(JsonApiDict.PARM_REQ_SUB).toString(),
                        requestingUser);
                break;

            case JsonApiDict.REQ_USER_EXPORT_DATA_HISTORY:
                requestHandler = new ReqExportUserDataHistory().export(
                        getSessionWebAppType(), getRequestCycle(), parameters,
                        isGetAction, requestingUser, null);
                break;

            default:
                throw new SpException(
                        "request [" + request + "] not supported");
            }

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);

            if (tempExportFile != null && tempExportFile.exists()) {
                tempExportFile.delete();
            }

            requestHandler = new TextRequestHandler("text/html", "UTF-8",
                    "<h2 style='color: red;'>" + e.getClass().getSimpleName()
                            + "</h2><p>" + e.getMessage() + "</p>");
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
     * @param requestingUserAdmin
     *            {@code true} if requesting user is an administrator.
     * @return The {@link PosDepositReceiptDto} with all the deposit
     *         information.
     * @throws JRException
     *             When JasperReport error
     */
    private PosDepositReceiptDto createPosPurchaseReceipt(final File pdfFile,
            final Long accountTrxDbId, final String requestingUser,
            final boolean requestingUserAdmin) throws JRException {

        final Locale reportLocale = ConfigManager.getDefaultLocale();

        final PosDepositReceiptDto receipt =
                ACCOUNTING_SERVICE.createPosDepositReceiptDto(accountTrxDbId);

        /*
         * INVARIANT: A user can only create his own receipts.
         */
        if (!requestingUserAdmin
                && !receipt.getUserId().equals(requestingUser)) {
            throw new SpException("User [" + requestingUser
                    + "] is not authorized to access receipt of user ["
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

        final JasperReport jasperReport = JasperCompileManager
                .compileReport(JrPosDepositReceipt.create(pageSizeDto,
                        ConfigManager.getConfigFontFamily(
                                Key.REPORTS_PDF_INTERNAL_FONT_FAMILY),
                        reportLocale));

        //
        final JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                JrPosDepositReceipt.getParameters(receipt, reportLocale),
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
     * @param isAdminRequest
     *            {@code true} if this is a request from an administrator.
     * @throws JRException
     * @throws MessagingException
     * @throws IOException
     * @throws CircuitBreakerException
     * @throws InterruptedException
     * @throws PGPBaseException
     */
    private void mailDepositReceipt(final Long accountTrxDbId,
            final String toAddress, final String requestingUser,
            boolean isAdminRequest)
            throws JRException, MessagingException, IOException,
            InterruptedException, CircuitBreakerException, PGPBaseException {

        final File tempPdfFile = new File(OutputProducer
                .createUniqueTempPdfName(requestingUser, "deposit-receipt"));
        try {

            final PosDepositReceiptDto receipt =
                    createPosPurchaseReceipt(tempPdfFile, accountTrxDbId,
                            requestingUser, isAdminRequest);

            final String subject = localize("msg-deposit-email-subject",
                    receipt.getReceiptNumber());
            final String body = localize("msg-deposit-email-body");

            final EmailMsgParms emailParms = new EmailMsgParms();

            emailParms.setToAddress(toAddress);
            emailParms.setSubject(subject);
            emailParms.setBodyInStationary(subject, body, getLocale(), true);
            emailParms.setFileAttach(tempPdfFile);
            emailParms.setFileName(getUserFriendlyFilename(receipt));

            EMAIL_SERVICE.writeEmail(emailParms);

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
     * @param tempFile
     * @param accountTrxDbId
     * @param requestingUser
     * @param requestingUserAdmin
     *            {@code true} if requesting user is an administrator.
     * @return
     * @throws JRException
     */
    private IRequestHandler exportPosPurchaseReceipt(final File tempFile,
            final Long accountTrxDbId, final String requestingUser,
            final boolean requestingUserAdmin) throws JRException {

        final PosDepositReceiptDto receipt = createPosPurchaseReceipt(tempFile,
                accountTrxDbId, requestingUser, requestingUserAdmin);

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

        final Locale reportlocale = ConfigManager.getDefaultLocale();

        final VoucherBatchPrintDto dto =
                AbstractDto.create(VoucherBatchPrintDto.class, jsonDto);

        final InternalFontFamilyEnum font = ConfigManager
                .getConfigFontFamily(Key.FINANCIAL_VOUCHER_CARD_FONT_FAMILY);

        final JasperDesign design =
                dto.getDesign().createDesign(font, reportlocale);

        final ConfigManager cm = ConfigManager.instance();

        JasperReport jasperReport = JasperCompileManager.compileReport(design);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put(JrVoucherPageDesign.PARM_HEADER,
                cm.getConfigValue(Key.FINANCIAL_VOUCHER_CARD_HEADER));
        parameters.put(JrVoucherPageDesign.PARM_FOOTER,
                cm.getConfigValue(Key.FINANCIAL_VOUCHER_CARD_FOOTER));

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,
                parameters, JrVoucherPageDesign
                        .createDataSource(dto.getBatchId(), reportlocale));

        JasperExportManager.exportReportToPdfFile(jasperPrint,
                tempFile.getAbsolutePath());

        final ResourceStreamRequestHandler handler =
                new DownloadRequestHandler(tempFile);

        final String userFriendlyFilename = CommunityDictEnum.SAVAPAGE.getWord()
                + "-voucher-batch-" + dto.getBatchId() + ".pdf";

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
     * @param tempExportFile
     *            The temporary {@link File}.
     * @param fileExt
     *            The export file type to create.
     * @param reportId
     *            The unique report ID
     * @param jsonData
     *            The JSON data string.
     * @param requestingUser
     *            The requesting user.
     * @param requestingUserAdmin
     *            {@code true} if requesting user is an administrator.
     * @return The {@link IRequestHandler}.
     * @throws JRException
     *             When Jasper Report error.
     */
    private IRequestHandler exportReport(final File tempExportFile,
            final JrExportFileExtEnum fileExt, final String reportId,
            final String jsonData, final String requestingUser,
            final boolean requestingUserAdmin) throws JRException {

        final Locale locale = getSession().getLocale();

        final ReportCreator report;

        if (reportId.equals(AccountTrxListReport.REPORT_ID)) {

            report = new AccountTrxListReport(requestingUser,
                    requestingUserAdmin, jsonData, locale);

        } else if (reportId.equals(UserListReport.REPORT_ID)) {

            report = new UserListReport(requestingUser, requestingUserAdmin,
                    jsonData, locale);

        } else {
            throw new UnsupportedOperationException(
                    "Report [" + reportId + "] is NOT supported");
        }

        report.create(tempExportFile, fileExt);

        final ResourceStreamRequestHandler handler =
                new DownloadRequestHandler(tempExportFile);

        final String userFriendlyFilename = String.format("%s.%s", reportId,
                fileExt.toString().toLowerCase());

        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        handler.setFileName(userFriendlyFilename);
        handler.setCacheDuration(Duration.NONE);

        return handler;
    }

    /**
     * <p>
     * Handles the {@link JsonApiDict#REQ_SMARTSCHOOL_PAPERCUT_STUDENT_COST_CSV}
     * or oor {@link JsonApiDict#REQ_PAPERCUT_DELEGATOR_COST_CSV} request by
     * returning the {@link IRequestHandler}.
     * </p>
     *
     * @param personalAccountType
     *            The value of
     *            {@link Key#PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_PERSONAL_TYPE}
     *            or {@link Key#SMARTSCHOOL_PAPERCUT_ACCOUNT_PERSONAL_TYPE}.
     * @param csvFileName
     *            The name of the CSV file as shown to the user.
     * @param tempCsvFile
     *            The temporary CSV {@link File}.
     * @param jsonData
     *            JSON data.
     * @param requestingUser
     *            The requesting user.
     * @return The {@link IRequestHandler}.
     * @throws IOException
     *             When IO error.
     */
    private IRequestHandler exportPapercutDelegatorCost(
            final String personalAccountType, final String csvFileName,
            final File tempCsvFile, final String jsonData,
            final String requestingUser) throws IOException {

        final DelegatedPrintPeriodDto dto =
                AbstractDto.create(DelegatedPrintPeriodDto.class, jsonData);

        dto.setPersonalAccountType(personalAccountType);

        ServiceContext.getServiceFactory().getPaperCutService()
                .createDelegatorPrintCostCsv(tempCsvFile, dto);

        final DownloadRequestHandler handler =
                new DownloadRequestHandler(tempCsvFile);

        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        handler.setFileName(csvFileName);
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

        final boolean removeGraphics = Boolean.parseBoolean(
                getParmValue(parameters, isGetAction, "removeGraphics"));

        final boolean ecoPrint = Boolean.parseBoolean(
                getParmValue(parameters, isGetAction, "ecoprint"));

        if (removeGraphics && ecoPrint) {
            return new TextRequestHandler("text/html", "UTF-8",
                    "<h3 style='color: gray;'>"
                            + localize("msg-select-single-pdf-filter")
                            + "</h3>");
        }

        if (USER_SERVICE.isUserPdfOutDisabled(lockedUser, new Date())) {
            return new TextRequestHandler("text/html", "UTF-8",
                    "<h2 style='color: red;'>"
                            + localize("msg-user-pdf-out-disabled") + "</h2>");
        }

        IRequestHandler requestHandler = null;

        File pdfTemp = null;

        try {

            final boolean grayscalePdf = Boolean.parseBoolean(
                    getParmValue(parameters, isGetAction, "grayscale"));

            final DocLog docLog = new DocLog();

            /*
             * (1) Generate the PDF
             */
            pdfTemp = generatePdfForExport(lockedUser,
                    Integer.parseInt(
                            getParmValue(parameters, isGetAction, "jobIndex")),
                    getParmValue(parameters, isGetAction, "ranges"),
                    removeGraphics, ecoPrint, grayscalePdf, docLog, "download");

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
                    new PdfFileRequestHandler(lockedUser.getUserId(), pdfTemp);

            handler.setContentDisposition(ContentDisposition.ATTACHMENT);
            handler.setFileName(createMeaningfullPdfFileName(docLog));
            handler.setCacheDuration(Duration.NONE);

            requestHandler = handler;

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);

            if (pdfTemp != null && pdfTemp.exists()) {
                pdfTemp.delete();
            }

            String msg = null;

            if (e instanceof LetterheadNotFoundException) {
                msg = localize("exc-letterhead-not-found-login");
            } else {
                msg = e.getMessage();
            }

            requestHandler = new TextRequestHandler("text/html", "UTF-8",
                    "<h2 style='color: red;'>" + msg + "</h2>");
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
            return reqVoucherBatchCreate(
                    getParmValue(parameters, isGetAction, "dto"));

        case JsonApiDict.REQ_ACCOUNT_VOUCHER_BATCH_DELETE:
            return reqVoucherBatchDelete(
                    getParmValue(parameters, isGetAction, "batch"));

        case JsonApiDict.REQ_ACCOUNT_VOUCHER_BATCH_EXPIRE:
            return reqVoucherBatchExpire(
                    getParmValue(parameters, isGetAction, "batch"));

        case JsonApiDict.REQ_ACCOUNT_VOUCHER_DELETE_EXPIRED:
            return reqVoucherDeleteExpired();

        case JsonApiDict.REQ_ACCOUNT_VOUCHER_REDEEM:
            return reqVoucherRedeem(requestingUser,
                    getParmValue(parameters, isGetAction, "cardNumber"));

        case JsonApiDict.REQ_BITCOIN_WALLET_REFRESH:
            return reqBitcoinWalletRefresh();

        case JsonApiDict.REQ_CARD_IS_REGISTERED:
            return reqCardIsRegistered(
                    getParmValue(parameters, isGetAction, "card"));

        case JsonApiDict.REQ_CONSTANTS:

            return reqConstants(
                    EnumUtils.getEnum(WebAppTypeEnum.class,
                            getParmValue(parameters, isGetAction,
                                    "webAppType")),
                    getParmValue(parameters, isGetAction, "authMode"));

        case JsonApiDict.REQ_DEVICE_NEW_CARD_READER:

            return reqDeviceNew(DeviceTypeEnum.CARD_READER);

        case JsonApiDict.REQ_DEVICE_NEW_TERMINAL:

            return reqDeviceNew(DeviceTypeEnum.TERMINAL);

        case JsonApiDict.REQ_EXIT_EVENT_MONITOR:

            return reqExitEventMonitor(requestingUser);

        case JsonApiDict.REQ_LANGUAGE:

            String language = getParmValue(parameters, isGetAction, "language");
            String country = getParmValue(parameters, isGetAction, "country");

            if (language == null || language.trim().isEmpty()) {

                language = getSession().getLocale().getLanguage();
                country = getSession().getLocale().getCountry();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format(
                            "using default language [%s] country [%s] "
                                    + "for user [%s]",
                            language, country, requestingUser));
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("language [%s] country [%s]",
                            language, country));
                }
            }
            return reqLanguage(language, country);

        case JsonApiDict.REQ_WEBAPP_UNLOAD:

            SpSession.get().decrementAuthWebApp();
            return createApiResultOK();

        case JsonApiDict.REQ_WEBAPP_CLOSE_SESSION:

            return reqWebAppCloseSession();

        case JsonApiDict.REQ_IMAP_TEST:

            return reqImapTest();

        case JsonApiDict.REQ_IMAP_START:

            return reqImapStart();

        case JsonApiDict.REQ_IMAP_STOP:

            return reqImapStop();

        case JsonApiDict.REQ_PAPERCUT_TEST:

            return reqPaperCutTest();

        case JsonApiDict.REQ_PAYMENT_GATEWAY_ONLINE:

            return reqPaymentGatewayOnline(
                    Boolean.parseBoolean(
                            getParmValue(parameters, isGetAction, "bitcoin")),
                    Boolean.parseBoolean(
                            getParmValue(parameters, isGetAction, "online")));

        case JsonApiDict.REQ_SMARTSCHOOL_TEST:

            return reqSmartSchoolTest();

        case JsonApiDict.REQ_SMARTSCHOOL_START:

            return reqSmartSchoolStart(false);

        case JsonApiDict.REQ_SMARTSCHOOL_START_SIMULATE:

            return reqSmartSchoolStart(true);

        case JsonApiDict.REQ_SMARTSCHOOL_STOP:

            return reqSmartSchoolStop();

        case JsonApiDict.REQ_USER_CREDIT_TRANSFER:

            return apiResultFromBasicRpcResponse(ACCOUNTING_SERVICE
                    .transferUserCredit(JsonAbstractBase.create(
                            UserCreditTransferDto.class,
                            getParmValue(parameters, isGetAction, "dto"))));

        case JsonApiDict.REQ_USER_MONEY_TRANSFER_REQUEST:

            return reqUserMoneyTransfer(requestingUser,
                    getParmValue(parameters, isGetAction, "dto"));

        case JsonApiDict.REQ_POS_DEPOSIT:

            return reqPosDeposit(requestingUser,
                    getParmValue(parameters, isGetAction, "dto"));

        case JsonApiDict.REQ_POS_RECEIPT_SENDMAIL:

            return reqPosReceiptSendMail(requestingUser,
                    getParmValue(parameters, isGetAction, "dto"));

        case JsonApiDict.REQ_RESET_ADMIN_PASSWORD:

            return reqAdminPasswordReset(
                    getParmValue(parameters, isGetAction, "password"));

        case JsonApiDict.REQ_RESET_JMX_PASSWORD:

            return reqJmxPasswordReset(
                    getParmValue(parameters, isGetAction, "password"));

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

        case JsonApiDict.REQ_INBOX_IS_VANILLA:

            return reqInboxIsVanilla(requestingUser);

        case JsonApiDict.REQ_INBOX_JOB_DELETE:

            return reqInboxJobDelete(requestingUser, Integer
                    .valueOf(getParmValue(parameters, isGetAction, "ijob")));

        case JsonApiDict.REQ_INBOX_JOB_EDIT:

            return reqInboxJobEdit(requestingUser,
                    Integer.valueOf(
                            getParmValue(parameters, isGetAction, "ijob")),
                    getParmValue(parameters, isGetAction, "data"));

        case JsonApiDict.REQ_INBOX_JOB_PAGES:

            return reqInboxJobPages(requestingUser,
                    getParmValue(parameters, isGetAction, "first-detail-page"),
                    getParmValue(parameters, isGetAction, "unique-url-value"),
                    Boolean.parseBoolean(
                            getParmValue(parameters, isGetAction, "base64")));

        case JsonApiDict.REQ_JQPLOT:

            return reqJqPlot(getParmValue(parameters, isGetAction, "chartType"),
                    Boolean.parseBoolean(
                            getParmValue(parameters, isGetAction, "isGlobal")),
                    mySessionUser);

        case JsonApiDict.REQ_PAGE_DELETE:

            return reqInboxPageDelete(requestingUser,
                    getParmValue(parameters, isGetAction, "ranges"));

        case JsonApiDict.REQ_PAGE_MOVE:

            return reqInboxPageMove(requestingUser,
                    getParmValue(parameters, isGetAction, "ranges"),
                    getParmValue(parameters, isGetAction, "position"));

        case JsonApiDict.REQ_PDF_GET_PROPERTIES:

            return reqPdfPropsGet(mySessionUser);

        case JsonApiDict.REQ_PRINTER_DETAIL:

            return reqPrinterDetail(requestingUser,
                    getParmValue(parameters, isGetAction, "printer"));

        case JsonApiDict.REQ_PRINT_AUTH_CANCEL:
            return reqPrintAuthCancel(
                    Long.parseLong(
                            getParmValue(parameters, isGetAction, "idUser")),
                    getParmValue(parameters, isGetAction, "printer"));

        case JsonApiDict.REQ_PRINT_FAST_RENEW:
            return reqPrintFastRenew(requestingUser);

        case JsonApiDict.REQ_PRINTER_RENAME:

            return reqPrinterRename(requestingUser,
                    getParmValue(parameters, isGetAction, "j_printer"));

        case JsonApiDict.REQ_PRINTER_SET_MEDIA_COST:

            return reqPrinterSetMediaCost(
                    getParmValue(parameters, isGetAction, "j_cost"));

        case JsonApiDict.REQ_LETTERHEAD_LIST:

            return setApiResultOK(
                    INBOX_SERVICE.getLetterheadList(mySessionUser));

        case JsonApiDict.REQ_LETTERHEAD_ATTACH:

            INBOX_SERVICE.attachLetterhead(mySessionUser,
                    getParmValue(parameters, isGetAction, "id"),
                    Boolean.parseBoolean(
                            getParmValue(parameters, isGetAction, "pub")));
            return createApiResultOK();

        case JsonApiDict.REQ_LETTERHEAD_DELETE:

            return reqLetterheadDelete(mySessionUser,
                    getParmValue(parameters, isGetAction, "id"),
                    Boolean.parseBoolean(
                            getParmValue(parameters, isGetAction, "pub")));

        case JsonApiDict.REQ_LETTERHEAD_DETACH:

            INBOX_SERVICE.detachLetterhead(requestingUser);
            return createApiResultOK();

        case JsonApiDict.REQ_LETTERHEAD_NEW:

            try {
                INBOX_SERVICE.createLetterhead(mySessionUser);
                return createApiResultOK();
            } catch (PostScriptDrmException e) {
                return setApiResult(new HashMap<String, Object>(),
                        ApiResultCodeEnum.ERROR, "msg-pdf-export-drm-error");
            }

        case JsonApiDict.REQ_LETTERHEAD_GET:

            return setApiResultOK(INBOX_SERVICE.getLetterheadDetails(
                    mySessionUser, getParmValue(parameters, isGetAction, "id"),
                    Boolean.parseBoolean(
                            getParmValue(parameters, isGetAction, "pub")),
                    Boolean.parseBoolean(
                            getParmValue(parameters, isGetAction, "base64"))));

        case JsonApiDict.REQ_LETTERHEAD_SET:

            return reqLetterheadSet(mySessionUser,
                    getParmValue(parameters, isGetAction, "id"),
                    getParmValue(parameters, isGetAction, "data"));

        case JsonApiDict.REQ_GCP_GET_DETAILS:

            return reqGcpGetDetails();

        case JsonApiDict.REQ_GCP_ONLINE:

            return reqGcpOnline(mySessionUser, Boolean.parseBoolean(
                    getParmValue(parameters, isGetAction, "online")));

        case JsonApiDict.REQ_GCP_REGISTER:

            return reqGcpRegister(mySessionUser,
                    getParmValue(parameters, isGetAction, "clientId"),
                    getParmValue(parameters, isGetAction, "clientSecret"),
                    getParmValue(parameters, isGetAction, "printerName"));

        case JsonApiDict.REQ_GCP_SET_DETAILS:

            return reqGcpSetDetails(mySessionUser,
                    Boolean.parseBoolean(
                            getParmValue(parameters, isGetAction, "enabled")),
                    getParmValue(parameters, isGetAction, "clientId"),
                    getParmValue(parameters, isGetAction, "clientSecret"),
                    getParmValue(parameters, isGetAction, "printerName"));

        case JsonApiDict.REQ_GCP_SET_NOTIFICATIONS:

            return reqGcpSetNotifications(mySessionUser,
                    Boolean.parseBoolean(
                            getParmValue(parameters, isGetAction, "enabled")),
                    getParmValue(parameters, isGetAction, "emailSubject"),
                    getParmValue(parameters, isGetAction, "emailBody"));

        case JsonApiDict.REQ_GET_EVENT:

            return reqGetEvent(requestingUser,
                    getParmValue(parameters, isGetAction, "page-offset"),
                    getParmValue(parameters, isGetAction, "unique-url-value"),
                    Boolean.parseBoolean(
                            getParmValue(parameters, isGetAction, "base64")));

        case JsonApiDict.REQ_PING:

            return createApiResultOK();

        case JsonApiDict.REQ_SEND:

            return reqSend(lockedUser,
                    getParmValue(parameters, isGetAction, "mailto"),
                    getParmValue(parameters, isGetAction, "jobIndex"),
                    getParmValue(parameters, isGetAction, "ranges"),
                    Boolean.parseBoolean(getParmValue(parameters, isGetAction,
                            "removeGraphics")),
                    Boolean.parseBoolean(
                            getParmValue(parameters, isGetAction, "ecoprint"))
                    //
                    , Boolean.parseBoolean(getParmValue(parameters, isGetAction,
                            "grayscale")));

        case JsonApiDict.REQ_USER_LAZY_ECOPRINT:

            return reqUserLazyEcoPrint(lockedUser,
                    Integer.parseInt(
                            getParmValue(parameters, isGetAction, "jobIndex")),
                    getParmValue(parameters, isGetAction, "ranges"));

        case JsonApiDict.REQ_USER_DELETE:

            return reqUserDelete(getParmValue(parameters, isGetAction, "id"),
                    getParmValue(parameters, isGetAction, "userid"));

        case JsonApiDict.REQ_USER_GET_STATS:

            return reqUserGetStats(requestingUser);

        case JsonApiDict.REQ_USER_SOURCE_GROUPS:

            return reqUserSourceGroups(requestingUser);

        case JsonApiDict.REQ_USER_SYNC:

            return reqUserSync(requestingUser,
                    getParmValue(parameters, isGetAction, "test")
                            .equalsIgnoreCase("Y"),
                    getParmValue(parameters, isGetAction, "delete-users")
                            .equalsIgnoreCase("Y"));

        default:

            final ApiRequestHandler apiReqHandler =
                    API_DICTIONARY.createApiRequest(request);

            if (apiReqHandler == null) {
                throw new SpException(
                        "Request [" + request + "] NOT supported");
            }

            return apiReqHandler.process(RequestCycle.get(), parameters,
                    isGetAction, requestingUser, lockedUser);
        }

    }

    /**
     *
     * @param user
     *            The {@link User}.
     * @param vanillaJobIndex
     *            The zero-based index of the vanilla job. If this index is LT
     *            zero the pageRangeFilter refers to the integrated document.
     * @param pageRangeFilter
     *            The page range filter. For example: '1,2,5-6'. The page
     *            numbers in page range filter refer to one-based page numbers
     *            of the integrated {@link InboxInfoDto} document OR for a
     *            single vanilla job. When {@code null}, then the full page
     *            range is applied.
     * @param removeGraphics
     *            If <code>true</code> graphics are removed (minified to
     *            one-pixel).
     * @param ecoPdf
     *            <code>true</code> if Eco PDF is to be generated.
     * @param grayscalePdf
     *            <code>true</code> if Grayscale PDF is to be generated.
     * @param docLog
     * @param purpose
     *            A simple tag to insert into the filename (to add some
     *            meaning).
     * @return The generated PDF file.
     * @throws PostScriptDrmException
     * @throws IOException
     * @throws LetterheadNotFoundException
     * @throws EcoPrintPdfTaskPendingException
     *             When {@link EcoPrintPdfTask} objects needed for this PDF are
     *             pending.
     */
    private File generatePdfForExport(final User user,
            final int vanillaJobIndex, final String pageRangeFilter,
            final boolean removeGraphics, final boolean ecoPdf,
            final boolean grayscalePdf, final DocLog docLog,
            final String purpose)
            throws LetterheadNotFoundException, IOException,
            PostScriptDrmException, EcoPrintPdfTaskPendingException {

        final String pdfFile =
                OutputProducer.createUniqueTempPdfName(user, purpose);

        final String documentPageRangeFilter = calcDocumentPageRangeFilter(user,
                vanillaJobIndex, pageRangeFilter);

        /*
         * Get the (filtered) jobs.
         */
        InboxInfoDto inboxInfo = INBOX_SERVICE.getInboxInfo(user.getUserId());

        if (StringUtils.isNotBlank(documentPageRangeFilter)) {
            inboxInfo = INBOX_SERVICE.filterInboxInfoPages(inboxInfo,
                    documentPageRangeFilter);
        }

        final PdfCreateRequest pdfRequest = new PdfCreateRequest();

        pdfRequest.setUserObj(user);
        pdfRequest.setPdfFile(pdfFile);
        pdfRequest.setInboxInfo(inboxInfo);
        pdfRequest.setRemoveGraphics(removeGraphics);
        pdfRequest.setApplyPdfProps(true);
        pdfRequest.setApplyLetterhead(true);
        pdfRequest.setForPrinting(false);
        pdfRequest.setEcoPdfShadow(ecoPdf);
        pdfRequest.setGrayscale(grayscalePdf);

        final ConfigManager cm = ConfigManager.instance();

        final PdfPgpVerifyUrl verifyUrl;

        if (ConfigManager.isPdfPgpAvailable()) {

            String host = cm.getConfigValue(Key.PDFPGP_VERIFICATION_HOST);
            if (StringUtils.isBlank(host)) {
                host = InetUtils.getServerHostAddress();
            }
            verifyUrl = new PdfPgpVerifyUrl(host,
                    cm.getConfigInteger(Key.PDFPGP_VERIFICATION_PORT));
        } else {
            verifyUrl = null;
        }

        pdfRequest.setVerifyUrl(verifyUrl);

        return OutputProducer.instance().generatePdfForExport(pdfRequest,
                docLog);
    }

    /**
     * Calculates the integrated document page filter for a {@link User}.
     *
     * @param user
     *            The {@link User}.
     * @param vanillaJobIndex
     *            The zero-based index of the vanilla job. If this index is LT
     *            zero the pageRangeFilter refers to the integrated document.
     * @param pageRangeFilter
     *            The page range filter. For example: '1,2,5-6'. The page
     *            numbers in page range filter refer to one-based page numbers
     *            of the integrated {@link InboxInfoDto} document OR for a
     *            single vanilla job. When {@code null}, then the full page
     *            range is applied.
     * @return The integrated document page filter.
     */
    private String calcDocumentPageRangeFilter(final User user,
            final int vanillaJobIndex, final String pageRangeFilter) {

        final String documentPageRangeFilter;

        if (vanillaJobIndex < 0) {

            documentPageRangeFilter = pageRangeFilter;

        } else {
            /*
             * Convert job scope to inbox scope.
             */
            final InboxInfoDto jobInfo =
                    INBOX_SERVICE.readInboxInfo(user.getUserId());

            documentPageRangeFilter = INBOX_SERVICE.toVanillaJobInboxRange(
                    jobInfo, vanillaJobIndex,
                    INBOX_SERVICE.createSortedRangeArray(pageRangeFilter));
        }

        return documentPageRangeFilter;
    }

    /**
     * Lazy executes background tasks for creating shadow EcoPrint PDFs.
     * <p>
     * See {@link EcoPrintPdfTask}.
     * </p>
     *
     * @param lockedUser
     * @param vanillaJobIndex
     *            The zero-based index of the vanilla job. If this index is LT
     *            zero the pageRangeFilter refers to the integrated document.
     * @param pageRangeFilter
     *            The page range filter. For example: '1,2,5-6'. The page
     *            numbers in page range filter refer to one-based page numbers
     *            of the integrated {@link InboxInfoDto} document OR for a
     *            single vanilla job. When {@code null}, then the full page
     *            range is applied.
     * @return
     */
    private Map<String, Object> reqUserLazyEcoPrint(final User lockedUser,
            final int vanillaJobIndex, final String pageRangeFilter) {

        final String documentPageRangeFilter = calcDocumentPageRangeFilter(
                lockedUser, vanillaJobIndex, pageRangeFilter);
        /*
         * Get the (filtered) jobs.
         */
        InboxInfoDto inboxInfo =
                INBOX_SERVICE.getInboxInfo(lockedUser.getUserId());

        if (StringUtils.isNotBlank(documentPageRangeFilter)) {
            inboxInfo = INBOX_SERVICE.filterInboxInfoPages(inboxInfo,
                    documentPageRangeFilter);
        }

        final int nTasksWaiting = INBOX_SERVICE.lazyStartEcoPrintPdfTasks(
                ConfigManager.getUserHomeDir(lockedUser.getUserId()),
                inboxInfo);

        if (nTasksWaiting > 0) {
            return setApiResult(new HashMap<String, Object>(),
                    ApiResultCodeEnum.INFO, "msg-ecoprint-pending");
        }

        return createApiResultOK();
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
    private boolean isApiResultError(final Map<String, Object> data) {
        return isApiResultCode(data, ApiResultCodeEnum.ERROR);
    }

    /**
     *
     * @param data
     * @param code
     *            The {@link #ApiResultCodeEnum}.
     * @return
     */
    private boolean isApiResultCode(final Map<String, Object> data,
            final ApiResultCodeEnum code) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) data.get("result");
        return result.get("code").equals(code.getValue());
    }

    /**
     * Creates the API result on parameter {@code out}.
     *
     * @param out
     *            The Map to put the result on.
     * @param code
     *            The {@link #ApiResultCodeEnum}.
     * @param msg
     *            The key of the message
     * @param txt
     *            The message text.
     * @return the {@code out} parameter.
     *
     */
    private static Map<String, Object> createApiResult(
            final Map<String, Object> out, final ApiResultCodeEnum code,
            final String msg, final String txt) {
        return ApiRequestMixin.createApiResult(out, code, msg, txt);
    }

    /**
     * Sets the {@link #ApiResultCodeEnum.OK} result on parameter {@code out}.
     *
     * @param out
     * @return the {@code out} parameter.
     */
    private Map<String, Object> setApiResultOK(final Map<String, Object> out) {
        return createApiResult(out, ApiResultCodeEnum.OK, null, null);
    }

    /**
     * Creates and {@link #ApiResultCodeEnum.OK} API result.
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
     *            The {@link #ApiResultCodeEnum}.
     * @param key
     *            The key of the message
     * @return the {@code out} parameter.
     *
     */
    private Map<String, Object> setApiResult(final Map<String, Object> out,
            final ApiResultCodeEnum code, final String key) {
        return createApiResult(out, code, key, localize(key));
    }

    /**
     * Sets the API result on parameter {@code out}.
     *
     * @param out
     *            The Map to put the result on.
     * @param code
     *            The {@link #ApiResultCodeEnum}.
     * @param txt
     *            The message text.
     * @return the {@code out} parameter.
     *
     */
    private Map<String, Object> setApiResultTxt(final Map<String, Object> out,
            final ApiResultCodeEnum code, final String txt) {
        return createApiResult(out, code, null, txt);
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
        return createApiResult(out, ApiResultCodeEnum.ERROR, key, msg);
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
        return createApiResult(out, ApiResultCodeEnum.OK, key, msg);
    }

    /**
     * Set the API result on parameter {@code out}.
     *
     * @param out
     *            The Map to put the result on.
     * @param code
     *            The {@link #ApiResultCodeEnum}.
     * @param key
     *            The key of the message
     * @param args
     *            The placeholder arguments for the message.
     * @return the {@code out} parameter.
     */
    private Map<String, Object> setApiResult(final Map<String, Object> out,
            final ApiResultCodeEnum code, final String key,
            final String... args) {
        return createApiResult(out, code, key, localize(key, args));
    }

    /**
     *
     * @param requestId
     * @param requestingUser
     * @param exception
     * @return
     */
    private Map<String, Object> handleException(final String requestId,
            final String requestingUser, final Exception exception) {

        String msg = null;
        String msgKey = null;

        if (exception instanceof LetterheadNotFoundException) {
            msgKey = "exc-letterhead-not-found";
            msg = localize(msgKey);
        }

        final boolean isShutdown = ConfigManager.isShutdownInProgress();

        if (msgKey == null) {

            msgKey = "msg-exception";
            msg = exception.getClass().getSimpleName() + ": "
                    + exception.getMessage();

            if (!isShutdown) {
                if (exception instanceof LockAcquisitionException
                        || exception instanceof PessimisticLockException) {
                    LOGGER.error(exception.getMessage());
                } else {
                    LOGGER.error(String.format("[%s][%s][%s]: %s", requestId,
                            StringUtils.defaultString(requestingUser, "-"),
                            this.createUserAgentHelper().getUserAgentHeader(),
                            exception.getMessage()), exception);
                }
            }

        } else {

            if (!isShutdown) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(exception.getMessage());
                }
            }
        }

        return createApiResult(new HashMap<String, Object>(),
                ApiResultCodeEnum.ERROR, msgKey, msg);
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
            setApiResult(userData, ApiResultCodeEnum.ERROR,
                    "msg-need-admin-rights");
            return false;
        }
        return true;
    }

    /**
     * Checks if session is logically expired, and touches the validated time.
     *
     * @param session
     *            The current session.
     * @return {@code true} when current session is logically expired.
     */
    private boolean checkTouchSessionExpired(final SpSession session) {

        // Mantis #1048
        if (ApiRequestHelper.isAuthTokenLoginEnabled()) {
            // no code intended.
        }

        if (session == null) {
            return true;
        }

        final IConfigProp.Key configKey;

        if (session.getWebAppType() == WebAppTypeEnum.ADMIN) {
            configKey = IConfigProp.Key.WEB_LOGIN_ADMIN_SESSION_TIMEOUT_MINS;
        } else {
            configKey = IConfigProp.Key.WEB_LOGIN_USER_SESSION_TIMEOUT_MINS;
        }

        final int minutes = ConfigManager.instance().getConfigInt(configKey);

        return session
                .checkTouchExpired(DateUtil.DURATION_MSEC_MINUTE * minutes);
    }

    /**
     * Checks if the request is valid, the Web App is right, and the user is
     * authorized to perform the API.
     *
     * @param request
     *            The request id.
     * @param uid
     *            The requesting user id.
     * @param webAppType
     *            The requested {@link WebAppTypeEnum}.
     * @return {@code null} if user is authorized, otherwise a Map object
     *         containing the error message.
     * @throws IOException
     *             When IO error.
     */
    private Map<String, Object> checkValidAndAuthorized(final String request,
            final String uid, final WebAppTypeEnum webAppType)
            throws IOException {

        Map<String, Object> userData = null;

        if (!API_DICTIONARY.isValidRequest(request)) {
            userData = new HashMap<String, Object>();
            return setApiResult(userData, ApiResultCodeEnum.ERROR,
                    "msg-invalid-request", request);
        }

        if (!API_DICTIONARY.isAuthenticationNeeded(request)) {
            return null;
        }

        final SpSession session = SpSession.get();

        final boolean authorized;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Checking user in session from application ["
                    + session.getApplication().getClass().toString() + "]");
        }

        if (checkTouchSessionExpired(session)) {

            authorized = false;

            final String userId;
            if (session.getUser() == null) {
                userId = null;
            } else {
                userId = session.getUser().getUserId();
            }

            ApiRequestHelper.stopReplaceSession(session, userId,
                    this.getRemoteAddr());

        } else if (session.isAuthenticated()) {

            if (webAppType != session.getWebAppType()) {

                authorized = false;

            } else if (session.getUser().getUserId().equals(uid)) {

                if (API_DICTIONARY.isAdminAuthenticationNeeded(request)) {
                    authorized = session.getUser().getAdmin().booleanValue();
                } else {
                    authorized = API_DICTIONARY.isWebAppAuthorized(request,
                            webAppType);
                }

                if (!authorized) {
                    LOGGER.warn(
                            "WebApp [{}] request [{}]: "
                                    + "user [{}] not authorized.",
                            webAppType.toString(), request,
                            session.getUser().getUserId());
                }

            } else {
                authorized = false;

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Request [" + request + "]: user [" + uid
                            + "] is NOT the owner " + "of this session ["
                            + session.getId() + "]");
                }
            }

        } else {

            authorized = false;

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Request [" + request
                        + "]: NO user authenticated in session ["
                        + session.getId() + "]");
            }
        }

        if (authorized) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("User [" + uid + "] is owner of session "
                        + session.getId());
            }

            if (!session.getUser().getAdmin().booleanValue()
                    && ConfigManager.isSysMaintenance()) {
                userData = new HashMap<String, Object>();
                createApiResult(userData, ApiResultCodeEnum.WARN, "",
                        PhraseEnum.SYS_MAINTENANCE.uiText(getLocale()));
            }

        } else {
            userData = new HashMap<String, Object>();
            setApiResult(userData, ApiResultCodeEnum.UNAUTH,
                    "msg-user-not-authorized");
        }

        return userData;
    }

    /**
     *
     * @param lockedUser
     * @param mailto
     * @param jobIndex
     * @param ranges
     * @param removeGraphics
     * @param ecoPdf
     *            <code>true</code> if Eco PDF is to be generated.
     * @param grayscalePdf
     *            <code>true</code> if Grayscale PDF is to be generated.
     * @return
     * @throws LetterheadNotFoundException
     * @throws IOException
     * @throws MessagingException
     * @throws InterruptedException
     * @throws CircuitBreakerException
     * @throws ParseException
     * @throws PGPBaseException
     */
    private Map<String, Object> reqSend(final User lockedUser,
            final String mailto, final String jobIndex, final String ranges,
            final boolean removeGraphics, final boolean ecoPdf,
            final boolean grayscalePdf) throws LetterheadNotFoundException,
            IOException, MessagingException, InterruptedException,
            CircuitBreakerException, ParseException, PGPBaseException {

        final Map<String, Object> userData = new HashMap<String, Object>();

        if (removeGraphics && ecoPdf) {
            return setApiResult(userData, ApiResultCodeEnum.INFO,
                    "msg-select-single-pdf-filter");
        }

        try {
            INBOX_SERVICE.calcPagesInRanges(
                    INBOX_SERVICE.getInboxInfo(lockedUser.getUserId()),
                    Integer.valueOf(jobIndex),
                    StringUtils.defaultString(getParmValue("ranges")), null);
        } catch (PageRangeException e) {
            return createApiResult(userData, ApiResultCodeEnum.ERROR, "",
                    e.getMessage(getLocale()));
        }

        final String user = lockedUser.getUserId();

        File fileAttach = null;

        try {

            final DocLog docLog = new DocLog();

            /*
             * (1) Generate with existing user lock.
             */
            fileAttach = generatePdfForExport(lockedUser,
                    Integer.parseInt(jobIndex), ranges, removeGraphics, ecoPdf,
                    grayscalePdf, docLog, "email");
            /*
             * INVARIANT: Since sending the mail is synchronous, file length is
             * important and MUST be less than criterion.
             */
            final int maxFileKb = ConfigManager.instance()
                    .getConfigInt(Key.MAIL_SMTP_MAX_FILE_KB);

            final long maxFileBytes = maxFileKb * 1024;

            if (fileAttach.length() > maxFileBytes) {

                final BigDecimal fileMb = new BigDecimal(
                        (double) fileAttach.length() / (1024 * 1024));

                final BigDecimal maxFileMb =
                        new BigDecimal((double) maxFileKb / 1024);

                setApiResult(userData, ApiResultCodeEnum.WARN,
                        "msg-mail-max-file-size",
                        BigDecimalUtil.localize(fileMb, 2,
                                this.getSession().getLocale(), true) + " MB",
                        BigDecimalUtil.localize(maxFileMb, 2,
                                this.getSession().getLocale(), true) + " MB");

            } else {

                final String fileName = createMeaningfullPdfFileName(docLog);

                final String subject = fileName;
                final String body = new StringBuilder()
                        .append("Hi,<p>Here are the SafePages "
                                + "attached from ")
                        .append(user).append(".</p>--<br/>")
                        .append(CommunityDictEnum.SAVAPAGE.getWord())
                        .append(" ").append(ConfigManager.getAppVersion())
                        .toString();

                /*
                 * (2) Unlock the user BEFORE sending the email.
                 */
                ServiceContext.getDaoContext().rollback();

                /*
                 * (3) Send email.
                 */

                final EmailMsgParms emailParms = new EmailMsgParms();

                emailParms.setToAddress(mailto);
                emailParms.setSubject(subject);
                emailParms.setBodyInStationary(fileName, body, getLocale(),
                        true);
                emailParms.setFileAttach(fileAttach);
                emailParms.setFileName(fileName);

                EMAIL_SERVICE.writeEmail(emailParms);

                /*
                 * (4) Log in database
                 */
                docLog.setDeliveryProtocol(DocLogProtocolEnum.SMTP.getDbName());
                docLog.getDocOut().setDestination(mailto);

                DOC_LOG_SERVICE.logDocOut(lockedUser, docLog.getDocOut());

                ApiRequestHelper.addUserStats(userData, lockedUser,
                        this.getSession().getLocale(),
                        SpSession.getAppCurrencySymbol());

                setApiResult(userData, ApiResultCodeEnum.OK, "msg-mail-sent",
                        mailto);
            }

        } catch (PostScriptDrmException e) {

            setApiResult(userData, ApiResultCodeEnum.ERROR,
                    "msg-pdf-export-drm-error");

        } catch (EcoPrintPdfTaskPendingException e) {

            setApiResult(userData, ApiResultCodeEnum.INFO,
                    "msg-ecoprint-pending");

        } finally {

            if ((fileAttach != null) && fileAttach.exists()) {

                if (fileAttach.delete()) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("deleted temp file [" + fileAttach + "]");
                    }
                } else {
                    LOGGER.error(
                            "delete of temp file [" + fileAttach + "] FAILED");
                }
            }
        }

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

        final int nPages;

        try {
            nPages = INBOX_SERVICE.deletePages(user, ranges);
        } catch (IllegalArgumentException e) {
            return setApiResult(userData, ApiResultCodeEnum.ERROR,
                    "msg-clear-range-syntax-error", ranges);
        }

        final String msgKey;

        if (nPages == 1) {
            msgKey = "msg-page-delete-one";
        } else {
            msgKey = "msg-page-delete-multiple";
        }

        return setApiResult(userData, ApiResultCodeEnum.OK, msgKey,
                String.valueOf(nPages));
    }

    /**
     * Gets the PDF properties.
     *
     * @param user
     * @return
     */
    private Map<String, Object> reqPdfPropsGet(final User user) {

        final PdfProperties objProps = USER_SERVICE.getPdfProperties(user);

        Map<String, Object> userData = new HashMap<String, Object>();

        userData.put("props", objProps);
        return setApiResultOK(userData);
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

        final JsonPrinterDetail jsonPrinter =
                PROXY_PRINT_SERVICE.getPrinterDetailUserCopy(
                        getSession().getLocale(), printerName, false);

        if (jsonPrinter == null) {

            setApiResult(data, ApiResultCodeEnum.ERROR,
                    "msg-printer-out-of-date");

        } else {

            if (jsonPrinter.getMediaSources().isEmpty()) {

                setApiResult(data, ApiResultCodeEnum.ERROR,
                        "msg-printer-no-media-sources-defined", printerName);

            } else {
                data.put("printer", jsonPrinter);
                setApiResultOK(data);
            }

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
            return setApiResult(data, ApiResultCodeEnum.OK,
                    "msg-print-auth-cancel-ok");
        } else {
            return setApiResult(data, ApiResultCodeEnum.WARN,
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

        return setApiResult(data, ApiResultCodeEnum.OK,
                "msg-print-fast-renew-ok", expiry);
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

        final int nPages = INBOX_SERVICE.movePages(user, ranges,
                Integer.valueOf(position));

        final String msgKey;

        if (nPages == 1) {
            msgKey = "msg-page-move-one";
        } else {
            msgKey = "msg-page-move-multiple";
        }

        return setApiResult(userData, ApiResultCodeEnum.OK, msgKey,
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

        setApiResult(userData, ApiResultCodeEnum.OK, "msg-pagometer-reset-ok");

        return userData;
    }

    /**
     *
     * @return
     * @throws IOException
     * @throws PaymentGatewayException
     */
    private Map<String, Object> reqBitcoinWalletRefresh() {

        final Map<String, Object> userData = new HashMap<String, Object>();

        try {
            WebApp.get().getPluginManager().refreshWalletInfoCache();
        } catch (PaymentGatewayException | IOException e) {
            return setApiResultMsgError(userData, "", e.getMessage());
        }
        return setApiResultOK(userData);
    }

    /**
     * @param user
     * @return
     */
    private Map<String, Object> reqUserSync(final String user,
            final boolean isTest, final boolean deleteUsers) {

        final Map<String, Object> data = new HashMap<String, Object>();

        if (SpJobScheduler.isJobCurrentlyExecuting(
                EnumSet.of(SpJobType.SYNC_USERS, SpJobType.SYNC_USER_GROUPS))) {

            setApiResult(data, ApiResultCodeEnum.WARN, "msg-user-sync-busy");

        } else {

            try {
                SpJobScheduler.instance().scheduleOneShotUserSync(isTest,
                        deleteUsers);

                setApiResult(data, ApiResultCodeEnum.OK, "msg-user-sync-busy");

            } catch (Exception e) {
                setApiResult(data, ApiResultCodeEnum.ERROR, "msg-tech-error",
                        e.getMessage());
            }
        }
        return data;
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

            setApiResult(userData, ApiResultCodeEnum.INFO,
                    "msg-imap-test-passed", nMessagesInbox.toString(),
                    nMessagesTrash.toString());

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = "connection error.";
            }
            setApiResult(userData, ApiResultCodeEnum.ERROR,
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

        if (!ConfigManager.isPrintImapEnabled()) {
            return setApiResultOK(userData);
        }

        final String msgKey;

        if (ImapPrinter.isOnline()) {
            msgKey = "msg-imap-started-already";
        } else {
            SpJobScheduler.instance().scheduleOneShotImapListener(1L);
            msgKey = "msg-imap-started";
        }

        return setApiResult(userData, ApiResultCodeEnum.OK, msgKey);
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqImapStop() {

        final Map<String, Object> userData = new HashMap<String, Object>();
        final String msgKey;

        if (SpJobScheduler.interruptImapListener()) {
            msgKey = "msg-imap-stopped";
        } else {
            msgKey = "msg-imap-stopped-already";
        }
        return setApiResult(userData, ApiResultCodeEnum.OK, msgKey);
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqPaperCutTest() {

        final Map<String, Object> userData = new HashMap<String, Object>();

        final ConfigManager cm = ConfigManager.instance();

        String error = PaperCutServerProxy.create(cm, false).testConnection();

        if (error == null) {
            error = new PaperCutDbProxy(cm, false).testConnection();
        }

        if (error == null) {
            setApiResult(userData, ApiResultCodeEnum.INFO,
                    "msg-papercut-test-passed");
        } else {
            setApiResult(userData, ApiResultCodeEnum.ERROR,
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

            SmartschoolPrintMonitor.testConnection(nMessagesInbox);

            setApiResult(userData, ApiResultCodeEnum.INFO,
                    "msg-smartschool-test-passed", nMessagesInbox.toString());

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = "connection error.";
            }
            setApiResult(userData, ApiResultCodeEnum.ERROR,
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

        final String msgKey;

        if (!ConfigManager.isSmartSchoolPrintActiveAndEnabled()) {
            return setApiResult(userData, ApiResultCodeEnum.ERROR,
                    "msg-smartschool-accounts-disabled");
        }

        SmartschoolPrinter
                .setBlocked(MemberCard.instance().isMembershipDesirable());

        if (SmartschoolPrinter.isBlocked()) {

            return setApiResult(userData, ApiResultCodeEnum.WARN,
                    "msg-smartschool-blocked",
                    CommunityDictEnum.SAVAPAGE.getWord(),
                    CommunityDictEnum.MEMBERSHIP.getWord());
        }

        if (SmartschoolPrinter.isOnline()) {

            msgKey = "msg-smartschool-started-already";

        } else {

            if (simulate) {
                SmartschoolPrintMonitor.resetJobTickerCounter();
            }

            SpJobScheduler.instance()
                    .scheduleOneShotSmartSchoolPrintMonitor(simulate, 1L);

            if (simulate) {
                msgKey = "msg-smartschool-started-simulation";
            } else {
                msgKey = "msg-smartschool-started";
            }

            /*
             * Have a retry and close relevant circuits.
             */
            ConfigManager
                    .getCircuitBreaker(
                            CircuitBreakerEnum.SMARTSCHOOL_CONNECTION)
                    .closeCircuit();

        }
        return setApiResult(userData, ApiResultCodeEnum.OK, msgKey);
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqSmartSchoolStop() {

        final Map<String, Object> userData = new HashMap<String, Object>();

        final String msgKey;

        if (SpJobScheduler.interruptSmartSchoolPoller()) {
            msgKey = "msg-smartschool-stopped";
        } else {
            msgKey = "msg-smartschool-stopped-already";
        }
        return setApiResult(userData, ApiResultCodeEnum.OK, msgKey);
    }

    /**
     *
     * @return
     * @throws PaymentGatewayException
     */
    private Map<String, Object> reqPaymentGatewayOnline(final boolean bitcoin,
            final boolean online) throws PaymentGatewayException {

        final Map<String, Object> userData = new HashMap<String, Object>();

        final ServerPluginManager manager = WebApp.get().getPluginManager();

        final PaymentGatewayPlugin plugin;

        if (bitcoin) {
            plugin = manager.getBitcoinGateway();
        } else {
            plugin = manager.getExternalPaymentGateway();
        }

        if (plugin != null) {

            plugin.setOnline(online);

            final String key;

            final PubLevelEnum pubLevel;
            if (online) {
                key = "msg-payment-gateway-online";
                pubLevel = PubLevelEnum.CLEAR;
            } else {
                key = "msg-payment-gateway-offline";
                pubLevel = PubLevelEnum.WARN;
            }

            final String systemMsg =
                    AppLogHelper.logInfo(getClass(), key, plugin.getName());

            AdminPublisher.instance().publish(PubTopicEnum.PAYMENT_GATEWAY,
                    pubLevel, systemMsg);

            return setApiResult(userData, ApiResultCodeEnum.OK, key,
                    plugin.getName());
        }

        return setApiResult(userData, ApiResultCodeEnum.ERROR,
                "msg-payment-gateway-not-found");
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

        final int minPwLength = ConfigManager.instance()
                .getConfigInt(Key.INTERNAL_USERS_PW_LENGTH_MIN);

        if (password.length() < minPwLength) {
            setApiResult(userData, ApiResultCodeEnum.ERROR,
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

        setApiResult(userData, ApiResultCodeEnum.OK, "msg-password-reset-ok");

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
        setApiResult(userData, ApiResultCodeEnum.OK, "msg-password-reset-ok");

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
            return setApiResult(userData, ApiResultCodeEnum.ERROR,
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
            return setApiResult(userData, ApiResultCodeEnum.ERROR,
                    "msg-password-reset-not-allowed", iuser);
        }

        final String encryptedPw =
                USER_SERVICE.encryptUserPassword(iuser, password);

        USER_SERVICE.setUserAttrValue(jpaUser, UserAttrEnum.INTERNAL_PASSWORD,
                encryptedPw);

        jpaUser.setModifiedBy(requestingUser);
        jpaUser.setModifiedDate(new Date());

        userDao.update(jpaUser);

        setApiResult(userData, ApiResultCodeEnum.OK, "msg-password-reset-ok");

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
                CryptoUser.encryptUserAttr(jpaUser.getId(), pin));

        jpaUser.setModifiedBy(requestingUser);
        jpaUser.setModifiedDate(new Date());

        userDao.update(jpaUser);

        setApiResult(userData, ApiResultCodeEnum.OK, "msg-pin-reset-ok");

        return userData;
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
     * @param deviceType
     * @return
     */
    private Map<String, Object> reqDeviceNew(final DeviceTypeEnum deviceType) {

        final Map<String, Object> userObj = new HashMap<String, Object>();

        userObj.put("deviceType", deviceType.toString());
        userObj.put("disabled", Boolean.FALSE);

        final HashMap<String, Object> attrMap = new HashMap<>();

        userObj.put("attr", attrMap);

        if (deviceType == DeviceTypeEnum.CARD_READER) {
            userObj.put("port", Integer.valueOf(ConfigManager.instance()
                    .getConfigInt(Key.DEVICE_CARD_READER_DEFAULT_PORT)));

        } else if (deviceType == DeviceTypeEnum.TERMINAL) {

            attrMap.put(DeviceAttrEnum.WEBAPP_USER_MAX_IDLE_SECS.getDbName(),
                    "0");
        }

        final Map<String, Object> userData = new HashMap<String, Object>();

        userData.put("j_device", userObj);

        return setApiResultOK(userData);
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
            return setApiResult(userData, ApiResultCodeEnum.ERROR,
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

            if (!replaceDuplicate && printerDao
                    .countPrintOuts(printerDuplicate.getId()) > 0) {

                return setApiResult(userData, ApiResultCodeEnum.ERROR,
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

        return setApiResult(userData, ApiResultCodeEnum.OK, msgKey, oldName,
                newName);
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
            return setApiResult(userData, ApiResultCodeEnum.ERROR,
                    "msg-printer-not-found", String.valueOf(id));
        }

        /*
         *
         */
        final AbstractJsonRpcMethodResponse rpcResponse =
                PROXY_PRINT_SERVICE.setProxyPrinterCostMedia(jpaPrinter, dto);

        if (rpcResponse.isResult()) {
            setApiResult(userData, ApiResultCodeEnum.OK,
                    "msg-printer-saved-ok");
        } else {
            setApiResultMsgError(userData, "", rpcResponse.asError().getError()
                    .data(ErrorDataBasic.class).getReason());
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
            setApiResult(userData, ApiResultCodeEnum.ERROR,
                    "msg-user-not-found", userId);
        } else {
            ApiRequestHelper.addUserStats(userData, user,
                    this.getSession().getLocale(),
                    SpSession.getAppCurrencySymbol());
            setApiResultOK(userData);
        }
        return userData;
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

            setApiResult(userData, ApiResultCodeEnum.ERROR,
                    "msg-user-pin-not-numeric");

        } else if (pin.length() < lengthMin
                || (lengthMax > 0 && pin.length() > lengthMax)) {

            if (lengthMin == lengthMax) {
                setApiResult(userData, ApiResultCodeEnum.ERROR,
                        "msg-user-pin-length-error", String.valueOf(lengthMin));
            } else if (lengthMax == 0) {
                setApiResult(userData, ApiResultCodeEnum.ERROR,
                        "msg-user-pin-length-error-min",
                        String.valueOf(lengthMin));
            } else {
                setApiResult(userData, ApiResultCodeEnum.ERROR,
                        "msg-user-pin-length-error-min-max",
                        String.valueOf(lengthMin), String.valueOf(lengthMax));
            }

        } else {
            isValid = true;
        }

        return isValid;
    }

    /**
     * Creates an API result from {@link AbstractJsonRpcMethodResponse}
     * containing either {@link ResultDataBasic} or {@link ErrorDataBasic}
     *
     * @param rpcResponse
     *            The {@link AbstractJsonRpcMethodResponse}.
     * @return
     */
    private static Map<String, Object> apiResultFromBasicRpcResponse(
            final AbstractJsonRpcMethodResponse rpcResponse) {

        final Map<String, Object> userData = new HashMap<String, Object>();

        if (rpcResponse.isResult()) {

            final ResultDataBasic result = rpcResponse.asResult().getResult()
                    .data(ResultDataBasic.class);

            setApiResultMsgOK(userData, null, result.getMessage());

        } else {

            final ErrorDataBasic error =
                    rpcResponse.asError().getError().data(ErrorDataBasic.class);

            setApiResultMsgError(userData, "", error.getReason());
        }
        return userData;
    }

    /**
     *
     * @param requestingUser
     * @param jsonDto
     * @return
     * @throws Exception
     */
    private Map<String, Object> reqUserMoneyTransfer(
            final String requestingUser, final String jsonDto)
            throws IOException {

        final Map<String, Object> userData = new HashMap<String, Object>();

        final MoneyTransferDto dto =
                JsonAbstractBase.create(MoneyTransferDto.class, jsonDto);

        /*
         * INVARIANT: Amount MUST be valid.
         */
        final String plainAmount =
                dto.getAmountMain() + "." + dto.getAmountCents();

        if (!BigDecimalUtil.isValid(plainAmount)) {
            return setApiResult(userData, ApiResultCodeEnum.ERROR,
                    "msg-amount-invalid");
        }

        final BigDecimal paymentAmount = BigDecimalUtil.valueOf(plainAmount);

        /*
         * INVARIANT: Amount MUST be GT zero.
         */
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return setApiResult(userData, ApiResultCodeEnum.ERROR,
                    "msg-amount-must-be positive");
        }

        /*
         * INVARIANT: plugin must be available.
         */
        final PaymentGatewayPlugin plugin = WebApp.get().getPluginManager()
                .getPaymentGateway(dto.getGatewayId());

        if (plugin == null) {
            throw new IllegalStateException(
                    String.format("Payment gateway \"%s\" is not available.",
                            dto.getGatewayId()));
        } else if (!plugin.isOnline()) {
            return setApiResult(userData, ApiResultCodeEnum.ERROR,
                    "msg-payment-method-not-possible",
                    dto.getMethod().toString());
        }

        try {
            final String comment = localize("msg-payment-gateway-comment",
                    CommunityDictEnum.SAVAPAGE.getWord(), requestingUser);

            final URL callbackUrl = ServerPluginManager.getCallBackUrl(plugin);

            final URL redirectUrl =
                    ServerPluginManager.getRedirectUrl(dto.getSenderUrl());

            final PaymentRequest req = new PaymentRequest();

            req.setMethod(dto.getMethod());
            req.setAmount(paymentAmount.doubleValue());
            req.setCallbackUrl(callbackUrl);
            req.setCurrency(ConfigManager.getAppCurrency());
            req.setDescription(comment);
            req.setRedirectUrl(redirectUrl);
            req.setUserId(requestingUser);

            final PaymentGatewayTrx trx = plugin.onPaymentRequest(req);

            setApiResultOK(userData);
            userData.put("paymentUrl", trx.getPaymentUrl().toExternalForm());

        } catch (IOException e) {

            final StringBuilder err = new StringBuilder();
            err.append("Communication error: ").append(e.getMessage());
            createApiResult(userData, ApiResultCodeEnum.ERROR, "",
                    err.toString());

        } catch (PaymentGatewayException e) {
            createApiResult(userData, ApiResultCodeEnum.ERROR, "",
                    e.getMessage());
        }

        return userData;
    }

    /**
     *
     * @param requestingUser
     * @param jsonDto
     * @return
     * @throws Exception
     */
    private Map<String, Object> reqPosDeposit(final String requestingUser,
            final String jsonDto) throws Exception {

        final Map<String, Object> userData = new HashMap<String, Object>();

        final PosDepositDto dto =
                JsonAbstractBase.create(PosDepositDto.class, jsonDto);

        final AbstractJsonRpcMethodResponse rpcResponse =
                ACCOUNTING_SERVICE.depositFunds(dto);

        if (rpcResponse.isResult()) {

            switch (dto.getReceiptDelivery()) {
            case EMAIL:
                final ResultPosDeposit data = rpcResponse.asResult().getResult()
                        .data(ResultPosDeposit.class);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("AccountTrxDbId [" + data.getAccountTrxDbId()
                            + "]");
                }
                /*
                 * Commit the result before sending the email.
                 */
                ServiceContext.getDaoContext().commit();

                setApiResult(userData, ApiResultCodeEnum.OK,
                        "msg-deposit-funds-receipt-email-ok",
                        dto.getUserEmail());

                mailDepositReceipt(data.getAccountTrxDbId(), dto.getUserEmail(),
                        requestingUser,
                        getSessionWebAppType().equals(WebAppTypeEnum.POS)
                                || getSessionWebAppType()
                                        .equals(WebAppTypeEnum.ADMIN));
                break;

            case NONE:
                // no break intended
            default:
                setApiResult(userData, ApiResultCodeEnum.OK,
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
     * An administrator sending email to a POS Receipt.
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

        final User user = userAccountDao
                .findByAccountId(accountTrx.getAccount().getId()).getUser();

        final String email = USER_SERVICE.getPrimaryEmailAddress(user);

        mailDepositReceipt(dto.getKey(), email, requestingUser, true);

        setApiResult(data, ApiResultCodeEnum.OK, "msg-pos-receipt-sendmail-ok",
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
            setApiResult(userData, ApiResultCodeEnum.OK,
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

        final Integer nDeleted = ServiceContext.getDaoContext()
                .getAccountVoucherDao().deleteBatch(batch);

        if (nDeleted == 0) {
            setApiResult(userData, ApiResultCodeEnum.OK,
                    "msg-voucher-batch-deleted-zero", batch);
        } else if (nDeleted == 1) {
            setApiResult(userData, ApiResultCodeEnum.OK,
                    "msg-voucher-batch-deleted-one", batch);
        } else {
            setApiResult(userData, ApiResultCodeEnum.OK,
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

        final Integer nExpired = ServiceContext.getDaoContext()
                .getAccountVoucherDao().expireBatch(batch, expiryToday);

        if (nExpired == 0) {
            setApiResult(userData, ApiResultCodeEnum.OK,
                    "msg-voucher-batch-expired-zero", batch);
        } else if (nExpired == 1) {
            setApiResult(userData, ApiResultCodeEnum.OK,
                    "msg-voucher-batch-expired-one", batch);
        } else {
            setApiResult(userData, ApiResultCodeEnum.OK,
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
            return setApiResult(userData, ApiResultCodeEnum.WARN,
                    "msg-voucher-redeem-void");
        }

        final AccountVoucherRedeemDto dto = new AccountVoucherRedeemDto();

        dto.setCardNumber(cardNumber);
        dto.setRedeemDate(System.currentTimeMillis());
        dto.setUserId(requestingUser);

        final AbstractJsonRpcMethodResponse rpcResponse =
                ACCOUNTING_SERVICE.redeemVoucher(dto);

        if (rpcResponse.isResult()) {

            setApiResult(userData, ApiResultCodeEnum.OK,
                    "msg-voucher-redeem-ok");

        } else {

            final JsonRpcError error = rpcResponse.asError().getError();
            final ErrorDataBasic errorData = error.data(ErrorDataBasic.class);

            setApiResultMsgError(userData, error.getMessage(),
                    errorData.getReason());

            if (error.getMessage().equals(
                    AccountingService.MSG_KEY_VOUCHER_REDEEM_NUMBER_INVALID)) {

                final String msg = AppLogHelper.logWarning(getClass(),
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

        final Integer nDeleted = ServiceContext.getDaoContext()
                .getAccountVoucherDao().deleteExpired(expiryToday);

        if (nDeleted == 0) {
            setApiResult(userData, ApiResultCodeEnum.OK,
                    "msg-voucher-deleted-expired-zero");
        } else if (nDeleted == 1) {
            setApiResult(userData, ApiResultCodeEnum.OK,
                    "msg-voucher-deleted-expired-one");
        } else {
            setApiResult(userData, ApiResultCodeEnum.OK,
                    "msg-voucher-deleted-expired-many", nDeleted.toString());
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
        return apiResultFromBasicRpcResponse(USER_SERVICE.deleteUser(userid));
    }

    /**
     *
     * @param user
     * @return
     */
    private Map<String, Object> reqUserSourceGroups(final String user) {

        final Map<String, Object> userData = new HashMap<String, Object>();

        final SortedSet<CommonUserGroup> sset =
                ConfigManager.instance().getUserSource().getGroups();

        final String[] groupNames = new String[sset.size()];
        int i = 0;
        for (final CommonUserGroup grp : sset) {
            groupNames[i++] = grp.getGroupName();
        }

        userData.put("groups", groupNames);

        return setApiResultOK(userData);
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
     * This method closes for the current {@link SpSession} and interrupts any
     * CometD long-polls of the owning user.
     *
     * @return The response map.
     * @throws IOException
     *             When IO errors.
     */
    private Map<String, Object> reqWebAppCloseSession() throws IOException {

        final SpSession session = SpSession.get();

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
         * IMPORTANT: Logout to remove the user and WebApp Type associated with
         * this session.
         */
        session.logout();

        /*
         * Replaces the underlying (Web)Session, invalidating the current one
         * and creating a new one. NOTE: data are copied from current session.
         */
        session.replaceSession();

        /*
         * Make sure that all User Web App long polls for this user are
         * interrupted.
         */
        if (savedWebAppType == WebAppTypeEnum.USER && userId != null) {
            ApiRequestHelper.interruptPendingLongPolls(userId,
                    this.getRemoteAddr());
        }

        return createApiResultOK();
    }

    /**
     *
     * @param lockedUser
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqExitEventMonitor(final String userId)
            throws IOException {
        ApiRequestHelper.interruptPendingLongPolls(userId,
                this.getRemoteAddr());
        return createApiResultOK();
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
    private Map<String, Object> reqInboxIsVanilla(final String user) {

        final boolean isVanilla =
                INBOX_SERVICE.isInboxVanilla(INBOX_SERVICE.getInboxInfo(user));
        final Map<String, Object> userData = new HashMap<String, Object>();
        userData.put("vanilla", Boolean.valueOf(isVanilla));
        return setApiResultOK(userData);
    }

    /**
     *
     * @return
     */
    private Map<String, Object> reqInboxJobDelete(final String user, int iJob) {

        INBOX_SERVICE.deleteJob(user, iJob);

        final Map<String, Object> userData = new HashMap<String, Object>();

        return setApiResult(userData, ApiResultCodeEnum.OK, "msg-job-deleted");
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
            return setApiResult(userData, ApiResultCodeEnum.ERROR,
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
        return setApiResult(userData, ApiResultCodeEnum.OK, msgKey);
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
            boolean enabled, final String emailSubject,
            final String emailBody) {

        Map<String, Object> userData = new HashMap<String, Object>();

        final ConfigManager cm = ConfigManager.instance();

        cm.updateConfigKey(Key.GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_ENABLE,
                enabled, user.getUserId());

        cm.updateConfigKey(Key.GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_SUBJECT,
                emailSubject, user.getUserId());

        cm.updateConfigKey(Key.GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_BODY,
                emailBody, user.getUserId());

        return setApiResult(userData, ApiResultCodeEnum.OK,
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

        return setApiResult(userData, ApiResultCodeEnum.OK,
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

        INBOX_SERVICE.setLetterhead(user, letterheadId,
                list.get("name").getTextValue(),
                list.get("foreground").getBooleanValue(),
                list.get("pub").getBooleanValue(),
                list.get("pub-new").getBooleanValue());

        final Map<String, Object> userData = new HashMap<String, Object>();

        return setApiResult(userData, ApiResultCodeEnum.OK,
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
        return setApiResult(userData, ApiResultCodeEnum.OK, "msg-job-edited");
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

        PageImages pages = INBOX_SERVICE.getPageChunks(user, nFirstDetailPage,
                uniqueUrlValue, base64);

        userData.put("jobs", pages.getJobs());
        userData.put("pages", pages.getPages());
        userData.put("url_template",
                ImageUrl.composeDetailPageTemplate(user, base64));

        return userData;
    }

    /**
     * @param webAppType
     * @param authModeReq
     *            The requested authentication mode.
     * @return
     */
    private Map<String, Object> reqConstants(final WebAppTypeEnum webAppType,
            final String authModeReq) {

        final Map<String, Object> userData = new HashMap<>();

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

        //
        final ConfigManager cm = ConfigManager.instance();
        final String remoteAddr = this.getRemoteAddr();
        final UserAuth userAuth = new UserAuth(
                ApiRequestHelper.getHostTerminal(remoteAddr), authModeReq,
                webAppType, InetUtils.isPublicAddress(remoteAddr));

        userData.put("authName", userAuth.isVisibleAuthName());
        userData.put("authId", userAuth.isVisibleAuthId());
        userData.put("authCardLocal", userAuth.isVisibleAuthCardLocal());
        userData.put("authCardIp", userAuth.isVisibleAuthCardIp());
        userData.put("authYubiKey", userAuth.isVisibleAuthYubikey());

        userData.put("authModeDefault",
                userAuth.getAuthModeDefault().toDbValue());

        userData.put("authCardPinReq", userAuth.isAuthCardPinReq());
        userData.put("authCardSelfAssoc", userAuth.isAuthCardSelfAssoc());

        final Integer maxIdleSeconds = userAuth.getMaxIdleSeconds();

        if (maxIdleSeconds != null && maxIdleSeconds > 0) {
            userData.put("maxIdleSeconds", maxIdleSeconds);
        }

        userData.put("cardLocalMaxMsecs",
                cm.getConfigInt(Key.WEBAPP_CARD_LOCAL_KEYSTROKES_MAX_MSECS));

        userData.put("yubikeyMaxMsecs",
                cm.getConfigInt(Key.WEBAPP_YUBIKEY_KEYSTROKES_MAX_MSECS));

        userData.put("cardAssocMaxSecs",
                cm.getConfigInt(Key.WEBAPP_CARD_ASSOC_DIALOG_MAX_SECS));

        userData.put("watchdogHeartbeatSecs",
                cm.getConfigInt(Key.WEBAPP_WATCHDOG_HEARTBEAT_SECS));

        userData.put("watchdogTimeoutSecs",
                cm.getConfigInt(Key.WEBAPP_WATCHDOG_TIMEOUT_SECS));

        //
        userData.put("cometdToken", CometdClientMixin.SHARED_DEVICE_TOKEN);

        userData.put("cometdMaxNetworkDelay",
                AbstractEventService.getMaxNetworkDelay());

        //
        userData.put("systime", Long.valueOf(System.currentTimeMillis()));

        //
        userData.put("showNavButtonTxt", cm.getConfigEnum(OnOffEnum.class,
                Key.WEBAPP_USER_MAIN_NAV_BUTTON_TEXT));

        //
        userData.put("jobticketCopierEnable",
                cm.isConfigValue(Key.JOBTICKET_COPIER_ENABLE));

        userData.put("jobticketDeliveryDays", Integer
                .valueOf(cm.getConfigInt(Key.JOBTICKET_DELIVERY_DAYS, 0)));

        userData.put("jobticketDeliveryDaysMin", Integer
                .valueOf(cm.getConfigInt(Key.JOBTICKET_DELIVERY_DAYS_MIN, 0)));

        final int minutes =
                cm.getConfigInt(Key.JOBTICKET_DELIVERY_DAY_MINUTES, 0);

        userData.put("jobticketDeliveryHour",
                Integer.valueOf(minutes / DateUtil.MINUTES_IN_HOUR));
        userData.put("jobticketDeliveryMinute",
                Integer.valueOf(minutes % DateUtil.MINUTES_IN_HOUR));

        userData.put("jobticketDeliveryDaysOfweek",
                JOBTICKET_SERVICE.getDeliveryDaysOfWeek());

        //
        userData.put("proxyPrintClearPrinter",
                cm.isConfigValue(Key.WEBAPP_USER_PROXY_PRINT_CLEAR_PRINTER));

        if (cm.isConfigValue(Key.PROXY_PRINT_DELEGATE_ENABLE)) {

            userData.put("delegatorGroupHideId", cm.isConfigValue(
                    Key.WEBAPP_USER_PROXY_PRINT_DELEGATOR_GROUP_HIDE_ID));

            userData.put("delegatorUserHideId", cm.isConfigValue(
                    Key.WEBAPP_USER_PROXY_PRINT_DELEGATOR_USER_HIDE_ID));

            userData.put("proxyPrintClearDelegate", cm
                    .isConfigValue(Key.WEBAPP_USER_PROXY_PRINT_CLEAR_DELEGATE));

            userData.put("delegateAccountSharedGroup", cm.isConfigValue(
                    Key.PROXY_PRINT_DELEGATE_ACCOUNT_SHARED_GROUP_ENABLE));
        }

        // Web Print
        final boolean isWebPrintEnabled =
                WebPrintHelper.isWebPrintEnabled(this.getRemoteAddr());

        userData.put("webPrintEnabled", isWebPrintEnabled);

        if (isWebPrintEnabled) {
            userData.put("webPrintDropZoneEnabled",
                    WebPrintHelper.isWebPrintDropZoneEnabled());
            userData.put("webPrintMaxBytes",
                    WebPrintHelper.getMaxUploadSize().bytes());
            userData.put("webPrintUploadUrl",
                    WebApp.MOUNT_PATH_UPLOAD_WEBPRINT);
            userData.put("webPrintUploadFileParm",
                    WebPrintDropZoneFileResource.UPLOAD_PARAM_NAME_FILE);
            userData.put("webPrintUploadFontParm",
                    WebPrintDropZoneFileResource.UPLOAD_PARAM_NAME_FONT);
            userData.put("webPrintFileExt",
                    WebPrintHelper.getSupportedFileExtensions(true));
        }

        if (ConfigManager.isPdfPgpEnabled()) {
            userData.put("pdfpgpMaxBytes",
                    PdfPgpUploadHelper.getMaxUploadSize().bytes());
            userData.put("pdfpgpUploadUrl",
                    WebApp.MOUNT_PATH_UPLOAD_PDF_VERIFY);
            userData.put("pdfpgpUploadFileParm",
                    PdfPgpDropZoneFileResource.UPLOAD_PARAM_NAME_FILE);
            userData.put("pdfpgpFileExt",
                    PdfPgpUploadHelper.getSupportedFileExtensions(true));
        }

        // Colors
        final Map<String, String> colors = new HashMap<>();

        colors.put("printOut", SparklineHtml.COLOR_PRINTER);
        colors.put("printIn", SparklineHtml.COLOR_QUEUE);
        colors.put("pdfOut", SparklineHtml.COLOR_PDF);

        userData.put("colors", colors);

        if (webAppType == WebAppTypeEnum.USER) {

            Map<String, Object> scaling;

            scaling = new HashMap<>();
            scaling.put("show", cm.isConfigValue(
                    Key.WEBAPP_USER_PROXY_PRINT_SCALING_MEDIA_MATCH_SHOW));
            scaling.put("value", cm.getConfigValue(
                    Key.WEBAPP_USER_PROXY_PRINT_SCALING_MEDIA_MATCH_DEFAULT));
            userData.put("printScalingMatch", scaling);

            scaling = new HashMap<>();
            scaling.put("show", cm.isConfigValue(
                    Key.WEBAPP_USER_PROXY_PRINT_SCALING_MEDIA_CLASH_SHOW));
            scaling.put("value", cm.getConfigValue(
                    Key.WEBAPP_USER_PROXY_PRINT_SCALING_MEDIA_CLASH_DEFAULT));
            userData.put("printScalingClash", scaling);
        }

        //
        return userData;
    }

    /**
     * Sets the language (translation) of the Web App, and the {@link Locale} in
     * the {@link SpSession}.
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
     * @param country
     *            The selected country for the Web App. This value is also used
     *            for the country setting in the session {@link Locale}.
     * @return
     * @throws IOException
     */
    private Map<String, Object> reqLanguage(final String language,
            final String country) throws IOException {

        final Map<String, Object> userData = new HashMap<String, Object>();

        /*
         * Straight values.
         */
        setApiResultOK(userData);

        final Map<String, Object> i18n = new HashMap<String, Object>();

        /*
         * Set the new locale, for the strings to return AND for the current
         * session.
         */
        final Locale.Builder localeBuilder = new Locale.Builder();

        Locale locale;

        //
        try {
            localeBuilder.setLanguage(language);

            if (StringUtils.isNotBlank(country)) {

                localeBuilder.setRegion(country);

            } else {
                /*
                 * We adopt the country/region code from the Browser, if the
                 * language setting of the Browser is the same as the SELECTED
                 * language in the SavaPage Web App.
                 */
                final Locale localeReq =
                        getRequestCycle().getRequest().getLocale();

                if (localeReq.getLanguage().equalsIgnoreCase(language)) {
                    localeBuilder.setRegion(localeReq.getCountry());
                }
            }

            locale = localeBuilder.build();

        } catch (Exception e) {
            locale = Locale.US;
        }

        SpSession.get().setLocale(locale);

        /*
         *
         */
        final ResourceBundle rcBundle =
                Messages.loadXmlResource(getClass(), "i18n", locale);

        final Set<String> keySet = rcBundle.keySet();

        for (final String key : keySet) {
            i18n.put(key, rcBundle.getString(key));
        }

        // i18n for FileUpload (WebPrint).
        i18n.put("button-back", HtmlButtonEnum.BACK.uiText(locale));
        i18n.put("button-upload", HtmlButtonEnum.UPLOAD.uiText(locale));
        i18n.put("button-reset", HtmlButtonEnum.RESET.uiText(locale));

        //
        userData.put("i18n", i18n);
        userData.put("language", locale.getLanguage());
        userData.put("country", locale.getCountry());

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
