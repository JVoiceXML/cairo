@echo off

set PACKAGE=org.speechforge.cairo.demo.recog
set CLASS=RecognitionClient

set LOCAL_RTP_PORT=42046
set GRAMMAR_URL=file:../grammar/example.gram
@REM uncomment the following line if -loop mode is enabled (adds quit/exit to grammar)
@REM set GRAMMAR_URL=file:../grammar/example-loop.gram
set EXAMPLE_PHRASE=I would like sports news.

start "%CLASS% - %GRAMMAR%" ..\..\bin\launch %PACKAGE%.%CLASS% %LOCAL_RTP_PORT% "%GRAMMAR_URL%" "%EXAMPLE_PHRASE%"
