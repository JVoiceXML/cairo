#!/bin/bash

CLASS=org.speechforge.cairo.server.resource.TransmitterResource
CAIRO_CONFIG=file:../config/cairo-config.xml
RES_NAME=transmitter1
sh launch.sh $CLASS $CAIRO_CONFIG $RES_NAME
