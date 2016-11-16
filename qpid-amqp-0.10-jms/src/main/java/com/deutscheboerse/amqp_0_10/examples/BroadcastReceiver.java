package com.deutscheboerse.amqp_0_10.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

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
            properties.setProperty("destination.broadcastAddress", String.format(
                    "broadcast.%s.TradeConfirmation; { node: { type: queue }, create: never, mode: consume, assert: never }",
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
        // Enable use of TLSv1 which is disabled by default since 6.1.0
        System.setProperty("qpid.security.tls.protocolWhiteList", "TLSv1, TLSv1.1, TLSv1.2");

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
            synchronized(this)
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
                .hostname("ecag-fixml-simu1.deutsche-boerse.com")
                .port(10170)
                .keystoreFilename("ABCFR_ABCFRALMMACC1.keystore")
                .keystorePassword("123456")
                .truststoreFilename("truststore")
                .truststorePassword("123456")
                .certificateAlias("abcfr_abcfralmmacc1")
                .build();
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver(options);
        broadcastReceiver.run();
    }
}
