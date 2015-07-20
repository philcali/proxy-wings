#! /bin/bash

cd /tmp/carwings
sbt "clean" "project carwings-server" "assembly"
