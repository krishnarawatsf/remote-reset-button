# GitHub Copilot Instructions – RESTCONF CRUD Capstone

This repository implements CRUD operations over a network device using RESTCONF (RFC 8040) and the ietf-interfaces YANG model.

## Project Context

- **Scenario**: "The Remote Reset Button" — remotely power-cycle frozen network devices (cameras, access points) at branch locations without a physical site visit.
- **Protocol**: RESTCONF over HTTP/HTTPS. All endpoints follow `/restconf/data/ietf-interfaces:interfaces`.
- **Language in use**: Java (Spring Boot + Maven).
- **Target device**: Cisco IOS-XE or compatible RESTCONF endpoint.

## Code Generation Rules

- Always use `application/yang-data+json` as the Content-Type and Accept header — never `application/json`.
- Always wrap payloads in the YANG key: `{ "ietf-interfaces:interface": { ... } }`.
- Always use Basic Auth on every request.
- Use **PATCH** (not PUT) when toggling interface state (`enabled: true/false`).
- URL-encode interface names that contain `/` as `%2F`.
- Handle HTTP 401, 404, 409, and 415 explicitly — do not silently ignore errors.
- Java: use OkHttp 4.x; never use HttpURLConnection.
- When in doubt about RESTCONF patterns, use `/restconf-crud` skill for reference.

## HTTP Method → CRUD Mapping

| Operation | Method | Endpoint | Expected Success Code |
|-----------|--------|----------|-----------------------|
| Create interface | POST | `/restconf/data/ietf-interfaces:interfaces` | 201 |
| Read interface | GET | `/restconf/data/ietf-interfaces:interfaces/interface={name}` | 200 |
| Update (partial) | PATCH | `/restconf/data/ietf-interfaces:interfaces/interface={name}` | 204 |
| Delete interface | DELETE | `/restconf/data/ietf-interfaces:interfaces/interface={name}` | 204 |

## File Responsibilities

| File | Owner | Purpose |
|------|-------|---------|
| `java/src/main/java/com/remote/resetbutton/RemoteResetButtonApplication.java` | Member 1 | Spring Boot entrypoint |
| `java/src/main/java/com/remote/resetbutton/client/RestconfClient.java` | Member 1 | Shared RESTCONF HTTP client |
| `java/src/main/java/com/remote/resetbutton/service/InterfaceService.java` | Member 2 | Search + reboot business logic |
| `java/src/main/java/com/remote/resetbutton/cli/RestconfCliRunner.java` | Member 3 | CLI command parsing and output |
| `java/src/main/java/com/remote/resetbutton/RestconfProperties.java` | Member 4 | Runtime configuration binding |

## Testing

- Build and test with Maven: `cd java && mvn clean package && mvn test`.
- For CLI smoke checks: `cd java && mvn spring-boot:run -Dspring-boot.run.arguments="find Camera 2"`.
- To connect to Cisco DevNet Always-On sandbox, override JVM properties at runtime (see `java/README.md`).
- Verify state changes on the CLI: `show interfaces GigabitEthernet2`.
