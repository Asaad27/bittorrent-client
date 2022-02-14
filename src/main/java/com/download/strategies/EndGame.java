package com.download.strategies;

import java.util.Set;

import com.download.NIODownloadHandler;
import com.peers.PeerInfo;
import com.peers.PeerState;
import com.torrent.*;

public class EndGame extends DownloadStrategy implements IObservable {
	
	private static EndGame instance;
	private final Set<PeerInfo> peers;
	private final TorrentState status;

	public EndGame(Set<PeerInfo> peers, TorrentState status, Observer subject) {
		this.peers = peers;
		this.status = status;
		subject.attach(this);
	}

	@Override
	public int updatePeerState() {
		Set<Integer> pieces = remainingPieces(status);
		for(int i : pieces) {
			for(PeerInfo peer : peers) {
				PeerState peerState = peer.getPeerState();
				NIODownloadHandler.sendFullPieceRequest(i, peerState, status);
			}
		}
		
		return -7;
	}

	@Override
	public void clear() {

	}


	@Override
	public String getName() {
		return "ENDGAME";
	}

	public static IDownloadStrategy instance(Set<PeerInfo> peers, TorrentState status, Observer subject) {
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
