package misc.download.strategies;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import misc.download.NIODownloadHandler;
import misc.download.TCPClient;
import misc.messages.Message;
import misc.messages.PeerMessage;
import misc.peers.PeerInfo;
import misc.peers.PeerState;
import misc.torrent.*;

public class EndGame extends DownloadStrat implements IObservable {
	
	private static EndGame instance;
	private final List<PeerInfo> peers;
	private final TorrentState status;
	public static final HashMap<Integer, List<Boolean>> piecesAndBlocks = new HashMap<>();

	public EndGame(List<PeerInfo> peers, TorrentState status, Observer subject) {
		this.peers = peers;
		this.status = status;
		subject.attach(this);
		//map that stores the blocks for the remaining pieces
		initPieceBlockMap();
	}

	//TODO : test
	public static void blockDownloaded(int index, int begin, TorrentState torrentState) {
		/*//int numberOfBlocks = torrentState.getNumberOfBlock(index);
		//TODO : test
		int blockIndex = numberOfBlocks / begin;
		piecesAndBlocks.get(index).set(blockIndex, true);*/
	}

	public void initPieceBlockMap(){

	}

	public static void sendCancels(List<PeerInfo> peerInfoList, int index, int begin, int length, PeerState peerState) {
		Message cancel = new Message(PeerMessage.MsgType.CANCEL, index, begin, length);
		for (PeerInfo peer: peerInfoList) {
			if (peer.getPeerState().killed)
				continue;
			if (!peer.getPeerState().equals(peerState)){
				peer.getPeerState().writeMessageQ.addFirst(cancel);
			}
		}
	}

	@Override
	public int updatePeerState() {
		Set<Integer> pieces = remainingPieces(status);
		for(int i : pieces) {
			for(PeerInfo peer : peers) {
				PeerState peerState = peer.getPeerState();
				NIODownloadHandler.sendFullPieceRequest(i, peerState, status, TCPClient.torrentMetaData);
			}
		}
		
		return -7;
	}




	@Override
	public String getName() {
		return "ENDGAME";
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
