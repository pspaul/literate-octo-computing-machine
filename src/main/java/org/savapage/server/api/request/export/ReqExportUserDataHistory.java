/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.api.request.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.time.Duration;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.UserAccountDao;
import org.savapage.core.dao.UserAttrDao;
import org.savapage.core.dao.UserNumberDao;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.AccountVoucher;
import org.savapage.core.jpa.CostChange;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PdfOut;
import org.savapage.core.jpa.PosPurchase;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.jpa.UserCard;
import org.savapage.core.jpa.UserEmail;
import org.savapage.core.jpa.UserNumber;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.api.JsonApiDict;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Creates ZIP file with CSV exports of document and transaction log for
 * requesting user.
 *
 * @author Rijk Ravestein
 *
 */
public class ReqExportUserDataHistory extends ApiRequestExportMixin {

    /** */
    private static final String FILE_DATE_FORMAT_PATTERN =
            "yyyy-MM-dd'T'HH-mm-ss";

    /** */
    private static final String DATE_FORMAT_PATTERN = "yyyy.MM.dd HH:mm:ss z";

    /** */
    private static final String SECRET_VALUE = "*****";

    /** */
    private final int maxExportResults;

    /** */
    private final Locale locale;

    /** */
    private final SimpleDateFormat dateFormat;

    /** */
    public ReqExportUserDataHistory() {
        this.maxExportResults = ConfigManager.instance()
                .getConfigInt(Key.DB_EXPORT_QUERY_MAX_RESULTS);

        this.locale = Locale.ENGLISH;
        this.dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN, locale);
    }

    @Override
    protected final IRequestHandler onExport(final WebAppTypeEnum webAppType,
            final String requestingUser, final RequestCycle requestCycle,
            final PageParameters parameters, final boolean isGetAction,
            final File tempExportFile) throws Exception {

        final String uid;
        final User user;

        if (webAppType == WebAppTypeEnum.USER) {
            uid = requestingUser;
            user = ServiceContext.getDaoContext().getUserDao()
                    .findActiveUserByUserId(uid);
        } else if (webAppType == WebAppTypeEnum.ADMIN) {
            final Long dbKey = Long.valueOf(getParmValue(requestCycle,
                    parameters, isGetAction, JsonApiDict.PARM_REQ_PARM));
            user = ServiceContext.getDaoContext().getUserDao().findById(dbKey);
            uid = user.getUserId();
        } else {
            throw new IllegalArgumentException();
        }

        final SimpleDateFormat dateFormatFile =
                new SimpleDateFormat(FILE_DATE_FORMAT_PATTERN);

        final String fileNameDateTimePart = dateFormatFile.format(new Date());

        final String handlerFileName = String.format("%s-data-export-%s.zip",
                uid, fileNameDateTimePart);

        final FileOutputStream fout = new FileOutputStream(tempExportFile);
        final ZipOutputStream zout = new ZipOutputStream(fout);

        final int level = 9; // highest level
        zout.setLevel(level);

        final OutputStreamWriter writer = new OutputStreamWriter(zout);
        final CSVWriter csvWriter = new CSVWriter(writer);

        try {
            // #1
            zout.putNextEntry(new ZipEntry(String.format("%s-%s-user.csv", uid,
                    fileNameDateTimePart)));
            this.exportUserDetails(csvWriter, user);

            // #2
            zout.putNextEntry(new ZipEntry(String.format("%s-%s-documents.csv",
                    uid, fileNameDateTimePart)));
            this.exportDocumentLog(csvWriter, user);

            // #3
            zout.putNextEntry(new ZipEntry(String.format(
                    "%s-%s-transactions.csv", uid, fileNameDateTimePart)));
            this.exportTransactionLog(csvWriter, user);

        } finally {
            csvWriter.flush();
            csvWriter.close();
        }

        /* */
        final ResourceStreamRequestHandler handler =
                new ResourceStreamRequestHandler(
                        new FileResourceStream(tempExportFile));

        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        handler.setFileName(handlerFileName);
        handler.setCacheDuration(Duration.NONE);

        return handler;
    }

    /**
     * Writes CSV for Transactions.
     *
     * @param writer
     *            The CVS writer
     * @param user
     *            The user.
     * @throws IOException
     *             When IO error.
     */
    private void exportUserDetails(final CSVWriter writer, final User user)
            throws IOException {

        final UserAttrDao dao = ServiceContext.getDaoContext().getUserAttrDao();

        final List<String[]> lines = new ArrayList<>();

        lines.add(new String[] { "ID", user.getUserId() });
        lines.add(new String[] { "Name", user.getFullName() });

        if (StringUtils.isNotBlank(user.getOffice())) {
            lines.add(new String[] { "Office", user.getOffice() });
        }
        if (StringUtils.isNotBlank(user.getDepartment())) {
            lines.add(new String[] { "Department", user.getDepartment() });
        }

        lines.add(new String[] { "Created",
                this.dateFormat.format(user.getCreatedDate()) });
        if (user.getLastUserActivity() != null) {
            lines.add(new String[] { "Last activity",
                    this.dateFormat.format(user.getLastUserActivity()) });
        }

        if (user.getEmails() != null) {
            int i = 1;
            for (final UserEmail mail : user.getEmails()) {
                lines.add(new String[] { String.format("Email #%d", i++),
                        mail.getAddress() });
            }
        }
        if (user.getCards() != null) {
            int i = 1;
            for (final UserCard card : user.getCards()) {
                lines.add(new String[] { String.format("Card #%d", i++),
                        SECRET_VALUE });
            }
        }
        if (user.getIdNumbers() != null) {

            final UserNumberDao daoNumber =
                    ServiceContext.getDaoContext().getUserNumberDao();

            int i = 1;
            int j = 1;

            for (final UserNumber number : user.getIdNumbers()) {

                if (daoNumber.isYubiKeyPubID(number)) {
                    lines.add(new String[] { String.format("YubiKey #%d", j++),
                            SECRET_VALUE });
                } else {
                    lines.add(
                            new String[] { String.format("ID number #%d", i++),
                                    number.getNumber() });
                }
            }
        }

        UserAttr attr;

        attr = dao.findByName(user, UserAttrEnum.INTERNAL_PASSWORD);
        if (attr != null && StringUtils.isNotBlank(attr.getValue())) {
            lines.add(new String[] { "Password", SECRET_VALUE });
        }
        attr = dao.findByName(user, UserAttrEnum.PIN);
        if (attr != null && StringUtils.isNotBlank(attr.getValue())) {
            lines.add(new String[] { "PIN", SECRET_VALUE });
        }
        attr = dao.findByName(user, UserAttrEnum.PGP_PUBKEY_ID);
        if (attr != null && StringUtils.isNotBlank(attr.getValue())) {
            lines.add(new String[] { "PGP key", attr.getValue() });
        }

        if (!user.getNumberOfPrintInJobs().equals(Integer.valueOf(0))) {
            lines.add(new String[] { "Print IN jobs",
                    user.getNumberOfPrintInJobs().toString() });
            lines.add(new String[] { "Print IN pages",
                    user.getNumberOfPrintInPages().toString() });
            lines.add(new String[] { "Print IN bytes",
                    user.getNumberOfPrintInBytes().toString() });
        }

        if (!user.getNumberOfPdfOutJobs().equals(Integer.valueOf(0))) {
            lines.add(new String[] { "PDF OUT files",
                    user.getNumberOfPdfOutJobs().toString() });
            lines.add(new String[] { "PDF OUT pages",
                    user.getNumberOfPdfOutPages().toString() });
            lines.add(new String[] { "PDF OUT bytes",
                    user.getNumberOfPdfOutBytes().toString() });
        }

        if (!user.getNumberOfPrintOutJobs().equals(Integer.valueOf(0))) {
            lines.add(new String[] { "Print OUT jobs",
                    user.getNumberOfPrintOutJobs().toString() });
            lines.add(new String[] { "Print OUT pages",
                    user.getNumberOfPrintOutPages().toString() });
            lines.add(new String[] { "Print OUT sheets",
                    user.getNumberOfPrintOutSheets().toString() });
            lines.add(new String[] { "Print OUT bytes",
                    user.getNumberOfPrintOutBytes().toString() });
        }
        //
        final UserAccountDao daoAcc =
                ServiceContext.getDaoContext().getUserAccountDao();

        final List<UserAccount> userAccList = daoAcc.findByUserId(user.getId());
        if (!userAccList.isEmpty()) {
            final String curr = ConfigManager.getAppCurrencySymbol(locale);
            int i = 1;
            for (final UserAccount userAcc : userAccList) {
                final Account acc = userAcc.getAccount();
                lines.add(new String[] { String.format("Account #%d", i++),
                        String.format("%s %s", curr,
                                acc.getBalance().toPlainString()) });
            }
        }
        //
        writer.writeAll(lines);
        writer.flush();
    }

    /**
     * Writes CSV for Documents.
     *
     * @param writer
     *            The CVS writer
     * @param user
     *            The user.
     * @throws IOException
     *             When IO error.
     */
    private void exportDocumentLog(final CSVWriter writer, final User user)
            throws IOException {

        // Header
        writer.writeNext(new String[] { "Created", "Type", "Mode", "Protocol",
                "From", "To", "Accepted", "Reason", "State", "Mime", "DRM",
                "Format", "Title", "Bytes", "Pages", "NumberUp", "Copies",
                "Sheets", "Duplex", "Color", "EcoPrint", "Letterhead",
                "RemoveGraphics", "Cost", "Refunded", "CostOrig", "UUID",
                "Signature", "Author", "Subject", "Keywords", "Encrypted",
                "OwnerPw", "UserPw", "Comment", "Details" });

        final TypedQuery<DocLog> query = ServiceContext.getDaoContext()
                .getDocLogDao().getExportQuery(user);

        query.setMaxResults(this.maxExportResults);

        int startPosition = 0;

        while (true) {
            query.setFirstResult(startPosition);
            final List<DocLog> list = query.getResultList();
            for (final DocLog docLog : list) {
                writer.writeNext(getRow(docLog));
            }
            writer.flush();
            if (list.size() < this.maxExportResults) {
                break;
            }
            startPosition += this.maxExportResults;
        }
    }

    /**
     * Creates a Document Log row.
     *
     * @param docLog
     *            The {@link DocLog}.
     * @return Columns value array.
     */
    private String[] getRow(final DocLog docLog) {

        final DocIn docIn = docLog.getDocIn();
        final PrintIn printIn;

        if (docIn == null) {
            printIn = null;
        } else {
            printIn = docIn.getPrintIn();
        }
        final DocOut docOut = docLog.getDocOut();
        final PrintOut printOut;
        final PdfOut pdfOut;
        if (docOut == null) {
            printOut = null;
            pdfOut = null;
        } else {
            printOut = docOut.getPrintOut();
            pdfOut = docOut.getPdfOut();
        }

        final Object docType;
        if (printIn != null) {
            docType = printIn;
        } else if (printOut != null) {
            docType = printOut;
        } else if (pdfOut != null) {
            docType = pdfOut;
        } else {
            docType = docLog;
        }

        // DocLog
        String refunded = null;
        String costOriginal = null;
        String drm = null;

        if (docLog.getRefunded() != null
                && docLog.getRefunded().booleanValue()) {
            refunded = docLog.getRefunded().toString();
            costOriginal = docLog.getCostOriginal().toPlainString();
        }

        if (docLog.getDrmRestricted().booleanValue()) {
            drm = docLog.getDrmRestricted().toString();
        }

        // DocIn
        String originatorIp = null;
        if (docIn != null) {
            originatorIp = docIn.getOriginatorIp();
        }

        String paperSize = null;
        if (printIn != null) {
            paperSize = printIn.getPaperSize();
        } else if (printOut != null) {
            paperSize = printOut.getPaperSize();
        }

        String printInAccepted = null;
        String deniedReason = null;

        if (printIn != null) {
            printInAccepted = printIn.getPrinted().toString();
            deniedReason = printIn.getDeniedReason();
        }

        // PrintOut
        String printMode = null;
        String printedCopies = null;
        String printedSheets = null;
        String ippOptions = null;
        String printedDuplex = null;
        String printedColor = null;
        String numberUp = null;
        String printOutState = null;

        if (printOut != null) {
            printMode = printOut.getPrintMode();
            numberUp = printOut.getCupsNumberUp();
            printedCopies = printOut.getNumberOfCopies().toString();
            printedSheets = printOut.getNumberOfSheets().toString();
            printedDuplex = printOut.getDuplex().toString();
            printedColor = Boolean.toString(!printOut.getGrayscale());
            ippOptions = printOut.getIppOptions();
            final IppJobStateEnum state =
                    IppJobStateEnum.asEnum(printOut.getCupsJobState());
            if (state != null) {
                printOutState = state.asLogText();
            }
        }

        String destination = null;
        String eco = null;
        String letterhead = null;
        String removeGraphics = null;
        String signature = null;

        if (docOut != null) {
            destination = docOut.getDestination();
            eco = docOut.getEcoPrint().toString();
            if (docOut.getLetterhead() != null) {
                letterhead = docOut.getLetterhead().toString();
            }
            removeGraphics = docOut.getRemoveGraphics().toString();
            signature = docOut.getSignature();
        } else if (printIn != null) {
            destination = String.format("/%s", printIn.getQueue().getUrlPath());
        }

        String pdfAuthor = null;
        String pdfSubject = null;
        String pdfKeywords = null;
        String pdfEncrypted = null;
        String pdfPwOwner = null;
        String pdfPwUser = null;

        if (pdfOut != null) {
            pdfAuthor = pdfOut.getAuthor();
            pdfSubject = pdfOut.getSubject();
            pdfKeywords = pdfOut.getKeywords();
            pdfEncrypted = pdfOut.getEncrypted().toString();
            if (!StringUtils.defaultString(pdfOut.getPasswordOwner())
                    .isEmpty()) {
                pdfPwOwner = SECRET_VALUE;
            }
            if (!StringUtils.defaultString(pdfOut.getPasswordUser())
                    .isEmpty()) {
                pdfPwUser = SECRET_VALUE;
            }
        }

        return new String[] { //
                this.dateFormat.format(docLog.getCreatedDate()), //
                docType.getClass().getSimpleName().toUpperCase(), //
                printMode, //
                docLog.getDeliveryProtocol(), //
                originatorIp, destination, //
                printInAccepted, deniedReason, //
                printOutState, //
                docLog.getMimetype(), //
                drm, //
                paperSize, //
                docLog.getTitle(), //
                docLog.getNumberOfBytes().toString(), //
                docLog.getNumberOfPages().toString(), //
                numberUp, //
                printedCopies, printedSheets, printedDuplex, printedColor, //
                eco, letterhead, removeGraphics, //
                docLog.getCost().toPlainString(), //
                refunded, costOriginal, //
                docLog.getUuid(), signature, //
                pdfAuthor, pdfSubject, pdfKeywords, pdfEncrypted, //
                pdfPwOwner, pdfPwUser, //
                docLog.getLogComment(), ippOptions //
        };
    }

    /**
     * Writes CSV for Transactions.
     *
     * @param writer
     *            The CVS writer
     * @param user
     *            The user.
     * @throws IOException
     *             When IO error.
     */
    private void exportTransactionLog(final CSVWriter writer, final User user)
            throws IOException {

        // Header
        writer.writeNext(new String[] { "Date", "Account", "Type", "Currency",
                "Amount", "Balance", "Document", "Title", "Pages", "Copies",
                "Sheets", "Receipt", "Method", "Payment", "Reason",
                "Comment" });

        final TypedQuery<AccountTrx> query = ServiceContext.getDaoContext()
                .getAccountTrxDao().getExportQuery(user);

        query.setMaxResults(this.maxExportResults);

        int startPosition = 0;

        while (true) {
            query.setFirstResult(startPosition);
            final List<AccountTrx> list = query.getResultList();
            for (final AccountTrx trx : list) {
                writer.writeNext(getRow(trx));
            }
            writer.flush();
            if (list.size() < this.maxExportResults) {
                break;
            }
            startPosition += this.maxExportResults;
        }
    }

    /**
     * Creates a Transaction Log row.
     *
     * @param trx
     *            The {@link AccountTrx}.
     * @return Columns value array.
     */
    private String[] getRow(final AccountTrx trx) {

        final DocLog docLog = trx.getDocLog();
        final PosPurchase posPurchase = trx.getPosPurchase();
        final AccountVoucher voucher = trx.getAccountVoucher();
        final CostChange costChange = trx.getCostChange();

        String pages = null;
        String uuid = null;
        String title = null;

        if (docLog != null) {
            pages = docLog.getNumberOfPages().toString();
            uuid = docLog.getUuid();
            title = docLog.getTitle();
        }

        PrintOut printOut = null;

        if (docLog != null && docLog.getDocOut() != null
                && docLog.getDocOut().getPrintOut() != null) {
            printOut = docLog.getDocOut().getPrintOut();
        }

        String copies = null;
        String sheets = null;

        if (printOut != null) {
            final int nCopies = trx.getTransactionWeight().intValue();
            final int nSheets =
                    nCopies * printOut.getNumberOfSheets().intValue()
                            / printOut.getNumberOfCopies().intValue();
            copies = String.valueOf(nCopies);
            sheets = String.valueOf(nSheets);
        }

        String receipt = null;
        String paymentType = null;
        if (posPurchase != null) {
            receipt = posPurchase.getReceiptNumber();
            paymentType = posPurchase.getPaymentType();
        }

        String reason = null;
        if (costChange != null) {
            reason = costChange.getChgReason();
        } else if (voucher != null) {
            reason = voucher.getCardNumber();
        } else if (printOut != null) {
            if (StringUtils.isNotBlank(docLog.getExternalId())) {
                reason = docLog.getExternalId();
            }
        }

        return new String[] { //
                this.dateFormat.format(trx.getTransactionDate()), //
                trx.getAccount().getAccountType(), //
                trx.getTrxType(), //
                trx.getCurrencyCode(), //
                trx.getAmount().toPlainString(), //
                trx.getBalance().toPlainString(), //
                uuid, title, pages, copies, sheets, //
                receipt, trx.getExtMethod(), paymentType, //
                reason, trx.getComment(), //
        };
    }
}
