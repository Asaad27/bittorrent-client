package misc.peers;

import misc.messages.Message;
import misc.messages.PeerMessage;
import misc.torrent.PieceStatus;
import misc.torrent.TorrentMetaData;
import misc.torrent.TorrentState;
import misc.utils.DEBUG;
import misc.utils.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class NIODownloadHandler {

    public ClientState clientState;
    public TorrentMetaData torrentMetaData;
    public TorrentState torrentState;
    public RandomAccessFile file = null;
    public List<PeerInfo> peerInfoList;         //TODO : NOTIFY WHEN PEERLIST CHANGES


    public NIODownloadHandler(TorrentMetaData torrentMetaData, ClientState clientState, TorrentState torrentState, List<PeerInfo> peerInfoList) {
        this.torrentMetaData = torrentMetaData;
        this.clientState = clientState;
        this.torrentState = torrentState;
        this.peerInfoList = peerInfoList;
        //peerState = new PeerState(torrentMetaData.getNumberOfPieces());

        clientState.choked = false;
        clientState.bitfield.initLeecher();
        try {
            file = new RandomAccessFile("src\\main\\" + torrentMetaData.getName(), "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Thread qui gere la lecture des messages recus
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
            System.out.println("CHOKE RECEIVED");
            peerState.choked = false;
            clientState.choked = true;
        } else if (receivedMessage.ID == PeerMessage.MsgType.UNCHOKE) {
            System.out.println("UCHOKE RECEIVED");
            peerState.choked = false;
            clientState.choked = false;
            //writeMessageQ.add(new Message(PeerMessage.MsgType.UNCHOKE));

        } else if (receivedMessage.ID == PeerMessage.MsgType.INTERESTED) {
            System.out.println("INTERESTED RECEIVED");
            peerState.interested = true;
        } else if (receivedMessage.ID == PeerMessage.MsgType.NOTINTERESTED) {
            System.out.println("NOTINTERESTED RECEIVED");
            peerState.interested = false;
            endConnexion();
        } else if (receivedMessage.ID == PeerMessage.MsgType.HAVE) {
            System.out.println("HAVE RECEIVED");
            peerState.setPiece(receivedMessage.getIndex());
            if (!clientState.hasPiece(receivedMessage.getIndex())) {
                Message interested = new Message(PeerMessage.MsgType.INTERESTED);
                peerState.writeMessageQ.add(interested);
            }

        } else if (receivedMessage.ID == PeerMessage.MsgType.BITFIELD) {
            DEBUG.log("the bitfield payload", Utils.bytesToHex(receivedMessage.getPayload()));
            peerState.bitfield.value = receivedMessage.getPayload();
        } else if (receivedMessage.ID == PeerMessage.MsgType.REQUEST) {
            System.out.println("request received");
            System.out.println("index : " + receivedMessage.getIndex() + " begin : " + receivedMessage.getBegin() + " size : " + receivedMessage.getLength());
            //we find the requested piece in the file

            int size = receivedMessage.getLength();
            byte[] ans = new byte[size];
            try {
                file.seek((long) (receivedMessage.getIndex()) * torrentState.getPieceSize() + receivedMessage.getBegin());
                file.read(ans);
                Message piece = new Message(PeerMessage.MsgType.PIECE, receivedMessage.getIndex(), receivedMessage.getBegin(), ans);
                peerState.writeMessageQ.add(piece);
                //sendMessage(piece);
                peerState.setPiece(receivedMessage.getIndex());
                torrentState.setDownloadedSize(torrentState.getDownloadedSize() + size);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (torrentState.getDownloadedSize() == torrentMetaData.getLength())
                endConnexion();


        } else if (receivedMessage.ID == PeerMessage.MsgType.PIECE) {
            // System.out.println("****piece received**** index : " + receivedMessage.index + " begin : " + receivedMessage.getBegin() + " size : " + receivedMessage.getPayload().length);
            torrentState.setDownloadedSize(torrentState.getDownloadedSize() + receivedMessage.getPayload().length);
            if (!clientState.hasPiece(receivedMessage.getIndex())) {
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
                if (receivedMessage.getIndex() == torrentState.numPieces - 1) {
                    if (downloadedBlocks == torrentState.getNumOfLastPieceBlocks()) {
                        downloaded = true;
                    }
                } else {
                    if (downloadedBlocks == torrentState.getNumOfBlocks()) {
                        downloaded = true;
                    }
                }
                if (downloaded){
                    System.out.println("PIECE DOWNLOADED : " + receivedMessage.getIndex() + "\t" + df.format(torrentState.getDownloadedSize() * 1.0 / torrentMetaData.getLength() * 100) + "% downloaded");
                    clientState.setPiece(receivedMessage.getIndex());
                    torrentState.getStatus().set(receivedMessage.getIndex(), PieceStatus.Downloaded);
                    Message have = new Message(PeerMessage.MsgType.HAVE, torrentState.numPieces - 1);
                    //we send have to all peers
                    for (PeerInfo peerInfo : peerInfoList) {
                        peerInfo.getPeerState().writeMessageQ.add(have);
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
    }

    //send bitfield and interested to all peers
    public void leechTorrent(List<PeerInfo> peerList) {

        for (int i = 0; i < peerList.size(); i++) {

            PeerState peerState = peerList.get(i).getPeerState();
            Message bitfield = new Message(PeerMessage.MsgType.BITFIELD, clientState.getBitfield());
            peerState.writeMessageQ.add(bitfield);
            Message unchoke = new Message(PeerMessage.MsgType.UNCHOKE);
            peerState.writeMessageQ.add(unchoke);
            Message interested = new Message(PeerMessage.MsgType.INTERESTED);
            peerState.writeMessageQ.add(interested);
        }
    }

    public boolean sendFullPieceRequest(int pieceIndex, PeerState peerState) {
        //Iterator<Integer> it = peerState.piecesToRequest.iterator();

        int blockSize = torrentState.blockSize;
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

            int lpoffset = 0, lpcountBlocks = 0;
            for (int j = 0; j < torrentState.getNumOfLastPieceBlocks() - 1; j++) {
                Message request = new Message(PeerMessage.MsgType.REQUEST, pieceIndex, lpoffset, blockSize);
                peerState.writeMessageQ.add(request);
                lpoffset += blockSize;
                lpcountBlocks++;
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
}
