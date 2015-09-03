#! /bin/bash

if [ -e "/usr/bin/sbt" ]; then
  yum -y upgrade sbt
else
  curl https://bintray.com/sbt/rpm/rpm | tee /etc/yum.repos.d/bintray-sbt-rpm.repo
  yum -y install sbt
fi

if [ -z `which awslogsd` ]; then
  yum install -y awslogs
fi

cd /webapps/carwings
cp aws/awslogs.conf /etc/awslogs/awslogs.conf
aws s3 cp s3://proxywings.com/proxywings.jks proxywings.jks
