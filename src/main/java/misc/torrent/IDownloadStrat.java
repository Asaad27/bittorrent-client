package misc.torrent;

import java.util.List;

import misc.peers.PeerInfo;

public interface IDownloadStrat {

	public abstract int updatePeerState(List<PeerInfo> peers, TorrentState status, int totalPieces);

}
