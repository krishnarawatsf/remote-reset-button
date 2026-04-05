package com.remote.resetbutton.cli;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.remote.resetbutton.service.InterfaceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class RestconfCliRunner implements CommandLineRunner {

    private final InterfaceService service;

    public RestconfCliRunner(InterfaceService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0].trim().toLowerCase();
        String value = joinArgs(args, 1);

        switch (command) {
            case "find" -> runFind(value);
            case "reboot" -> runReboot(value);
            default -> printUsage();
        }
    }

    private void runFind(String searchTerm) throws IOException {
        if (searchTerm.isBlank()) {
            printUsage();
            return;
        }

        List<JsonObject> matches = service.searchInterfaces(searchTerm);
        System.out.println("Searching for: " + searchTerm);
        System.out.println("Target device: " + service.baseUrl());

        if (matches.isEmpty()) {
            System.out.println("No interface found matching \"" + searchTerm + "\".");
            return;
        }

        System.out.println("Found " + matches.size() + " match(es):");
        for (JsonObject match : matches) {
            printInterface(match);
        }
    }

    private void runReboot(String interfaceName) throws IOException, InterruptedException {
        if (interfaceName.isBlank()) {
            printUsage();
            return;
        }

        JsonObject before = service.getInterface(interfaceName);
        if (before == null) {
            System.out.println("Interface not found: " + interfaceName);
            return;
        }

        System.out.println("Target interface: " + interfaceName);
        System.out.println("Device: " + service.baseUrl());
        printState("BEFORE", before);

        int status = service.rebootInterface(interfaceName);
        if (status >= 200 && status < 300) {
            JsonObject after = service.getInterface(interfaceName);
            if (after != null) {
                printState("AFTER", after);
            }
            System.out.println("Power cycle complete for " + interfaceName + ".");
        } else {
            System.out.println("Failed to reboot interface. HTTP " + status);
        }
    }

    private static void printInterface(JsonObject iface) {
        System.out.println("- Name: " + stringValue(iface, "name"));
        System.out.println("  Description: " + stringValue(iface, "description"));
        System.out.println("  Status: " + (booleanValue(iface, "enabled") ? "UP" : "DOWN"));
        System.out.println("  IP: " + interfaceIp(iface));
    }

    private static void printState(String label, JsonObject iface) {
        System.out.println(label + ": " + stringValue(iface, "name") + " enabled=" + booleanValue(iface, "enabled"));
    }

    private static String interfaceIp(JsonObject iface) {
        if (iface.has("ipv4") && iface.get("ipv4").isJsonObject()) {
            JsonObject ipv4 = iface.getAsJsonObject("ipv4");
            if (ipv4.has("address") && ipv4.get("address").isJsonArray()) {
                JsonElement first = ipv4.getAsJsonArray("address").size() > 0 ? ipv4.getAsJsonArray("address").get(0) : null;
                if (first != null && first.isJsonObject()) {
                    JsonObject address = first.getAsJsonObject();
                    String ip = stringValue(address, "ip");
                    String prefix = stringValue(address, "prefix-length");
                    if (!prefix.isBlank()) {
                        return ip + "/" + prefix;
                    }
                    return ip;
                }
            }
        }

        String ip = stringValue(iface, "ipAddress");
        String prefix = stringValue(iface, "prefixLength");
        return prefix.isBlank() ? ip : ip + "/" + prefix;
    }

    private static String stringValue(JsonObject object, String property) {
        JsonElement element = object.get(property);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static boolean booleanValue(JsonObject object, String property) {
        JsonElement element = object.get(property);
        return element != null && !element.isJsonNull() && element.getAsBoolean();
    }

    private static String joinArgs(String[] args, int startIndex) {
        if (args.length <= startIndex) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = startIndex; index < args.length; index++) {
            if (index > startIndex) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        return builder.toString();
    }

    private static void printUsage() {
        System.out.println("Usage: mvn spring-boot:run -Dspring-boot.run.arguments=\"find Camera 2\"");
        System.out.println("   or: mvn spring-boot:run -Dspring-boot.run.arguments=\"reboot GigabitEthernet2\"");
        System.out.println("   or: java -jar target/restconf-crud-1.0.0.jar find Camera 2");
    }
}