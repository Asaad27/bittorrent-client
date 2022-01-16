package misc;

import misc.download.PeerDownloadHandler;
import misc.torrent.TorrentFileController;
import misc.torrent.TorrentMetaData;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;


public class Main {

    public enum PORTCLIENT {
        VUZE(12314),
        QBITTORRENT(12316),
        DELUGE(57675),
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
        //int PORT = 57675;
        int PORT = 62875;
        String SERVER = "127.0.0.1";
        //String filePath = "C:\\Users\\asaad_6stn3w\\IdeaProjects\\latestequipe5\\src\\main\\CTos-8.2-2004-VB-64bit.7z";  //path of the file to seed
        PeerDownloadHandler peerDownloadHandler = null;
        TorrentFileController torrentHandler = null;
        TorrentMetaData torrentMetaData = null;
        try {
            torrentHandler = new TorrentFileController(new FileInputStream(args[0]));
            torrentMetaData = torrentHandler.ParseTorrent();

            peerDownloadHandler = new PeerDownloadHandler(PORT, SERVER, torrentMetaData);
            peerDownloadHandler.downloadTorrent();

            //peerDownloadHandler.seedTorrent(filePath);


        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


    }


}
