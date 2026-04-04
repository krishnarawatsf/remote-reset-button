# =============================================================================
#  config.py  –  Remote Reset Button
#  Central configuration for the RESTCONF target device.
#
#  To switch from the mock server to the real Cisco DevNet Always-On sandbox,
#  change the values below:
#
#    HOST     = "devnetsandboxiosxe.cisco.com"
#    PORT     = 443
#    USERNAME = "developer"
#    PASSWORD = "C1sco12345"
#    USE_HTTPS = True
# =============================================================================

# ── Target Device ─────────────────────────────────────────────────────────────
HOST      = "localhost"
PORT      = 8080
USERNAME  = "admin"
PASSWORD  = "admin"
USE_HTTPS = False          # Set True for real Cisco sandbox (HTTPS)

# ── RESTCONF base URL (auto-built from above) ──────────────────────────────────
SCHEME   = "https" if USE_HTTPS else "http"
BASE_URL = f"{SCHEME}://{HOST}:{PORT}"

# ── RESTCONF paths (RFC 8040 + ietf-interfaces YANG model) ─────────────────────
INTERFACES_PATH      = "/restconf/data/ietf-interfaces:interfaces"
INTERFACE_PATH       = "/restconf/data/ietf-interfaces:interfaces/interface={name}"

# ── RESTCONF media type (RFC 8040 §5.2) ────────────────────────────────────────
YANG_JSON = "application/yang-data+json"

# ── Reboot settings ─────────────────────────────────────────────────────────────
REBOOT_WAIT_SECONDS  = 5   # Simulates power-down duration

# ── ANSI colour codes for terminal output ──────────────────────────────────────
class Colour:
    RESET  = "\033[0m"
    BOLD   = "\033[1m"
    DIM    = "\033[2m"
    RED    = "\033[91m"
    GREEN  = "\033[92m"
    YELLOW = "\033[93m"
    CYAN   = "\033[96m"
    WHITE  = "\033[97m"
    ORANGE = "\033[38;5;208m"
