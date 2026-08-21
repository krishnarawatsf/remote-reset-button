package com.restconf.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.restconf.model.NetworkInterface;
import okhttp3.*;

import java.io.IOException;
import java.util.Base64;

public class RestconfClient {

    private static final String YANG_JSON = "application/yang-data+json";
    private static final String BASE_PATH = "/restconf/data/ietf-interfaces:interfaces";

    private final String baseUrl;
    private final String authHeader;
    private final OkHttpClient client;
    private final Gson gson;

    public RestconfClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

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

    public CrudResponse getInterface(String name) {
        Request request = new Request.Builder()
                .url(baseUrl + BASE_PATH + "/interface=" + encode(name))
                .header("Authorization", authHeader)
                .header("Accept", YANG_JSON)
                .get()
                .build();

        return execute(request);
    }

    public CrudResponse updateInterface(String name, NetworkInterface iface) {
        JsonObject outer = new JsonObject();
        outer.add("ietf-interfaces:interface", gson.toJsonTree(iface));

        Request request = new Request.Builder()
                .url(baseUrl + BASE_PATH + "/interface=" + encode(name))
                .header("Authorization", authHeader)
                .header("Accept", YANG_JSON)
                .header("Content-Type", YANG_JSON)
                .put(RequestBody.create(gson.toJson(outer), MediaType.get(YANG_JSON)))
                .build();

        return execute(request);
    }

    public CrudResponse deleteInterface(String name) {
        Request request = new Request.Builder()
                .url(baseUrl + BASE_PATH + "/interface=" + encode(name))
                .header("Authorization", authHeader)
                .header("Accept", YANG_JSON)
                .delete()
                .build();

        return execute(request);
    }

    private CrudResponse execute(Request request) {
        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            return new CrudResponse(response.code(), body, response.isSuccessful());
        } catch (IOException e) {
            return new CrudResponse(-1, e.getMessage(), false);
        }
    }

    private String encode(String value) {
        return value.replace("/", "%2F");
    }
}
