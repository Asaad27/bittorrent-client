##Informations

reports folder contains profiler informations about the execution and call trees... <br />
javadoc folder contains java documentation;

## How to Use
either compile jar using : `mvn compile assembly:single` <br/>
or use ready made equipe5-Final-jar-with-dependencies.jar

**command to execute:**

`java -jar equi*.jar torrentFileName.torrent [-s or -f] `<br />
**-s** : used for secure downloading mode eq: limited number of requests to send and receive. <br />
**-f** : used for fast downloading mode <br />
detailed output is written to log.txt file in the jar directory. <br />

scripts are in [scriptsForTesting](/scriptsForTesting) <br />

**example of use:** <br />

inside [scriptsForTesting](/scriptsForTesting) folder, run  a script, for example `./scriptMultiClient.sh `
<br />, the script will create a file, generate it's torrent file from it and launch 5 aria2c instances, each with a specific range of pieces (can be used without opentracker to demonstrate strategies). The script will create
folders named peer_`from`_`to` with _from_ = first piece (1-index based) and _to_ = lastpiece **(max is 1000)**, with each folder is used by one Aria2c<br />
see the script file for more details and modifications. <br />
**IMPORTANT**: change announce URL to opentracker url in `-a http://127.0.0.1:6969/announce` inside the script sh file. <br />
the script creates both torrent file, and metainfo file, hence the script can be used for multiple machines without the need of copying any file, and also for testing in one machine. <br />

see [scripts for generating files and torrents](/scripts) Readme for more details about how i created the scripts and infos about their CLI arguments


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
