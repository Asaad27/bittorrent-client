package test;


import static junit.framework.TestCase.assertEquals;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import be.adaxisoft.bencode.InvalidBEncodingException;
import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;

import org.junit.Test;


public class InfoHashTest {
	

	@Test
	public void testFile1() throws IOException, NoSuchAlgorithmException {
		TorrentFileHandler torrentHandler = new TorrentFileHandler(new FileInputStream("src/main/resources/torrents/hello_world.txt.torrent"));
		TorrentMetaData torrentMetaData = torrentHandler.ParseTorrent();
		//Torrent t = new Torrent(new File(""));
		assertEquals("285DCBB0DC5AE2ECB78F363AD1295A321C8EBFAF", torrentMetaData.getSHA1Info());
	}
	
	@Test
	public void testFile2() throws IOException, NoSuchAlgorithmException {
		TorrentFileHandler torrentHandler = new TorrentFileHandler(new FileInputStream("src/main/resources/torrents/iceberg.jpg.torrent"));
		TorrentMetaData torrentMetaData = torrentHandler.ParseTorrent();
		assertEquals("067133ACE5DD0C5027B99DE5D4BA512828208D5B", torrentMetaData.getSHA1Info());
	}
	
	@Test
	public void testFile3() throws IOException, NoSuchAlgorithmException {
		TorrentFileHandler torrentHandler = new TorrentFileHandler(new FileInputStream("src/main/resources/torrents/test.torrent"));
		TorrentMetaData torrentMetaData = torrentHandler.ParseTorrent();

		assertEquals("e5f669898038384d240a0f6356582b35ca3181a6".toUpperCase(), torrentMetaData.getSHA1Info());
	}

}
