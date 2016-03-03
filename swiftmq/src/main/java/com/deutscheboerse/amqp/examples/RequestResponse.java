package com.deutscheboerse.amqp.examples;

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
import com.swiftmq.amqp.v100.types.AMQPString;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Broadcast Receiver Receives broadcasts from the persistent broadcast queue
 */
public class RequestResponse
{
    private static final int TIMEOUT_MILLIS = 100000;
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponse.class);

    public RequestResponse(String[] args)
    {
    }
    
    public void run() throws AMQPException
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
        Producer requestProducer = null;
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
            requestProducer = session.createProducer("request.ABCFR_ABCFRALMMACC1", QoS.AT_LEAST_ONCE);
            responseConsumer = session.createConsumer("response.ABCFR_ABCFRALMMACC1", 1000, QoS.AT_LEAST_ONCE, true, null);
            
            /*
             * Step 5: Sending a request
             */
            AMQPMessage msg = new AMQPMessage();
            msg.setAmqpValue(new AmqpValue(new AMQPString("<FIXML>...</FIXML>")));
            Properties msgProp = new Properties();
            msgProp.setReplyTo(new AddressString("response/response.ABCFR_ABCFRALMMACC1"));
            msgProp.setCorrelationId(new MessageIdString(UUID.randomUUID().toString()));
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
            LOGGER.info("Waiting {} seconds for reply", TIMEOUT_MILLIS/1000);
            AMQPMessage receivedMsg = responseConsumer.receive(TIMEOUT_MILLIS);
            if (receivedMsg != null)
            {
                LOGGER.info("RECEIVED MESSAGE:");
                LOGGER.info("#################");
                LOGGER.info("Correlation ID: {}", receivedMsg.getProperties().getCorrelationId().getValueString());
                LOGGER.info("Message Text  : {}", new String(receivedMsg.getData().get(0).getValue()));
                LOGGER.info("#################");
                receivedMsg.accept();
            }
            else
            {
                LOGGER.error("Reply wasn't received for {} seconds", TIMEOUT_MILLIS/1000);
            }
        }
        catch (IOException | UnsupportedProtocolVersionException | AuthenticationException | AMQPException ex)
        {
            LOGGER.error("Failed to connect and create consumer or producer!", ex);
            System.exit(1);
        }
        finally
        {
            // Closing the connection
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
    
    public static void main(String[] args) throws AMQPException
    {
        RequestResponse requestResponse = new RequestResponse(args);
        requestResponse.run();
    }
}
