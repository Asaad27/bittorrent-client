package misc.torrent;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
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


	}

	public  TorrentMetaData ParseTorrent() throws InvalidBEncodingException {
		try {

			this.document = reader.decodeMap().getMap();

		} catch (InvalidBEncodingException e) {

			System.out.println("BEncodage du fichier torrent invalide");

		} catch (IOException e) {

			System.out.println("Fichier d'entrée invalide lors du décodage du fichier Torrent");

		}

		var torrentMetaData = new TorrentMetaData();
		// TODO : fix bugs 	&& add the other attributes
		torrentMetaData.setAnnounceUrlString(getAnnounceURL());
		torrentMetaData.setName(getFilename());
		//torrentMetaData.setSHA1Info(getSHA1Info());
		torrentMetaData.setLength(getFileInfo().get("length").toString());



		return torrentMetaData;
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
		
		return getFileInfo().get("name").getString();
		
	}

	//TODO : fix
	public String getSHA1Info() throws InvalidBEncodingException {
		
		byte[] bencodInfo = document.get("info").getBytes();
		
		String sha1 = "";
		
		try {
			
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.reset();
			// TODO : Vérifier les tailles /!\
			digest.update(bencodInfo);
			sha1 = Arrays.toString(digest.digest());
			
			// SHA-1 Length : 20
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		return sha1;
	}
}
