package com.deutscheboerse.amqp.examples.it;

import com.deutscheboerse.amqp.examples.BroadcastReceiver;
import com.deutscheboerse.amqp.examples.RequestResponse;
import com.deutscheboerse.amqp.examples.Options;
import com.deutscheboerse.amqp.examples.it.utils.AutoCloseableConnection;
import com.deutscheboerse.amqp.examples.it.utils.Utils;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.NamingException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class BaseIT {

    public static final String BROKER_HOSTNAME = "ecag-fixml-dev1";
    public static final String BROADCAST_QUEUE = "broadcast.ABCFR_ABCFRALMMACC1.TradeConfirmation";
    public static final String REQUEST_QUEUE = "request_be.ABCFR_ABCFRALMMACC1";
    public static final String RESPONSE_QUEUE = "response.ABCFR_ABCFRALMMACC1";

    protected Utils brokerUtils = new Utils();

    @Test
    public void broadcastReceiverIT() throws JMSException, NamingException, InterruptedException
    {
        final String broadcastMessageText = "Broadcast Text";
        try (AutoCloseableConnection connection = this.brokerUtils.getAdminConnection(BROKER_HOSTNAME))
        {
            connection.start();
            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            MessageProducer broadcastProducer = session.createProducer(this.brokerUtils.getQueue(BROADCAST_QUEUE));
            Message broadcastMessage = session.createTextMessage(broadcastMessageText);
            broadcastProducer.send(broadcastMessage);
        }
        final String keystorePath = BaseIT.class.getResource("ABCFR_ABCFRALMMACC1.keystore").getPath();
        final String truststorePath = BaseIT.class.getResource("ecag-fixml-dev1.truststore").getPath();

        Options options = new Options.OptionsBuilder()
                .timeoutInMillis(1000)
                .accountName("ABCFR_ABCFRALMMACC1")
                .hostname(BaseIT.BROKER_HOSTNAME)
                .port(5671)
                .keystoreFilename(keystorePath)
                .keystorePassword("123456")
                .truststoreFilename(truststorePath)
                .truststorePassword("123456")
                .certificateAlias("abcfr_abcfralmmacc1")
                .build();
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver(options);
        broadcastReceiver.run();
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
                try (AutoCloseableConnection connection = BaseIT.this.brokerUtils.getAdminConnection(BROKER_HOSTNAME))
                {
                    connection.start();
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    MessageConsumer requestConsumer = session.createConsumer(BaseIT.this.brokerUtils.getQueue(REQUEST_QUEUE));
                    Message requestMessage = requestConsumer.receive(10000);
                    assertNotNull(requestMessage, "Didn't receive request message");
                    String receivedMessageText = ((TextMessage) requestMessage).getText();
                    assertEquals("<FIXML>...</FIXML>", receivedMessageText, "Received message doesn't contain expected text");
                    Message responseMessage = session.createTextMessage("RESPONSE TO:" + receivedMessageText);
                    MessageProducer responseProducer = session.createProducer(BaseIT.this.brokerUtils.getQueue(RESPONSE_QUEUE));
                    responseProducer.send(responseMessage);
                }
                catch (JMSException | NamingException ex)
                {
                    success = false;
                }
                return success;
            }
        });
        final String keystorePath = BaseIT.class.getResource("ABCFR_ABCFRALMMACC1.keystore").getPath();
        final String truststorePath = BaseIT.class.getResource("ecag-fixml-dev1.truststore").getPath();

        Options options = new Options.OptionsBuilder()
                .timeoutInMillis(10000)
                .accountName("ABCFR_ABCFRALMMACC1")
                .hostname(BaseIT.BROKER_HOSTNAME)
                .port(5671)
                .keystoreFilename(keystorePath)
                .keystorePassword("123456")
                .truststoreFilename(truststorePath)
                .truststorePassword("123456")
                .certificateAlias("abcfr_abcfralmmacc1")
                .build();
        RequestResponse requestResponse = new RequestResponse(options);
        requestResponse.run();
        assertTrue(future.get(), "Responder thread finished with failure");
        executor.shutdown();
    }
}
