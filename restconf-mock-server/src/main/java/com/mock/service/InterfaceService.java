package com.mock.service;

import com.mock.model.NetworkInterface;

import java.util.List;
import java.util.Map;

public interface InterfaceService {
    NetworkInterface createInterface(NetworkInterface networkInterface);
    NetworkInterface getInterface(String name);
    List<NetworkInterface> listInterfaces(String search, Boolean enabled);
    NetworkInterface updateInterface(String name, NetworkInterface networkInterface);
    NetworkInterface patchInterface(String name, Map<String, Object> updates);
    void deleteInterface(String name);
    void resetData();
    long count();
}
