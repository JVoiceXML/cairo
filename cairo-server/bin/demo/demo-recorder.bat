@echo off

set PACKAGE=org.speechforge.cairo.demo.recorder
set CLASS=RecorderClient

set LOCAL_RTP_PORT=42046
@REM uncomment the following line if -loop mode is enabled (adds quit/exit to grammar)
@REM set GRAMMAR_URL=file:../grammar/example-loop.gram

start "%CLASS% - %GRAMMAR%" ..\..\bin\launch %PACKAGE%.%CLASS% %LOCAL_RTP_PORT%
