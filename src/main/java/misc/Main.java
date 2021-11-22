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

    public static void leecher(String[] args) {

        //CAS LEECHER 100%

        // Port d'écoute Bittorent
        int PORT = 59407;
        String SERVER = "127.0.0.1";

        String PEERID = TrackerHandler.genPeerId();
        // On initialise le TorrentFileHandler à partir du fichier Torrent d'entrée
        PeerDownloadHandler peerDownloadHandler = null;

        try {

            TorrentFileHandler torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
            TorrentMetaData torrentMetaData = torrentHandler.ParseTorrent();

            System.out.println(torrentMetaData.toString());
            URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());

            //LocalFileHandler localFile = new LocalFileHandler(torrentMetaData.getName(), torrentMetaData.getNumberOfPieces(), torrentMetaData.getPiece_length(), torrentMetaData.getPieces());

            //TrackerHandler tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1InfoByte(), localFile, PORT);

            //System.out.println("looking for peers");
            //List<PeerInfo> peerLst = tracker.getPeerLst();
            //System.out.println("peerlist received");
            peerDownloadHandler = new PeerDownloadHandler(PORT, SERVER);

            peerDownloadHandler.initLeecher(torrentMetaData);

            peerDownloadHandler.doHandShake(Utils.hexStringToByteArray(torrentMetaData.getSHA1Info()), Utils.hexStringToByteArray(PEERID));


            Message bitfield = new Message(PeerMessage.MsgType.BITFIELD, PeerDownloadHandler.clientBitfield);
            peerDownloadHandler.sendMessage(bitfield);

            Message interested = new Message(PeerMessage.MsgType.INTERESTED);
            peerDownloadHandler.sendMessage(interested);

            Message unchoke = new Message(PeerMessage.MsgType.UNCHOKE);
            peerDownloadHandler.sendMessage(unchoke);

            /* divide the current piece into blocks */
            // if it's the last piece compute it's size
            int blockSize = CHUNK_SIZE;
            int lastBlockSize = (torrentMetaData.getPieceLength() % blockSize != 0) ? torrentMetaData.getPieceLength() % blockSize : blockSize;
            int numOfBlocks = (torrentMetaData.getPieceLength() + blockSize - 1) / blockSize;
            int pieceSize = torrentMetaData.getPieceLength();
            int lastPieceSize = (torrentMetaData.getLength() % pieceSize == 0) ? pieceSize : (int) (torrentMetaData.getLength() % pieceSize);
            //TODO : traiter le cas ou la taille de la piece n'est pas divisible par la taille du block
            //TODO : gerer le bitfield

            for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {


                if (i == torrentMetaData.getNumberOfPieces() - 1) {

                    int lpoffset = 0, lpcountBlocks = 0;
                    //nombre de block de 16KIB
                    int numOfLastPieceBlocks = (lastPieceSize + blockSize -1) / blockSize;
                    //le reste
                    int remainingBlockSize = lastPieceSize % blockSize;
                    for (int j = 0; j < numOfLastPieceBlocks-1; j++) {
                        Message request = new Message(PeerMessage.MsgType.REQUEST, i, lpoffset, blockSize);
                        peerDownloadHandler.sendMessage(request);
                        lpoffset += blockSize;
                        lpcountBlocks++;

                    }
                    //we send the last block
                    if (remainingBlockSize != 0) {
                        Message request = new Message(PeerMessage.MsgType.REQUEST, i, lpoffset, remainingBlockSize);
                        peerDownloadHandler.sendMessage(request);
                        System.out.println("request number : " + i);
                    }

                } else {
                    int offset = 0, countBlocks = 0;
                    while (offset < pieceSize) {
                        while (PeerDownloadHandler.requestedMsgs.get() >= 5) {
                            //System.out.println("waiting to free requests");
                        }

                        PeerDownloadHandler.requestedMsgs.incrementAndGet();

                        Message request = new Message(PeerMessage.MsgType.REQUEST, i, offset, blockSize);
                        peerDownloadHandler.sendMessage(request);
                        countBlocks++;
                        offset += (countBlocks == numOfBlocks) ? lastBlockSize : blockSize;
                    }
                }

            }
            //var notInter = new Message(PeerMessage.MsgType.NOTINTERESTED);
            //peerConnectionHandler.sendMessage(notInter);

        } catch (Exception e) {

            e.printStackTrace();

        }
    }

    public static void main(String[] args) {
        leecher(args);

        /*int PORT = 59407;
        String SERVER = "127.0.0.1";
        String PEERID = "2D7142343334312D395356716A36505139722D48";
        PeerDownloadHandler peerDownloadHandler = null;


        try {
            TorrentFileHandler torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
            TorrentMetaData torrentMetaData = torrentHandler.ParseTorrent();

            System.out.println(torrentMetaData);
            URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());
            //LocalFileHandler localFile = new LocalFileHandler(torrentMetaData.getName(), torrentMetaData.getNumberOfPieces(), torrentMetaData.getPieceLength(), torrentMetaData.getPieces());
            //TrackerHandler tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1InfoByte(), PORT);

           *//* System.out.println("looking for peers");
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
