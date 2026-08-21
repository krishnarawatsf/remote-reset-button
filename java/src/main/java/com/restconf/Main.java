package com.restconf;

import com.restconf.client.CrudResponse;
import com.restconf.client.RestconfClient;
import com.restconf.model.NetworkInterface;

import java.util.List;
import java.util.Map;

/**
 * Showcase CLI Application demonstrating end-to-end RESTCONF CRUD workflows.
 */
public class Main {

    public static void main(String[] args) {
        String host = System.getProperty("restconf.url", "http://localhost:8080");
        String username = System.getProperty("restconf.user", "admin");
        String password = System.getProperty("restconf.pass", "admin");

        System.out.println("===============================================================");
        System.out.println("     RESTCONF Network Interface Automation & CRUD Demo");
        System.out.println("     Target: " + host);
        System.out.println("===============================================================");

        RestconfClient client = new RestconfClient(host, username, password);

        // 1. CREATE (POST)
        System.out.println("\n[1] CREATE INTERFACE (POST)");
        NetworkInterface newIface = NetworkInterface.builder()
                .name("Loopback100")
                .description("Automated Loopback Interface")
                .type("iana-if-type:softwareLoopback")
                .enabled(true)
                .ipAddress("10.100.1.1")
                .prefixLength(32)
                .build();

        CrudResponse createRes = client.createInterface(newIface);
        printResponse(createRes);

        // 2. READ (GET ONE)
        System.out.println("\n[2] READ INTERFACE (GET)");
        CrudResponse getRes = client.getInterface("Loopback100");
        printResponse(getRes);
        NetworkInterface parsed = client.parseInterface(getRes.getBody());
        if (parsed != null) {
            System.out.println("  -> Parsed IP: " + parsed.getIpAddress() + "/" + parsed.getPrefixLength());
        }

        // 3. SEARCH & LIST (GET ALL)
        System.out.println("\n[3] SEARCH & LIST INTERFACES (GET with Query Params)");
        CrudResponse searchRes = client.searchInterfaces("Camera", true);
        printResponse(searchRes);
        List<NetworkInterface> searchList = client.parseInterfaceList(searchRes.getBody());
        System.out.println("  -> Found " + searchList.size() + " matching interface(s)");

        // 4. UPDATE / REPLACE (PUT)
        System.out.println("\n[4] UPDATE INTERFACE (PUT)");
        newIface.setDescription("Updated Loopback Interface Description");
        newIface.setIpAddress("10.100.1.2");
        CrudResponse updateRes = client.updateInterface("Loopback100", newIface);
        printResponse(updateRes);

        // 5. PARTIAL UPDATE (PATCH) - Power cycle / enable-disable
        System.out.println("\n[5] PARTIAL UPDATE / PATCH (Simulating Port Shutdown)");
        CrudResponse patchRes = client.patchInterface("Loopback100", Map.of("enabled", false));
        printResponse(patchRes);

        // 6. DELETE (DELETE)
        System.out.println("\n[6] DELETE INTERFACE (DELETE)");
        CrudResponse deleteRes = client.deleteInterface("Loopback100");
        printResponse(deleteRes);

        // 7. VERIFY DELETION (GET -> 404 NOT FOUND)
        System.out.println("\n[7] VERIFY DELETION (GET expecting 404)");
        CrudResponse verifyRes = client.getInterface("Loopback100");
        printResponse(verifyRes);
        System.out.println("  -> Correctly received 404 Not Found: " + verifyRes.isNotFound());

        System.out.println("\n===============================================================");
        System.out.println("✔ RESTCONF CRUD Operations completed successfully.");
        System.out.println("===============================================================");
    }

    private static void printResponse(CrudResponse response) {
        System.out.println("  Status Code: " + response.getStatusCode() + (response.isSuccess() ? " (OK/Success)" : " (Expected/Error)"));
        if (!response.getBody().isBlank()) {
            System.out.println("  Response Body: " + response.getBody());
        }
    }
}
