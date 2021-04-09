package com.deutscheboerse.amqp_swiftmq.examples;

import java.util.UUID;

import com.swiftmq.amqp.AMQPContext;
import com.swiftmq.amqp.v100.client.AMQPException;
import com.swiftmq.amqp.v100.client.AuthenticationException;
import com.swiftmq.amqp.v100.client.Connection;
import com.swiftmq.amqp.v100.client.Consumer;
import com.swiftmq.amqp.v100.client.Producer;
import com.swiftmq.amqp.v100.client.QoS;
import com.swiftmq.amqp.v100.client.Session;
import com.swiftmq.amqp.v100.client.UnsupportedProtocolVersionException;
import com.swiftmq.amqp.v100.generated.messaging.message_format.*;
import com.swiftmq.amqp.v100.messaging.AMQPMessage;
import com.swiftmq.amqp.v100.types.AMQPBinary;
import com.swiftmq.amqp.v100.types.AMQPString;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Broadcast Receiver Receives broadcasts from the persistent broadcast queue
 */
public class RequestResponse
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponse.class);

    private final Options options;
    private final int timeoutInMillis;

    public RequestResponse(Options options)
    {
        System.setProperty("javax.net.ssl.trustStore", options.getTruststoreFileName());
        System.setProperty("javax.net.ssl.trustStorePassword", options.getTruststorePassword());
        System.setProperty("javax.net.ssl.keyStore", options.getKeystoreFileName());
        System.setProperty("javax.net.ssl.keyStorePassword", options.getKeystorePassword());

//        System.setProperty("javax.net.debug", "ssl");
//        System.setProperty("swiftmq.amqp.debug", "true");
//        System.setProperty("swiftmq.amqp.frame.debug", "true");
        this.options = options;
        this.timeoutInMillis = options.getTimeoutInMillis();
    }
    
    public void run() throws AMQPException, UnsupportedProtocolVersionException, IOException, AuthenticationException {
        /*
        * Step 1: Initializing the variables
        */
        Connection connection = null;
        Session session = null;
        Producer requestProducer = null;
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
            requestProducer = session.createProducer(String.format("request.%s", options.getAccountName()),
                    QoS.AT_LEAST_ONCE);
            String correlationID = UUID.randomUUID().toString();
            responseConsumer = session.createConsumer(String.format("response.%s", options.getAccountName()),
                    1000, QoS.AT_LEAST_ONCE, true, null);
            
            /*
            * Step 5: Sending a request
            */
            AMQPMessage msg = new AMQPMessage();
            msg.setAmqpValue(new AmqpValue(new AMQPString("<FIXML>...</FIXML>")));
            Properties msgProp = new Properties();
            msgProp.setReplyTo(new AddressString(String.format("response/response.%s", options.getAccountName())));
            msgProp.setCorrelationId(new MessageIdString(correlationID));
            msg.setProperties(msgProp);
            requestProducer.send(msg);
            
            LOGGER.info("REQUEST SENT:");
            LOGGER.info("#############");
            LOGGER.info("Correlation ID: {}", msg.getProperties().getCorrelationId().getValueString());
            LOGGER.info("Message Text  : {}", msg.getAmqpValue().getValue().getValueString());
            LOGGER.info("#############");
            
            /*
            * Step 6: Receive response
            */
            LOGGER.info("Waiting {} seconds for reply", timeoutInMillis/1000);
            AMQPMessage receivedMsg = responseConsumer.receive(timeoutInMillis);
            if (receivedMsg != null)
            {
                LOGGER.info("RECEIVED MESSAGE:");
                LOGGER.info("#################");
                //LOGGER.info("Correlation ID: {}", receivedMsg.getProperties().getCorrelationId().getValueString());
                //LOGGER.info("Message Text  : {}", new String(receivedMsg.getData().get(0).getValue()));
                //LOGGER.info("#################");
                receivedMsg.accept();
            }
            else
            {
                LOGGER.error("Reply wasn't received for {} seconds", timeoutInMillis/1000);
                throw new java.lang.IllegalStateException("Reply wasn't received");
            }
        }
        catch (IOException | UnsupportedProtocolVersionException | AuthenticationException | AMQPException ex)
        {
            LOGGER.error("Failed to connect and create consumer or producer!", ex);
            throw ex;
        }
        finally
        {
            if (responseConsumer != null)
            {
                LOGGER.info("Closing consumer");
                responseConsumer.close();
            }
            if (requestProducer != null)
            {
                LOGGER.info("Closing producer");
                requestProducer.close();
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
    
    public static void main(String[] args) throws AMQPException, IOException, UnsupportedProtocolVersionException, AuthenticationException {
        Options options = new Options.OptionsBuilder()
                .timeoutInMillis(10000)
                .accountName("ABCFR_ABCFRALMMACC1")
                .hostname("ecag-fixml-dev1")
                .port(35671)
                .keystoreFilename("ABCFR_ABCFRALMMACC1.keystore")
                .keystorePassword("12345678")
                .truststoreFilename("ecag-fixml-dev1.truststore")
                .truststorePassword("12345678")
                .certificateAlias("abcfr_abcfralmmacc1")
                .build();
        RequestResponse requestResponse = new RequestResponse(options);
        requestResponse.run();
    }
}
