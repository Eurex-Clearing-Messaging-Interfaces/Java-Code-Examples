package com.deutscheboerse.amqp.examples;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
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

    public BroadcastReceiver(String[] args)
    {
    }
    
    public void run() throws JMSException
    {
        /*
         * Step 1: Initializing the context based on the properties file we prepared
         */
        Listener listener = new Listener();
        Properties properties = new Properties();
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
            LOGGER.error("Unable to read configuration from file", e);
        }
        catch (IOException e)
        {
            LOGGER.error("Unable to read configuration from file", e);
        }
        catch (JMSException | NamingException | InterruptedException e)
        {
            LOGGER.error("Unable to proceed with broadcast receiver", e);
        }
        finally
        {
            /*
             * Step 6: Closing the connection
             */
            if (broadcastConsumer != null)
            {
                LOGGER.info("Closing consumer");
                broadcastConsumer.close();
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
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver(args);
        broadcastReceiver.run();
    }
}
