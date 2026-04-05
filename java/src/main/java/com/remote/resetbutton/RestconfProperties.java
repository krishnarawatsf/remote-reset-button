package com.remote.resetbutton;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "restconf")
public class RestconfProperties {

    private String host = "localhost";
    private int port = 8080;
    private String username = "admin";
    private String password = "admin";
    private boolean useHttps = false;
    private boolean skipSslVerification = true;
    private int rebootWaitSeconds = 5;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseHttps() {
        return useHttps;
    }

    public void setUseHttps(boolean useHttps) {
        this.useHttps = useHttps;
    }

    public boolean isSkipSslVerification() {
        return skipSslVerification;
    }

    public void setSkipSslVerification(boolean skipSslVerification) {
        this.skipSslVerification = skipSslVerification;
    }

    public int getRebootWaitSeconds() {
        return rebootWaitSeconds;
    }

    public void setRebootWaitSeconds(int rebootWaitSeconds) {
        this.rebootWaitSeconds = rebootWaitSeconds;
    }

    public String baseUrl() {
        return (useHttps ? "https" : "http") + "://" + host + ":" + port;
    }
}