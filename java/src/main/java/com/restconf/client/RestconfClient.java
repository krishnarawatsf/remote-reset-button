package com.restconf.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.restconf.model.NetworkInterface;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Production-ready HTTP RESTCONF Client supporting full CRUD, Search, and PATCH
 * over the standard IETF Interfaces YANG data model (RFC 8040 / RFC 8343).
 */
public class RestconfClient {

    public static final String YANG_JSON = "application/yang-data+json";
    public static final String BASE_PATH = "/restconf/data/ietf-interfaces:interfaces";

    private final String baseUrl;
    private final String authHeader;
    private final OkHttpClient client;
    private final Gson gson;

    public RestconfClient(String baseUrl, String username, String password) {
        this(baseUrl, username, password, Duration.ofSeconds(10));
    }

    public RestconfClient(String baseUrl, String username, String password, Duration timeout) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();
        this.gson = new Gson();
    }

    public RestconfClient(String baseUrl, String username, String password, OkHttpClient customClient) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.client = customClient;
        this.gson = new Gson();
    }

    /**
     * CREATE: Add a new network interface configuration.
     */
    public CrudResponse createInterface(NetworkInterface iface) {
        JsonObject outer = new JsonObject();
        outer.add("ietf-interfaces:interface", gson.toJsonTree(iface));

        Request request = new Request.Builder()
                .url(baseUrl + BASE_PATH)
                .header("Authorization", authHeader)
                .header("Accept", YANG_JSON)
                .header("Content-Type", YANG_JSON)
                .post(RequestBody.create(gson.toJson(outer), MediaType.get(YANG_JSON)))
                .build();

        return execute(request);
    }

    /**
     * READ ONE: Retrieve single interface configuration by name.
     */
    public CrudResponse getInterface(String name) {
        Request request = new Request.Builder()
                .url(baseUrl + BASE_PATH + "/interface=" + encodePathSegment(name))
                .header("Authorization", authHeader)
                .header("Accept", YANG_JSON)
                .get()
                .build();

        return execute(request);
    }

    /**
     * READ ALL: Retrieve all configured interfaces.
     */
    public CrudResponse listInterfaces() {
        Request request = new Request.Builder()
                .url(baseUrl + BASE_PATH)
                .header("Authorization", authHeader)
                .header("Accept", YANG_JSON)
                .get()
                .build();

        return execute(request);
    }

    /**
     * SEARCH: Query interfaces by keyword or enabled status.
     */
    public CrudResponse searchInterfaces(String keyword, Boolean enabled) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + BASE_PATH).newBuilder();
        if (keyword != null && !keyword.isBlank()) {
            urlBuilder.addQueryParameter("search", keyword);
        }
        if (enabled != null) {
            urlBuilder.addQueryParameter("enabled", String.valueOf(enabled));
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("Authorization", authHeader)
                .header("Accept", YANG_JSON)
                .get()
                .build();

        return execute(request);
    }

    /**
     * UPDATE (REPLACE): Full resource replacement (PUT).
     */
    public CrudResponse updateInterface(String name, NetworkInterface iface) {
        JsonObject outer = new JsonObject();
        outer.add("ietf-interfaces:interface", gson.toJsonTree(iface));

        Request request = new Request.Builder()
                .url(baseUrl + BASE_PATH + "/interface=" + encodePathSegment(name))
                .header("Authorization", authHeader)
                .header("Accept", YANG_JSON)
                .header("Content-Type", YANG_JSON)
                .put(RequestBody.create(gson.toJson(outer), MediaType.get(YANG_JSON)))
                .build();

        return execute(request);
    }

    /**
     * PARTIAL UPDATE: Selectively modify interface attributes (PATCH).
     */
    public CrudResponse patchInterface(String name, Map<String, Object> delta) {
        JsonObject outer = new JsonObject();
        outer.add("ietf-interfaces:interface", gson.toJsonTree(delta));

        Request request = new Request.Builder()
                .url(baseUrl + BASE_PATH + "/interface=" + encodePathSegment(name))
                .header("Authorization", authHeader)
                .header("Accept", YANG_JSON)
                .header("Content-Type", YANG_JSON)
                .patch(RequestBody.create(gson.toJson(outer), MediaType.get(YANG_JSON)))
                .build();

        return execute(request);
    }

    /**
     * DELETE: Remove interface resource.
     */
    public CrudResponse deleteInterface(String name) {
        Request request = new Request.Builder()
                .url(baseUrl + BASE_PATH + "/interface=" + encodePathSegment(name))
                .header("Authorization", authHeader)
                .header("Accept", YANG_JSON)
                .delete()
                .build();

        return execute(request);
    }

    /**
     * Helper to deserialize single interface from response body.
     */
    public NetworkInterface parseInterface(String jsonBody) {
        if (jsonBody == null || jsonBody.isBlank()) return null;
        try {
            JsonObject obj = gson.fromJson(jsonBody, JsonObject.class);
            if (obj.has("ietf-interfaces:interface")) {
                return gson.fromJson(obj.get("ietf-interfaces:interface"), NetworkInterface.class);
            } else if (obj.has("data") && obj.getAsJsonObject("data").has("ietf-interfaces:interface")) {
                return gson.fromJson(obj.getAsJsonObject("data").get("ietf-interfaces:interface"), NetworkInterface.class);
            }
            return gson.fromJson(obj, NetworkInterface.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Helper to deserialize list of interfaces from response body.
     */
    public List<NetworkInterface> parseInterfaceList(String jsonBody) {
        List<NetworkInterface> list = new ArrayList<>();
        if (jsonBody == null || jsonBody.isBlank()) return list;
        try {
            JsonObject obj = gson.fromJson(jsonBody, JsonObject.class);
            if (obj.has("ietf-interfaces:interfaces")) {
                JsonObject inner = obj.getAsJsonObject("ietf-interfaces:interfaces");
                if (inner.has("interface") && inner.get("interface").isJsonArray()) {
                    JsonArray arr = inner.getAsJsonArray("interface");
                    for (JsonElement el : arr) {
                        list.add(gson.fromJson(el, NetworkInterface.class));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private CrudResponse execute(Request request) {
        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            return new CrudResponse(response.code(), body, response.isSuccessful());
        } catch (IOException e) {
            return new CrudResponse(-1, "Network error: " + e.getMessage(), false);
        }
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
