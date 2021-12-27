package misc.torrent;

import java.util.*;

import misc.peers.PeerInfo;
import misc.peers.PeerState;

public class RandomPiece extends DownloadStrat implements IObservable {

	private static RandomPiece instance;
	private List<PeerInfo> peers;
	private Set<Integer> pieceSet;
	private TorrentState status;
	private Set<Integer> piecesWithNoPeers;

	private final Observer subject;

	private RandomPiece(List<PeerInfo> peers, TorrentState status, Observer subject) {
		this.peers = peers;
		this.status = status;
		this.subject = subject;
		this.subject.attach(this);
		initAlgo();
	}

	private void initAlgo() {
		piecesWithNoPeers = new HashSet<>();
		pieceSet = remainingPieces(status);
	}

	@Override
	public int updatePeerState() {
		//System.out.println("random");
		int random = -1;
		List<PeerInfo> valuablePeers = null;
		while(!pieceSet.isEmpty()){
			int n = new Random().nextInt(pieceSet.size());
			Iterator<Integer> iter = pieceSet.iterator();
			for (int i = 0; i < n; i++) {
				iter.next();
			}
			random = iter.next();
			iter.remove();
			valuablePeers = peersByPieceIndex(peers, random);
			if (valuablePeers.isEmpty()){
				piecesWithNoPeers.add(random);
			}
			else
				break;
		}

		return random;
	}

	public static IDownloadStrat instance(List<PeerInfo> peers, TorrentState status, Observer subject) {
		if (instance == null) {
			instance = new RandomPiece(peers, status, subject);
		}
		return instance;
	}

	@Override
	public void peerHasPiece(int index) {
		status.getPieceCount()[index]++;
		if (piecesWithNoPeers.contains(index)){
			pieceSet.add(index);
			piecesWithNoPeers.remove(index);
		}

	}

	@Override
	public void peerConnection(PeerState peerState) {
		for (int i = 0; i < status.getNumberOfPieces(); i++) {
			if (peerState.hasPiece(i)) {
				peerHasPiece(i);
			}
		}
	}

	@Override
	public void peerDisconnection(PeerState peerState) {
		for (int i = 0; i < status.getNumberOfPieces(); i++) {
			if (peerState.hasPiece(i)) {
				status.getPieceCount()[i]--;
			}
		}
	}
}
