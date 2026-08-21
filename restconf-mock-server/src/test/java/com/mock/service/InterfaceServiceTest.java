package com.mock.service;

import com.mock.exception.InterfaceAlreadyExistsException;
import com.mock.exception.InterfaceNotFoundException;
import com.mock.model.NetworkInterface;
import com.mock.repository.InMemoryInterfaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterfaceServiceTest {

    private InterfaceService service;
    private InMemoryInterfaceRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryInterfaceRepository();
        service = new InterfaceServiceImpl(repository);
    }

    @Test
    @DisplayName("Should successfully create a new interface")
    void testCreateInterface() {
        NetworkInterface iface = new NetworkInterface("Loopback500", "BGP Peering", "iana-if-type:softwareLoopback", true, "10.50.0.1", 32);
        NetworkInterface created = service.createInterface(iface);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Loopback500");
        assertThat(service.getInterface("Loopback500")).isNotNull();
    }

    @Test
    @DisplayName("Should throw InterfaceAlreadyExistsException when creating duplicate")
    void testCreateDuplicateThrowsException() {
        NetworkInterface duplicate = new NetworkInterface("GigabitEthernet1", "Duplicate Port", "iana-if-type:ethernetCsmacd", true, null, null);
        assertThatThrownBy(() -> service.createInterface(duplicate))
                .isInstanceOf(InterfaceAlreadyExistsException.class)
                .hasMessageContaining("GigabitEthernet1");
    }

    @Test
    @DisplayName("Should throw InterfaceNotFoundException when retrieving non-existent")
    void testGetNotFoundThrowsException() {
        assertThatThrownBy(() -> service.getInterface("NonExistentPort"))
                .isInstanceOf(InterfaceNotFoundException.class)
                .hasMessageContaining("NonExistentPort");
    }

    @Test
    @DisplayName("Should update existing interface")
    void testUpdateInterface() {
        NetworkInterface updatePayload = new NetworkInterface("GigabitEthernet2", "Updated Camera Entrance", "iana-if-type:ethernetCsmacd", false, "192.168.10.20", 24);
        NetworkInterface updated = service.updateInterface("GigabitEthernet2", updatePayload);

        assertThat(updated.getDescription()).isEqualTo("Updated Camera Entrance");
        assertThat(updated.getEnabled()).isFalse();
        assertThat(updated.getIpAddress()).isEqualTo("192.168.10.20");
    }

    @Test
    @DisplayName("Should partially patch interface attributes")
    void testPatchInterface() {
        // Shutdown port via patch
        NetworkInterface patched = service.patchInterface("GigabitEthernet3", Map.of("enabled", false, "description", "Disabled AP"));

        assertThat(patched.getEnabled()).isFalse();
        assertThat(patched.getDescription()).isEqualTo("Disabled AP");
        // Other attributes should remain intact
        assertThat(patched.getIpAddress()).isEqualTo("192.168.10.3");
        assertThat(patched.getType()).isEqualTo("iana-if-type:ethernetCsmacd");
    }

    @Test
    @DisplayName("Should delete interface successfully")
    void testDeleteInterface() {
        service.deleteInterface("GigabitEthernet4");
        assertThatThrownBy(() -> service.getInterface("GigabitEthernet4"))
                .isInstanceOf(InterfaceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw InterfaceNotFoundException when deleting non-existent interface")
    void testDeleteNotFound() {
        assertThatThrownBy(() -> service.deleteInterface("MissingPort"))
                .isInstanceOf(InterfaceNotFoundException.class);
    }
}
