package misc;

import misc.peers.PeerDownloadHandler;
import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;


public class Main {

    public static void main(String[] args) {

        //qbittorrent  THE FASTEST CLIENT IN THE WEST
        //int PORT = 59407;
        //vuze
        int PORT = 12314;
        String SERVER = "127.0.0.1";
        String filePath = "";  //path of the file to seed
        PeerDownloadHandler peerDownloadHandler = null;
        TorrentFileHandler torrentHandler = null;
        TorrentMetaData torrentMetaData = null;
        try {
            torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
            torrentMetaData = torrentHandler.ParseTorrent();
            peerDownloadHandler = new PeerDownloadHandler(PORT, SERVER, torrentMetaData);
            //peerDownloadHandler.downloadTorrent();
            System.out.println(torrentMetaData);
            peerDownloadHandler.seedTorrent(filePath);

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


    }


}
