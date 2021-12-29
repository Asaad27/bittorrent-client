## file generator Linux and Windows

the files created contains 1000 pieces, with 256KB the size of each
command to generate a file
`./main filename numberOfPiecesToWrite offset`
offset is 1-based
1<= numberOfPiecesToWrite <= 1000

## opentracker for Windows

`docker build -t ot .`
`docker run -p 6969:6969 ot`
access opentracker in  http://localhost:6969/stats

## Bittorrent

execute MainBittorent with the torrent file as an argument 
