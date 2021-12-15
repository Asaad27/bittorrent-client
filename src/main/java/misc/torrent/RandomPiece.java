package misc.torrent;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import misc.peers.PeerInfo;

public class RandomPiece extends DownloadStrat {

	private static RandomPiece instance;
	@Override
	public int updatePeerState(List<PeerInfo> peers, TorrentState status, int totalPieces) {
		Set<Integer> pieceSet = remainingPieces(status, totalPieces);
		int n = new Random().nextInt(pieceSet.size());
		Iterator<Integer> iter = pieceSet.iterator();
		for (int i = 0; i < n; i++) {
		    iter.next();
		}
		
		int random = iter.next(); 
		for(PeerInfo peer : peers) { // ValuablePeers = Peers car toutes les peers ont toutes les pièces
			peer.getPeerState().addPieceToRequest(random);
		}
		
		return random;
	}

	public static IDownloadStrat instance() {
		if (instance == null) {
			instance = new RandomPiece();
		}
		return instance;
	}
	

}
