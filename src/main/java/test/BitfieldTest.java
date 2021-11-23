package test;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import misc.torrent.LocalFileHandler;
import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;
import misc.utils.Utils;

public class BitfieldTest {
	@Test
	public void localBitfield() throws IOException, NoSuchAlgorithmException {
		TorrentFileHandler torrent = new TorrentFileHandler(new FileInputStream("src/main/resources/torrents/lorem.txt.torrent"));
		TorrentMetaData metaData = torrent.ParseTorrent();
		
		System.out.println("Name : " + metaData.getName());
		System.out.println("Number of pieces : " + metaData.getNumberOfPieces());
		System.out.println("Piece length : " + metaData.getPieceLength());
		System.out.println("SHA-1 Hash : " + metaData.getPieces());
		
		String CURR_DIR = "/home/cheylusp/Documents/Bittorent/equipe5/src/main/resources/torrents/";
		LocalFileHandler localFile = new LocalFileHandler(CURR_DIR + metaData.getName(),metaData.getPieceLength(), metaData.getNumberOfPieces(), metaData.getLength(), metaData.getPieces());
		System.out.println(localFile.getBitfield());
	}

}
