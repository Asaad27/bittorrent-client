package misc.torrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;

import misc.utils.Utils;

public class LocalFileHandler {

	private String filename;
	private File localFile;
	private RandomAccessFile fileAccess;
	private Bitfield bitfield;
	private int pieceSize;
	private int totalPieces;
	private double fileLength;
	private boolean lock = Boolean.FALSE;
	private String piecesSHA1;
	
	public LocalFileHandler(String filename, int pieceSize, int pieceNb, double length, String piecesSHA1) {
		
		this.filename = filename;
		this.localFile = new File(filename);
		this.pieceSize = pieceSize;
		this.totalPieces = pieceNb;
		this.piecesSHA1 = piecesSHA1;
		this.fileLength = length;
		
		try {
			this.fileAccess = new RandomAccessFile(localFile, "rw");
			fileAccess.setLength((int) length);
		} catch (IOException e) { e.printStackTrace(); }
		
		this.bitfield = new Bitfield(totalPieces);
		initBitfield(); 
		
	}
	
	public void initBitfield() {
		
		try {
			if(!localFile.createNewFile()) {
				for(int i = 0; i < totalPieces ; i++) {
					
					setPieceStatus(i, verifyPieceSHA1(i));
					
				}
			}
		} catch(IOException e) { e.printStackTrace(); }
		
	}
	
	public boolean verifyPieceSHA1(int pieceNb) {
		
		int size = pieceSize;
		
		if(pieceNb == totalPieces - 1) {	
			size = (int) ((fileLength % pieceSize == 0) ? pieceSize : fileLength % pieceSize);
		}
		
		try {
			byte[] tab = new byte[size];
			
			// System.out.println("Piece Number " + (i + 1) + " :");
			fileAccess.seek(pieceNb * pieceSize);
			fileAccess.read(tab);
			
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(tab);
			byte[] pieceSHA1 = md.digest();
			String expectedStr = piecesSHA1.substring(pieceNb * 40, Math.min((pieceNb + 1) * 40, piecesSHA1.length()));
			byte[] expectedSHA1 = Utils.hexStringToByteArray(expectedStr);
			
			/*
			System.out.println("Expected SHA1 : " + Utils.bytesToHex(expectedSHA1) + " / length : " + expectedSHA1.length);
			System.out.println("Computed SHA1 : " + Utils.bytesToHex(pieceSHA1) + " / length : " + pieceSHA1.length);
			*/
			
			return Arrays.equals(pieceSHA1, expectedSHA1);
			
		} catch (NoSuchAlgorithmException | IOException e) { e.printStackTrace(); return false;} 
		
		
	}
	
	public void setPieceStatus(int pieceNb, boolean value) {
		this.bitfield.set(pieceNb, value);
	}
	
	public Bitfield getBitfield() {
		return this.bitfield;
	}
	
	public void writePieceBlock(int pieceNb, int offset, byte[] data) {
		if (!lock) {
			
			try {
				
				fileAccess.write(data, pieceNb * pieceSize + offset, data.length);
				
			} catch (IOException e) { e.printStackTrace(); }
			
			
		} else {
			// TODO : Wait pour l'écriture, Mutex ?
		}
	}
	
	public byte[] getPieceBlock(int pieceNb, int offset, int len) {
		// TODO : Ajouter mutex pour l'accès concurrent au fichier ?
		
		// On vérifie que l'on a cette pièce
		if(bitfield.get(pieceNb)) {
		
			byte[] data = new byte[len];
			try {
				fileAccess.readFully(data, pieceNb * pieceSize + offset, len);
			} catch (IOException e) { e.printStackTrace(); }
			
			return data;
		} else {
			return null;
		}
	}
	
	public void close() {
		try {
			fileAccess.close();
		} catch (IOException e){ e.printStackTrace(); }
	}
	
}
