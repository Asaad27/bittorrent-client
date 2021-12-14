package misc.torrent;

import java.util.LinkedList;
import java.util.List;
import misc.peers.PeerInfo;

public class RarestFirst extends DownloadStrat {
	
	private static RarestFirst instance;

	@Override
	public void updatePeerState(List<PeerInfo> peers, TorrentState status, int totalPieces) {
		
		int rarest = rarestPiece(peers, status, totalPieces); // On calcule la pièce la plus rare
		List<PeerInfo> valuablePeers =  peersByPieceIndex(peers, rarest); // Les peers qui ont la pièce en question
		
		for (PeerInfo peer : valuablePeers) {
			peer.getPeerState().addPieceToRequest(rarest);
		}
		
	}
	
	public static RarestFirst instance() {
		if (instance == null) {
			instance = new RarestFirst();
		}
		return instance;
	}

	public int rarestPiece(List<PeerInfo> peers, TorrentState status, int totalPieces) {
		
		/*List<BitSet> bitfields = new LinkedList<BitSet>();
		for(PeerInfo peer : peers) {					//TODO : fix
			bitfields.add(peer.getBitfield());
		}*/
		List<ByteBitfield> bitfields = new LinkedList<>();
		for (PeerInfo peer : peers){
			bitfields.add(peer.getPeerState().bitfield);
		}
		int[] pieceCount = new int[totalPieces]; // Array du nombre de peers possédant les pieces
		
		/*for(BitSet bf : bitfields) {
			for(int i = 0; i < totalPieces; i++) {
				if(bf.get(i)) {
					pieceCount[i]+=1;
				}
			}
		}*/

		for(ByteBitfield bf : bitfields) {
			for(int i = 0; i < totalPieces; i++) {
				if(bf.hasPiece(i)) {
					pieceCount[i]+=1;
				}
			}
		}
		
		int min = 0;
		for(int i = 0;  i < totalPieces; i++) {
			if((pieceCount[i] < pieceCount[min]) && (status.getStatus().get(i) == PieceStatus.ToBeDownloaded) && (pieceCount[i] > 0)) {
				min = i;
			}
		}
		
		return min;
		
	}
}
