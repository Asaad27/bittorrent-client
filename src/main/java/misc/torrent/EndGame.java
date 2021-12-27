package misc.torrent;

import java.util.List;
import java.util.Set;

import misc.peers.PeerInfo;

public class EndGame extends DownloadStrat {
	
	private static EndGame instance;
	private List<PeerInfo> peers;
	private TorrentState status;

	public EndGame(List<PeerInfo> peers, TorrentState status) {
		this.peers = peers;
		this.status = status;
	}

	@Override
	public int updatePeerState() {
		Set<Integer> pieces = remainingPieces(status);
		for(int n : pieces) {
			for(PeerInfo peer : peers) {
				peer.getPeerState().addPieceToRequest(n);
			}
		}
		
		return -1;
	}

	public static IDownloadStrat instance(List<PeerInfo> peers,  TorrentState status) {
		if (instance == null) {
			instance = new EndGame(peers, status);
		}
		return instance;
	}

}
