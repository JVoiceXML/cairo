#!/bin/bash

error() {
   echo "Error occurred"
   exit 1
}

CPATH="$CPATH:$CAIRO_HOME:$CAIRO_HOME/config"

for file in $( find $CAIRO_HOME/lib -name '*.jar' |sort)
do
    CPATH="$CPATH:$file"
done

export CPATH=${CPATH}:${CLASSPATH}
echo CPATH=$CPATH

java -cp $CPATH  -Xmx2048m  -XX:+UseConcMarkSweepGC -Dlog4j.configuration=log4j.xml -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 "$@"
exit 0

