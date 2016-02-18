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
package org.savapage.server.cometd;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.server.AbstractService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulation of CometD {@link AbstractService}.
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractEventService extends AbstractService
        implements ServiceEntryPoint {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractEventService.class);

    /**
     *
     * The {@code maxNetworkDelay} Long Polling Client Transport Parameter.
     * <p>
     * "The maximum number of milliseconds to wait before considering a request
     * to the Bayeux server failed."
     * </p>
     */
    private static final long MAX_NETWORKDELAY_MSEC_DEFAULT = 60000;

    /**
     * 5 seconds margin, so we know for sure the long poll will return in time.
     */
    private static final long MSECS_MAX_MONITOR_MARGIN = 5000;

    /**
     * This value should correspond to COMETD_MAX_NETWORKDELAY on the JavaScript
     * CometD client.
     */
    private static long theMaxNetworkDelayMsec = MAX_NETWORKDELAY_MSEC_DEFAULT;

    /**
     * The max time this service is allowed to take. We compensate with some
     * extra time for network transfer.
     */
    protected static long theMaxMonitorMsec =
            theMaxNetworkDelayMsec - MSECS_MAX_MONITOR_MARGIN;

    /**
     *
     * @param bayeux
     *            The bayeux instance
     * @param name
     *            The name of the service
     */
    public AbstractEventService(final BayeuxServer bayeux, final String name) {
        super(bayeux, name);
    }

    /**
     *
     * @return The max time a CometD service is allowed to take in milliseconds.
     */
    public static long getMaxMonitorMsec() {
        return theMaxMonitorMsec;
    }

    /**
     *
     * @param msec
     */
    public static long getMaxNetworkDelayMsecDefault() {
        return MAX_NETWORKDELAY_MSEC_DEFAULT;
    }

    /**
     * Gets the remote IP address from the Bayeux context.
     *
     * @return The IP address.
     */
    protected final String getClientIpAddress() {
        return getBayeux().getContext().getRemoteAddress().getHostString();
    }

    /**
     * Get the {@link Locale} from input map.
     *
     * @param input
     *            The input map.
     * @param keyLanguage
     *            The key of language value.
     * @param keyCountry
     *            The key of country value.
     * @return The {@link Locale}.
     */
    protected Locale getLocale(final Map<String, Object> input,
            final String keyLanguage, final String keyCountry) {

        final String language = (String) input.get(keyLanguage);
        final String country = (String) input.get(keyCountry);

        try {
            if (StringUtils.isBlank(country)) {
                return new Locale(language);
            }
            return new Locale(language, country);
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(String.format(
                        "Locale cannot be created for %s %s (falling back to %s)",
                        language, StringUtils.defaultString(country),
                        Locale.getDefault().toString()));
            }
            return Locale.getDefault();
        }
    }

    /**
     * Return a localized message string. IMPORTANT:
     * {@link ServiceContext#getLocale() is used.
     *
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    protected final String localize(final String key, final String... args) {
        return this.localize(ServiceContext.getLocale(), key, args);
    }

    /**
     * Return a localized message string.
     *
     * @param locale
     *            The {@link Locale}.
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    protected final String localize(final Locale locale, final String key,
            final String... args) {
        return Messages.getMessage(getClass(), locale, key, args);
    }

    /**
     * Sets the value for the CometD {@code maxNetworkDelay}.
     * <p>
     * From the CometD client's perspective: <i>"The max number of milliseconds
     * to wait before considering a request to the Bayeux server failed."</i>
     * </p>
     * <p>
     * From <i>this</i> Bayeux server perspective: this value corresponds to the
     * max time WITHIN this service MUST return the long poll request.
     * </p>
     *
     * @param delay
     *            The delay in milliseconds.
     */
    public static void setMaxNetworkDelay(final long delay) {
        theMaxNetworkDelayMsec = delay;
        theMaxMonitorMsec = theMaxNetworkDelayMsec - MSECS_MAX_MONITOR_MARGIN;
    }

    /**
     * See {@link #setMaxNetworkDelay(long)}.
     *
     * @return The delay in milliseconds.
     */
    public static long getMaxNetworkDelay() {
        return theMaxNetworkDelayMsec;
    }

}
