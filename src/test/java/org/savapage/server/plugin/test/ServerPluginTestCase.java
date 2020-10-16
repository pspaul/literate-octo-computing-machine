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
package org.savapage.server.plugin.test;

import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.server.ext.ServerPluginManager;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ServerPluginTestCase {

    /**
     * Set up a simple log4j configuration that logs on the console.
     */
    @BeforeClass
    public static void initTest() {
        BasicConfigurator.configure();
    }

    /**
     * Testing plugin loading.
     */
    @Test
    public void pluginTest() {

        final String[] fileNames = new String[] { "test-plugin.properties" };
        final URL[] urls = new URL[fileNames.length];

        for (int i = 0; i < fileNames.length; i++) {
            urls[i] = this.getClass()
                    .getResource(String.format("/plugins/%s", fileNames[i]));
        }

        final ServerPluginManager manager = ServerPluginManager.create(urls);

        final Map<String, PaymentGatewayPlugin> plugins =
                manager.getPaymentPlugins();

        Assert.assertTrue("All plugins must be created", plugins.size() == 1);

        for (final Entry<String, PaymentGatewayPlugin> entry : plugins
                .entrySet()) {
            Assert.assertTrue("Plugin instance must be the right class",
                    (entry.getValue() instanceof TestPlugin));
        }

    }
}
