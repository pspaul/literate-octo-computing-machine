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
package org.savapage.server.session;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.crypto.OneTimeAuthToken;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.User;

/**
 * Customized {@link WebSession}.
 *
 * <p>
 * <i>A session instance is shared by all Web Apps across multiple tabs in a
 * single browser</i>.
 * </p>
 * <p>
 * NOTE: synchronized statements are used because sessions aren’t thread-safe.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class SpSession extends WebSession {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private static final String SESSION_ATTR_WEBAPP_TYPE = "sp-webapp-type";

    /** */
    private final Object mutex = new Object();

    /** */
    private UserIdDto userIdDto;

    /** */
    private UserIdDto userIdDtoDocLog;

    /**
     * User info for TOTP request.
     */
    private UserIdDto totpRequest;

    /**
     * {@code true} when authenticated by {@link OneTimeAuthToken}.
     */
    private boolean oneTimeAuthToken = false;

    /**
     *
     */
    private int authWebAppCount = 0;

    /**
     *
     */
    private long creationTime;

    /**
    *
    */
    private long lastValidateTime;

    /**
     *
     */
    private JobTicketSession jobTicketSession;

    /**
     * {@code true} when a session attribute changed by human interaction in
     * WebApp.
     */
    private boolean humanDetected = false;

    /**
     *
     * @param request
     *            The {@link Request}.
     */
    public SpSession(final Request request) {
        super(request);
        this.creationTime = System.currentTimeMillis();
        this.lastValidateTime = creationTime;
    }

    /**
     * Gets the current session.
     *
     * This method uses Java’s covariance feature so users can get the current
     * session instance without casting (you can do SpSession s =
     * SpSession.get() instead of SpSession s = (SpSession)SpSession.get())
     *
     * @return The session.
     */
    public static SpSession get() {
        return (SpSession) Session.get();
    }

    /**
     * Increments the authenticated Web App counter.
     */
    public void incrementAuthWebAppCount() {
        synchronized (this.mutex) {
            this.authWebAppCount++;
        }
    }

    /**
     * Decrements the authenticated Web App counter.
     */
    public void decrementAuthWebAppCount() {
        synchronized (this.mutex) {
            if (this.authWebAppCount > 0) {
                this.authWebAppCount--;
            }
        }
    }

    /**
     * @return The authenticated Web App counter.
     */
    public int getAuthWebAppCount() {
        synchronized (this.mutex) {
            return this.authWebAppCount;
        }
    }

    /**
     * Gets the Application's currency symbol according to the session
     * {@link Locale} .
     *
     * @return The currency symbol.
     */
    public static String getAppCurrencySymbol() {
        return ConfigManager.getAppCurrencySymbol(get().getLocale());
    }

    /**
     * Gets the decimal separator from the {@link Locale} setting of the current
     * session.
     *
     * @return The decimal separator.
     */
    public static String getDecimalSeparator() {
        final DecimalFormat format = (DecimalFormat) NumberFormat
                .getNumberInstance(get().getLocale());
        final DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return String.valueOf(symbols.getDecimalSeparator());
    }

    /**
     * @param dto
     *            User info for TOTP request.
     */
    public void setTOTPRequest(final UserIdDto dto) {
        synchronized (this.mutex) {
            this.totpRequest = dto;
        }
    }

    /**
     * @return User info for TOTP request.
     */
    public UserIdDto getTOTPRequest() {
        synchronized (this.mutex) {
            return this.totpRequest;
        }
    }

    /**
     *
     * @return The user.
     */
    public UserIdDto getUserIdDto() {
        synchronized (this.mutex) {
            return this.userIdDto;
        }
    }

    /**
     * @param dto
     *            The {@link DocLog} user.
     */
    public void setUserIdDtoDocLog(final UserIdDto dto) {
        synchronized (this.mutex) {
            this.userIdDtoDocLog = dto;
        }
    }

    /**
     * @return The {@link DocLog} user.
     */
    public UserIdDto getUserIdDtoDocLog() {
        synchronized (this.mutex) {
            if (this.userIdDtoDocLog == null) {
                return this.userIdDto;
            }
            return this.userIdDtoDocLog;
        }
    }

    /**
     * @return The authenticated unique User ID, or {@code null} if no
     *         authenticated {@link User}.
     */
    public String getUserId() {
        synchronized (this.mutex) {
            if (this.userIdDto == null) {
                return null;
            }
            return this.userIdDto.getUserId();
        }
    }

    /**
     * @return The unique User ID for the {@link DocLog}, or {@code null} if no
     *         authenticated {@link User}.
     */
    public String getUserIdDocLog() {
        synchronized (this.mutex) {
            final String userId;
            if (this.userIdDto == null) {
                userId = null;
            } else if (this.userIdDtoDocLog == null) {
                userId = this.userIdDto.getUserId();
            } else {
                userId = this.userIdDtoDocLog.getUserId();
            }
            return userId;
        }
    }

    /**
     * @return The primary database key of authenticated User, or {@code null}
     *         if no authenticated {@link User}.
     */
    public Long getUserDbKey() {
        synchronized (this.mutex) {
            if (this.userIdDto == null) {
                return null;
            }
            return this.userIdDto.getDbKey();
        }
    }

    /**
     * @return The primary database key of the User for the {@link DocLog}, or
     *         {@code null} if no authenticated {@link User}.
     */
    public Long getUserDbKeyDocLog() {
        synchronized (this.mutex) {
            final Long dbKey;
            if (this.userIdDto == null) {
                dbKey = null;
            } else if (this.userIdDtoDocLog == null) {
                dbKey = this.userIdDto.getDbKey();
            } else {
                dbKey = this.userIdDtoDocLog.getDbKey();
            }
            return dbKey;
        }
    }

    /**
     *
     * @return {@code true} if authenticated.
     */
    public boolean isAuthenticated() {
        synchronized (this.mutex) {
            return this.userIdDto != null;
        }
    }

    /**
     *
     * @return {@code true} if user is an administrator.
     */
    public boolean isAdmin() {
        synchronized (this.mutex) {
            return this.userIdDto != null && this.userIdDto.isAdmin();
        }
    }

    /**
     *
     * @return {@code true} if {@link User} was authenticated by
     *         {@link OneTimeAuthToken}.
     */
    public boolean isOneTimeAuthToken() {
        synchronized (this.mutex) {
            return this.userIdDto != null && this.oneTimeAuthToken;
        }
    }

    /**
     * Checks if session is expired, and touches the validated time.
     *
     * @param timeToLive
     *            Max duration (msec) of session before it expires.
     * @return {@code true} when session expired.
     */
    public boolean checkTouchExpired(final long timeToLive) {
        synchronized (this.mutex) {
            final long currentTime = System.currentTimeMillis();
            final boolean expired =
                    currentTime - this.lastValidateTime > timeToLive;
            this.lastValidateTime = currentTime;
            return expired;
        }
    }

    /**
     * This sets the session {@link User} object to {@code null} and decrements
     * the authenticated webapp counter.
     */
    public void logout() {
        synchronized (this.mutex) {
            setUser(null);
            setUserIdDtoDocLog(null);
            setWebAppType(WebAppTypeEnum.UNDEFINED);
            setHumanDetected(false);
            oneTimeAuthToken = false;

            decrementAuthWebAppCount();

            this.creationTime = System.currentTimeMillis();
            this.lastValidateTime = this.creationTime;
        }
    }

    /**
     * Sets the authenticated user for this session.
     *
     * @param authUser
     *            The authenticated user. A {@code null} user makes the session
     *            unauthenticated. See {@link #isAuthenticated()}
     */
    public void setUser(final User authUser) {
        this.setUser(authUser, false);
    }

    /**
     * Sets the authenticated user for this session.
     * <p>
     * Note that this method calls dirty so that any clustering is properly
     * performed.
     * </p>
     *
     * @param authUser
     *            The authenticated user. A {@code null} user makes the session
     *            unauthenticated. See {@link #isAuthenticated()}
     * @param authToken
     *            {@code true} when authenticated by {@link OneTimeAuthToken}.
     */
    public void setUser(final User authUser, final boolean authToken) {
        synchronized (this.mutex) {
            if (authUser == null) {
                this.userIdDto = null;
            } else {
                this.userIdDto = UserIdDto.create(authUser);
            }
            this.oneTimeAuthToken = authToken;
            dirty();
        }
    }

    /**
     * Sets the {@link WebAppTypeEnum} of this session.
     *
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     */
    public void setWebAppType(final WebAppTypeEnum webAppType) {
        synchronized (this.mutex) {
            this.setAttribute(SESSION_ATTR_WEBAPP_TYPE, webAppType.toString());
        }
    }

    /**
     * Gets the {@link WebAppTypeEnum} from this session.
     *
     * @return The {@link WebAppTypeEnum}.
     */
    public WebAppTypeEnum getWebAppType() {
        synchronized (this.mutex) {
            WebAppTypeEnum webAppType = WebAppTypeEnum.UNDEFINED;
            final String value =
                    (String) this.getAttribute(SESSION_ATTR_WEBAPP_TYPE);
            if (value != null) {
                webAppType = WebAppTypeEnum.valueOf(value);
            }
            return webAppType;
        }
    }

    public JobTicketSession getJobTicketSession() {
        return this.jobTicketSession;
    }

    public void setJobTicketSession(JobTicketSession jobTicketSession) {
        this.jobTicketSession = jobTicketSession;
    }

    public boolean isHumanDetected() {
        return humanDetected;
    }

    public void setHumanDetected(boolean userInteraction) {
        this.humanDetected = userInteraction;
    }

}
