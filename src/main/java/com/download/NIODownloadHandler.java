package com.download;

import com.messages.Message;
import com.messages.PeerMessage;
import com.peers.ClientState;
import com.peers.PeerInfo;
import com.peers.PeerState;
import com.torrent.*;
import com.utils.DEBUG;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import static com.download.TCPClient.torrentMetaData;
import static com.messages.PeerMessage.MsgType.CANCEL;
import static com.messages.PeerMessage.MsgType.UNINTERESTED;

/**
 * Class qui gére la machine à états et les decisions à prendre concernant le télechargement
 */
public class NIODownloadHandler {

    public static int BLOCKS_PER_PEER = 60;  //safe mode : 6, fast mode : 60
    private final ClientState clientState;
    private final TorrentState torrentState;
    private final Set<PeerInfo> peerInfoList;
    private final Observer observer;
    private final DecimalFormat df;


    public NIODownloadHandler(ClientState clientState, TorrentState torrentState, Set<PeerInfo> peerInfoList, Observer observer) {
        this.clientState = clientState;
        this.torrentState = torrentState;
        this.peerInfoList = peerInfoList;
        this.observer = observer;
        df = new DecimalFormat();
        df.setMaximumFractionDigits(2);

    }

    /**
     * send all blocks of piece to a peer, used in Endgame
     *
     * @param pieceIndex   index of piece
     * @param peerState    peer to send pieces to
     * @param torrentState state of the torrent
     */
    public static void sendFullPieceRequest(int pieceIndex, PeerState peerState, TorrentState torrentState) {

        Piece piece = torrentState.pieces.get(pieceIndex);
        int blockSize = piece.getBlockSize();
        int numOfBlocks = piece.getNumberOfBlocks();
        int lastBlockSize = piece.getLastBlockSize();

        if (piece.getStatus() == PieceStatus.Downloaded) {
            DEBUG.logf("on a deja cette piece");
            return;
        }


        int offset = 0;
        for (int i = 0; i < numOfBlocks - 1; i++) {
            Message request = new Message(PeerMessage.MsgType.REQUEST, pieceIndex, offset, blockSize);
            peerState.writeMessageQ.add(request);
            piece.setBlocks(i, BlockStatus.QUEUED);
            offset += blockSize;
        }

        //remaining block
        if (lastBlockSize != 0) {
            Message request = new Message(PeerMessage.MsgType.REQUEST, pieceIndex, offset, lastBlockSize);
            peerState.writeMessageQ.add(request);
            piece.setBlocks(piece.getNumberOfBlocks() - 1, BlockStatus.QUEUED);
        }


        //torrentState.getStatus().set(pieceIndex, PieceStatus.Requested);

    }

    public void sentStateMachine(Message message, PeerState peerState) {
        switch (message.getID()) {
            case UNCHOKE:
                peerState.choked = false;
                ClientState.isSeeder = true;
                break;
            case CHOKE:
                peerState.choked = true;
                break;
            case INTERESTED:
                peerState.weAreInterested = true;
                break;
            case UNINTERESTED:
                peerState.weAreInterested = false;
                break;
            case PIECE:
                peerState.numberOfBlocksReceived++;
                peerState.queuedRequestsFromPeer.decrementAndGet();
                Piece piece = torrentState.pieces.get(message.getIndex());
                int blockId = message.getBegin() / torrentState.BLOCK_SIZE;
                piece.setBlocks(blockId, BlockStatus.Requested);
                long uploadedSize = torrentState.getUploadedSize();
                if (blockId == piece.getNumberOfBlocks() - 1) {
                    torrentState.setUploadedSize(uploadedSize + piece.getLastBlockSize());
                } else {
                    torrentState.setUploadedSize(uploadedSize + piece.getBlockSize());
                }
                break;
            case REQUEST:
                peerState.queuedRequestsFromClient.incrementAndGet();
                peerState.numberOfRequests++;
                torrentState.pieces.get(message.getIndex()).setPieceStatus(PieceStatus.Requested);
                torrentState.pieces.get(message.getIndex()).setBlocks(message.getBegin() / torrentState.BLOCK_SIZE, BlockStatus.Requested);
                break;
            case BITFIELD:
                peerState.receivedBitfield = true;
                break;
            default:
                break;
        }
    }

    /**
     * state machine for received messages
     *
     * @param receivedMessage received message from peer
     * @param peerState       peer
     */
    public void stateMachine(Message receivedMessage, PeerState peerState) {

        peerState.updateTime();

        if (receivedMessage.ID == PeerMessage.MsgType.KEEPALIVE) {
            DEBUG.logf("KEEP ALIVE RECEIVED");
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (receivedMessage.ID == PeerMessage.MsgType.CHOKE) {
            peerState.weAreChokedByPeer = true;

        } else if (receivedMessage.ID == PeerMessage.MsgType.UNCHOKE) {
            peerState.weAreChokedByPeer = false;

        } else if (receivedMessage.ID == PeerMessage.MsgType.INTERESTED) {
            peerState.interested = true;
            peerState.writeMessageQ.addFirst(new Message(PeerMessage.MsgType.UNCHOKE));

        } else if (receivedMessage.ID == UNINTERESTED) {
            peerState.interested = false;

        } else if (receivedMessage.ID == PeerMessage.MsgType.HAVE) {
            if (!peerState.hasPiece(receivedMessage.getIndex())) {
                observer.notifyAllObserves(receivedMessage.getIndex());
            }
            peerState.setPiece(receivedMessage.getIndex());
            if (!clientState.hasPiece(receivedMessage.getIndex())) {
                Message interested = new Message(PeerMessage.MsgType.INTERESTED);
                peerState.writeMessageQ.addFirst(interested);
                peerState.writeMessageQ.addFirst(new Message(PeerMessage.MsgType.UNCHOKE));
            }

        } else if (receivedMessage.ID == PeerMessage.MsgType.BITFIELD) {
            onBitfieldReceived(peerState, receivedMessage.getPayload());

        } else if (receivedMessage.ID == PeerMessage.MsgType.REQUEST) {
            peerState.numberOfRequestsReceived++;
            peerState.queuedRequestsFromPeer.incrementAndGet();
            if (clientState.hasPiece(receivedMessage.getIndex())) {
                byte[] ans = torrentState.localFileHandler.readPieceBlock(receivedMessage.getLength(), receivedMessage.getIndex(), receivedMessage.getBegin());
                Message piece = new Message(PeerMessage.MsgType.PIECE, receivedMessage.getIndex(), receivedMessage.getBegin(), ans);
                peerState.writeMessageQ.addFirst(piece);
                peerState.setPiece(receivedMessage.getIndex());
            }

            if (torrentState.getDownloadedSize() == torrentMetaData.getLength())
                onDownloadCompleted();

        } else if (receivedMessage.ID == PeerMessage.MsgType.PIECE) {
            onBlockReceived(receivedMessage, peerState);
            peerState.queuedRequestsFromClient.decrementAndGet();
            peerState.numberOfBlocksSent++;
            if (torrentState.getDownloadedSize() == torrentMetaData.getLength())
                onDownloadCompleted();

        }
    }

    /**
     * Deal with the event of receiving a block from a peer
     *
     * @param receivedMessage received message from peer
     * @param peerState       state of the peer
     */
    public void onBlockReceived(Message receivedMessage, PeerState peerState) {
        int index = receivedMessage.getIndex();
        int begin = receivedMessage.getBegin();
        byte[] payload = receivedMessage.getPayload();

        if (TCPClient.torrentContext.getStrategy().getName().equals("ENDGAME")) {
            sendCancels(peerInfoList, index, begin, payload.length, peerState);
        }

        if (!clientState.hasPiece(receivedMessage.getIndex())) {

            boolean result = torrentState.localFileHandler.writePieceBlock(index, begin, payload);
            if (!result) {
                System.err.println("error writing downloaded block");
            }

            boolean pieceCompleted = onBlockDownloaded(receivedMessage);

            if (pieceCompleted) {
                System.out.print("PIECE N° : " + index + " DOWNLOADED" + "\t" + df.format(torrentState.getDownloadedSize() * 1.0 / torrentMetaData.getLength() * 100) + "% downloaded");
                System.out.println(" UL : " + df.format(DownloadRate.uploadRate) + "mb/s\t" + "DL : " + df.format(DownloadRate.downloadRate) + "mb/s");
                clientState.setPiece(index);
                clientState.piecesToRequest.remove(index);
                torrentState.pieces.get(index).setPieceStatus(PieceStatus.Downloaded);

                Message have = new Message(PeerMessage.MsgType.HAVE, index);
                //we send have to all peers
                for (PeerInfo peerInfo : peerInfoList) {
                    if (!peerInfo.getPeerState().hasPiece(index))
                        peerInfo.getPeerState().writeMessageQ.addFirst(have);
                }
            }

        }

    }

    /**
     * send cancels to all peers
     *
     * @param peerInfoList peers
     * @param index        index of received block piece
     * @param begin        offset of received block
     * @param length       length of received block
     * @param peerState    the peer we received the block from
     */
    private void sendCancels(Set<PeerInfo> peerInfoList, int index, int begin, int length, PeerState peerState) {
        for (PeerInfo peer : peerInfoList) {
            if (peer.getPeerState() == peerState)
                continue;
            Message cancel = new Message(CANCEL, index, begin, length);
            peer.getPeerState().writeMessageQ.addFirst(cancel);
        }
    }

    /**
     * handle blocks download, and checks for piece integrity
     *
     * @param receivedMessage piece message
     * @return true if the whole piece was downloaded
     */
    public boolean onBlockDownloaded(Message receivedMessage) {

        torrentState.setDownloadedSize(torrentState.getDownloadedSize() + receivedMessage.getPayload().length);
        torrentState.pieceDownloadedBlocks.putIfAbsent(receivedMessage.getIndex(), new AtomicInteger(0));
        int downloadedBlocks = torrentState.pieceDownloadedBlocks.get(receivedMessage.getIndex()).incrementAndGet();
        int pieceIndex = receivedMessage.getIndex();

        Piece piece = torrentState.pieces.get(pieceIndex);
        piece.setBlocks(receivedMessage.getBegin() / torrentState.BLOCK_SIZE, BlockStatus.Downloaded);

        return downloadedBlocks == piece.getNumberOfBlocks();
    }

    /**
     * Handles cases when download of a file is completed
     */
    public void onDownloadCompleted() {

        if (!ClientState.isDownloading)
            return;
        ClientState.isDownloading = false;


        System.out.println("processing file verification ");
        if (!torrentState.fileCheckedSuccess) {
            if (!torrentState.localFileHandler.verifyDownloadedFile()) {
                ClientState.isDownloading = true;
            }
        }

        if (!ClientState.isDownloading) {
            System.out.println("download ended");
        } else
            return;

        Stack<PeerInfo> peerNotNeeded = new Stack<>();
        for (PeerInfo peerInfo : peerInfoList) {
            System.out.println(peerInfo);
            System.out.println("number of requests we sent to them : " + peerInfo.getPeerState().numberOfRequests);
            System.out.println("number of blocks we received from them : " + peerInfo.getPeerState().numberOfBlocksSent);
            System.out.println("number of blocks we sent to them : " + peerInfo.getPeerState().numberOfRequestsReceived);

            boolean needToSeed = needToSeed(peerInfo.getPeerState());
            boolean needToDownload = needToDownload(peerInfo.getPeerState());

            if (!needToSeed && !peerInfo.getPeerState().choked) {
                Message chokeMessage = new Message(PeerMessage.MsgType.CHOKE);
                peerInfo.getPeerState().writeMessageQ.add(chokeMessage);
            }

            if (!needToDownload) {
                Message notInterestedMessage = new Message(UNINTERESTED);
                peerInfo.getPeerState().writeMessageQ.add(notInterestedMessage);

            }

            if (needToSeed && peerInfo.getPeerState().interested) {
                Message chokeMessage = new Message(PeerMessage.MsgType.UNCHOKE);
                peerInfo.getPeerState().writeMessageQ.add(chokeMessage);
                ClientState.isSeeder = true;
            }

            if (!needToDownload && !needToSeed)
                peerNotNeeded.add(peerInfo);
        }

        //TODO : let ?
      /*  for (PeerInfo useless: peerNotNeeded) {

            System.out.println("removing peer : " + peerNotNeeded);
            peerInfoList.remove(useless);
        }*/

    }

    /**
     * Handles event of receiving bitfield and notifies observes
     *
     * @param peerState peers who sent the bitfield
     * @param payload   bitfield payload
     */
    public void onBitfieldReceived(PeerState peerState, byte[] payload) {
        peerState.bitfield.value = payload;
        peerState.sentBitfield = true;
        observer.notifyAllObserves(Events.PEER_CONNECTED, peerState);

        if (needToDownload(peerState)) {
            Message interested = new Message(PeerMessage.MsgType.INTERESTED);
            peerState.welcomeQ.add(interested);
            ClientState.isDownloading = true;
        }
        if (needToSeed(peerState)) {
            if (peerState.interested) {
                Message unchoke = new Message(PeerMessage.MsgType.UNCHOKE);
                peerState.welcomeQ.add(unchoke);
            }
        }


        if (!needToSeed(peerState) && !needToDownload(peerState)) {
            if (peerState.writeMessageQ.isEmpty()) {
                peerInfoList.removeIf(peerInfo -> peerInfo.getPeerState() == peerState);
                System.out.println("removing useless peer");
            }
        }

    }

    /**
     * @param peerState peer
     * @return true if we want to download from the peer
     */
    public boolean needToDownload(PeerState peerState) {
        boolean download = false;
        for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
            if (peerState.hasPiece(i) && !clientState.hasPiece(i)) {
                download = true;
                break;
            }

        }

        return download;
    }

    /**
     * @param peerState peer
     * @return true we are interested in seeding to the peer
     */
    public boolean needToSeed(PeerState peerState) {
        boolean seed = false;
        for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
            if (!peerState.hasPiece(i) && clientState.hasPiece(i)) {
                seed = true;
                break;
            }
        }

        return seed;
    }

    /**
     * we distribute the blocks of a piece randomly over valuable peers
     *
     * @param valuablePeers list of peers who have the piece
     * @param pieceIndex    the piece we need
     * @return true if we could distribute the blocks
     **/

    public boolean sendBlockRequests(List<PeerInfo> valuablePeers, int pieceIndex) {

        Piece piece = torrentState.pieces.get(pieceIndex);
        if (piece.getStatus() == PieceStatus.Downloaded) {
            return false;
        } else if (piece.getStatus() == PieceStatus.Requested) {
            return false;
        } else if (piece.getStatus() == PieceStatus.QUEUED) {
            return false;
        }

        if (clientState.hasPiece(pieceIndex))
            return false;


        int numberOfPeers = valuablePeers.size();
        if (numberOfPeers == 0)
            return false;

        Random random = new Random();

        int numberOfBlocks = piece.getNumberOfBlocks();

        int n = random.nextInt(numberOfPeers);
        int offset = 0;
        for (int j = 0; j < piece.getNumberOfBlocks() - 1; j++) {
            Message request = new Message(PeerMessage.MsgType.REQUEST, pieceIndex, offset, piece.getBlockSize());
            piece.setBlocks(offset / torrentState.BLOCK_SIZE, BlockStatus.QUEUED);
            if (j % BLOCKS_PER_PEER == 0) {
                //n = random.nextInt(numberOfPeers);
                n = (n + 1) % numberOfPeers;
            }
            valuablePeers.get(n).getPeerState().writeMessageQ.add(request);
            offset += torrentState.BLOCK_SIZE;
        }
        //we send the last block
        if (piece.getLastBlockSize() != 0) {
            Message request = new Message(PeerMessage.MsgType.REQUEST, pieceIndex, offset, piece.getLastBlockSize());
            piece.setBlocks(offset / torrentState.BLOCK_SIZE, BlockStatus.QUEUED);
            n = random.nextInt(numberOfPeers);
            valuablePeers.get(n).getPeerState().writeMessageQ.add(request);
        }


        piece.setPieceStatus(PieceStatus.QUEUED);

        return true;
    }

    public ClientState getClientState() {
        return clientState;
    }
}
