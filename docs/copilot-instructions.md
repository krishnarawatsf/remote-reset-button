# GitHub Copilot Instructions – RESTCONF CRUD Capstone

This repository implements CRUD operations over a network device using RESTCONF (RFC 8040) and the ietf-interfaces YANG model.

## Project Context

- **Scenario**: "The Remote Reset Button" — remotely power-cycle frozen network devices (cameras, access points) at branch locations without a physical site visit.
- **Protocol**: RESTCONF over HTTP/HTTPS. All endpoints follow `/restconf/data/ietf-interfaces:interfaces`.
- **Languages in use**: Python (scripts) and/or Java (Maven project).
- **Target device**: Cisco IOS-XE or local mock server on `localhost:8080`.

## Code Generation Rules

- Always use `application/yang-data+json` as the Content-Type and Accept header — never `application/json`.
- Always wrap payloads in the YANG key: `{ "ietf-interfaces:interface": { ... } }`.
- Always use Basic Auth on every request.
- Use **PATCH** (not PUT) when toggling interface state (`enabled: true/false`).
- URL-encode interface names that contain `/` as `%2F`.
- Handle HTTP 401, 404, 409, and 415 explicitly — do not silently ignore errors.
- Python: use `verify=False` with `urllib3.disable_warnings()` for DevNet sandbox self-signed certs.
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
| `config.py` | Member 1 | Central settings (host, port, credentials, paths) |
| `restconf_client.py` | Member 1 | Shared HTTP client |
| `find_device.py` | Member 2 | Phase 1: GET all interfaces, search by description |
| `reboot_port.py` | Member 3 | Phase 2: PATCH port off → wait → PATCH port on |
| `mock_server.py` | Member 4 | Local RESTCONF simulation (no real hardware needed) |
| `demo.py` | Member 4 | End-to-end demo runner |

## Testing

- Use the mock server (`python mock_server.py`) for all local development.
- To connect to the real Cisco DevNet Always-On sandbox, update `HOST`, `PORT`, `USERNAME`, `PASSWORD`, and `USE_HTTPS` in `config.py` only.
- Verify state changes on the CLI: `show interfaces GigabitEthernet2`.
