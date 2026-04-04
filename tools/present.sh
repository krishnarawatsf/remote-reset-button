#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

open "file://$PWD/ui/index.html?mode=present"
