package misc.torrent;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import misc.peers.PeerInfo;

public class RarestFirst extends DownloadStrat {

	@Override
	public void download(List<PeerInfo> peers, TorrentStatus status, int totalPieces) {
		
		int rarest = rarestPiece(peers, status, totalPieces); // On calcule la pièce la plus rare
		List<PeerInfo> valuablePeers =  peersByPieceIndex(peers, rarest); // Les peers qui ont la pièce en question
		
		//TODO : le Download
		
	}

	public int rarestPiece(List<PeerInfo> peers, TorrentStatus status, int totalPieces) {
		
		List<BitSet> bitfields = new LinkedList<BitSet>();
		for(PeerInfo peer : peers) {
			bitfields.add(peer.getBitfield());
		}
		
		int[] pieceCount = new int[totalPieces]; // Array du nombre de peers possédant les pieces
		
		for(BitSet bf : bitfields) {
			
			for(int i = 0; i < totalPieces; i++) {
				if(bf.get(i)) {
					pieceCount[i]+=1;
				}
			}
			
		}
		
		int min = 0;
		for(int i = 0;  i < totalPieces; i++) {
			if((pieceCount[i] < pieceCount[min]) && (status.getStatus().get(i) == PieceStatus.ToBeDownloaded)) {
				min = i;
			}
		}
		
		return min;
		
	}
}
