package com.deutscheboerse.amqp.examples;

import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import javax.jms.BytesMessage;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message Listener
 * Processes incoming / received messages by printing them
 */
public class Listener implements MessageListener, ExceptionListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Listener.class);
    private Timer timer;
    
    public void setTimeout(int miliseconds) throws InterruptedException
    {
        timer = new Timer();
        timer.schedule(new StopListening(this), miliseconds);
        synchronized (this)
        {
            this.wait();
        }
        timer.cancel();
    }

    public void onMessage(Message msg)
    {
        try
        {
            LOGGER.info("RECEIVED MESSAGE:");
            LOGGER.info("#################");

            if (msg instanceof TextMessage)
            {
                TextMessage textMessage = (TextMessage) msg;
                String messageText = textMessage.getText();
                LOGGER.info("messageText = {}", messageText);
            }
            else if (msg instanceof BytesMessage)
            {
                BytesMessage  bytesMessage = (BytesMessage) msg;
                StringBuilder builder      = new StringBuilder();
                for (int i = 0; i < bytesMessage.getBodyLength(); i++)
                {
                    builder.append((char) bytesMessage.readByte());
                }
                LOGGER.info("messageBytes = {}", builder.toString());
            }

            //@SuppressWarnings("unchecked")
            Enumeration<?> propertyNames = msg.getPropertyNames();

            while (propertyNames.hasMoreElements())
            {
                Object prop = propertyNames.nextElement();
                LOGGER.info("{} = {}", prop, msg.getStringProperty((String) prop));
            }

            LOGGER.info("Correlation ID {}", msg.getJMSCorrelationID());
            LOGGER.info("#################");
            msg.acknowledge();
        }
        catch (JMSException ex)
        {
            LOGGER.error("Failed to process incomming message {}", ex);
        }
    }

    @Override
    public void onException(JMSException ex)
    {
        LOGGER.error("Exception: {} caught from connection object. Reconnect needed. Exiting ... ", ex);
        System.exit(0);
    }
    
    private class StopListening extends TimerTask
    {
        private final Listener listener;
        public StopListening(Listener listener)
        {
            this.listener = listener;
        }
        
        public void run()
        {
            synchronized (this.listener)
            {
                this.listener.notify();   
            }
        }
    }
}
