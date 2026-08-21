# 🔄 RESTCONF Network Interface CRUD Automation

A production-grade, modular Java application demonstrating full **CRUD** (Create, Read, Update, Delete), Search, and Partial Modification (PATCH) workflows over the **IETF Network Interfaces YANG model** ([RFC 8343](https://datatracker.ietf.org/doc/html/rfc8343) / [RFC 8040](https://datatracker.ietf.org/doc/html/rfc8040)).

[![Java CI](https://github.com/krishnarawatsf/remote-reset-automation/actions/workflows/ci.yml/badge.svg)](https://github.com/krishnarawatsf/remote-reset-automation/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JDK: 17+](https://img.shields.io/badge/JDK-17%2B-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot: 3.2.5](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)

---

## 📌 Problem Statement

Network engineers and automation pipelines frequently need to inspect, provision, modify, and power-cycle remote network interfaces across edge devices (e.g., switches, routers, and IoT endpoints). Traditional manual CLI interventions (Telnet/SSH) are error-prone, lack structured contracts, and do not provide atomic validation.

This project delivers:
1. A **high-performance, thread-safe RESTCONF Mock Server** implementing IETF YANG data model contracts with validation and HTTP Basic Authentication.
2. A **resilient Java RESTCONF Client** with configurable connection pools, timeouts, and type-safe response handling.
3. An **algorithmic benchmark suite** comparing measured in-memory performance across varying dataset sizes ($N = 100$ to $50,000$).

---

## ✨ Features

- **Full CRUD Support**:
  - `CREATE`: Provision loopback and physical ethernet interfaces (`POST`).
  - `READ`: Retrieve specific interface configs or search across the entire inventory (`GET`).
  - `UPDATE`: Complete replacement of interface parameters (`PUT`).
  - `PATCH`: Atomic attribute updates (e.g., shutting down a port for a remote power-cycle) (`PATCH`).
  - `DELETE`: Safely decommission network interfaces (`DELETE`).
- **RESTCONF Standards Compliance**: Handles `application/yang-data+json` media types, URL-encoded path segments, and standard YANG JSON envelopes.
- **Thread-Safe In-Memory Storage**: Backed by `ConcurrentHashMap` with $O(1)$ average time complexity for primary-key lookups and mutations.
- **Multi-Layer Validation**: Enforces IPv4 regex checks, non-blank names, and valid CIDR prefix boundaries ($0 \le \text{prefix} \le 128$).
- **Security by Design**: HTTP Basic Authentication with constant-time comparison (`MessageDigest.isEqual`) to prevent timing side-channel attacks.
- **Zero-Dependency Core Client**: Built with standard OkHttp 4.x and Gson for fast startup and minimal dependency footprint.

---

## 🏛 Architecture

The project adopts a clean, decoupled 3-tier architecture:

```
+-------------------------------------------------------------+
|                      Client Layer                           |
|        (RestconfClient / Main / BenchmarkRunner)            |
+-------------------------------------------------------------+
                              │ HTTP / YANG-JSON
                              ▼
+-------------------------------------------------------------+
|                    RESTCONF Controller                      |
|                  (InterfaceController)                      |
|          - Endpoint Routing & URL Parameter Decoding        |
|          - Media Type Negotiation (yang-data+json)          |
|          - Exception Mapping via GlobalExceptionHandler     |
+-------------------------------------------------------------+
                              │
                              ▼
+-------------------------------------------------------------+
|                       Service Layer                         |
|                   (InterfaceServiceImpl)                    |
|          - Business Logic & Duplicate Prevention            |
|          - Partial Attribute Patch Evaluation               |
+-------------------------------------------------------------+
                              │
                              ▼
+-------------------------------------------------------------+
|                     Repository Layer                        |
|               (InMemoryInterfaceRepository)                 |
|          - ConcurrentHashMap Storage Engine                 |
|          - In-Memory Filter & Search Predicates             |
+-------------------------------------------------------------+
```

---

## 💻 Tech Stack

- **Language**: Java 17+ (Tested on Java 17 and Java 22)
- **Framework**: Spring Boot 3.2.5 (Mock Server)
- **HTTP Engine**: OkHttp 4.12.0
- **JSON Processing**: Google Gson 2.10.1
- **Testing**: JUnit 5, AssertJ 3.25.3, Spring MockMvc, MockWebServer
- **Build System**: Multi-module Apache Maven

---

## 📊 Data Structures & Algorithms

### Chosen Data Structures

1. **`ConcurrentHashMap<String, NetworkInterface>` (Primary Store)**:
   - *Why*: Provides non-blocking concurrent reads and lock striping across table buckets.
   - *Key*: Interface `name` (e.g., `GigabitEthernet2`, `Loopback0`).
   - *Alternative Considered*: `TreeMap` (offers sorted keys at $O(\log n)$ cost, unnecessary for point lookups).

2. **`ArrayList<NetworkInterface>` (Read/Search Result Buffers)**:
   - *Why*: Fast sequential iteration and deterministic sizing when returning lists to controllers.

---

## ⏱ Complexity Analysis

| Operation | Data Structure / Algorithm | Time (Avg) | Time (Worst) | Space (Aux) | Reason / Mechanism |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Create (POST)** | `ConcurrentHashMap.put` | $O(1)$ | $O(n)$ | $O(1)$ | Direct hash bucket insertion (hash collisions degrade to tree/list). |
| **Get by Name (GET)** | `ConcurrentHashMap.get` | $O(1)$ | $O(n)$ | $O(1)$ | Hash lookup indexed by interface name. |
| **Update (PUT)** | `ConcurrentHashMap.put` | $O(1)$ | $O(n)$ | $O(1)$ | Direct hash table value replacement. |
| **Patch (PATCH)** | Fetch + Field Delta + Save | $O(1)$ | $O(n)$ | $O(1)$ | In-place property update on existing reference. |
| **Delete (DELETE)** | `ConcurrentHashMap.remove` | $O(1)$ | $O(n)$ | $O(1)$ | Direct hash bucket entry removal. |
| **Search / Filter** | Stream Predicate Scan | $O(n)$ | $O(n)$ | $O(k)$ | Linear scan across $n$ items, collecting $k$ matching records. |

*Total Storage Space*: $O(n)$ where $n$ is the number of active interfaces stored in memory.

---

## 📡 API Documentation

Base URL: `http://localhost:8080/restconf/data/ietf-interfaces:interfaces`  
Headers:  
- `Authorization: Basic YWRtaW46YWRtaW4=` (`admin:admin`)
- `Accept: application/yang-data+json`
- `Content-Type: application/yang-data+json`

### Endpoints

#### 1. Create Interface
- **Method**: `POST`
- **Path**: `/restconf/data/ietf-interfaces:interfaces`
- **Request Body**:
  ```json
  {
    "ietf-interfaces:interface": {
      "name": "Loopback100",
      "description": "Demo loopback",
      "type": "iana-if-type:softwareLoopback",
      "enabled": true,
      "ipAddress": "10.10.10.1",
      "prefixLength": 32
    }
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "message": "Interface created",
    "data": {
      "ietf-interfaces:interface": { ... }
    }
  }
  ```

#### 2. Get Interface by Name
- **Method**: `GET`
- **Path**: `/restconf/data/ietf-interfaces:interfaces/interface={name}`
- **Response (200 OK)**:
  ```json
  {
    "ietf-interfaces:interface": {
      "name": "Loopback100",
      "description": "Demo loopback",
      "type": "iana-if-type:softwareLoopback",
      "enabled": true,
      "ipAddress": "10.10.10.1",
      "prefixLength": 32
    }
  }
  ```

#### 3. Search & List Interfaces
- **Method**: `GET`
- **Path**: `/restconf/data/ietf-interfaces:interfaces?search=Camera&enabled=true`
- **Response (200 OK)**:
  ```json
  {
    "ietf-interfaces:interfaces": {
      "interface": [ ... ]
    }
  }
  ```

#### 4. Update Interface
- **Method**: `PUT`
- **Path**: `/restconf/data/ietf-interfaces:interfaces/interface={name}`
- **Response (200 OK)**

#### 5. Patch Interface (Partial Update)
- **Method**: `PATCH`
- **Path**: `/restconf/data/ietf-interfaces:interfaces/interface={name}`
- **Request Body**:
  ```json
  {
    "enabled": false
  }
  ```
- **Response (200 OK)**

#### 6. Delete Interface
- **Method**: `DELETE`
- **Path**: `/restconf/data/ietf-interfaces:interfaces/interface={name}`
- **Response (200 OK)**

---

## 🧪 Testing & Verification

The test suite contains **34 comprehensive tests** across unit, integration, and MockWebServer boundaries.

```bash
# Run all tests across both modules
mvn clean test
```

### Test Summary
- **Total Tests Run**: 34
- **Passed**: 34
- **Failed**: 0
- **Skipped**: 0

### Coverage Areas:
- ✅ Repository $O(1)$ CRUD correctness and thread concurrency (multi-threaded stress test).
- ✅ Service duplicate prevention (`409 Conflict`) and missing record handling (`404 Not Found`).
- ✅ Jakarta input validation (malformed IPv4, invalid names, prefix boundary violations).
- ✅ Basic Auth filter timing-attack resilience and credential validation (`401 Unauthorized`).
- ✅ Client URI encoding for path segments containing slashes (e.g., `GigabitEthernet0/1`).

---

## ⚡ Performance Benchmarks

The benchmark suite (`BenchmarkRunner`) executes real in-memory measurement runs across dataset sizes from $N = 100$ to $N = 50,000$.

*Executed on Apple M-series / JDK 22 environment:*

| Dataset Size ($N$) | Operation | Total Time (ms) | Observed Throughput | Avg Latency | Heap Used |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **100** | `CREATE` | 0.01 ms | 7,017,544 ops/s | 0.14 µs | < 1 MB |
| **100** | `READ (O(1))` | 0.09 ms | 1,117,831 ops/s | 0.89 µs | 23 MB |
| **100** | `SEARCH (O(N))`| 0.37 ms | 2,688 ops/s | 372.08 µs | 23 MB |
| **1,000** | `CREATE` | 0.09 ms | 10,738,255 ops/s | 0.09 µs | < 1 MB |
| **1,000** | `READ (O(1))` | 0.13 ms | 7,680,020 ops/s | 0.13 µs | 24 MB |
| **1,000** | `SEARCH (O(N))`| 0.47 ms | 2,137 ops/s | 467.96 µs | 24 MB |
| **10,000** | `CREATE` | 0.98 ms | 10,160,454 ops/s | 0.10 µs | 1 MB |
| **10,000** | `READ (O(1))` | 0.95 ms | 10,482,631 ops/s | 0.10 µs | 7 MB |
| **10,000** | `SEARCH (O(N))`| 1.12 ms | 895 ops/s | 1.12 ms | 7 MB |
| **50,000** | `CREATE` | 4.38 ms | 11,419,217 ops/s | 0.09 µs | 2 MB |
| **50,000** | `READ (O(1))` | 3.05 ms | 16,401,961 ops/s | 0.06 µs | 23 MB |
| **50,000** | `SEARCH (O(N))`| 5.85 ms | 171 ops/s | 5.85 ms | 23 MB |

---

## 🚀 Installation & Usage

### Prerequisites
- JDK 17 or higher
- Apache Maven 3.8+

### Step 1: Start the RESTCONF Mock Server
```bash
cd restconf-mock-server
mvn spring-boot:run
```
*(Server listens on port 8080)*

### Step 2: Run the Java Demonstration Client
In a separate terminal:
```bash
cd java
mvn compile exec:java
```

### Step 3: Run the Benchmarking Engine
```bash
cd java
mvn test -Dtest=BenchmarkRunnerTest
```

---

## 📂 Project Structure

```
.
├── pom.xml                                   # Parent multi-module build descriptor
├── LICENSE                                   # MIT License
├── README.md                                 # Complete documentation & benchmarks
├── .github/
│   └── workflows/
│       └── ci.yml                            # Automated GitHub Actions CI pipeline
├── restconf-mock-server/                     # Spring Boot Mock Server Module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/mock/
│       │   ├── config/SecurityFilter.java    # Basic Auth with constant-time verification
│       │   ├── controller/InterfaceController.java
│       │   ├── exception/GlobalExceptionHandler.java
│       │   ├── model/NetworkInterface.java   # Validated IETF YANG model
│       │   ├── repository/InMemoryInterfaceRepository.java
│       │   └── service/InterfaceServiceImpl.java
│       └── test/java/com/mock/
│           ├── controller/InterfaceControllerIntegrationTest.java
│           ├── repository/InMemoryInterfaceRepositoryTest.java
│           └── service/InterfaceServiceTest.java
└── java/                                     # Java RESTCONF Client & Benchmarks
    ├── pom.xml
    └── src/
        ├── main/java/com/restconf/
        │   ├── Main.java                     # Interactive CLI walkthrough
        │   ├── benchmark/BenchmarkRunner.java# Latency & memory benchmark suite
        │   ├── client/RestconfClient.java    # HTTP RESTCONF Client with timeouts
        │   ├── client/CrudResponse.java
        │   └── model/NetworkInterface.java
        └── test/java/com/restconf/
            ├── benchmark/BenchmarkRunnerTest.java
            ├── client/RestconfClientTest.java
            └── model/NetworkInterfaceModelTest.java
```

---

## 🔒 Security & Validation

1. **Authentication**: All endpoints require HTTP Basic Auth credentials (`admin:admin`). Checked via `MessageDigest.isEqual` to prevent side-channel timing attacks.
2. **Input Hygiene**: Enforces regex-checked IPv4 addresses and prevents malicious path traversal or injection through URL-encoded interface keys.
3. **No Hardcoded Secrets**: Credentials and ports are externalizable via system properties and environment variables.

---

## ⚖ Design Decisions & Tradeoffs

1. **In-Memory Store vs. Embedded H2**:
   - *Decision*: Used `ConcurrentHashMap` for maximum throughput (>10M ops/sec) and minimal resource overhead without SQL dialect impedance mismatch.
   - *Tradeoff*: Data resets when process terminates. Suitable for test harnesses, mock sandboxes, and automated CI pipelines.
2. **OkHttp vs. Spring WebClient / HttpClient**:
   - *Decision*: OkHttp provides lightweight, zero-overhead HTTP execution without forcing reactive dependencies or bulky frameworks onto client consumers.

---

## 🔮 Limitations & Future Improvements

- **Limitations**:
  - In-memory storage is ephemeral.
  - Multi-attribute composite indexing is not implemented; searches run linear scans $O(n)$.
- **Realistic Next Steps**:
  - Add inverted indexing for $O(1)$ substring / prefix searching on interface descriptions.
  - Support IPv6 prefix validation in addition to IPv4.
  - Add WebSocket / SSE notification stream for telemetry changes ([RFC 8040 Section 6](https://datatracker.ietf.org/doc/html/rfc8040#section-6)).

---

## 📜 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
