package misc.torrent;

import java.util.List;

import misc.peers.PeerInfo;

public interface IDownloadStrat {

	public abstract void download(List<PeerInfo> peers, TorrentStatus status, int totalPieces);

}
