/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server;

import java.util.HashMap;
import java.util.Map;

import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;

/**
 * A dedicated class for initializing the JSON API dictionary at the right time.
 *
 * @author Datraverse B.V.
 */
public class JsonApiDict {

    public static final String PARM_REQ = "request";
    public static final String PARM_USER = "user";

    public static final String PARM_REQ_SUB = "request-sub";
    public static final String PARM_DATA = "data";

    public static final String REQ_ACCOUNT_VOUCHER_BATCH_CREATE =
            "account-voucher-batch-create";
    public static final String REQ_ACCOUNT_VOUCHER_BATCH_DELETE =
            "account-voucher-batch-delete";
    public static final String REQ_ACCOUNT_VOUCHER_BATCH_EXPIRE =
            "account-voucher-batch-expire";
    public static final String REQ_ACCOUNT_VOUCHER_BATCH_PRINT =
            "account-voucher-batch-print";
    public static final String REQ_ACCOUNT_VOUCHER_DELETE_EXPIRED =
            "account-voucher-delete-expired";
    public static final String REQ_ACCOUNT_VOUCHER_REDEEM =
            "account-voucher-redeem";

    public static final String REQ_CARD_IS_REGISTERED = "card-is-registered";
    public static final String REQ_CONFIG_GET_PROP = "config-get-prop";
    public static final String REQ_CONFIG_SET_PROPS = "config-set-props";
    public static final String REQ_CONSTANTS = "constants";
    public static final String REQ_DB_BACKUP = "db-backup";
    public static final String REQ_DEVICE_DELETE = "device-delete";
    public static final String REQ_DEVICE_GET = "device-get";
    public static final String REQ_DEVICE_SET = "device-set";
    public static final String REQ_EXIT_EVENT_MONITOR = "exit-event-monitor";
    public static final String REQ_GCP_GET_DETAILS = "gcp-get-details";
    public static final String REQ_GCP_ONLINE = "gcp-online";
    public static final String REQ_GCP_REGISTER = "gcp-register";
    public static final String REQ_GCP_SET_DETAILS = "gcp-set-details";
    public static final String REQ_GCP_SET_NOTIFICATIONS =
            "gcp-set-notifications";
    public static final String REQ_GET_EVENT = "get-event";

    public static final String REQ_IMAP_TEST = "imap-test";
    public static final String REQ_IMAP_START = "imap-start";
    public static final String REQ_IMAP_STOP = "imap-stop";

    public static final String REQ_JOB_DELETE = "job-delete";
    public static final String REQ_JOB_EDIT = "job-edit";
    public static final String REQ_JOB_PAGES = "job-pages";
    public static final String REQ_INBOX_IS_VANILLA = "inbox-is-vanilla";
    public static final String REQ_JQPLOT = "jqplot";
    public static final String REQ_LANGUAGE = "language";

    public static final String REQ_PAPERCUT_TEST = "papercut-test";

    public static final String REQ_SMARTSCHOOL_TEST = "smartschool-test";
    public static final String REQ_SMARTSCHOOL_START = "smartschool-start";
    public static final String REQ_SMARTSCHOOL_START_SIMULATE =
            "smartschool-start-simulate";
    public static final String REQ_SMARTSCHOOL_STOP = "smartschool-stop";

    public static final String REQ_LETTERHEAD_ATTACH = "letterhead-attach";
    public static final String REQ_LETTERHEAD_DELETE = "letterhead-delete";
    public static final String REQ_LETTERHEAD_DETACH = "letterhead-detach";
    public static final String REQ_LETTERHEAD_GET = "letterhead-get";
    public static final String REQ_LETTERHEAD_LIST = "letterhead-list";
    public static final String REQ_LETTERHEAD_NEW = "letterhead-new";
    public static final String REQ_LETTERHEAD_SET = "letterhead-set";

    public static final String REQ_LOGIN = "login";
    public static final String REQ_LOGOUT = "logout";

    public static final String REQ_WEBAPP_UNLOAD = "webapp-unload";
    public static final String REQ_WEBAPP_CLOSE_SESSION =
            "webapp-close-session";

    public static final String REQ_MAIL_TEST = "mail-test";
    public static final String REQ_PAGOMETER_RESET = "pagometer-reset";

    public static final String REQ_OUTBOX_CLEAR = "outbox-clear";
    public static final String REQ_OUTBOX_DELETE_JOB = "outbox-delete-job";
    public static final String REQ_OUTBOX_EXTEND = "outbox-extend";

    public static final String REQ_PAGE_DELETE = "page-delete";
    public static final String REQ_PAGE_MOVE = "page-move";

    public static final String REQ_PDF = "pdf";
    public static final String REQ_PDF_GET_PROPERTIES = "pdf-get-properties";
    public static final String REQ_PDF_SET_PROPERTIES = "pdf-set-properties";

    public static final String REQ_PING = "ping";

    public static final String REQ_USER_PAYMENT_REQUEST =
            "user-payment-request";

    public static final String REQ_POS_DEPOSIT = "pos-deposit";
    public static final String REQ_POS_RECEIPT_DOWNLOAD =
            "pos-receipt-download";
    public static final String REQ_POS_RECEIPT_DOWNLOAD_USER =
            "pos-receipt-download-user";

    public static final String REQ_POS_RECEIPT_SENDMAIL =
            "pos-receipt-sendmail";

    public static final String REQ_POS_DEPOSIT_QUICK_SEARCH =
            "pos-deposit-quick-search";

    public static final String REQ_PRINT_AUTH_CANCEL = "print-auth-cancel";
    public static final String REQ_PRINT_FAST_RENEW = "print-fast-renew";

    public static final String REQ_PRINTER_OPT_DOWNLOAD =
            "printer-opt-download";

    public static final String REQ_PRINTER_DETAIL = "printer-detail";
    public static final String REQ_PRINTER_GET = "printer-get";
    public static final String REQ_PRINTER_LIST = "printer-list";
    public static final String REQ_PRINTER_PRINT = "printer-print";
    public static final String REQ_PRINTER_RENAME = "printer-rename";
    public static final String REQ_PRINTER_QUICK_SEARCH =
            "printer-quick-search";
    public static final String REQ_PRINTER_SET = "printer-set";
    public static final String REQ_PRINTER_SET_MEDIA_COST =
            "printer-set-media-cost";
    public static final String REQ_PRINTER_SET_MEDIA_SOURCES =
            "printer-set-media-sources";

    public static final String REQ_PRINTER_SYNC = "printer-sync";

    public static final String REQ_QUEUE_GET = "queue-get";
    public static final String REQ_QUEUE_SET = "queue-set";

    public static final String REQ_REPORT = "report";

    public static final String REQ_RESET_JMX_PASSWORD = "reset-jmx-pw";
    public static final String REQ_RESET_ADMIN_PASSWORD = "reset-admin-pw";
    public static final String REQ_RESET_USER_PASSWORD = "reset-user-pw";
    public static final String REQ_RESET_USER_PIN = "reset-user-pin";
    public static final String REQ_SEND = "send";
    public static final String REQ_USER_DELETE = "user-delete";
    public static final String REQ_USER_GET = "user-get";
    public static final String REQ_USER_GET_STATS = "user-get-stats";
    public static final String REQ_USER_NOTIFY_ACCOUNT_CHANGE =
            "user-notify-account-change";
    public static final String REQ_USER_QUICK_SEARCH = "user-quick-search";
    public static final String REQ_USER_SET = "user-set";
    public static final String REQ_USER_SOURCE_GROUPS = "user-source-groups";
    public static final String REQ_USER_SYNC = "user-sync";

    /**
     */
    public enum DbClaim {
        /**
         * NO {@link ConfigManager#dbClaim()} or
         * {@link ConfigManager#dbClaimExclusive()} is needed.
         */
        NONE,

        /**
         * {@link ConfigManager#dbClaim()} is needed.
         */
        READ,

        /**
         * {@link ConfigManager#dbClaimExclusive()} is needed.
         */
        EXCLUSIVE
    }

    /**
     *
     */
    public enum LetterheadLock {
        NONE, READ, UPDATE
    }

    /**
     *
     */
    public enum AuthReq {
        NONE, USER, ADMIN
    }

    /**
     *
     */
    public enum DbAccess {
        NO, YES, USER_LOCK
    }

    /**
     *
     */
    private class Req {

        final AuthReq authReq;
        final DbAccess dbAccess;
        final DbClaim dbClaim;

        private Req(AuthReq authReq, DbClaim dbClaim, DbAccess dbAccess) {
            this.dbAccess = dbAccess;
            this.dbClaim = dbClaim;
            this.authReq = authReq;
        }
    };

    /**
     *
     */
    private final Map<String, Req> dict = new HashMap<>();

    /**
     *
     * @param key
     * @param dbClaim
     * @param dbUsage
     * @param authReq
     */
    private void put(final String key, AuthReq authReq, DbClaim dbClaim,
            DbAccess dbUsage) {
        dict.put(key, new Req(authReq, dbClaim, dbUsage));
    }

    /**
     * Puts a user's {@link Req} in the dictionary.
     *
     * @param key
     * @param dbClaim
     * @param dbAccess
     */
    private void usr(final String key, DbClaim dbClaim, DbAccess dbAccess) {
        dict.put(key, new Req(AuthReq.USER, dbClaim, dbAccess));
    }

    /**
     * Puts a administrator's {@link Req} in the dictionary.
     *
     * @param key
     * @param dbClaim
     * @param dbAccess
     */
    private void adm(final String key, DbClaim dbClaim, DbAccess dbAccess) {
        dict.put(key, new Req(AuthReq.ADMIN, dbClaim, dbAccess));
    }

    private void non(final String key) {
        dict.put(key, new Req(AuthReq.NONE, DbClaim.NONE, DbAccess.NO));
    }

    /**
     * Checks which DbClaim the request needs.
     *
     * @param request
     *            The id string of the request.
     * @return The database claim needed.
     */
    public DbClaim getDbClaimNeeded(final String request) {
        return dict.get(request).dbClaim;
    }

    public void lock(final LetterheadLock lock) {
        if (lock == JsonApiDict.LetterheadLock.READ) {
            ReadWriteLockEnum.LETTERHEAD_STORE.setReadLock(true);
        } else if (lock == JsonApiDict.LetterheadLock.UPDATE) {
            ReadWriteLockEnum.LETTERHEAD_STORE.setWriteLock(true);
        }
    }

    /**
     * @param claim
     */
    public void lock(final DbClaim claim) {
        if (claim == JsonApiDict.DbClaim.READ) {
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
        } else if (claim == JsonApiDict.DbClaim.EXCLUSIVE) {
            ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(true);
        }
    }

    public void unlock(final LetterheadLock lock) {
        if (lock == JsonApiDict.LetterheadLock.READ) {
            ReadWriteLockEnum.LETTERHEAD_STORE.setReadLock(false);
        } else if (lock == JsonApiDict.LetterheadLock.UPDATE) {
            ReadWriteLockEnum.LETTERHEAD_STORE.setWriteLock(false);
        }
    }

    /**
     * @param claim
     */
    public void unlock(final DbClaim claim) {
        if (claim == JsonApiDict.DbClaim.READ) {
            ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
        } else if (claim == JsonApiDict.DbClaim.EXCLUSIVE) {
            ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(false);
        }
    }

    /**
     * Checks which Public Letterhead lock is needed for a request.
     * <p>
     * For convenience, we are conservative and assume that request that use
     * letterheads use <b>public</b> letterheads.
     * </p>
     * <p>
     * If a user is an administrator this rules out some possibilities too.
     * </p>
     *
     * @param request
     *            The id string of the request.
     * @param isAdmin
     *            {@code true} if e requesting is an administrator.
     * @return The {@link LetterheadLock} needed.
     */
    public LetterheadLock getLetterheadLockNeeded(final String request,
            final boolean isAdmin) {

        /*
         * @param isPublicContext {@code true} if the request involves a public
         * letterhead.
         */
        boolean isPublicContext = true;

        switch (request) {

        case REQ_LETTERHEAD_ATTACH:
        case REQ_LETTERHEAD_LIST:
        case REQ_LETTERHEAD_GET:
            return LetterheadLock.READ;

        case REQ_LETTERHEAD_DELETE:
        case REQ_LETTERHEAD_SET:
            if (!isAdmin) {
                return LetterheadLock.NONE;
            } else if (isPublicContext) {
                return LetterheadLock.UPDATE;
            }
            return LetterheadLock.NONE;

        case REQ_PDF:
        case REQ_PRINTER_PRINT:
        case REQ_SEND:
            if (isPublicContext) {
                return LetterheadLock.READ;
            }
            return LetterheadLock.NONE;

        case REQ_LETTERHEAD_DETACH:
        case REQ_LETTERHEAD_NEW:
        default:
            return LetterheadLock.NONE;
        }

    }

    /**
     * Is request about downloading a file?
     *
     * @param request
     * @return
     */
    public static boolean isDownloadRequest(final String request) {

        switch (request) {

        case REQ_PDF:
        case REQ_REPORT:
        case REQ_ACCOUNT_VOUCHER_BATCH_PRINT:
        case REQ_POS_RECEIPT_DOWNLOAD:
        case REQ_POS_RECEIPT_DOWNLOAD_USER:
        case REQ_PRINTER_OPT_DOWNLOAD:
            return true;
        default:
            return false;
        }
    }

    /**
     * Checks if the <b>requesting</b> user needs to be locked because of access
     * to database or user file system (safe-pages).
     *
     * @param request
     *            The id string of the request.
     * @return {@code true} if requesting user needs to be locked for this
     *         request.
     */
    public boolean isUserLockNeeded(final String request) {
        return dict.get(request).dbAccess == DbAccess.USER_LOCK;
    }

    /**
     *
     * @param request
     * @return
     */
    public boolean isDbAccessNeeded(final String request) {
        return dict.get(request).dbAccess != DbAccess.NO;
    }

    /**
     * Checks if request is valid.
     *
     * @param request
     *            The request.
     * @return {@code false} is the request is invalid (unknown).
     */
    public boolean isValidRequest(final String request) {
        return dict.get(request) != null;
    }

    /**
     * Checks if the request needs an authenticated user.
     *
     * @param request
     *            The id string of the request.
     * @return {@code true} if the request needs user (or admin) authorization.
     */
    public boolean isAuthenticationNeeded(final String request) {
        return dict.get(request).authReq != AuthReq.NONE;
    }

    /**
     * Checks if the request needs an authenticated administrator.
     *
     * @param request
     *            The id string of the request.
     * @return {@code true} if the request needs an authenticated administrator.
     */
    public boolean isAdminAuthenticationNeeded(final String request) {
        return dict.get(request).authReq == AuthReq.ADMIN;
    }

    /**
     *
     */
    public JsonApiDict() {

        adm(REQ_ACCOUNT_VOUCHER_BATCH_CREATE, DbClaim.READ, DbAccess.YES);
        adm(REQ_ACCOUNT_VOUCHER_BATCH_DELETE, DbClaim.READ, DbAccess.YES);
        adm(REQ_ACCOUNT_VOUCHER_BATCH_EXPIRE, DbClaim.READ, DbAccess.YES);
        adm(REQ_ACCOUNT_VOUCHER_BATCH_PRINT, DbClaim.READ, DbAccess.YES);
        adm(REQ_ACCOUNT_VOUCHER_DELETE_EXPIRED, DbClaim.READ, DbAccess.YES);

        usr(REQ_ACCOUNT_VOUCHER_REDEEM, DbClaim.READ, DbAccess.USER_LOCK);

        put(REQ_CARD_IS_REGISTERED, AuthReq.NONE, DbClaim.READ, DbAccess.YES);
        adm(REQ_CONFIG_GET_PROP, DbClaim.READ, DbAccess.YES);
        adm(REQ_CONFIG_SET_PROPS, DbClaim.READ, DbAccess.YES);
        put(REQ_CONSTANTS, AuthReq.NONE, DbClaim.READ, DbAccess.YES);
        adm(REQ_DB_BACKUP, DbClaim.NONE, DbAccess.NO);
        adm(REQ_DEVICE_DELETE, DbClaim.READ, DbAccess.YES);
        adm(REQ_DEVICE_GET, DbClaim.NONE, DbAccess.YES);
        adm(REQ_DEVICE_SET, DbClaim.READ, DbAccess.YES);
        usr(REQ_EXIT_EVENT_MONITOR, DbClaim.NONE, DbAccess.NO);
        adm(REQ_GCP_GET_DETAILS, DbClaim.READ, DbAccess.YES);
        adm(REQ_GCP_ONLINE, DbClaim.READ, DbAccess.YES);
        adm(REQ_GCP_REGISTER, DbClaim.READ, DbAccess.YES);
        adm(REQ_GCP_SET_DETAILS, DbClaim.READ, DbAccess.YES);
        adm(REQ_GCP_SET_NOTIFICATIONS, DbClaim.READ, DbAccess.YES);
        usr(REQ_GET_EVENT, DbClaim.NONE, DbAccess.USER_LOCK);

        adm(REQ_IMAP_TEST, DbClaim.NONE, DbAccess.NO);
        adm(REQ_IMAP_START, DbClaim.NONE, DbAccess.NO);
        adm(REQ_IMAP_STOP, DbClaim.NONE, DbAccess.NO);

        adm(REQ_PAPERCUT_TEST, DbClaim.NONE, DbAccess.NO);

        adm(REQ_SMARTSCHOOL_TEST, DbClaim.NONE, DbAccess.NO);
        adm(REQ_SMARTSCHOOL_START, DbClaim.NONE, DbAccess.NO);
        adm(REQ_SMARTSCHOOL_START_SIMULATE, DbClaim.NONE, DbAccess.NO);
        adm(REQ_SMARTSCHOOL_STOP, DbClaim.NONE, DbAccess.NO);

        usr(REQ_JOB_DELETE, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_JOB_EDIT, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_JOB_PAGES, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_INBOX_IS_VANILLA, DbClaim.NONE, DbAccess.USER_LOCK);

        usr(REQ_JQPLOT, DbClaim.NONE, DbAccess.YES);
        non(REQ_LANGUAGE);
        usr(REQ_LETTERHEAD_ATTACH, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_LETTERHEAD_DELETE, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_LETTERHEAD_DETACH, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_LETTERHEAD_LIST, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_LETTERHEAD_NEW, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_LETTERHEAD_GET, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_LETTERHEAD_SET, DbClaim.NONE, DbAccess.USER_LOCK);

        put(REQ_LOGIN, AuthReq.NONE, DbClaim.READ, DbAccess.YES);
        usr(REQ_LOGOUT, DbClaim.NONE, DbAccess.NO);
        usr(REQ_WEBAPP_UNLOAD, DbClaim.NONE, DbAccess.NO);
        put(REQ_WEBAPP_CLOSE_SESSION, AuthReq.NONE, DbClaim.NONE, DbAccess.NO);

        adm(REQ_MAIL_TEST, DbClaim.NONE, DbAccess.NO);

        usr(REQ_OUTBOX_CLEAR, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_OUTBOX_DELETE_JOB, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_OUTBOX_EXTEND, DbClaim.NONE, DbAccess.USER_LOCK);

        adm(REQ_PAGOMETER_RESET, DbClaim.EXCLUSIVE, DbAccess.YES);

        usr(REQ_PAGE_DELETE, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_PAGE_MOVE, DbClaim.NONE, DbAccess.USER_LOCK);

        usr(REQ_PDF, DbClaim.READ, DbAccess.USER_LOCK);
        usr(REQ_PDF_GET_PROPERTIES, DbClaim.NONE, DbAccess.YES);
        usr(REQ_PDF_SET_PROPERTIES, DbClaim.READ, DbAccess.YES);

        non(REQ_PING);

        usr(REQ_USER_PAYMENT_REQUEST, DbClaim.READ, DbAccess.YES);

        adm(REQ_POS_DEPOSIT, DbClaim.NONE, DbAccess.YES);
        adm(REQ_POS_RECEIPT_DOWNLOAD, DbClaim.READ, DbAccess.YES);
        usr(REQ_POS_RECEIPT_DOWNLOAD_USER, DbClaim.READ, DbAccess.YES);
        adm(REQ_POS_RECEIPT_SENDMAIL, DbClaim.READ, DbAccess.YES);
        adm(REQ_POS_DEPOSIT_QUICK_SEARCH, DbClaim.READ, DbAccess.YES);

        usr(REQ_PRINT_AUTH_CANCEL, DbClaim.NONE, DbAccess.NO);
        usr(REQ_PRINT_FAST_RENEW, DbClaim.NONE, DbAccess.USER_LOCK);
        usr(REQ_PRINTER_DETAIL, DbClaim.NONE, DbAccess.YES);
        adm(REQ_PRINTER_GET, DbClaim.NONE, DbAccess.YES);
        usr(REQ_PRINTER_LIST, DbClaim.READ, DbAccess.YES);
        adm(REQ_PRINTER_OPT_DOWNLOAD, DbClaim.NONE, DbAccess.YES);
        usr(REQ_PRINTER_PRINT, DbClaim.READ, DbAccess.USER_LOCK);
        usr(REQ_PRINTER_QUICK_SEARCH, DbClaim.READ, DbAccess.YES);
        adm(REQ_PRINTER_SET, DbClaim.READ, DbAccess.YES);
        adm(REQ_PRINTER_SET_MEDIA_COST, DbClaim.READ, DbAccess.YES);
        adm(REQ_PRINTER_SET_MEDIA_SOURCES, DbClaim.READ, DbAccess.YES);
        adm(REQ_PRINTER_RENAME, DbClaim.READ, DbAccess.YES);
        adm(REQ_PRINTER_SYNC, DbClaim.READ, DbAccess.YES);
        adm(REQ_QUEUE_GET, DbClaim.NONE, DbAccess.YES);
        adm(REQ_QUEUE_SET, DbClaim.READ, DbAccess.YES);
        adm(REQ_REPORT, DbClaim.READ, DbAccess.YES);
        adm(REQ_RESET_ADMIN_PASSWORD, DbClaim.NONE, DbAccess.NO);
        adm(REQ_RESET_JMX_PASSWORD, DbClaim.NONE, DbAccess.NO);
        usr(REQ_RESET_USER_PASSWORD, DbClaim.READ, DbAccess.USER_LOCK);
        usr(REQ_RESET_USER_PIN, DbClaim.READ, DbAccess.USER_LOCK);
        usr(REQ_SEND, DbClaim.READ, DbAccess.USER_LOCK);
        adm(REQ_USER_DELETE, DbClaim.READ, DbAccess.YES);
        adm(REQ_USER_GET, DbClaim.READ, DbAccess.YES);
        usr(REQ_USER_GET_STATS, DbClaim.READ, DbAccess.YES);
        adm(REQ_USER_NOTIFY_ACCOUNT_CHANGE, DbClaim.READ, DbAccess.YES);
        adm(REQ_USER_QUICK_SEARCH, DbClaim.READ, DbAccess.YES);
        adm(REQ_USER_SET, DbClaim.READ, DbAccess.YES);
        adm(REQ_USER_SOURCE_GROUPS, DbClaim.NONE, DbAccess.NO);
        adm(REQ_USER_SYNC, DbClaim.NONE, DbAccess.NO);
    }

}
