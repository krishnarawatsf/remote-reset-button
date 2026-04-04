#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

python3 mock_server.py > /tmp/restconf-mock.log 2>&1 &
SERVER_PID=$!

cleanup() {
  if kill -0 "$SERVER_PID" >/dev/null 2>&1; then
    kill "$SERVER_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

sleep 1

echo "[1/2] Running find_device smoke check..."
python3 find_device.py "Camera 2" >/tmp/restconf-find.log

echo "[2/2] Running reboot_port smoke check..."
python3 reboot_port.py GigabitEthernet2 >/tmp/restconf-reboot.log

echo "Verification succeeded."
echo "- Mock log:   /tmp/restconf-mock.log"
echo "- Find log:   /tmp/restconf-find.log"
echo "- Reboot log: /tmp/restconf-reboot.log"
