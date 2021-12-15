package misc.torrent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import misc.peers.PeerInfo;

public abstract class DownloadStrat implements IDownloadStrat {

	public List<PeerInfo> peersByPieceIndex(List<PeerInfo> peers, int pieceNb) {
		
		List<PeerInfo> valuablePeers = new ArrayList<PeerInfo>();
		
		for(PeerInfo peer : peers) {
			/*if(peer.getBitfield().get(pieceNb)) {  //TODO : fix bitfield class
				valuablePeers.add(peer);
			}*/
			if (peer.getPeerState().hasPiece(pieceNb)){
				valuablePeers.add(peer);
			}
		}
		
		return valuablePeers;
	}
	
	public Set<Integer> remainingPieces(TorrentState status, int totalPieces){
		Set<Integer> pieceSet = new LinkedHashSet<>();
		for (int i = 0; i < totalPieces; i++) {
			if(status.getStatus().get(i) == PieceStatus.ToBeDownloaded) {
				pieceSet.add(i);
			}
		}
		
		return pieceSet;
	}
	
}
