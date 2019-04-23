#!/bin/bash

PACKAGE=org.speechforge.cairo.demo.bargein
CLASS=BargeInClient

LOCAL_RTP_PORT=42046
GRAMMAR_URL="file:../grammar/example.gram"
PROMPT_TEXT="You can start speaking any time.  Would you like to hear the weather, get sports news or hear a stock quote?"

sh ../../bin/launch.sh $PACKAGE.$CLASS  $LOCAL_RTP_PORT "$GRAMMAR_URL" "$PROMPT_TEXT"
