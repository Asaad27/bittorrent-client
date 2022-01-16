package misc.download.strategies;

import java.util.*;

import misc.peers.PeerInfo;
import misc.torrent.Piece;
import misc.torrent.PieceStatus;
import misc.torrent.TorrentState;

import static misc.download.TCPClient.torrentMetaData;

public abstract class DownloadStrategy implements IDownloadStrategy {

	public static List<PeerInfo> peersByPieceIndex(Set<PeerInfo> peers, int pieceNb) {
		
		List<PeerInfo> valuablePeers = new ArrayList<PeerInfo>();
		
		for(PeerInfo peer : peers) {
			if (peer.getPeerState().hasPiece(pieceNb)){
				valuablePeers.add(peer);
			}
		}
		
		return valuablePeers;
	}
	
	public Set<Integer> remainingPieces(TorrentState status){
		Set<Integer> pieceSet = new LinkedHashSet<>();
		for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
			Piece piece = status.pieces.get(i);
			if(piece.getStatus() == PieceStatus.ToBeDownloaded) {
				pieceSet.add(i);
			}
		}
		
		return pieceSet;
	}



	
}
