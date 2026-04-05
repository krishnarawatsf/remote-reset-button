package com.remote.resetbutton.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.remote.resetbutton.RestconfProperties;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class RestconfClient {

    public static final String YANG_JSON = "application/yang-data+json";
    public static final String INTERFACES_PATH = "/restconf/data/ietf-interfaces:interfaces";
    public static final String INTERFACE_PATH = "/restconf/data/ietf-interfaces:interfaces/interface=%s";

    private final RestconfProperties properties;
    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public RestconfClient(RestconfProperties properties) {
        this.properties = properties;
        this.client = buildClient(properties);
    }

    public JsonObject getInterfaces() throws IOException {
        Request request = baseRequest(INTERFACES_PATH)
            .get()
            .build();
        return executeJson(request);
    }

    public JsonObject getInterface(String name) throws IOException {
        Request request = baseRequest(interfacePath(name))
            .get()
            .build();
        JsonObject root = executeJson(request);
        return extractSingleInterface(root);
    }

    public int patchEnabled(String name, boolean enabled) throws IOException {
        JsonObject wrapper = new JsonObject();
        JsonObject inner = new JsonObject();
        inner.addProperty("name", name);
        inner.addProperty("enabled", enabled);
        wrapper.add("ietf-interfaces:interface", inner);

        Request request = baseRequest(interfacePath(name))
            .patch(RequestBody.create(gson.toJson(wrapper), MediaType.get(YANG_JSON)))
            .build();

        try (Response response = client.newCall(request).execute()) {
            return response.code();
        }
    }

    public List<JsonObject> allInterfaces() throws IOException {
        JsonObject root = getInterfaces();
        List<JsonObject> interfaces = new ArrayList<>();

        JsonObject container = getInterfaceContainer(root);
        if (container == null) {
            return interfaces;
        }

        JsonElement element = container.get("interface");
        if (element == null || !element.isJsonArray()) {
            return interfaces;
        }

        JsonArray array = element.getAsJsonArray();
        for (JsonElement entry : array) {
            if (entry.isJsonObject()) {
                interfaces.add(entry.getAsJsonObject());
            }
        }
        return interfaces;
    }

    private JsonObject executeJson(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            return gson.fromJson(response.body().charStream(), JsonObject.class);
        }
    }

    private Request.Builder baseRequest(String path) {
        return new Request.Builder()
            .url(properties.baseUrl() + path)
            .header("Authorization", "Basic " + credentials())
            .header("Accept", YANG_JSON)
            .header("Content-Type", YANG_JSON);
    }

    private String credentials() {
        return Base64.getEncoder().encodeToString(
            (properties.getUsername() + ":" + properties.getPassword()).getBytes()
        );
    }

    private String interfacePath(String name) {
        return String.format(INTERFACE_PATH, encodeInterfaceName(name));
    }

    private static String encodeInterfaceName(String name) {
        return name.replace("/", "%2F");
    }

    private static JsonObject getInterfaceContainer(JsonObject root) {
        if (root == null) {
            return null;
        }
        if (root.has("ietf-interfaces:interfaces")) {
            return root.getAsJsonObject("ietf-interfaces:interfaces");
        }
        if (root.has("interfaces")) {
            return root.getAsJsonObject("interfaces");
        }
        return null;
    }

    private static JsonObject extractSingleInterface(JsonObject root) {
        if (root == null) {
            return null;
        }

        if (root.has("ietf-interfaces:interface")) {
            JsonElement element = root.get("ietf-interfaces:interface");
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
            if (element.isJsonArray() && !element.getAsJsonArray().isEmpty()) {
                JsonElement first = element.getAsJsonArray().get(0);
                if (first.isJsonObject()) {
                    return first.getAsJsonObject();
                }
            }
        }

        if (root.has("interface")) {
            JsonElement element = root.get("interface");
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
            if (element.isJsonArray() && !element.getAsJsonArray().isEmpty()) {
                JsonElement first = element.getAsJsonArray().get(0);
                if (first.isJsonObject()) {
                    return first.getAsJsonObject();
                }
            }
        }

        return null;
    }

    private static OkHttpClient buildClient(RestconfProperties properties) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (properties.isUseHttps() && properties.isSkipSslVerification()) {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};

            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new SecureRandom());
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                X509TrustManager trustManager = (X509TrustManager) trustAllCerts[0];

                builder.sslSocketFactory(sslSocketFactory, trustManager);
                builder.hostnameVerifier((HostnameVerifier) (hostname, session) -> true);
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to configure SSL trust manager", exception);
            }
        }

        return builder.build();
    }
}