#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

#/ Usage: bash compile.sh
#/ Description: Compiles all cairo projects.
#/ Options:
#/   --help: Display this help message
usage() { grep '^#/' "$0" | cut -c4- ; exit 0 ; }
expr "$*" : ".*--help" > /dev/null && usage

# Locate this script.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Import helper functions.
source ${SCRIPT_DIR}/../bash_helpers/utils.sh

environment() {
	# Test for required environment variables.
	check_null_or_unset ${JAVA_HOME:-""} "JAVA_HOME"
	check_null_or_unset ${M2_HOME:-""} "M2_HOME"
	info "Environment check passed."
}

if [[ "${BASH_SOURCE[0]}" = "$0" ]]; then
    bash ${SCRIPT_DIR}/compile-cairo-rtp.sh
    bash ${SCRIPT_DIR}/compile-cairo-sip.sh
    bash ${SCRIPT_DIR}/compile-cairo-client.sh
    bash ${SCRIPT_DIR}/compile-cairo-server.sh
fi

