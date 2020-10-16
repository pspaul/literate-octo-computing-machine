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
package org.savapage.server.xmlrpc;

import java.util.HashMap;
import java.util.Map;

import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.ipp.IppPrinterStateEnum;
import org.savapage.core.print.proxy.ProxyPrintJobStatusCups;
import org.savapage.core.print.proxy.ProxyPrintJobStatusMonitor;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class CupsEventHandler implements ServiceEntryPoint {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(CupsEventHandler.class);

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final String EVENT_JOB_COMPLETED = "job-completed";
    /** */
    private static final String EVENT_JOB_CREATED = "job-created";
    /** */
    private static final String EVENT_JOB_PROGRESS = "job-progress";
    /** */
    private static final String EVENT_JOB_STOPPED = "job-stopped";

    /**
     * Notification from custom SavaPage CUPS notifier.
     * <p>
     * <b>IMPORTANT</b>: The "notify-recipient-uri" option in the
     * cupsCreateJob() is a one-shot notification. E.g. when the printer is
     * disabled, the notification will deliver the status PENDING. When the
     * printer is enabled again NO notification with status COMPLETED will be
     * observed.
     * </p>
     * <p>
     * The <i>user</i> is not passed as parameter, because the user is
     * "withheld" from the CUPS API client by default. This is a security mojo
     * in CUPS 1.5.0 that is controlled by the JobPrivateAccess and
     * JobPrivateValues directives in each operation policy. Basically, by
     * default, all "personal" information is hidden in jobs unless you are an
     * admin or the owner of the job. You can disable this pretty easily to get
     * the pre-1.5.0 behavior by replacing the existing lines in cupsd.conf
     * with:
     *
     * {@code JobPrivateAccess all JobPrivateValues none}
     *
     * and then restarting cupsd (editing via the web interface will do this for
     * you...)
     *
     * See: <a href=
     * "http://comments.gmane.org/gmane.comp.printing.cups.general/28645"
     * >here</a>.
     * </p>
     * <p>
     * NOTE: we can do without the user, since jobId, creationTime and
     * printerName can be used to uniquely identify the print job. Beware, the
     * use case of restored backup on a new SavaPage server with a different
     * job_id counter.
     * </p>
     *
     * @param apiId
     *            API ID.
     * @param apiKey
     *            API key.
     * @param jobId
     * @param jobName
     * @param jobState
     * @param creationTime
     * @param completedTime
     * @param printerName
     *            Printer name.
     * @param printerState
     *            Printer state.
     * @return The XML-RPC object map.
     */
    public Map<String, Object> jobEvent(//
            final String apiId, //
            final String apiKey, //
            final String event, //
            final Integer jobId, //
            final String jobName, //
            final Integer jobState, //
            final Integer creationTime, //
            final Integer completedTime, //
            final String printerName, //
            final Integer printerState //
    ) {

        final Map<String, Object> map = new HashMap<String, Object>();

        Integer rc = 1;
        String msgError = null;

        ServiceContext.open();

        try {
            /*
             * NOTE: when apiId/Key is invalid an exception is thrown.
             */
            MemberCard.instance().validateContent(apiId, apiKey);

            /*
             * IMPORTANT: CUPS notifies job state CHANGES, but it continuously
             * (every second) notifies status PROCESSING while job is being
             * processed.
             */

            String printerStateTxt = null;
            try {
                printerStateTxt =
                        IppPrinterStateEnum.asEnum(printerState).asLogText();
            } catch (Exception e) {
                printerStateTxt = "?";
            }

            IppJobStateEnum ippJobState = null;

            try {
                ippJobState = IppJobStateEnum.asEnum(jobState);
            } catch (Exception e) {
                LOGGER.warn("Printer [{}] [{}] Job #{}: {} [{}]: {}",
                        printerName, printerStateTxt, jobId, event, jobState,
                        e.getMessage());
            }

            if (ippJobState != null) {

                // Correction of job state and completed time?
                IppJobStateEnum ippStateCorr = ippJobState;
                Integer completedTimeCorr = completedTime;

                if (ippJobState == IppJobStateEnum.IPP_JOB_UNKNOWN) {

                    if (event.equals(EVENT_JOB_PROGRESS)) {
                        ippStateCorr = IppJobStateEnum.IPP_JOB_PROCESSING;

                    } else if (event.equals(EVENT_JOB_CREATED)) {
                        ippStateCorr = IppJobStateEnum.IPP_JOB_PENDING;

                    } else if (event.equals(EVENT_JOB_COMPLETED)) {
                        ippStateCorr = IppJobStateEnum.IPP_JOB_COMPLETED;

                    } else if (event.equals(EVENT_JOB_STOPPED)) {
                        ippStateCorr = IppJobStateEnum.IPP_JOB_STOPPED;
                    }

                    if (ippStateCorr == IppJobStateEnum.IPP_JOB_UNKNOWN
                            && completedTimeCorr == null) {
                        completedTimeCorr =
                                PROXY_PRINT_SERVICE.getCupsSystemTime();
                    }
                }

                if (ippStateCorr.equals(ippJobState)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Printer [{}] [{}] Job #{}: {} [{}]",
                                printerName, printerStateTxt, jobId, event,
                                ippJobState.asLogText());
                    }
                } else {
                    LOGGER.warn("Printer [{}] [{}] Job #{}: {} [{}]->[{}]",
                            printerName, printerStateTxt, jobId, event,
                            ippJobState.asLogText(), ippStateCorr.asLogText());
                    ippJobState = ippStateCorr;
                }

                final ProxyPrintJobStatusCups jobStatus =
                        new ProxyPrintJobStatusCups(printerName, jobId, jobName,
                                ippJobState);

                jobStatus.setCupsCreationTime(creationTime);
                jobStatus.setCupsCompletedTime(completedTimeCorr);

                jobStatus.setUpdateTime(System.currentTimeMillis());

                /*
                 * We pass the job status to the monitor who detect and handle
                 * state changes.
                 */
                ProxyPrintJobStatusMonitor.notify(jobStatus);
            }

            rc = 0;

        } catch (Exception ex) {

            msgError = ex.getMessage();

            if (!ConfigManager.isShutdownInProgress()) {
                LOGGER.error(ex.getMessage(), ex);
            }

        } finally {

            try {

                ServiceContext.close();

            } catch (Exception ex) {

                if (!ConfigManager.isShutdownInProgress()) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }

        }

        if (msgError != null) {
            map.put("error", msgError);
        }

        map.put("rc", rc);
        return map;
    }

    /**
     *
     * @param apiId
     * @param apiKey
     * @param event
     * @param printer_name
     * @param printer_state
     * @return
     */
    public Map<String, Object> printerEvent(final String apiId,
            final String apiKey, final String event, final String printer_name,
            final Integer printer_state) {

        final Map<String, Object> map = new HashMap<String, Object>();

        Integer rc = 1;
        String msgError = null;

        ServiceContext.open();

        try {
            /*
             * NOTE: when apiId/Key is invalid an exception is thrown.
             */
            MemberCard.instance().validateContent(apiId, apiKey);
            rc = 0;

            PROXY_PRINT_SERVICE.notificationRecipient().onPrinterEvent(event,
                    printer_name, printer_state);

        } catch (Exception ex) {

            if (!ConfigManager.isShutdownInProgress()) {
                msgError = ex.getMessage();
            }

        } finally {
            try {
                ServiceContext.close();
            } catch (Exception ex) {

                if (!ConfigManager.isShutdownInProgress()) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }

        if (msgError != null) {
            map.put("error", msgError);
            LOGGER.error(msgError);
        }

        map.put("rc", rc);
        return map;
    }

    /**
     *
     * @param apiId
     * @param apiKey
     * @param event
     * @return
     */
    public Map<String, Object> serverEvent(final String apiId,
            final String apiKey, final String event) {

        final Map<String, Object> map = new HashMap<String, Object>();

        Integer rc = 1;
        String msgError = null;

        ServiceContext.open();

        try {
            /*
             * NOTE: when apiId/Key is invalid an exception is thrown.
             */
            MemberCard.instance().validateContent(apiId, apiKey);
            rc = 0;

            PROXY_PRINT_SERVICE.notificationRecipient().onServerEvent(event);

        } catch (Exception ex) {

            if (!ConfigManager.isShutdownInProgress()) {
                msgError = ex.getMessage();
            }

        } finally {

            try {
                ServiceContext.close();
            } catch (Exception ex) {

                if (!ConfigManager.isShutdownInProgress()) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }

        if (msgError != null) {
            map.put("error", msgError);
            LOGGER.error(msgError);
        }

        map.put("rc", rc);
        return map;
    }
}
