package com.deutscheboerse.amqp.examples.it.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class ConnectionBuilder
{
    public static final String TCP_PORT = "5672";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    private final String[] connectionOptions = new String[]
    {
        "sync_publish='all'", "sync_ack='true'"
    };

    protected String hostname;
    protected String port;
    protected String clientID;
    protected String username = ADMIN_USERNAME;
    protected String password = ADMIN_PASSWORD;
    protected Boolean ssl = false;
    protected Boolean syncPublish;
    protected final List<String> brokerOptions = new LinkedList<>();

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
        String brokerOptionsString = "";
        String connectionOptionsString = "";

        if (port == null)
        {
            this.port = TCP_PORT;
        }

        if (brokerOptions.size() > 0)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("?");

            for (String option : brokerOptions)
            {
                if (sb.length() > 1)
                {
                    sb.append("&");
                }
                sb.append(option);
            }

            brokerOptionsString = sb.toString();
        }

        if (connectionOptions.length > 0)
        {
            StringBuilder sb = new StringBuilder();

            for (String option : connectionOptions)
            {
                sb.append("&");
                sb.append(option);
            }

            connectionOptionsString = sb.toString();
        }

        String brokerList = String.format("tcp://%1$s:%2$s%3$s", hostname, port, brokerOptionsString);

        return String.format("amqp://%1$s:%2$s@%3$s/?brokerlist='%4$s'%5$s", username, password, clientID, brokerList, connectionOptionsString);
    }

    public AutoCloseableConnection build() throws NamingException, JMSException
    {
        Properties props = new Properties();
        props.setProperty("java.naming.factory.initial", "org.apache.qpid.jndi.PropertiesFileInitialContextFactory");
        props.setProperty("connectionfactory.connection", url());

        InitialContext ctx = new InitialContext(props);
        ConnectionFactory fact = (ConnectionFactory) ctx.lookup("connection");

        return new AutoCloseableConnection(fact.createConnection());
    }

}
