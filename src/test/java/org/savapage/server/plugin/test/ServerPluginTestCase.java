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
package org.savapage.server.plugin.test;

import java.net.URL;
import java.util.ArrayList;

import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.server.ext.ServerPluginManager;

/**
 *
 * @author Datraverse B.V.
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
            urls[i] =
                    this.getClass().getResource(
                            String.format("/plugins/%s", fileNames[i]));
        }

        final ServerPluginManager manager = ServerPluginManager.create(urls);

        final ArrayList<PaymentGatewayPlugin> plugins =
                manager.getPaymentGatewayPlugins();

        Assert.assertTrue("All plugins must be created", plugins.size() == 1);

        final Object plugin = plugins.get(0);

        Assert.assertTrue("Plugin instance must be the right class",
                (plugin instanceof TestPlugin));

    }
}
