@echo off

set CLASS=org.speechforge.cairo.server.resource.TransmitterResource
set CAIRO_CONFIG=file:../config/cairo-config.xml
set RES_NAME=transmitter1

start "%RES_NAME%" launch %CLASS% "%CAIRO_CONFIG%" "%RES_NAME%"
