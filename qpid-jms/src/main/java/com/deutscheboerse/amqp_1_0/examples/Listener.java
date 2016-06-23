package com.deutscheboerse.amqp_1_0.examples;


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
    private int messagesReceivedCount = 0;
    private boolean exceptionReceived = false;

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
                LOGGER.info("Message Text = {}", messageText);
            }
            else if (msg instanceof BytesMessage)
            {
                BytesMessage bytesMessage = (BytesMessage) msg;
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < bytesMessage.getBodyLength(); i++)
                {
                    builder.append((char) bytesMessage.readByte());
                }
                LOGGER.info("Message Text = {}", builder.toString());
            }
            else
            {
                LOGGER.error("Unexpected message type delivered: {}", msg.toString());
                this.exceptionReceived = true;
            }
            LOGGER.info("Correlation ID {}", msg.getJMSCorrelationID());
            LOGGER.info("#################");
            msg.acknowledge();
            this.messagesReceivedCount++;
        }
        catch (JMSException ex)
        {
            LOGGER.error("Failed to process incomming message", ex);
            this.exceptionReceived = true;
        }
    }

    @Override
    public void onException(JMSException ex)
    {
        LOGGER.error("Exception caught from connection object. Reconnect needed. Exiting ... ", ex);
        this.exceptionReceived = true;
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
