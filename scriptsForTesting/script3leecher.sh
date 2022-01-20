#!/bin/bash

rm ../test
rm -rf peer*
rm *.torrent
./filegen test 1 1000
./metainfo peer_1_to_1000/test -p 256 -a http://$(hostname).ensimag.fr:6969/announce
cp peer_1_to_1000/test.torrent .
mkdir peer_0_to_O
mkdir peer2_0_to_O
mkdir peer3_0_to_O

gnome-terminal -e "aria2c --listen-port 20042 -V  -d peer_0_to_O test.torrent"
gnome-terminal -e "aria2c --listen-port 20043 -V  -d peer2_0_to_O test.torrent"
gnome-terminal -e "aria2c --listen-port 20044 -V  -d peer3_0_to_O test.torrent"




