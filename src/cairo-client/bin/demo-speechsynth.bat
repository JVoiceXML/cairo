@echo off

set PACKAGE=org.speechforge.cairo.client.demo.tts
set CLASS=SpeechSynthClient

set LOCAL_RTP_PORT=42046
set PROMPT_TEXT=Congratulations! You have successfully installed the Cairo speech server. Please try the other demos to test out Cairo's speech recognition capabilities.

start "%CLASS% - %GRAMMAR%" launch %PACKAGE%.%CLASS% %LOCAL_RTP_PORT% "%PROMPT_TEXT%"
