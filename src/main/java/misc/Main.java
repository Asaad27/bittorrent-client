package misc;

import misc.peers.Message;
import misc.peers.PeerDownloadHandler;
import misc.peers.PeerInfo;
import misc.peers.PeerMessage;
import misc.torrent.LocalFileHandler;
import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;
import misc.tracker.TrackerHandler;
import misc.utils.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;


public class Main {

    public static void main(String[] args) {

        //int PORT = 59407;
        String SERVER = "127.0.0.1";
        String filePath = "";
        PeerDownloadHandler peerDownloadHandler = null;
        TorrentFileHandler torrentHandler = null;
        TorrentMetaData torrentMetaData = null;
        try {
            torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
            torrentMetaData = torrentHandler.ParseTorrent();
            peerDownloadHandler = new PeerDownloadHandler(12314, SERVER, torrentMetaData);
            //peerDownloadHandler.downloadTorrent();
            System.out.println(torrentMetaData);
            peerDownloadHandler.seedTorrent(filePath);

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


    }


}
