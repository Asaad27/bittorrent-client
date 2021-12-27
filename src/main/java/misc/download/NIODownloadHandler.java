package misc.download;

import misc.messages.Message;
import misc.messages.PeerMessage;
import misc.peers.ClientState;
import misc.peers.PeerInfo;
import misc.peers.PeerState;
import misc.torrent.*;
import misc.utils.DEBUG;
import misc.utils.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class NIODownloadHandler{

    public static ClientState clientState;
    public static TorrentMetaData torrentMetaData;
    public static TorrentState torrentState;
    public static RandomAccessFile file = null;
    public static List<PeerInfo> peerInfoList;         //TODO : NOTIFY WHEN PEERLIST CHANGES
    private Observer observer;


    public NIODownloadHandler(TorrentMetaData torrentMetaData, ClientState clientState, TorrentState torrentState, List<PeerInfo> peerInfoList, Observer observer) {
        NIODownloadHandler.torrentMetaData = torrentMetaData;
        NIODownloadHandler.clientState = clientState;
        NIODownloadHandler.torrentState = torrentState;
        NIODownloadHandler.peerInfoList = peerInfoList;
        this.observer = observer;

        clientState.choked = false;
        clientState.bitfield.initLeecher();
        try {
            file = new RandomAccessFile("src\\main\\" + torrentMetaData.getName(), "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * fonction qui gere la lecture des messages recus
     */
    public void messageHandler(Message receivedMessage, PeerState peerState) {
        //PeerState peerState = peerList.get(peerIndex).getPeerState();

        if (receivedMessage.ID == PeerMessage.MsgType.KEEPALIVE) {
            //System.out.println("KEEP ALIVE RECEIVED");
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (receivedMessage.ID == PeerMessage.MsgType.CHOKE) {
            peerState.choked = false;
            clientState.choked = true;
        } else if (receivedMessage.ID == PeerMessage.MsgType.UNCHOKE) {
            peerState.choked = false;
            peerState.weAreChokedByPeer = false;
            //peerState.writeMessageQ.add(new Message(PeerMessage.MsgType.UNCHOKE));

        } else if (receivedMessage.ID == PeerMessage.MsgType.INTERESTED) {
            peerState.interested = true;
            peerState.writeMessageQ.addFirst(new Message(PeerMessage.MsgType.UNCHOKE));
        } else if (receivedMessage.ID == PeerMessage.MsgType.NOTINTERESTED) {
            peerState.interested = false;
            //endConnexion();
        } else if (receivedMessage.ID == PeerMessage.MsgType.HAVE) {
            if (!peerState.hasPiece(receivedMessage.getIndex())){
                observer.notifyAllObservers(receivedMessage.getIndex());
            }
            peerState.setPiece(receivedMessage.getIndex());
            if (!clientState.hasPiece(receivedMessage.getIndex())) {
                Message interested = new Message(PeerMessage.MsgType.INTERESTED);
                peerState.writeMessageQ.addFirst(interested);
                peerState.writeMessageQ.addFirst(new Message(PeerMessage.MsgType.UNCHOKE));
            }

        } else if (receivedMessage.ID == PeerMessage.MsgType.BITFIELD) {
            peerState.bitfield.value = receivedMessage.getPayload();
            peerState.sentBitfield = true;
            observer.notifyAllObservers(Events.PEER_CONNECTED, peerState);
        } else if (receivedMessage.ID == PeerMessage.MsgType.REQUEST) {
            //we find the requested piece in the file

            int size = receivedMessage.getLength();
            byte[] ans = new byte[size];
            try {
                file.seek((long) (receivedMessage.getIndex()) * torrentState.getPieceSize() + receivedMessage.getBegin());
                file.read(ans);
                Message piece = new Message(PeerMessage.MsgType.PIECE, receivedMessage.getIndex(), receivedMessage.getBegin(), ans);
                peerState.writeMessageQ.addFirst(piece);
                //sendMessage(piece);
                peerState.setPiece(receivedMessage.getIndex());
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (torrentState.getDownloadedSize() == torrentMetaData.getLength())
                endConnexion();


        } else if (receivedMessage.ID == PeerMessage.MsgType.PIECE) {
            // System.out.println("****piece received**** index : " + receivedMessage.index + " begin : " + receivedMessage.getBegin() + " size : " + receivedMessage.getPayload().length);

            if (!clientState.hasPiece(receivedMessage.getIndex())) {
                torrentState.setDownloadedSize(torrentState.getDownloadedSize() + receivedMessage.getPayload().length);
                try {
                    file.seek(((long) receivedMessage.getIndex() * torrentState.getPieceSize() + receivedMessage.getBegin()));
                    file.write(receivedMessage.getPayload());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                torrentState.pieceDownloadedBlocks.putIfAbsent(receivedMessage.getIndex(), new AtomicInteger(0));
                int downloadedBlocks = torrentState.pieceDownloadedBlocks.get(receivedMessage.getIndex()).incrementAndGet();
                DecimalFormat df = new DecimalFormat();
                df.setMaximumFractionDigits(2);
                boolean downloaded = false;
                if (receivedMessage.getIndex() == torrentState.getNumberOfPieces() - 1) {
                    if (downloadedBlocks == torrentState.getNumOfLastPieceBlocks()) {
                        downloaded = true;
                    }
                } else {
                    if (downloadedBlocks == torrentState.getNumOfBlocks()) {
                        downloaded = true;
                    }
                }
                if (downloaded){
                    System.err.println("PIECE N° : " + receivedMessage.getIndex() + " DOWNLOADED" + "\t" + df.format(torrentState.getDownloadedSize() * 1.0 / torrentMetaData.getLength() * 100) + "% downloaded");
                    clientState.setPiece(receivedMessage.getIndex());
                    torrentState.getStatus().set(receivedMessage.getIndex(), PieceStatus.Downloaded);
                    Message have = new Message(PeerMessage.MsgType.HAVE, receivedMessage.getIndex());
                    //we send have to all peers
                    for (PeerInfo peerInfo : peerInfoList) {
                            if (!peerInfo.getPeerState().hasPiece(receivedMessage.getIndex()) && peerInfo.getPeerState().sentBitfield)
                                peerInfo.getPeerState().writeMessageQ.addFirst(have);
                    }
                }

            } else
                DEBUG.log("we already have the piece***************");
            if (torrentState.getDownloadedSize() == torrentMetaData.getLength())
                endConnexion();
        }
    }

    public void endConnexion() {
        //TODO : endconnexion
        System.out.println("connexion ended");
        for (PeerInfo peerInfo : peerInfoList){
            System.out.println(peerInfo);
            System.out.println("number of requests we sent to them : " + peerInfo.getPeerState().numberOfRequests);
            System.out.println("number of blocks we received from them : " + peerInfo.getPeerState().requestReceivedFromPeer);
        }
        System.out.println("peer  : ");

        if(verifyDownloadedFile()) {
            System.out.println("FILE CHECK : SUCCESS");
        }
        else{
            System.out.println("FILE CHECK : FAILED");
        }
        //TODO : if peer finished downloading and we have all pieces he has, choke
    }

    public static boolean verifyPiece(int index){
        int numPieces = torrentState.getNumberOfPieces();
        int lastPieceSize = torrentState.getLastPieceSize();
        int pieceSize = torrentState.getPieceSize();
        byte[] piece;
        if (index == numPieces-1)
            piece = new byte[lastPieceSize];
        else
            piece = new byte[pieceSize];
        try {
            file.seek((long) index * pieceSize);
            file.read(piece);

            MessageDigest digest=MessageDigest.getInstance("SHA-1");
            digest.update(piece);
            byte[] sha = digest.digest();
            String originalHash = torrentMetaData.getPiecesList().get(index);
            String downloadedHash = Utils.bytesToHex(sha);
            if (!downloadedHash.equals(originalHash))
            {
                System.err.println("piece id : " + index + "\noriginal hash : " + originalHash + "\ndownloaded hash : " + downloadedHash);
                return false;
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return true;

    }
    public static boolean verifyDownloadedFile(){

        int numPieces = torrentState.getNumberOfPieces();
        int lastPieceSize = torrentState.getLastPieceSize();
        int pieceSize = torrentState.getPieceSize();
        for (int i = 0; i < numPieces; i++)
        {
           if(!verifyPiece(i)) {
               //TODO : redownload
               return false;
           }
        }

        return true;
    }

    public boolean sendFullPieceRequest(int pieceIndex, PeerState peerState) {
        //Iterator<Integer> it = peerState.piecesToRequest.iterator();

        int blockSize = torrentState.getBlockSize();
        int remainingBlockSize = torrentState.getRemainingBlockSize();
        int pieceSize = torrentState.getPieceSize();
        int numOfBlocks = torrentState.getNumOfBlocks();
        int lastBlockSize = torrentState.getLastBlockSize();

        if (torrentState.getStatus().get(pieceIndex) == PieceStatus.Downloaded) {
            DEBUG.log("on a deja cette piece");
            return false;
        }
        if (torrentState.getStatus().get(pieceIndex) == PieceStatus.Requested){
            //DEBUG.log("on a deja request cette piece");
            return false;
        }

        if (pieceIndex == torrentMetaData.getNumberOfPieces() - 1) {

            int lpoffset = 0;
            for (int j = 0; j < torrentState.getNumOfLastPieceBlocks() - 1; j++) {
                Message request = new Message(PeerMessage.MsgType.REQUEST, pieceIndex, lpoffset, blockSize);
                peerState.writeMessageQ.add(request);
                lpoffset += blockSize;
            }
            //we send the last block
            if (remainingBlockSize != 0) {
                Message request = new Message(PeerMessage.MsgType.REQUEST, pieceIndex, lpoffset, remainingBlockSize);
                peerState.writeMessageQ.add(request);
            }

        } else {
            int offset = 0, countBlocks = 0;
            while (offset < pieceSize) {
                Message request = new Message(PeerMessage.MsgType.REQUEST, pieceIndex, offset, blockSize);
                peerState.writeMessageQ.add(request);
                countBlocks++;
                offset += (countBlocks == numOfBlocks) ? lastBlockSize : blockSize;
            }
        }

        torrentState.getStatus().set(pieceIndex, PieceStatus.Requested);

        return true;

    }

    public static boolean sendBlockRequests(List<PeerInfo> valuablePeers, int pieceIndex){

        if (torrentState.getStatus().get(pieceIndex) == PieceStatus.Downloaded) {
            return false;
        }
        else if (torrentState.getStatus().get(pieceIndex) == PieceStatus.Requested){
            return false;
        }
        else if (torrentState.getStatus().get(pieceIndex) == PieceStatus.QUEUED){
            return false;
        }

        if (clientState.hasPiece(pieceIndex))
            return false;


        int numberOfPeers = valuablePeers.size();
        Random random = new Random();

        int numberOfBlocks = torrentState.getNumOfBlocks();

        if (pieceIndex == torrentState.getNumberOfPieces()-1){
            int lpoffset = 0;
            for (int j = 0; j < torrentState.getNumOfLastPieceBlocks() - 1; j++) {
                Message request = new Message(PeerMessage.MsgType.REQUEST, pieceIndex, lpoffset, torrentState.getBlockSize());
                int n = random.nextInt(numberOfPeers);
                valuablePeers.get(n).getPeerState().writeMessageQ.add(request);
                lpoffset += torrentState.getBlockSize();
            }
            //we send the last block
            if (torrentState.getRemainingBlockSize() != 0) {
                Message request = new Message(PeerMessage.MsgType.REQUEST, pieceIndex, lpoffset, torrentState.getRemainingBlockSize());
                int n = random.nextInt(numberOfPeers);
                valuablePeers.get(n).getPeerState().writeMessageQ.add(request);
            }

        }else{
            int offset = 0, countBlocks = 0;
            while (offset < torrentState.getPieceSize()){
                Message request = new Message(PeerMessage.MsgType.REQUEST, pieceIndex, offset, torrentState.getBlockSize());
                int n = random.nextInt(numberOfPeers);
                valuablePeers.get(n).getPeerState().writeMessageQ.add(request);
                countBlocks++;
                offset += (countBlocks == numberOfBlocks) ? torrentState.getLastBlockSize() : torrentState.getBlockSize();
            }
        }

        torrentState.getStatus().set(pieceIndex, PieceStatus.QUEUED);
        NIODownloadHandler.clientState.piecesToRequest.remove(pieceIndex);

        return true;
    }

}
