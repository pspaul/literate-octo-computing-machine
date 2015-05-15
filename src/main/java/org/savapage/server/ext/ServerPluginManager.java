/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.dto.UserPaymentGatewayDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.User;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.CurrencyUtil;
import org.savapage.core.util.Messages;
import org.savapage.ext.ServerPlugin;
import org.savapage.ext.payment.PaymentGatewayListener;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.ext.payment.PaymentGatewayTrx;
import org.savapage.ext.payment.PaymentMethodEnum;
import org.savapage.server.callback.CallbackServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for reading plug-in property files and then
 * instantiating each plug-in with the properties found.
 *
 * @author Datraverse B.V.
 * @since 0.9.9
 */
public final class ServerPluginManager implements PaymentGatewayListener {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ServerPluginManager.class);

    /**
     * Property key prefix.
     */
    private static final String PROP_KEY_PLUGIN_PFX = "savapage.plugin.";

    /**
     * Property key for plug-in name.
     */
    private static final String PROP_KEY_PLUGIN_NAME = PROP_KEY_PLUGIN_PFX
            + "name";

    /**
     * Property key for plug-in class name.
     */
    private static final String PROP_KEY_PLUGIN_CLASS = PROP_KEY_PLUGIN_PFX
            + "class";

    /**
     * Property key for plug-in ID.
     */
    private static final String PROP_KEY_PLUGIN_ID = PROP_KEY_PLUGIN_PFX + "id";

    /**
     * Property key for plug-in enable switch.
     */
    private static final String PROP_KEY_PLUGIN_ENABLE = PROP_KEY_PLUGIN_PFX
            + "enable";

    /**
     * Property key for plug-in live switch.
     */
    private static final String PROP_KEY_PLUGIN_LIVE = PROP_KEY_PLUGIN_PFX
            + "live";

    /**
     *
     */
    private final Map<String, PaymentGatewayPlugin> paymentPlugins =
            new HashMap<>();

    /**
     *
     * @return The {@link Map} of (@link {@link PaymentGatewayPlugin} instances
     *         by unique ID.
     */
    public Map<String, PaymentGatewayPlugin> getPaymentPlugins() {
        return paymentPlugins;
    }

    /**
     * Creates and loads the {@link ServerPlugin} as described in a
     * {@link Properties} file.
     *
     * @param istr
     *            The {@link InputStream} of the {@link Properties} file.
     * @param istrName
     *            The name used for logging.
     * @throws IOException
     *             If an error occurs when loading the properties file.
     */
    private boolean loadPlugin(final InputStream istr, final String istrName)
            throws IOException {

        // Load
        final Properties props = new Properties();
        props.load(istr);

        // Enabled?
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
            LOGGER.warn(String.format("Plugin [%s]: plugin ID missing.",
                    istrName));
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
        final boolean pluginLive =
                Boolean.parseBoolean(props.getProperty(PROP_KEY_PLUGIN_LIVE,
                        Boolean.FALSE.toString()));

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

            if (plugin instanceof PaymentGatewayPlugin) {

                pluginType = PaymentGatewayPlugin.class.getSimpleName();

                final PaymentGatewayPlugin paymentPlugin =
                        (PaymentGatewayPlugin) plugin;

                paymentPlugin.onInit(pluginId, pluginName, pluginLive, props);
                paymentPlugin.onInit(this);

                paymentPlugins.put(pluginId, paymentPlugin);

            } else {

                throw new IllegalArgumentException(String.format(
                        "Unsupported plugin type [%s]", plugin.getClass()
                                .getName()));
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("%s [%s] loaded.", pluginType,
                        istrName));
            }

        } catch (NoSuchMethodException | SecurityException
                | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            LOGGER.error(e.getMessage());
            return false;
        }

        return true;
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
            LOGGER.debug("Loading plugins from ["
                    + pluginHome.getAbsolutePath() + "]");
        }

        if (!pluginHome.isDirectory()) {
            LOGGER.error(pluginHome + " is not a directory");
            return;
        }

        final File[] files = pluginHome.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(final File dir, final String name) {
                return FilenameUtils.isExtension(name, "properties");
            }
        });

        for (final File file : files) {

            if (!file.isFile()) {
                continue;
            }

            FileInputStream fstream = null;

            try {
                fstream = new FileInputStream(file);
                loadPlugin(fstream, file.getName());
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                continue;
            } finally {
                IOUtils.closeQuietly(fstream);
            }
        }
    }

    /**
     * Gets the first generic {@link PaymentGatewayPlugin}.
     *
     * @return The {@link PaymentGatewayPlugin}, or {@code null} when not found.
     */
    public PaymentGatewayPlugin getGenericPaymentGateway() {

        for (final Entry<String, PaymentGatewayPlugin> entry : this.paymentPlugins
                .entrySet()) {
            if (entry.getValue().getExternalPaymentMethods().isEmpty()) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Gets the first {@link PaymentGatewayPlugin} with an external
     * {@link PaymentMethodEnum}.
     *
     * @param paymentMethod
     *            The {@link PaymentMethodEnum}.
     * @return The {@link PaymentGatewayPlugin}, or {@code null} when not found.
     */
    public PaymentGatewayPlugin getExternalPaymentGateway(
            final PaymentMethodEnum paymentMethod) {

        for (final Entry<String, PaymentGatewayPlugin> entry : this.paymentPlugins
                .entrySet()) {

            if (entry.getValue().getExternalPaymentMethods()
                    .contains(paymentMethod)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Gets the {@link PaymentGatewayPlugin} by its ID.
     *
     * @return The {@link PaymentGatewayPlugin}, or {@code null} when not found.
     */
    public PaymentGatewayPlugin getPaymentGateway(final String gatewayId) {
        return this.paymentPlugins.get(gatewayId);
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

            InputStream istr = null;

            try {
                istr = url.openStream();
                manager.loadPlugin(istr, url.getFile());
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                continue;
            } finally {
                IOUtils.closeQuietly(istr);
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
    private void onPaymentTrxReceived(final PaymentGatewayTrx trx,
            final String status) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("User [%s] Trx [%s] Amount [%.2f] [%s]",
                    trx.getUserId(), trx.getTransactionId(), trx.getAmount(),
                    status));
        }
    }

    /**
     *
     * @param level
     * @param msg
     */
    private static void
            publishEvent(final PubLevelEnum level, final String msg) {
        AdminPublisher.instance().publish(PubTopicEnum.PAYMENT_GATEWAY, level,
                msg);
    }

    @Override
    public void onPaymentCancelled(final PaymentGatewayTrx trx) {

        publishEvent(
                PubLevelEnum.WARN,
                localize("payment-cancelled", trx.getUserId(), trx
                        .getGatewayId(), String.format("%s%.2f", CurrencyUtil
                        .getCurrencySymbol(ConfigManager.getDefaultLocale()),
                        trx.getAmount())));
        onPaymentTrxReceived(trx, "CANCELLED");
        PaymentGatewayLogger.instance().onPaymentCancelled(trx);
    }

    @Override
    public void onPaymentExpired(final PaymentGatewayTrx trx) {

        publishEvent(
                PubLevelEnum.WARN,
                localize("payment-expired", trx.getGatewayId(), String.format(
                        "%s%.2f", CurrencyUtil.getCurrencySymbol(ConfigManager
                                .getDefaultLocale()), trx.getAmount()), trx
                        .getUserId()));

        onPaymentTrxReceived(trx, "EXPIRED");

        PaymentGatewayLogger.instance().onPaymentExpired(trx);
    }

    @Override
    public void onPaymentAcknowledged(final PaymentGatewayTrx trx) {

        onPaymentTrxReceived(trx, "ACKNOWLEDGED");

        publishEvent(
                PubLevelEnum.INFO,
                localize("payment-acknowledged", String.format("%s%.2f",
                        CurrencyUtil.getCurrencySymbol(ConfigManager
                                .getDefaultLocale()), trx.getAmount()), trx
                        .getGatewayId(), trx.getUserId()));

        PaymentGatewayLogger.instance().onPaymentAcknowledged(trx);
    }

    @Override
    public void onPaymentPaid(final PaymentGatewayTrx trx) {

        onPaymentTrxReceived(trx, "PAID");

        final UserPaymentGatewayDto dto = new UserPaymentGatewayDto();

        dto.setAmount(new BigDecimal(trx.getAmount()));
        dto.setComment(trx.getComment());
        dto.setUserId(trx.getUserId());
        dto.setGatewayId(trx.getGatewayId());
        dto.setTransactionId(trx.getTransactionId());
        dto.setPaymentMethod(trx.getPaymentMethod().toString());

        // TODO
        final Account orphanedPaymentAccount = null;

        final User lockedUser =
                ServiceContext.getDaoContext().getUserDao()
                        .lockByUserId(dto.getUserId());

        final AccountingService service =
                ServiceContext.getServiceFactory().getAccountingService();

        service.acceptFundsFromGateway(lockedUser, dto, orphanedPaymentAccount);

        publishEvent(
                PubLevelEnum.CLEAR,
                localize("payment-transferred", trx.getUserId(), String.format(
                        "%s%.2f", CurrencyUtil.getCurrencySymbol(ConfigManager
                                .getDefaultLocale()), trx.getAmount()), trx
                        .getGatewayId()));

        PaymentGatewayLogger.instance().onPaymentPaid(trx);
    }

    @Override
    public void onPaymentRefunded(final PaymentGatewayTrx trx) {
        throw new UnsupportedOperationException("not supported yet");
    }

    /**
     *
     * @return String for logging signatures of loaded plug-ins.
     */
    public String asLoggingInfo() {

        final StringBuilder builder = new StringBuilder();
        final String delim =
                "+----------------------------"
                        + "--------------------------------------------+";

        builder.append("Loaded plugins [").append(this.paymentPlugins.size())
                .append("]");

        if (!this.paymentPlugins.isEmpty()) {

            builder.append('\n').append(delim);

            for (final Entry<String, PaymentGatewayPlugin> entry : this.paymentPlugins
                    .entrySet()) {

                builder.append("\n| ").append(
                        String.format("[%s]", entry.getValue().getClass()
                                .getName()));
            }

            builder.append('\n').append(delim);
        }

        return builder.toString();
    }

    /**
     * Starts all plug-ins.
     */
    public void start() {

        for (final Entry<String, PaymentGatewayPlugin> entry : this.paymentPlugins
                .entrySet()) {
            entry.getValue().onStart();
        }
    }

    /**
     * Stops all plug-ins.
     */
    public void stop() {
        for (final Entry<String, PaymentGatewayPlugin> entry : this.paymentPlugins
                .entrySet()) {
            entry.getValue().onStop();
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

        String urlValue =
                ConfigManager.instance().getConfigValue(
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
        return Messages.getMessage(getClass(),
                ConfigManager.getDefaultLocale(), key, args);
    }

}
