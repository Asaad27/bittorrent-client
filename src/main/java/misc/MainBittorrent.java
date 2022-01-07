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
        //DEBUG.switchIOToFile();
        /*TCPClient tcpClient = new TCPClient(args[0]);
        tcpClient.run();
*/
        String path = args[0];
        String path2 = "C:\\Users\\asaad_6stn3w\\go\\src\\metainfo\\iceberg.jpg.torrent";
        String path3 = "iceberg.jpg.torrent";
        try {
            TorrentFileHandler torrentFileHandler = new TorrentFileHandler(new FileInputStream(path2));
            TorrentMetaData torrentMetaData = torrentFileHandler.ParseTorrent();
            System.out.println(torrentMetaData);
            List<String> pieceList =  torrentMetaData.getPiecesList();
            for (String piece : pieceList)
                System.out.println(piece);
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

    //announce   createdBy  comment
    }
}
