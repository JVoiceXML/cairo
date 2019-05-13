#!/usr/bin/env bash
set -eo pipefail
IFS=$'\n\t'

#/ Usage: bash configure.sh
#/ Description: Configures the installation in $CAIRO_HOME
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
	check_null_or_unset ${CAIRO_HOME:-""} "CAIRO_HOME"
	info "Environment check passed."
}

set_defaults() {
    info "Setting default values."
    info "Getting local IP address if we are on AWS"
    default_ip_address=$(get_aws_internal_ip)
    if ! check_null_or_unset $default_ip_address; then
    	info "Looks like we are not on AWS. Setting default_ip_address to 127.0.0.1 instead."
    	default_ip_address=127.0.0.1
    fi
    default_record_dir=/tmp/cairo-record
    default_grammar_dir=/tmp/cairo-grammar
    default_prompt_dir=/tmp/cairo-prompt
    default_tts_voice=dfki-spike
}

if [[ "${BASH_SOURCE[0]}" = "$0" ]]; then
	environment
	set_defaults

	cd ${CAIRO_HOME}

	echo "Enter IP address [${default_ip_address}]"
	read ip_address
	ip_address=${ip_address:-${default_ip_address}}

	echo "Enter recording dir [${default_record_dir}]"
	read record_dir
	record_dir=${record_dir:-${default_record_dir}}

	echo "Enter grammar dir [${default_grammar_dir}]"
	read grammar_dir
	grammar_dir=${grammar_dir:-${default_grammar_dir}}

	echo "Enter prompt dir [${default_prompt_dir}]"
	read prompt_dir
	prompt_dir=${prompt_dir:-${default_prompt_dir}}

	echo "Enter TTS voice [${default_tts_voice}]"
	read tts_voice
	tts_voice=${tts_voice:-${default_tts_voice}}

	if [ -d "config" ]; then
		rm -rf config
	fi
	mkdir config
	mkdir -p ${record_dir} ${grammar_dir} ${prompt_dir}
	mkdir -p ${CAIRO_HOME}/logs
	touch ${CAIRO_HOME}/logs/cairo.log

	chown cairo ${record_dir}
	chown cairo ${grammar_dir}
	chown cairo ${prompt_dir}
	chown cairo ${CAIRO_HOME}/logs/cairo.log


	cp config-template/* config/.
	sed -i -e "s,%%IP%%,$ip_address,g" config/cairo-config.xml
	sed -i -e "s,%%GRAMMAR_DIR%%,$grammar_dir,g" config/cairo-config.xml
	sed -i -e "s,%%PROMPT_DIR%%,$prompt_dir,g" config/cairo-config.xml
	sed -i -e "s,%%VOICE%%,$tts_voice,g" config/cairo-config.xml
	sed -i -e "s,%%RECORD_DIR%%,$record_dir,g" config/cairo-config.xml
	sed -i -e "s,%%RECORD_DIR%%,$record_dir,g" bin/cairo/receiver1.sh

	info "Done."
fi

