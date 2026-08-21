package com.restconf.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkInterfaceModelTest {

    @Test
    @DisplayName("Should build NetworkInterface using Builder pattern")
    void testBuilder() {
        NetworkInterface iface = NetworkInterface.builder()
                .name("GigabitEthernet0/0/0")
                .description("Core Backbone Router")
                .type("iana-if-type:ethernetCsmacd")
                .enabled(true)
                .ipAddress("10.0.1.1")
                .prefixLength(30)
                .build();

        assertThat(iface.getName()).isEqualTo("GigabitEthernet0/0/0");
        assertThat(iface.getDescription()).isEqualTo("Core Backbone Router");
        assertThat(iface.getType()).isEqualTo("iana-if-type:ethernetCsmacd");
        assertThat(iface.getEnabled()).isTrue();
        assertThat(iface.getIpAddress()).isEqualTo("10.0.1.1");
        assertThat(iface.getPrefixLength()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should verify equality based on interface name")
    void testEquality() {
        NetworkInterface iface1 = new NetworkInterface("Loopback0", "Desc 1", "iana-if-type:softwareLoopback", true, "10.0.0.1", 32);
        NetworkInterface iface2 = new NetworkInterface("Loopback0", "Desc 2", "iana-if-type:softwareLoopback", false, "10.0.0.2", 32);
        NetworkInterface iface3 = new NetworkInterface("Loopback1", "Desc 1", "iana-if-type:softwareLoopback", true, "10.0.0.1", 32);

        assertThat(iface1).isEqualTo(iface2);
        assertThat(iface1.hashCode()).isEqualTo(iface2.hashCode());
        assertThat(iface1).isNotEqualTo(iface3);
    }
}
