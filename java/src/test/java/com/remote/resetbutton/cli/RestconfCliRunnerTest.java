package com.remote.resetbutton.cli;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.remote.resetbutton.service.InterfaceService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestconfCliRunnerTest {

    @Test
    void runWithNoArgsShouldPrintUsage() throws Exception {
        InterfaceService service = mock(InterfaceService.class);
        RestconfCliRunner runner = new RestconfCliRunner(service);

        String output = captureStdout(() -> runner.run());

        assertTrue(output.contains("Usage: mvn spring-boot:run"));
    }

    @Test
    void findShouldPrintMatchingInterfaceDetails() throws Exception {
        InterfaceService service = mock(InterfaceService.class);
        RestconfCliRunner runner = new RestconfCliRunner(service);

        JsonObject iface = new JsonObject();
        iface.addProperty("name", "Camera 2");
        iface.addProperty("description", "Lobby Camera");
        iface.addProperty("enabled", true);
        JsonObject ipv4 = new JsonObject();
        JsonArray addresses = new JsonArray();
        JsonObject address = new JsonObject();
        address.addProperty("ip", "10.0.0.2");
        address.addProperty("prefix-length", "24");
        addresses.add(address);
        ipv4.add("address", addresses);
        iface.add("ipv4", ipv4);

        when(service.searchInterfaces("Camera 2")).thenReturn(List.of(iface));
        when(service.baseUrl()).thenReturn("http://localhost:8080");

        String output = captureStdout(() -> runner.run("find", "Camera", "2"));

        assertTrue(output.contains("Searching for: Camera 2"));
        assertTrue(output.contains("Target device: http://localhost:8080"));
        assertTrue(output.contains("Found 1 match(es):"));
        assertTrue(output.contains("- Name: Camera 2"));
        assertTrue(output.contains("Status: UP"));
        assertTrue(output.contains("IP: 10.0.0.2/24"));
    }

    @Test
    void rebootShouldReportWhenInterfaceIsMissing() throws Exception {
        InterfaceService service = mock(InterfaceService.class);
        RestconfCliRunner runner = new RestconfCliRunner(service);

        when(service.getInterface("GigabitEthernet2")).thenReturn(null);

        String output = captureStdout(() -> runner.run("reboot", "GigabitEthernet2"));

        assertTrue(output.contains("Interface not found: GigabitEthernet2"));
    }

    @Test
    void rebootShouldPrintBeforeAndAfterOnSuccess() throws Exception {
        InterfaceService service = mock(InterfaceService.class);
        RestconfCliRunner runner = new RestconfCliRunner(service);

        JsonObject before = new JsonObject();
        before.addProperty("name", "GigabitEthernet2");
        before.addProperty("enabled", true);

        JsonObject after = new JsonObject();
        after.addProperty("name", "GigabitEthernet2");
        after.addProperty("enabled", true);

        when(service.getInterface("GigabitEthernet2")).thenReturn(before, after);
        when(service.rebootInterface("GigabitEthernet2")).thenReturn(204);
        when(service.baseUrl()).thenReturn("http://localhost:8080");

        String output = captureStdout(() -> runner.run("reboot", "GigabitEthernet2"));

        assertTrue(output.contains("Target interface: GigabitEthernet2"));
        assertTrue(output.contains("Device: http://localhost:8080"));
        assertTrue(output.contains("BEFORE: GigabitEthernet2 enabled=true"));
        assertTrue(output.contains("AFTER: GigabitEthernet2 enabled=true"));
        assertTrue(output.contains("Power cycle complete for GigabitEthernet2."));
    }

    private static String captureStdout(ThrowingRunnable action) throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(outputStream, true, StandardCharsets.UTF_8);
        try {
            System.setOut(capture);
            action.run();
            capture.flush();
            return outputStream.toString(StandardCharsets.UTF_8);
        } finally {
            System.setOut(originalOut);
            capture.close();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
