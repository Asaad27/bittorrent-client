package misc.torrent;

import java.util.List;
import java.util.Set;

import misc.peers.PeerInfo;
import misc.peers.PeerState;

public class EndGame extends DownloadStrat implements IObservable{
	
	private static EndGame instance;
	private List<PeerInfo> peers;
	private TorrentState status;

	private final Observer subject;

	public EndGame(List<PeerInfo> peers, TorrentState status, Observer subject) {
		this.peers = peers;
		this.status = status;
		this.subject = subject;
		subject.attach(this);
	}

	@Override
	public int updatePeerState() {
		Set<Integer> pieces = remainingPieces(status);
		for(int n : pieces) {
			for(PeerInfo peer : peers) {
				peer.getPeerState().addPieceToRequest(n);
			}
		}
		
		return -1;
	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	public static IDownloadStrat instance(List<PeerInfo> peers, TorrentState status, Observer subject) {
		if (instance == null) {
			instance = new EndGame(peers, status, subject);
		}
		return instance;
	}

	@Override
	public void peerHasPiece(int index) {

	}

	@Override
	public void peerConnection(PeerState peerState) {

	}

	@Override
	public void peerDisconnection(PeerState peerState) {

	}
}
