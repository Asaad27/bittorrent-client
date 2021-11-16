package misc.torrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;

public class LocalFileHandler {

	private String filename;
	private File localFile;
	private RandomAccessFile fileAccess;
	private BitSet bitfield;
	private int pieceSize;
	private int totalPieces;
	private boolean lock = Boolean.FALSE;
	
	public LocalFileHandler(String filename, int pieceSize, int pieceNb) {
		
		this.filename = filename;
		this.localFile = new File(filename);
		this.pieceSize = pieceSize;
		this.totalPieces = pieceNb;
		
		try {
			this.fileAccess = new RandomAccessFile(localFile, "rw");
		} catch (FileNotFoundException e) {
			System.out.println("Fichier local introuvable");
		}
		
		this.bitfield = initBitfield();
	}
	
	public BitSet initBitfield() {
		BitSet bf = new BitSet(totalPieces);
		
		// TODO : remplir le bitset de 1 en examinant le fichier local
		
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
