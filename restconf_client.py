# =============================================================================
#  restconf_client.py  –  Remote Reset Button
#
#  Shared RESTCONF HTTP client used by every script in the project.
#  Handles authentication, headers, and all HTTP methods.
#
#  Methods:
#    get(path)               → dict | None
#    patch(path, payload)    → requests.Response
#    get_all_interfaces()    → list[dict]
# =============================================================================

import requests
import urllib3
from requests.auth import HTTPBasicAuth
from config import BASE_URL, USERNAME, PASSWORD, YANG_JSON, Colour

# Suppress SSL warnings when verify=False is used against self-signed certs
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


class RestconfClient:
    """
    Thin wrapper around the requests library for RESTCONF calls.
    All methods share the same auth, headers, and error handling.
    """

    def __init__(self):
        self.base_url = BASE_URL
        self.auth     = HTTPBasicAuth(USERNAME, PASSWORD)
        self.headers  = {
            "Accept":       YANG_JSON,
            "Content-Type": YANG_JSON,
        }

    # ── GET ───────────────────────────────────────────────────────────────────
    def get(self, path: str) -> dict | None:
        """
        Sends a RESTCONF GET request.

        Returns the parsed JSON dict on success (200),
        or None if not found / error.
        """
        url = self.base_url + path
        try:
            response = requests.get(
                url,
                auth=self.auth,
                headers=self.headers,
                verify=False,       # Required for DevNet sandbox self-signed cert
                timeout=10,
            )
            self._log_response("GET", url, response)

            if response.status_code == 200:
                return response.json()
            return None

        except requests.exceptions.ConnectionError:
            print(f"{Colour.RED}✘  Connection refused — is the mock server running?{Colour.RESET}")
            print(f"{Colour.DIM}   Run:  python mock_server.py{Colour.RESET}")
            return None

        except requests.exceptions.Timeout:
            print(f"{Colour.RED}✘  Request timed out.{Colour.RESET}")
            return None

    # ── PATCH ─────────────────────────────────────────────────────────────────
    def patch(self, path: str, payload: dict) -> requests.Response | None:
        """
        Sends a RESTCONF PATCH request with a JSON payload.

        PATCH performs a partial update — only the fields in the
        payload are changed; other fields are left untouched.
        This is the correct method for toggling enabled: true/false.

        Returns the raw Response object so callers can check .status_code.
        """
        import json
        url  = self.base_url + path
        body = json.dumps(payload, indent=2)

        try:
            response = requests.patch(
                url,
                auth=self.auth,
                headers=self.headers,
                data=body,
                verify=False,
                timeout=10,
            )
            self._log_response("PATCH", url, response)
            return response

        except requests.exceptions.ConnectionError:
            print(f"{Colour.RED}✘  Connection refused.{Colour.RESET}")
            return None

        except requests.exceptions.Timeout:
            print(f"{Colour.RED}✘  Request timed out.{Colour.RESET}")
            return None

    # ── Convenience: get all interfaces ───────────────────────────────────────
    def get_all_interfaces(self) -> list:
        """
        Retrieves the full ietf-interfaces list from the device.
        Returns a list of interface dicts, or [] on failure.
        """
        from config import INTERFACES_PATH
        data = self.get(INTERFACES_PATH)
        if not data:
            return []

        # The YANG wrapper key may appear as either of these
        for key in ("ietf-interfaces:interfaces", "interfaces"):
            if key in data:
                container = data[key]
                iface_list = container.get("interface") or container.get("ietf-interfaces:interface", [])
                return iface_list if isinstance(iface_list, list) else [iface_list]

        return []

    # ── Internal logger ───────────────────────────────────────────────────────
    @staticmethod
    def _log_response(method: str, url: str, response: requests.Response):
        colour = Colour.GREEN if 200 <= response.status_code < 300 else Colour.RED
        symbol = "✔" if 200 <= response.status_code < 300 else "✘"
        print(
            f"{Colour.DIM}   {method} {url}{Colour.RESET}\n"
            f"   {colour}{symbol}  HTTP {response.status_code}{Colour.RESET}"
        )
