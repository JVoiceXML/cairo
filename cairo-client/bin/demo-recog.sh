#!/bin/bash

PACKAGE=org.speechforge.cairo.client.demo.recog
CLASS=RecognitionClient

LOCAL_RTP_PORT=42046
GRAMMAR_URL="file:../demo/grammar/example.gram"
# uncomment the following line if -loop mode is enabled (adds quit/exit to grammar)
# set GRAMMAR_URL=file:../grammar/example-loop.gram
EXAMPLE_PHRASE="I would like sports news."

sh  launch.sh "$PACKAGE.$CLASS"  $LOCAL_RTP_PORT "$GRAMMAR_URL" "$EXAMPLE_PHRASE"
