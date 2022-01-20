#!/bin/bash

rm -rf peer_0_to_1000
./filegen test 1 1000
cp peer*/test .
./metainfo test -p 256 -a http://$(hostname).ensimag.fr:6969/announce
mkdir peer_0_to_1000
cd peer_0_to_1000/
gnome-terminal -e "aria2c --listen-port 2008 -V  -d . ../test.torrent"
