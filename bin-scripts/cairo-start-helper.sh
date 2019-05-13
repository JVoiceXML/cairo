#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

#/ Usage: bash start-cairo.sh
#/ Description: Starts cairo components
#/ Example: bash start-cairo.sh &
#/ Options:
#/   --help: Display this help message
usage() { grep '^#/' "$0" | cut -c4- ; exit 0 ; }
expr "$*" : ".*--help" > /dev/null && usage

# Locate this script.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Import helper functions.
source ${SCRIPT_DIR}/bash_helpers/utils.sh

environment() {
	# Test for required environment variables.
	check_null_or_unset ${CAIRO_HOME:-""} "CAIRO_HOME"
	info "Environment check passed."
}

if [[ "${BASH_SOURCE[0]}" = "$0" ]]; then
	environment
	bash ${SCRIPT_DIR}/cairo/rserver.sh 2>&1 >> $CAIRO_HOME/logs/cairo &
	info "Waiting until rserver.sh has started..."
	sleep 2
	for i in {1..30}; do
		netstat -a -n|grep LISTEN|grep ":1099" >/dev/null
		[[ $? -eq 0 ]] && break
		sleep 5
		[[ $i -lt 30 ]] || exit 1
	done

	bash ${SCRIPT_DIR}/cairo/receiver1.sh 2>&1 >> $CAIRO_HOME/logs/cairo &
	bash ${SCRIPT_DIR}/cairo/transmitter1.sh 2>&1 >> $CAIRO_HOME/logs/cairo &
fi
