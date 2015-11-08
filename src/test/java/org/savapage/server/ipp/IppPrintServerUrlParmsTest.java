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
package org.savapage.server.ipp;

import org.apache.wicket.request.Url;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppPrintServerUrlParmsTest {

    /**
     *
     * @param printer
     * @param user
     * @param uuid
     * @return
     */
    private static IppPrintServerUrlParms createParm(final String printer,
            final String user, final String uuid) {

        final StringBuilder url = new StringBuilder();

        url.append("https://savapage:8632");

        if (printer != null) {
            url.append("/").append(IppPrintServerUrlParms.PARM_PRINTERS)
                    .append("/").append(printer);
        }
        if (user != null) {
            url.append("/").append(IppPrintServerUrlParms.PARM_USER_NUMBER)
                    .append("/").append(user);
        }
        if (uuid != null) {
            url.append("/").append(IppPrintServerUrlParms.PARM_USER_UUID)
                    .append("/").append(uuid);
        }

        return new IppPrintServerUrlParms(Url.parse(url.toString()));
    }

    @Test
    public void test1() {

        final String printer = "internet";
        final String user = "rijk";
        final String uuid = "5c7bfb3e-83c4-11e5-a976-406186940c49";

        final IppPrintServerUrlParms parms = createParm(printer, user, uuid);

        Assert.assertTrue("parm 1", parms.getPrinter().equals(printer));
        Assert.assertTrue("parm 2", parms.getUserNumber().equals(user));
        Assert.assertTrue("parm 3", parms.getUserUuid().toString().equals(uuid));
    }

    @Test
    public void test2() {

        final String uuid = "fail";

        final IppPrintServerUrlParms parms = createParm(null, null, uuid);

        Assert.assertTrue("parm 1", parms.getPrinter().equals(""));
        Assert.assertTrue("parm 2", parms.getUserNumber() == null);
        Assert.assertTrue("parm 3", parms.getUserUuid() == null);
    }

    @Test
    public void test3() {

        final String printer = "";

        final IppPrintServerUrlParms parms = createParm(printer, null, null);

        Assert.assertTrue("parm 1", parms.getPrinter().equals(printer));
        Assert.assertTrue("parm 2", parms.getUserNumber() == null);
        Assert.assertTrue("parm 3", parms.getUserUuid() == null);
    }

    @Test
    public void test4() {

        final String printer = "public";

        final IppPrintServerUrlParms parms = createParm(printer, null, null);

        Assert.assertTrue("parm 1", parms.getPrinter().equals(printer));
        Assert.assertTrue("parm 2", parms.getUserNumber() == null);
        Assert.assertTrue("parm 3", parms.getUserUuid() == null);
    }
}
