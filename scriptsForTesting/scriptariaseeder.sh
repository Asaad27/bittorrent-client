#!/bin/bash

rm ../test
rm -rf peer*
rm *.torrent
./filegen test 1 1000
cp peer*/test .
./metainfo test -p 256 -a http://127.0.0.1:6969/announce
cd peer_1_to_1000/
gnome-terminal -e "aria2c --listen-port 2001 -V  -d . ../test.torrent"



