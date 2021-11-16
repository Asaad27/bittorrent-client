package misc.torrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;

public class LocalFileHandler {

	private String filename;
	private File localFile;
	private RandomAccessFile fileAccess;
	private BitSet bitfield;
	private int pieceSize;
	private int totalPieces;
	private boolean lock = Boolean.FALSE;
	private String piecesSHA1;
	
	public LocalFileHandler(String filename, int pieceSize, int pieceNb, String piecesSHA1) {
		
		this.filename = filename;
		this.localFile = new File(filename);
		this.pieceSize = pieceSize;
		this.totalPieces = pieceNb;
		this.bitfield = initBitfield(); 
		this.piecesSHA1 = piecesSHA1;
		
		try {
			this.fileAccess = new RandomAccessFile(localFile, "rw");
			fileAccess.setLength(pieceNb * pieceSize);
		} catch (IOException e) { e.printStackTrace(); }
		
	}
	
	public BitSet initBitfield() {
		BitSet bf = new BitSet(totalPieces);
		bf.clear();
		
		try {
			if(!localFile.createNewFile()) {
				for(int i = 0; i < totalPieces ; i++) {
					
					byte[] tab = new byte[pieceSize];
					fileAccess.readFully(tab);
					
					try {
						byte[] pieceSHA1 = MessageDigest.getInstance("SHA-1").digest(tab);
						System.out.println(pieceSHA1.toString());
					} catch (NoSuchAlgorithmException e) { e.printStackTrace(); } 
					
				}
			}
		} catch(IOException e) { e.printStackTrace(); }
		
		return bf;
	}
	
	public void setPieceStatus(int pieceNb, boolean value) {
		this.bitfield.set(pieceNb, value);
	}
	
	public BitSet getBitfield() {
		return this.bitfield;
	}
	
	public void writePieceBlock(int pieceNb, int offset, byte[] data) {
		if (!lock) {
			
			try {
				
				fileAccess.write(data, (pieceNb * pieceSize) + offset, data.length);
				
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
	
}
