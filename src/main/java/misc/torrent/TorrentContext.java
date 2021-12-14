package misc.torrent;
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
		chooseStrategy();
		int piece = strat.updatePeerState(peers, status, totalPieces);
		if(piece != -1) { // EndGame : -1
			status.getStatus().set(piece, PieceStatus.Requested);
		}
	}
	
	public boolean allPeersHaveAllPieces() {
		for(PeerInfo peer : peers) {
			for(int i = 0; i < totalPieces; i++) {
				if(!(peer.getPeerState().bitfield.hasPiece(i))) {
					return false;
				}
			}
			
		}
		return true;
	}
	
	public boolean allPiecesAreRequested() {
		for(PieceStatus pieceStat : status.getStatus()) {
			if(pieceStat == PieceStatus.ToBeDownloaded) {
				return false;
			}
		}
		return true;
	}
	
	public boolean allPiecesAreDownloaded() {
		for(PieceStatus pieceStat : status.getStatus()) {
			if((pieceStat != PieceStatus.Downloaded) && (pieceStat != PieceStatus.Verified)) {
				return false;
			}
		}
		return true;
	}
	
	private void chooseStrategy() {
		
		// TODO : Choisir la stratégie à appliquer en fonction du contexte 
		/* RAREST FIRST : Début
		 * RANDOM : Tous les peers ont les pièces manquantes
		 * ENGAME : Toutes les pièces sont Requested
		 */
		
		if(allPiecesAreDownloaded()) {
			// this.strat = Seeder.instance();
		} else {
			if(allPiecesAreRequested()) {
				this.strat = EndGame.instance();
			} else {
				if(allPeersHaveAllPieces()) {
					this.strat = RandomPiece.instance();
				} else {
					this.strat = RarestFirst.instance();
				}
			}
		}
	}

}
