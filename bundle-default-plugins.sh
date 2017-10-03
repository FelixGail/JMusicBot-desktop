#!/usr/bin/env bash

HD=$PWD

function abort_everything {
  echo "Aborting..."
  cd ${HD}
  exit 1
}

function fail_if_fucked {
  if [ ! $1 -eq 0 ]
  then
    echo "Something failed"
    abort_everything
  fi
}

trap 'abort_everything' SIGINT

if [ ! -d bundle ]
then mkdir bundle
fi
cd bundle

cp ../target/*-jar-with-dependencies.jar musicbot-desktop.jar
fail_if_fucked $?

python3 -m venv ./venv/
fail_if_fucked $?
python3 -m pip install requests circleclient
fail_if_fucked $?

mkdir plugins
cd plugins

echo "Bundling YouTube-plugin..."
python3 ${HD}/load_artifact.py -p JMusicBot-YouTube -t 6965c3e03f484db176a269945a35140bfcec8f9e -a "root/app/jar/musicbot-youtube.jar"
fail_if_fucked $?
echo "Bundling Spotify-plugin..."
python3 ${HD}/load_artifact.py -p JMusicBot-Spotify -t 1f31a397872d6ff9c89a894228a3ae274c9841f6 -a "root/app/jar/musicbot-spotify.jar"
fail_if_fucked $?

cd ..

echo "Should zip now..."

# TODO: zip and move
cd ${HD}
