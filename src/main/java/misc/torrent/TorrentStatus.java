package misc.torrent;

import java.util.ArrayList;
import java.util.List;

public class TorrentStatus {
	
	List<PieceStatus> status;
	
	public TorrentStatus(int totalPieces, Bitfield localBf) {
		
		status = initStatus(totalPieces, localBf);
		
	}
	
	private List<PieceStatus> initStatus(int totalPieces, Bitfield localBf) {
		
		List<PieceStatus> status = new ArrayList<PieceStatus>();
		
		for(int i = 0; i < totalPieces; i++) {
			if(localBf.get(i)) {
				status.add(PieceStatus.Verified);
			} else {
				status.add(PieceStatus.ToBeDownloaded);
			}
		}
		
		return status;
		
	}
	
	public List<PieceStatus> getStatus() {
		return this.status;
	}
}
