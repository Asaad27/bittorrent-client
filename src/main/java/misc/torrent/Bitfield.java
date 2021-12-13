package misc.torrent;

import java.util.BitSet;

public class Bitfield {
	private BitSet value;
	private int maxindex;
	
	public Bitfield(int totalPieces) {
		this.value = new BitSet(totalPieces);
		this.maxindex = totalPieces;
	}
	
	public void set(int pieceNb, boolean val) {
		if((pieceNb < maxindex) && (pieceNb >= 0)) {
			this.value.set(pieceNb, val);
		} else {
			throw new IndexOutOfBoundsException();
		}
	}
	
	public boolean get(int pieceNb) {
		if((pieceNb < maxindex) && (pieceNb >= 0)) {
			return this.value.get(pieceNb);
		} else {
			throw new IndexOutOfBoundsException();
		}
	}
	
	
	
	
}
