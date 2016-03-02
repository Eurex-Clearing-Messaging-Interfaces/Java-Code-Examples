package com.deutscheboerse.amqp.examples;

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
    private boolean received = false;

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
        if (!received)
        {
            LOGGER.error("Reply wasn't received for {} seconds", timeout/1000);
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
                    Listener.this.received = true;
                    LOGGER.info("RECEIVED MESSAGE:");
                    LOGGER.info("#################");
                    String correlationId = (receivedMsg.getProperties() == null) ? "null" : receivedMsg.getProperties().getCorrelationId().getValueString();
                    LOGGER.info("Correlation ID: {}", correlationId);
                    LOGGER.info("Message Text  : {}", new String(receivedMsg.getData().get(0).getValue()));
                    LOGGER.info("#################");
                    try
                    {
                        receivedMsg.accept();
                    }
                    catch (InvalidStateException e)
                    {
                        LOGGER.error("Failed to acknowledge message.");
                    }
                }
                else
                {
                    return null;
                }
            }
        }
    }
}