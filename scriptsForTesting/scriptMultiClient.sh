#!/bin/bash

rm ../test
rm -rf peer*
rm *.torrent
./filegen test 1 1000
./filegen test 1 200
./filegen test 800 1000
./filegen test 100 900
./filegen test 400 669
./filegen test 200 700

./metainfo peer_1_to_1000/test -p 256 -a http://$(hostname).ensimag.fr:6969/announce
cp peer_1_to_1000/test.torrent .

gnome-terminal -e "aria2c --listen-port 20011 -V  -d peer_1_to_200 test.torrent"
gnome-terminal -e "aria2c --listen-port 20031 -V  -d peer_800_to_1000 test.torrent"
gnome-terminal -e "aria2c --listen-port 20041 -V  -d peer_100_to_900 test.torrent"
gnome-terminal -e "aria2c --listen-port 20051 -V  -d peer_400_669 test.torrent"
gnome-terminal -e "aria2c --listen-port 20021 -V  -d peer_200_700 test.torrent"


