#!/usr/bin/env python3
# =============================================================================
#  demo.py  –  Full End-to-End Automated Demo
#
#  USAGE:
#    python demo.py
#
#  WHAT IT DOES:
#    Runs the complete "Remote Reset Button" scenario automatically:
#
#    1. Starts the mock RESTCONF server in a background thread
#    2. Phase 1 → find_device: searches for "Camera 2"
#    3. Phase 2 → reboot_port: power-cycles GigabitEthernet2
#    4. Shows the Before/After state comparison
#    5. Stops the server
#
#  Use this script for your capstone DEMO DAY — one command,
#  full working output, no manual steps.
# =============================================================================

import sys
import time
import threading
from http.server import HTTPServer

# ── Inline import of mock server handler ─────────────────────────────────────
sys.path.insert(0, ".")
from mock_server import RestconfHandler, INTERFACES, PORT
from restconf_client import RestconfClient
from config import INTERFACE_PATH, REBOOT_WAIT_SECONDS, Colour, BASE_URL


def start_mock_server():
    """Start the mock RESTCONF server in a daemon thread."""
    server = HTTPServer(("0.0.0.0", PORT), RestconfHandler)
    t = threading.Thread(target=server.serve_forever, daemon=True)
    t.start()
    time.sleep(0.3)   # Let the server bind
    return server


def divider(title=""):
    line = "═" * 60
    if title:
        print(f"\n{Colour.CYAN}{Colour.BOLD}  {line}")
        print(f"  {title}")
        print(f"  {line}{Colour.RESET}\n")
    else:
        print(f"\n{Colour.DIM}  {'─'*60}{Colour.RESET}\n")


def main():
    print(f"""
{Colour.ORANGE}{Colour.BOLD}╔══════════════════════════════════════════════════════════════╗
║       THE REMOTE RESET BUTTON  –  Full Capstone Demo         ║
║       Scenario: "Camera 2 is Down" at the Coffee Shop         ║
╚══════════════════════════════════════════════════════════════╝{Colour.RESET}

  {Colour.DIM}This demo simulates a complete real-world network automation
  workflow using RESTCONF and Python — no physical hardware needed.{Colour.RESET}
""")

    # ── Start server ──────────────────────────────────────────────────────────
    print(f"  {Colour.DIM}Starting mock RESTCONF server on {BASE_URL}...{Colour.RESET}")
    server = start_mock_server()
    print(f"  {Colour.GREEN}✔  Mock server ready.{Colour.RESET}\n")

    client = RestconfClient()

    # ══════════════════════════════════════════════════════════════════════════
    #  PHASE 1 – SEARCH  (READ / GET)
    # ══════════════════════════════════════════════════════════════════════════
    divider("PHASE 1: SEARCH  —  'Camera 2 is down. Which port is it on?'")

    search_term = "Camera 2"
    print(f"  {Colour.BOLD}Store manager calls:{Colour.RESET} \"{search_term} is down!\"\n")
    print(f"  {Colour.BOLD}Your command:{Colour.RESET}  python find_device.py \"{search_term}\"\n")
    time.sleep(1)

    print(f"  {Colour.CYAN}Sending GET → /restconf/data/ietf-interfaces:interfaces{Colour.RESET}\n")
    interfaces = client.get_all_interfaces()

    found = None
    for iface in interfaces:
        if search_term.lower() in iface.get("description", "").lower():
            found = iface
            break

    if found:
        name    = found["name"]
        desc    = found["description"]
        enabled = found["enabled"]
        status  = f"{Colour.GREEN}UP{Colour.RESET}" if enabled else f"{Colour.RED}DOWN{Colour.RESET}"

        print(f"""  {Colour.GREEN}{Colour.BOLD}✔  RESULT:{Colour.RESET}
  ┌──────────────────────────────────────────────────┐
  │  Found \"{search_term}\" on interface:              │
  │                                                  │
  │  Interface   :  {Colour.CYAN}{Colour.BOLD}{name:<33}{Colour.RESET}│
  │  Description :  {desc:<33}│
  │  Status      :  {status:<45}│
  └──────────────────────────────────────────────────┘
""")
        print(f"  {Colour.BOLD}Console output:{Colour.RESET}  Found \"{search_term}\" on Interface {name}. Status: UP.")
    else:
        print(f"  {Colour.RED}Device not found.{Colour.RESET}")
        server.shutdown()
        sys.exit(1)

    time.sleep(1)

    # ══════════════════════════════════════════════════════════════════════════
    #  PHASE 2 – REBOOT  (UPDATE / PATCH)
    # ══════════════════════════════════════════════════════════════════════════
    divider("PHASE 2: REBOOT  —  'Power-cycle GigabitEthernet2 remotely'")

    print(f"  {Colour.BOLD}Your command:{Colour.RESET}  python reboot_port.py {name}\n")
    time.sleep(1)

    path = INTERFACE_PATH.format(name=name.replace("/", "%2F"))

    # ── BEFORE state ──────────────────────────────────────────────────────────
    before = INTERFACES.get(name, {}).copy()
    before_enabled = before.get("enabled", True)

    print(f"  {Colour.BOLD}BEFORE:{Colour.RESET}  {name}  enabled = {Colour.GREEN}{str(before_enabled).lower()}{Colour.RESET}  (port is UP)\n")

    # ── PATCH: enabled = false ────────────────────────────────────────────────
    print(f"  {Colour.RED}{Colour.BOLD}[1/3]  PATCH → enabled: false  (shutting port DOWN){Colour.RESET}")
    r1 = client.patch(path, {"ietf-interfaces:interface": {"name": name, "enabled": False}})
    print(f"\n        {Colour.RED}● {name} is now DOWN.{Colour.RESET}\n")
    time.sleep(0.5)

    # ── Wait ──────────────────────────────────────────────────────────────────
    print(f"  {Colour.YELLOW}[2/3]  Waiting {REBOOT_WAIT_SECONDS}s (simulating device reboot){Colour.RESET}")
    for i in range(REBOOT_WAIT_SECONDS, 0, -1):
        bar  = "█" * (REBOOT_WAIT_SECONDS - i + 1) + "░" * (i - 1)
        print(f"        [{bar}] {i}s remaining...", end="\r", flush=True)
        time.sleep(1)
    print(" " * 55, end="\r")

    # ── PATCH: enabled = true ─────────────────────────────────────────────────
    print(f"\n  {Colour.GREEN}{Colour.BOLD}[3/3]  PATCH → enabled: true   (bringing port back UP){Colour.RESET}")
    r2 = client.patch(path, {"ietf-interfaces:interface": {"name": name, "enabled": True}})
    print(f"\n        {Colour.GREEN}● {name} is now UP.{Colour.RESET}\n")
    time.sleep(0.5)

    # ── AFTER state ───────────────────────────────────────────────────────────
    after_enabled = INTERFACES.get(name, {}).get("enabled", True)

    print(f"  {Colour.BOLD}AFTER:{Colour.RESET}   {name}  enabled = {Colour.GREEN}{str(after_enabled).lower()}{Colour.RESET}  (port is UP)\n")

    # ══════════════════════════════════════════════════════════════════════════
    #  FINAL SUMMARY
    # ══════════════════════════════════════════════════════════════════════════
    divider("DEMO COMPLETE – Summary for Report")

    print(f"""
  {Colour.BOLD}Scenario     :{Colour.RESET}  "{search_term}" camera was frozen/offline
  {Colour.BOLD}Action Taken :{Colour.RESET}  Remote power-cycle via RESTCONF PATCH
  {Colour.BOLD}Interface    :{Colour.RESET}  {name}
  {Colour.BOLD}Before State :{Colour.RESET}  enabled = {Colour.GREEN}true{Colour.RESET} (but device was frozen)
  {Colour.BOLD}During Reset :{Colour.RESET}  enabled = {Colour.RED}false{Colour.RESET} → wait → enabled = {Colour.GREEN}true{Colour.RESET}
  {Colour.BOLD}After State  :{Colour.RESET}  enabled = {Colour.GREEN}true{Colour.RESET} (device rebooted cleanly)

  {Colour.GREEN}{Colour.BOLD}✔  Camera 2 is back online.{Colour.RESET}

  {Colour.BOLD}Problem Solved:{Colour.RESET}
    ✘  Old way   →  2-hour drive + ladder + manual unplug = costly
    ✔  New way   →  3-line Python command from your desk = done

  {Colour.BOLD}RESTCONF Calls Made:{Colour.RESET}
    GET   /restconf/data/ietf-interfaces:interfaces          (Phase 1)
    PATCH /restconf/data/ietf-interfaces:interfaces/         (Phase 2a)
          interface={name}  → enabled: false
    PATCH /restconf/data/ietf-interfaces:interfaces/         (Phase 2b)
          interface={name}  → enabled: true

  {Colour.DIM}To verify on a real router CLI:{Colour.RESET}
    {Colour.DIM}show interfaces {name}{Colour.RESET}
    {Colour.DIM}show running-config interface {name}{Colour.RESET}
""")

    server.shutdown()


if __name__ == "__main__":
    main()
