package misc.download.strategies;

import misc.messages.ByteBitfield;
import misc.peers.PeerInfo;
import misc.peers.PeerState;
import misc.torrent.*;
import misc.torrent.Observer;
import misc.utils.Pair;

import java.util.*;

import static misc.download.TCPClient.torrentMetaData;


public class RarestFirst extends DownloadStrategy implements IObservable {

    private static RarestFirst instance;

    private final HashSet<Pair> rareSet = new HashSet<>();
    private final Set<PeerInfo> peers;
    private final TorrentState status;
    private final PriorityQueue<Pair> minHeap = new PriorityQueue<>();
    private final Observer subject;


    private RarestFirst(Set<PeerInfo> peers, TorrentState status, Observer subject) {
        this.peers = peers;
        this.status = status;
        this.subject = subject;
        subject.attach(this);
        initAlgo();
    }

    public static IDownloadStrategy instance(Set<PeerInfo> peers, TorrentState status, Observer subject) {
        if (instance == null) {
            instance = new RarestFirst(peers, status, subject);
        }
        return instance;
    }

    @Override
    public void clear() {
        subject.detach(instance);
        RarestFirst.instance = null;
    }

    public boolean receivedAllBitfields() {
        for (PeerInfo peer : peers) {
            if (!peer.getPeerState().sentBitfield)
                return false;
        }
        return true;
    }

    @Override
    public int updatePeerState() {

        if (rareSet.isEmpty() && !receivedAllBitfields())    //on a request/donwload tous les pieces
            return -1;


        else if (rareSet.isEmpty())   //switch strategy
            return -3;

        Iterator<Pair> hashSetIterator = rareSet.iterator();

        Pair rarest = hashSetIterator.next();
        hashSetIterator.remove();


        return rarest.getIndex();
    }

    @Override
    public String getName() {
        return Strategies.RAREST_FIRST.toString();
    }

    private void initRarity() {
        minHeap.clear();
        rareSet.clear();
        int numberOfPieces = torrentMetaData.getNumberOfPieces();
        for (int i = 0; i < numberOfPieces; i++) {
            Piece piece = status.pieces.get(i);
            if (piece.getStatus() == PieceStatus.ToBeDownloaded && piece.getNumberOfPeerOwners() != 0)
                minHeap.add(new Pair(piece.getNumberOfPeerOwners(), i));
        }

        int minOccurrences = !minHeap.isEmpty() ? minHeap.peek().getValue() : 0;
        while (!minHeap.isEmpty()) {
            Pair element = minHeap.poll();
            if (element.getValue() != minOccurrences)
                break;
            rareSet.add(element);
        }
    }

    private void initAlgo() {
        int numberOfPieces = torrentMetaData.getNumberOfPieces();
        for (PeerInfo peer : peers) {
            ByteBitfield bf = peer.getPeerState().bitfield;
            for (int i = 0; i < numberOfPieces; i++) {
                Piece piece = status.pieces.get(i);
                if (bf.hasPiece(i))
                    piece.incrementNumOfPeerOwners();
            }
        }
        initRarity();
    }

    @Override
    public void peerHasPiece(int index) {
        Piece piece = status.pieces.get(index);
        Pair piecePair = new Pair(piece.getNumberOfPeerOwners(), index);
        rareSet.remove(piecePair);
        piece.incrementNumOfPeerOwners();
    }

    @Override
    public void peerConnection(PeerState peerState) {
        for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
            if (peerState.hasPiece(i)) {
                peerHasPiece(i);
            }
        }
        initRarity();
    }

    //we recalculate everything, because the rarest set is gonna change
    @Override
    public void peerDisconnection(PeerState peerState) {
        for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
            if (peerState.hasPiece(i)) {
                Piece piece = status.pieces.get(i);
                piece.decrementNumOfPeerOwners();
            }
        }
        initRarity();
    }
}

//WE CALCULATE FIRST m THE RAREST PIECES
//WE PUT THEM IN A PRIORITY QUEUE <PIECEINDEX, RARETY>
//WE POLL EACH TIME AN EVENT HAPPENS
//WE UPDATE THE PIECE TO REQUEST FOR EACH PEER