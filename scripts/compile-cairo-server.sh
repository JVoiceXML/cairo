#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

#/ Usage: bash compile-cairo-server.sh
#/ Description: Compiles cairo-server project and puts jars into `src/cairo/cairo-server/trunk/`
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
    cd ${SCRIPT_DIR}/../src/cairo/cairo-server/trunk/
    mvn -Dmaven.test.skip=true -DskipTests -Dgpg.skip clean install package -U
fi
