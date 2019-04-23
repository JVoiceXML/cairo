@echo off

set CLASS=org.speechforge.cairo.server.resource.ReceiverResource
set CAIRO_CONFIG=file:../config/cairo-config.xml
set RES_NAME=receiver1

start "%RES_NAME%" launch %CLASS% "%CAIRO_CONFIG%" "%RES_NAME%"
