#!/usr/bin/env bash

HD=$PWD

function abort_everything {
  echo "Aborting..."
  cd ${HD}
  exit 1
}

trap 'abort_everything' SIGINT

if [ ! -d bundle ]
then mkdir bundle
fi
cd bundle

wget -q  -O android-sdk.tgz