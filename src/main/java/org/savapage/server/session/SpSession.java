/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
import org.savapage.core.jpa.User;

/**
 * Customized {@link WebSession}.
 *
 * <p>
 * <i>A session instance is shared by all Web Apps across multiple tabs in a
 * single browser</i>.
 * </p>
 * <p>
 * NOTE: methods are synchronized, because sessions aren’t thread-safe.
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

    /**
    *
    */
    private User user;

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
    public void incrementAuthWebApp() {
        this.authWebAppCount++;
    }

    /**
     * Decrements the authenticated Web App counter.
     */
    public void decrementAuthWebApp() {
        if (this.authWebAppCount > 0) {
            this.authWebAppCount--;
        }
    }

    /**
     * @return The authenticated Web App counter.
     */
    public int getAuthWebAppCount() {
        return this.authWebAppCount;
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
        DecimalFormat format = (DecimalFormat) NumberFormat
                .getNumberInstance(get().getLocale());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return String.valueOf(symbols.getDecimalSeparator());
    }

    /**
     *
     * @return The user.
     */
    public synchronized User getUser() {
        return user;
    }

    /**
     *
     * @return {@code true} if authenticated.
     */
    public synchronized boolean isAuthenticated() {
        return user != null;
    }

    /**
     *
     * @return {@code true} if user is an administrator.
     */
    public synchronized boolean isAdmin() {
        return user != null && user.getAdmin();
    }

    /**
     *
     * @return {@code true} if {@link User} was authenticated by
     *         {@link OneTimeAuthToken}.
     */
    public synchronized boolean isOneTimeAuthToken() {
        return this.user != null && this.oneTimeAuthToken;
    }

    /**
     * Checks if session is expired, and touches the validated time.
     *
     * @param timeToLive
     *            Max duration (msec) of session before it expires.
     * @return {@code true} when session expired.
     */
    public synchronized boolean checkTouchExpired(final long timeToLive) {

        final long currentTime = System.currentTimeMillis();
        final boolean expired =
                currentTime - this.lastValidateTime > timeToLive;
        this.lastValidateTime = currentTime;
        return expired;
    }

    /**
     * This sets the session {@link User} object to {@code null} and decrements
     * the authenticated webapp counter.
     */
    public synchronized void logout() {

        setUser(null);
        setWebAppType(WebAppTypeEnum.UNDEFINED);

        decrementAuthWebApp();

        this.creationTime = System.currentTimeMillis();
        this.lastValidateTime = this.creationTime;
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
    public synchronized void setUser(final User authUser,
            final boolean authToken) {
        this.user = authUser;
        this.oneTimeAuthToken = authToken;
        dirty();
    }

    /**
     * Sets the {@link WebAppTypeEnum} of this session.
     *
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     */
    public synchronized void setWebAppType(final WebAppTypeEnum webAppType) {
        this.setAttribute(SESSION_ATTR_WEBAPP_TYPE, webAppType.toString());
    }

    /**
     * Gets the {@link WebAppTypeEnum} from this session.
     *
     * @return The {@link WebAppTypeEnum}.
     */
    public synchronized WebAppTypeEnum getWebAppType() {
        WebAppTypeEnum webAppType = WebAppTypeEnum.UNDEFINED;
        final String value =
                (String) this.getAttribute(SESSION_ATTR_WEBAPP_TYPE);
        if (value != null) {
            webAppType = WebAppTypeEnum.valueOf(value);
        }
        return webAppType;
    }

    public JobTicketSession getJobTicketSession() {
        return jobTicketSession;
    }

    public void setJobTicketSession(JobTicketSession jobTicketSession) {
        this.jobTicketSession = jobTicketSession;
    }

}
