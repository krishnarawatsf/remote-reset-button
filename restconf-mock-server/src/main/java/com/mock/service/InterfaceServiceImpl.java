package com.mock.service;

import com.mock.exception.InterfaceAlreadyExistsException;
import com.mock.exception.InterfaceNotFoundException;
import com.mock.model.NetworkInterface;
import com.mock.repository.InterfaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InterfaceServiceImpl implements InterfaceService {

    private final InterfaceRepository repository;

    public InterfaceServiceImpl(InterfaceRepository repository) {
        this.repository = repository;
    }

    @Override
    public NetworkInterface createInterface(NetworkInterface networkInterface) {
        if (networkInterface == null || networkInterface.getName() == null) {
            throw new IllegalArgumentException("Network interface payload and name are required");
        }
        if (repository.existsByName(networkInterface.getName())) {
            throw new InterfaceAlreadyExistsException(networkInterface.getName());
        }
        return repository.save(networkInterface);
    }

    @Override
    public NetworkInterface getInterface(String name) {
        return repository.findByName(name)
                .orElseThrow(() -> new InterfaceNotFoundException(name));
    }

    @Override
    public List<NetworkInterface> listInterfaces(String search, Boolean enabled) {
        return repository.search(search, enabled);
    }

    @Override
    public NetworkInterface updateInterface(String name, NetworkInterface networkInterface) {
        if (networkInterface == null) {
            throw new IllegalArgumentException("Network interface payload cannot be null");
        }
        // Ensure the interface exists before update
        if (!repository.existsByName(name)) {
            throw new InterfaceNotFoundException(name);
        }
        networkInterface.setName(name);
        return repository.save(networkInterface);
    }

    @Override
    public NetworkInterface patchInterface(String name, Map<String, Object> updates) {
        NetworkInterface existing = getInterface(name);
        if (updates == null || updates.isEmpty()) {
            return existing;
        }

        // Support nested YANG structures or flat maps
        Map<String, Object> data = updates;
        if (updates.containsKey("ietf-interfaces:interface")) {
            Object nested = updates.get("ietf-interfaces:interface");
            if (nested instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>) nested;
                data = casted;
            }
        }

        if (data.containsKey("description")) {
            Object val = data.get("description");
            existing.setDescription(val != null ? val.toString() : null);
        }
        if (data.containsKey("enabled")) {
            Object val = data.get("enabled");
            if (val instanceof Boolean) {
                existing.setEnabled((Boolean) val);
            } else if (val != null) {
                existing.setEnabled(Boolean.parseBoolean(val.toString()));
            }
        }
        if (data.containsKey("type")) {
            Object val = data.get("type");
            if (val != null) {
                existing.setType(val.toString());
            }
        }
        if (data.containsKey("ipAddress")) {
            Object val = data.get("ipAddress");
            existing.setIpAddress(val != null ? val.toString() : null);
        }
        if (data.containsKey("prefixLength")) {
            Object val = data.get("prefixLength");
            if (val instanceof Number) {
                existing.setPrefixLength(((Number) val).intValue());
            } else if (val != null) {
                existing.setPrefixLength(Integer.parseInt(val.toString()));
            }
        }

        return repository.save(existing);
    }

    @Override
    public void deleteInterface(String name) {
        boolean removed = repository.deleteByName(name);
        if (!removed) {
            throw new InterfaceNotFoundException(name);
        }
    }

    @Override
    public void resetData() {
        repository.resetDefaults();
    }

    @Override
    public long count() {
        return repository.count();
    }
}
