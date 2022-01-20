#!/bin/bash

rm ../test
rm -rf peer*
rm *.torrent
./filegen test 1 1000

./metainfo peer_1_to_1000/test -p 256 -a http://$(hostname).ensimag.fr:6969/announce
cp peer_1_to_1000/test.torrent .

gnome-terminal -e "aria2c --listen-port 20022 -V  -d peer_1_to_1000 test.torrent"




