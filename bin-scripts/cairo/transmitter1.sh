#!/bin/bash

# Locate this script.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export DISPLAY=:0
CLASS=org.speechforge.cairo.server.resource.TransmitterResource
CAIRO_CONFIG=file:${CAIRO_HOME}/config/cairo-config.xml
RES_NAME=transmitter1
sh ${SCRIPT_DIR}/launch.sh $CLASS $CAIRO_CONFIG $RES_NAME
