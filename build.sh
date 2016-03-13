#!/bin/bash

. ~/.profile

buildNumber=`cat ./src/main/resources/build.number`
let "buildNumber=buildNumber+1"
echo "$buildNumber" > ./src/main/resources/build.number

mvn clean compile install
