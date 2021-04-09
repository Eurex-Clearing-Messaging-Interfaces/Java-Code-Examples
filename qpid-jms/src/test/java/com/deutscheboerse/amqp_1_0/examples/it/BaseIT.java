package com.deutscheboerse.amqp_1_0.examples.it;

import com.deutscheboerse.amqp_1_0.examples.BroadcastReceiver;
import com.deutscheboerse.amqp_1_0.examples.RequestResponse;
import com.deutscheboerse.amqp_1_0.examples.Options;
import com.deutscheboerse.amqp_1_0.examples.it.utils.Utils;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.jms.*;
import javax.naming.NamingException;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class BaseIT {

    public static final String BROKER_HOSTNAME = "ecag-fpml-sim1";
    public static final String BROADCAST_QUEUE = "broadcast.ABCFR_ABCFRALMMACC1.TradeNotification";
    public static final String REQUEST_QUEUE = "request_be.ABCFR_ABCFRALMMACC1";
    public static final String RESPONSE_QUEUE = "response.ABCFR_ABCFRALMMACC1";

    protected Utils brokerUtils = new Utils();

    @Test
    public void broadcastReceiverIT() throws JMSException, NamingException, InterruptedException
    {
        final String broadcastMessageText = "Broadcast Text";
        try (Connection connection = this.brokerUtils.getAdminConnection(BROKER_HOSTNAME))
        {
            connection.start();
            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            MessageProducer broadcastProducer = session.createProducer(this.brokerUtils.getQueue(BROADCAST_QUEUE));
            Message textMessage = session.createTextMessage(broadcastMessageText);
            broadcastProducer.send(textMessage);
            BytesMessage bytesMessage = session.createBytesMessage();
            bytesMessage.writeUTF(broadcastMessageText);
            broadcastProducer.send(bytesMessage);

        }
        final String keystorePath = BaseIT.class.getResource("ABCFR_ABCFRALMMACC1.keystore").getPath();
        final String truststorePath = BaseIT.class.getResource("ecag-fpml-sim1.truststore").getPath();

        Options options = new Options.OptionsBuilder()
                .timeoutInMillis(1000)
                .accountName("ABCFR_ABCFRALMMACC1")
                .hostname(BaseIT.BROKER_HOSTNAME)
                .port(35671)
                .keystoreFilename(keystorePath)
                .keystorePassword("12345678")
                .truststoreFilename(truststorePath)
                .truststorePassword("12345678")
                .certificateAlias("abcfr_abcfralmmacc1")
                .build();
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver(options);
        broadcastReceiver.run();
        Assert.assertEquals(broadcastReceiver.getMessagesReceivedCount(), 2, "Invalid broadcast messages received");
        Assert.assertFalse(broadcastReceiver.isExceptionReceived(), "Exception while receiving broadcast");
    }

    @Test
    public void requestResponseIT() throws InterruptedException, JMSException, NamingException, ExecutionException
    {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<Boolean> future = executor.submit(new Callable<Boolean>()
        {
            @Override
            public Boolean call()
            {
                boolean success = true;
                try (Connection connection = BaseIT.this.brokerUtils.getAdminConnection(BROKER_HOSTNAME))
                {
                    connection.start();
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    MessageConsumer requestConsumer = session.createConsumer(BaseIT.this.brokerUtils.getQueue(REQUEST_QUEUE));
                    // receive request message and create response as a text message
                    Message requestMessage = requestConsumer.receive(10000);
                    assertNotNull(requestMessage, "Didn't receive first request message");
                    String receivedMessageText = ((TextMessage) requestMessage).getText();
                    assertEquals("<FPML>...</FPML>", receivedMessageText, "First received message doesn't contain expected text");
                    Message responseMessage = session.createTextMessage("RESPONSE TO:" + receivedMessageText);
                    MessageProducer responseProducer = session.createProducer(BaseIT.this.brokerUtils.getQueue(RESPONSE_QUEUE));
                    responseProducer.send(responseMessage);
                    // receive request message and create response as a bytes message
                    requestMessage = requestConsumer.receive(10000);
                    assertNotNull(requestMessage, "Didn't receive second request message");
                    receivedMessageText = ((TextMessage) requestMessage).getText();
                    assertEquals("<FPML>...</FPML>", receivedMessageText, "Second received message doesn't contain expected text");
                    responseMessage = session.createBytesMessage();
                    ((BytesMessage) responseMessage).writeUTF("RESPONSE TO:" + receivedMessageText);
                    responseProducer.send(responseMessage);
                }
                catch (JMSException | NamingException ex)
                {
                    System.out.println("ERRRROR:");
                    System.out.println(ex);
                    success = false;
                }
                return success;
            }
        });
        final String keystorePath = BaseIT.class.getResource("ABCFR_ABCFRALMMACC1.keystore").getPath();
        final String truststorePath = BaseIT.class.getResource("ecag-fpml-sim1.truststore").getPath();

        Options options = new Options.OptionsBuilder()
                .timeoutInMillis(10000)
                .accountName("ABCFR_ABCFRALMMACC1")
                .hostname(BaseIT.BROKER_HOSTNAME)
                .port(35671)
                .keystoreFilename(keystorePath)
                .keystorePassword("12345678")
                .truststoreFilename(truststorePath)
                .truststorePassword("12345678")
                .certificateAlias("abcfr_abcfralmmacc1")
                .build();
        RequestResponse requestResponse = new RequestResponse(options);
        // send first request (testing response as a text message)
        requestResponse.run();
        // send second request (testing response as a bytes message)
        requestResponse.run();
        assertTrue(future.get(), "Responder thread finished with failure");
        executor.shutdown();
    }
}
