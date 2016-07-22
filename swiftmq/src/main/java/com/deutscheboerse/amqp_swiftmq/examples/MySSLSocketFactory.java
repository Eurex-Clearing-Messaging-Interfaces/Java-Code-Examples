package com.deutscheboerse.amqp_swiftmq.examples;

import com.swiftmq.net.JSSESocketFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class MySSLSocketFactory extends JSSESocketFactory
{
    private static final long serialVersionUID = 1L;
    private final String alias;

    public MySSLSocketFactory(String alias)
    {
        this.alias = alias;
    }

    @Override
    public Socket createSocket(InetAddress addr, int port) throws UnknownHostException, IOException
    {
        return initializeSSLContext().getSocketFactory().createSocket(addr, port);
    }

    @Override
    public Socket createSocket(String host, int port) throws UnknownHostException, IOException
    {
        return initializeSSLContext().getSocketFactory().createSocket(host, port);
    }

    private SSLContext initializeSSLContext()
    {
        SSLContext sslContext = null;
        KeyManager[] keyManagers = null;

        try
        {
            sslContext = SSLContext.getInstance("TLS");
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }

        try
        {
            keyManagers = new KeyManager[] { new MyKeyManager(alias) };
        }
        catch (GeneralSecurityException e1)
        {
            e1.printStackTrace();
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }

        try
        {
            sslContext.init(keyManagers, null, null);
        }
        catch (KeyManagementException e)
        {
            e.printStackTrace();
        }

        return sslContext;
    }
}
