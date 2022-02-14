#!/bin/bash


rm *.torrent
rm -rf peer*
./filegen test 1 1000
./metainfo peer_1_to_1000/test -p 256 -a http://127.0.0.1:6969/announce
cp peer_1_to_1000/test.torrent .
mkdir peer_0_to_0
cd peer_0_to_0
gnome-terminal -e "aria2c --listen-port 20023 -V  -d . test.torrent"




