#! /bin/bash

cd /tmp/carwings
PROXY_OPTIONS=`cat /home/ec2-user/credentials`
java $PROXY_OPTIONS -jar server/target/scala-2.11/carwings-server-assembly-1.0.1.jar 443 > /dev/null 2>&1 &
