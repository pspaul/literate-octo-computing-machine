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
package org.savapage.server.ext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.UserAttrDao;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dto.UserPaymentGatewayDto;
import org.savapage.core.ipp.routing.IppRoutingContext;
import org.savapage.core.ipp.routing.IppRoutingListener;
import org.savapage.core.ipp.routing.IppRoutingResult;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.AccountingException;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.BitcoinUtil;
import org.savapage.core.util.CurrencyUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.Messages;
import org.savapage.ext.ServerPlugin;
import org.savapage.ext.ServerPluginContext;
import org.savapage.ext.ServerPluginException;
import org.savapage.ext.notification.JobTicketCancelEvent;
import org.savapage.ext.notification.JobTicketCloseEvent;
import org.savapage.ext.notification.NotificationEventProgressImpl;
import org.savapage.ext.notification.NotificationListener;
import org.savapage.ext.notification.NotificationPlugin;
import org.savapage.ext.oauth.OAuthClientPlugin;
import org.savapage.ext.oauth.OAuthProviderEnum;
import org.savapage.ext.payment.PaymentGateway;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.PaymentGatewayListener;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.ext.payment.PaymentGatewayTrx;
import org.savapage.ext.payment.PaymentGatewayTrxEvent;
import org.savapage.ext.payment.PaymentMethodEnum;
import org.savapage.ext.payment.bitcoin.BitcoinGateway;
import org.savapage.ext.payment.bitcoin.BitcoinGatewayListener;
import org.savapage.ext.payment.bitcoin.BitcoinGatewayTrx;
import org.savapage.ext.payment.bitcoin.BitcoinWalletInfo;
import org.savapage.ext.print.IppRoutingPlugin;
import org.savapage.ext.rest.RestClient;
import org.savapage.server.CustomWebServlet;
import org.savapage.server.WebApp;
import org.savapage.server.WebAppParmEnum;
import org.savapage.server.callback.CallbackServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for reading plug-in property files and then
 * instantiating each plug-in with the properties found.
 *
 * @author Rijk Ravestein
 *
 */
public final class ServerPluginManager
        implements PaymentGatewayListener, BitcoinGatewayListener,
        NotificationListener, IppRoutingListener, ServerPluginContext {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ServerPluginManager.class);

    /**
     * The number of satoshi in one (1) BTC.
     */
    private static final BigDecimal SATOSHIS_IN_BTC =
            BigDecimal.valueOf(BitcoinUtil.SATOSHIS_IN_BTC);

    /** */
    private static final String STAT_TRUSTED = "TRUSTED";
    /** */
    private static final String STAT_ACKNOWLEDGED = "ACKNOWLEDGED";
    /** */
    private static final String STAT_CONFIRMED = "CONFIRMED";
    /** */
    private static final String STAT_CANCELLED = "CANCELLED";
    /** */
    private static final String STAT_EXPIRED = "EXPIRED";
    /** */
    private static final String STAT_FAILED = "FAILED";

    /**
     * Property key prefix.
     */
    private static final String PROP_KEY_PLUGIN_PFX = "savapage.plugin.";

    /**
     * Property key for plug-in name.
     */
    private static final String PROP_KEY_PLUGIN_NAME =
            PROP_KEY_PLUGIN_PFX + "name";

    /**
     * Property key for plug-in class name.
     */
    private static final String PROP_KEY_PLUGIN_CLASS =
            PROP_KEY_PLUGIN_PFX + "class";

    /**
     * Property key for plug-in ID.
     */
    private static final String PROP_KEY_PLUGIN_ID = PROP_KEY_PLUGIN_PFX + "id";

    /**
     * Property key for plug-in enable switch.
     */
    private static final String PROP_KEY_PLUGIN_ENABLE =
            PROP_KEY_PLUGIN_PFX + "enable";

    /**
     * Property key for plug-in online switch.
     */
    private static final String PROP_KEY_PLUGIN_ONLINE =
            PROP_KEY_PLUGIN_PFX + "online";

    /**
     * Property key for plug-in live switch.
     */
    private static final String PROP_KEY_PLUGIN_LIVE =
            PROP_KEY_PLUGIN_PFX + "live";

    /**
     * All {@link PaymentGatewayPlugin} instances.
     */
    private final Map<String, PaymentGatewayPlugin> paymentPlugins =
            new HashMap<>();

    /**
     * Subset of {@link #paymentPlugins}.
     */
    private final Map<String, PaymentGateway> paymentGateways = new HashMap<>();

    /**
     * Subset of {@link #paymentPlugins}.
     */
    private final Map<String, BitcoinGateway> bitcoinGateways = new HashMap<>();

    /**
     * All {@link OAuthClientPlugin} instances.
     */
    private final Map<OAuthProviderEnum, Map<String, OAuthClientPlugin>> //
    oauthClientPlugins = new HashMap<>();

    /**
     * All {@link NotificationPlugin} instances.
     */
    private final Map<String, NotificationPlugin> notificationPlugins =
            new HashMap<>();

    /**
     * All {@link IppRoutingPlugin} instances.
     */
    private final Map<String, IppRoutingPlugin> ippRoutingPlugins =
            new HashMap<>();

    /**
     * All plug-in instances.
     */
    private final List<ServerPlugin> allPlugins = new ArrayList<>();

    /** */
    private BitcoinWalletInfo walletInfoCache;

    /** */
    private final Object walletInfoCacheMutex = new Object();

    /**
     *
     * @return The {@link Map} of (@link {@link PaymentGatewayPlugin} instances
     *         by unique ID.
     */
    public Map<String, PaymentGatewayPlugin> getPaymentPlugins() {
        return paymentPlugins;
    }

    /**
     *
     * @return The {@link Map} of (@link {@link OAuthClientPlugin} instances by
     *         unique ID.
     */
    public Map<OAuthProviderEnum, Map<String, OAuthClientPlugin>>
            getOAuthClientPlugins() {
        return oauthClientPlugins;
    }

    /**
     *
     * @throws IOException
     * @throws PaymentGatewayException
     */
    public void refreshWalletInfoCache()
            throws PaymentGatewayException, IOException {

        final BitcoinGateway gateway = this.getBitcoinGateway();

        if (gateway == null) {
            return;
        }

        synchronized (this.walletInfoCacheMutex) {
            this.walletInfoCache = gateway
                    .getWalletInfo(ConfigManager.getAppCurrency(), false);
        }
    }

    /**
     * @param baseCurrency
     *            The base {@link Currency).
     * @return {@code true} if the {@link #walletInfoCache} is expired or
     *         {@code null}.
     */
    private boolean isWalletInfoCacheExpired(final Currency baseCurrency) {

        if (this.walletInfoCache == null) {
            return true;
        }

        /*
         * Did base currency change?
         */
        if (baseCurrency != null && !this.walletInfoCache.getCurrencyCode()
                .equals(baseCurrency.getCurrencyCode())) {
            return true;
        }

        long timeRef = this.walletInfoCache.getDate().getTime();

        timeRef += ConfigManager.instance().getConfigLong(
                Key.WEBAPP_ADMIN_BITCOIN_WALLET_CACHE_EXPIRY_SECS)
                * DateUtil.DURATION_MSEC_SECOND;

        return timeRef < System.currentTimeMillis();
    }

    /**
     * Gets a deep copy of the cached bitcoin wallet info.
     *
     * @return A deep copy of the cached {@link BitcoinWalletInfo}.
     * @throws IOException
     * @throws PaymentGatewayException
     */
    public BitcoinWalletInfo getWalletInfoCache(final BitcoinGateway gateway,
            final boolean refresh) throws PaymentGatewayException, IOException {

        synchronized (this.walletInfoCacheMutex) {

            final Currency baseCurrency = ConfigManager.getAppCurrency();

            if (refresh || isWalletInfoCacheExpired(baseCurrency)) {
                this.walletInfoCache =
                        gateway.getWalletInfo(baseCurrency, false);
            }

            try {
                return (BitcoinWalletInfo) this.walletInfoCache.clone();
            } catch (CloneNotSupportedException e) {
                throw new SpException(e.getMessage(), e);
            }
        }
    }

    /**
     * Creates and loads the {@link ServerPlugin} as described in a
     * {@link Properties} file.
     *
     * @param istr
     *            The {@link InputStream} of the {@link Properties} file.
     * @param istrName
     *            The name used for logging.
     * @return {@code true} when plug-in was successfully loaded.
     * @throws IOException
     *             If an error occurs when loading the properties file.
     */
    private boolean loadPlugin(final InputStream istr, final String istrName)
            throws IOException {

        // Load
        final Properties props = new Properties();
        props.load(istr);

        /*
         * Enabled?
         */
        if (!Boolean.parseBoolean(props.getProperty(PROP_KEY_PLUGIN_ENABLE,
                Boolean.FALSE.toString()))) {
            LOGGER.warn(String.format("Plugin [%s] is disabled.", istrName));
            return false;
        }

        /*
         * Checking invariants.
         */
        final String pluginId = props.getProperty(PROP_KEY_PLUGIN_ID);

        if (StringUtils.isBlank(pluginId)) {
            LOGGER.warn(
                    String.format("Plugin [%s]: plugin ID missing.", istrName));
            return false;
        }

        //
        final String pluginName = props.getProperty(PROP_KEY_PLUGIN_NAME);

        if (StringUtils.isBlank(pluginName)) {
            LOGGER.warn(String.format("Plugin [%s]: plugin name missing.",
                    istrName));
            return false;
        }

        //
        final boolean pluginLive = Boolean.parseBoolean(props
                .getProperty(PROP_KEY_PLUGIN_LIVE, Boolean.FALSE.toString()));

        final boolean pluginOnline = Boolean.parseBoolean(props
                .getProperty(PROP_KEY_PLUGIN_ONLINE, Boolean.TRUE.toString()));

        //
        final String pluginClassName = props.getProperty(PROP_KEY_PLUGIN_CLASS);

        if (StringUtils.isBlank(pluginClassName)) {
            LOGGER.warn(String.format("Plugin [%s]: plugin class missing.",
                    istrName));
            return false;
        }

        final Class<?> clazz;
        try {
            clazz = Class.forName(pluginClassName);
        } catch (ClassNotFoundException e) {
            LOGGER.error(String.format("Class %s not found", pluginClassName));
            return false;
        }

        final Constructor<?> ctor;

        try {
            ctor = clazz.getConstructor();

            final Object plugin = ctor.newInstance();

            final String pluginType;

            if (plugin instanceof PaymentGateway) {

                pluginType = PaymentGateway.class.getSimpleName();

                final PaymentGateway paymentPlugin = (PaymentGateway) plugin;

                paymentPlugin.onInit(pluginId, pluginName, pluginLive,
                        pluginOnline, props, this);
                paymentPlugin.onInit(this);

                paymentPlugins.put(pluginId, paymentPlugin);
                paymentGateways.put(pluginId, paymentPlugin);

            } else if (plugin instanceof BitcoinGateway) {

                pluginType = BitcoinGateway.class.getSimpleName();

                final BitcoinGateway paymentPlugin = (BitcoinGateway) plugin;

                paymentPlugin.onInit(pluginId, pluginName, pluginLive,
                        pluginOnline, props, this);
                paymentPlugin.onInit(this);

                paymentPlugins.put(pluginId, paymentPlugin);
                bitcoinGateways.put(pluginId, paymentPlugin);

            } else if (plugin instanceof OAuthClientPlugin) {

                pluginType = OAuthClientPlugin.class.getSimpleName();

                final OAuthClientPlugin oauthPlugin =
                        (OAuthClientPlugin) plugin;

                oauthPlugin.onInit(pluginId, pluginName, pluginLive,
                        pluginOnline, props, this);

                Map<String, OAuthClientPlugin> mapPluginInstance =
                        this.oauthClientPlugins.get(oauthPlugin.getProvider());

                if (mapPluginInstance == null) {
                    mapPluginInstance = new HashMap<>();
                }

                mapPluginInstance.put(oauthPlugin.getInstanceId(), oauthPlugin);

                this.oauthClientPlugins.put(oauthPlugin.getProvider(),
                        mapPluginInstance);

                validateOAuthCallbackUrl(oauthPlugin);

            } else if (plugin instanceof NotificationPlugin) {

                pluginType = NotificationPlugin.class.getSimpleName();

                final NotificationPlugin notificationPlugin =
                        (NotificationPlugin) plugin;

                notificationPlugin.onInit(pluginId, pluginName, pluginLive,
                        pluginOnline, props, this);

                this.notificationPlugins.put(pluginId, notificationPlugin);

            } else if (plugin instanceof IppRoutingPlugin) {

                pluginType = IppRoutingPlugin.class.getSimpleName();

                final IppRoutingPlugin ippRoutingPlugin =
                        (IppRoutingPlugin) plugin;

                ippRoutingPlugin.onInit(pluginId, pluginName, pluginLive,
                        pluginOnline, props, this);

                this.ippRoutingPlugins.put(pluginId, ippRoutingPlugin);

            } else {

                throw new IllegalArgumentException(
                        String.format("Unsupported plugin type [%s]",
                                plugin.getClass().getName()));
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        String.format("%s [%s] loaded.", pluginType, istrName));
            }

            this.allPlugins.add((ServerPlugin) plugin);

        } catch (NoSuchMethodException | SecurityException
                | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | ServerPluginException e) {

            publishAndLogEvent(PubLevelEnum.ERROR, PubTopicEnum.PLUGIN,
                    String.format("Plugin [%s] could not be loaded: %s",
                            clazz.getSimpleName(), e.getMessage()));
            return false;
        }

        return true;
    }

    /**
     * Validates an OAuth Client Plug-in.
     *
     * @param plugin
     *            The plug-in.
     * @throws MalformedURLException
     *             When the callback URL is invalid.
     */
    private void validateOAuthCallbackUrl(final OAuthClientPlugin plugin)
            throws MalformedURLException {

        final StringBuilder query = new StringBuilder();
        final StringBuilder path = new StringBuilder();

        query.append(WebAppParmEnum.SP_OAUTH.parm()).append('=')
                .append(plugin.getProvider().toString().toLowerCase());

        path.append(WebApp.MOUNT_PATH_WEBAPP_USER).append("/")
                .append(WebAppParmEnum.SP_OAUTH.parm()).append('/')
                .append(plugin.getProvider().toString().toLowerCase());

        if (StringUtils.isNotBlank(plugin.getInstanceId())) {

            query.append('&').append(WebAppParmEnum.SP_OAUTH_ID.parm())
                    .append('=').append(plugin.getInstanceId());

            path.append('/').append(plugin.getInstanceId());
        }

        final String pluginQuery = plugin.getCallbackUrl().getQuery();
        final String pluginPath = plugin.getCallbackUrl().getPath();

        final boolean isValid;

        if (StringUtils.isBlank(pluginQuery)) {
            isValid = StringUtils.isNotBlank(pluginPath)
                    && pluginPath.equals(path.toString());
        } else {
            isValid = pluginQuery.equals(query.toString());
        }

        if (!isValid) {
            throw new MalformedURLException(String.format(
                    "Plugin [%s] callback URL [%s] "
                            + "must have query [%s] or path [%s]",
                    plugin.getId(), plugin.getCallbackUrl().toString(),
                    query.toString(), path.toString()));
        }
    }

    /**
     * Loads plug-in descriptors from a directory location and then creates
     * plug-in instances.
     *
     * @param pluginHome
     *            The directory with the plug-in property files.
     */
    private void loadPlugins(final File pluginHome) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Loading plugins from [" + pluginHome.getAbsolutePath()
                    + "]");
        }

        final File[] files = pluginHome.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(final File dir, final String name) {
                return FilenameUtils.isExtension(name, "properties");
            }
        });

        if (files == null) {
            LOGGER.error(String.format("Plugin home [%s] is not a directory.",
                    pluginHome.getAbsolutePath()));
            return;
        }

        for (final File file : files) {

            if (!file.isFile()) {
                continue;
            }

            try (FileInputStream fstream = new FileInputStream(file);) {
                loadPlugin(fstream, file.getName());
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                continue;
            }
        }
    }

    /**
     * @return {@code true} is Financial system is enabled.
     */
    private static boolean isFinEnabled() {
        return StringUtils.isNotBlank(ConfigManager.getAppCurrencyCode());
    }

    /**
     * Gets the first {@link BitcoinGateway}.
     *
     * @return The {@link BitcoinGateway}, or {@code null} when not found.
     */
    public BitcoinGateway getBitcoinGateway() {

        if (isFinEnabled()) {
            for (final Entry<String, BitcoinGateway> entry : this.bitcoinGateways
                    .entrySet()) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Gets the first {@link PaymentGateway} with at least one (1) external
     * {@link PaymentMethodEnum}.
     *
     * @return The {@link PaymentGateway}, or {@code null} when not found.
     * @throws PaymentGatewayException
     */
    public PaymentGateway getExternalPaymentGateway()
            throws PaymentGatewayException {

        for (final Entry<String, PaymentGateway> entry : this.paymentGateways
                .entrySet()) {

            if (!entry.getValue().getExternalPaymentMethods().isEmpty()) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Gets the {@link PaymentGateway} by its ID.
     *
     * @param gatewayId
     *            ID of the gateway.
     * @return The {@link PaymentGateway}, or {@code null} when not found.
     */
    public PaymentGateway getExternalPaymentGateway(final String gatewayId) {
        final PaymentGatewayPlugin plugin = this.getPaymentGateway(gatewayId);
        if (plugin instanceof PaymentGateway) {
            return (PaymentGateway) plugin;
        }
        return null;
    }

    /**
     * Gets the {@link PaymentGatewayPlugin} by its ID.
     *
     * @param gatewayId
     *            ID of the gateway.
     * @return The {@link PaymentGatewayPlugin}, or {@code null} when not found.
     */
    public PaymentGatewayPlugin getPaymentGateway(final String gatewayId) {
        return this.paymentPlugins.get(gatewayId);
    }

    /**
     * Gets the {@link OAuthClientPlugin} by its IDs.
     *
     * @param provider
     *            The OAuth provider.
     * @param instanceId
     *            The OAuth instance ID (can be {@code null}).
     * @return The {@link OAuthClientPlugin}, or {@code null} when not found.
     */
    public OAuthClientPlugin getOAuthClient(final OAuthProviderEnum provider,
            final String instanceId) {
        return this.oauthClientPlugins.get(provider).get(instanceId);
    }

    /**
     *
     * @param plugin
     *            The{@link OAuthClientPlugin}.
     * @return The URL path to the icon.
     */
    public String getOAuthClientIconPath(final OAuthClientPlugin plugin) {
        if (plugin.getCustomIconPath() == null) {
            final ExternalSupplierEnum supplier =
                    ServerPluginHelper.getEnum(plugin.getProvider());
            return WebApp.getExtSupplierEnumImgUrl(supplier);
        }
        return String.format("/%s/%s", CustomWebServlet.PATH_BASE,
                plugin.getCustomIconPath());
    }

    /**
     * Creates the {@link ServerPluginManager} containing the
     * {@link ServerPlugin} objects instantiated according to the plug-in
     * property files in the plug-in directory.
     *
     * @param urls
     *            The file {@link URL}
     * @return The {@link ServerPluginManager} instance.
     */
    public static ServerPluginManager create(final URL[] urls) {

        final ServerPluginManager manager = new ServerPluginManager();

        for (final URL url : urls) {
            try (InputStream istr = url.openStream();) {
                manager.loadPlugin(istr, url.getFile());
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                continue;
            }
        }
        return manager;
    }

    /**
     * Creates the {@link ServerPluginManager} containing the
     * {@link ServerPlugin} objects instantiated according to the plug-in
     * property files in the plug-in directory.
     *
     * @param directory
     *            The directory with the plug-in property files.
     * @return The {@link ServerPluginManager} instance.
     */
    public static ServerPluginManager create(final File directory) {
        final ServerPluginManager manager = new ServerPluginManager();
        manager.loadPlugins(directory);
        return manager;
    }

    /**
     *
     * @param trx
     *            The {@link PaymentGatewayTrx} received.
     * @param status
     *            The status text.
     */
    private void logPaymentTrxReceived(final PaymentGatewayTrx trx,
            final String status) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("User [%s] Trx [%s] Amount [%.2f] [%s]",
                    trx.getUserId(), trx.getTransactionId(), trx.getAmount(),
                    status));
        }
    }

    /**
     *
     * @param trx
     *            The {@link PaymentGatewayTrx} received.
     * @param status
     *            The status text.
     */
    private void logPaymentTrxReceived(final BitcoinGatewayTrx trx,
            final String status, final String userId) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("User [%s] Trx [%s] Satoshi [%d] [%s]",
                    userId, trx.getTransactionId(), trx.getSatoshi(), status));
        }
    }

    /**
     * Publishes a payment event.
     *
     * @param level
     *            {@link PubLevelEnum}.
     * @param msg
     *            Message.
     */
    private static void publishPaymentEvent(final PubLevelEnum level,
            final String msg) {
        publishEvent(level, PubTopicEnum.PAYMENT_GATEWAY, msg);
    }

    /**
     * Publishes an event.
     *
     * @param level
     *            {@link PubLevelEnum}.
     * @param topic
     *            {@link PubTopicEnum}.
     * @param msg
     *            Message.
     */
    private static void publishEvent(final PubLevelEnum level,
            final PubTopicEnum topic, final String msg) {
        AdminPublisher.instance().publish(topic, level, msg);
    }

    /**
     * Publishes and writes event to App Log.
     *
     * @param level
     *            {@link PubLevelEnum}.
     * @param topic
     *            {@link PubTopicEnum}.
     * @param msg
     *            Message.
     */
    private static void publishAndLogEvent(final PubLevelEnum level,
            final PubTopicEnum topic, final String msg) {

        final AppLogLevelEnum logLevel;

        switch (level) {
        case ERROR:
            logLevel = AppLogLevelEnum.ERROR;
            LOGGER.error(msg);
            break;
        case WARN:
            logLevel = AppLogLevelEnum.WARN;
            LOGGER.warn(msg);
            break;
        case INFO:
            // no break intended
        default:
            LOGGER.info(msg);
            logLevel = AppLogLevelEnum.INFO;
            break;
        }

        AppLogHelper.log(logLevel, msg);
        publishEvent(level, topic, msg);
    }

    @Override
    public PaymentGatewayTrxEvent onPaymentFailed(final PaymentGatewayTrx trx) {

        publishPaymentEvent(PubLevelEnum.WARN,
                localize("payment-failed", trx.getUserId(), trx.getGatewayId(),
                        String.format("%s %.2f", CurrencyUtil.getCurrencySymbol(
                                trx.getCurrencyCode(), Locale.getDefault()),
                                trx.getAmount())));
        logPaymentTrxReceived(trx, STAT_FAILED);
        PaymentGatewayLogger.instance().onPaymentFailed(trx);

        //
        final PaymentGatewayTrxEvent trxEvent = new PaymentGatewayTrxEvent();

        trxEvent.setUserId(trx.getUserId());
        trxEvent.setBalanceUpdate(false);

        return trxEvent;
    }

    @Override
    public PaymentGatewayTrxEvent
            onPaymentCancelled(final PaymentGatewayTrx trx) {

        publishPaymentEvent(PubLevelEnum.WARN,
                localize("payment-cancelled", trx.getUserId(),
                        trx.getGatewayId(),
                        String.format("%s %.2f", CurrencyUtil.getCurrencySymbol(
                                trx.getCurrencyCode(), Locale.getDefault()),
                                trx.getAmount())));
        logPaymentTrxReceived(trx, STAT_CANCELLED);
        PaymentGatewayLogger.instance().onPaymentCancelled(trx);

        //
        final PaymentGatewayTrxEvent trxEvent = new PaymentGatewayTrxEvent();

        trxEvent.setUserId(trx.getUserId());
        trxEvent.setBalanceUpdate(false);

        return trxEvent;
    }

    @Override
    public PaymentGatewayTrxEvent
            onPaymentExpired(final PaymentGatewayTrx trx) {

        publishPaymentEvent(PubLevelEnum.WARN,
                localize("payment-expired", trx.getGatewayId(),
                        String.format("%s %.2f", CurrencyUtil.getCurrencySymbol(
                                trx.getCurrencyCode(), Locale.getDefault()),
                                trx.getAmount()),
                        trx.getUserId()));

        logPaymentTrxReceived(trx, STAT_EXPIRED);

        PaymentGatewayLogger.instance().onPaymentExpired(trx);

        //
        final PaymentGatewayTrxEvent trxEvent = new PaymentGatewayTrxEvent();

        trxEvent.setUserId(trx.getUserId());
        trxEvent.setBalanceUpdate(false);

        return trxEvent;
    }

    /**
     * Creates the Bitcoin transaction DTO.
     * <p>
     * Note: when the payment is trusted, we use the amount from the
     * {@link BitcoinGatewayTrx}, if not trusted we use amount zero.
     * </p>
     *
     * @param trx
     *            The {@link BitcoinGatewayTrx}.
     * @param userId
     *            The unique user id.
     * @param isTrusted
     *            {@code true} if the payment is trusted.
     * @return The {@link UserPaymentGatewayDto}.
     */
    private static UserPaymentGatewayDto createDtoFromTrx(
            final BitcoinGatewayTrx trx, final String userId,
            final boolean isTrusted) {

        final UserPaymentGatewayDto dto = new UserPaymentGatewayDto();

        dto.setConfirmations(trx.getConfirmations());
        dto.setCurrencyCode(trx.getExchangeCurrencyCode());
        dto.setExchangeRate(BigDecimal.valueOf(trx.getExchangeRate()));
        dto.setGatewayId(trx.getGatewayId());
        dto.setPaymentMethod(PaymentMethodEnum.BITCOIN.toString());
        dto.setPaymentMethodAddress(trx.getTransactionAddress());
        dto.setPaymentMethodAmount(
                BigDecimal.valueOf(trx.getSatoshi()).divide(SATOSHIS_IN_BTC));
        dto.setPaymentMethodCurrency(CurrencyUtil.CURRENCY_CODE_BITCOIN);
        dto.setPaymentMethodDetails(null);
        dto.setPaymentMethodFee(BigDecimal.ZERO);
        dto.setTransactionId(trx.getTransactionId());
        dto.setUserId(userId);

        dto.setAmount(dto.getPaymentMethodAmount()
                .multiply(BigDecimal.valueOf(trx.getExchangeRate())));

        if (isTrusted) {
            dto.setAmountAcknowledged(dto.getAmount());
        } else {
            dto.setAmountAcknowledged(BigDecimal.ZERO);
        }

        dto.setComment(null);
        dto.setPaymentMethodOther(null);

        return dto;
    }

    @Override
    public PaymentGatewayTrxEvent onPaymentConfirmed(
            final BitcoinGatewayTrx trx) throws PaymentGatewayException {

        final DaoContext daoCtx = ServiceContext.getDaoContext();

        final UserAttrDao userAttrDao = daoCtx.getUserAttrDao();
        final AccountTrxDao accountTrxDao = daoCtx.getAccountTrxDao();

        /*
         * Find and lock User.
         */
        final User trxUser;

        /*
         * Find User by Bitcoin ADDRESS in UserAttr.
         */
        final UserAttr attr = userAttrDao.findByNameValue(
                UserAttrEnum.BITCOIN_PAYMENT_ADDRESS,
                trx.getTransactionAddress());

        /*
         * Find User by Bitcoin TRANSACTION in AccountTrx.
         */
        final AccountTrx accountTrx =
                accountTrxDao.findByExtId(trx.getTransactionId());

        if (accountTrx != null) {

            trxUser = accountTrx.getAccount().getMembers().get(0).getUser();

        } else if (attr != null) {

            trxUser = attr.getUser();

        } else {

            /*
             * Bitcoin address and transactions are NOT found in our database.
             *
             * Possible reasons:
             *
             * (1) User reused a bitcoin address, that was issued earlier, to
             * make an extra payment ...
             *
             * (2) An outgoing payment was done from the Wallet and an earlier
             * incoming payment address was used to pay. NOTE: negative satoshi
             * amounts will (probably) be ignored by the Bitcoin Gateway plug-in
             * and not be notified here, but positive satoshi artifacts of
             * payments from the Wallet WILL be notified.
             */

            /*
             * Prepare message and throw exception.
             */
            final List<AccountTrx> list = accountTrxDao
                    .findByExtMethodAddress(trx.getTransactionAddress());

            if (list.isEmpty()) {

                trxUser = null;

            } else {

                final List<UserAccount> userAccounts =
                        list.get(0).getAccount().getMembers();

                if (userAccounts == null || userAccounts.isEmpty()) {
                    trxUser = null;
                } else {
                    trxUser = userAccounts.get(0).getUser();
                }
            }

            final StringBuilder msg = new StringBuilder(128);

            msg.append("Ignored unknown bitcoin transaction ")
                    .append(trx.getTransactionId());

            if (trxUser == null) {
                msg.append(trx.getTransactionId()).append(" and adress ")
                        .append(trx.getTransactionAddress());
            } else {
                msg.append(trx.getTransactionId()).append(" because adress ")
                        .append(trx.getTransactionAddress())
                        .append(" is already consumed by user \"")
                        .append(trxUser.getUserId())
                        .append("\" (this might be the result of "
                                + "a payment from the wallet, "
                                + "or an extra payment send by the user)");
            }

            msg.append(": ").append(trx.getSatoshi()).append(" satoshi, ")
                    .append(trx.getConfirmations()).append(" confirmations.");

            throw new PaymentGatewayException(msg.toString());
        }

        /*
         * Lock User.
         */
        final User lockedUser = ServiceContext.getServiceFactory()
                .getUserService().lockUser(trxUser.getId());

        final String userId = lockedUser.getUserId();

        /*
         *
         */
        final boolean isBalanceUpdate;

        /*
         * Note: when the trx is acknowledged, we create the DTO as trusted. If
         * acknowledgment is configured at zero (0) confirmations, it is quick
         * and will result in an almost immediate re-charge of user balance.
         */
        final UserPaymentGatewayDto dto =
                createDtoFromTrx(trx, userId, trx.isAcknowledged());

        final AccountingService accountingService =
                ServiceContext.getServiceFactory().getAccountingService();

        final String baseCurrencyAmount;

        if (accountTrx == null) {

            baseCurrencyAmount = dto.getAmount().toPlainString();

            isBalanceUpdate = trx.isAcknowledged();

            if (isBalanceUpdate) {
                /*
                 * Note: orphanedPaymentAccount is irrelevant since at this
                 * point User is known.
                 */
                accountingService.acceptFundsFromGateway(lockedUser, dto, null);

            } else {
                accountingService.createPendingFundsFromGateway(lockedUser,
                        dto);
            }

        } else {

            final boolean isAmountPending =
                    accountTrx.getAmount().compareTo(BigDecimal.ZERO) == 0;

            isBalanceUpdate = trx.isAcknowledged() && isAmountPending;

            try {

                if (isBalanceUpdate) {

                    baseCurrencyAmount = dto.getAmount().toPlainString();

                    accountingService.acceptPendingFundsFromGateway(accountTrx,
                            dto);

                } else {

                    baseCurrencyAmount = accountTrx.getAmount().toPlainString();

                    accountTrx.setExtConfirmations(dto.getConfirmations());
                    accountTrxDao.update(accountTrx);

                }

            } catch (AccountingException e) {
                throw new PaymentGatewayException(e.getMessage(), e);
            }
        }

        /*
         * Delete used Bitcoin address from the UserAttr.
         */
        if (attr != null) {
            userAttrDao.delete(attr);
        }

        /*
         * Notifications and logging.
         */

        PaymentGatewayLogger.instance().onPaymentConfirmed(trx);

        final String statusMsg;
        final String msgKey;

        final PubLevelEnum pubLevel;

        if (trx.isTrusted()) {
            statusMsg = STAT_TRUSTED;
            msgKey = "payment-status-trusted";
            pubLevel = PubLevelEnum.CLEAR;
        } else if (trx.isAcknowledged()) {
            statusMsg = STAT_ACKNOWLEDGED;
            msgKey = "payment-status-acknowledged";
            pubLevel = PubLevelEnum.CLEAR;
        } else {
            statusMsg = STAT_CONFIRMED;
            msgKey = "payment-status-pending";
            pubLevel = PubLevelEnum.WARN;
        }

        publishPaymentEvent(pubLevel, localize("payment-confirmed",
                //
                String.format("%s %s (%s %s)",
                        CurrencyUtil.CURRENCY_CODE_BITCOIN,
                        dto.getPaymentMethodAmount().toPlainString(),
                        dto.getCurrencyCode(), baseCurrencyAmount),
                //
                trx.getGatewayId(),
                //
                userId,
                //
                String.valueOf(trx.getConfirmations()),
                //
                localize(msgKey)
        //
        ));

        logPaymentTrxReceived(trx, statusMsg, userId);

        //
        final PaymentGatewayTrxEvent trxEvent = new PaymentGatewayTrxEvent();

        trxEvent.setUserId(userId);
        trxEvent.setBalanceUpdate(isBalanceUpdate);

        return trxEvent;
    }

    @Override
    public PaymentGatewayTrxEvent
            onPaymentPending(final PaymentGatewayTrx trx) {

        logPaymentTrxReceived(trx, STAT_CONFIRMED);
        publishPaymentEvent(PubLevelEnum.INFO,
                localize("payment-confirmed", String.format("%s %.2f",
                        CurrencyUtil.getCurrencySymbol(trx.getCurrencyCode(),
                                Locale.getDefault()),
                        trx.getAmount()), trx.getGatewayId(), trx.getUserId(),
                        String.valueOf(trx.getConfirmations())));

        PaymentGatewayLogger.instance().onPaymentPending(trx);

        //
        final PaymentGatewayTrxEvent trxEvent = new PaymentGatewayTrxEvent();

        trxEvent.setUserId(trx.getUserId());
        trxEvent.setBalanceUpdate(false);

        return trxEvent;
    }

    @Override
    public void onPaymentCommitted(final PaymentGatewayTrxEvent event)
            throws PaymentGatewayException {

        if (event.isBalanceUpdate()
                && StringUtils.isNotBlank(event.getUserId())) {
            UserMsgIndicator.notifyAccountInfoEvent(event.getUserId());
        }
    }

    @Override
    public PaymentGatewayTrxEvent onPaymentAcknowledged(
            final PaymentGatewayTrx trx) throws PaymentGatewayException {

        logPaymentTrxReceived(trx, STAT_ACKNOWLEDGED);

        final UserPaymentGatewayDto dto = new UserPaymentGatewayDto();

        /*
         * INVARIANT: currency code must be identical to App Currency.
         */
        if (!ConfigManager.getAppCurrency().getCurrencyCode()
                .equals(trx.getCurrencyCode())) {
            throw new PaymentGatewayException(
                    String.format("Currency code %s is not supported.",
                            trx.getCurrencyCode()));
        }

        dto.setCurrencyCode(trx.getCurrencyCode());

        dto.setPaymentMethodCurrency(trx.getExchangeCurrencyCode());
        dto.setExchangeRate(trx.getExchangeRate());
        dto.setPaymentMethodFee(trx.getFee());

        /*
         * Calculate amount in App Currency and acknowledge.
         */
        dto.setAmount(trx.getAmount().subtract(trx.getFee())
                .divide(trx.getExchangeRate()));

        dto.setAmountAcknowledged(dto.getAmount());

        /*
         * Trx identifications.
         */
        dto.setUserId(trx.getUserId());
        dto.setGatewayId(trx.getGatewayId());
        dto.setTransactionId(trx.getTransactionId());
        dto.setPaymentMethod(trx.getPaymentMethod().toString());
        dto.setPaymentMethodAddress(trx.getTransactionAccount());

        /*
         * Other...
         */
        dto.setComment(trx.getComment());
        dto.setPaymentMethodAmount(trx.getAmount());
        dto.setPaymentMethodDetails(trx.getDetails());
        dto.setPaymentMethodOther(trx.getPaymentMethodOther());

        /*
         * INVARIANT: User must exist.
         */
        final User lockedUser = ServiceContext.getServiceFactory()
                .getUserService().lockByUserId(dto.getUserId());

        final String formattedAmount = String.format("%s %.2f", CurrencyUtil
                .getCurrencySymbol(trx.getCurrencyCode(), Locale.getDefault()),
                trx.getAmount());

        if (lockedUser == null) {
            throw new PaymentGatewayException(this.localize(
                    "payment-ignored-user-unknown", trx.getGatewayId(),
                    trx.getTransactionId(), formattedAmount, trx.getUserId()));
        }

        final AccountingService service =
                ServiceContext.getServiceFactory().getAccountingService();

        /*
         * Note: orphanedPaymentAccount is irrelevant since at this point User
         * is known.
         */
        service.acceptFundsFromGateway(lockedUser, dto, null);

        publishPaymentEvent(PubLevelEnum.CLEAR, localize("payment-acknowledged",
                formattedAmount, trx.getGatewayId(), trx.getUserId()));

        PaymentGatewayLogger.instance().onPaymentAcknowledged(trx);

        //
        final PaymentGatewayTrxEvent trxEvent = new PaymentGatewayTrxEvent();

        trxEvent.setUserId(trx.getUserId());
        trxEvent.setBalanceUpdate(true);

        return trxEvent;
    }

    @Override
    public PaymentGatewayTrxEvent
            onPaymentRefunded(final PaymentGatewayTrx trx) {
        throw new UnsupportedOperationException("not supported yet");
    }

    /**
     *
     * @return String for logging signatures of loaded plug-ins.
     */
    public String asLoggingInfo() {

        final String appCurrencyCode = ConfigManager.getAppCurrencyCode();

        final StringBuilder builder = new StringBuilder();
        final String delim = "+----------------------------"
                + "--------------------------------------------+";

        int pluginsWlk = 0;

        builder.append("Loaded plugins [").append(this.allPlugins.size())
                .append("]");

        if (!this.oauthClientPlugins.isEmpty()) {

            if (pluginsWlk == 0) {
                builder.append('\n').append(delim);
            }

            for (final Map<String, OAuthClientPlugin> pluginMap : //
            this.oauthClientPlugins.values()) {
                for (final OAuthClientPlugin plugin : pluginMap.values()) {
                    builder.append("\n| ").append(
                            String.format("[%s]", plugin.getClass().getName()));
                    if (plugin.getInstanceId() != null) {
                        builder.append(
                                String.format(" [%s]", plugin.getInstanceId()));
                    }
                    builder.append("\n| Authorization: ")
                            .append(plugin.getAuthorizationUrl());
                    builder.append('\n').append(delim);
                }
            }
            pluginsWlk++;
        }

        if (!this.paymentPlugins.isEmpty()) {

            if (pluginsWlk == 0) {
                builder.append('\n').append(delim);
            }

            for (final Entry<String, PaymentGatewayPlugin> entry : //
            this.paymentPlugins.entrySet()) {

                builder.append("\n| ").append(String.format("[%s]",
                        entry.getValue().getClass().getName()));

                builder.append(" [");

                if (StringUtils.isBlank(appCurrencyCode)) {
                    builder.append("no application currency");
                } else {
                    builder.append(appCurrencyCode);

                    if (!entry.getValue()
                            .isCurrencySupported(appCurrencyCode)) {
                        builder.append(" not supported");
                    }
                }

                builder.append("]");

                try {
                    builder.append("\n| CallBack: ")
                            .append(getCallBackUrl(entry.getValue()));
                } catch (MalformedURLException e) {
                    builder.append("\n| ").append(e.getClass().getSimpleName());
                }
                builder.append('\n').append(delim);

            }
            pluginsWlk++;
        }

        if (!this.notificationPlugins.isEmpty()) {
            if (pluginsWlk == 0) {
                builder.append('\n').append(delim);
            }

            for (final Entry<String, NotificationPlugin> entry : //
            this.notificationPlugins.entrySet()) {
                builder.append("\n| ").append(String.format("[%s]",
                        entry.getValue().getClass().getName()));
                builder.append("\n| ").append(entry.getValue().getName());
                builder.append('\n').append(delim);
            }
            pluginsWlk++;
        }

        if (!this.ippRoutingPlugins.isEmpty()) {
            if (pluginsWlk == 0) {
                builder.append('\n').append(delim);
            }

            for (final Entry<String, IppRoutingPlugin> entry : //
            this.ippRoutingPlugins.entrySet()) {
                builder.append("\n| ").append(String.format("[%s]",
                        entry.getValue().getClass().getName()));
                builder.append("\n| ").append(entry.getValue().getName());
                builder.append('\n').append(delim);
            }
            pluginsWlk++;
        }

        return builder.toString();
    }

    @Override
    public void onPluginException(final ServerPlugin plugin,
            final IOException ex) {
        publishAndLogEvent(PubLevelEnum.ERROR, PubTopicEnum.PLUGIN,
                String.format("%s in plug-in [%s]: %s",
                        ex.getClass().getSimpleName(), plugin.getId(),
                        ex.getMessage()));
    }

    /**
     * Starts all plug-ins.
     */
    public void start() {
        for (final ServerPlugin plugin : this.allPlugins) {
            try {
                plugin.onStart();
            } catch (ServerPluginException e) {
                publishAndLogEvent(PubLevelEnum.ERROR, PubTopicEnum.PLUGIN,
                        e.getMessage());
            }
        }
    }

    /**
     * Stops all plug-ins.
     */
    public void stop() {
        for (final ServerPlugin plugin : this.allPlugins) {
            try {
                SpInfo.instance().log(String.format(
                        "Shutting down [%s] plug-in ...", plugin.getName()));
                plugin.onStop();
                SpInfo.instance()
                        .log(String.format(
                                "... [%s] plug-in shutdown completed.",
                                plugin.getName()));
            } catch (ServerPluginException e) {
                publishAndLogEvent(PubLevelEnum.ERROR, PubTopicEnum.PLUGIN,
                        e.getMessage());
            }
        }
    }

    /**
     * Gets the {@link URL} of the User Web App used by a Web API to redirect to
     * after remote Web App dialog is done.
     *
     * @param dfaultUrl
     *            De default URL.
     * @return The {@link URL}.
     * @throws MalformedURLException
     *             When format of the URL is invalid.
     */
    public static URL getRedirectUrl(final String dfaultUrl)
            throws MalformedURLException {

        String urlValue = ConfigManager.instance().getConfigValue(
                IConfigProp.Key.EXT_WEBAPI_REDIRECT_URL_WEBAPP_USER);

        if (StringUtils.isBlank(urlValue)) {
            urlValue = dfaultUrl;
        }

        return new URL(urlValue);
    }

    /**
     * Gets the callback {@link URL} for a {@link PaymentGatewayPlugin}.
     *
     * @param plugin
     *            The {@link PaymentGatewayPlugin}.
     * @return The callback {@link URL}.
     * @throws MalformedURLException
     *             When format of the URL is invalid.
     */
    public static URL getCallBackUrl(final PaymentGatewayPlugin plugin)
            throws MalformedURLException {
        return CallbackServlet.getCallBackUrl(plugin);
    }

    /**
     * Return a localized message string using the default locale of the
     * application.
     *
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    private String localize(final String key, final String... args) {
        return Messages.getMessage(getClass(), ConfigManager.getDefaultLocale(),
                key, args);
    }

    /**
     * @return {@code true} when one or more {@link NotificationPlugin}
     *         listeners are present.
     */
    public boolean hasNotificationListener() {
        return !this.notificationPlugins.isEmpty();
    }

    @Override
    public void onIppRoutingEvent(final IppRoutingContext ctx,
            final IppRoutingResult res) {
        this.ippRoutingPlugins.forEach((k, v) -> {
            v.onRouting(ctx, res);
        });
    }

    @Override
    public void onJobTicketEvent(final JobTicketCancelEvent event) {
        final NotificationEventProgressImpl progress =
                new NotificationEventProgressImpl();
        this.notificationPlugins.forEach((k, v) -> {
            if (v.onJobTicketEvent(event, progress)) {
                progress.onAccept(v);
            } else {
                progress.onReject(v);
            }
        });
    }

    @Override
    public void onJobTicketEvent(final JobTicketCloseEvent event) {
        final NotificationEventProgressImpl progress =
                new NotificationEventProgressImpl();
        this.notificationPlugins.forEach((k, v) -> {
            if (v.onJobTicketEvent(event, progress)) {
                progress.onAccept(v);
            } else {
                progress.onReject(v);
            }
        });
    }

    @Override
    public File getPluginHome() {
        return ConfigManager.getServerExtHome();
    }

    @Override
    public boolean isUserInGroup(final String groupName, final String userId) {
        return ServiceContext.getDaoContext().getUserGroupMemberDao()
                .isUserInGroup(groupName, userId);
    }

    @Override
    public RestClient createRestClient(final URI uri) {
        return RestClientImpl.create(uri);
    }

    @Override
    public RestClient createRestClient(final URI uri, final String username,
            final String password) {
        return RestClientImpl.create(uri, username, password);
    }

}
