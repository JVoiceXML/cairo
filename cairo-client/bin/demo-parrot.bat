@echo off

set PACKAGE=org.speechforge.cairo.client.demo.bargein
set CLASS=BargeInClient

set LOCAL_RTP_PORT=42046
set GRAMMAR_URL=file:../demo/grammar/example-loop.gram
set PROMPT_TEXT=You can start speaking any time.  Would you like to hear the weather, get sports news or hear a stock quote?  Say quit to exit.

start "Parrot - %GRAMMAR%" launch %PACKAGE%.%CLASS% -loop -parrot %LOCAL_RTP_PORT% "%GRAMMAR_URL%" "%PROMPT_TEXT%"
