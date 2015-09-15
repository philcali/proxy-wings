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

if [ ! -d "/var/awslogs/state" ] then
  mkdir -p /var/awslogs/state
fi
