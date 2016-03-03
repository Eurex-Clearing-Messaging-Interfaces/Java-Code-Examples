package com.deutscheboerse.amqp.examples;

import com.swiftmq.amqp.AMQPContext;
import com.swiftmq.amqp.v100.client.AMQPException;
import com.swiftmq.amqp.v100.client.AuthenticationException;
import com.swiftmq.amqp.v100.client.Connection;
import com.swiftmq.amqp.v100.client.Consumer;
import com.swiftmq.amqp.v100.client.QoS;
import com.swiftmq.amqp.v100.client.Session;
import com.swiftmq.amqp.v100.client.UnsupportedProtocolVersionException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Broadcast Receiver Receives broadcasts from the persistent broadcast queue
 */
public class BroadcastReceiver
{
    private static final int TIMEOUT_MILLIS = 100000;
    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastReceiver.class);
    
    public static void main(String[] args) throws AMQPException
    {
        System.setProperty("javax.net.ssl.trustStore", "truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        System.setProperty("javax.net.ssl.keyStore", "ABCFR_ABCFRALMMACC1.keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        //System.setProperty("javax.net.debug", "ssl");
        //System.setProperty("swiftmq.amqp.debug", "true");
        //System.setProperty("swiftmq.amqp.frame.debug", "true");

        /*
         * Step 1: Initializing the variables
         */
        Connection connection = null;
        Session session = null;
        Consumer responseConsumer = null;
        
        try
        {
            /*
             * Step 2: Preparing the connection and session
             */
            LOGGER.info("Creating connection");
            AMQPContext ctx = new AMQPContext(AMQPContext.CLIENT);
            connection = new Connection(ctx, "ecag-fixml-simu1.deutsche-boerse.de", 10170, "", "");
            connection.setMechanism("EXTERNAL");
            connection.setSocketFactory(new MySSLSocketFactory("abcfr_abcfralmmacc1"));
            
            /*
             * Step 3: Starting the connection
             */
            connection.connect();
            LOGGER.info("Connected");

            /*
             * Step 4: Creating a producer and consumer
             */
            session = connection.createSession(1000, 1000);
            responseConsumer = session.createConsumer("broadcast.ABCFR_ABCFRALMMACC1.TradeConfirmation", 1000, QoS.AT_LEAST_ONCE, true, null);
        
            /*
             * Step 5: Receiving broadcast messages using listener for timeout seconds
             */
            LOGGER.info("Receiving broadcast messages for {} seconds", TIMEOUT_MILLIS/1000);
            Listener listener = new Listener(responseConsumer);
            listener.setTimeout(TIMEOUT_MILLIS);
            LOGGER.info("Finished receiving broadcast messages for {} seconds", TIMEOUT_MILLIS/1000);
        }
        catch (IOException | UnsupportedProtocolVersionException | AuthenticationException | AMQPException | InterruptedException ex)
        {
            LOGGER.info("Failed to connect and create consumer or producer!", ex);
            System.exit(1);
        }
        finally
        {
            // Closing the connection
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
}
