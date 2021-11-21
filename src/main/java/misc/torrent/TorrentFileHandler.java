package misc.torrent;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.security.MessageDigest;
import be.adaxisoft.bencode.*;

import misc.utils.Utils;


/**
 * Class to read and parse torrent metadata from .torrent file
 * @author Asaad
 */

public class TorrentFileHandler {

	private FileInputStream srcTorrentFile;
	private BDecoder reader;
	private Map<String, BEncodedValue> document;



	public TorrentFileHandler(FileInputStream torrentFile) {
		this.srcTorrentFile = torrentFile;
		this.reader = new BDecoder(this.srcTorrentFile);


	}

	/**
	 * Decode torrent file and create a TorrentMetaData object containing all meta informations
	 */
	public  TorrentMetaData ParseTorrent() throws IOException, NoSuchAlgorithmException {
		try {

			this.document = reader.decodeMap().getMap();

		} catch (InvalidBEncodingException e) {

			System.out.println("BEncodage du fichier torrent invalide");
			System.err.println("error : " + e.getMessage());
			return null;

		} catch (IOException e) {

			System.out.println("Fichier d'entrée invalide lors du décodage du fichier Torrent");
			System.err.println(e.getMessage());
			return null;

		}

		TorrentMetaData torrentMetaData = new TorrentMetaData();


			torrentMetaData.setAnnounceUrlString(getAnnounceURL());
			torrentMetaData.setName(getFilename());
			torrentMetaData.setComment(getComment());
			torrentMetaData.setSHA1Info(getSHA1Info());
			torrentMetaData.setSHA1InfoByte(getSHA1InfoBytes());
			torrentMetaData.setLength(getLength());
			torrentMetaData.setCreatedBy(getCreatedBy());
			torrentMetaData.setCreationDate(getCreationDate());
			torrentMetaData.setPieceLength(getPieceLength());
			torrentMetaData.setPieces(getPieces());
			torrentMetaData.setNumberOfPieces(((int) ((torrentMetaData.getLength() + getPieceLength() - 1 ) / getPieceLength())));
			torrentMetaData.setPiecesList(getPiecesList());


		return torrentMetaData;
	}

	private Long getLength() throws  InvalidBEncodingException{
		if (!getFileInfo().containsKey("length")) {
			System.err.println("The length field does not exists");
			return null;
		}

		return getFileInfo().get("length").getLong();
	}

	private Date getCreationDate() throws InvalidBEncodingException{
		if (!document.containsKey("creation date")) {
			System.err.println("The creation date field does not exists");
			return null;
		}
		Long num = this.document.get("creation date").getLong();
		Date date = new Date(num * 1000);

		return date;
	}

	private String getCreatedBy() throws InvalidBEncodingException {
		if (!document.containsKey("created by")) {
			System.err.println("The created by field does not exists");
			return null;
		}
		return this.document.get("created by").getString();
	}

	private String getComment() throws InvalidBEncodingException {
		if (!document.containsKey("comment")) {
			System.err.println("The comment field does not exists");
			return null;
		}
		return this.document.get("comment").getString();
	}


	private String getAnnounceURL() throws InvalidBEncodingException {
		if (!document.containsKey("announce")) {
			System.err.println("The announce field does not exists");
			return null;
		}
		return this.document.get("announce").getString();

	}

	private Map<String, BEncodedValue> getFileInfo() throws InvalidBEncodingException {

		if (!document.containsKey("info")) {
			System.err.println("The info field does not exists");
			return null;
		}

		return this.document.get("info").getMap();

		// contenu de info :
		// String name : filename
		// int length : length of the file in bytes
		// md5sum : 32 hex characters : MD5 sum of the file

	}

	private String getPieces() throws InvalidBEncodingException {
		if (!Objects.requireNonNull(getFileInfo()).containsKey("pieces")) {
			System.err.println("The pieces field does not exists");
			return null;
		}

		return Utils.bytesToHex(getFileInfo().get("pieces").getBytes());
	}

	private List<String> getPiecesList() throws InvalidBEncodingException {

		String result = getPieces();
		assert result != null;
		int numberOfPieces = result.length();
		List<String> piecesList = new ArrayList<>();
		for (int i = 0; i < numberOfPieces; i += 40)
			piecesList.add(result.substring(i, i+40));

		return piecesList;
	}

	private int getPieceLength() throws  InvalidBEncodingException{
		if (!Objects.requireNonNull(getFileInfo()).containsKey("piece length")) {
			System.err.println("The piece length field does not exists");
			return -1;
		}

		return getFileInfo().get("piece length").getInt();
	}

	private String getFilename() throws InvalidBEncodingException {

		if (!getFileInfo().containsKey("name")) {
			System.err.println("The name field does not exists");
			return null;
		}
		return getFileInfo().get("name").getString();

	}



	private String getSHA1Info() throws IOException, NoSuchAlgorithmException {


		Map<String, BEncodedValue> bencodInfo = document.get("info").getMap();

		MessageDigest md = MessageDigest.getInstance("SHA-1");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BEncoder.encode(bencodInfo, baos);

		byte[] bytes = baos.toByteArray();


		return Utils.bytesToHex(md.digest(bytes));
	}

	private byte[] getSHA1InfoBytes() throws IOException, NoSuchAlgorithmException{
		Map<String, BEncodedValue> bencodInfo = document.get("info").getMap();

		MessageDigest md = MessageDigest.getInstance("SHA-1");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BEncoder.encode(bencodInfo, baos);

		byte[] bytes = baos.toByteArray();

		return md.digest(bytes);
	}


}
