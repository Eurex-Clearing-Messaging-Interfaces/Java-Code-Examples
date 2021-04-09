package com.deutscheboerse.amqp_1_0.examples;

import java.util.Properties;

import javax.jms.*;
import javax.naming.Context;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastReceiver.class);

    private final InitialContext context;
    private final int timeoutInMillis;
    private final Listener listener = new Listener();

    public BroadcastReceiver(Options options) throws NamingException
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
            properties.setProperty("queue.broadcastAddress", String.format(
                    "broadcast.%s.TradeConfirmation",
                    options.getAccountName()));
            this.context = new InitialContext(properties);
        }
        catch (NamingException ex)
        {
            LOGGER.error("Unable to proceed with broadcast receiver", ex);
            throw ex;
        }
    }

    public void run() throws JMSException, NamingException, InterruptedException
    {
        /*
        * Step 1: Initializing the context based on the properties file we prepared
        */
        Connection connection = null;
        Session session = null;
        MessageConsumer broadcastConsumer = null;

        try
        {
            /*
            * Step 2: Preparing the connection and session
            */
            LOGGER.info("Creating connection");
            connection = ((ConnectionFactory) context.lookup("connection")).createConnection();
            connection.setExceptionListener(listener);
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            /*
            * Step 3: Creating a broadcast receiver / consumer
            */
            broadcastConsumer = session.createConsumer((Destination) context.lookup("broadcastAddress"));
            broadcastConsumer.setMessageListener(listener);

            /*
            * Step 4: Starting the connection
            */
            connection.start();
            LOGGER.info("Connected");

            /*
            * Step 5: Receiving broadcast messages using listener for timeout seconds
            */
            LOGGER.info("Receiving broadcast messages for {} seconds", this.timeoutInMillis / 1000);
            synchronized (this)
            {
                this.wait(this.timeoutInMillis);
            }
            LOGGER.info("Finished receiving broadcast messages for {} seconds", this.timeoutInMillis / 1000);
        }
        catch (JMSException | NamingException | InterruptedException e)
        {
            LOGGER.error("Unable to proceed with broadcast receiver", e);
            throw e;
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

    public int getMessagesReceivedCount()
    {
        return this.listener.getMessagesReceivedCount();
    }

    public boolean isExceptionReceived()
    {
        return this.listener.isExceptionReceived();
    }

    public static void main(String[] args) throws JMSException, NamingException, InterruptedException
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
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver(options);
        broadcastReceiver.run();
    }
}
