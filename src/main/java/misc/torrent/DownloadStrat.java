package misc.torrent;

import java.util.ArrayList;
import java.util.List;

import misc.peers.PeerInfo;

public abstract class DownloadStrat implements IDownloadStrat {

	public List<PeerInfo> peersByPieceIndex(List<PeerInfo> peers, int pieceNb) {
		
		List<PeerInfo> valuablePeers = new ArrayList<PeerInfo>();
		
		for(PeerInfo peer : peers) {
			if(peer.getBitfield().get(pieceNb)) {
				valuablePeers.add(peer);
			}
		}
		
		return valuablePeers;
	}
	
}
