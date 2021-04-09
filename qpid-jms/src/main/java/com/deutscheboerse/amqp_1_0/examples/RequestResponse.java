package com.deutscheboerse.amqp_1_0.examples;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.util.Properties;
import java.util.UUID;
import javax.naming.Context;
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
        //System.setProperty("javax.net.debug", "ssl");

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss Z");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");

        this.timeoutInMillis = options.getTimeoutInMillis();
        try
        {
            Properties properties = new Properties();
            properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
            properties.setProperty("connectionfactory.connection", String.format(
                    "amqps://%s:%d?transport.keyStoreLocation=%s&transport.keyStorePassword=%s&transport.trustStoreLocation=%s&transport.trustStorePassword=%s&transport.keyAlias=%s&amqp.idleTimeout=0",
                    options.getHostname(),
                    options.getPort(),
                    options.getKeystoreFileName(),
                    options.getKeystorePassword(),
                    options.getTruststoreFileName(),
                    options.getTruststorePassword(),
                    options.getCertificateAlias()));
            properties.setProperty("topic.requestAddress", String.format("request.%s", options.getAccountName()));
            properties.setProperty("queue.responseAddress", String.format("response.%s", options.getAccountName()));
            properties.setProperty("topic.replyAddress", String.format("response/response.%s", options.getAccountName()));

            this.context = new InitialContext(properties);
        }
        catch (NamingException ex)
        {
            LOGGER.error("Unable to proceed with broadcast receiver", ex);
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
            TextMessage message = session.createTextMessage("<FPML>...</FPML>");
            message.setJMSCorrelationID(UUID.randomUUID().toString());
            message.setJMSReplyTo((Destination) context.lookup("replyAddress"));

            requestProducer.send(message);

            LOGGER.info("REQUEST SENT:");
            LOGGER.info("#############");
            LOGGER.info("Correlation ID: {}", message.getJMSCorrelationID());
            LOGGER.info("Message Text  {}: ", message.getText());
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
                LOGGER.error("Reply wasn't received for {} seonds", this.timeoutInMillis / 1000);
                throw new java.lang.IllegalStateException("Reply wasn't received");
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
                .hostname("ecag-fpml-sim1")
                .port(35671)
                .keystoreFilename("ABCFR_ABCFRALMMACC1.keystore")
                .keystorePassword("12345678")
                .truststoreFilename("ecag-fpml-sim1.truststore")
                .truststorePassword("12345678")
                .certificateAlias("abcfr_abcfralmmacc1")
                .timeoutInMillis(10000)
                .build();
        RequestResponse requestResponse = new RequestResponse(options);
        requestResponse.run();
    }
}

