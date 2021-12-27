package misc.torrent;
import misc.download.NIODownloadHandler;
import misc.utils.DEBUG;
import misc.peers.PeerInfo;
import java.util.List;

public class TorrentContext {
	
	private List<PeerInfo> peers; 
	private IDownloadStrat strat;
	private TorrentState status;
	private Observer subject;

	public TorrentContext(List<PeerInfo> peers, TorrentState torrentState, Observer subject) {
		this.peers = peers;
		this.status = torrentState;
		this.subject = subject;

		chooseStrategy();
	}
	
	public void setPeers(List<PeerInfo> peers) {
		this.peers = peers;
	}
	
	public void updatePeerState() {
		int piece = strat.updatePeerState();
		//status.getStatus().set(piece, PieceStatus.Requested);
		if (piece != -1)
			DEBUG.log("*********************** la piece  est ", String.valueOf(piece));

		if (piece == -3){
			DEBUG.loge("changing strat ****************************************************");
			this.strat = RandomPiece.instance(peers, status, subject);
		}

		if (piece >= 0 && piece < status.getNumberOfPieces() && status.getStatus().get(piece) == PieceStatus.ToBeDownloaded ){
			NIODownloadHandler.clientState.piecesToRequest.add(piece);
		}
	}
	//ADD ENUM STRATEGY AS A PARAM
	private void chooseStrategy() {
		// TODO : Choisir la stratégie à appliquer en fonction du contexte 
		/* RAREST FIRST : Début
		 * RANDOM : Tous les peers ont les pièces manquantes
		 * ENDGAME : Toutes les pièces sont Requested
		 */
		this.strat = RarestFirst.instance(peers, status, subject);

	}

}
