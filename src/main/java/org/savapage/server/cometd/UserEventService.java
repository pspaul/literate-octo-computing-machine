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
package org.savapage.server.cometd;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.jackson.map.ObjectMapper;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.savapage.core.PerformanceLogger;
import org.savapage.core.ShutdownException;
import org.savapage.core.SpException;
import org.savapage.core.UserNotFoundException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.imaging.ImageUrl;
import org.savapage.core.inbox.PageImages;
import org.savapage.core.inbox.PageImages.PageImage;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.JsonUserMsg;
import org.savapage.core.msg.JsonUserMsgNotification;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.users.AbstractUserSource;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.DateUtil;
import org.savapage.server.api.request.ApiRequestHelper;
import org.savapage.server.auth.ClientAppUserAuthManager;
import org.savapage.server.auth.UserAuthToken;
import org.savapage.server.webapp.WebAppHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to and handles {@linkplain #CHANNEL_SUBSCRIPTION} where
 * clients publish service requests to be notified of personal events like
 * <b>created</b> (printed) and <b>deleted</b> jobs. Events are published on
 * {@linkplain #CHANNEL_PUBLISH}.
 *
 * @author Rijk Ravestein
 *
 */
public final class UserEventService extends AbstractEventService {

    /**
     * .
     */
    private static final boolean ADMIN_PUB_USER_EVENT = true;

    /**
     * .
     */
    private static final InboxService INBOX_SERVICE =
            ServiceContext.getServiceFactory().getInboxService();

    /**
     * .
     */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /**
     * The channel this <i>service</i> <strong>subscribes</strong> (listens) to.
     * <p>
     * This is the channel where the <i>client</i> <strong>publishes</strong>
     * to.
     * </p>
     * <p>
     * This is a <strong>Service</strong> channel (because its name name starts
     * with /services/ and is used from client to server communication),
     * contrary to <strong>Normal</strong> channels (whose name starts with any
     * other string, except '/meta/', and are used to broadcast messages between
     * clients).
     * </p>
     */
    public static final String CHANNEL_SUBSCRIPTION = "/service/user";

    /**
     * The channel this <i>service</i> <strong>publishes</strong> (writes
     * response) to.
     * <p>
     * This is the channel where the <i>client</i> <strong>subscribes</strong>
     * to.
     * </p>
     */
    public static final String CHANNEL_PUBLISH = "/user/event";

    /**
     * The name of the Java method (in this class) to call when messages are
     * received on this channel.
     */
    private static final String CHANNEL_MESSAGE_HANDLER = "monitorUserEvent";

    private static final String KEY_EVENT = "event";
    private static final String KEY_DATA = "data";
    private static final String KEY_ERROR = "error";
    private static final String KEY_JOBS = "jobs";
    private static final String KEY_PAGES = "pages";
    private static final String KEY_MSG_TIME = "msgTime";
    private static final String KEY_URL_TEMPLATE = "url_template";

    /**
     *
     */
    private static final long MSECS_WAIT_BETWEEN_POLLS = 3000;

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserEventService.class);

    /**
     * Number of active User Client sessions.
     */
    private static final AtomicInteger clientAppCount = new AtomicInteger();

    /**
     * Number of active User Web App sessions.
     */
    private static final AtomicInteger userWebAppCount = new AtomicInteger();

    /**
     *
     * @param bayeux
     *            The bayeux instance
     * @param name
     *            The name of the service
     */
    public UserEventService(final BayeuxServer bayeux, final String name) {

        super(bayeux, name);

        addService(CHANNEL_SUBSCRIPTION, CHANNEL_MESSAGE_HANDLER);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("USER EVENT SERVICE ADDED");
        }
    }

    /**
     * @return Number of active User Client sessions.
     */
    public static int getClientAppCount() {
        return clientAppCount.get();
    }

    /**
     * Increment active User Client sessions.
     */
    private static void incrementClientAppCount() {
        clientAppCount.incrementAndGet();
    }

    /**
     * Decrement active User Client sessions.
     */
    private static void decrementClientAppCount() {
        clientAppCount.decrementAndGet();
    }

    /**
     * @return Number of active User Web App sessions.
     */
    public static int getUserWebAppCount() {
        return userWebAppCount.get();
    }

    /**
     * Increment active User Web App sessions.
     */
    private static void incrementUserWebAppCount() {
        userWebAppCount.incrementAndGet();
    }

    /**
     * Decrement active User Web App sessions.
     */
    private static void decrementUserWebAppCount() {
        userWebAppCount.decrementAndGet();
    }

    /**
     * Checks if any messages need to be notified since the previous check date
     * (time).
     *
     * @param clientIpAddress
     *            Client IP address.
     * @param msgPrevMonitorTime
     *            The previous date (time) since messages were monitored.
     * @param user
     *            The user id.
     * @param locale
     *            The user locale.
     * @return The messages to notify, or {@code null} when no user messages are
     *         waiting.
     * @throws IOException
     *             If IO error.
     */
    private Map<String, Object> checkUserMsgIndicator(
            final String clientIpAddress, final Date msgPrevMonitorTime,
            final String user, final Locale locale) throws IOException {

        Map<String, Object> eventData = null;

        final UserMsgIndicator msgIndicator = UserMsgIndicator.read(user);

        if (msgIndicator.hasMessage()) {

            final Date messageDate = msgIndicator.getMessageDate();

            if (msgPrevMonitorTime.before(messageDate)) {

                switch (msgIndicator.getMessage()) {

                case ACCOUNT_INFO:
                    eventData = createAccountMsg(user, locale);
                    break;

                case JOBTICKET_CHANGED:
                case JOBTICKET_DENIED:
                case JOBTICKET_SETTLED_COPY:
                case JOBTICKET_SETTLED_PRINT:
                    eventData = createJobTicketMsg(user,
                            msgIndicator.getMessage(), locale);
                    break;

                case PRINT_IN_DENIED:
                    // no break intended
                case PRINT_OUT_COMPLETED:
                    eventData = createPrintMsg(user, locale, msgPrevMonitorTime,
                            messageDate);
                    break;

                case PRINT_IN_EXPIRED:
                    eventData = createPrintInExpiredMsg(user, locale);
                    break;

                case STOP_POLL_REQ:
                    /*
                     * Do NOT eventData = createUserMessageNullEvent(); because
                     * this will result in an endless loop.
                     */
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format(
                                "STOP_POLL_REQ at start of poll for "
                                        + "user [%s] at [%s] ignored",
                                user, clientIpAddress));
                    }

                    break;

                default:
                    break;
                }

            }
        }

        return eventData;
    }

    /**
     *
     * @param user
     * @param clientIpAddress
     * @param isWebAppClient
     *            {@code true} is client is User Web App, {@code false} if Java
     *            Client.
     * @param userEvent
     */
    private void publishAdminEvent(final String user,
            final String clientIpAddress, final boolean isWebAppClient,
            final UserEventEnum userEvent) {

        final String eventName;
        if (userEvent == UserEventEnum.NULL) {
            eventName = "timeout";
        } else {
            eventName = userEvent.getUiText();
        }

        final String eventSource;
        if (isWebAppClient) {
            eventSource = "Web App";
        } else {
            eventSource = "Client";
        }

        AdminPublisher.instance().publish(PubTopicEnum.USER, PubLevelEnum.INFO,
                this.localize("user-event", user, eventName, eventSource,
                        clientIpAddress));
    }

    /**
     *
     * @param user
     * @param clientIpAddress
     * @param exception
     * @param isWebAppClient
     *            {@code true} is client is User Web App, {@code false} if Java
     *            Client.
     */
    private void publishAdminException(final String user,
            final String clientIpAddress, final Exception exception,
            final boolean isWebAppClient) {

        final StringBuilder pubMsg = new StringBuilder(128);

        pubMsg.append(this.getClass().getSimpleName()).append(": ")
                .append(exception.getClass().getSimpleName()).append(": ")
                .append(exception.getMessage());

        pubMsg.append(" (User \"").append(user).append("\" at ");

        if (isWebAppClient) {
            pubMsg.append("WebApp ");
        } else {
            pubMsg.append("Client ");
        }

        pubMsg.append(clientIpAddress).append(')');

        AdminPublisher.instance().publish(PubTopicEnum.USER, PubLevelEnum.ERROR,
                pubMsg.toString());

        AppLogHelper.logError(this.getClass(), "exception", pubMsg.toString());
    }

    /**
     * Monitors any event that should be notified to a user.
     * <p>
     * NOTE: when listener on this {@link ServerSession} is <i>removed</i> we
     * stop this service immediately. This is done to free the way for a long
     * poll to another CometD service (like {@link ProxyPrintEventService}).
     * This is done "gracefully" by sending the
     * {@link UserMsgIndicator.Msg#STOP_POLL_REQ} message.
     * </p>
     * <p>
     * <i>Remember that long polls are queued on the server side!</i> So,
     * although a long poll will return immediately on the client side, it will
     * start on the server when its turn has come.
     * </p>
     *
     * @param remote
     *            The {@link ServerSession}.
     * @param message
     *            The {@link Message}.
     */
    public void monitorUserEvent(final ServerSession remote,
            final ServerMessage message) {

        final String clientIpAddress = WebAppHelper.getClientIP(message);

        Map<String, Object> input = message.getDataAsMap();

        // Mantis #503
        final String user = AbstractUserSource.asDbUserId(
                (String) input.get("user"), ConfigManager.isLdapUserSync());

        // NOTE: The Java Client does not give a pageOffset.
        final Long pageOffset = (Long) input.get("page-offset");

        final String uniqueUrlValue = (String) input.get("unique-url-value");
        final Boolean base64 = (Boolean) input.get("base64");

        final Locale locale = this.getLocale(input, "language", "country");

        final Long msgPrevTime = (Long) input.get("msg-prev-time");
        final Boolean webAppClient = (Boolean) input.get("webAppClient");

        final boolean isWebAppClient =
                webAppClient != null && webAppClient.booleanValue();

        if (isWebAppClient) {
            incrementUserWebAppCount();
        } else {
            /*
             * Replaces an existing token, so the creation time is refreshed.
             */
            ClientAppUserAuthManager.replaceIpAuthToken(clientIpAddress,
                    new UserAuthToken(user));
            incrementClientAppCount();
        }

        /*
         * Mantis #328
         */
        remote.addListener(new ServerSession.RemoveListener() {
            @Override
            public void removed(final ServerSession session,
                    final boolean timeout) {

                if (!isWebAppClient) {
                    ClientAppUserAuthManager
                            .removeUserAuthToken(clientIpAddress);
                }

                if (!timeout) {
                    return;
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Listener removed (timeout) " + "for user ["
                            + user + "] from [" + clientIpAddress + "]");
                }

                // See Mantis #515
                if (!isWebAppClient) {
                    return;
                }

                try {
                    UserMsgIndicator.write(user, new Date(),
                            UserMsgIndicator.Msg.STOP_POLL_REQ,
                            clientIpAddress);

                } catch (IOException e) {
                    if (!ConfigManager.isShutdownInProgress()) {
                        LOGGER.error("Listener removed (timeout) for user ["
                                + user + "] from [" + clientIpAddress
                                + "]. UserMsgIndicator write failed: "
                                + e.getMessage());
                    }
                }
            }
        });

        Date msgPrevMonitorTime = null;

        if (msgPrevTime != null) {
            msgPrevMonitorTime = new Date(msgPrevTime);
        }

        final Date dateStart = new Date();

        if (LOGGER.isTraceEnabled()) {

            LOGGER.trace("START job monitoring for user [" + user + "] at ["
                    + clientIpAddress + "] pageOffset [" + pageOffset
                    + "] uniqueUrlValue [" + uniqueUrlValue + "] base64 ["
                    + base64 + "] locale [" + locale + "] msgPrevTime ["
                    + msgPrevTime + "]");
        }

        if (msgPrevMonitorTime != null) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("msgPrevMonitorTime ["
                        + msgPrevMonitorTime.getTime() + "]");
            }
        }

        /*
         * Initial probe to see if new jobs arrived.
         */
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("initial check for user [" + user + "]");
        }

        Map<String, Object> eventData = null;

        try {
            /*
             * Note: this picks up jobs that are new/deleted after the last call
             * to this method and before the WatchService is established.
             *
             * Also, when user deletes a 'page' from a multi-page job and the
             * job was not deleted in the prune, this change will be notified
             * here (with a max delay of theMaxMonitorMsec).
             */
            if (isWebAppClient) {
                eventData = getChangedJobsEvent(user, pageOffset,
                        uniqueUrlValue, base64, isWebAppClient, locale,
                        dateStart.getTime());

            } else if (msgPrevMonitorTime != null) {

                final Long lastPrintInTime =
                        INBOX_SERVICE.getLastPrintInTime(user);

                if (lastPrintInTime != null && lastPrintInTime
                        .longValue() > msgPrevMonitorTime.getTime()) {
                    eventData = createPrintInEvent(lastPrintInTime.longValue());
                }
            }

            /*
             * Any MESSAGES to be notified since the previous check date?
             */
            if (eventData == null && msgPrevMonitorTime != null) {
                eventData = checkUserMsgIndicator(clientIpAddress,
                        msgPrevMonitorTime, user, locale);
            }

            /*
             *
             */
            if (eventData == null) {

                eventData = watchUserFileEvents(clientIpAddress, dateStart,
                        user, locale, pageOffset, uniqueUrlValue, base64,
                        isWebAppClient);
            }

            if (eventData == null) {
                eventData = checkPrintInJobExpiry(user, dateStart, locale);
            }

            if (eventData == null) {
                eventData = createNullMsg(user, isWebAppClient, locale,
                        dateStart.getTime());
            }

            if (ADMIN_PUB_USER_EVENT) {

                final UserEventEnum userEvent = UserEventEnum
                        .valueOf(eventData.get(KEY_EVENT).toString());

                if (userEvent != UserEventEnum.NULL) {
                    publishAdminEvent(user, clientIpAddress, isWebAppClient,
                            userEvent);
                }
            }

        } catch (Exception e) {

            eventData = new HashMap<String, Object>();

            if (ConfigManager.isShutdownInProgress()) {

                eventData.put(KEY_EVENT, UserEventEnum.SERVER_SHUTDOWN);

            } else {

                eventData.put(KEY_EVENT, UserEventEnum.ERROR);
                eventData.put(KEY_ERROR, e.getMessage());

                if (e instanceof UserNotFoundException) {
                    LOGGER.warn(e.getMessage());
                } else {
                    LOGGER.error(e.getMessage(), e);
                }

                publishAdminException(user, clientIpAddress, e, isWebAppClient);
            }

        }

        /*
         *
         */
        try {
            String jsonEvent = new ObjectMapper().writeValueAsString(eventData);

            /*
             * The JavaScript client subscribes to CHANNEL_PUBLISH like this:
             * $.cometd.subscribe('/user/event', function(message) {
             */
            remote.deliver(getServerSession(), CHANNEL_PUBLISH, jsonEvent);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Delivered event [" + jsonEvent + "] for user ["
                        + user + "]");
            }

        } catch (Exception e) {

            if (!ConfigManager.isShutdownInProgress()) {
                LOGGER.error(e.getMessage());
            }

            throw new SpException(e);
        } finally {
            if (isWebAppClient) {
                decrementUserWebAppCount();
            } else {
                decrementClientAppCount();
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    String.format("STOP job monitoring for user [%s] at [%s]",
                            user, clientIpAddress));
        }
    }

    /**
     * Cast event.
     *
     * @param <T>
     *            Event class.
     * @param event
     *            The Event.
     * @return The casted event.
     */
    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(final WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Waits for a any change in jobs (created, deleted) or user message.
     *
     * @param clientIpAddress
     * @param dateStart
     * @param user
     *            The user (identified with unique user name) to find jobs for.
     * @param locale
     * @param pageOffset
     *            The page offset as trigger for the event. Is {@code null} for
     *            Java Web Client.
     * @param uniqueUrlValue
     *            Value to make the output page URL's unique, so the browser
     *            will not use its cache, but will retrieve the image from the
     *            server again.
     * @param base64
     *            {@code true}: create image URL for inline BASE64 embedding.
     * @param isWebAppClient
     *            {@code true} is client is User Web App, {@code false} if Java
     *            Client.
     * @return <code>null</code>when the max wait time has elapsed, or a object
     *         map with information about the change.
     * @throws IOException
     * @throws UserNotFoundException
     * @throws ShutdownException
     */
    private Map<String, Object> watchUserFileEvents(
            final String clientIpAddress, final Date dateStart,
            final String user, final Locale locale, final Long pageOffset,
            final String uniqueUrlValue, final boolean base64,
            final boolean isWebAppClient)
            throws IOException, UserNotFoundException, ShutdownException {

        final WatchService watchService =
                FileSystems.getDefault().newWatchService();

        final String userHomeDir = ConfigManager.getUserHomeDir(user);

        final Path path2WatchJob = Paths.get(userHomeDir);

        final WatchKey watchKeyJob = path2WatchJob.register(watchService,
                ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        final Map<WatchKey, Path> watchKeys = new HashMap<WatchKey, Path>();

        watchKeys.put(watchKeyJob, path2WatchJob);

        Map<String, Object> returnData = null;

        try {

            final long msecStart = dateStart.getTime();
            int i = 0;

            while (true) {

                if (ConfigManager.isShutdownInProgress()) {
                    throw new ShutdownException();
                }

                i++;

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("watch [" + i + "] for user [" + user + "] ["
                            + clientIpAddress + "]");
                }
                /*
                 * WAIT for key to be signaled ...
                 */
                WatchKey key = watchService.poll(MSECS_WAIT_BETWEEN_POLLS,
                        TimeUnit.MILLISECONDS);

                boolean bUserHomeInaccessible = false;

                boolean bJobsCreated = false;
                boolean bJobsDeleted = false;

                boolean bMsgDeleted = false;
                boolean bMsgCreated = false;

                long fileLastModifiedRecent = 0;

                if (key != null) {

                    Path dir = watchKeys.get(key);
                    if (dir == null) {
                        throw new SpException("WatchKey not recognized!!");
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        @SuppressWarnings("rawtypes")
                        final WatchEvent.Kind kind = event.kind();

                        /*
                         * A special event to indicate that events may have been
                         * lost or discarded.
                         */
                        if (kind == OVERFLOW) {
                            if (LOGGER.isWarnEnabled()) {
                                LOGGER.warn(String.format(
                                        "%s : events may have been lost "
                                                + "or discarded",
                                        event.kind().name()));
                            }
                            continue;
                        }
                        /*
                         * Context for directory entry event is the file name of
                         * entry.
                         */
                        final WatchEvent<Path> ev = cast(event);
                        final Path name = ev.context();
                        final Path child = dir.resolve(name);
                        final File file = child.toFile();

                        final long fileLastModifiedWlk = file.lastModified();

                        if (fileLastModifiedRecent < fileLastModifiedWlk) {
                            fileLastModifiedRecent = fileLastModifiedWlk;
                        }

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(
                                    String.format("EVENT [%s] for file [%s]",
                                            event.kind().name(),
                                            file.getAbsolutePath()));
                        }

                        final boolean isJobEvent =
                                INBOX_SERVICE.isSupportedJobType(file);

                        boolean isMsgEvent = false;

                        if (!isJobEvent) {
                            isMsgEvent = UserMsgIndicator
                                    .isMsgIndicatorFile(user, file);
                        }

                        /*
                         * Event
                         */
                        if (isMsgEvent) {

                            if (kind == ENTRY_CREATE) {
                                bMsgCreated = true;
                            } else if (kind == ENTRY_MODIFY) {
                                bMsgCreated = true;
                            } else if (kind == ENTRY_DELETE) {
                                bMsgDeleted = true;
                            }

                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(String.format("MSG: %s - [%s]",
                                        event.kind().name(), child));
                            }

                        } else if (isJobEvent) {

                            if (kind == ENTRY_CREATE) {
                                bJobsCreated = true;
                            } else if (kind == ENTRY_DELETE) {
                                bJobsDeleted = true;
                            }
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(String.format("JOB: %s - [%s]",
                                        event.kind().name(), child));
                            }
                        } else {

                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(String.format("Ignored: %s - [%s]",
                                        event.kind().name(), child));
                            }
                        }

                    } // end-for (events)

                    /*
                     * Reset key for next iteration.
                     */
                    if (!key.reset()) {
                        /*
                         * Reset failed, because watch key is no longer valid!
                         */

                        // Remove watch key from from the set.
                        watchKeys.remove(key);

                        // Just one location to watch, so check if set is empty.
                        bUserHomeInaccessible = watchKeys.isEmpty();
                    }
                } // end-if

                /*
                 * Find out about changes.
                 */
                if (bUserHomeInaccessible) {
                    /*
                     * STOP if user home directory is inaccessible.
                     */
                    LOGGER.warn(String.format("[%s] is inaccessible.",
                            userHomeDir));
                    returnData = createErrorMsg(
                            this.localize(locale, "user-inbox-inaccessible"));
                    break;

                } else if (bMsgCreated) {

                    final UserMsgIndicator msgIndicator =
                            UserMsgIndicator.read(user);

                    final Date messageDate = msgIndicator.getMessageDate();

                    final UserMsgIndicator.Msg msg = msgIndicator.getMessage();

                    if (msg != null) {

                        switch (msg) {

                        case ACCOUNT_INFO:
                            returnData = createAccountMsg(user, locale);
                            break;

                        case JOBTICKET_CHANGED:
                        case JOBTICKET_DENIED:
                        case JOBTICKET_SETTLED_COPY:
                        case JOBTICKET_SETTLED_PRINT:
                            returnData = createJobTicketMsg(user, msg, locale);
                            break;

                        case PRINT_IN_DENIED:
                            // no break intended
                        case PRINT_OUT_COMPLETED:
                            returnData = createPrintMsg(user, locale,
                                    messageDate, messageDate);
                            break;

                        case PRINT_IN_EXPIRED:
                            returnData = createPrintInExpiredMsg(user, locale);
                            break;

                        case PRINT_OUT_HOLD:
                            returnData = createNullMsg(user, isWebAppClient,
                                    locale, fileLastModifiedRecent);
                            break;

                        case STOP_POLL_REQ:
                            /*
                             * This messages is meant for WebApp clients only.
                             * See Mantis #515.
                             */
                            if (!isWebAppClient) {
                                break;
                            }

                            final String senderId = msgIndicator.getSenderId();

                            if (clientIpAddress == null || (senderId != null
                                    && senderId.equals(clientIpAddress))) {

                                returnData = createNullMsg(user, isWebAppClient,
                                        locale, fileLastModifiedRecent);
                                break;
                            }

                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("Ignored message ["
                                        + msgIndicator.getMessage() + "] from ["
                                        + msgIndicator.getSenderId()
                                        + "] since we are [" + clientIpAddress
                                        + "]");
                            }
                            break;

                        default:
                            break;
                        }
                    }

                } else if (bMsgDeleted) {
                    // No code intended

                } else if (bJobsDeleted && !isWebAppClient) {
                    /*
                     * Jobs get deleted at a Fast or Hold Print action. During
                     * Fast/Hold print a User is locked. So at this point a User
                     * will be locked. If we would handle a jobs deleted event
                     * the User will be locked again: we see that this can lead
                     * to a lock exception (A lock could not be obtained within
                     * the time requested).
                     *
                     * Therefore, do NOT notify this event when this is Client
                     * Java App.
                     */

                    // No code intended

                } else if (bJobsCreated || bJobsDeleted) {

                    if (isWebAppClient) {
                        returnData = getChangedJobsEvent(user, pageOffset,
                                uniqueUrlValue, base64, isWebAppClient, locale,
                                fileLastModifiedRecent);
                    } else if (bJobsCreated) {
                        returnData = createPrintInEvent(fileLastModifiedRecent);
                    }
                }

                /*
                 * If changes found, STOP to notify immediately.
                 */
                if (returnData != null) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("File Watch: changes found.");
                    }
                    break;
                }

                final long msecNow = System.currentTimeMillis();
                final long timeElapsed =
                        msecNow + MSECS_WAIT_BETWEEN_POLLS - msecStart;

                /*
                 * STOP if the max monitor time has elapsed.
                 */
                if (timeElapsed >= theMaxMonitorMsec) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("File Watch: time elapsed.");
                    }
                    break;
                }

            } // end-for

        } catch (InterruptedException e) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("File Watch: InterruptedException ["
                        + e.getMessage() + "]");
            }
            throw new ShutdownException();

        } finally {
            // Closing is CRUCIAL to prevent the "Too Many Open Files" Error
            watchService.close();
        }

        return returnData;
    }

    /**
     * Creates a simple {@link UserEventEnum#PRINT_IN} event without any job or
     * page information.
     *
     * @return The event data.
     */
    private static Map<String, Object> createPrintInEvent(final long msgTime) {
        final Map<String, Object> userData = new HashMap<String, Object>();
        userData.put(KEY_EVENT, UserEventEnum.PRINT_IN);
        userData.put(KEY_MSG_TIME, Long.valueOf(msgTime));
        return userData;
    }

    /**
     * Creates a simple {@link UserEventEnum#SYS_MAINTENANCE} event.
     *
     * @return The event data.
     */
    private static Map<String, Object> createSysMaintenanceEvent() {
        final Map<String, Object> userData = new HashMap<String, Object>();
        userData.put(KEY_EVENT, UserEventEnum.SYS_MAINTENANCE);
        return userData;
    }

    /**
     * Gets the event for new or deleted jobs since pageOffset.
     *
     * @param userName
     *            The user (identified with unique user name) to find jobs for.
     * @param nPageOffset
     *            The page offset as trigger for the event.
     * @param uniqueUrlValue
     *            Value to make the output page URL's unique, so the browser
     *            will not use its cache, but will retrieve the image from the
     *            server again.
     * @param base64
     *            {@code true}: create image URL for inline BASE64 embedding.
     * @param isWebAppClient
     *            {@code true} is client is User Web App, {@code false} if Java
     *            Client.
     * @param locale
     *            The user {@link Locale}.
     * @param msgTime
     *            The time of the message.
     * @return The event data, or null when no new jobs are present (or old jobs
     *         deleted).
     * @throws UserNotFoundException
     *             When userName is not found in database.
     * @throws IOException
     *             When the user's SafePages directory could not be (lazy)
     *             created.
     */
    private static Map<String, Object> getChangedJobsEvent(
            final String userName, final Long nPageOffset,
            final String uniqueUrlValue, final boolean base64,
            final boolean isWebAppClient, final Locale locale,
            final long msgTime) throws UserNotFoundException, IOException {

        final Date perfStartTime = PerformanceLogger.startTime();

        Map<String, Object> userData = null;

        User lockedUser = null;

        ServiceContext.open();
        ServiceContext.setLocale(locale);

        final DaoContext daoContext = ServiceContext.getDaoContext();

        daoContext.beginTransaction();

        try {
            /*
             * #575: Be less strict on internal Apache Derby database by NOT
             * locking the user, since multiple row locks of same user leads to
             * random deadlocks in Fast Print Release scenario's.
             *
             * #575: PostgreSQL handles same user concurrent row locking just
             * fine (?)
             */
            if (ConfigManager.isDbInternal()
                    || !ConfigManager.isUserWebAppDatabaseUserRowLocking()) {
                lockedUser = daoContext.getUserDao()
                        .findActiveUserByUserId(userName);
            } else {
                lockedUser = USER_SERVICE.lockByUserId(userName);
            }

            if (lockedUser == null) {
                throw new UserNotFoundException(
                        "user [" + userName + "] not found.");
            }

            if (isWebAppClient && !lockedUser.getAdmin().booleanValue()
                    && ConfigManager.isSysMaintenance()) {

                userData = createSysMaintenanceEvent();

            } else {

                USER_SERVICE.lazyUserHomeDir(userName);

                final PageImages pages = INBOX_SERVICE.getPageChunks(userName,
                        null, uniqueUrlValue, base64);

                long totPages = 0;

                for (final PageImage image : pages.getPages()) {
                    totPages += image.getPages();
                }

                /*
                 * NOTE: we compare with NOT EQUAL, so any change (new jobs, or
                 * old jobs deleted) is identified.
                 */
                if (totPages != nPageOffset.longValue()) {

                    userData = createPrintInEvent(msgTime);
                    userData.put(KEY_JOBS, pages.getJobs());
                    userData.put(KEY_PAGES, pages.getPages());
                    userData.put(KEY_URL_TEMPLATE, ImageUrl
                            .composeDetailPageTemplate(userName, base64));

                    if (isWebAppClient) {
                        ApiRequestHelper.addUserStats(userData, lockedUser,
                                ServiceContext.getLocale(),
                                ServiceContext.getAppCurrencySymbol());
                    }
                }
            }

        } finally {
            try {
                daoContext.rollback();
            } catch (SpException e) {
                LOGGER.error(e.getMessage());
            }
            ServiceContext.close();
        }

        PerformanceLogger.log(UserEventService.class, "getChangedJobsEvent",
                perfStartTime, userName);

        return userData;
    }

    /**
     * Creates the user {@link PrintIn} or {@link PrintOut} message since
     * lastTime, or null when no new messages are present.
     *
     * @param userName
     *            The user (identified with unique user name) to find jobs for.
     * @param locale
     *            The user's locale like 'en', 'nl', 'en-EN', 'nl-NL'.
     * @param msgPrevMonitorTime
     *            The last time a user message was polled as trigger for any new
     *            user messages.
     * @param messageDate
     * @return The new user messages, or null when no new messages are present
     *         since lastTime.
     */
    private static Map<String, Object> createPrintMsg(final String userName,
            final Locale locale, final Date msgPrevMonitorTime,
            final Date messageDate) throws IOException {

        Map<String, Object> userData = null;

        if (msgPrevMonitorTime.compareTo(messageDate) <= 0) {

            ServiceContext.open();
            ServiceContext.setLocale(locale);
            ServiceContext.getDaoContext();

            try {
                JsonUserMsgNotification json =
                        UserMsgIndicator.getPrintMsgNotification(
                                DaoContextImpl.peekEntityManager(), userName,
                                locale, msgPrevMonitorTime, messageDate);

                userData = new HashMap<String, Object>();

                userData.put(KEY_EVENT, UserEventEnum.PRINT_MSG);
                userData.put(KEY_DATA, json);
                userData.put(KEY_MSG_TIME, json.getMsgTime());

                addUserStats(userData, userName, locale);

            } finally {
                ServiceContext.close();
            }

        }

        return userData;
    }

    /**
     * Adds user statistics to event data.
     *
     * @param eventData
     *            The event data.
     * @param userId
     *            The user id.
     * @param locale
     *            The user {@link Locale}.
     * @return The eventData input parameter.
     */
    private static Map<String, Object> addUserStats(
            final Map<String, Object> eventData, final String userId,
            final Locale locale) {

        ServiceContext.open();
        ServiceContext.setLocale(locale);

        try {
            final User user = ServiceContext.getDaoContext().getUserDao()
                    .findActiveUserByUserId(userId);

            ApiRequestHelper.addUserStats(eventData, user,
                    ServiceContext.getLocale(),
                    ServiceContext.getAppCurrencySymbol());

        } finally {
            ServiceContext.close();
        }

        return eventData;
    }

    /**
     *
     * @return
     */
    private Map<String, Object> createAccountMsg(final String userId,
            final Locale locale) {

        final Map<String, Object> eventData = new HashMap<String, Object>();

        final JsonUserMsg msg = new JsonUserMsg();
        msg.setLevel(JsonUserMsg.LEVEL_INFO);
        msg.setText(this.localize(locale, "account-changed"));

        final JsonUserMsgNotification json = new JsonUserMsgNotification();
        json.addUserMsg(msg);
        json.setMsgTime(System.currentTimeMillis());

        eventData.put(KEY_DATA, json);
        eventData.put(KEY_EVENT, UserEventEnum.ACCOUNT);
        eventData.put(KEY_MSG_TIME, json.getMsgTime());

        return addUserStats(eventData, userId, locale);
    }

    /**
     * Creates a Job Ticket User message.
     *
     * @return
     */
    private Map<String, Object> createJobTicketMsg(final String userId,
            UserMsgIndicator.Msg msgInd, final Locale locale) {

        final String text;

        switch (msgInd) {
        case JOBTICKET_CHANGED:
            text = this.localize(locale, "jobticket-changed");
            break;
        case JOBTICKET_DENIED:
            text = this.localize(locale, "jobticket-denied");
            break;
        case JOBTICKET_SETTLED_COPY:
            text = this.localize(locale, "jobticket-copy-completed");
            break;
        case JOBTICKET_SETTLED_PRINT:
            text = this.localize(locale, "jobticket-print-completed");
            break;
        default:
            text = "???";
            break;
        }

        final JsonUserMsg msg = new JsonUserMsg();
        msg.setLevel(JsonUserMsg.LEVEL_INFO);
        msg.setText(text);

        final JsonUserMsgNotification json = new JsonUserMsgNotification();
        json.addUserMsg(msg);
        json.setMsgTime(System.currentTimeMillis());

        final Map<String, Object> eventData = new HashMap<String, Object>();

        eventData.put(KEY_DATA, json);
        eventData.put(KEY_EVENT, UserEventEnum.JOBTICKET);
        eventData.put(KEY_MSG_TIME, json.getMsgTime());

        return addUserStats(eventData, userId, locale);
    }

    /**
     *
     * @return
     */
    private Map<String, Object> createPrintInExpiredMsg(final String userId,
            final Locale locale) {

        final Map<String, Object> eventData = new HashMap<String, Object>();

        final JsonUserMsg msg = new JsonUserMsg();
        msg.setLevel(JsonUserMsg.LEVEL_WARN);
        msg.setText(this.localize(locale, "print-in-expired"));

        final JsonUserMsgNotification json = new JsonUserMsgNotification();
        json.addUserMsg(msg);
        json.setMsgTime(System.currentTimeMillis());

        eventData.put(KEY_DATA, json);
        eventData.put(KEY_EVENT, UserEventEnum.PRINT_IN_EXPIRED);
        eventData.put(KEY_MSG_TIME, json.getMsgTime());

        return eventData;
    }

    /**
     *
     * @param msg
     *            The error message.
     * @return
     */
    private static Map<String, Object> createErrorMsg(final String msg) {

        final Map<String, Object> eventData = new HashMap<String, Object>();
        final JsonUserMsgNotification json = new JsonUserMsgNotification();

        json.setMsgTime(new Date().getTime());

        eventData.put(KEY_EVENT, UserEventEnum.ERROR);
        eventData.put(KEY_ERROR, msg);
        eventData.put(KEY_DATA, json);
        eventData.put(KEY_MSG_TIME, json.getMsgTime());

        return eventData;
    }

    /**
     *
     * @param userId
     *            The user id.
     * @param isWebAppClient
     *            {@code true} is client is User Web App, {@code false} if Java
     *            Client.
     * @param locale
     * @param msgTime
     * @return
     */
    private static Map<String, Object> createNullMsg(final String userId,
            final boolean isWebAppClient, final Locale locale,
            final long msgTime) {

        final Map<String, Object> eventData = new HashMap<String, Object>();
        final JsonUserMsgNotification json = new JsonUserMsgNotification();

        json.setMsgTime(msgTime);

        eventData.put(KEY_EVENT, UserEventEnum.NULL);
        eventData.put(KEY_DATA, json);
        eventData.put(KEY_MSG_TIME, Long.valueOf(msgTime));

        if (isWebAppClient) {
            addUserStats(eventData, userId, locale);
        }

        return eventData;
    }

    /**
     *
     * @param userId
     *            The user id.
     * @param dateStart
     * @param locale
     * @return
     * @throws IOException
     */
    private Map<String, Object> checkPrintInJobExpiry(final String userId,
            final Date dateStart, final Locale locale) throws IOException {

        /*
         * Check for expired inbox jobs.
         */
        final long msecJobExpiry = ConfigManager.instance()
                .getConfigInt(Key.PRINT_IN_JOB_EXPIRY_MINS, 0)
                * DateUtil.DURATION_MSEC_MINUTE;

        if (msecJobExpiry <= 0) {
            return null;
        }

        final int nDeleted = INBOX_SERVICE.deleteJobs(userId,
                dateStart.getTime(), msecJobExpiry);

        if (nDeleted == 0) {
            return null;
        }

        UserMsgIndicator.write(userId, dateStart,
                UserMsgIndicator.Msg.PRINT_IN_EXPIRED, null);

        return createPrintInExpiredMsg(userId, locale);
    }

}
