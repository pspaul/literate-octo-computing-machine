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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.services.ServiceContext;
import org.savapage.ext.smartschool.SmartschoolConnection;
import org.savapage.ext.smartschool.SmartschoolConstants;
import org.savapage.ext.smartschool.SmartschoolRequestEnum;
import org.savapage.ext.smartschool.services.SmartschoolProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
@WebServlet(name = "SmartschoolProxyServlet",
        urlPatterns = { SmartschoolProxyServlet.SERVLET_URL_PATTERN })
public final class SmartschoolProxyServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * The buffer size for reading.
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SmartschoolProxyServlet.class);

    private static final SmartschoolProxyService SMARTSCHOOL_PROXY =
            ServiceContext.getServiceFactory().getSmartSchoolProxyService();
    /**
     * Base path (without leading or trailing '/').
     */
    public static final String PATH_BASE = "ext/smartschool";

    /**
     * .
     */
    public static final String SERVLET_URL_PATTERN = "/" + PATH_BASE + "/*";

    /**
     * Prints a request for debugging purposes.
     *
     * @param istr
     *            The input stream.
     * @param pstr
     *            The output to print to.
     * @throws IOException
     *             When errors.
     */
    @SuppressWarnings("unused")
    private static void printRequest(final InputStream istr,
            final PrintStream pstr) throws IOException {

        final byte[] aByte = new byte[BUFFER_SIZE];
        int nBytes = istr.read(aByte);
        while (-1 < nBytes) {
            pstr.write(aByte, 0, nBytes);
            nBytes = istr.read(aByte);
        }
    }

    /**
     * Creates {@link SOAPMessage} from {@link InputStream}.
     *
     * @param istr
     * @return The {@link SOAPMessage}.
     * @throws SOAPException
     *             When SOAP message is invalid.
     * @throws IOException
     *             When problem reading input stream.
     */
    private static SOAPMessage createSoapMessage(final InputStream istr)
            throws SOAPException, IOException {
        final MessageFactory factory = MessageFactory.newInstance();
        return factory.createMessage(new MimeHeaders(), istr);
    }

    /**
     * Fills the name/value map with operation request parameters and returns
     * the operation node.
     *
     * @param soapMessageIn
     *            The input {@link SOAPMessage}.
     * @param map
     *            The {@link Map} to fill.
     * @return The SOAP operation node, or {@code null} when not present.
     * @throws SOAPException
     *             When SOAP syntax errors.
     */
    private static Node fillRequestParms(final SOAPMessage soapMessageIn,
            final Map<String, String> map) throws SOAPException {

        @SuppressWarnings("unchecked")
        final Iterator<Node> iter =
                soapMessageIn.getSOAPBody().getChildElements();

        if (!iter.hasNext()) {
            return null;
        }

        final Node operationNode = iter.next();

        final org.w3c.dom.NodeList children = operationNode.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            final org.w3c.dom.Node child = children.item(i);
            map.put(child.getNodeName().toLowerCase(), child.getTextContent());
        }

        return operationNode;
    }

    /**
     * Checks if password is valid for Smartschool account.
     *
     * @param accountName
     *            The Smartschool account name.
     * @param requestParms
     *            The SOAP request parameter name value map..
     * @return {@code true} when password is valid for account.
     */
    private static boolean checkPwd(final String accountName,
            final Map<String, String> requestParms) {
        final String pwd = requestParms.get(SmartschoolConstants.XML_ELM_PWD);
        return accountName != null && pwd != null
                && SMARTSCHOOL_PROXY.checkPwd(accountName, pwd);
    }

    /**
     *
     * @param smartschoolAccount
     * @param clusterNode
     * @param smartschoolRequest
     */
    private static void logRequest(final String smartschoolAccount,
            final String clusterNode,
            final SmartschoolRequestEnum smartschoolRequest) {

        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        final StringBuilder msg = new StringBuilder();
        msg.append("Proxy Request [");
        if (smartschoolRequest != null) {
            msg.append(smartschoolRequest.toString());
        }
        msg.append("] for Account [").append(smartschoolAccount)
                .append("] from Node [").append(clusterNode).append("]");

        LOGGER.debug(msg.toString());
    }

    @Override
    protected void doPost(final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, IOException {

        // printRequest(req.getInputStream(), System.out);

        int httpStatus = HttpServletResponse.SC_OK;

        final String smartschoolAccount =
                req.getParameter(SmartschoolConnection.PROXY_URL_PARM_ACCOUNT);

        final String clusterNode =
                req.getParameter(SmartschoolConnection.PROXY_URL_PARM_NODE);

        // Is URL ok?
        if (smartschoolAccount == null || clusterNode == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            final SOAPMessage soapMessageIn =
                    createSoapMessage(req.getInputStream());

            final Map<String, String> soapRequestParms = new HashMap<>();

            final Node operationNode =
                    fillRequestParms(soapMessageIn, soapRequestParms);

            SOAPMessage soapMessageOut = null;

            if (operationNode == null) {

                httpStatus = HttpServletResponse.SC_BAD_REQUEST;

            } else if (!checkPwd(smartschoolAccount, soapRequestParms)) {

                httpStatus = HttpServletResponse.SC_UNAUTHORIZED;

            } else {

                final SmartschoolRequestEnum smartschoolRequest =
                        SmartschoolRequestEnum
                                .fromSoapName(operationNode.getNodeName());

                if (LOGGER.isDebugEnabled()) {
                    logRequest(smartschoolAccount, clusterNode,
                            smartschoolRequest);
                }

                if (smartschoolRequest == null) {

                    httpStatus = HttpServletResponse.SC_NOT_FOUND;

                } else {
                    switch (smartschoolRequest) {

                    case GET_PRINTJOBS:

                        SMARTSCHOOL_PROXY.keepNodeAlive(clusterNode);

                        soapMessageOut = SMARTSCHOOL_PROXY.createPrintJobsRsp(
                                smartschoolAccount, clusterNode);
                        break;

                    case GET_DOCUMENT:

                        SMARTSCHOOL_PROXY.keepNodeAlive(clusterNode);

                        final String documentId = soapRequestParms
                                .get(SmartschoolConstants.XML_ELM_UID);

                        if (StringUtils.isBlank(documentId)) {
                            httpStatus = HttpServletResponse.SC_BAD_REQUEST;
                            break;
                        }
                        try {
                            SMARTSCHOOL_PROXY.streamGetDocumentRsp(
                                    smartschoolAccount, clusterNode, documentId,
                                    resp.getOutputStream());
                            resp.setContentType("application/soap+xml");
                        } catch (FileNotFoundException e) {
                            // TODO
                            httpStatus = HttpServletResponse.SC_NO_CONTENT;
                        }
                        break;

                    default:
                        httpStatus = HttpServletResponse.SC_NOT_FOUND;
                        break;
                    }
                }
            }

            if (soapMessageOut != null) {
                soapMessageOut.writeTo(resp.getOutputStream());
                resp.setContentType("application/soap+xml");
            }

        } catch (IOException | SOAPException | JAXBException e) {
            LOGGER.error(e.getMessage(), e);
            httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        resp.setStatus(httpStatus);
    }
}
