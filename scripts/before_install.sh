#! /bin/bash

if [ -e "/usr/bin/sbt" ]; then
  yum -y upgrade sbt
else
  curl https://bintray.com/sbt/rpm/rpm | sudo tee /etc/yum.repos.d/bintray-sbt-rpm.repo
  yum -y install sbt
fi
