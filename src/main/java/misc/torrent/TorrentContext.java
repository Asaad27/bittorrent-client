package misc.torrent;
import misc.download.NIODownloadHandler;
import misc.download.strategies.*;
import misc.peers.ClientState;
import misc.utils.DEBUG;
import misc.peers.PeerInfo;
import java.util.List;

public class TorrentContext {
	
	private List<PeerInfo> peers; 
	private IDownloadStrat strat;
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
		int piece = strat.updatePeerState();
		//status.getStatus().set(piece, PieceStatus.Requested);
		if (piece > -1)
			DEBUG.log("*********************** la piece  est ", String.valueOf(piece), strat.getName());

		if (piece == -3){
			DEBUG.loge("changing strategie ****************************************************");
			chooseStrategy(Strategies.RANDOM);
		}

		if (piece >= 0 && piece < status.getNumberOfPieces() && status.getStatus().get(piece) == PieceStatus.ToBeDownloaded ){
			clientState.piecesToRequest.add(piece);
		}
	}
	//ADD ENUM STRATEGY AS A PARAM
	private void chooseStrategy(Strategies strategy) {
		// TODO : Choisir la stratégie à appliquer en fonction du contexte 
		/* RAREST FIRST : Début
		 * RANDOM : Tous les peers ont les pièces manquantes
		 * ENDGAME : Toutes les pièces sont Requested
		 */
		switch (strategy){
			case RANDOM:
				this.strat = RandomPiece.instance(peers, status, subject);
				break;
			case RAREST_FIRST:
				this.strat = RarestFirst.instance(peers, status, subject);
				break;
			case END_GAME:
				this.strat = EndGame.instance(peers, status, subject);
				break;
		}


	}

}
