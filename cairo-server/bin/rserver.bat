@echo off
set CLASS=org.speechforge.cairo.server.resource.ResourceServerImpl
start "rserver" launch %CLASS% -sipPort 5050 -sipTransport udp
