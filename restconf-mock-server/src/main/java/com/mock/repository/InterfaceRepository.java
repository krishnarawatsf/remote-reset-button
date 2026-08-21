package com.mock.repository;

import com.mock.model.NetworkInterface;

import java.util.List;
import java.util.Optional;

public interface InterfaceRepository {
    NetworkInterface save(NetworkInterface networkInterface);
    Optional<NetworkInterface> findByName(String name);
    List<NetworkInterface> findAll();
    List<NetworkInterface> search(String keyword, Boolean enabled);
    boolean deleteByName(String name);
    boolean existsByName(String name);
    long count();
    void resetDefaults();
    void clear();
}
