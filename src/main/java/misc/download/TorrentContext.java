package misc.download;
import misc.download.strategies.*;
import misc.peers.ClientState;
import misc.torrent.Observer;
import misc.torrent.PieceStatus;
import misc.torrent.TorrentState;
import misc.utils.DEBUG;
import misc.peers.PeerInfo;
import java.util.List;

public class TorrentContext {
	
	private List<PeerInfo> peers; 
	private IDownloadStrat strategy;
	private final TorrentState status;
	private final Observer subject;
	private final ClientState clientState;

	public TorrentContext(List<PeerInfo> peers, TorrentState torrentState, ClientState clientState, Observer subject) {
		this.peers = peers;
		this.status = torrentState;
		this.subject = subject;
		this.clientState = clientState;
		chooseStrategy(Strategies.RAREST_FIRST);
	}
	
	public void setPeers(List<PeerInfo> peers) {
		this.peers = peers;
	}
	
	public void updatePeerState() {
		int piece = strategy.updatePeerState();
		//status.getStatus().set(piece, PieceStatus.Requested);
		if (piece > -1)
			DEBUG.log("*********************** la piece  est ", String.valueOf(piece), strategy.getName());

		if (piece == -3){
			DEBUG.loge("changing strategie to RANDOM");
			chooseStrategy(Strategies.RANDOM);
		}

		if (piece == -4){
			DEBUG.loge("changing strategie to ENDGAME");
			chooseStrategy(Strategies.END_GAME);
		}

		if (piece >= 0 && piece < status.getNumberOfPieces() && status.getStatus().get(piece) == PieceStatus.ToBeDownloaded ){
			clientState.piecesToRequest.add(piece);

		}

	}
	//ADD ENUM STRATEGY AS A PARAM
	private void chooseStrategy(Strategies strategy) {
		/* RAREST FIRST : Début
		 * RANDOM : Tous les peers ont les pièces manquantes
		 * ENDGAME : Toutes les pièces sont Requested
		 */
		switch (strategy){
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

	public IDownloadStrat getStrategy() {
		return strategy;
	}
}
