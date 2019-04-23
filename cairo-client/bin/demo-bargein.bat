@echo off

set PACKAGE=org.speechforge.cairo.client.demo.bargein
set CLASS=BargeInClient

set LOCAL_RTP_PORT=42046
set GRAMMAR_URL=file:../demo/grammar/example.gram
set PROMPT_TEXT=You can start speaking any time.  Would you like to hear the weather, get sports news or hear a stock quote?

start "%CLASS% - %GRAMMAR%" launch %PACKAGE%.%CLASS% %LOCAL_RTP_PORT% "%GRAMMAR_URL%" "%PROMPT_TEXT%"
