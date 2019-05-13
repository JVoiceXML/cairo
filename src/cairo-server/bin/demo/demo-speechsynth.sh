#!/bin/bash

PACKAGE=org.speechforge.cairo.demo.tts
CLASS=SpeechSynthClient

LOCAL_RTP_PORT=42046
PROMPT_TEXT="Congratulations! You have successfully installed the Cairo speech server. Please try the other demos to test out Cairo's speech recognition capabilities."

sh ../../bin/launch.sh $PACKAGE.$CLASS $LOCAL_RTP_PORT "$PROMPT_TEXT"
