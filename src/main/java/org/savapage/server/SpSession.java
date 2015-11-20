/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.jpa.User;
import org.savapage.server.webapp.WebAppTypeEnum;

/**
 * Customized {@link WebSession}.
 * <p>
 * Note that methods are synchronized, because sessions aren’t thread-safe.
 * </p>
 *
 * @author Datraverse B.V.
 */
public class SpSession extends WebSession {

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
     * @param request
     */
    public SpSession(Request request) {
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
     * @return
     */
    public static SpSession get() {
        return (SpSession) Session.get();
    }

    public void incrementAuthWebApp() {
        this.authWebAppCount++;
    }

    public void decrementAuthWebApp() {
        if (this.authWebAppCount > 0) {
            this.authWebAppCount--;
        }
    }

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
        DecimalFormat format =
                (DecimalFormat) NumberFormat.getNumberInstance(get()
                        .getLocale());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return String.valueOf(symbols.getDecimalSeparator());
    }

    /**
     *
     * @return
     */
    public synchronized User getUser() {
        return user;
    }

    /**
     *
     * @return
     */
    public synchronized boolean isAuthenticated() {
        return user != null;
    }

    /**
     *
     * @return
     */
    public synchronized boolean isAdmin() {
        return user != null && user.getAdmin();
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
        decrementAuthWebApp();
        this.creationTime = System.currentTimeMillis();
        this.lastValidateTime = this.creationTime;
    }

    /**
     * Sets the authenticated user for this session.
     *
     * Note that this method calls dirty so that any clustering is properly
     * performed.
     *
     * @param authUser
     *            The authenticated user. A {@code null} user makes the session
     *            unauthenticated. See {@link #isAuthenticated()}
     */

    public final synchronized void setUser(final User authUser) {
        this.user = authUser;
        dirty();
    }

    /**
     * Sets the {@link WebAppTypeEnum} of this session.
     *
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     */
    public final synchronized void
            setWebAppType(final WebAppTypeEnum webAppType) {
        this.setAttribute(SESSION_ATTR_WEBAPP_TYPE, webAppType.toString());
    }

    /**
     * Gets the {@link WebAppTypeEnum} from this session.
     *
     * @return The {@link WebAppTypeEnum}.
     */
    public final synchronized WebAppTypeEnum getWebAppType() {
        WebAppTypeEnum webAppType = WebAppTypeEnum.UNDEFINED;
        final String value =
                (String) this.getAttribute(SESSION_ATTR_WEBAPP_TYPE);
        if (value != null) {
            webAppType = WebAppTypeEnum.valueOf(value);
        }
        return webAppType;
    }

}
