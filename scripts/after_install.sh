#! /bin/bash

echo "$PWD"
sbt "clean" "project carwings-server" "assembly"
