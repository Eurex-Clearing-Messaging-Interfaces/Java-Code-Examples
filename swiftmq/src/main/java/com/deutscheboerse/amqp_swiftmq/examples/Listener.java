package com.deutscheboerse.amqp_swiftmq.examples;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.swiftmq.amqp.v100.client.AMQPException;
import com.swiftmq.amqp.v100.client.Consumer;
import com.swiftmq.amqp.v100.client.InvalidStateException;
import com.swiftmq.amqp.v100.messaging.AMQPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message Listener
 * Processes incoming / received messages by printing them
 */
public class Listener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Listener.class);

    private Timer timer;
    private int timeout;
    private final Consumer consumer;
    private final ExecutorService service = Executors.newFixedThreadPool(1);
    private int messagesReceivedCount = 0;
    private boolean exceptionReceived = false;

    public Listener(Consumer consumer)
    {
        this.consumer = consumer;
    }

    public void setTimeout(int miliseconds) throws InterruptedException, AMQPException
    {
        timer = new Timer();
        timeout = miliseconds;
        timer.schedule(new StopListening(), miliseconds);
        Future<?> future = service.submit(new MessageReceiver());
        synchronized (this)
        {
            this.wait();
        }
        timer.cancel();
        future.cancel(true);
        service.shutdown();
        if (messagesReceivedCount == 0)
        {
            LOGGER.error("No message received for {} seconds", timeout/1000);
        }
    }
    
    private class StopListening extends TimerTask
    {
        public void run()
        {
            synchronized (Listener.this)
            {
                Listener.this.notify();   
            }
        }
    }

    private class MessageReceiver implements Callable<Object>
    {
        @Override
        public Object call()
        {
            while (true)
            {
                AMQPMessage receivedMsg = consumer.receive();
                if (receivedMsg != null)
                {
                    Listener.this.messagesReceivedCount++;
                    LOGGER.info("RECEIVED MESSAGE:");
                    LOGGER.info("#################");
                    /*String correlationId = (receivedMsg.getProperties() == null) ? "null" : receivedMsg.getProperties().getCorrelationId().getValueString();
                    LOGGER.info("Correlation ID: {}", correlationId);
                    LOGGER.info("Message Text  : {}", new String(receivedMsg.getData().get(0).getValue()));
                    LOGGER.info("#################");*/
                    try
                    {
                        receivedMsg.accept();
                    }
                    catch (InvalidStateException e)
                    {
                        LOGGER.error("Failed to acknowledge message.");
                        Listener.this.exceptionReceived = true;
                    }
                }
                else
                {
                    return null;
                }
            }
        }
    }

    public int getMessagesReceivedCount()
    {
        return this.messagesReceivedCount;
    }

    public boolean isExceptionReceived()
    {
        return this.exceptionReceived;
    }
}