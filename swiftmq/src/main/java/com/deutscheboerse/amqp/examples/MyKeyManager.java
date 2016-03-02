package com.deutscheboerse.amqp.examples;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class MyKeyManager extends X509ExtendedKeyManager
{
    private final String alias;
    private final X509ExtendedKeyManager originalKeyManager;

    public MyKeyManager(String alias) throws GeneralSecurityException, IOException
    {
        this.alias = alias;

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(new File(System.getProperty("javax.net.ssl.keyStore"))), System.getProperty("javax.net.ssl.keyStorePassword").toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, System.getProperty("javax.net.ssl.keyStorePassword").toCharArray());
        originalKeyManager = (X509ExtendedKeyManager)kmf.getKeyManagers()[0];
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket)
    {
        return alias;
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket)
    {
        return originalKeyManager.chooseServerAlias(keyType, issuers, socket);
    }

    public X509Certificate[] getCertificateChain(String alias)
    {
        return originalKeyManager.getCertificateChain(alias);
    }

    public String[] getClientAliases(String keyType, Principal[] issuers)
    {
        return new String[]{alias};
    }

    public PrivateKey getPrivateKey(String alias)
    {
        return originalKeyManager.getPrivateKey(alias);
    }

    public String[] getServerAliases(String keyType, Principal[] issuers)
    {
        return originalKeyManager.getServerAliases(keyType, issuers);
    }

    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine)
    {
        return alias;
    }

    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine)
    {
        return originalKeyManager.chooseEngineServerAlias(keyType, issuers, engine);
    }
}
