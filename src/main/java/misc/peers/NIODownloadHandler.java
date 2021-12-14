package misc.peers;

import misc.messages.Message;
import misc.messages.PeerMessage;
import misc.torrent.TorrentMetaData;
import misc.torrent.TorrentState;
import misc.utils.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class NIODownloadHandler {

    public PeerState peerState;
    public ClientState clientState;
    public TorrentMetaData torrentMetaData;
    public TorrentState torrentState;
    public RandomAccessFile file = null;


    public NIODownloadHandler(TorrentMetaData torrentMetaData) {
        this.torrentMetaData = torrentMetaData;
        torrentState = new TorrentState(torrentMetaData);
        clientState = new ClientState(torrentMetaData.getNumberOfPieces());
        peerState = new PeerState(torrentMetaData.getNumberOfPieces());

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
    public void messageHandler(Message receivedMessage, Queue<Message> writeMessageQ) {
        if (receivedMessage.ID == PeerMessage.MsgType.KEEPALIVE) {
            System.out.println("KEEP ALIVE RECEIVED");
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (receivedMessage.ID == PeerMessage.MsgType.CHOKE) {
            System.out.println("CHOKE RECEIVED");
            peerState.choked = false;
            clientState.choked= true;
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
                writeMessageQ.add(interested);
            }

        } else if (receivedMessage.ID == PeerMessage.MsgType.BITFIELD) {
            DEBUG.log("the bitfield payload", Utils.bytesToHex(receivedMessage.getPayload()));
            peerState.bitfield.value = receivedMessage.getPayload();
        } else if (receivedMessage.ID == PeerMessage.MsgType.REQUEST){
            System.out.println("request received");
            System.out.println("index : " + receivedMessage.getIndex() + " begin : " + receivedMessage.getBegin() + " size : " + receivedMessage.getLength());
            //we find the requested piece in the file

            int size = receivedMessage.getLength();
            byte[] ans = new byte[size];
            try {
                file.seek((long) (receivedMessage.getIndex()) * torrentState.getPieceSize() + receivedMessage.getBegin());
                file.read(ans);
                Message piece = new Message(PeerMessage.MsgType.PIECE, receivedMessage.getIndex(), receivedMessage.getBegin(), ans);
                writeMessageQ.add(piece);
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
                if (receivedMessage.getIndex() == torrentState.numPieces-1)
                {
                    if (downloadedBlocks == torrentState.getNumOfLastPieceBlocks())
                    {
                        System.out.println("PIECE DOWNLOADED : " + receivedMessage.getIndex() + "\t" + df.format(torrentState.getDownloadedSize()*1.0/torrentMetaData.getLength()*100) + "% downloaded" );
                        clientState.setPiece(receivedMessage.getIndex());
                        Message have = new Message(PeerMessage.MsgType.HAVE, torrentState.numPieces-1);
                        writeMessageQ.add(have);
                    }
                }
                else
                {
                    if (downloadedBlocks == torrentState.getNumOfBlocks())
                    {
                        System.out.println("PIECE DOWNLOADED : " + receivedMessage.getIndex() + "\t" + df.format(torrentState.getDownloadedSize()*1.0/torrentMetaData.getLength()*100) + "% downloaded");
                        clientState.setPiece(receivedMessage.getIndex());
                        Message have = new Message(PeerMessage.MsgType.HAVE, receivedMessage.getIndex());
                        writeMessageQ.add(have);
                    }
                }
            }
            if (torrentState.getDownloadedSize() == torrentMetaData.getLength())
                endConnexion();
        }
    }

    public void endConnexion(){
        //TODO : endconnexion
        System.out.println("connexion ended");
    }


    public void leechTorrent(Queue<Message> writeMessageQ) {
        //TODO ;

        Message bitfield = new Message(PeerMessage.MsgType.BITFIELD, clientState.getBitfield());
        writeMessageQ.add(bitfield);

        Message interested = new Message(PeerMessage.MsgType.INTERESTED);
        writeMessageQ.add(interested);

    }
}
