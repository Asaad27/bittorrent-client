package test;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;

import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;
import misc.tracker.TrackerHandler;

public class TestTracker {

	/*
	@Test
	public void testGenPeerId() {
		
		TrackerHandler tracker = new TrackerHandler(null, null, null, 0);
		String peerId = tracker.genPeerId();
		System.out.println(peerId);
		
	}
	
	@Test
	public void testGenQueryUri() throws NoSuchAlgorithmException, IOException {
		
		TorrentFileHandler torrentHandler = new TorrentFileHandler(new FileInputStream("src/main/resources/torrents/hello_world.txt.torrent"));
		TorrentMetaData torrentMetaData = torrentHandler.ParseTorrent();
		
		TrackerHandler tracker = new TrackerHandler(new URL(torrentMetaData.getAnnounceUrlString()),torrentMetaData.getSHA1Info(), null, 6969);
		String queryURI = tracker.buildQueryURI();
		System.out.println(queryURI);
	}
	*/
	
	@Test
	public void testQuery() throws NoSuchAlgorithmException, IOException {
		TorrentFileHandler torrent = new TorrentFileHandler(new FileInputStream("src/main/resources/torrents/by.txt.torrent"));
		TorrentMetaData metaData = torrent.ParseTorrent();
		
		TrackerHandler tracker = new TrackerHandler(new URL(metaData.getAnnounceUrlString()), metaData.getSHA1Info(), null, 6969);
		tracker.getPeerLst();
	}
}
