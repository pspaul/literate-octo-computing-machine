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
package org.savapage.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.Session;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.mapper.parameter.UrlPathPageParametersEncoder;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.savapage.common.ConfigDefaults;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.RunMode;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.imaging.ImageUrl;
import org.savapage.core.jpa.tools.DatabaseTypeEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.services.helpers.ThirdPartyEnum;
import org.savapage.core.services.helpers.UserAuth;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.LocaleHelper;
import org.savapage.core.util.Messages;
import org.savapage.ext.oauth.OAuthProviderEnum;
import org.savapage.ext.payment.PaymentMethodEnum;
import org.savapage.server.api.JsonApiServer;
import org.savapage.server.cometd.AbstractEventService;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.img.ImageServer;
import org.savapage.server.ios.WebClipServer;
import org.savapage.server.ipp.IppPrintServer;
import org.savapage.server.ipp.IppPrintServerHomePage;
import org.savapage.server.pages.AbstractPage;
import org.savapage.server.pages.admin.AbstractAdminPage;
import org.savapage.server.pages.user.AbstractUserPage;
import org.savapage.server.raw.RawPrintServer;
import org.savapage.server.session.SpSession;
import org.savapage.server.webapp.CustomStringResourceLoader;
import org.savapage.server.webapp.OAuthRedirectPage;
import org.savapage.server.webapp.WebAppAdmin;
import org.savapage.server.webapp.WebAppJobTickets;
import org.savapage.server.webapp.WebAppPos;
import org.savapage.server.webapp.WebAppTypeEnum;
import org.savapage.server.webapp.WebAppUser;
import org.savapage.server.webprint.DropZoneResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.webjars.WicketWebjars;
import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;

/**
 * Application object for your web application. If you want to run this
 * application without deploying, run
 * {@link org.savapage.server.WebServer#main(String[])}.
 *
 * @author Rijk Ravestein
 *
 */
public final class WebApp extends WebApplication implements ServiceEntryPoint {

    /**
     * Used in {@code web.xml}.
     */
    public static final String MOUNT_PATH_COMETD = "/cometd";

    /**
     * The URL path for "printers" as used in {@link #MOUNT_PATH_PRINTERS}.
     */
    public static final String PATH_PRINTERS = "printers";

    /**
     * Used in this class to set mountPage() for {@link WebApp#PATH_PRINTERS}.
     */
    public static final String MOUNT_PATH_PRINTERS = "/" + PATH_PRINTERS;

    /**
     * Used in this class to set mountPage().
     */
    public static final String MOUNT_PATH_WEBAPP_USER = "/user";

    /**
     * The URL path for "printers" as used in {@link #MOUNT_PATH_WEBAPP_OAUTH}
     * and {@link #MOUNT_PATH_WEBAPP_USER_OAUTH}.
     */
    public static final String PATH_OAUTH = "oauth";

    /**
     * URL path to directly authenticate with OAuth provider for User Web App.
     * E.g. /oauth/google . Where "google" is lowercase
     * {@link OAuthProviderEnum} value.
     */
    public static final String MOUNT_PATH_WEBAPP_OAUTH = "/" + PATH_OAUTH;

    /**
     * URL path to directly authenticate with OAuth provider for User Web App .
     * E.g. /user/oauth/google . Where "google" is lowercase
     * {@link OAuthProviderEnum} value.
     */
    public static final String MOUNT_PATH_WEBAPP_USER_OAUTH =
            MOUNT_PATH_WEBAPP_USER + "/" + PATH_OAUTH;

    /**
     * Used in this class to set mountPage().
     */
    public static final String MOUNT_PATH_WEBAPP_ADMIN = "/admin";

    /**
     * The Point-of-Sale mount path.
     */
    public static final String MOUNT_PATH_WEBAPP_POS = "/pos";

    /**
     * The Job Tickets mount path.
     */
    public static final String MOUNT_PATH_WEBAPP_JOBTICKETS = "/jobtickets";

    /**
     * Used in this class to set mountPage().
     */
    private static final String MOUNT_PATH_API = "/api";

    /**
     * Mount path for WenPrint drop zone.
     */
    public static final String MOUNT_PATH_UPLOAD_WEBPRINT = "/upload/webprint";

    /**
     *
     */
    public static final String WEBJARS_PATH_JQUERY_CORE_JS =
            "jquery/current/jquery.js";

    /**
     *
     */
    public static final String PATH_IMAGES = "/images";

    /**
     *
     */
    public static final String PATH_IMAGES_PAYMENT = PATH_IMAGES + "/payment";

    /**
     * .
     */
    public static final String PATH_IMAGES_EXT_SUPPLIER =
            PATH_IMAGES + "/ext-supplier";

    /**
     * .
     */
    public static final String PATH_IMAGES_THIRDPARTY =
            PATH_IMAGES + "/thirdparty";

    /**
     * Basename of the properties file for web customization.
     */
    public static final String FILENAME_WEB_PROPERTIES = "web.properties";

    /**
     *
     */
    private static Properties theServerProps = new Properties();

    /**
    *
    */
    private static Properties theWebProps = new Properties();

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebApp.class);

    /**
     * Dictionary with key IP-address, getting the User of an active
     * authenticated WebApp Session.
     */
    private static Map<String, String> theDictIpAddrUser = new HashMap<>();

    /**
     * Dictionary with key SessionId, getting the IP-address of an active
     * authenticated WebApp Session.
     */
    private static Map<String, String> theDictSessionIpAddr = new HashMap<>();

    /**
     * Dictionary with key IP-address, getting the SessionId of an active
     * authenticated WebApp Session.
     */
    private static Map<String, String> theDictIpAddrSession = new HashMap<>();

    /**
     * The RAW Print Server.
     */
    private RawPrintServer rawPrintServer = null;

    /**
     * The {@link ServerPluginManager}.
     */
    private ServerPluginManager pluginManager;

    /**
     * Return a localized message string. IMPORTANT: The locale from the
     * application is used.
     *
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    private String localize(final String key, final String... args) {
        return Messages.getMessage(getClass(), key, args);
    }

    /**
     * Gets the relative PNG image URL of the payment method.
     * <p>
     * The name of the PNG file is EQ to the lower case enum value of the
     * payment method. For a bigger image "@2x" is appended.
     * </p>
     *
     * @param method
     *            The {@link PaymentMethodEnum}.
     * @param bigger
     *            {@code true} for bigger image.
     * @return The relative URL as string.
     */
    public static String getPaymentMethodImgUrl(final PaymentMethodEnum method,
            final boolean bigger) {

        final StringBuilder url =
                new StringBuilder().append(PATH_IMAGES_PAYMENT).append("/")
                        .append(method.toString().toLowerCase());

        if (bigger) {
            url.append("@2x");
        }
        url.append(".png");

        return url.toString();
    }

    /**
     * Gets the relative image URL of an {@link ExternalSupplierEnum}.
     *
     * @param extSupplierEnum
     *            The {@link ExternalSupplierEnum}.
     * @return The relative URL as string.
     */
    public static String getExtSupplierEnumImgUrl(
            final ExternalSupplierEnum extSupplierEnum) {

        final StringBuilder url =
                new StringBuilder().append(PATH_IMAGES_EXT_SUPPLIER).append("/")
                        .append(extSupplierEnum.getImageFileName());

        return url.toString();
    }

    /**
     * Gets the relative image URL of an {@link ThirdPartyEnum}.
     *
     * @param extSupplierEnum
     *            The {@link ThirdPartyEnum}.
     * @return The relative URL as string.
     */
    public static String
            getThirdPartyEnumImgUrl(final ThirdPartyEnum extSupplierEnum) {

        final StringBuilder url =
                new StringBuilder().append(PATH_IMAGES_THIRDPARTY).append("/")
                        .append(extSupplierEnum.getImageFileName());

        return url.toString();
    }

    /**
     * Gets the WebApp authenticated user on remote host using IP address of
     * remote host.
     * <p>
     * <b>Note</b>: IP User Session cache is used to retrieve the user.
     * </p>
     *
     * @param ipAddr
     *            The IP address of the remote host.
     * @return {@code null} when user is NOT found.
     */
    public static synchronized String getAuthUserByIpAddr(final String ipAddr) {
        return theDictIpAddrUser.get(ipAddr);
    }

    /**
     * Gets the number of authenticated user WebApp sessions.
     *
     * @return the number of sessions.
     */
    public static synchronized int getAuthUserSessionCount() {
        return theDictSessionIpAddr.size();
    }

    /**
     * Adds authenticated entry in IP User Session cache. When a user is already
     * present on the IP address it is replaced by the user offered here.
     *
     * @param webAppType
     *            The {@link WebAppTypeEnum}.
     * @param authMode
     *            The {@link UserAuth.Mode}. {@code null} when authenticated by
     *            (WebApp or Client) token.
     * @param sessionId
     *            The session ID as key.
     * @param ipAddr
     *            The IP address of the remote host.
     * @param user
     *            The authenticated user.
     */
    public synchronized void onAuthenticatedUser(
            final WebAppTypeEnum webAppType, final UserAuth.Mode authMode,
            final String sessionId, final String ipAddr, final String user) {

        /*
         * Removing the old session on same IP address
         */
        final String oldUser = theDictIpAddrUser.remove(ipAddr);

        if (oldUser == null) {
            /*
             * ADD first-time authenticated user on IP address
             */
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("IP User Session [" + ipAddr + "] [" + user + "] ["
                        + sessionId + "] added. Sessions ["
                        + (theDictIpAddrUser.size() + 1) + "].");
            }

        } else {

            /*
             * REMOVE current authenticated user on IP address
             */
            final String oldSession = theDictIpAddrSession.remove(ipAddr);

            if (oldSession == null) {

                LOGGER.error(
                        "addSessionIpUser: no session for " + "IP address ["
                                + ipAddr + "] of old user [" + oldUser + "]");

            } else {

                if (theDictSessionIpAddr.remove(oldSession) == null) {
                    LOGGER.error("addSessionIpUser: no IP address for "
                            + "old session [" + oldSession + "] of old user ["
                            + oldUser + "]");
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("IP User Session [" + ipAddr + "] [" + user
                            + "] [" + sessionId + "] replaced [" + oldUser
                            + "] [" + oldSession + "]. Sessions ["
                            + (theDictIpAddrUser.size() + 1) + "].");
                }
            }
        }

        /*
         * Add the new one.
         */
        theDictIpAddrUser.put(ipAddr, user);
        theDictIpAddrSession.put(ipAddr, sessionId);
        theDictSessionIpAddr.put(sessionId, ipAddr);

        /*
         *
         */
        AdminPublisher.instance().publish(PubTopicEnum.USER, PubLevelEnum.INFO,
                localize("pub-user-login-success", webAppType.getUiText(),
                        UserAuth.getUiText(authMode), user, ipAddr));
        //
        checkIpUserSessionCache("notifyAuthenticatedUser");
    }

    /**
     * Checks if the IP User Session User cache is in-sync.
     *
     * @param action
     *            Action string used for logging.
     * @return {@code true} if the cache is in-sync.
     */
    private static synchronized boolean
            checkIpUserSessionCache(final String action) {

        boolean inSync = theDictIpAddrUser.size() == theDictSessionIpAddr.size()
                && theDictSessionIpAddr.size() == theDictIpAddrSession.size();

        if (!inSync) {
            LOGGER.error(action + ": SessionIpUserCache is out-of-sync:"
                    + " IPaddr->User [" + theDictIpAddrUser.size() + "]"
                    + " IPaddr->SessionId [" + theDictIpAddrSession.size() + "]"
                    + " SessionId->IPaddr [" + theDictSessionIpAddr.size()
                    + "]");
        }
        return inSync;
    }

    /**
     * Covariant override for easy getting the current WebApplication without
     * having to cast it.
     *
     * @return The {@link WebApp}.
     */
    public static WebApp get() {
        return (WebApp) WebApplication.get();
    }

    /**
     *
     * @return The {@link ServerPluginManager}.
     */
    public ServerPluginManager getPluginManager() {
        return this.pluginManager;
    }

    /**
     * @param props
     *            The server properties.
     */
    public static void setServerProps(final Properties props) {
        theServerProps = props;
        ConfigManager.setServerProps(props);
        ConfigManager.setWebAppAdminPath(MOUNT_PATH_WEBAPP_ADMIN);
    }

    /**
     * Get a property from {@link WebApp#FILENAME_WEB_PROPERTIES}.
     *
     * @param key
     *            The key of the property
     * @return {@code null} when not found.
     */
    public static String getWebProperty(final String key) {
        return theWebProps.getProperty(key);
    }

    /**
     * Loads the web properties from file {@link #FILENAME_WEB_PROPERTIES}.
     *
     * @throws IOException
     *             When error loading properties file.
     */
    public static void loadWebProperties() throws IOException {

        final StringBuilder builder = new StringBuilder();

        builder.append(ConfigManager.getServerHome()).append("/custom/")
                .append(FILENAME_WEB_PROPERTIES);

        final File file = new File(builder.toString());

        if (!file.exists()) {
            return;
        }

        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            theWebProps.load(fis);
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    /**
     *
     * @return The non-SSL server port.
     */
    public static String getServerPort() {
        return ConfigManager.getServerPort(theServerProps);
    }

    /**
     *
     * @return The SSL server port.
     */
    public static String getServerSslPort() {
        return theServerProps.getProperty("server.ssl.port",
                ConfigDefaults.SERVER_SSL_PORT);
    }

    /**
     * The location of the Mobi Pick jQuery library files.
     *
     * @return The location including a trailing '/'.
     */
    public static String getJqMobiPickLocation() {
        return "mobipick/";
    }

    /**
     *
     * @param namePath
     * @return
     */
    public static JavaScriptReferenceHeaderItem
            getWebjarsJsRef(final String namePath) {
        return JavaScriptHeaderItem
                .forReference(new WebjarsJavaScriptResourceReference(namePath));
    }

    /**
     *
     * @param namePath
     * @return
     */
    public static CssReferenceHeaderItem
            getWebjarsCssRef(final String namePath) {
        return CssHeaderItem
                .forReference(new WebjarsCssResourceReference(namePath));
    }

    /**
     * Indicator if the WebApp is initialized.
     */
    private static boolean theIsInitialized = false;

    /**
     * Initializes the WepApp.
     * <p>
     * Mounts (bookmarkable) page classes to a given path.
     * </p>
     * <p>
     * NOTE: DocBook generated HTML chunks (and Maven site generated HTML) are
     * handled by <savapage-docs> servletname, see web.xml and
     * {@link DocumentationServlet}.
     * </p>
     */
    private void myInitialize() {

        java.io.FileInputStream fis = null;

        try {

            /*
             * Configure so the wicket application maps requests for /webjars
             * and instances of IWebjarsResourceReference to the
             * /META-INF/resources/webjars directory of all the JARs in the
             * CLASSPATH.
             */
            WicketWebjars.install(this);

            /*
             *
             */
            mountResource(MOUNT_PATH_UPLOAD_WEBPRINT,
                    new DropZoneResourceReference("webprint"));

            /*
             * Mount a page class to a given path
             *
             * http://wicketinaction.com/2011/07/wicket-1-5-mounting-pages/
             */
            mountPage(MOUNT_PATH_WEBAPP_ADMIN, WebAppAdmin.class);
            mountPage(MOUNT_PATH_WEBAPP_JOBTICKETS, WebAppJobTickets.class);
            mountPage(MOUNT_PATH_WEBAPP_POS, WebAppPos.class);

            mountPage(MOUNT_PATH_WEBAPP_USER, WebAppUser.class);

            mountPage(MOUNT_PATH_WEBAPP_OAUTH, OAuthRedirectPage.class);
            mountPage(MOUNT_PATH_WEBAPP_USER_OAUTH, OAuthRedirectPage.class);

            mountPage(MOUNT_PATH_API, JsonApiServer.class);
            mountPage(MOUNT_PATH_PRINTERS, IppPrintServer.class);

            /*
             * Mount installation of iOS WebClip
             */
            mountPage("/ios/install", WebClipServer.class);

            /*
             * Arbitrary named parameters -
             * /page/param1Name/param1Value/param2Name/param2Value
             *
             * Now a request to "/page/a/1/b/2" will be handled by MyPage and
             * the parameters can be get with PageParameters.get(String) (e.g.
             * parameters.get("a") will return "1")
             *
             * https://cwiki.apache.org/confluence/display/WICKET/Request+
             * mapping
             */
            mount(new MountedMapper(ImageUrl.MOUNT_PATH, ImageServer.class,
                    new UrlPathPageParametersEncoder()));

            /*
             * Mount ALL bookmarkable pages in the class package to the given
             * path.
             */
            mountPackage("/pages", AbstractPage.class);
            mountPackage("/pages/admin", AbstractAdminPage.class);
            mountPackage("/pages/user", AbstractUserPage.class);

            //
            replaceJQueryCore();

            /*
             * Initialize the ConfigManager (use empty properties for now).
             */
            ConfigManager.instance().init(RunMode.SERVER,
                    DatabaseTypeEnum.Internal);

            AppLogHelper.logInfo(getClass(), "WebApp.starting",
                    ConfigManager.getAppVersionBuild());

            /*
             * Web server.
             */
            final StringBuilder logMsg = new StringBuilder();
            logMsg.append("Web Server started on port ");

            if (!WebServer.isSSLOnly()) {
                logMsg.append(WebServer.getServerPort());

                if (WebServer.isSSLRedirect()) {
                    logMsg.append(" with redirect to ");
                } else {
                    logMsg.append(" and ");
                }
            }
            logMsg.append(WebServer.getServerPortSsl()).append(" (SSL)");
            SpInfo.instance().log(logMsg.toString());

            //
            final WebServer.SslCertInfo sslCert = WebServer.getSslCertInfo();

            if (sslCert == null) {
                SpInfo.instance().log("SSL Certificate info NOT avaliable.");
            } else {
                final LocaleHelper helper = new LocaleHelper(Locale.ENGLISH);

                logMsg.setLength(0);
                logMsg.append("SSL Cert Issuer  [")
                        .append(sslCert.getIssuerCN()).append("]");

                if (sslCert.isSelfSigned()) {
                    logMsg.append(" self-signed.");
                }
                SpInfo.instance().log(logMsg.toString());

                logMsg.setLength(0);
                logMsg.append("SSL Cert Created [").append(
                        helper.getLongMediumDateTime(sslCert.getCreationDate()))
                        .append("]");
                SpInfo.instance().log(logMsg.toString());

                logMsg.setLength(0);
                logMsg.append("SSL Cert Expires [")
                        .append(helper
                                .getLongMediumDateTime(sslCert.getNotAfter()))
                        .append("]");
                SpInfo.instance().log(logMsg.toString());
            }

            /*
             *
             */
            final Long maxnetworkdelay = Long.parseLong(theServerProps
                    .getProperty("cometd.client.maxnetworkdelay.msec",
                            String.valueOf(AbstractEventService
                                    .getMaxNetworkDelayMsecDefault())));

            AbstractEventService.setMaxNetworkDelay(maxnetworkdelay);

            /*
             *
             */
            ConfigManager.instance().initScheduler();

            if (WebServer.isWebAppCustomI18n()) {
                SpInfo.instance().log("Web App Custom i18n enabled.");
            }

            /*
             * Server plug-in manager.
             */
            this.pluginManager = ServerPluginManager
                    .create(ConfigManager.getServerExtHome());

            this.pluginManager.start();

            SpInfo.instance().log(this.pluginManager.asLoggingInfo());

            /*
             * IPP Print Server.
             */
            IppPrintServer.init();

            /*
             * IP Print Server (RAW)
             */
            final int iRawPrintPort = Integer.valueOf(theServerProps
                    .getProperty(ConfigManager.SERVER_PROP_PRINTER_RAW_PORT,
                            ConfigManager.PRINTER_RAW_PORT_DEFAULT));

            if (iRawPrintPort == 0) {
                SpInfo.instance().log("IP Print Server disabled.");
            } else {
                this.rawPrintServer = new RawPrintServer(iRawPrintPort);
                this.rawPrintServer.start();
            }

        } catch (Exception e) {

            throw new SpException(e.getMessage(), e);

        } finally {

            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            ServiceContext.close();
        }
    }

    /**
     * Yes, we make {@link IppPrintServerHomePage} the home page. See Mantis
     * #154.
     *
     * @see org.apache.wicket.Application#getHomePage()
     *
     * @return The home page class for the application.
     */
    @Override
    public Class<IppPrintServerHomePage> getHomePage() {
        return IppPrintServerHomePage.class;
    }

    /**
     * Applies Wicket Application settings.
     * <p>
     * See Wicket Wiki <a href=
     * "https://cwiki.apache.org/WICKET/error-pages-and-feedback-messages.html"
     * >here</a>.
     * </p>
     * <p>
     * There are also settings several areas you should check out in:
     * <ul>
     * <li>{@link WebApplication#getApplicationSettings()}</li>
     * <li>{@link WebApplication#getDebugSettings()}</li>
     * <li>{@link WebApplication#getMarkupSettings()}</li>
     * <li>{@link WebApplication#getExceptionSettings()}</li>
     * <li>{@link WebApplication#getPageSettings()}</li>
     * <li>{@link WebApplication#getRequestCycleSettings()}</li>
     * <li>{@link WebApplication#getSecuritySettings()}</li>
     * </ul>
     * </p>
     */
    private void applyWicketApplicationSettings() {
        getApplicationSettings().setUploadProgressUpdatesEnabled(true);
    }

    /**
     * Replaces the built-in jQuery Core library with the one we need for JQuery
     * Mobile and other jQuery plugins.
     * <p>
     * See <a href=
     * "http://wicketinaction.com/2012/07/wicket-6-resource-management/"> Wicket
     * 6 resource management</a>.
     * </p>
     */
    private void replaceJQueryCore() {

        final JavaScriptReferenceHeaderItem replacement =
                getWebjarsJsRef(WEBJARS_PATH_JQUERY_CORE_JS);

        addResourceReplacement(
                (JavaScriptResourceReference) getJavaScriptLibrarySettings()
                        .getJQueryReference(),
                replacement.getReference());
    }

    /**
     * Add the {@link CustomStringResourceLoader} as first resource loader.
     */
    private void addCustomStringResourceLoader() {
        /*
         * Retrieve ResourceSettings and then the list of resource loaders.
         */
        final List<IStringResourceLoader> resourceLoaders =
                getResourceSettings().getStringResourceLoaders();
        /*
         * Add custom resource loader as first.
         */
        resourceLoaders.add(0, new CustomStringResourceLoader());
    }

    /**
     * @see org.apache.wicket.Application#init()
     */
    @Override
    public void init() {

        super.init();

        if (WebServer.isWebAppCustomI18n()) {
            addCustomStringResourceLoader();
        }

        applyWicketApplicationSettings();

        getRequestCycleListeners().add(new SpRequestCycleListener());

        /*
         * Sets the time that a request will by default be waiting for the
         * previous request to be handled before giving up.
         *
         * STICK TO THE DEFAULT FOR NOW
         *
         * getRequestCycleSettings().setTimeout(Duration.seconds(60));
         */

        /*
         * Note: the init is called for EACH filter in the web.xml. Since
         * several filters can point to one class (this one) we need to guard
         * that initialization is done once.
         */
        synchronized (this) {
            if (!theIsInitialized) {
                theIsInitialized = true;
                myInitialize();
            }
        }
    }

    @Override
    public Session newSession(final Request request, final Response response) {

        final String remoteAddr = ((ServletWebRequest) request)
                .getContainerRequest().getRemoteAddr();

        final String urlPath = request.getUrl().getPath();

        final String debugMsg;

        if (LOGGER.isDebugEnabled()) {
            debugMsg = String.format("newSession: URL path [%s] IP [%s]",
                    urlPath, remoteAddr);
        } else {
            debugMsg = null;
        }

        final Session session;

        /*
         * Mind the "/" at the beginning of the MOUNT_PATH_PRINTERS constant.
         */
        if (urlPath.startsWith(MOUNT_PATH_PRINTERS.substring(1))) {

            /*
             * IMPORTANT: do NOT bind() since we want print request sessions to
             * be temporary.
             *
             * Note that our own SpSession object is used. This is because the
             * PPD download needs a SpSession.
             */
            session = new SpSession(request);
            session.invalidateNow();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("%s [TEMP]", debugMsg));
            }

        } else if (urlPath.startsWith(MOUNT_PATH_COMETD.substring(1))) {

            // TODO: is this code ever executed?

            /*
             * IMPORTANT: do NOT bind() since we want cometd sessions to be
             * temporary. If we DO bind() this new session, then things get
             * mixed up between the regular SpSession and this session: because
             * at some point this cometd session will be used for /api calls,
             * which will fail because because they are not of type SpSession.
             * But even if they were of the right SpSession type, /api handler
             * would deny access because the session does not contain a valid
             * user. So, we just create a default session and do NOT bind.
             */
            session = super.newSession(request, response);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("%s [TEMP]", debugMsg));
            }

        } else {

            session = new SpSession(request);

            /*
             * IMPORTANT: A bind() is essential, otherwise the session is
             * temporary and not persistent across JSON API calls. The bind()
             * will also generate the session id.
             */
            session.bind();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("%s [%s]. Sessions [%d]", debugMsg,
                        session.getId(), theDictIpAddrUser.size()));
            }
        }
        return session;
    }

    @Override
    public void sessionUnbound(final String sessionId) {

        super.sessionUnbound(sessionId);

        synchronized (this) {

            String ipAddr = theDictSessionIpAddr.remove(sessionId);

            if (ipAddr == null) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("IP User Session [?.?.?.?] [" + sessionId
                            + "] unbound. Sessions [" + theDictIpAddrUser.size()
                            + "].");
                }

            } else {

                if (theDictIpAddrSession.remove(ipAddr) == null) {
                    LOGGER.error("Inconsistent IP User Session cache: "
                            + "no session found for [" + ipAddr + "]");
                }

                final String user = theDictIpAddrUser.remove(ipAddr);

                if (user == null) {

                    LOGGER.error("Inconsistent IP User Session cache: "
                            + "no user found for [" + ipAddr + "]");

                } else {

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("IP User Session [" + ipAddr + "] [" + user
                                + "] [" + sessionId + "] removed. Sessions ["
                                + (theDictIpAddrUser.size()) + "].");
                    }

                    /*
                     * TODO: how to get the WebAppTypEnum?
                     */
                    AdminPublisher.instance().publish(PubTopicEnum.USER,
                            PubLevelEnum.INFO,
                            localize("pub-user-logout", user, ipAddr));
                }

            }
            checkIpUserSessionCache("sessionUnbound");
        }
    }

    @Override
    protected void onDestroy() {
        if (this.pluginManager != null) {
            this.pluginManager.stop();
        }
    }

}
