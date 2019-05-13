@echo off

set PACKAGE=org.speechforge.cairo.demo.standalone
set CLASS=StandaloneRecogClient

set GRAMMAR_URL=file:../grammar/example.gram
set EXAMPLE_PHRASE=I would like sports news.

start "%CLASS% - %GRAMMAR%" ..\..\bin\launch %PACKAGE%.%CLASS% "%GRAMMAR_URL%" "%EXAMPLE_PHRASE%"
