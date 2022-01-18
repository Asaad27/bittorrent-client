package misc.download;

import misc.download.strategies.*;
import misc.peers.ClientState;
import misc.torrent.Observer;
import misc.torrent.PieceStatus;
import misc.torrent.TorrentState;
import misc.utils.DEBUG;
import misc.peers.PeerInfo;

import java.util.Set;

import static misc.download.TCPClient.torrentMetaData;

/**
 * Handles strategies
 */
public class TorrentContext {

    private final TorrentState status;
    private final Observer subject;
    private final ClientState clientState;
    private Set<PeerInfo> peers;
    private IDownloadStrategy strategy;

    public TorrentContext(Set<PeerInfo> peers, TorrentState torrentState, ClientState clientState, Observer subject) {
        this.peers = peers;
        this.status = torrentState;
        this.subject = subject;
        this.clientState = clientState;
        chooseStrategy(Strategies.RAREST_FIRST);
    }

    public void setPeers(Set<PeerInfo> peers) {
        this.peers = peers;
    }

    public boolean updatePeerState() {
        int piece = strategy.updatePeerState();

        if (piece < 0) {
            changeStrategy(piece);
        }


        if (piece >= 0 && piece < torrentMetaData.getNumberOfPieces() && status.pieces.get(piece).getStatus() == PieceStatus.ToBeDownloaded) {
            clientState.piecesToRequest.add(piece);
            return true;
        }
        return false;
    }

    private void changeStrategy(int ID) {

        switch (ID) {
            case -3:
                this.strategy.clear();
                DEBUG.logf("changing strategy to RANDOM");
                chooseStrategy(Strategies.RANDOM);
                break;
            case -4:
                this.strategy.clear();
                DEBUG.logf("changing strategy to ENDGAME");
                chooseStrategy(Strategies.END_GAME);
                break;
            default:
                break;
        }
    }

    private void chooseStrategy(Strategies strategy) {
        switch (strategy) {
            case RANDOM:
                this.strategy = RandomPiece.instance(peers, status, subject);
                break;
            case RAREST_FIRST:
                this.strategy = RarestFirst.instance(peers, status, subject);
                break;
            case END_GAME:
                this.strategy = EndGame.instance(peers, status, subject);
                break;
        }

    }

    public IDownloadStrategy getStrategy() {
        return strategy;
    }
}
