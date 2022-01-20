#!/bin/bash

rm ../test
rm -rf peer*
rm *.torrent
./filegen test 1 1000
cp peer*/test .
./filegen test 1 200
./filegen test 800 1000
./filegen test 100 900
./filegen test 400 669
./filegen test 200 700
# shellcheck disable=SC2046
./metainfo test -p 256 -a http://$(hostname).ensimag.fr:6969/announce
cd peer_1_to_200/
gnome-terminal -e "aria2c --listen-port 2001 -V  -d . ../test.torrent"
cd ..

cd peer_800_to_1000
gnome-terminal -e "aria2c --listen-port 2003 -V  -d . ../test.torrent"
cd ..

cd peer_100_to_900
gnome-terminal -e "aria2c --listen-port 2004 -V  -d . ../test.torrent"
cd..

cd peer_400_to_669
gnome-terminal -e "aria2c --listen-port 2005 -V  -d . ../test.torrent"
cd..

cd peer_200_700
gnome-terminal -e "aria2c --listen-port 2002 -V  -d . ../test.torrent"


