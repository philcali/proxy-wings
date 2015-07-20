#! /bin/bash

cd /tmp/carwings
java -jar server/target/scala-2.11/carwings-server-assembly-1.0.0.jar 80 & > /dev/null 2>&1
