# Mock RESTCONF Server Reference

A zero-dependency mock server using Python's built-in `http.server`.
Simulates a Cisco IOS-XE device for local development and demo.

## Pre-loaded Coffee Shop Interfaces

```python
INTERFACES = {
    "GigabitEthernet1": {
        "name": "GigabitEthernet1", "description": "WAN Uplink – ISP Connection",
        "type": "iana-if-type:ethernetCsmacd", "enabled": True,
        "ipAddress": "203.0.113.1", "prefixLength": 30,
    },
    "GigabitEthernet2": {
        "name": "GigabitEthernet2", "description": "Camera 2 – Entrance",
        "type": "iana-if-type:ethernetCsmacd", "enabled": True,
        "ipAddress": "192.168.10.2", "prefixLength": 24,
    },
    "GigabitEthernet3": {
        "name": "GigabitEthernet3", "description": "Access Point – Seating Area",
        "type": "iana-if-type:ethernetCsmacd", "enabled": True,
        "ipAddress": "192.168.10.3", "prefixLength": 24,
    },
    "GigabitEthernet4": {
        "name": "GigabitEthernet4", "description": "POS Terminal – Counter",
        "type": "iana-if-type:ethernetCsmacd", "enabled": True,
        "ipAddress": "192.168.10.4", "prefixLength": 24,
    },
    "Loopback0": {
        "name": "Loopback0", "description": "Management Interface",
        "type": "iana-if-type:softwareLoopback", "enabled": True,
        "ipAddress": "10.0.0.1", "prefixLength": 32,
    },
}
```

## Full mock_server.py

```python
#!/usr/bin/env python3
import sys, json, base64
from http.server import HTTPServer, BaseHTTPRequestHandler

PORT     = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
YANG_JSON = "application/yang-data+json"
AUTH_OK   = "Basic " + base64.b64encode(b"admin:admin").decode()

# (paste INTERFACES dict from above)

class RestconfHandler(BaseHTTPRequestHandler):
    def log_message(self, *args): pass  # suppress default log

    def do_GET(self):
        if not self._auth(): return
        path = self.path.split("?")[0]

        if path == "/restconf/data/ietf-interfaces:interfaces":
            self._send(200, json.dumps({
                "ietf-interfaces:interfaces": {"interface": list(INTERFACES.values())}
            }, indent=2))
        elif "/interface=" in path:
            name = self._name(path)
            if name in INTERFACES:
                self._send(200, json.dumps(
                    {"ietf-interfaces:interface": INTERFACES[name]}, indent=2))
            else:
                self._send(404, self._err(f"Interface '{name}' not found"))
        else:
            self._send(404, self._err("Path not found"))

    def do_PATCH(self):
        if not self._auth(): return
        path = self.path.split("?")[0]
        name = self._name(path)
        if name not in INTERFACES:
            self._send(404, self._err(f"Interface '{name}' not found")); return

        payload = json.loads(self._body())
        for key in ("ietf-interfaces:interface", "interface"):
            if key in payload: payload = payload[key]; break

        for field, value in payload.items():
            if field in INTERFACES[name]:
                INTERFACES[name][field] = value

        self._send(204, "")

    def _auth(self):
        if self.headers.get("Authorization") != AUTH_OK:
            self._send(401, self._err("Unauthorized")); return False
        return True

    def _name(self, path):
        idx = path.find("interface=")
        return path[idx + 10:].replace("%2F", "/") if idx >= 0 else ""

    def _body(self):
        n = int(self.headers.get("Content-Length", 0))
        return self.rfile.read(n).decode() if n else ""

    def _send(self, code, body):
        b = body.encode()
        self.send_response(code)
        self.send_header("Content-Type", YANG_JSON)
        self.send_header("Content-Length", str(len(b)))
        self.end_headers()
        if b: self.wfile.write(b)

    def _err(self, msg):
        return json.dumps({"ietf-restconf:errors": {"error": [{"error-message": msg}]}})

if __name__ == "__main__":
    print(f"Mock RESTCONF server running on http://localhost:{PORT}")
    print("Auth: admin / admin  |  Press Ctrl+C to stop")
    HTTPServer(("0.0.0.0", PORT), RestconfHandler).serve_forever()
```

## Running

```bash
# Terminal 1
python mock_server.py

# Terminal 2
python find_device.py "Camera 2"
python reboot_port.py GigabitEthernet2
```

## Behaviour

| Request | Response |
|---------|----------|
| `GET /restconf/data/ietf-interfaces:interfaces` | 200 + all interfaces JSON |
| `GET .../interface=GigabitEthernet2` | 200 + single interface JSON |
| `GET .../interface=DoesNotExist` | 404 |
| `PATCH .../interface=GigabitEthernet2` + `{"enabled": false}` | 204 (state persists in memory) |
| Any request without Basic Auth | 401 |
