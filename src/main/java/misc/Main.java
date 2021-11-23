package misc;

import misc.peers.PeerDownloadHandler;
import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;


public class Main {

    public enum PORTCLIENT {
        VUZE(12314),
        QBITTORRENT(59407),
        DELUGE(53244),
        UTORRENT(33580)
        ;

        public final int port;
        PORTCLIENT(int port) {
            this.port = port;
        }

    }

    public static void main(String[] args) {

        //qbittorrent  THE FASTEST CLIENT IN THE WEST
        //int PORT = 59407;
        //vuze
        //int PORT = 12314;
        int PORT = PORTCLIENT.UTORRENT.port;
        String SERVER = "127.0.0.1";
        String filePath = "C:\\Users\\asaad_6stn3w\\IdeaProjects\\equipe5new\\src\\main\\2. SOLID Principles of OOP.mp4";  //path of the file to seed
        PeerDownloadHandler peerDownloadHandler = null;
        TorrentFileHandler torrentHandler = null;
        TorrentMetaData torrentMetaData = null;
        try {
            torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
            torrentMetaData = torrentHandler.ParseTorrent();
            System.out.println(torrentMetaData);
            peerDownloadHandler = new PeerDownloadHandler(PORT, SERVER, torrentMetaData);
            //peerDownloadHandler.downloadTorrent();

            //peerDownloadHandler.seedTorrent(filePath);


        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


    }


}
