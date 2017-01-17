package com.deutscheboerse.amqp_1_0.examples.it.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class ConnectionBuilder
{
    public static final String TCP_PORT = "35672";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    protected String hostname;
    protected String port;
    protected String clientID;
    protected String username = ADMIN_USERNAME;
    protected String password = ADMIN_PASSWORD;
    protected Boolean ssl = false;
    protected Boolean syncPublish;
    protected final List<String> brokerOptions = new LinkedList<>();

    public ConnectionBuilder()
    {
        this.brokerOptions.add("amqp.idleTimeout=0");
    }

    public ConnectionBuilder ssl()
    {
        this.ssl = true;
        return this;
    }

    public ConnectionBuilder hostname(String hostname)
    {
        this.hostname = hostname;
        return this;
    }

    public ConnectionBuilder port(String port)
    {
        this.port = port;
        return this;
    }

    public ConnectionBuilder clientID(String clientID)
    {
        this.clientID = clientID;
        return this;
    }

    public ConnectionBuilder username(String username)
    {
        this.username = username;
        return this;
    }

    public ConnectionBuilder password(String password)
    {
        this.password = password;
        return this;
    }

    public ConnectionBuilder syncPublish(Boolean syncPublish)
    {
        this.syncPublish = syncPublish;
        return this;
    }

    public ConnectionBuilder brokerOption(String brokerOption)
    {
        this.brokerOptions.add(brokerOption);
        return this;
    }

    protected String url()
    {
        String protocol = "amqp";
        String connectionOptionsString = "";

        if (port == null)
        {
            this.port = TCP_PORT;
        }

        if (username != null)
        {
            brokerOptions.add("jms.username=" + username);
        }

        if (password != null)
        {
            brokerOptions.add("jms.password=" + password);
        }

        if (clientID != null)
        {
            brokerOptions.add("jms.clientID=" + clientID);
        }

        if (syncPublish != null)
        {
            if (syncPublish)
            {
                brokerOptions.add("jms.alwaysSyncSend=True");
            }
            else
            {
                brokerOptions.add("jms.forceAsyncSend=True");
            }
        }

        if (brokerOptions.size() > 0)
        {
            StringBuilder sb = new StringBuilder();

            for (String option : brokerOptions)
            {
                sb.append("&");
                sb.append(option);
            }

            sb.replace(0, 1, "?");
            connectionOptionsString = sb.toString();
        }

        return String.format("%1$s://%2$s:%3$s%4$s", protocol, hostname, port, connectionOptionsString);

    }

    public Connection build() throws NamingException, JMSException
    {
        System.out.println(url());
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        props.setProperty("connectionfactory.connection", url());

        InitialContext ctx = new InitialContext(props);
        ConnectionFactory fact = (ConnectionFactory) ctx.lookup("connection");

        return fact.createConnection();
    }
}
