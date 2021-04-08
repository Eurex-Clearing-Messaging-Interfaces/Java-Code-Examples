package com.deutscheboerse.amqp_swiftmq.examples;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastReceiver.class);
    
    private final Options options;
    private final int timeoutInMillis;

    private Listener listener;

    public BroadcastReceiver(Options options)
    {
        System.setProperty("javax.net.ssl.trustStore", options.getTruststoreFileName());
        System.setProperty("javax.net.ssl.trustStorePassword", options.getTruststorePassword());
        System.setProperty("javax.net.ssl.keyStore", options.getKeystoreFileName());
        System.setProperty("javax.net.ssl.keyStorePassword", options.getKeystorePassword());

        //System.setProperty("javax.net.debug", "ssl");
        //System.setProperty("swiftmq.amqp.debug", "true");
        //System.setProperty("swiftmq.amqp.frame.debug", "true");
        this.options = options;
        this.timeoutInMillis = options.getTimeoutInMillis();
    }
    
    public void run() throws AMQPException, UnsupportedProtocolVersionException, AuthenticationException, InterruptedException, IOException {
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
            connection = new Connection(ctx, options.getHostname(), options.getPort(), "", "");
            connection.setIdleTimeout(120000);
            connection.setMechanism("EXTERNAL");
            connection.setSocketFactory(new MySSLSocketFactory(options.getCertificateAlias()));
            
            /*
             * Step 3: Starting the connection
             */
            connection.connect();
            LOGGER.info("Connected");

            /*
             * Step 4: Creating a producer and consumer
             */
            session = connection.createSession(1000, 1000);
            responseConsumer = session.createConsumer(String.format("broadcast.%s.TradeConfirmation", options.getAccountName()),
                    1000, QoS.AT_LEAST_ONCE, true, null);
        
            /*
             * Step 5: Receiving broadcast messages using listener for timeout seconds
             */
            LOGGER.info("Receiving broadcast messages for {} seconds", timeoutInMillis/1000);
            listener = new Listener(responseConsumer);
            listener.setTimeout(timeoutInMillis);
            LOGGER.info("Finished receiving broadcast messages for {} seconds", timeoutInMillis/1000);
        }
        catch (IOException | UnsupportedProtocolVersionException | AuthenticationException | AMQPException | InterruptedException ex)
        {
            LOGGER.info("Failed to connect and create consumer or producer!", ex);
            throw ex;
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

    public int getMessagesReceivedCount()
    {
        return this.listener.getMessagesReceivedCount();
    }

    public boolean isExceptionReceived()
    {
        return this.listener.isExceptionReceived();
    }

    public static void main(String[] args) throws AMQPException, UnsupportedProtocolVersionException, AuthenticationException, InterruptedException, IOException {
        Options options = new Options.OptionsBuilder()
                .timeoutInMillis(10000)
                .accountName("ABCFR_ABCFRALMMACC1")
                .hostname("ecag-fixml-simu1.deutsche-boerse.com")
                .port(10170)
                .keystoreFilename("ABCFR_ABCFRALMMACC1.keystore")
                .keystorePassword("12345678")
                .truststoreFilename("truststore")
                .truststorePassword("12345678")
                .certificateAlias("abcfr_abcfralmmacc1")
                .build();
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver(options);
        broadcastReceiver.run();
    }
}
