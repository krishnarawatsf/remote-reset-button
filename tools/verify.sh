#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "[1/3] Building Java module..."
(cd java && mvn -q clean package)

echo "[2/3] Running Java tests..."
(cd java && mvn -q test)

echo "[3/3] Running Java CLI smoke check..."
(cd java && mvn -q spring-boot:run -Dspring-boot.run.arguments="find Camera 2") >/tmp/restconf-java-find.log

echo "Verification succeeded."
echo "- Java find log: /tmp/restconf-java-find.log"
