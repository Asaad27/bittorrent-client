## file generator Linux and Windows
[file generator](/generateFiles) <br />
the files created contains 1000 pieces, with 256KB the size of each <br />
command to generate a file <br />
`./main test begin end` <br />
1<= begin <=end : the first piece<br />
begin <= end <= 1000 : the last piece <br />

then generate .torrent using <br />
`./metainfo test sizeOfPieceInKB` 
optional arguments :  announceUrl, createdBy, comment, must be in order <br />
`./metainfo test sizeOfPieceInKB announceUrl` <br />
`./metainfo test sizeOfPieceInKB announceUrl createdBy` <br />
`./metainfo test sizeOfPieceInKB announceUrl createdBy comment` <br />

## opentracker for Windows

`docker build -t ot .` <br />
`docker run -p 6969:6969 ot` <br />
access opentracker in  http://localhost:6969/stats

## Bittorrent

execute MainBittorent with the torrent file as an argument <br />
edit generatePeerList() in TCPClient constructor with the ports
or use trackerList() to generate them from the tracker
