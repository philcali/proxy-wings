#! /bin/bash

if [ -z `which sbt` ]; then
  curl https://bintray.com/sbt/rpm/rpm | sudo tee /etc/yum.repos.d/bintray-sbt-rpm.repo
  yum -y install sbt
fi
yum -y upgrade sbt
