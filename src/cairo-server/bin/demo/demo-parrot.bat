@echo off

set PACKAGE=org.speechforge.cairo.demo.bargein
set CLASS=BargeInClient

set LOCAL_RTP_PORT=42046
set GRAMMAR_URL=file:../grammar/example-loop.gram
set PROMPT_TEXT=You can start speaking any time.  Would you like to hear the weather, get sports news or hear a stock quote?  Say quit to exit.

start "Parrot - %GRAMMAR%" ..\..\bin\launch %PACKAGE%.%CLASS% -loop -parrot %LOCAL_RTP_PORT% "%GRAMMAR_URL%" "%PROMPT_TEXT%"
