## file generator Linux and Windows

the files created contains 1000 pieces, with 256KB the size of each <br />
command to generate a file <br />
`./main test begin end` <br />
1<= begin <=end<br />
begin <= end <= 1000

## opentracker for Windows

`docker build -t ot .` <br />
`docker run -p 6969:6969 ot` <br />
access opentracker in  http://localhost:6969/stats

## Bittorrent

execute MainBittorent with the torrent file as an argument <br />
edit generatePeerList() in TCPClient constructor with the ports
or use trackerList() to generate them from the tracker
