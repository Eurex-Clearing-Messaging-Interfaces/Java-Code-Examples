package com.deutscheboerse.amqp.examples;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Broadcast Receiver
 * Receives broadcasts from the persistent broadcast queue
 */
public class BroadcastReceiver
{
    private static final int TIMEOUT_MILLIS = 100000;
    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastReceiver.class);

    public static void main(String[] args) throws JMSException
    {
        //System.setProperty("javax.net.debug", "ssl");

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss Z");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");

        /*
         * Step 1: Initializing the context based on the properties file we prepared
         */
        Properties properties = new Properties();
        Listener listener = new Listener();
        Connection connection = null;
        Session session = null;
        MessageConsumer broadcastConsumer = null;

        try
        {
            properties.load(BroadcastReceiver.class.getResourceAsStream("examples.properties"));
            InitialContext ctx = new InitialContext(properties);

            /*
             * Step 2: Preparing the connection and session
             */
            LOGGER.info("Creating connection");
            connection = ((ConnectionFactory) ctx.lookup("connection")).createConnection();
            connection.setExceptionListener(listener);
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            /*
             * Step 3: Creating a broadcast receiver / consumer
             */
            broadcastConsumer = session.createConsumer((Destination) ctx.lookup("broadcastAddress"));
            broadcastConsumer.setMessageListener(listener);

            /*
             * Step 4: Starting the connection
             */
            connection.start();
            LOGGER.info("Connected");

            /*
             * Step 5: Receiving broadcast messages using listener for timeout seconds
             */
            LOGGER.info("Receiving broadcast messages for {} seconds", TIMEOUT_MILLIS/1000);
            listener.setTimeout(TIMEOUT_MILLIS);
            LOGGER.info("Finished receiving broadcast messages for {} seconds", TIMEOUT_MILLIS/1000);
        }
        catch (FileNotFoundException e)
        {
            LOGGER.error("Unable to read configuration from file {}", e);
        }
        catch (IOException e)
        {
            LOGGER.error("Unable to read configuration from file {}", e);
        }
        catch (JMSException | NamingException | InterruptedException e)
        {
            LOGGER.error("Unable to proceed with broadcast receiver {}", e);
        }
        finally
        {
            /*
             * Step 6: Closing the connection
             */
            if (broadcastConsumer != null)
            {
                System.out.println("Closing consumer");
                broadcastConsumer.close();
            }
            if (session != null)
            {
                System.out.println("Closing session");
                session.close();
            }
            if (connection != null)
            {
                // implicitly closes session and producers/consumers
                System.out.println("Closing connection");
                connection.close();
            }
        }
    }
}
