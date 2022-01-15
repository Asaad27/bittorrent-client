package misc;

import misc.download.TCPClient;
import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;
import misc.utils.DEBUG;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class MainBittorrent {

    public static void main(String[] args) {
        DEBUG.switchIOToFile();
        TCPClient tcpClient = new TCPClient(args[0]);
        tcpClient.run();

    }
}
