package misc.download.strategies;

import misc.messages.ByteBitfield;
import misc.peers.PeerInfo;
import misc.peers.PeerState;
import misc.torrent.*;
import misc.torrent.Observer;
import misc.utils.Pair;

import java.util.*;

public class RarestFirst extends DownloadStrat implements IObservable {

    private static RarestFirst instance;

    private final HashSet<Pair> rareSet = new HashSet<>();
    private final Set<PeerInfo> peers;
    private final TorrentState status;
    private final PriorityQueue<Pair> minHeap = new PriorityQueue<>();


    private RarestFirst(Set<PeerInfo> peers, TorrentState status, Observer subject) {
        this.peers = peers;
        this.status = status;
        subject.attach(this);
        initAlgo();
    }

    public static IDownloadStrat instance(Set<PeerInfo> peers, TorrentState status, Observer subject) {
        if (instance == null) {
            instance = new RarestFirst(peers, status, subject);
        }
        return instance;
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

        //TODO : fix, whe a leecher connects it switches here
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
        for (int i = 0; i < status.getNumberOfPieces(); i++) {
            if (status.getStatus().get(i) == PieceStatus.ToBeDownloaded && status.getPieceCount()[i] != 0)
                minHeap.add(new Pair(status.getPieceCount()[i], i));
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
        for (PeerInfo peer : peers) {
            ByteBitfield bf = peer.getPeerState().bitfield;
            for (int i = 0; i < status.getNumberOfPieces(); i++) {
                if (bf.hasPiece(i))
                    status.getPieceCount()[i] += 1;
            }
        }
        initRarity();
    }

    @Override
    public void peerHasPiece(int index) {
        Pair piece = new Pair(status.getPieceCount()[index], index);
        rareSet.remove(piece);
        status.getPieceCount()[index]++;
    }

    @Override
    public void peerConnection(PeerState peerState) {
        for (int i = 0; i < status.getNumberOfPieces(); i++) {
            if (peerState.hasPiece(i)) {
                peerHasPiece(i);
            }
        }
        initRarity();
    }

    //we recalculate everything, because the rarest set is gonna change
    @Override
    public void peerDisconnection(PeerState peerState) {
        for (int i = 0; i < status.getNumberOfPieces(); i++) {
            if (peerState.hasPiece(i)) {
                status.getPieceCount()[i]--;
            }
        }
        initRarity();
    }
}

//WE CALCULATE FIRST m THE RAREST PIECES
//WE PUT THEM IN A PRIORITY QUEUE <PIECEINDEX, RARETY>
//WE POLL EACH TIME AN EVENT HAPPENS
//WE UPDATE THE PIECE TO REQUEST FOR EACH PEER