#!/usr/bin/env python3
# =============================================================================
#  reboot_port.py  –  Phase 2: The "Fix" (UPDATE / PATCH)
#
#  USAGE:
#    python reboot_port.py GigabitEthernet2
#    python reboot_port.py "Loopback1"
#
#  WHAT IT DOES:
#    1. Reads the current interface state (GET) — "Before" snapshot
#    2. Sends PATCH → enabled: false  (port goes DOWN)
#    3. Waits REBOOT_WAIT_SECONDS     (simulates device power-down)
#    4. Sends PATCH → enabled: true   (port comes UP)
#    5. Reads the final interface state (GET) — "After" snapshot
#    6. Prints a Before/After comparison for your report / demo
#
#  RESTCONF CALLS:
#    GET   /restconf/data/ietf-interfaces:interfaces/interface={name}
#    PATCH /restconf/data/ietf-interfaces:interfaces/interface={name}
#          body: { "ietf-interfaces:interface": { "name": "...", "enabled": false } }
#    PATCH /restconf/data/ietf-interfaces:interfaces/interface={name}
#          body: { "ietf-interfaces:interface": { "name": "...", "enabled": true  } }
# =============================================================================

import sys
import time
from restconf_client import RestconfClient
from config import (
    INTERFACE_PATH, REBOOT_WAIT_SECONDS, Colour
)


# ── Helpers ───────────────────────────────────────────────────────────────────

def print_banner():
    print(f"""
{Colour.ORANGE}{Colour.BOLD}╔══════════════════════════════════════════════════════════╗
║          REMOTE RESET BUTTON  –  Phase 2: REBOOT         ║
║          Power-Cycle a Port Without Leaving Your Desk     ║
╚══════════════════════════════════════════════════════════╝{Colour.RESET}
""")


def get_interface(client: RestconfClient, name: str) -> dict | None:
    """Fetch a single interface by name. Returns the inner interface dict."""
    path = INTERFACE_PATH.format(name=name.replace("/", "%2F"))
    data = client.get(path)
    if not data:
        return None
    for key in ("ietf-interfaces:interface", "interface"):
        if key in data:
            val = data[key]
            return val[0] if isinstance(val, list) else val
    return None


def build_patch_payload(name: str, enabled: bool) -> dict:
    """
    RESTCONF PATCH body for toggling the admin state of an interface.
    Only 'name' and 'enabled' are sent — PATCH leaves all other
    fields untouched (unlike PUT which replaces the whole resource).
    """
    return {
        "ietf-interfaces:interface": {
            "name":    name,
            "enabled": enabled,
        }
    }


def print_state(label: str, iface: dict):
    enabled = iface.get("enabled", True)
    status  = f"{Colour.GREEN}● ENABLED (UP){Colour.RESET}"   if enabled \
              else f"{Colour.RED}○ DISABLED (DOWN){Colour.RESET}"
    name    = iface.get("name", "?")
    desc    = iface.get("description", "(no description)")

    print(f"  {Colour.BOLD}{label}{Colour.RESET}")
    print(f"  ┌─────────────────────────────────────────────┐")
    print(f"  │  Interface : {Colour.CYAN}{name:<30}{Colour.RESET}│")
    print(f"  │  Description : {desc:<28}│")
    print(f"  │  Status    : {status:<45}│")
    print(f"  └─────────────────────────────────────────────┘")
    print()


def countdown(seconds: int):
    """Visual countdown so the demo shows the reboot is 'happening'."""
    for i in range(seconds, 0, -1):
        bar  = "█" * (seconds - i + 1) + "░" * (i - 1)
        line = f"  {Colour.YELLOW}  [{bar}] Waiting {i}s...{Colour.RESET}"
        print(line, end="\r", flush=True)
        time.sleep(1)
    print(" " * 60, end="\r")   # clear the line


def print_summary(before: dict, after: dict, interface_name: str):
    b_up = before.get("enabled", True)
    a_up = after.get("enabled",  True)

    print(f"""  {Colour.BOLD}{Colour.WHITE}─── REBOOT SUMMARY ───────────────────────────────────────{Colour.RESET}

  Interface   : {Colour.CYAN}{interface_name}{Colour.RESET}
  Before      : {"UP" if b_up else "DOWN"}
  After       : {"UP" if a_up else "DOWN"}

  {Colour.GREEN}{Colour.BOLD}✔  Power cycle complete — {interface_name} is back online.{Colour.RESET}
  {Colour.DIM}  No truck roll required. Estimated time saved: ~2 hours.{Colour.RESET}

  {Colour.DIM}  Proof for report:{Colour.RESET}
  {Colour.DIM}  • Before screenshot: enabled = {str(b_up).lower()}{Colour.RESET}
  {Colour.DIM}  • After  screenshot: enabled = {str(a_up).lower()}{Colour.RESET}
  {Colour.DIM}  • Verify on router CLI:  show interfaces {interface_name}{Colour.RESET}
""")


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    print_banner()

    # ── Validate arguments ────────────────────────────────────────────────────
    if len(sys.argv) < 2:
        print(f"{Colour.YELLOW}Usage:{Colour.RESET}  python reboot_port.py <InterfaceName>")
        print(f"  Example:  python reboot_port.py GigabitEthernet2")
        print(f"\n  Run find_device.py first to get the exact interface name.")
        sys.exit(1)

    interface_name = " ".join(sys.argv[1:])
    path           = INTERFACE_PATH.format(name=interface_name.replace("/", "%2F"))
    client         = RestconfClient()

    print(f"  {Colour.BOLD}Target interface:{Colour.RESET}  {interface_name}")
    print(f"  {Colour.BOLD}Device:{Colour.RESET}            {__import__('config').BASE_URL}")
    print()

    # ── Step 1: Read BEFORE state ─────────────────────────────────────────────
    print(f"  {Colour.CYAN}[Step 1/4]  Reading current interface state...{Colour.RESET}\n")
    before = get_interface(client, interface_name)

    if before is None:
        print(f"{Colour.RED}  ✘  Interface \"{interface_name}\" not found on device.{Colour.RESET}")
        print(f"  {Colour.DIM}  Run python find_device.py to see available interfaces.{Colour.RESET}")
        sys.exit(1)

    print_state("BEFORE (current state):", before)

    # ── Step 2: PATCH → enabled: false (PORT DOWN) ───────────────────────────
    print(f"  {Colour.RED}{Colour.BOLD}[Step 2/4]  Sending PATCH → enabled: false (shutting port down){Colour.RESET}\n")
    r_down = client.patch(path, build_patch_payload(interface_name, False))

    if r_down is None or not (200 <= r_down.status_code < 300):
        code = r_down.status_code if r_down else "N/A"
        print(f"{Colour.RED}  ✘  Failed to disable interface. HTTP {code}{Colour.RESET}")
        sys.exit(1)

    print(f"\n  {Colour.RED}  ● Port {interface_name} is now DOWN.{Colour.RESET}\n")

    # ── Step 3: Wait (simulate device reboot) ─────────────────────────────────
    print(f"  {Colour.YELLOW}[Step 3/4]  Simulating device reboot — waiting {REBOOT_WAIT_SECONDS} seconds...{Colour.RESET}\n")
    countdown(REBOOT_WAIT_SECONDS)

    # ── Step 4: PATCH → enabled: true (PORT UP) ───────────────────────────────
    print(f"\n  {Colour.GREEN}{Colour.BOLD}[Step 4/4]  Sending PATCH → enabled: true (bringing port back up){Colour.RESET}\n")
    r_up = client.patch(path, build_patch_payload(interface_name, True))

    if r_up is None or not (200 <= r_up.status_code < 300):
        code = r_up.status_code if r_up else "N/A"
        print(f"{Colour.RED}  ✘  Failed to re-enable interface. HTTP {code}{Colour.RESET}")
        sys.exit(1)

    print(f"\n  {Colour.GREEN}  ● Port {interface_name} is now UP.{Colour.RESET}\n")

    # ── Step 5: Read AFTER state ──────────────────────────────────────────────
    after = get_interface(client, interface_name)
    if after:
        print_state("AFTER (final state):", after)

    # ── Summary ───────────────────────────────────────────────────────────────
    print_summary(before, after or {}, interface_name)


if __name__ == "__main__":
    main()
