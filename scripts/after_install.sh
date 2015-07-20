#! /bin/bash

cd /tmp/webapp
sbt "clean" "project carwings-server" "assembly"
