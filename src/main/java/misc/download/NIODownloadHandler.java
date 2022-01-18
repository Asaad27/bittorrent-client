package misc.download;

import misc.download.strategies.EndGame;
import misc.messages.Message;
import misc.messages.PeerMessage;
import misc.peers.ClientState;
import misc.peers.PeerInfo;
import misc.peers.PeerState;
import misc.torrent.*;
import misc.utils.DEBUG;

import java.security.Timestamp;
import java.sql.Time;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import static misc.download.TCPClient.torrentMetaData;
import static misc.messages.PeerMessage.MsgType.*;


public class NIODownloadHandler {

    private final ClientState clientState;
    private final TorrentState torrentState;
    private final Set<PeerInfo> peerInfoList;
    private final Observer observer;


    public NIODownloadHandler(ClientState clientState, TorrentState torrentState, Set<PeerInfo> peerInfoList, Observer observer) {
        this.clientState = clientState;
        this.torrentState = torrentState;
        this.peerInfoList = peerInfoList;
        this.observer = observer;


    }

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
     * fonction qui gere la lecture des messages recus
     */
    public void stateMachine(Message receivedMessage, PeerState peerState) {

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
            if (peerState.choked) {
                peerState.writeMessageQ.addFirst(new Message(PeerMessage.MsgType.UNCHOKE));
            }
            //peerState.welcomeQ.add(new Message(PeerMessage.MsgType.UNCHOKE));
        } else if (receivedMessage.ID == UNINTERESTED) {
            peerState.interested = false;
            peerState.writeMessageQ.add(new Message(CHOKE));

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
            //we find the requested piece in the file
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


        }  //DEBUG.log("we already have the piece***************");


    }

    private void sendCancels(Set<PeerInfo> peerInfoList, int index, int begin, int length, PeerState peerState) {
        for (PeerInfo peer : peerInfoList) {
            if (peer.getPeerState() == peerState)
                continue;
            Message cancel = new Message(CANCEL, index, begin, length);
            peer.getPeerState().writeMessageQ.addFirst(cancel);
        }
    }

    public boolean onBlockDownloaded(Message receivedMessage) {
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);


        torrentState.setDownloadedSize(torrentState.getDownloadedSize() + receivedMessage.getPayload().length);
        torrentState.pieceDownloadedBlocks.putIfAbsent(receivedMessage.getIndex(), new AtomicInteger(0));
        int downloadedBlocks = torrentState.pieceDownloadedBlocks.get(receivedMessage.getIndex()).incrementAndGet();
        int pieceIndex = receivedMessage.getIndex();

        Piece piece = torrentState.pieces.get(pieceIndex);
        piece.setBlocks(receivedMessage.getBegin() / torrentState.BLOCK_SIZE, BlockStatus.Downloaded);



        //check if we complete piece download
        boolean downloaded = downloadedBlocks == piece.getNumberOfBlocks();

        if (downloaded) {
            System.out.println("PIECE N° : " + pieceIndex + " DOWNLOADED" + "\t" + df.format(torrentState.getDownloadedSize() * 1.0 / torrentMetaData.getLength() * 100) + "% downloaded");

            clientState.setPiece(pieceIndex);
            clientState.piecesToRequest.remove(pieceIndex);
            torrentState.pieces.get(pieceIndex).setPieceStatus(PieceStatus.Downloaded);

            Message have = new Message(PeerMessage.MsgType.HAVE, pieceIndex);
            //we send have to all peers
            for (PeerInfo peerInfo : peerInfoList) {
                if (!peerInfo.getPeerState().hasPiece(pieceIndex))
                    peerInfo.getPeerState().writeMessageQ.addFirst(have);
            }
        }

        return downloaded;
    }

    public void onDownloadCompleted() {

        if (!clientState.isDownloading)
            return;
        clientState.isDownloading = false;


        System.out.println("processing file verification ");
        if (!torrentState.fileCheckedSuccess) {
            if (!torrentState.localFileHandler.verifyDownloadedFile()) {
                clientState.isDownloading = true;
            }
        }

        if (!clientState.isDownloading) {
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

    public void onBitfieldReceived(PeerState peerState, byte[] payload) {
        peerState.bitfield.value = payload;
        peerState.sentBitfield = true;
        observer.notifyAllObserves(Events.PEER_CONNECTED, peerState);

        if (needToDownload(peerState)) {
            Message interested = new Message(PeerMessage.MsgType.INTERESTED);
            peerState.welcomeQ.add(interested);
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
            if (j % 10 == 0) {
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
