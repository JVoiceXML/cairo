@echo off

set PACKAGE=org.speechforge.cairo.demo.hotword
set CLASS=HotwordClient

set LOCAL_RTP_PORT=42046
set GRAMMAR_URL=file:../grammar/hotword.gram
set PROMPT_TEXT=Say Computer, if you want to get my attention.

start "%CLASS% - %GRAMMAR%" ..\..\bin\launch %PACKAGE%.%CLASS% %LOCAL_RTP_PORT% "%GRAMMAR_URL%" "%PROMPT_TEXT%"
