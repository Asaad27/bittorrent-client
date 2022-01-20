#!/bin/bash

rm -rf peer*
./filegen test 1 1000
cp peer*/test .
./metainfo test -p 256 -a http://$(hostname).ensimag.fr:6969/announce
./filegen test 1 200
mkdir peer_0_to_1000
mkdir peer2_0_to_1000
cd peer_0_to_1000/
gnome-terminal -e "aria2c --listen-port 2001 -V  -d . ../test.torrent"
cd ..
cd peer2_0_to_1000/
gnome-terminal -e "aria2c --listen-port 2002 -V  -d . ../test.torrent"
cd ..
cd peer_1_to_200
gnome-terminal -e "aria2c --listen-port 2003 -V  -d . ../test.torrent"
