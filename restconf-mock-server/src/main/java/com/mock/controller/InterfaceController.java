package com.mock.controller;

import com.mock.model.NetworkInterface;
import com.mock.service.InterfaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * RESTCONF RFC 8040 compliant controller for IETF Network Interfaces (ietf-interfaces:interfaces).
 * 
 * Supports Content-Type and Accept:
 * - application/yang-data+json
 * - application/json
 */
@RestController
@RequestMapping(
        value = "/restconf/data/ietf-interfaces:interfaces",
        produces = {"application/yang-data+json", MediaType.APPLICATION_JSON_VALUE}
)
public class InterfaceController {

    private final InterfaceService interfaceService;

    public InterfaceController(InterfaceService interfaceService) {
        this.interfaceService = interfaceService;
    }

    /**
     * CREATE: Adds a new interface to the device configuration.
     * RESTCONF Method: POST /restconf/data/ietf-interfaces:interfaces
     */
    @PostMapping(consumes = {"application/yang-data+json", MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> createInterface(@RequestBody Map<String, Object> payload) {
        NetworkInterface iface = extractInterface(payload);
        NetworkInterface created = interfaceService.createInterface(iface);
        
        URI location = URI.create("/restconf/data/ietf-interfaces:interfaces/interface=" + created.getName());
        return ResponseEntity.created(location).body(Map.of(
                "message", "Interface created",
                "data", Map.of("ietf-interfaces:interface", created)
        ));
    }

    /**
     * READ ALL / SEARCH: Retrieve all configured interfaces or filter by search keyword / status.
     * RESTCONF Method: GET /restconf/data/ietf-interfaces:interfaces
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listInterfaces(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean enabled
    ) {
        List<NetworkInterface> interfaces = interfaceService.listInterfaces(search, enabled);
        return ResponseEntity.ok(Map.of(
                "ietf-interfaces:interfaces", Map.of("interface", interfaces)
        ));
    }

    /**
     * READ ONE: Retrieve single interface configuration by name.
     * RESTCONF Method: GET /restconf/data/ietf-interfaces:interfaces/interface={name}
     */
    @GetMapping("/interface={name}")
    public ResponseEntity<Map<String, Object>> getInterface(@PathVariable String name) {
        String decodedName = decode(name);
        NetworkInterface iface = interfaceService.getInterface(decodedName);
        return ResponseEntity.ok(Map.of(
                "ietf-interfaces:interface", iface
        ));
    }

    /**
     * UPDATE (REPLACE): Full replacement of the interface resource.
     * RESTCONF Method: PUT /restconf/data/ietf-interfaces:interfaces/interface={name}
     */
    @PutMapping(value = "/interface={name}", consumes = {"application/yang-data+json", MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> updateInterface(
            @PathVariable String name,
            @RequestBody Map<String, Object> payload
    ) {
        String decodedName = decode(name);
        NetworkInterface iface = extractInterface(payload);
        NetworkInterface updated = interfaceService.updateInterface(decodedName, iface);
        return ResponseEntity.ok(Map.of(
                "message", "Interface updated",
                "data", Map.of("ietf-interfaces:interface", updated)
        ));
    }

    /**
     * PARTIAL UPDATE (PATCH): Target specific fields (e.g. power cycle port).
     * RESTCONF Method: PATCH /restconf/data/ietf-interfaces:interfaces/interface={name}
     */
    @PatchMapping(value = "/interface={name}", consumes = {"application/yang-data+json", MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> patchInterface(
            @PathVariable String name,
            @RequestBody Map<String, Object> delta
    ) {
        String decodedName = decode(name);
        NetworkInterface patched = interfaceService.patchInterface(decodedName, delta);
        return ResponseEntity.ok(Map.of(
                "message", "Interface patched",
                "data", Map.of("ietf-interfaces:interface", patched)
        ));
    }

    /**
     * DELETE: Delete interface resource.
     * RESTCONF Method: DELETE /restconf/data/ietf-interfaces:interfaces/interface={name}
     */
    @DeleteMapping("/interface={name}")
    public ResponseEntity<Map<String, Object>> deleteInterface(@PathVariable String name) {
        String decodedName = decode(name);
        interfaceService.deleteInterface(decodedName);
        return ResponseEntity.ok(Map.of(
                "message", "Interface deleted"
        ));
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    @SuppressWarnings("unchecked")
    private NetworkInterface extractInterface(Map<String, Object> payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be empty");
        }
        Map<String, Object> data = payload;
        if (payload.containsKey("ietf-interfaces:interface")) {
            Object nested = payload.get("ietf-interfaces:interface");
            if (nested instanceof Map) {
                data = (Map<String, Object>) nested;
            }
        }

        String name = data.get("name") != null ? data.get("name").toString() : null;
        String desc = data.get("description") != null ? data.get("description").toString() : null;
        String type = data.get("type") != null ? data.get("type").toString() : "iana-if-type:ethernetCsmacd";
        Boolean enabled = true;
        if (data.containsKey("enabled")) {
            Object val = data.get("enabled");
            if (val instanceof Boolean) {
                enabled = (Boolean) val;
            } else if (val != null) {
                enabled = Boolean.parseBoolean(val.toString());
            }
        }
        String ipAddress = data.get("ipAddress") != null ? data.get("ipAddress").toString() : null;
        Integer prefixLength = null;
        if (data.containsKey("prefixLength")) {
            Object val = data.get("prefixLength");
            if (val instanceof Number) {
                prefixLength = ((Number) val).intValue();
            } else if (val != null) {
                prefixLength = Integer.parseInt(val.toString());
            }
        }

        return new NetworkInterface(name, desc, type, enabled, ipAddress, prefixLength);
    }
}
