package com.restconf.client;

import com.restconf.model.NetworkInterface;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RestconfClientTest {

    private MockWebServer mockWebServer;
    private RestconfClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        client = new RestconfClient(baseUrl, "admin", "admin");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should send POST request with yang-data+json header and authorization")
    void testCreateInterface() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody("{\"message\":\"Interface created\"}"));

        NetworkInterface iface = new NetworkInterface("GigabitEthernet0/1", "Uplink", "iana-if-type:ethernetCsmacd", true, "10.0.0.1", 24);
        CrudResponse response = client.createInterface(iface);

        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.isSuccess()).isTrue();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/restconf/data/ietf-interfaces:interfaces");
        assertThat(request.getHeader("Authorization")).startsWith("Basic ");
        assertThat(request.getHeader("Content-Type")).startsWith("application/yang-data+json");
        assertThat(request.getHeader("Accept")).isEqualTo("application/yang-data+json");
        assertThat(request.getBody().readUtf8()).contains("GigabitEthernet0/1");
    }

    @Test
    @DisplayName("Should send GET request with encoded path")
    void testGetInterface() throws InterruptedException {
        String responseBody = """
                {
                    "ietf-interfaces:interface": {
                        "name": "GigabitEthernet0/1",
                        "description": "Uplink",
                        "type": "iana-if-type:ethernetCsmacd",
                        "enabled": true,
                        "ipAddress": "10.0.0.1",
                        "prefixLength": 24
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody));

        CrudResponse response = client.getInterface("GigabitEthernet0/1");
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.isSuccess()).isTrue();

        NetworkInterface iface = client.parseInterface(response.getBody());
        assertThat(iface).isNotNull();
        assertThat(iface.getName()).isEqualTo("GigabitEthernet0/1");
        assertThat(iface.getIpAddress()).isEqualTo("10.0.0.1");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/restconf/data/ietf-interfaces:interfaces/interface=GigabitEthernet0%2F1");
    }

    @Test
    @DisplayName("Should search interfaces with query parameters")
    void testSearchInterfaces() throws InterruptedException {
        String responseBody = """
                {
                    "ietf-interfaces:interfaces": {
                        "interface": [
                            {
                                "name": "GigabitEthernet2",
                                "description": "Camera 2",
                                "type": "iana-if-type:ethernetCsmacd",
                                "enabled": true
                            }
                        ]
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody));

        CrudResponse response = client.searchInterfaces("Camera", true);
        assertThat(response.isSuccess()).isTrue();

        List<NetworkInterface> list = client.parseInterfaceList(response.getBody());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("GigabitEthernet2");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("search=Camera");
        assertThat(request.getPath()).contains("enabled=true");
    }

    @Test
    @DisplayName("Should send PUT request for full update")
    void testUpdateInterface() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"message\":\"Interface updated\"}"));

        NetworkInterface updated = new NetworkInterface("Loopback0", "Updated Loopback", "iana-if-type:softwareLoopback", true, "10.0.0.1", 32);
        CrudResponse response = client.updateInterface("Loopback0", updated);

        assertThat(response.isSuccess()).isTrue();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getPath()).isEqualTo("/restconf/data/ietf-interfaces:interfaces/interface=Loopback0");
    }

    @Test
    @DisplayName("Should send PATCH request for partial update")
    void testPatchInterface() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"message\":\"Interface patched\"}"));

        CrudResponse response = client.patchInterface("GigabitEthernet2", Map.of("enabled", false));
        assertThat(response.isSuccess()).isTrue();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PATCH");
        assertThat(request.getBody().readUtf8()).contains("\"enabled\":false");
    }

    @Test
    @DisplayName("Should send DELETE request")
    void testDeleteInterface() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"message\":\"Interface deleted\"}"));

        CrudResponse response = client.deleteInterface("Loopback0");
        assertThat(response.isSuccess()).isTrue();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/restconf/data/ietf-interfaces:interfaces/interface=Loopback0");
    }

    @Test
    @DisplayName("Should handle 404 Not Found error gracefully")
    void testNotFoundError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"error\":\"NOT_FOUND\",\"message\":\"Interface not found\"}"));

        CrudResponse response = client.getInterface("MissingPort");
        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.isNotFound()).isTrue();
        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("Should handle 409 Conflict error on duplicate create")
    void testConflictError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(409)
                .setBody("{\"error\":\"CONFLICT\",\"message\":\"Already exists\"}"));

        NetworkInterface iface = new NetworkInterface("GigabitEthernet1", "Uplink", "iana-if-type:ethernetCsmacd", true, null, null);
        CrudResponse response = client.createInterface(iface);
        assertThat(response.getStatusCode()).isEqualTo(409);
        assertThat(response.isConflict()).isTrue();
        assertThat(response.isSuccess()).isFalse();
    }
}
