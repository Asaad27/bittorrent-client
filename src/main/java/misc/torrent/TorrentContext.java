package misc.torrent;
import misc.peers.PeerInfo;
import java.util.List;

public class TorrentContext {
	
	private List<PeerInfo> peers; 
	private IDownloadStrat strat;
	private int totalPieces;
	private Bitfield localBf;
	private TorrentStatus status;
	
	public TorrentContext(List<PeerInfo> peers, Bitfield localBf, IDownloadStrat strat, int totalPieces) {
		this.peers = peers;
		this.localBf = localBf;
		this.strat = strat;
		this.totalPieces = totalPieces;
		this.status = new TorrentStatus(totalPieces, localBf);
	}
	
	public void download() {
		strat.download(peers, status, totalPieces);
	}
	
	public void setStrategy(IDownloadStrat strat) {
		this.strat = strat;
	}

}
