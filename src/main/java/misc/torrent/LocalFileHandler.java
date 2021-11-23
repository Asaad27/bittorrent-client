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
	private BitSet bitfield;
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
			fileAccess.setLength(pieceNb * pieceSize);
		} catch (IOException e) { e.printStackTrace(); }
		
		this.bitfield = initBitfield(); 
		
	}
	
	public BitSet initBitfield() {
		BitSet bf = new BitSet(totalPieces);
		bf.clear();
		
		try {
			if(!localFile.createNewFile()) {
				for(int i = 0; i < totalPieces ; i++) {
					
					int size = pieceSize;
					
					if(i == totalPieces - 1) {	
						size = (int) ((fileLength % pieceSize == 0) ? pieceSize : fileLength % pieceSize);
					}
					
					byte[] tab = new byte[size];
					
					// System.out.println("Piece Number " + (i + 1) + " :");
					fileAccess.seek(i * pieceSize);
					fileAccess.read(tab);
					
					try {
						MessageDigest md = MessageDigest.getInstance("SHA-1");
						md.update(tab);
						byte[] pieceSHA1 = md.digest();
						String expectedStr = piecesSHA1.substring(i * 40, Math.min((i + 1) * 40, piecesSHA1.length()));
						byte[] expectedSHA1 = Utils.hexStringToByteArray(expectedStr);
						
						/*
						System.out.println("Expected SHA1 : " + Utils.bytesToHex(expectedSHA1) + " / length : " + expectedSHA1.length);
						System.out.println("Computed SHA1 : " + Utils.bytesToHex(pieceSHA1) + " / length : " + pieceSHA1.length);
						*/
						
						if(Arrays.equals(pieceSHA1, expectedSHA1)) {
							bf.set(i, true);
						} else {
							bf.set(i, false);
						}
						
					} catch (NoSuchAlgorithmException e) { e.printStackTrace(); } 
					
				}
			}
		} catch(IOException e) { e.printStackTrace(); }
		
		return bf;
	}
	
	public void setPieceStatus(int pieceNb, boolean value) {
		this.bitfield.set(pieceNb, value);
	}
	
	public byte[] getBitfield() {
		return this.bitfield.toByteArray();
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
	
	public String bitfieldStr() {
		String s = new String("{");
		for(int i = 0; i < totalPieces; i++) {
			String val = new String();
			if(bitfield.get(i)) {
				val = "1";
			} else {
				val = "0";
			}
			s = s.concat(val);
			if(i != totalPieces - 1) {
				s = s.concat(", ");
			}
		}
		
		s = s.concat("}");
		return s;
	}
	
}
