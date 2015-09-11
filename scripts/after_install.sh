#! /bin/bash

cd /webapps/carwings
cp aws/awslogs.conf /etc/awslogs/awslogs.conf
aws s3 cp s3://proxywings.com/proxywings.jks proxywings.jks
sbt "clean" "project carwings-server" "assembly"
