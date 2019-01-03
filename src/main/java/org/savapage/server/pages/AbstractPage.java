/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.server.pages;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.PerformanceLogger;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Printer;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.util.InetUtils;
import org.savapage.server.WebAppParmEnum;
import org.savapage.server.api.UserAgentHelper;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.helpers.HtmlPrinterImgEnum;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract page for all pages.
 * <p>
 * TODO: All helper methods should be moved to {@link MarkupHelper}.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractPage extends WebPage
        implements ServiceEntryPoint {

    private static final long serialVersionUID = 1L;

    /** */
    protected static final String POST_METHOD = "POST";

    /** */
    protected static final String POST_PARM_WEBAPPTYPE = "webAppType";

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractPage.class);

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private final DateFormat dfLongDate = DateFormat
            .getDateInstance(DateFormat.LONG, getSession().getLocale());

    /** */
    private final DateFormat dfDateTime = DateFormat.getDateTimeInstance(
            DateFormat.LONG, DateFormat.LONG, getSession().getLocale());

    /** */
    private final DateFormat dfShortDateTime = DateFormat.getDateTimeInstance(
            DateFormat.SHORT, DateFormat.SHORT, getSession().getLocale());

    /** */
    private final DateFormat dfShortDate = DateFormat
            .getDateInstance(DateFormat.SHORT, getSession().getLocale());

    /** */
    private final DateFormat dfMediumDate = DateFormat
            .getDateInstance(DateFormat.MEDIUM, getSession().getLocale());

    /** */
    private boolean serviceContextOpened = false;

    /**
     *
     */
    private Date perfStartTime;

    /**
     *
     * @param parameters
     */
    protected AbstractPage() {
        super();
    }

    /**
     *
     * @param parameters
     */
    protected AbstractPage(final PageParameters parameters) {
        super(parameters);
    }

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     * @return The web App type.
     */
    protected final WebAppTypeEnum
            getWebAppTypeEnum(final PageParameters parameters) {
        return EnumUtils.getEnum(WebAppTypeEnum.class,
                parameters.get(WebAppParmEnum.SP_APP.parm()).toString());
    }

    /**
     * Opens the {@link ServiceContext} with the {@link Locale} of the session.
     * <p>
     * When needed, this method MUST be called in the <b>constructor</b> of the
     * actual Page implementation.
     * </p>
     * <p>
     * Note: The {@link #onAfterRender()} method is overloaded to
     * COMMIT/ROLLBACK the {@link DaoContext} and to CLOSE the
     * {@link ServiceContext}.
     * </p>
     * <p>
     * Note: The {@link #onBeforeRender()} method is overloaded to handle any
     * {@link Exception} from super#{@link #onAfterRender()} to ROLLBACK the
     * {@link DaoContext} and to CLOSE the {@link ServiceContext}.
     * </p>
     */
    private void openServiceContext() {
        ServiceContext.open();
        serviceContextOpened = true;
        ServiceContext.setLocale(getSession().getLocale());
    }

    @Override
    protected final void onBeforeRender() {

        perfStartTime = PerformanceLogger.startTime();

        openServiceContext();

        try {

            super.onBeforeRender();

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);

            if (serviceContextOpened) {
                ServiceContext.getDaoContext().rollback();
                ServiceContext.close();
            }

            throw e;
        }
    }

    @Override
    protected final void onAfterRender() {
        try {
            super.onAfterRender();
            if (serviceContextOpened) {
                ServiceContext.getDaoContext().commit();
            }
        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);

            if (serviceContextOpened) {
                ServiceContext.getDaoContext().rollback();
            }

            throw e;

        } finally {
            if (serviceContextOpened) {
                ServiceContext.close();
            }
        }

        if (PerformanceLogger.isEnabled()) {
            PerformanceLogger.log(this.getClass(), "onAfterRender",
                    perfStartTime, getSessionWebAppType().toString());
        }
    }

    /**
     * Gets the authenticated {@link WebAppTypeEnum} from the session.
     *
     * @return The {@link WebAppTypeEnum}.
     */
    protected final WebAppTypeEnum getSessionWebAppType() {

        WebAppTypeEnum webAppType = WebAppTypeEnum.UNDEFINED;

        final SpSession session = SpSession.get();

        if (getSession() != null) {
            webAppType = session.getWebAppType();
        }

        return webAppType;
    }

    /**
     * @return {@code true} when login is restricted to local methods, i.e.
     *         Google Sign-In is inactive.
     */
    protected final boolean isRestrictedToLocalLogin() {
        return this.getParmValue(this.getPageParameters(), true,
                WebAppParmEnum.SP_LOGIN_LOCAL.parm()) != null;
    }

    /**
     * Gets as localized (long) date string of a Date. The locale of the current
     * session is used.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    protected final String localizedDate(final Date date) {
        return dfLongDate.format(date);
    }

    /**
     * Gets as localized (short) date string of a Date. The locale of the
     * current session is used.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    protected final String localizedShortDate(final Date date) {
        return dfShortDate.format(date);
    }

    /**
     * Gets as localized (medium) date string of a Date. The locale of the
     * current session is used.
     *
     * @param date
     *            The date.
     * @return The localized date string.
     */
    protected final String localizedMediumDate(final Date date) {
        return dfMediumDate.format(date);
    }

    /**
     * Gets as localized date/time string of a Date. The locale of the current
     * session is used.
     *
     * @param date
     *            The date.
     * @return The localized date/time string.
     */
    protected final String localizedDateTime(final Date date) {
        return dfDateTime.format(date);
    }

    /**
     * Gets as localized short date/time string of a Date. The locale of the
     * current session is used.
     *
     * @param date
     *            The date.
     * @return The localized short date/time string.
     */
    protected final String localizedShortDateTime(final Date date) {
        return dfShortDateTime.format(date);
    }

    /**
     * Localizes and format a string with placeholder arguments.
     *
     * @param key
     *            The key from the XML resource file
     * @param objects
     *            The values to fill the placeholders
     * @return The localized string.
     */
    protected final String localized(final String key,
            final Object... objects) {
        return MessageFormat.format(getLocalizer().getString(key, this),
                objects);
    }

    /**
     * Gives the localized string for a key.
     *
     * @param key
     *            The key from the XML resource file
     * @return The localized string.
     */
    protected final String localized(final String key) {
        return getLocalizer().getString(key, this);
    }

    /**
     * Gives the localized text for a button.
     *
     * @param btn
     *            The button.
     * @return The localized button text.
     */
    protected final String localized(final HtmlButtonEnum btn) {
        return btn.uiText(getLocale());
    }

    /**
     * Gets the POST-ed parameter value.
     *
     * @param parm
     *            Parameter name.
     * @return {@code null} when parameter is not present.
     */
    protected final String getParmValue(final String parm) {
        return getRequestCycle().getRequest().getPostParameters()
                .getParameterValue(parm).toString();
    }

    /**
     * Gets the POST-ed parameter boolean value.
     *
     * @param parm
     *            Parameter name.
     * @param defaultValue
     *            The dfaut value.
     * @return The boolean (default) value.
     */
    protected final boolean getParmBoolean(final String parm,
            final boolean defaultValue) {
        return getRequestCycle().getRequest().getPostParameters()
                .getParameterValue(parm).toBoolean(defaultValue);
    }

    /**
     * Gets the POST-ed parameter Long value.
     *
     * @param parm
     *            Parameter name.
     * @return The Long value.
     */
    protected final Long getParmLong(final String parm) {
        return getRequestCycle().getRequest().getPostParameters()
                .getParameterValue(parm).toLongObject();
    }

    /**
     *
     * @param getParms
     *            The {@link PageParameters}.
     * @param isGetAction
     *            {@code true} when a GET parameter.
     * @param parm
     *            The parameter name.
     * @return {@code null} when parameter is not present.
     */
    protected final String getParmValue(final PageParameters getParms,
            final boolean isGetAction, final String parm) {
        if (isGetAction) {
            return getParms.get(parm).toString();
        }
        return getParmValue(parm);
    }

    /**
     *
     * @return {@link DateFormat} for a short date.
     */
    protected final DateFormat getDfShortDate() {
        return dfShortDate;
    }

    /**
     * Gets the IP address of the client.
     *
     * @return IP address.
     */
    protected final String getClientIpAddr() {
        return ((ServletWebRequest) RequestCycle.get().getRequest())
                .getContainerRequest().getRemoteAddr();
    }

    /**
     *
     * @param isVisible
     * @param id
     * @param val
     * @return
     */
    protected final Label createVisibleLabel(final boolean isVisible,
            final String id, final String val) {

        Label label = new Label(id, val) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isVisible;
            }
        };
        return label;
    }

    /**
     *
     * @param isVisible
     * @param id
     * @param val
     * @param cssClass
     * @return
     */
    protected final Label createVisibleLabel(final boolean isVisible,
            final String id, final String val, final String cssClass) {

        Label label = createVisibleLabel(isVisible, id, val);

        if (isVisible && cssClass != null) {
            label.add(new AttributeModifier("class", cssClass));
        }

        return label;
    }

    /**
     *
     * @param isVisible
     * @param id
     * @param val
     */
    protected final void addVisible(final boolean isVisible, final String id,
            final String val) {
        addVisible(isVisible, id, val, null);
    }

    /**
     *
     * @param isVisible
     * @param id
     * @param val
     * @param cssClass
     *            Can be {@code null}.
     */
    protected final void addVisible(final boolean isVisible, final String id,
            final String val, final String cssClass) {
        add(createVisibleLabel(isVisible, id, val, cssClass));
    }

    /**
     *
     * @return The {@link HttpServletRequest}.
     */
    private HttpServletRequest getHttpServletRequest() {
        return (HttpServletRequest) getRequestCycle().getRequest()
                .getContainerRequest();
    }

    /**
     * Probes if servlet request method is {@link #POST_METHOD}. When
     * <i>not</i>, sets response page with error message by throwing an
     * exception.
     *
     * @throws RestartResponseException
     *             When servlet request method is <i>not</i>
     *             {@link #POST_METHOD}.
     */
    protected final void probePostMethod() {

        if (!getHttpServletRequest().getMethod()
                .equalsIgnoreCase(POST_METHOD)) {
            throw new RestartResponseException(new MessageContent(
                    AppLogLevelEnum.ERROR, "invalid access"));
        }
    }

    /**
     * Probes session user permission. When session user is <i>not</i>
     * authorized, sets response page with error message by throwing an
     * exception.
     *
     * @param oid
     *            The OID.
     * @param permission
     *            The required permission.
     * @return List with permissions, or {@code null} when undetermined.
     * @throws RestartResponseException
     *             When access is denied.
     */
    protected final List<ACLPermissionEnum> probePermission(
            final ACLOidEnum oid, final ACLPermissionEnum permission) {

        final List<ACLPermissionEnum> perms = ACCESS_CONTROL_SERVICE
                .getPermission(SpSession.get().getUser(), oid);

        if (perms == null) {
            return null;
        }

        if (!ACCESS_CONTROL_SERVICE.hasPermission(perms, permission)) {
            throw new RestartResponseException(NotAuthorized.class);
        }

        return perms;
    }

    /**
     * @param oid
     *            The OID.
     * @throws RestartResponseException
     *             When access is denied.
     */
    protected final void probePermissionToRead(final ACLOidEnum oid) {
        this.probePermission(oid, ACLPermissionEnum.READER);
    }

    /**
     * @param oid
     *            The OID.
     * @return {@code true} if session user has {@link ACLPermissionEnum#EDITOR}
     *         permission.
     * @throws RestartResponseException
     *             When read access is denied.
     */
    protected final boolean probePermissionToEdit(final ACLOidEnum oid) {

        final List<ACLPermissionEnum> perms =
                this.probePermission(oid, ACLPermissionEnum.READER);

        return perms == null || ACCESS_CONTROL_SERVICE.hasPermission(perms,
                ACLPermissionEnum.EDITOR);
    }

    /**
     * @return The {@link UserAgentHelper}.
     */
    protected final UserAgentHelper createUserAgentHelper() {
        return new UserAgentHelper(getHttpServletRequest());
    }

    /**
     * Adds a label/input text.
     *
     * @param wicketId
     *            The {@code wicket:id} of the {@code <input>} part.
     * @param attrIdFor
     *            The value of the HTML 'id' attribute of the {@code <input>}
     *            part, and the 'for' attribute of the {@code <label>} part.
     * @param attrValue
     *            The value of the HTML 'value' attribute of this radio button.
     */
    protected final void labelledText(final String wicketId,
            final String attrIdFor, final String attrValue) {
        Label labelWrk = new Label(wicketId);
        labelWrk.add(new AttributeModifier("id", attrIdFor));
        labelWrk.add(new AttributeModifier("value", attrValue));
        add(labelWrk);
        tagLabel(wicketId + "-label", wicketId, attrIdFor);
    }

    /**
     * @deprecated Adds a label.
     *             <p>
     *             Use {@link MarkupHelper#tagLabel(String, String, String)}.
     *             </p>
     *
     * @param wicketId
     *            The {@code wicket:id} of the HTML entity.
     * @param localizerKey
     *            The localizer key of the label text as used in
     *            {@link #getLocalizer()}.
     * @param attrFor
     *            The value of the HTML 'for' attribute.
     */
    @Deprecated
    protected final void tagLabel(final String wicketId,
            final String localizerKey, final String attrFor) {
        Label labelWrk = new Label(wicketId,
                getLocalizer().getString(localizerKey, this));
        labelWrk.add(new AttributeModifier("for", attrFor));
        add(labelWrk);
    }

    /**
     *
     * @param wicketIdBase
     *            The base {@code wicket:id} for the radio group items.
     * @param wicketIdSuffix
     *            The {@code wicket:id} suffix for this item.
     * @param attrName
     *            The value of the HTML 'name' attribute of this radio button.
     * @param attrValue
     *            The value of the HTML 'value' attribute of this radio button.
     * @param checked
     *            If {@code true} radio button is checked.
     */
    protected final void labelledRadio(final String wicketIdBase,
            final String wicketIdSuffix, final String attrName,
            final String attrValue, final boolean checked) {

        final String attrId = attrName + wicketIdSuffix;
        tagRadio(wicketIdBase + wicketIdSuffix, attrName, attrId, attrValue,
                checked);
        final Label labelWrk = new Label(
                wicketIdBase + wicketIdSuffix + "-label",
                getLocalizer().getString(wicketIdBase + wicketIdSuffix, this));
        labelWrk.add(new AttributeModifier("for", attrId));
        add(labelWrk);
    }

    /**
     *
     * @param wicketId
     *            The {@code wicket:id} for this radio group item.
     * @param attrName
     *            The value of the HTML 'name' attribute of this radio button.
     * @param attrId
     *            The value of the HTML 'id' attribute of this radio button.
     * @param attrValue
     *            The value of the HTML 'value' attribute of this radio button.
     * @param checked
     *            If {@code true} radio button is checked.
     */
    protected final void tagRadio(final String wicketId, final String attrName,
            final String attrId, final String attrValue,
            final boolean checked) {

        final Label labelWrk = new Label(wicketId);

        labelWrk.add(new AttributeModifier("name", attrName));
        labelWrk.add(new AttributeModifier("id", attrId));
        labelWrk.add(new AttributeModifier("value", attrValue));

        if (checked) {
            labelWrk.add(new AttributeModifier("checked", "checked"));
        }
        add(labelWrk);
    }

    /**
     * @return {@code true} when this request is from an intranet host and not
     *         SSL-tunneled.
     */
    protected final boolean isIntranetRequest() {

        final Url url = getRequestCycle().getRequest().getClientUrl();
        final String port = url.getPort().toString();

        return InetUtils.isIntranetBrowserHost(url.getHost())
                && (port.equals(ConfigManager.getServerSslPort())
                        || port.equals(ConfigManager.getServerPort()));
    }

    /**
     * @param printer
     *            Printer.
     * @return The printer image.
     */
    public static HtmlPrinterImgEnum getImgSrc(final Printer printer) {

        final Map<String, Device> terminalDevices = new HashMap<>();
        final Map<String, Device> readerDevices = new HashMap<>();

        return getImgSrc(printer, terminalDevices, readerDevices);
    }

    /**
     *
     * @param printer
     *            Printer.
     * @param terminalDevices
     *            The Terminal Devices responsible for printer being secured.
     * @param readerDevices
     *            The Reader Devices responsible for printer being secured.
     * @return The printer image.
     */
    public static HtmlPrinterImgEnum getImgSrc(final Printer printer,
            final Map<String, Device> terminalDevices,
            final Map<String, Device> readerDevices) {

        final PrinterService printerService =
                ServiceContext.getServiceFactory().getPrinterService();

        final HtmlPrinterImgEnum imageSrc;

        if (printerService.isJobTicketPrinter(printer.getId())) {

            imageSrc = HtmlPrinterImgEnum.JOBTICKET;

        } else {

            final MutableBoolean terminalSecured = new MutableBoolean();
            final MutableBoolean readerSecured = new MutableBoolean();

            final boolean isSecured = printerService.checkPrinterSecurity(
                    printer, terminalSecured, readerSecured, terminalDevices,
                    readerDevices);

            if (isSecured) {

                if (terminalSecured.booleanValue()
                        && readerSecured.booleanValue()) {
                    imageSrc = HtmlPrinterImgEnum.TERMINAL_AND_READER;
                } else if (terminalSecured.booleanValue()) {
                    imageSrc = HtmlPrinterImgEnum.TERMINAL;
                } else {
                    imageSrc = HtmlPrinterImgEnum.READER;
                }
            } else if (ConfigManager.instance()
                    .isNonSecureProxyPrinter(printer)) {
                imageSrc = HtmlPrinterImgEnum.NON_SECURE;
            } else {
                imageSrc = HtmlPrinterImgEnum.SECURE;
            }

        }
        return imageSrc;
    }

}
