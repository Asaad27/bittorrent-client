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
import java.io.RandomAccessFile;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static misc.peers.PeerDownloadHandler.CHUNK_SIZE;
import static misc.peers.PeerDownloadHandler.file;

public class Main {

    public static void main(String[] args) {

        int PORT = 59407;
        String SERVER = "127.0.0.1";
        PeerDownloadHandler peerDownloadHandler = null;
        TorrentFileHandler torrentHandler = null;
        TorrentMetaData torrentMetaData = null;
        try {
            torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
            torrentMetaData = torrentHandler.ParseTorrent();
            peerDownloadHandler = new PeerDownloadHandler(PORT, SERVER, torrentMetaData);
            peerDownloadHandler.downloadTorrent();

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }



/*

        try {
            TorrentFileHandler torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
            TorrentMetaData torrentMetaData = torrentHandler.ParseTorrent();

            System.out.println(torrentMetaData);
            URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());
            //LocalFileHandler localFile = new LocalFileHandler(torrentMetaData.getName(), torrentMetaData.getNumberOfPieces(), torrentMetaData.getPieceLength(), torrentMetaData.getPieces());
            //TrackerHandler tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1InfoByte(), PORT);

            /*System.out.println("looking for peers");
            List<PeerInfo> peerLst = tracker.getPeerLst();
            System.out.println("peerlist received");*//*

            peerDownloadHandler = new PeerDownloadHandler(PORT, SERVER);
            peerDownloadHandler.initSeeder(torrentMetaData, "C:\\Users\\asaad_6stn3w\\IdeaProjects\\equipe5new\\src\\main\\javaouma.txt");

            peerDownloadHandler.doHandShake(Utils.hexStringToByteArray(torrentMetaData.getSHA1Info()), Utils.hexStringToByteArray(PEERID));
            Message bitfield = new Message(PeerMessage.MsgType.BITFIELD, PeerDownloadHandler.clientBitfield);
            peerDownloadHandler.sendMessage(bitfield);



            Message interested = new Message(PeerMessage.MsgType.INTERESTED);
            peerDownloadHandler.sendMessage(interested);

            Message unchoke = new Message(PeerMessage.MsgType.UNCHOKE);
            peerDownloadHandler.sendMessage(unchoke);


            //TODO: generalize it to pieces and blocks
            RandomAccessFile randomAccessFile = new RandomAccessFile("C:\\Users\\asaad_6stn3w\\IdeaProjects\\equipe5new\\src\\main\\javaouma.txt", "r");
            byte[] block = new byte[10];
            randomAccessFile.read(block, 0, 10);
            Message piece = new Message(PeerMessage.MsgType.PIECE, 0, 0, block);
            peerDownloadHandler.sendMessage(piece);


        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
       */

    }


}
