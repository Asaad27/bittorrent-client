package misc.torrent;
import misc.utils.DEBUG;
import misc.peers.PeerInfo;
import java.util.List;

public class TorrentContext {
	
	private List<PeerInfo> peers; 
	private IDownloadStrat strat;
	private int totalPieces;
	private TorrentState status;
	
	/*public TorrentContext(List<PeerInfo> peers, Bitfield localBf, IDownloadStrat strat, int totalPieces) {
		this.peers = peers;			//TODO : bitfield
		this.localBf = localBf;
		this.strat = strat;
		this.totalPieces = totalPieces;
		this.status = new TorrentStatus(totalPieces, localBf);
	}*/

	public TorrentContext(List<PeerInfo> peers, int totalPieces, TorrentState torrentState) {
		this.peers = peers;
		chooseStrategy();
		this.totalPieces = totalPieces;
		this.status = torrentState;
	}
	
	public void setPeers(List<PeerInfo> peers) {
		this.peers = peers;
	}
	
	public void updatePeerState() {
		int piece = strat.updatePeerState(peers, status, totalPieces);
		//status.getStatus().set(piece, PieceStatus.Requested);
		/*if (piece != -1)
			DEBUG.log("*********************** la piece la plus rare est ", String.valueOf(piece));*/


	}
	
	private void chooseStrategy() {
		
		// TODO : Choisir la stratégie à appliquer en fonction du contexte 
		/* RAREST FIRST : Début
		 * RANDOM : Tous les peers ont les pièces manquantes
		 * ENGAME : Toutes les pièces sont Requested
		 */
		
		this.strat = RarestFirst.instance();
		
	}

}
