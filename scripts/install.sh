#!/usr/bin/env bash
set -eo pipefail
IFS=$'\n\t'

#/ Usage: bash install.sh install-path
#/ Description: Compiles all cairo projects and installs the server into 'install-path'
#/ Example: bash install.sh /usr/local/halef-cairo
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

move_libs() {
    cp ${SCRIPT_DIR}/../src/cairo/cairo-server/trunk/target/buildlib/* lib/.
    cp ${SCRIPT_DIR}/../src/cairo/cairo-server/trunk/target/cairo-*.jar lib/.
}

move_bin() {
    cp -r ${SCRIPT_DIR}/../bin-scripts/ bin/
    cp -r ${SCRIPT_DIR}/../bash_helpers bin/bash_helpers/
    chmod +x bin/*.sh
    chmod +x bin/cairo/*.sh
}

move_config() {
    cp -r ${SCRIPT_DIR}/../config-template/ config-template/
}

if [[ "${BASH_SOURCE[0]}" = "$0" ]]; then
    install_path="${1}"
    mkdir -p "${install_path}"
    cd "${install_path}" || fatal "Could not create install_path."
    info "Compiling ..."
    bash ${SCRIPT_DIR}/compile.sh

    info "Moving binaries ..."
    cd "${install_path}"
    mkdir -p lib
    move_libs
    move_bin
    move_config
fi

