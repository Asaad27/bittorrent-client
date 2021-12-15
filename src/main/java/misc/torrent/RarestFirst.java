package misc.torrent;

import java.util.*;


import misc.peers.PeerInfo;
import misc.utils.Pair;

public class RarestFirst extends DownloadStrat {
	
	private static RarestFirst instance;

	@Override
	public int updatePeerState(List<PeerInfo> peers, TorrentState status, int totalPieces) {
		
		int rarest = rarestPiece(peers, status, totalPieces); // On calcule la pièce la plus rare

		if (rarest == -1)	//on a request/donwload tous les pieces
			return -1;

		List<PeerInfo> valuablePeers =  peersByPieceIndex(peers, rarest); // Les peers qui ont la pièce en question
		
		for (PeerInfo peer : valuablePeers) {
			peer.getPeerState().addPieceToRequest(rarest);
		}
		
		return rarest;
	}
	
	public static RarestFirst instance() {
		if (instance == null) {
			instance = new RarestFirst();
		}
		return instance;
	}

	public int rarestPiece(List<PeerInfo> peers, TorrentState status, int totalPieces) {
		//TODO : if a peer gets disconnected, or a peer is connected, then make the queue a member variable

		int[] pieceCount = new int[totalPieces];
		PriorityQueue<Pair> minHeap = new PriorityQueue<>();
		for (PeerInfo peer : peers){
			ByteBitfield bf = peer.getPeerState().bitfield;
			for(int i = 0; i < totalPieces; i++) {
				if(bf.hasPiece(i))
					pieceCount[i]+=1;
			}
		}

		for (int i = 0; i < totalPieces; i++) {
			if (status.getStatus().get(i) == PieceStatus.ToBeDownloaded && pieceCount[i] != 0)
				minHeap.add(new Pair(pieceCount[i], i));
		}

		if (minHeap.isEmpty()){
			//DEBUG.log("empty heap");
			return -1;
		}


		return minHeap.poll().getIndex();
	}
}

//WE CALCULATE FIRST THE RAREST PIECES
//WE PUT THEM IN A PRIORITY QUEUE <PIECEINDEX, RARETY>
//WE POLL EACH TIME AN EVENT HAPPENS
//WE UPDATE THE PIECE TO REQUEST FOR EACH PEER