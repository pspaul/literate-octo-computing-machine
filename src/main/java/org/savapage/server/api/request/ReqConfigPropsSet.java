/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.api.request;

import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.savapage.core.SpException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.PullPushEnum;
import org.savapage.core.config.validator.ValidationResult;
import org.savapage.core.config.validator.ValidationStatusEnum;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.SOfficeService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.JobTicketLabelCache;
import org.savapage.core.services.helpers.SOfficeConfigProps;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.ext.papercut.services.PaperCutService;
import org.savapage.server.dropzone.WebPrintHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates one or more configuration properties.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqConfigPropsSet extends ApiRequestMixin {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReqConfigPropsSet.class);

    /** */
    private static final SOfficeService SOFFICE_SERVICE =
            ServiceContext.getServiceFactory().getSOfficeService();

    /** */
    private static final PaperCutService PAPERCUT_SERVICE =
            ServiceContext.getServiceFactory().getPaperCutService();

    /** */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final IppQueueDao IPP_QUEUE_DAO =
            ServiceContext.getDaoContext().getIppQueueDao();

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        String msgKey = "msg-config-props-applied";

        final JsonNode list;

        try {
            list = new ObjectMapper().readTree(this.getParmValueDto());
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

        final ConfigManager cm = ConfigManager.instance();

        final Iterator<String> iter = list.getFieldNames();

        boolean isSOfficeUpdate = false;
        boolean isSOfficeTrigger = false;

        boolean isCUPSNotifierUpdate = false;
        PullPushEnum cupsNotifierMethodPrv = null;

        boolean isValid = true;
        int nJobsRescheduled = 0;
        int nValid = 0;

        while (iter.hasNext() && isValid) {

            final String key = iter.next();

            String value = list.get(key).getTextValue();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("%s = %s", key, value));
            }

            final Key configKey = cm.getConfigKey(key);

            if (configKey == null) {
                throw new IllegalArgumentException(key);
            }

            /*
             * If this value is Locale formatted, we MUST revert to locale
             * independent format.
             */
            if (cm.isConfigBigDecimal(configKey)) {
                try {
                    value = BigDecimalUtil.toPlainString(value,
                            this.getLocale(), true);
                } catch (ParseException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }

            /*
             *
             */
            if (!customConfigPropValidate(configKey, value)) {
                return;
            }

            final ValidationResult res = cm.validate(configKey, value);
            isValid = res.isValid();

            if (isValid) {

                boolean preValue = false;

                switch (configKey) {

                case SYS_DEFAULT_LOCALE:
                    ConfigManager.setDefaultLocale(value);
                    break;
                case PRINT_IMAP_ENABLE:
                    preValue = cm.isConfigValue(configKey);
                    isSOfficeTrigger = true;
                    break;
                case PAPERCUT_ENABLE:
                    preValue = cm.isConfigValue(configKey);
                    break;
                case WEB_PRINT_ENABLE:
                    isSOfficeTrigger = true;
                    break;
                case CUPS_IPP_NOTIFICATION_METHOD:
                    isCUPSNotifierUpdate = true;
                    cupsNotifierMethodPrv =
                            cm.getConfigEnum(PullPushEnum.class, configKey);
                    break;

                default:
                    break;
                }

                /*
                 * TODO: This updates the cache while database is not committed
                 * yet! When database transaction is rolled back back the cache
                 * is dirty.
                 */
                cm.updateConfigKey(configKey, value, requestingUser);

                // Reschedule.
                switch (configKey) {

                case SCHEDULE_HOURLY:
                case SCHEDULE_DAILY:
                case SCHEDULE_WEEKLY:
                case SCHEDULE_MONTHLY:
                case SCHEDULE_DAILY_MAINT:
                    SpJobScheduler.instance().scheduleJobs(configKey);
                    nJobsRescheduled++;
                    break;
                default:
                    break;
                }

                nValid++;

                if (configKey == Key.PAPERCUT_ENABLE) {

                    if (preValue && !cm.isConfigValue(configKey)) {
                        SpJobScheduler.interruptPaperCutPrintMonitor();
                        msgKey = "msg-config-props-applied-"
                                + "papercut-print-monitor-stopped";
                        PAPERCUT_SERVICE.resetDbConnectionPool();
                    } else if (!preValue && cm.isConfigValue(configKey)) {
                        SpJobScheduler.instance()
                                .scheduleOneShotPaperCutPrintMonitor(0);
                        msgKey = "msg-config-props-applied-"
                                + "papercut-print-monitor-started";
                        PAPERCUT_SERVICE.resetDbConnectionPool();
                    }

                } else if (configKey == Key.WEB_PRINT_ENABLE) {

                    IPP_QUEUE_DAO.updateDisabled(ReservedIppQueueEnum.WEBPRINT,
                            !cm.isConfigValue(configKey));

                } else if (configKey == Key.PRINT_IMAP_ENABLE) {

                    final boolean newValue = cm.isConfigValue(configKey);

                    IPP_QUEUE_DAO.updateDisabled(ReservedIppQueueEnum.MAILPRINT,
                            !newValue);

                    if (preValue && !newValue) {
                        if (SpJobScheduler.interruptMailPrintListener()) {
                            msgKey = "msg-config-props-applied-mail-print-stopped";
                        }
                    }

                } else if (configKey == Key.DOC_CONVERT_LIBRE_OFFICE_ENABLED
                        || configKey == Key.SOFFICE_ENABLE) {
                    isSOfficeUpdate = true;

                } else if (configKey == Key.WEB_PRINT_FILE_EXT_EXCLUDE) {
                    // Provoke errors/warnings in server.log.
                    WebPrintHelper.getExcludeTypes();

                } else if (configKey == Key.JOBTICKET_DOMAINS) {
                    JobTicketLabelCache.initTicketDomains(value);

                } else if (configKey == Key.JOBTICKET_USES) {
                    JobTicketLabelCache.initTicketUses(value);

                } else if (configKey == Key.JOBTICKET_TAGS) {
                    JobTicketLabelCache.initTicketTags(value,
                            cm.getConfigValue(Key.JOBTICKET_TAGS_1));

                } else if (configKey == Key.JOBTICKET_TAGS_1) {
                    JobTicketLabelCache.initTicketTags(
                            cm.getConfigValue(Key.JOBTICKET_TAGS), value);
                }

            } else {
                if (res.getStatus() == ValidationStatusEnum.ERROR_MAX_LEN_EXCEEDED) {
                    setApiResultText(ApiResultCodeEnum.ERROR, res.getMessage());
                } else {
                    setApiResult(ApiResultCodeEnum.ERROR,
                            "msg-config-props-error", res.getValue());
                }
            }

        } // end-while

        if (nValid > 0) {
            cm.calcRunnable();
        }

        if (isValid) {

            if (nJobsRescheduled > 0) {
                msgKey = "msg-config-props-applied-rescheduled";

            } else if (isCUPSNotifierUpdate) {
                evaluateCUPSNotifierMethod(cm, cupsNotifierMethodPrv);
            } else if (isSOfficeTrigger) {
                evaluateSOfficeService(cm, false);
            } else if (isSOfficeUpdate) {
                evaluateSOfficeService(cm, true);
            }

            setApiResult(ApiResultCodeEnum.OK, msgKey);
        }
    }

    /**
     * Evaluates the application configuration and decides to (re)start or
     * shutdown the {@link SOfficeService}.
     *
     * @param cm
     *            The {@link ConfigManager}.
     * @param restart
     *            if {@code true} the service is restarted, if {@code false} it
     *            is started when currently shutdown.
     */
    private static void evaluateSOfficeService(final ConfigManager cm,
            final boolean restart) {

        final boolean dependentServices = cm.isConfigValue(Key.WEB_PRINT_ENABLE)
                || cm.isConfigValue(Key.PRINT_IMAP_ENABLE);

        if (dependentServices
                && cm.isConfigValue(Key.DOC_CONVERT_LIBRE_OFFICE_ENABLED)
                && cm.isConfigValue(Key.SOFFICE_ENABLE)) {

            final SOfficeConfigProps props = new SOfficeConfigProps();

            if (restart) {
                SOFFICE_SERVICE.restart(props);
            } else {
                SOFFICE_SERVICE.start(props);
            }

        } else {
            SOFFICE_SERVICE.shutdown();
        }
    }

    /**
     *
     * @param cm
     *            The {@link ConfigManager}.
     * @param valuePrevious
     *            Previous value.
     * @throws IOException
     *             When IPP error.
     */
    private void evaluateCUPSNotifierMethod(final ConfigManager cm,
            final PullPushEnum valuePrevious) throws IOException {

        final PullPushEnum curr = cm.getConfigEnum(PullPushEnum.class,
                Key.CUPS_IPP_NOTIFICATION_METHOD);

        if (valuePrevious == curr) {
            return;
        }

        try {
            if (curr == PullPushEnum.PULL) {
                PROXYPRINT_SERVICE.stopCUPSEventSubscription();
                SpJobScheduler.pauseCUPSPushEventRenewal();
            } else {
                PROXYPRINT_SERVICE.startCUPSPushEventSubscription();
                SpJobScheduler.resumeCUPSPushEventRenewal();
            }

            AdminPublisher.instance().publish(PubTopicEnum.IPP,
                    PubLevelEnum.INFO,
                    this.localize(ConfigManager.getDefaultLocale(),
                            "msg-cups-notifier-method-changed",
                            curr.toString()));

        } catch (IppConnectException | IppSyntaxException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Custom validates a single {@link IConfigProp.Key} value.
     *
     * @param key
     *            The key of the configuration item.
     * @param value
     *            The value of the configuration item.
     * @return {@code null} when NO validation error, or the userData object
     *         filled with the error message when an error is encountered..
     */
    private boolean customConfigPropValidate(final Key key,
            final String value) {

        if (key == Key.PROXY_PRINT_NON_SECURE_PRINTER_GROUP
                && StringUtils.isNotBlank(value)) {

            final PrinterGroup jpaPrinterGroup = ServiceContext.getDaoContext()
                    .getPrinterGroupDao().findByName(value);

            if (jpaPrinterGroup == null) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-device-printer-group-not-found", value);
                return false;
            }
        }

        if (StringUtils.isNotBlank(value)) {
            try {
                if (key == Key.JOBTICKET_DOMAINS) {
                    JobTicketLabelCache.parseTicketDomains(value);
                } else if (key == Key.JOBTICKET_USES) {
                    JobTicketLabelCache.parseTicketUses(value);
                } else if (key == Key.JOBTICKET_TAGS
                        || key == Key.JOBTICKET_TAGS_1) {
                    JobTicketLabelCache.parseTicketTags(value);
                }
            } catch (IllegalArgumentException e) {
                setApiResultText(ApiResultCodeEnum.ERROR, e.getMessage());
                return false;
            }
        }
        return true;
    }

}
