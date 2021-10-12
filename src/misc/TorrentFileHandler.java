package misc;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.security.MessageDigest;
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
	
	public Map<String, BEncodedValue> getFileInfo() throws InvalidBEncodingException {
		
		return this.document.get("info").getMap();
		
		// contenu de info :
		// String name : filename
		// int length : length of the file in bytes
		// md5sum : 32 hex characters : MD5 sum of the file
		
	}
	
	public String getFilename() throws InvalidBEncodingException {
		
		return getFileInfo().get("filename").getString();
		
	}
	
	public String getSHA1Info() throws InvalidBEncodingException {
		
		String bencodInfo = document.get("info").getString();
		
		String sha1 = "";
		
		try {
			
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.reset();
			digest.update(bencodInfo.getBytes("utf8"));
			sha1 = String.format("%040x", new BigInteger(1, digest.digest()));
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		return sha1;
	}
}
