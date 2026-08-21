package com.mock.controller;

import com.mock.service.InterfaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InterfaceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InterfaceService interfaceService;

    private String basicAuthHeader;

    @BeforeEach
    void setUp() {
        interfaceService.resetData();
        String credentials = "admin:admin";
        basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when auth header is missing or invalid")
    void testAuthenticationRequired() throws Exception {
        mockMvc.perform(get("/restconf/data/ietf-interfaces:interfaces"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));

        mockMvc.perform(get("/restconf/data/ietf-interfaces:interfaces")
                        .header(HttpHeaders.AUTHORIZATION, "Basic invalidbase64!"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should list all interfaces with 200 OK and valid Basic Auth")
    void testListInterfaces() throws Exception {
        mockMvc.perform(get("/restconf/data/ietf-interfaces:interfaces")
                        .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
                        .accept("application/yang-data+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['ietf-interfaces:interfaces'].interface", hasSize(5)))
                .andExpect(jsonPath("$.['ietf-interfaces:interfaces'].interface[0].name", notNullValue()));
    }

    @Test
    @DisplayName("Should get single interface by name")
    void testGetInterfaceByName() throws Exception {
        mockMvc.perform(get("/restconf/data/ietf-interfaces:interfaces/interface=GigabitEthernet2")
                        .header(HttpHeaders.AUTHORIZATION, basicAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['ietf-interfaces:interface'].name", is("GigabitEthernet2")))
                .andExpect(jsonPath("$.['ietf-interfaces:interface'].description", is("Camera 2 - Entrance")))
                .andExpect(jsonPath("$.['ietf-interfaces:interface'].enabled", is(true)));
    }

    @Test
    @DisplayName("Should return 404 Not Found when interface does not exist")
    void testGetInterfaceNotFound() throws Exception {
        mockMvc.perform(get("/restconf/data/ietf-interfaces:interfaces/interface=NoSuchPort")
                        .header(HttpHeaders.AUTHORIZATION, basicAuthHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("Should create a new interface with 201 Created and Location header")
    void testCreateInterface() throws Exception {
        String payload = """
                {
                    "ietf-interfaces:interface": {
                        "name": "Loopback101",
                        "description": "Integration Test Loopback",
                        "type": "iana-if-type:softwareLoopback",
                        "enabled": true,
                        "ipAddress": "172.16.1.1",
                        "prefixLength": 24
                    }
                }
                """;

        mockMvc.perform(post("/restconf/data/ietf-interfaces:interfaces")
                        .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
                        .contentType("application/yang-data+json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("Loopback101")))
                .andExpect(jsonPath("$.message", is("Interface created")))
                .andExpect(jsonPath("$.data.['ietf-interfaces:interface'].name", is("Loopback101")));
    }

    @Test
    @DisplayName("Should return 409 Conflict when creating duplicate interface")
    void testCreateDuplicateConflict() throws Exception {
        String payload = """
                {
                    "name": "GigabitEthernet1",
                    "type": "iana-if-type:ethernetCsmacd"
                }
                """;

        mockMvc.perform(post("/restconf/data/ietf-interfaces:interfaces")
                        .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("CONFLICT")));
    }

    @Test
    @DisplayName("Should update interface with 200 OK")
    void testUpdateInterface() throws Exception {
        String payload = """
                {
                    "name": "GigabitEthernet2",
                    "description": "Updated Entrance Cam",
                    "type": "iana-if-type:ethernetCsmacd",
                    "enabled": false,
                    "ipAddress": "192.168.10.25",
                    "prefixLength": 24
                }
                """;

        mockMvc.perform(put("/restconf/data/ietf-interfaces:interfaces/interface=GigabitEthernet2")
                        .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
                        .contentType("application/yang-data+json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Interface updated")))
                .andExpect(jsonPath("$.data.['ietf-interfaces:interface'].description", is("Updated Entrance Cam")))
                .andExpect(jsonPath("$.data.['ietf-interfaces:interface'].enabled", is(false)));
    }

    @Test
    @DisplayName("Should patch interface enabled state (shutdown / power-cycle simulation)")
    void testPatchInterface() throws Exception {
        String patchPayload = """
                {
                    "enabled": false
                }
                """;

        mockMvc.perform(patch("/restconf/data/ietf-interfaces:interfaces/interface=GigabitEthernet3")
                        .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
                        .contentType("application/yang-data+json")
                        .content(patchPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Interface patched")))
                .andExpect(jsonPath("$.data.['ietf-interfaces:interface'].enabled", is(false)));
    }

    @Test
    @DisplayName("Should delete interface with 200 OK")
    void testDeleteInterface() throws Exception {
        mockMvc.perform(delete("/restconf/data/ietf-interfaces:interfaces/interface=GigabitEthernet4")
                        .header(HttpHeaders.AUTHORIZATION, basicAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Interface deleted")));

        // Verify 404 on subsequent get
        mockMvc.perform(get("/restconf/data/ietf-interfaces:interfaces/interface=GigabitEthernet4")
                        .header(HttpHeaders.AUTHORIZATION, basicAuthHeader))
                .andExpect(status().isNotFound());
    }
}
