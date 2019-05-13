#!/bin/bash

CLASS=org.speechforge.cairo.server.resource.ReceiverResource
CAIRO_CONFIG=file:../config/cairo-config.xml
RES_NAME=receiver1
sh launch.sh $CLASS $CAIRO_CONFIG $RES_NAME
