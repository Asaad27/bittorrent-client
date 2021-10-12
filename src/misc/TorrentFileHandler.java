package misc;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import be.adaxisoft.bencode.*;

public class TorrentFileHandler {
	
	private FileInputStream srcTorrentFile;
	private BDecoder reader;
	private Map<String, BEncodedValue> document;
	
	public TorrentFileHandler(FileInputStream torrentFile) {
		this.srcTorrentFile = torrentFile;
		this.reader = new BDecoder(this.srcTorrentFile);
		
		try {
			
			this.document = reader.decodeMap().getMap();
			
		} catch (InvalidBEncodingException e) {
			
			System.out.println("BEncodage du fichier torrent invalide");
			
		} catch (IOException e) {
			
			System.out.println("Fichier d'entrée invalide lors du décodage du fichier Torrent");
			
		}
	}
	
	public String getAnnounceURL() throws InvalidBEncodingException {
		
		return this.document.get("announce").getString();
		
	}
	
}
