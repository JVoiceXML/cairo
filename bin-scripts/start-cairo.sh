#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
nohup ${SCRIPT_DIR}/cairo-start-helper.sh &
