#!/usr/bin/env python3
# =============================================================================
#  mock_server.py  –  Simulated RESTCONF Device
#
#  USAGE:
#    python mock_server.py            (runs on port 8080)
#    python mock_server.py 9090       (custom port)
#
#  WHY THIS EXISTS:
#    You can demo the entire project WITHOUT a real Cisco router.
#    This server behaves exactly like a Cisco IOS-XE RESTCONF endpoint:
#      • Returns proper YANG JSON responses
#      • Requires Basic Auth (admin/admin)
#      • Supports GET and PATCH on ietf-interfaces
#
#  PRE-LOADED INTERFACES (simulating a coffee shop network):
#    GigabitEthernet1   → "WAN Uplink"
#    GigabitEthernet2   → "Camera 2 – Entrance"
#    GigabitEthernet3   → "Access Point – Seating Area"
#    GigabitEthernet4   → "POS Terminal – Counter"
#    Loopback0          → "Management Interface"
# =============================================================================

import sys
import json
import base64
from http.server import HTTPServer, BaseHTTPRequestHandler
from config import Colour, PORT as DEFAULT_PORT

PORT = DEFAULT_PORT

# ── Simulated device datastore ────────────────────────────────────────────────
# Mirrors the ietf-interfaces YANG model (RFC 7223)

INTERFACES = {
    "GigabitEthernet1": {
        "name":        "GigabitEthernet1",
        "description": "WAN Uplink – ISP Connection",
        "type":        "iana-if-type:ethernetCsmacd",
        "enabled":     True,
        "ipAddress":   "203.0.113.1",
        "prefixLength": 30,
    },
    "GigabitEthernet2": {
        "name":        "GigabitEthernet2",
        "description": "Camera 2 – Entrance",
        "type":        "iana-if-type:ethernetCsmacd",
        "enabled":     True,
        "ipAddress":   "192.168.10.2",
        "prefixLength": 24,
    },
    "GigabitEthernet3": {
        "name":        "GigabitEthernet3",
        "description": "Access Point – Seating Area",
        "type":        "iana-if-type:ethernetCsmacd",
        "enabled":     True,
        "ipAddress":   "192.168.10.3",
        "prefixLength": 24,
    },
    "GigabitEthernet4": {
        "name":        "GigabitEthernet4",
        "description": "POS Terminal – Counter",
        "type":        "iana-if-type:ethernetCsmacd",
        "enabled":     True,
        "ipAddress":   "192.168.10.4",
        "prefixLength": 24,
    },
    "Loopback0": {
        "name":        "Loopback0",
        "description": "Management Interface",
        "type":        "iana-if-type:softwareLoopback",
        "enabled":     True,
        "ipAddress":   "10.0.0.1",
        "prefixLength": 32,
    },
}

YANG_JSON = "application/yang-data+json"
AUTH_OK   = "Basic " + base64.b64encode(b"admin:admin").decode()


# ── Request Handler ───────────────────────────────────────────────────────────

class RestconfHandler(BaseHTTPRequestHandler):

    # Suppress default request log — we print our own
    def log_message(self, format, *args):
        pass

    def do_GET(self):
        if not self._check_auth():
            return

        path = self.path.split("?")[0]   # strip query params

        # GET /restconf/data/ietf-interfaces:interfaces
        if path == "/restconf/data/ietf-interfaces:interfaces":
            body = json.dumps({
                "ietf-interfaces:interfaces": {
                    "interface": list(INTERFACES.values())
                }
            }, indent=2)
            self._send(200, body)
            self._log("GET", path, 200)

        # GET /restconf/data/ietf-interfaces:interfaces/interface={name}
        elif "/interface=" in path:
            name = self._extract_name(path)
            if name in INTERFACES:
                body = json.dumps({
                    "ietf-interfaces:interface": INTERFACES[name]
                }, indent=2)
                self._send(200, body)
                self._log("GET", path, 200, name)
            else:
                self._send_error(404, f"Interface '{name}' not found")
                self._log("GET", path, 404, name, ok=False)

        else:
            self._send_error(404, "Path not found")

    def do_PATCH(self):
        if not self._check_auth():
            return

        path = self.path.split("?")[0]

        if "/interface=" not in path:
            self._send_error(400, "PATCH requires an interface key in the path")
            return

        name = self._extract_name(path)
        if name not in INTERFACES:
            self._send_error(404, f"Interface '{name}' not found")
            self._log("PATCH", path, 404, name, ok=False)
            return

        body = self._read_body()
        try:
            payload = json.loads(body)

            # Support both wrapped and unwrapped payloads
            for key in ("ietf-interfaces:interface", "interface"):
                if key in payload:
                    payload = payload[key]
                    break

            # Apply only the fields present in the patch payload
            for field, value in payload.items():
                if field in INTERFACES[name]:
                    INTERFACES[name][field] = value

            self._send(204, "")
            enabled = INTERFACES[name]["enabled"]
            state   = "UP ✔" if enabled else "DOWN ✘"
            self._log("PATCH", path, 204, f"{name} → enabled={enabled} [{state}]")

        except json.JSONDecodeError as e:
            self._send_error(400, f"Invalid JSON: {e}")

    # ── Helpers ───────────────────────────────────────────────────────────────

    def _check_auth(self) -> bool:
        auth = self.headers.get("Authorization", "")
        if auth != AUTH_OK:
            self._send_error(401, "Unauthorized")
            return False
        return True

    def _extract_name(self, path: str) -> str:
        idx = path.find("interface=")
        return path[idx + len("interface="):].replace("%2F", "/")

    def _read_body(self) -> str:
        length = int(self.headers.get("Content-Length", 0))
        return self.rfile.read(length).decode("utf-8") if length else ""

    def _send(self, code: int, body: str):
        encoded = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", YANG_JSON)
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        if encoded:
            self.wfile.write(encoded)

    def _send_error(self, code: int, message: str):
        body = json.dumps({
            "ietf-restconf:errors": {
                "error": [{"error-message": message}]
            }
        }, indent=2)
        self._send(code, body)

    def _log(self, method, path, code, detail="", ok=True):
        c = Colour.GREEN if ok else Colour.RED
        s = "✔" if ok else "✘"
        print(f"  {c}{s}{Colour.RESET}  {method:<6} {code}  {path}"
              + (f"  ({detail})" if detail else ""))


# ── Entry point ───────────────────────────────────────────────────────────────

if __name__ == "__main__":
    port = PORT
    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            print(f"{Colour.RED}Invalid port: {sys.argv[1]}{Colour.RESET}")
            sys.exit(1)

    print(f"""
{Colour.CYAN}{Colour.BOLD}╔══════════════════════════════════════════════════════════╗
║         MOCK RESTCONF SERVER  –  Remote Reset Button      ║
║         Simulating a Cisco IOS-XE device                  ║
╚══════════════════════════════════════════════════════════╝{Colour.RESET}

  {Colour.BOLD}Listening on:{Colour.RESET}   http://localhost:{port}
  {Colour.BOLD}Auth:{Colour.RESET}           admin / admin
  {Colour.BOLD}Base path:{Colour.RESET}      /restconf/data/ietf-interfaces:interfaces

  {Colour.DIM}Pre-loaded interfaces (simulating a coffee shop network):{Colour.RESET}""")

    for name, iface in INTERFACES.items():
        state = f"{Colour.GREEN}UP{Colour.RESET}" if iface["enabled"] else f"{Colour.RED}DOWN{Colour.RESET}"
        print(f"  {Colour.DIM}  {name:<22} {iface['description']:<35}{Colour.RESET} {state}")

    print(f"\n  {Colour.DIM}Press Ctrl+C to stop.{Colour.RESET}\n")
    print(f"  {'─'*55}")
    print(f"  Incoming requests:\n")

    try:
        server = HTTPServer(("0.0.0.0", port), RestconfHandler)
        server.serve_forever()
    except KeyboardInterrupt:
        print(f"\n\n  {Colour.YELLOW}Server stopped.{Colour.RESET}\n")
