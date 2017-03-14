#!/bin/bash

# Bash script to launch an EventManager running in its own temp directory and network ports

N=${1:-0}
shift

RMI_PORT=$(expr 1199 + $N)
LOCK_PORT=$(expr 58536 + $N)
DIR=tmp/run/$N
ROOT=../../..

mkdir -p $DIR
cp -r resources license.lic $DIR
cd $DIR
java -cp "$ROOT/dist/lib/*" -Deventmanager.lockport=$LOCK_PORT -Deventmanager.rmi.port=$RMI_PORT "$@" -jar $ROOT/dist/EventManager.jar

