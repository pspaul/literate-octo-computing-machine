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
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dto.UserPaymentGatewayDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.ServerPlugin;
import org.savapage.ext.payment.PaymentGatewayListener;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.ext.payment.PaymentGatewayTrx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for reading plug-in property files and then
 * instantiating each plug-in with the properties found.
 *
 * @author Datraverse B.V.
 *
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
     * The loaded {@link PaymentGatewayPlugin} objects.
     */
    private final ArrayList<PaymentGatewayPlugin> paymentGatewayPlugins =
            new ArrayList<PaymentGatewayPlugin>();

    /**
     *
     * @return The loaded {@link PaymentGatewayPlugin} objects.
     */
    public ArrayList<PaymentGatewayPlugin> getPaymentGatewayPlugins() {
        return paymentGatewayPlugins;
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
    private void loadPlugin(final InputStream istr, final String istrName)
            throws IOException {

        final Properties props = new Properties();
        props.load(istr);

        //
        final String pluginName = props.getProperty(PROP_KEY_PLUGIN_NAME);

        if (StringUtils.isBlank(pluginName)) {
            LOGGER.warn(String.format("Plugin [%s]: plugin name missing.",
                    istrName));
            return;
        }

        final String pluginClassName = props.getProperty(PROP_KEY_PLUGIN_CLASS);

        if (StringUtils.isBlank(pluginClassName)) {
            LOGGER.warn(String.format("Plugin [%s]: plugin class missing.",
                    istrName));
            return;
        }

        final Class<?> clazz;
        try {
            clazz = Class.forName(pluginClassName);
        } catch (ClassNotFoundException e) {
            LOGGER.error(String.format("Class %s not found", pluginClassName));
            return;
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

                paymentPlugin.onInit(props);
                paymentPlugin.onInit(this);

                paymentGatewayPlugins.add(paymentPlugin);

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
            return;
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
     * Gets the first {@link PaymentGatewayPlugin}.
     *
     * @return The {@link PaymentGatewayPlugin}, or {@code null} when not found.
     */
    public PaymentGatewayPlugin getPaymentGatewayPlugin() {

        if (this.paymentGatewayPlugins.isEmpty()) {
            return null;
        }
        return this.paymentGatewayPlugins.get(0);
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

    @Override
    public void onPaymentCancelled(final PaymentGatewayTrx trx) {
        onPaymentTrxReceived(trx, "CANCELLED");
    }

    @Override
    public void onPaymentExpired(final PaymentGatewayTrx trx) {
        onPaymentTrxReceived(trx, "EXPIRED");
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
        dto.setPaymentType(trx.getPaymentType());

        // TODO
        final Account orphanedPaymentAccount = null;

        ServiceContext.getServiceFactory().getAccountingService()
                .acceptFundsFromGateway(dto, orphanedPaymentAccount);
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

        builder.append("Loaded plugins [")
                .append(this.getPaymentGatewayPlugins().size()).append("]");

        if (!this.getPaymentGatewayPlugins().isEmpty()) {
            builder.append('\n').append(delim);

            for (final PaymentGatewayPlugin plugin : this
                    .getPaymentGatewayPlugins()) {

                builder.append("\n| ").append(
                        String.format("[%s]", plugin.getClass().getName()));
            }

            builder.append('\n').append(delim);
        }

        return builder.toString();
    }

    /**
     * Starts all plug-ins.
     */
    public void start() {
        for (final PaymentGatewayPlugin plugin : this
                .getPaymentGatewayPlugins()) {
            plugin.onStart();
        }
    }

    /**
     * Stops all plug-ins.
     */
    public void stop() {
        for (final PaymentGatewayPlugin plugin : this
                .getPaymentGatewayPlugins()) {
            plugin.onStop();
        }
    }

}
