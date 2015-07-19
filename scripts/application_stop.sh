#! /bin/bash

if [ -z `ps aux | grep java | grep -x "grep java"` ]; then
  echo "Nothing to do... Skipping"
else
  killall java
fi
