# 🔄 The Remote Reset Button
### RESTCONF CRUD Capstone — Python + YANG + Cisco IOS-XE

> **Problem:** A security camera at a remote coffee shop freezes.  
> **Old Fix:** 2-hour drive, ladder, manual unplug.  
> **New Fix:** Run a Python script from your desk in 10 seconds.

---

## Project Files

```
remote-reset-button/
├── config.py              ← Target device settings (change here for real sandbox)
├── restconf_client.py     ← Shared GET / PATCH HTTP client
├── find_device.py         ← Phase 1: Search by device name (GET)
├── reboot_port.py         ← Phase 2: Power-cycle a port (PATCH × 2)
├── mock_server.py         ← Simulated Cisco IOS-XE RESTCONF device
├── demo.py                ← One-command full demo (for demo day)
├── docs/
│   ├── copilot-skill-guide.md
│   ├── copilot-instructions.md
│   ├── java-implementation.md
│   └── mock-server-reference.md
└── tools/
	└── scaffold.sh
```

---

## RESTCONF Operations Used

| Phase | Operation | HTTP | RESTCONF Path |
|-------|-----------|------|---------------|
| 1 | Search / Read | `GET` | `/restconf/data/ietf-interfaces:interfaces` |
| 2a | Shutdown port | `PATCH` | `/restconf/data/ietf-interfaces:interfaces/interface={name}` |
| 2b | Re-enable port | `PATCH` | `/restconf/data/ietf-interfaces:interfaces/interface={name}` |

**Why PATCH (not PUT)?** PATCH updates only the fields you specify (`enabled`). PUT would replace the entire interface config — dangerous on a live device.

---

## Requirements

```bash
pip install requests
```
Python 3.10+ required (uses `dict | None` union type hints).

Or install from the dependency manifest:

```bash
pip install -r requirements.txt
```

---

## How to Run

### Option 0 - Teacher-Friendly UI Walkthrough

Open the presentation UI in browser:

```bash
open ui/index.html
```

This page explains the project in a grading-friendly sequence: problem, architecture, execution phases, proof points, and validation checklist.

### Option 0.1 - One-Click Viva Presentation Mode

```bash
./tools/present.sh
```

Presentation controls:
- Right Arrow: next slide
- Left Arrow: previous slide
- N: show/hide speaker notes

### Option A — Full automated demo (recommended for demo day)
```bash
python demo.py
```
Starts the mock server, runs both phases, prints summary. One command.

---

### Option B — Manual step-by-step

**Terminal 1 — Start the mock server:**
```bash
python mock_server.py
```

**Terminal 2 — Phase 1: Find the device:**
```bash
python find_device.py "Camera 2"
python find_device.py "Access Point"
python find_device.py "GigabitEthernet3"
```
Expected output:
```
Found "Camera 2" on Interface GigabitEthernet2. Status: UP.
```

**Terminal 2 — Phase 2: Reboot the port:**
```bash
python reboot_port.py GigabitEthernet2
```
Expected output:
```
BEFORE: GigabitEthernet2  enabled = true  (UP)
[1/3]  PATCH → enabled: false  (shutting port DOWN)
[2/3]  Waiting 5s...
[3/3]  PATCH → enabled: true   (bringing port back UP)
AFTER:  GigabitEthernet2  enabled = true  (UP)
✔  Power cycle complete — GigabitEthernet2 is back online.
```

---

## Switching to a Real Cisco DevNet Sandbox

1. Go to https://devnetsandbox.cisco.com → reserve **IOS-XE Always-On**
2. Edit `config.py`:
```python
HOST      = "devnetsandboxiosxe.cisco.com"
PORT      = 443
USERNAME  = "developer"
PASSWORD  = "C1sco12345"
USE_HTTPS = True
```
3. Run the scripts — **no other code changes needed.**

### Verify on Router CLI (proof for report):
```
show interfaces GigabitEthernet2
show running-config interface GigabitEthernet2
```
Screenshot the output before and after running `reboot_port.py`.

---

## Pre-loaded Mock Interfaces

The mock server simulates a coffee shop network:

| Interface | Description | IP |
|-----------|-------------|-----|
| GigabitEthernet1 | WAN Uplink – ISP Connection | 203.0.113.1/30 |
| GigabitEthernet2 | Camera 2 – Entrance | 192.168.10.2/24 |
| GigabitEthernet3 | Access Point – Seating Area | 192.168.10.3/24 |
| GigabitEthernet4 | POS Terminal – Counter | 192.168.10.4/24 |
| Loopback0 | Management Interface | 10.0.0.1/32 |

---

## Team Roles

| Member | Script Owned |
|--------|-------------|
| Member 1 | `config.py` + `restconf_client.py` + integration |
| Member 2 | `find_device.py` (Phase 1 – GET) |
| Member 3 | `reboot_port.py` (Phase 2 – PATCH) |
| Member 4 | `mock_server.py` + `demo.py` + README + report |

---

## Project Completion Status

- Checklist: see `TODO.md`
- One-command verification:

```bash
./tools/verify.sh
```

This runs the mock server, validates find flow, validates reboot flow, and exits with success only when all checks pass.
