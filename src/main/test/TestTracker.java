
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import misc.utils.Utils;
import org.junit.Test;

import misc.peers.PeerInfo;
import misc.torrent.TorrentFileController;
import misc.torrent.TorrentMetaData;
import misc.tracker.TrackerHandler;

public class TestTracker {
	
	@Test
	public void testTracker() throws NoSuchAlgorithmException, IOException {
		TorrentFileController torrent = new TorrentFileController(new FileInputStream("src/main/resources/torrents/by.txt.torrent"));
		TorrentMetaData metaData = torrent.ParseTorrent();
		TrackerHandler tracker = new TrackerHandler(new URL(metaData.getAnnounceUrlString()), Utils.hexStringToByteArray(metaData.getSHA1Info()), 6969, metaData.getNumberOfPieces());

		Set<PeerInfo> lst = tracker.getPeerLst();
		for(PeerInfo p : lst) {
			System.out.println(p);
		}
		
	}
}
