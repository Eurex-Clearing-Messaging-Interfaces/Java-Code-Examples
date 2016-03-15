package com.deutscheboerse.amqp.examples;

import java.util.Properties;
import java.util.UUID;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request Sender
 * Sends request to the request exchange
 */
public class RequestResponse
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponse.class);

    private final int timeoutInMillis;
    private InitialContext context;

    public RequestResponse(Options options)
    {
        this.timeoutInMillis = options.getTimeoutInMillis();
        try
        {
            Properties properties = new Properties();
            properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jndi.PropertiesFileInitialContextFactory");
            properties.setProperty("connectionfactory.connection", String.format(
                    "amqp://:@App1/?brokerlist='tcp://%s:%d?ssl='true'&trust_store='%s'&trust_store_password='%s'&key_store='%s'&key_store_password='%s'&sasl_mechs='EXTERNAL'&ssl_cert_alias='%s''",
                    options.getHostname(),
                    options.getPort(),
                    options.getTruststoreFileName(),
                    options.getTruststorePassword(),
                    options.getKeystoreFileName(),
                    options.getKeystorePassword(),
                    options.getCertificateAlias()));
            properties.setProperty("destination.requestAddress", String.format(
                    "request.%s; { node: { type: topic }, create: never }",
                    options.getAccountName()));
            properties.setProperty("destination.replyAddress", String.format(
                    "response/response.%s.response_queue; { create: receiver, node: {type: topic } }",
                    options.getAccountName()));
            properties.setProperty("destination.responseAddress", String.format(
                    "response.%s.response_queue; {create: receiver, assert: never, node: { type: queue, x-declare: { auto-delete: true, exclusive: false, arguments: {'qpid.policy_type': ring, 'qpid.max_count': 1000, 'qpid.max_size': 1000000}}, x-bindings: [{exchange: 'response', queue: 'response.%s.response_queue', key: 'response.%s.response_queue'}]}}",
                    options.getAccountName(), options.getAccountName(), options.getAccountName()));
            this.context = new InitialContext(properties);
        }
        catch (NamingException ex)
        {
            LOGGER.error("Unable to proceed with request response", ex);
        }
    }

    public void run() throws JMSException, NamingException
    {
        /*
        * Step 1: Initializing the context based on the properties file we prepared
        */
        Listener listener = new Listener();
        Connection connection = null;
        Session session = null;
        MessageProducer requestProducer = null;
        MessageConsumer responseConsumer = null;

        try
        {
            /*
            * Step 2: Preparing the connection and session
            */
            ConnectionFactory fact = (ConnectionFactory) context.lookup("connection");
            connection = fact.createConnection();
            connection.setExceptionListener(listener);
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            /*
            * Step 3: Creating a producer and consumer
            */
            Destination requestDestination = (Destination) context.lookup("requestAddress");
            requestProducer = session.createProducer(requestDestination);

            Destination responseDest = (Destination) context.lookup("responseAddress");
            responseConsumer = session.createConsumer(responseDest);

            /*
            * Step 4: Starting the connection
            */
            connection.start();
            LOGGER.info("Connected");

            /*
            * Step 5: Sending a request
            */
            TextMessage message = session.createTextMessage("<FIXML>...</FIXML>");
            message.setJMSCorrelationID(UUID.randomUUID().toString());
            message.setJMSReplyTo((Destination) context.lookup("replyAddress"));

            requestProducer.send(message);

            LOGGER.info("REQUEST SENT:");
            LOGGER.info("#############");
            LOGGER.info(message.toString());
            LOGGER.info("#############");

            /*
            * Step 6: Receive response
            */
            LOGGER.info("Waiting {} seconds for reply", this.timeoutInMillis / 1000);
            Message receivedMsg = responseConsumer.receive(this.timeoutInMillis);
            if (receivedMsg != null)
            {
                LOGGER.info("RECEIVED MESSAGE:");
                LOGGER.info("#################");
                if (receivedMsg instanceof TextMessage)
                {
                    LOGGER.info("Message Text  : {}", ((TextMessage) receivedMsg).getText());
                }
                else if (receivedMsg instanceof BytesMessage)
                {
                    BytesMessage  bytesMessage = (BytesMessage) receivedMsg;
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < bytesMessage.getBodyLength(); i++)
                    {
                        builder.append((char) bytesMessage.readByte());
                    }
                    LOGGER.info("Message Text = {}", builder.toString());
                }
                else
                {
                    LOGGER.error("Unexpected message type delivered: {}", receivedMsg.toString());
                }
                LOGGER.info("Correlation ID {}", receivedMsg.getJMSCorrelationID());
                LOGGER.info("#################");
                receivedMsg.acknowledge();
            }
            else
            {
                LOGGER.error("Reply wasn't received for {} seconds", this.timeoutInMillis / 1000);
            }
        }
        catch (NamingException | JMSException e)
        {
            LOGGER.error("Unable to proceed with request responder", e);
            throw e;
        }
        finally
        {
            /*
            * Step 7: Closing the connection
            */
            if (requestProducer != null)
            {
                LOGGER.info("Closing producer");
                requestProducer.close();
            }
            if (responseConsumer != null)
            {
                LOGGER.info("Closing consumer");
                responseConsumer.close();
            }
            if (session != null)
            {
                LOGGER.info("Closing session");
                session.close();
            }
            if (connection != null)
            {
                // implicitly closes session and producers/consumers
                LOGGER.info("Closing connection");
                connection.close();
            }
        }
    }

    public static void main(String[] args) throws JMSException, NamingException
    {
        Options options = new Options.OptionsBuilder()
                .accountName("ABCFR_ABCFRALMMACC1")
                .hostname("ecag-fixml-simu1.deutsche-boerse.com")
                .port(10170)
                .keystoreFilename("ABCFR_ABCFRALMMACC1.keystore")
                .keystorePassword("123456")
                .truststoreFilename("truststore")
                .truststorePassword("123456")
                .certificateAlias("abcfr_abcfralmmacc1")
                .build();
        RequestResponse requestResponse = new RequestResponse(options);
        requestResponse.run();
    }
}

