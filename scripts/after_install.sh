#! /bin/bash

cd /webapps/carwings
sbt "clean" "project carwings-server" "assembly"
