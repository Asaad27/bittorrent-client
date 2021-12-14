package misc.torrent;

import java.util.List;

import misc.peers.PeerInfo;

public interface IDownloadStrat {

	public abstract void updatePeerState(List<PeerInfo> peers, TorrentState status, int totalPieces);

}
