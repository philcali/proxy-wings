#! /bin/bash

VALUE=`ps aux | grep java | grep -v "grep java"`
if [ -z "$VALUE" ]; then
  echo "Nothing to do... Skipping"
else
  killall java
fi
