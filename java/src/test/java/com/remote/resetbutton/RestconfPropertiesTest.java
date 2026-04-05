package com.remote.resetbutton;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestconfPropertiesTest {

    @Test
    void defaultsShouldMatchExpectedValues() {
        RestconfProperties properties = new RestconfProperties();

        assertEquals("localhost", properties.getHost());
        assertEquals(8080, properties.getPort());
        assertEquals("admin", properties.getUsername());
        assertEquals("admin", properties.getPassword());
        assertFalse(properties.isUseHttps());
        assertTrue(properties.isSkipSslVerification());
        assertEquals(5, properties.getRebootWaitSeconds());
    }

    @Test
    void settersShouldUpdateValues() {
        RestconfProperties properties = new RestconfProperties();

        properties.setHost("router-1");
        properties.setPort(8443);
        properties.setUsername("netops");
        properties.setPassword("secret");
        properties.setUseHttps(true);
        properties.setSkipSslVerification(false);
        properties.setRebootWaitSeconds(10);

        assertEquals("router-1", properties.getHost());
        assertEquals(8443, properties.getPort());
        assertEquals("netops", properties.getUsername());
        assertEquals("secret", properties.getPassword());
        assertTrue(properties.isUseHttps());
        assertFalse(properties.isSkipSslVerification());
        assertEquals(10, properties.getRebootWaitSeconds());
    }

    @Test
    void baseUrlShouldUseConfiguredSchemeHostAndPort() {
        RestconfProperties properties = new RestconfProperties();

        properties.setHost("devnet");
        properties.setPort(443);
        properties.setUseHttps(true);
        assertEquals("https://devnet:443", properties.baseUrl());

        properties.setUseHttps(false);
        assertEquals("http://devnet:443", properties.baseUrl());
    }
}
