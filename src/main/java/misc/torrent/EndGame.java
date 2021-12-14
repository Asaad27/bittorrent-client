package misc.torrent;

import java.util.List;
import java.util.Set;

import misc.peers.PeerInfo;

public class EndGame extends DownloadStrat {
	
	private static EndGame instance;

	@Override
	public int updatePeerState(List<PeerInfo> peers, TorrentState status, int totalPieces) {
		Set<Integer> pieces = remainingPieces(status, totalPieces);
		for(int n : pieces) {
			for(PeerInfo peer : peers) {
				peer.getPeerState().addPieceToRequest(n);
			}
		}
		
		return -1;
	}
	
	public static IDownloadStrat instance() {
		if (instance == null) {
			instance = new EndGame();
		}
		return instance;
	}

}
