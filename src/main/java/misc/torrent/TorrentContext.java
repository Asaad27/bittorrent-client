package misc.torrent;
import misc.peers.PeerInfo;
import java.util.List;

public class TorrentContext {
	
	private List<PeerInfo> peers; 
	private IDownloadStrat strat;
	private int totalPieces;
	private ByteBitfield localBf;
	private TorrentStatus status;
	
	/*public TorrentContext(List<PeerInfo> peers, Bitfield localBf, IDownloadStrat strat, int totalPieces) {
		this.peers = peers;			//TODO : bitfield
		this.localBf = localBf;
		this.strat = strat;
		this.totalPieces = totalPieces;
		this.status = new TorrentStatus(totalPieces, localBf);
	}*/

	public TorrentContext(List<PeerInfo> peers, ByteBitfield localBf, IDownloadStrat strat, int totalPieces) {
		this.peers = peers;
		this.localBf = localBf;
		this.strat = strat;
		this.totalPieces = totalPieces;
		this.status = new TorrentStatus(totalPieces, localBf);
	}
	
	public void setPeers(List<PeerInfo> peers) {
		this.peers = peers;
	}
	
	public void download() {
		strat.download(peers, status, totalPieces);
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
