#!/usr/bin/env bash
# =============================================================================
#  scaffold.sh  –  RESTCONF CRUD Project Scaffolder
#
#  Usage:
#    bash .github/skills/restconf-crud/scripts/scaffold.sh python
#    bash .github/skills/restconf-crud/scripts/scaffold.sh java
#
#  Creates all necessary project files from scratch.
# =============================================================================

set -e

LANG="${1:-python}"
PROJECT_DIR="$(pwd)"

echo ""
echo "=== RESTCONF CRUD Project Scaffolder ==="
echo "Language : $LANG"
echo "Location : $PROJECT_DIR"
echo ""

if [ "$LANG" = "python" ]; then
    echo "[1/5] Creating config.py..."
    cat > "$PROJECT_DIR/config.py" << 'PYEOF'
HOST      = "localhost"
PORT      = 8080
USERNAME  = "admin"
PASSWORD  = "admin"
USE_HTTPS = False

SCHEME   = "https" if USE_HTTPS else "http"
BASE_URL = f"{SCHEME}://{HOST}:{PORT}"

INTERFACES_PATH = "/restconf/data/ietf-interfaces:interfaces"
INTERFACE_PATH  = "/restconf/data/ietf-interfaces:interfaces/interface={name}"
YANG_JSON       = "application/yang-data+json"
REBOOT_WAIT_SECONDS = 5
PYEOF

    echo "[2/5] Creating restconf_client.py..."
    cat > "$PROJECT_DIR/restconf_client.py" << 'PYEOF'
import requests, urllib3
from requests.auth import HTTPBasicAuth
from config import BASE_URL, USERNAME, PASSWORD, YANG_JSON, INTERFACES_PATH

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

class RestconfClient:
    def __init__(self):
        self.auth    = HTTPBasicAuth(USERNAME, PASSWORD)
        self.headers = {"Accept": YANG_JSON, "Content-Type": YANG_JSON}

    def get(self, path):
        r = requests.get(BASE_URL + path, auth=self.auth,
                         headers=self.headers, verify=False, timeout=10)
        return r.json() if r.status_code == 200 else None

    def patch(self, path, payload):
        import json
        return requests.patch(BASE_URL + path, auth=self.auth,
                              headers=self.headers, data=json.dumps(payload),
                              verify=False, timeout=10)

    def get_all_interfaces(self):
        data = self.get(INTERFACES_PATH)
        if not data: return []
        for key in ("ietf-interfaces:interfaces", "interfaces"):
            if key in data:
                ifaces = data[key].get("interface") or \
                         data[key].get("ietf-interfaces:interface", [])
                return ifaces if isinstance(ifaces, list) else [ifaces]
        return []
PYEOF

    echo "[3/5] Creating find_device.py..."
    cat > "$PROJECT_DIR/find_device.py" << 'PYEOF'
#!/usr/bin/env python3
import sys
from restconf_client import RestconfClient

def main():
    if len(sys.argv) < 2:
        print("Usage: python find_device.py <search_term>"); sys.exit(1)
    term   = " ".join(sys.argv[1:]).lower()
    client = RestconfClient()
    ifaces = client.get_all_interfaces()
    found  = [i for i in ifaces if term in i.get("name","").lower()
              or term in i.get("description","").lower()]
    if not found:
        print(f'No interface found matching "{sys.argv[1]}"')
        [print(f'  {i["name"]:<25} {i.get("description","")}') for i in ifaces]
    else:
        for m in found:
            status = "UP" if m.get("enabled", True) else "DOWN"
            print(f'Found "{sys.argv[1]}" on Interface {m["name"]}. Status: {status}.')

if __name__ == "__main__": main()
PYEOF

    echo "[4/5] Creating reboot_port.py..."
    cat > "$PROJECT_DIR/reboot_port.py" << 'PYEOF'
#!/usr/bin/env python3
import sys, time
from restconf_client import RestconfClient
from config import INTERFACE_PATH, REBOOT_WAIT_SECONDS

def patch_enabled(client, name, enabled):
    path    = INTERFACE_PATH.format(name=name.replace("/", "%2F"))
    payload = {"ietf-interfaces:interface": {"name": name, "enabled": enabled}}
    return client.patch(path, payload)

def main():
    if len(sys.argv) < 2:
        print("Usage: python reboot_port.py <InterfaceName>"); sys.exit(1)
    name = " ".join(sys.argv[1:])
    client = RestconfClient()
    print(f"[1/3] Shutting down {name}...")
    r = patch_enabled(client, name, False)
    print(f"      HTTP {r.status_code} – {name} is DOWN")
    print(f"[2/3] Waiting {REBOOT_WAIT_SECONDS}s...")
    time.sleep(REBOOT_WAIT_SECONDS)
    print(f"[3/3] Re-enabling {name}...")
    r = patch_enabled(client, name, True)
    print(f"      HTTP {r.status_code} – {name} is UP")
    print(f"✔ Power cycle complete.")

if __name__ == "__main__": main()
PYEOF

    echo "[5/5] Creating mock_server.py..."
    cat > "$PROJECT_DIR/mock_server.py" << 'PYEOF'
#!/usr/bin/env python3
import sys, json, base64
from http.server import HTTPServer, BaseHTTPRequestHandler

PORT      = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
YANG_JSON = "application/yang-data+json"
AUTH_OK   = "Basic " + base64.b64encode(b"admin:admin").decode()

INTERFACES = {
    "GigabitEthernet1": {"name":"GigabitEthernet1","description":"WAN Uplink","type":"iana-if-type:ethernetCsmacd","enabled":True,"ipAddress":"203.0.113.1","prefixLength":30},
    "GigabitEthernet2": {"name":"GigabitEthernet2","description":"Camera 2 – Entrance","type":"iana-if-type:ethernetCsmacd","enabled":True,"ipAddress":"192.168.10.2","prefixLength":24},
    "GigabitEthernet3": {"name":"GigabitEthernet3","description":"Access Point – Seating Area","type":"iana-if-type:ethernetCsmacd","enabled":True,"ipAddress":"192.168.10.3","prefixLength":24},
    "GigabitEthernet4": {"name":"GigabitEthernet4","description":"POS Terminal – Counter","type":"iana-if-type:ethernetCsmacd","enabled":True,"ipAddress":"192.168.10.4","prefixLength":24},
    "Loopback0":        {"name":"Loopback0","description":"Management Interface","type":"iana-if-type:softwareLoopback","enabled":True,"ipAddress":"10.0.0.1","prefixLength":32},
}

class H(BaseHTTPRequestHandler):
    def log_message(self, *a): pass
    def do_GET(self):
        if not self._ok(): return
        p = self.path.split("?")[0]
        if p == "/restconf/data/ietf-interfaces:interfaces":
            self._s(200, json.dumps({"ietf-interfaces:interfaces":{"interface":list(INTERFACES.values())}},indent=2))
        elif "/interface=" in p:
            n = self._n(p)
            self._s(200, json.dumps({"ietf-interfaces:interface":INTERFACES[n]},indent=2)) if n in INTERFACES else self._s(404,self._e(f"'{n}' not found"))
        else: self._s(404, self._e("Not found"))
    def do_PATCH(self):
        if not self._ok(): return
        n = self._n(self.path)
        if n not in INTERFACES: self._s(404, self._e(f"'{n}' not found")); return
        pl = json.loads(self._b())
        for k in ("ietf-interfaces:interface","interface"):
            if k in pl: pl = pl[k]; break
        for f,v in pl.items():
            if f in INTERFACES[n]: INTERFACES[n][f] = v
        self._s(204,"")
    def _ok(self):
        if self.headers.get("Authorization") != AUTH_OK: self._s(401,self._e("Unauthorized")); return False
        return True
    def _n(self, p):
        i = p.find("interface="); return p[i+10:].replace("%2F","/") if i>=0 else ""
    def _b(self):
        n=int(self.headers.get("Content-Length",0)); return self.rfile.read(n).decode() if n else ""
    def _s(self, c, b):
        b=b.encode(); self.send_response(c); self.send_header("Content-Type",YANG_JSON); self.send_header("Content-Length",str(len(b))); self.end_headers()
        if b: self.wfile.write(b)
    def _e(self, m): return json.dumps({"ietf-restconf:errors":{"error":[{"error-message":m}]}})

if __name__=="__main__":
    print(f"Mock RESTCONF server → http://localhost:{PORT}  (admin/admin)  Ctrl+C to stop")
    HTTPServer(("0.0.0.0",PORT),H).serve_forever()
PYEOF

    echo ""
    echo "✔ Python project scaffolded successfully!"
    echo ""
    echo "Next steps:"
    echo "  1. pip install requests"
    echo "  2. python mock_server.py          (Terminal 1)"
    echo "  3. python find_device.py 'Camera 2'   (Terminal 2)"
    echo "  4. python reboot_port.py GigabitEthernet2"

elif [ "$LANG" = "java" ]; then
    echo "[1/2] Creating Maven project structure..."
    mkdir -p "$PROJECT_DIR/src/main/java/com/restconf/{client,model,server,util}"
    mkdir -p "$PROJECT_DIR/src/test/java/com/restconf"

    echo "[2/2] Creating pom.xml..."
    cat > "$PROJECT_DIR/pom.xml" << 'XMLEOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.restconf</groupId>
  <artifactId>restconf-crud</artifactId>
  <version>1.0-SNAPSHOT</version>
  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
  </properties>
  <dependencies>
    <dependency><groupId>com.squareup.okhttp3</groupId><artifactId>okhttp</artifactId><version>4.12.0</version></dependency>
    <dependency><groupId>com.google.code.gson</groupId><artifactId>gson</artifactId><version>2.10.1</version></dependency>
    <dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId><version>5.10.0</version><scope>test</scope></dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.1.2</version></plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId><artifactId>maven-shade-plugin</artifactId><version>3.5.0</version>
        <executions><execution><phase>package</phase><goals><goal>shade</goal></goals>
          <configuration><transformers><transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer"><mainClass>com.restconf.Main</mainClass></transformer></transformers></configuration>
        </execution></executions>
      </plugin>
    </plugins>
  </build>
</project>
XMLEOF

    echo ""
    echo "✔ Java project scaffolded successfully!"
    echo ""
    echo "Next steps:"
    echo "  1. Ask Copilot to generate each Java class using /restconf-crud"
    echo "  2. mvn clean package"
    echo "  3. java -jar target/restconf-crud-1.0-SNAPSHOT.jar"

else
    echo "Unknown language: $LANG"
    echo "Usage: bash scaffold.sh [python|java]"
    exit 1
fi
