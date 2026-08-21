package com.mock.repository;

import com.mock.model.NetworkInterface;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Thread-safe In-Memory Repository backed by ConcurrentHashMap.
 * 
 * Time Complexities:
 * - findByName: O(1) average hash lookup
 * - save:       O(1) average hash insert/update
 * - deleteByName: O(1) average hash remove
 * - existsByName: O(1) average hash containsKey
 * - findAll:    O(N) iteration
 * - search:     O(N) iteration with predicate filtering
 * 
 * Space Complexity:
 * - O(N) where N is the number of interfaces stored in memory.
 */
@Repository
public class InMemoryInterfaceRepository implements InterfaceRepository {

    private final ConcurrentMap<String, NetworkInterface> store = new ConcurrentHashMap<>();

    public InMemoryInterfaceRepository() {
        resetDefaults();
    }

    @Override
    public NetworkInterface save(NetworkInterface networkInterface) {
        if (networkInterface == null || networkInterface.getName() == null) {
            throw new IllegalArgumentException("Network interface and name must not be null");
        }
        store.put(networkInterface.getName(), networkInterface);
        return networkInterface;
    }

    @Override
    public Optional<NetworkInterface> findByName(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(store.get(name));
    }

    @Override
    public List<NetworkInterface> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<NetworkInterface> search(String keyword, Boolean enabled) {
        return store.values().stream()
                .filter(iface -> {
                    if (enabled != null && !enabled.equals(iface.getEnabled())) {
                        return false;
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        String lowerKeyword = keyword.toLowerCase();
                        boolean nameMatch = iface.getName() != null && iface.getName().toLowerCase().contains(lowerKeyword);
                        boolean descMatch = iface.getDescription() != null && iface.getDescription().toLowerCase().contains(lowerKeyword);
                        boolean ipMatch = iface.getIpAddress() != null && iface.getIpAddress().contains(keyword);
                        boolean typeMatch = iface.getType() != null && iface.getType().toLowerCase().contains(lowerKeyword);
                        return nameMatch || descMatch || ipMatch || typeMatch;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteByName(String name) {
        if (name == null) return false;
        return store.remove(name) != null;
    }

    @Override
    public boolean existsByName(String name) {
        if (name == null) return false;
        return store.containsKey(name);
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void resetDefaults() {
        store.clear();
        save(new NetworkInterface("GigabitEthernet1", "WAN Uplink - ISP Connection", "iana-if-type:ethernetCsmacd", true, "203.0.113.1", 30));
        save(new NetworkInterface("GigabitEthernet2", "Camera 2 - Entrance", "iana-if-type:ethernetCsmacd", true, "192.168.10.2", 24));
        save(new NetworkInterface("GigabitEthernet3", "Access Point - Seating Area", "iana-if-type:ethernetCsmacd", true, "192.168.10.3", 24));
        save(new NetworkInterface("GigabitEthernet4", "POS Terminal - Counter", "iana-if-type:ethernetCsmacd", true, "192.168.10.4", 24));
        save(new NetworkInterface("Loopback0", "Management Interface", "iana-if-type:softwareLoopback", true, "10.0.0.1", 32));
    }

    @Override
    public void clear() {
        store.clear();
    }
}
