package com.mock.repository;

import com.mock.model.NetworkInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryInterfaceRepositoryTest {

    private InMemoryInterfaceRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryInterfaceRepository();
    }

    @Test
    @DisplayName("Should initialize with default interfaces")
    void testInitialSeedData() {
        assertThat(repository.count()).isEqualTo(5);
        assertThat(repository.findByName("GigabitEthernet2")).isPresent();
        assertThat(repository.findByName("Loopback0")).isPresent();
    }

    @Test
    @DisplayName("Should save and retrieve a new interface")
    void testSaveAndFind() {
        NetworkInterface iface = new NetworkInterface("Loopback99", "Test Loopback", "iana-if-type:softwareLoopback", true, "10.99.99.1", 32);
        repository.save(iface);

        Optional<NetworkInterface> found = repository.findByName("Loopback99");
        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Test Loopback");
        assertThat(found.get().getIpAddress()).isEqualTo("10.99.99.1");
    }

    @Test
    @DisplayName("Should throw exception when saving null entity or null name")
    void testSaveNullValidation() {
        assertThatThrownBy(() -> repository.save(null))
                .isInstanceOf(IllegalArgumentException.class);

        NetworkInterface nullName = new NetworkInterface(null, "desc", "type", true, null, null);
        assertThatThrownBy(() -> repository.save(nullName))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should delete interface by name")
    void testDelete() {
        assertThat(repository.existsByName("GigabitEthernet1")).isTrue();
        boolean deleted = repository.deleteByName("GigabitEthernet1");

        assertThat(deleted).isTrue();
        assertThat(repository.existsByName("GigabitEthernet1")).isFalse();
        assertThat(repository.findByName("GigabitEthernet1")).isEmpty();
    }

    @Test
    @DisplayName("Should return false when deleting non-existent interface")
    void testDeleteNonExistent() {
        boolean deleted = repository.deleteByName("NonExistentPort");
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("Should filter interfaces by keyword search and status")
    void testSearchAndFilter() {
        List<NetworkInterface> cameraResults = repository.search("Camera", null);
        assertThat(cameraResults).hasSize(1);
        assertThat(cameraResults.get(0).getName()).isEqualTo("GigabitEthernet2");

        List<NetworkInterface> ipResults = repository.search("192.168.10.3", null);
        assertThat(ipResults).hasSize(1);
        assertThat(ipResults.get(0).getName()).isEqualTo("GigabitEthernet3");

        List<NetworkInterface> activeResults = repository.search(null, true);
        assertThat(activeResults).hasSize(5);

        List<NetworkInterface> inactiveResults = repository.search(null, false);
        assertThat(inactiveResults).isEmpty();
    }

    @Test
    @DisplayName("Should safely handle concurrent writes and reads without corruption")
    void testConcurrentAccess() throws InterruptedException {
        int threads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String name = "TestPort-" + threadId + "-" + j;
                        repository.save(new NetworkInterface(name, "Desc", "iana-if-type:ethernetCsmacd", true, "10.0." + threadId + "." + j, 24));
                        repository.findByName(name);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(repository.count()).isEqualTo(5 + (threads * operationsPerThread));
    }
}
