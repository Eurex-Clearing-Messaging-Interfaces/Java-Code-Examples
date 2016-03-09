package com.deutscheboerse.amqp.examples;

public final class Options
{
    private final int timeoutInMillis;
    private final String hostname;
    private final int port;
    private final String accountName;
    private final String keystoreFileName;
    private final String truststoreFileName;
    private final String keystorePassword;
    private final String truststorePassword;
    private final String certificateAlias;

    private Options(int timeoutInMillis, String hostname, int port, String accountName, String keystoreFileName, String truststoreFileName,
            String keystorePassword, String truststorePassword, String certificateAlias)
    {
        this.timeoutInMillis = timeoutInMillis;
        this.hostname = hostname;
        this.port = port;
        this.accountName = accountName;
        this.keystoreFileName = keystoreFileName;
        this.truststoreFileName = truststoreFileName;
        this.keystorePassword = keystorePassword;
        this.truststorePassword = truststorePassword;
        this.certificateAlias = certificateAlias;
    }

    public int getTimeoutInMillis()
    {
        return timeoutInMillis;
    }

    public String getHostname()
    {
        return hostname;
    }

    public int getPort()
    {
        return port;
    }

    public String getAccountName()
    {
        return accountName;
    }

    public String getKeystoreFileName()
    {
        return keystoreFileName;
    }

    public String getTruststoreFileName()
    {
        return truststoreFileName;
    }

    public String getKeystorePassword()
    {
        return keystorePassword;
    }

    public String getTruststorePassword()
    {
        return truststorePassword;
    }

    public String getCertificateAlias()
    {
        return certificateAlias;
    }

    public static class OptionsBuilder
    {
        private int nestedTimeoutInMillis = 100000;
        private String nestedHostname = "ecag-fixml-simu1.deutsche-boerse.com";
        private int nestedPort = 10170;
        private String nestedAccountName = "ABCFR_ABCFRALMMACC1";
        private String nestedKeystoreFileName = "ABCFR_ABCFRALMMACC1.keystore";
        private String nestedTruststoreFileName = "truststore.jks";
        private String nestedKeystorePassword = "123456";
        private String nestedTruststorePassword = "123456";
        private String nestedCertificateAlias = "abcfr_abcfralmmacc1";

        public OptionsBuilder timeoutInMillis(int timeout)
        {
            this.nestedTimeoutInMillis = timeout;
            return this;
        }

        public OptionsBuilder hostname(String nestedHostname)
        {
            this.nestedHostname = nestedHostname;
            return this;
        }

        public OptionsBuilder port(int nestedPort)
        {
            this.nestedPort = nestedPort;
            return this;
        }

        public OptionsBuilder accountName(String nestedAccountName)
        {
            this.nestedAccountName = nestedAccountName;
            return this;
        }

        public OptionsBuilder keystoreFilename(String nestedKeystoreFilename)
        {
            this.nestedKeystoreFileName = nestedKeystoreFilename;
            return this;
        }

        public OptionsBuilder truststoreFilename(String nestedTruststoreFilename)
        {
            this.nestedTruststoreFileName = nestedTruststoreFilename;
            return this;
        }

        public OptionsBuilder keystorePassword(String nestedKeystorePassword)
        {
            this.nestedKeystorePassword = nestedKeystorePassword;
            return this;
        }

        public OptionsBuilder truststorePassword(String nestedTruststorePassword)
        {
            this.nestedTruststorePassword = nestedTruststorePassword;
            return this;
        }

        public OptionsBuilder certificateAlias(String nestedCertificateAlias)
        {
            this.nestedCertificateAlias = nestedCertificateAlias;
            return this;
        }

        public Options build()
        {
            return new Options(nestedTimeoutInMillis, nestedHostname, nestedPort, nestedAccountName, nestedKeystoreFileName,
                    nestedTruststoreFileName, nestedKeystorePassword, nestedTruststorePassword, nestedCertificateAlias);
        }
    }
}
