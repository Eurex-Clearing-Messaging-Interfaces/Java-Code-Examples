package com.deutscheboerse.amqp_1_0.examples.it.utils;

import java.util.Properties;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class Utils
{
    public Connection getAdminConnection(String hostname) throws JMSException, NamingException
    {
        return new ConnectionBuilder().hostname(hostname).build();
    }

    public Queue getQueue(String queueName) throws NamingException
    {
        Properties props = new Properties();
        props.setProperty("java.naming.factory.initial", "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        props.setProperty("queue.queue", queueName);
        InitialContext ctx = new InitialContext(props);
        return (Queue) ctx.lookup("queue");
    }

    public Topic getTopic(String queueName) throws NamingException
    {
        Properties props = new Properties();
        props.setProperty("java.naming.factory.initial", "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        props.setProperty("topic.topic", queueName);
        InitialContext ctx = new InitialContext(props);
        return (Topic) ctx.lookup("topic");
    }
}
