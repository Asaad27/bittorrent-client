package com.download.strategies;

import java.util.*;

import com.peers.PeerInfo;
import com.peers.PeerState;
import com.torrent.IObservable;
import com.torrent.Observer;
import com.torrent.TorrentState;

import static com.download.TCPClient.torrentMetaData;

public class RandomPiece extends DownloadStrategy implements IObservable {

	private static RandomPiece instance;
	private final Set<PeerInfo> peers;
	private Set<Integer> pieceSet;
	private final TorrentState status;
	private Set<Integer> piecesWithNoPeers;

	private final com.torrent.Observer subject;
	private final static int Threshold = 1;

	private RandomPiece(Set<PeerInfo> peers, TorrentState status, Observer subject) {
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
		if (pieceSet.size() <= Threshold) {
			return -4;
		}
		int random = -1;
		List<PeerInfo> valuablePeers;
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

	@Override
	public void clear() {
		subject.detach(instance);
		RandomPiece.instance = null;
	}

	@Override
	public String getName() {
		return "RANDOM";
	}

	public static IDownloadStrategy instance(Set<PeerInfo> peers, TorrentState status, Observer subject) {
		if (instance == null) {
			instance = new RandomPiece(peers, status, subject);
		}
		return instance;
	}



	@Override
	public void peerHasPiece(int index) {
		status.pieces.get(index).incrementNumOfPeerOwners();
		if (piecesWithNoPeers.contains(index)){
			pieceSet.add(index);
			piecesWithNoPeers.remove(index);
		}
	}

	@Override
	public void peerConnection(PeerState peerState) {
		for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
			if (peerState.hasPiece(i)) {
				peerHasPiece(i);
			}
		}

	}

	@Override
	public void peerDisconnection(PeerState peerState) {
		for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
			if (peerState.hasPiece(i)) {
				status.pieces.get(i).decrementNumOfPeerOwners();
			}
		}
	}
}
