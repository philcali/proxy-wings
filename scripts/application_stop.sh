#! /bin/bash

if [ -z `ps aux | grep java | grep -v "grep java"` ]; then
  echo "Nothing to do... Skipping"
else
  killall java
fi
