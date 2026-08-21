package com.restconf;

import com.restconf.client.CrudResponse;
import com.restconf.client.RestconfClient;
import com.restconf.model.NetworkInterface;

public class Main {
    public static void main(String[] args) {
        RestconfClient client = new RestconfClient(
                "http://localhost:8080",
                "admin",
                "admin"
        );

        NetworkInterface iface = new NetworkInterface(
                "Loopback100",
                "Demo interface",
                "iana-if-type:softwareLoopback",
                true,
                "10.10.10.1",
                32
        );

        System.out.println("===== CREATE (POST) =====");
        CrudResponse createRes = client.createInterface(iface);
        printResponse(createRes);

        System.out.println("\n===== READ (GET) =====");
        CrudResponse getRes = client.getInterface("Loopback100");
        printResponse(getRes);

        System.out.println("\n===== UPDATE (PUT) =====");
        iface.setDescription("Updated loopback interface");
        iface.setEnabled(false);
        CrudResponse updateRes = client.updateInterface("Loopback100", iface);
        printResponse(updateRes);

        System.out.println("\n===== DELETE (DELETE) =====");
        CrudResponse deleteRes = client.deleteInterface("Loopback100");
        printResponse(deleteRes);
    }

    private static void printResponse(CrudResponse response) {
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Success: " + response.isSuccess());
        System.out.println("Body: " + response.getBody());
    }
}
