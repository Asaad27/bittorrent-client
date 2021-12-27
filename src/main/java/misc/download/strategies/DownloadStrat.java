package misc.download.strategies;

import java.util.*;

import misc.peers.PeerInfo;
import misc.torrent.PieceStatus;
import misc.torrent.TorrentState;

public abstract class DownloadStrat implements IDownloadStrat {


	public static List<PeerInfo> peersByPieceIndex(List<PeerInfo> peers, int pieceNb) {
		
		List<PeerInfo> valuablePeers = new ArrayList<PeerInfo>();
		
		for(PeerInfo peer : peers) {
			if (peer.getPeerState().killed)
				continue;
			if (peer.getPeerState().hasPiece(pieceNb)){
				valuablePeers.add(peer);
			}
		}
		
		return valuablePeers;
	}
	
	public Set<Integer> remainingPieces(TorrentState status){
		Set<Integer> pieceSet = new LinkedHashSet<>();
		for (int i = 0; i < status.getNumberOfPieces(); i++) {
			if(status.getStatus().get(i) == PieceStatus.ToBeDownloaded) {
				pieceSet.add(i);
			}
		}
		
		return pieceSet;
	}



	
}
