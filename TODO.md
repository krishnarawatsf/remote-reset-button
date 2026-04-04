# Project TODO and Completion Status

## Core Deliverables

- [x] Organize files by use case (runtime, docs, tools)
- [x] Ensure RESTCONF headers and YANG payload wrapper usage
- [x] Verify GET search flow (`find_device.py`)
- [x] Verify PATCH reboot flow (`reboot_port.py`)
- [x] Validate local mock server behavior (`mock_server.py`)
- [x] Update project documentation structure (`README.md`)
- [x] Add Python dependency manifest (`requirements.txt`)
- [x] Add one-command verification script (`tools/verify.sh`)

## Verification Evidence

- Executed `python3 find_device.py 'Camera 2'` -> HTTP 200 and device match found.
- Executed `python3 reboot_port.py GigabitEthernet2` -> PATCH 204 down/up cycle succeeded.
- Confirmed mock server request logs include GET and PATCH requests with expected paths.

## Remaining Optional Enhancements

- [ ] Add Python unit tests for `RestconfClient`
- [ ] Add CI workflow for lint + smoke tests
- [ ] Add Java runnable sample project under a dedicated `java/` folder
