package com.remote.resetbutton.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.remote.resetbutton.RestconfProperties;
import com.remote.resetbutton.client.RestconfClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class InterfaceService {

    private final RestconfClient client;
    private final RestconfProperties properties;

    public InterfaceService(RestconfProperties properties) {
        this.properties = properties;
        this.client = new RestconfClient(properties);
    }

    public List<JsonObject> searchInterfaces(String searchTerm) throws IOException {
        List<JsonObject> interfaces = client.allInterfaces();
        List<JsonObject> matches = new ArrayList<>();
        String term = searchTerm.toLowerCase();

        for (JsonObject iface : interfaces) {
            String name = stringValue(iface, "name");
            String description = stringValue(iface, "description");
            if (name.toLowerCase().contains(term) || description.toLowerCase().contains(term)) {
                matches.add(iface);
            }
        }

        return matches;
    }

    public JsonObject getInterface(String name) throws IOException {
        return client.getInterface(name);
    }

    public int rebootInterface(String name) throws IOException, InterruptedException {
        JsonObject before = client.getInterface(name);
        if (before == null) {
            return 404;
        }

        int disableStatus = client.patchEnabled(name, false);
        if (disableStatus < 200 || disableStatus >= 300) {
            return disableStatus;
        }

        Thread.sleep(properties.getRebootWaitSeconds() * 1000L);
        return client.patchEnabled(name, true);
    }

    public String baseUrl() {
        return properties.baseUrl();
    }

    private static String stringValue(JsonObject object, String property) {
        JsonElement element = object.get(property);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }
}