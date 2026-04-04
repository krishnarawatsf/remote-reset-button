# Java RESTCONF Implementation Reference

Complete reference for the Java capstone project using OkHttp + Gson + JUnit 5.

## pom.xml Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## RestconfClient.java

```java
package com.restconf.client;

import com.google.gson.*;
import com.restconf.model.NetworkInterface;
import okhttp3.*;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class RestconfClient {
    private static final String YANG_JSON = "application/yang-data+json";
    private static final String YANG_KEY  = "ietf-interfaces:interface";
    private static final String BASE_PATH =
        "/restconf/data/ietf-interfaces:interfaces";

    private final String      baseUrl;
    private final String      authHeader;
    private final OkHttpClient http;
    private final Gson        gson;

    public RestconfClient(String baseUrl, String username, String password) {
        this.baseUrl    = baseUrl;
        this.authHeader = "Basic " + Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes());
        this.http = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS).build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // CREATE – POST
    public CrudResponse createInterface(NetworkInterface iface) {
        String body = wrap(iface);
        Request req = new Request.Builder()
            .url(baseUrl + BASE_PATH)
            .header("Authorization", authHeader)
            .header("Content-Type", YANG_JSON)
            .header("Accept", YANG_JSON)
            .post(RequestBody.create(body, MediaType.parse(YANG_JSON)))
            .build();
        return execute(req);
    }

    // READ – GET
    public CrudResponse readInterface(String name) {
        Request req = new Request.Builder()
            .url(baseUrl + BASE_PATH + "/interface=" + encode(name))
            .header("Authorization", authHeader)
            .header("Accept", YANG_JSON)
            .get().build();
        return execute(req);
    }

    // UPDATE – PATCH (partial update: only 'enabled' field changes)
    public CrudResponse patchEnabled(String name, boolean enabled) {
        JsonObject inner = new JsonObject();
        inner.addProperty("name", name);
        inner.addProperty("enabled", enabled);
        JsonObject outer = new JsonObject();
        outer.add(YANG_KEY, inner);
        String body = gson.toJson(outer);

        Request req = new Request.Builder()
            .url(baseUrl + BASE_PATH + "/interface=" + encode(name))
            .header("Authorization", authHeader)
            .header("Content-Type", YANG_JSON)
            .header("Accept", YANG_JSON)
            .method("PATCH", RequestBody.create(body, MediaType.parse(YANG_JSON)))
            .build();
        return execute(req);
    }

    // DELETE
    public CrudResponse deleteInterface(String name) {
        Request req = new Request.Builder()
            .url(baseUrl + BASE_PATH + "/interface=" + encode(name))
            .header("Authorization", authHeader)
            .header("Accept", YANG_JSON)
            .delete().build();
        return execute(req);
    }

    private CrudResponse execute(Request req) {
        try (Response r = http.newCall(req).execute()) {
            int    code = r.code();
            String body = r.body() != null ? r.body().string() : "";
            return new CrudResponse(code, body, code >= 200 && code < 300);
        } catch (IOException e) {
            return new CrudResponse(-1, e.getMessage(), false);
        }
    }

    private String wrap(NetworkInterface iface) {
        JsonObject outer = new JsonObject();
        outer.add(YANG_KEY, gson.toJsonTree(iface).getAsJsonObject());
        return gson.toJson(outer);
    }

    private static String encode(String key) {
        return key.replace("/", "%2F");
    }
}
```

## CrudResponse.java

```java
package com.restconf.client;

public class CrudResponse {
    private final int    statusCode;
    private final String body;
    private final boolean success;

    public CrudResponse(int statusCode, String body, boolean success) {
        this.statusCode = statusCode;
        this.body       = body;
        this.success    = success;
    }

    public int     getStatusCode() { return statusCode; }
    public String  getBody()       { return body; }
    public boolean isSuccess()     { return success; }
}
```

## NetworkInterface.java

```java
package com.restconf.model;

public class NetworkInterface {
    private String  name;
    private String  description;
    private String  type;
    private boolean enabled;
    private String  ipAddress;
    private int     prefixLength;

    public NetworkInterface() {}

    public NetworkInterface(String name, String description, String type,
                            boolean enabled, String ipAddress, int prefixLength) {
        this.name = name; this.description = description;
        this.type = type; this.enabled = enabled;
        this.ipAddress = ipAddress; this.prefixLength = prefixLength;
    }

    // getters/setters for all fields ...
    public String  getName()        { return name; }
    public boolean isEnabled()      { return enabled; }
    public void    setEnabled(boolean e) { this.enabled = e; }
    // add remaining getters/setters following the same pattern
}
```

## JUnit 5 Test Pattern

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RestconfCrudTest {
    private static RestconfClient client;

    @BeforeAll
    static void setup() {
        // Start mock server first, then:
        client = new RestconfClient("http://localhost:8080", "admin", "admin");
    }

    @Test @Order(1)
    @DisplayName("CREATE should return 201")
    void testCreate() {
        NetworkInterface iface = new NetworkInterface(
            "Loopback1", "Test", "iana-if-type:softwareLoopback",
            true, "10.0.0.1", 32);
        assertEquals(201, client.createInterface(iface).getStatusCode());
    }

    @Test @Order(2)
    @DisplayName("READ should return 200 with interface data")
    void testRead() {
        CrudResponse r = client.readInterface("Loopback1");
        assertEquals(200, r.getStatusCode());
        assertTrue(r.getBody().contains("Loopback1"));
    }

    @Test @Order(3)
    @DisplayName("PATCH enabled=false should return 204")
    void testPatch() {
        assertEquals(204, client.patchEnabled("Loopback1", false).getStatusCode());
    }

    @Test @Order(4)
    @DisplayName("DELETE should return 204 and subsequent GET should return 404")
    void testDelete() {
        assertEquals(204, client.deleteInterface("Loopback1").getStatusCode());
        assertEquals(404, client.readInterface("Loopback1").getStatusCode());
    }
}
```

## Build & Run

```bash
# Build
mvn clean package -q

# Run demo
java -jar target/restconf-crud-1.0-SNAPSHOT.jar

# Run tests
mvn test
```
