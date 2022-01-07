## file generator Linux and Windows
[scripts for generating files and torrents](/scripts) <br />

## opentracker for Windows

`docker build -t ot .` <br />
`docker run -p 6969:6969 ot` <br />
access opentracker in  http://localhost:6969/stats

## Bittorrent

execute MainBittorent with the torrent file as an argument <br />
edit generatePeerList() in TCPClient constructor with the ports
or use trackerList() to generate them from the tracker
