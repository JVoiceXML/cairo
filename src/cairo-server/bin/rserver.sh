#!/bin/bash

CLASS="org.speechforge.cairo.server.resource.ResourceServerImpl"
sh launch.sh $CLASS -sipPort 5050 -sipTransport udp
