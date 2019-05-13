#!/bin/bash

## Locate this script.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLASS="org.speechforge.cairo.server.resource.ResourceServerImpl"
sh ${SCRIPT_DIR}/launch.sh $CLASS -sipPort 5050 -sipTransport udp
