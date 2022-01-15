package misc.download;

import misc.download.strategies.EndGame;
import misc.messages.Message;
import misc.messages.PeerMessage;
import misc.peers.ClientState;
import misc.peers.PeerInfo;
import misc.peers.PeerState;
import misc.torrent.*;
import misc.utils.DEBUG;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static misc.messages.PeerMessage.MsgType.UNINTERESTED;


public class NIODownloadHandler {

    private final ClientState clientState;
    private final TorrentMetaData torrentMetaData;
    private final TorrentState torrentState;
    private final Set<PeerInfo> peerInfoList;
    private final Observer observer;


    public NIODownloadHandler(TorrentMetaData torrentMetaData, ClientState clientState, TorrentState torrentState, Set<PeerInfo> peerInfoList, Observer observer) {
        this.torrentMetaData = torrentMetaData;
        this.clientState = clientState;
        this.torrentState = torrentState;
        this.peerInfoList = peerInfoList;
        this.observer = observer;



    }


    public void sentStateMachine(Message message, PeerState peerState){
        switch (message.getID()){
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
                break;
            case REQUEST:
                peerState.queuedRequestsFromClient.incrementAndGet();
                peerState.numberOfRequests++;
                getTorrentState().getStatus().set(message.getIndex(), PieceStatus.Requested);
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
            //System.out.println("KEEP ALIVE RECEIVED");
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
            if (peerState.choked){
                peerState.writeMessageQ.add(new Message(PeerMessage.MsgType.UNCHOKE));
            }
                //peerState.welcomeQ.add(new Message(PeerMessage.MsgType.UNCHOKE));
        } else if (receivedMessage.ID == UNINTERESTED) {
            peerState.interested = false;

        } else if (receivedMessage.ID == PeerMessage.MsgType.HAVE) {
            if (!peerState.hasPiece(receivedMessage.getIndex())) {
                observer.notifyAllObservees(receivedMessage.getIndex());
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

            if (!needToDownload(peerState)) {
                Message notInterested = new Message(UNINTERESTED);
                peerState.writeMessageQ.add(notInterested);
            }

        }
    }


    public void onBlockReceived(Message receivedMessage, PeerState peerState) {
        if (!clientState.hasPiece(receivedMessage.getIndex())) {
            int index = receivedMessage.getIndex();
            int begin = receivedMessage.getBegin();
            byte[] payload =  receivedMessage.getPayload();
            boolean result = torrentState.localFileHandler.writePieceBlock(index, begin, payload);
            if (!result) {
                System.err.println("error writing downloaded block");
            }
            if (TCPClient.torrentContext.getStrategy().getName().equals("ENDGAME")){
                EndGame.sendCancels(peerInfoList, index, begin, payload.length, peerState);
                EndGame.blockDownloaded(index, begin, torrentState);

            }

            boolean pieceCompleted = onBlockDownloaded(receivedMessage);
            if (pieceCompleted && TCPClient.torrentContext.getStrategy().getName().equals("ENDGAME")) {
                //EndGame.sendCancels(peerInfoList, receivedMessage.getIndex(), receivedMessage.getBegin(), receivedMessage.getLength(), peerState);
            }

        } else {
            DEBUG.log("we already have the piece***************");
        }

    }

    public boolean onBlockDownloaded(Message receivedMessage) {
        torrentState.setDownloadedSize(torrentState.getDownloadedSize() + receivedMessage.getPayload().length);
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
        if (downloaded) {
            System.err.println("PIECE N° : " + receivedMessage.getIndex() + " DOWNLOADED" + "\t" + df.format(torrentState.getDownloadedSize() * 1.0 / torrentMetaData.getLength() * 100) + "% downloaded");
            clientState.setPiece(receivedMessage.getIndex());
            clientState.piecesToRequest.remove(receivedMessage.getIndex());
            torrentState.getStatus().set(receivedMessage.getIndex(), PieceStatus.Downloaded);
            Message have = new Message(PeerMessage.MsgType.HAVE, receivedMessage.getIndex());
            //we send have to all peers
            for (PeerInfo peerInfo : peerInfoList) {
                if (!peerInfo.getPeerState().hasPiece(receivedMessage.getIndex()) && peerInfo.getPeerState().sentBitfield)
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

        System.out.println("download ended");
        for (PeerInfo peerInfo : peerInfoList) {
            if (!needToSeed(peerInfo.getPeerState()) && peerInfo.getPeerState().choked) {
                Message chokeMessage = new Message(PeerMessage.MsgType.CHOKE);
                peerInfo.getPeerState().writeMessageQ.add(chokeMessage);
            }

            if (!needToDownload(peerInfo.getPeerState())) {
                Message notInterestedMessage = new Message(UNINTERESTED);
                peerInfo.getPeerState().writeMessageQ.add(notInterestedMessage);
            }

            System.out.println(peerInfo);
            System.out.println("number of requests we sent to them : " + peerInfo.getPeerState().numberOfRequests);
            System.out.println("number of blocks we received from them : " + peerInfo.getPeerState().numberOfBlocksSent);
            System.out.println("number of blocks we sent to them : " + peerInfo.getPeerState().numberOfRequestsReceived);
        }

    }

    public void onBitfieldReceived(PeerState peerState, byte[] payload){
        peerState.bitfield.value = payload;
        peerState.sentBitfield = true;
        observer.notifyAllObservees(Events.PEER_CONNECTED, peerState);

        if (needToDownload(peerState)){
            Message interested = new Message(PeerMessage.MsgType.INTERESTED);
            peerState.welcomeQ.add(interested);
        }
        if (needToSeed(peerState)){
            if (peerState.interested){
                Message unchoke = new Message(PeerMessage.MsgType.UNCHOKE);
                peerState.welcomeQ.add(unchoke);
            }
        }


        if (!needToSeed(peerState) && !needToDownload(peerState)){
            peerInfoList.removeIf(peerInfo -> peerInfo.getPeerState() == peerState);

        }

    }

    public boolean needToDownload(PeerState peerState) {
        boolean download = false;
        for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
            if (peerState.hasPiece(i) && !clientState.hasPiece(i)){
                download = true;
                break;
            }

        }

        return download;
    }

    public boolean needToSeed(PeerState peerState) {
        boolean seed = false;
        for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
            if (!peerState.hasPiece(i) && clientState.hasPiece(i)){
                seed = true;
                break;
            }

        }

        return seed;
    }


    public static void sendFullPieceRequest(int pieceIndex, PeerState peerState, TorrentState torrentState, TorrentMetaData torrentMetaData) {

        int blockSize = torrentState.getBlockSize();
        int remainingBlockSize = torrentState.getRemainingBlockSize();
        int pieceSize = torrentState.getPieceSize();
        int numOfBlocks = torrentState.getNumOfBlocks();
        int lastBlockSize = torrentState.getLastBlockSize();

        if (torrentState.getStatus().get(pieceIndex) == PieceStatus.Downloaded) {
            DEBUG.log("on a deja cette piece");
            return;
        }
/*        if (torrentState.getStatus().get(pieceIndex) == PieceStatus.Requested) {
            //DEBUG.log("on a deja request cette piece");
            return false;
        }*/

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

        //torrentState.getStatus().set(pieceIndex, PieceStatus.Requested);

    }

    /** we distribute the blocks of a piece randomly over valuable peers
     * @param valuablePeers list of peers who have the piece
     * @param pieceIndex the piece we need
     * @return true if we could distribute the blocks
     **/
    public boolean sendBlockRequests(List<PeerInfo> valuablePeers, int pieceIndex) {

        if (torrentState.getStatus().get(pieceIndex) == PieceStatus.Downloaded) {
            return false;
        } else if (torrentState.getStatus().get(pieceIndex) == PieceStatus.Requested) {
            return false;
        } else if (torrentState.getStatus().get(pieceIndex) == PieceStatus.QUEUED) {
            return false;
        }

        if (clientState.hasPiece(pieceIndex))
            return false;


        int numberOfPeers = valuablePeers.size();
        if (numberOfPeers == 0)
            return false;

        Random random = new Random();

        int numberOfBlocks = torrentState.getNumOfBlocks();

        if (pieceIndex == torrentState.getNumberOfPieces() - 1) {
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

        } else {
            int offset = 0, countBlocks = 0;
            while (offset < torrentState.getPieceSize()) {
                Message request = new Message(PeerMessage.MsgType.REQUEST, pieceIndex, offset, torrentState.getBlockSize());
                int n = random.nextInt(numberOfPeers);
                valuablePeers.get(n).getPeerState().writeMessageQ.add(request);
                countBlocks++;
                offset += (countBlocks == numberOfBlocks) ? torrentState.getLastBlockSize() : torrentState.getBlockSize();
            }
        }

        torrentState.getStatus().set(pieceIndex, PieceStatus.QUEUED);


        return true;
    }

    public TorrentState getTorrentState() {
        return torrentState;
    }

    public ClientState getClientState() {
        return clientState;
    }
}
