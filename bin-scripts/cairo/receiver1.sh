#!/bin/bash

# Locate this script.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export DISPLAY=:0
export CAIRO_RECORD_DIR=%%RECORD_DIR%%
CLASS=org.speechforge.cairo.server.resource.ReceiverResource
CAIRO_CONFIG=file:${CAIRO_HOME}/config/cairo-config.xml
RES_NAME=receiver1
sh ${SCRIPT_DIR}/launch.sh $CLASS $CAIRO_CONFIG $RES_NAME
