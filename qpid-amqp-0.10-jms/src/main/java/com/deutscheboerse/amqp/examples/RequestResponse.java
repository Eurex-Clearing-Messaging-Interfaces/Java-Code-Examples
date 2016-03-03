package com.deutscheboerse.amqp.examples;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import javax.jms.*;
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
    private static final int TIMEOUT_MILLIS = 100000;
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponse.class);

    public RequestResponse(String[] args)
    {
    }

    public void run() throws JMSException
    {
        /*
         * Step 1: Initializing the context based on the properties file we prepared
         */
        Properties properties = new Properties();
        Listener listener = new Listener();
        Connection connection = null;
        Session session = null;
        MessageProducer requestProducer = null;
        MessageConsumer responseConsumer = null;

        try
        {
            properties.load(RequestResponse.class.getResourceAsStream("examples.properties"));
            InitialContext ctx = new InitialContext(properties);
            /*
             * Step 2: Preparing the connection and session
             */
            ConnectionFactory fact = (ConnectionFactory) ctx.lookup("connection");
            connection = fact.createConnection();
            connection.setExceptionListener(listener);
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            /*
             * Step 3: Creating a producer and consumer
             */
            Destination requestDestination = (Destination) ctx.lookup("requestAddress");
            requestProducer = session.createProducer(requestDestination);

            Destination responseDest = (Destination) ctx.lookup("responseAddress");
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
            message.setJMSReplyTo((Destination) ctx.lookup("replyAddress"));

            requestProducer.send(message);

            LOGGER.info("REQUEST SENT:");
            LOGGER.info("#############");
            LOGGER.info(message.toString());
            LOGGER.info("#############");

            /*
             * Step 6: Receive response
             */
            LOGGER.info("Waiting {} seconds for reply", TIMEOUT_MILLIS/1000);
            Message receivedMsg = responseConsumer.receive(TIMEOUT_MILLIS);
            if (receivedMsg != null)
            {
                LOGGER.info("RECEIVED MESSAGE:");
                LOGGER.info("#################");
                if (receivedMsg instanceof TextMessage)
                {
                    LOGGER.info("Message Text  : {}", ((TextMessage) receivedMsg).getText());   
                }
                LOGGER.info("Correlation ID {}", receivedMsg.getJMSCorrelationID());
                LOGGER.info("#################");
                receivedMsg.acknowledge();
            }
            else
            {
                LOGGER.error("Reply wasn't received for {} seconds", TIMEOUT_MILLIS/1000);
            }
        }
        catch (NamingException | JMSException | IOException e)
        {
            LOGGER.error("Unable to proceed with request responder", e);
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
    
    public static void main(String[] args) throws JMSException
    {
        RequestResponse requestResponse = new RequestResponse(args);
        requestResponse.run();
    }
}

