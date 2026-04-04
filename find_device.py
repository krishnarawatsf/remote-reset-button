#!/usr/bin/env python3
# =============================================================================
#  find_device.py  –  Phase 1: The "Search" (READ / GET)
#
#  USAGE:
#    python find_device.py "Camera 2"
#    python find_device.py "Access Point"
#    python find_device.py "GigabitEthernet1"
#
#  WHAT IT DOES:
#    1. Sends a GET request to the router asking for ALL interfaces
#    2. Searches every interface's "description" field for your search term
#    3. If found, prints the interface name, description, and current status
#    4. If not found, lists all available interfaces so you know what exists
#
#  RESTCONF CALL:
#    GET /restconf/data/ietf-interfaces:interfaces
#    Accept: application/yang-data+json
# =============================================================================

import sys
from restconf_client import RestconfClient
from config import Colour


# ── Helpers ───────────────────────────────────────────────────────────────────

def print_banner():
    print(f"""
{Colour.CYAN}{Colour.BOLD}╔══════════════════════════════════════════════════════════╗
║          REMOTE RESET BUTTON  –  Phase 1: SEARCH         ║
║          Find Device by Name / Description                ║
╚══════════════════════════════════════════════════════════╝{Colour.RESET}
""")


def format_status(enabled: bool) -> str:
    if enabled:
        return f"{Colour.GREEN}● UP (enabled){Colour.RESET}"
    return f"{Colour.RED}○ DOWN (disabled){Colour.RESET}"


def search_interfaces(interfaces: list, search_term: str) -> list:
    """
    Case-insensitive search across interface name AND description.
    Returns all matching interface dicts.
    """
    term  = search_term.lower()
    found = []
    for iface in interfaces:
        name = iface.get("name", "")
        desc = iface.get("description", "")
        if term in name.lower() or term in desc.lower():
            found.append(iface)
    return found


def print_match(iface: dict):
    name    = iface.get("name", "Unknown")
    desc    = iface.get("description", "(no description)")
    enabled = iface.get("enabled", True)
    ip      = iface.get("ipv4", {}).get("address", [{}])[0].get("ip", "N/A") \
              if "ipv4" in iface else iface.get("ipAddress", "N/A")
    prefix  = iface.get("ipv4", {}).get("address", [{}])[0].get("prefix-length", "") \
              if "ipv4" in iface else iface.get("prefixLength", "")

    ip_str = f"{ip}/{prefix}" if prefix else ip

    print(f"""  {Colour.BOLD}{Colour.WHITE}Interface Found:{Colour.RESET}
  ┌────────────────────────────────────────────┐
  │  Name        : {Colour.CYAN}{Colour.BOLD}{name:<28}{Colour.RESET}│
  │  Description : {desc:<28}│
  │  Status      : {format_status(enabled):<45}│
  │  IP Address  : {ip_str:<28}│
  └────────────────────────────────────────────┘
""")


def print_all_interfaces(interfaces: list):
    print(f"  {Colour.YELLOW}Available interfaces on this device:{Colour.RESET}")
    print(f"  {'Interface':<30} {'Description':<30} Status")
    print(f"  {'─'*30} {'─'*30} {'─'*10}")
    for iface in interfaces:
        name    = iface.get("name", "?")
        desc    = iface.get("description", "(none)")
        enabled = iface.get("enabled", True)
        status  = f"{Colour.GREEN}UP{Colour.RESET}" if enabled else f"{Colour.RED}DOWN{Colour.RESET}"
        print(f"  {name:<30} {desc:<30} {status}")
    print()


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    print_banner()

    # Validate arguments
    if len(sys.argv) < 2:
        print(f"{Colour.YELLOW}Usage:{Colour.RESET}  python find_device.py <search_term>")
        print(f"  Example:  python find_device.py \"Camera 2\"")
        sys.exit(1)

    search_term = " ".join(sys.argv[1:])
    print(f"  {Colour.BOLD}Searching for:{Colour.RESET}  \"{search_term}\"")
    print(f"  {Colour.BOLD}Target device:{Colour.RESET}  {__import__('config').BASE_URL}")
    print()

    # ── Phase 1: GET all interfaces ───────────────────────────────────────────
    client     = RestconfClient()
    interfaces = client.get_all_interfaces()

    if not interfaces:
        print(f"{Colour.RED}  No interfaces returned. Check the server is running.{Colour.RESET}")
        sys.exit(1)

    print(f"  {Colour.DIM}Retrieved {len(interfaces)} interface(s) from device.{Colour.RESET}\n")

    # ── Search ────────────────────────────────────────────────────────────────
    matches = search_interfaces(interfaces, search_term)

    if not matches:
        print(f"{Colour.YELLOW}  ⚠  No interface found matching \"{search_term}\".{Colour.RESET}\n")
        print_all_interfaces(interfaces)
        print(f"  {Colour.DIM}Tip: Try python reboot_port.py <InterfaceName> with an exact name above.{Colour.RESET}")
        sys.exit(0)

    # ── Print results ─────────────────────────────────────────────────────────
    print(f"  {Colour.GREEN}{Colour.BOLD}✔  Found {len(matches)} match(es) for \"{search_term}\":{Colour.RESET}\n")
    for match in matches:
        print_match(match)

    # Hint to the next phase
    if len(matches) == 1:
        name = matches[0].get("name", "")
        print(f"  {Colour.CYAN}▶  Ready to reboot? Run:{Colour.RESET}")
        print(f"     python reboot_port.py \"{name}\"\n")


if __name__ == "__main__":
    main()
