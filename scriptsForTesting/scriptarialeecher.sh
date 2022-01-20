#!/bin/bash

rm -rf peer_0_to_1000
mkdir peer_0_to_1000
cd peer_0_to_1000/
gnome-terminal -e "aria2c --listen-port 2001 -V  -d . ../test.torrent"
