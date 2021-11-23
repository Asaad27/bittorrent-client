package test;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import misc.utils.Utils;
import org.junit.Test;

import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;
import misc.tracker.TrackerHandler;

public class TestTracker {
	
	@Test
	public void testTracker() throws NoSuchAlgorithmException, IOException {
		TorrentFileHandler torrent = new TorrentFileHandler(new FileInputStream("src/main/resources/torrents/by.txt.torrent"));
		TorrentMetaData metaData = torrent.ParseTorrent();
		TrackerHandler tracker = new TrackerHandler(new URL(metaData.getAnnounceUrlString()), Utils.hexStringToByteArray(metaData.getSHA1Info()), 6969);

		tracker.getPeerLst();
	}
}
