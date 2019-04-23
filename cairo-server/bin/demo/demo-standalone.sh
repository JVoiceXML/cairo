#!/bin/bash

PACKAGE=org.speechforge.cairo.demo.standalone
CLASS=StandaloneRecogClient

GRAMMAR_URL="file:../grammar/example.gram"
EXAMPLE_PHRASE="I would like sports news."

sh ../../bin/launch.sh $PACKAGE.$CLASS "$GRAMMAR_URL" "$EXAMPLE_PHRASE"
