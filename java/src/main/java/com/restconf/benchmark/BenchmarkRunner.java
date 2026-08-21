package com.restconf.benchmark;

import com.restconf.model.NetworkInterface;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Performance Benchmark Suite for CRUD Operations.
 * 
 * Measures actual execution time, throughput, latency percentiles,
 * and heap memory footprint across increasing dataset sizes (100 to 50,000 items).
 */
public class BenchmarkRunner {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("        RESTCONF CRUD IN-MEMORY ENGINE PERFORMANCE BENCHMARKS");
        System.out.println("==========================================================================");

        int[] datasetSizes = {100, 1000, 10000, 50000};

        // Warmup JIT compiler
        runWarmup();

        System.out.println(String.format("%-10s | %-12s | %-12s | %-12s | %-12s | %-10s",
                "Dataset N", "Operation", "Total (ms)", "Throughput", "Avg Latency", "Heap (MB)"));
        System.out.println("--------------------------------------------------------------------------");

        for (int n : datasetSizes) {
            benchmarkDataset(n);
            System.out.println("--------------------------------------------------------------------------");
        }
    }

    private static void runWarmup() {
        ConcurrentMap<String, NetworkInterface> map = new ConcurrentHashMap<>();
        for (int i = 0; i < 5000; i++) {
            String name = "Warmup-" + i;
            NetworkInterface iface = new NetworkInterface(name, "Warmup", "iana-if-type:ethernetCsmacd", true, "10.0.0.1", 24);
            map.put(name, iface);
            map.get(name);
            map.remove(name);
        }
    }

    private static void benchmarkDataset(int n) {
        ConcurrentMap<String, NetworkInterface> store = new ConcurrentHashMap<>(n);
        List<NetworkInterface> pregenerated = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            pregenerated.add(new NetworkInterface(
                    "GigabitEthernet" + i,
                    "Camera " + (i % 20) + " - Endpoint",
                    "iana-if-type:ethernetCsmacd",
                    i % 2 == 0,
                    "192.168." + (i / 250) + "." + (i % 250),
                    24
            ));
        }

        // 1. CREATE (Insert)
        long startMem = getUsedMemoryMb();
        long start = System.nanoTime();
        for (NetworkInterface iface : pregenerated) {
            store.put(iface.getName(), iface);
        }
        long createTimeNs = System.nanoTime() - start;
        long endMem = getUsedMemoryMb();
        printRow(n, "CREATE", createTimeNs, n, Math.max(0, endMem - startMem));

        // 2. READ (Get by Name / ID)
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            NetworkInterface found = store.get("GigabitEthernet" + i);
            if (found == null) {
                throw new IllegalStateException("Missing item during benchmark");
            }
        }
        long readTimeNs = System.nanoTime() - start;
        printRow(n, "READ (O(1))", readTimeNs, n, endMem);

        // 3. UPDATE (Modify)
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            NetworkInterface existing = store.get("GigabitEthernet" + i);
            existing.setDescription("Updated " + i);
            store.put("GigabitEthernet" + i, existing);
        }
        long updateTimeNs = System.nanoTime() - start;
        printRow(n, "UPDATE", updateTimeNs, n, endMem);

        // 4. SEARCH (Filtered scan)
        start = System.nanoTime();
        long matches = store.values().stream()
                .filter(iface -> iface.getDescription() != null && iface.getDescription().contains("Camera 2"))
                .count();
        long searchTimeNs = System.nanoTime() - start;
        printRow(n, "SEARCH (O(N))", searchTimeNs, 1, endMem);

        // 5. DELETE (Remove)
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            store.remove("GigabitEthernet" + i);
        }
        long deleteTimeNs = System.nanoTime() - start;
        printRow(n, "DELETE", deleteTimeNs, n, getUsedMemoryMb());
    }

    private static void printRow(int n, String op, long timeNs, int opCount, long memMb) {
        double timeMs = timeNs / 1_000_000.0;
        double throughput = (opCount / (timeNs / 1_000_000_000.0));
        double avgLatencyUs = (timeNs / (double) opCount) / 1_000.0;

        String latencyStr = avgLatencyUs >= 1000 ? String.format("%.2f ms", avgLatencyUs / 1000.0) : String.format("%.2f µs", avgLatencyUs);
        String throughputStr = String.format("%,.0f ops/s", throughput);

        System.out.println(String.format("%-10d | %-12s | %-12.2f | %-12s | %-12s | %-10d",
                n, op, timeMs, throughputStr, latencyStr, memMb));
    }

    private static long getUsedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }
}
