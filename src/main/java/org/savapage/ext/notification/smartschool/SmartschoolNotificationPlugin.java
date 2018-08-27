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
package org.savapage.ext.notification.smartschool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.savapage.core.SpException;
import org.savapage.core.template.dto.TemplateJobTicketDto;
import org.savapage.core.template.dto.TemplateUserDto;
import org.savapage.ext.ServerPluginContext;
import org.savapage.ext.ServerPluginException;
import org.savapage.ext.notification.JobTicketCancelEvent;
import org.savapage.ext.notification.JobTicketCloseEvent;
import org.savapage.ext.notification.JobTicketEvent;
import org.savapage.ext.notification.NotificationPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SmartschoolNotificationPlugin extends NotificationPlugin {

    /**
     * The {@link Logger}.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SmartschoolNotificationPlugin.class);

    /** */
    private static final String PROP_ACCOUNT = "smartschool.account";

    /** */
    private static final String PROP_PASSWORD = "smartschool.api.password";

    /** */
    private static final String ENDPOINT_STRING_FORMAT =
            "https://%s.smartschool.be/Webservices/V3";

    /** */
    private static final String QNAME_SEND_MSG = "sendMsg";

    /** */
    private String id;

    /** */
    private String name;

    /** */
    private SOAPConnection connection;

    /**
     * SOAP end-point as {@link URL}.
     */
    private URL endpointUrl;

    /**
     * The secret access code for sendMsg.
     */
    private char[] accesscode;

    /** */
    private ServerPluginContext plugInContext;

    /** */
    private final String jobTicketCancelResource = "JobTicketCancelEvent";
    /** */
    private final String jobTicketCloseResource = "JobTicketCloseEvent";

    /**
     * Events to send.
     */
    private final BlockingQueue<SmartschoolMsg> eventQueue =
            new LinkedBlockingQueue<>();

    /** */
    private EventConsumer eventConsumer;

    /**
     * {@code true} when stop is requested.
     */
    private volatile boolean requestStop = false;

    /** */
    class SmartschoolMsg {

        private String sender;
        private String recipient;
        private String title;
        private String body;

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public String getRecipient() {
            return recipient;
        }

        public void setRecipient(String recipient) {
            this.recipient = recipient;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

    }

    /** */
    class EventConsumer extends Thread {

        /** */
        private final SmartschoolNotificationPlugin parent;

        /** */
        private static final long POLL_SECS_TIMEOUT = 2L;

        /** */
        public static final long MAX_MSECS_PROCESSING_AFTER_STOP = 20 * 1000;

        /**
         *
         * @param plugin
         *            The parent plug-in.
         */
        EventConsumer(final SmartschoolNotificationPlugin plugin) {
            super("SmartschoolEventConsumer");
            this.parent = plugin;
        }

        @Override
        public void run() {

            try {
                while (true) {

                    final SmartschoolMsg msg = parent.eventQueue
                            .poll(POLL_SECS_TIMEOUT, TimeUnit.SECONDS);

                    if (msg == null && parent.requestStop) {
                        break;
                    }

                    if (msg != null) {
                        try {
                            parent.sendMsg(msg);
                        } catch (SOAPException e) {
                            LOGGER.error("sendMsg failed:{}", e.getMessage());
                        }
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
            }
        }
    }

    /**
     * Gets a string representation of a {@link SOAPMessage} for debugging
     * purposes.
     *
     * @param msg
     *            The message
     * @return The XML string.
     */
    private static String getXmlFromSOAPMessage(final SOAPMessage msg) {
        final ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        try {
            msg.writeTo(byteArrayOS);
        } catch (SOAPException | IOException e) {
            throw new SpException(e.getMessage());
        }
        return new String(byteArrayOS.toByteArray());
    }

    /**
     * Sends a SOAP message.
     *
     * @param msg
     *            The message.
     * @throws SOAPException
     *             When SOAP (connection) error.
     */
    private void sendMsg(final SmartschoolMsg msg) throws SOAPException {

        final SOAPMessage message =
                MessageFactory.newInstance().createMessage();

        final SOAPHeader header = message.getSOAPHeader();
        header.detachNode();

        final SOAPBody body = message.getSOAPBody();
        final QName bodyName = new QName(QNAME_SEND_MSG);
        final SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

        bodyElement.addChildElement("accesscode")
                .addTextNode(String.valueOf(this.accesscode));
        bodyElement.addChildElement("senderIdentifier")
                .addTextNode(msg.getSender());
        bodyElement.addChildElement("userIdentifier")
                .addTextNode(msg.getRecipient());
        bodyElement.addChildElement("title").addTextNode(msg.getTitle());
        bodyElement.addChildElement("body").addTextNode(msg.getBody());

        //
        final SOAPMessage response =
                this.connection.call(message, this.endpointUrl);

        if (response == null) {
            throw new SOAPException(String.format(
                    "Smartschool [%s] response is null.", QNAME_SEND_MSG));
        }

        final SOAPBody responseBody = response.getSOAPBody();
        final SOAPBodyElement responseElement =
                (SOAPBodyElement) responseBody.getChildElements().next();
        final SOAPElement returnElement =
                (SOAPElement) responseElement.getChildElements().next();

        if (responseBody.getFault() != null) {
            throw new SOAPException(returnElement.getValue() + " "
                    + responseBody.getFault().getFaultString());
        }

        // Unexpected response
        if (!NumberUtils.isDigits(returnElement.getValue())) {
            LOGGER.error(getXmlFromSOAPMessage(response));
            return;
        }

        // Logical error?
        final int returnCode = Integer.valueOf(returnElement.getValue());

        if (returnCode == 0) {
            LOGGER.trace("{} from [{}] to [{}]", QNAME_SEND_MSG,
                    msg.getSender(), msg.getRecipient());
        } else {
            LOGGER.warn("{} from [{}] to [{}]: return [{}]", QNAME_SEND_MSG,
                    msg.getSender(), msg.getRecipient(), returnCode);
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void onStart() throws ServerPluginException {

        this.requestStop = false;

        try {
            this.connection =
                    SOAPConnectionFactory.newInstance().createConnection();

            this.eventConsumer = new EventConsumer(this);
            this.eventConsumer.start();

        } catch (UnsupportedOperationException | SOAPException e) {
            throw new ServerPluginException(e.getMessage());
        }
    }

    @Override
    public void onStop() throws ServerPluginException {

        this.requestStop = true;

        try {
            this.eventConsumer
                    .join(EventConsumer.MAX_MSECS_PROCESSING_AFTER_STOP);
        } catch (InterruptedException e) {
            LOGGER.warn("EventConsumer interrupted.");
        }

        try {
            this.connection.close();
        } catch (SOAPException e) {
            LOGGER.warn("Error closing SOAP connection: " + e.getMessage());
        }
    }

    @Override
    public void onInit(final String pluginId, final String pluginName,
            final boolean live, final boolean online, final Properties props,
            final ServerPluginContext context) throws ServerPluginException {

        this.id = pluginId;
        this.name = pluginName;
        this.plugInContext = context;

        try {
            this.endpointUrl = new URL(String.format(ENDPOINT_STRING_FORMAT,
                    props.getProperty(PROP_ACCOUNT)));
        } catch (MalformedURLException e) {
            throw new ServerPluginException(e.getMessage());
        }

        this.accesscode = props.getProperty(PROP_PASSWORD).toCharArray();
    }

    /**
     *
     * @param event
     * @return
     */
    private static TemplateJobTicketDto
            createTemplateJobTicketDto(final JobTicketEvent event) {

        final TemplateJobTicketDto dto = new TemplateJobTicketDto();

        dto.setName(event.getDocumentName());
        dto.setNumber(event.getTicketNumber());
        dto.setOperator(event.getOperatorName());

        return dto;
    }

    /**
     *
     * @param event
     * @return
     */
    private static TemplateUserDto
            createTemplateUserDto(final JobTicketEvent event) {

        final TemplateUserDto dto = new TemplateUserDto();

        dto.setFullName(event.getCreatorName());

        return dto;
    }

    /**
     * Put message on queue.
     *
     * @param msg
     *            The message.
     */
    private void putMsg(final SmartschoolMsg msg) {
        try {
            this.eventQueue.put(msg);
        } catch (InterruptedException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    /**
     *
     * @param event
     * @param ticketDto
     * @param userDto
     * @param resourceName
     *            The name of the XML resource without the locale suffix and
     *            file extension.
     */
    private void putMsg(final JobTicketEvent event,
            final TemplateJobTicketDto ticketDto, final TemplateUserDto userDto,
            final String resourceName) {

        final SmartschoolMsg msg = new SmartschoolMsg();

        msg.setRecipient(event.getCreator());
        msg.setSender(event.getOperator());

        final JobTicketEventTemplate tmp = new JobTicketEventTemplate(
                this.plugInContext.getPluginHome(), ticketDto, userDto);

        final JobTicketEventMessage eventMsg =
                tmp.render(resourceName, true, event.getLocale());

        msg.setTitle(eventMsg.getTitle());
        msg.setBody(eventMsg.getBody());

        this.putMsg(msg);
    }

    @Override
    public void onJobTicketEvent(final JobTicketCancelEvent event) {

        final TemplateJobTicketDto ticketDto =
                createTemplateJobTicketDto(event);

        ticketDto.setReturnMessage(
                StringUtils.defaultString(event.getReason(), "-"));

        this.putMsg(event, ticketDto, createTemplateUserDto(event),
                this.jobTicketCancelResource);
    }

    @Override
    public void onJobTicketEvent(final JobTicketCloseEvent event) {

        this.putMsg(event, createTemplateJobTicketDto(event),
                createTemplateUserDto(event), this.jobTicketCloseResource);
    }

}
