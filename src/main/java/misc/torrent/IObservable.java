package misc.torrent;

import misc.peers.PeerState;

public interface IObservable {
   void peerHasPiece(int index);
   void peerConnection(PeerState peerState);
   void peerDisconnection(PeerState peerState);
}
