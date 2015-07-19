#! /bin/bash

if [ -z `which sbt` ] then
  curl https://bintray.com/sbt/rpm/rpm | sudo tee /etc/yum.repos.d/bintray-sbt-rpm.repo
  yum install sbt
fi
yum upgrade sbt
