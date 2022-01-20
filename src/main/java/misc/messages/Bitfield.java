package misc.messages;

import java.util.BitSet;

public class Bitfield {
	private BitSet value;
	private final int maxIndex;
	
	public Bitfield(int totalPieces) {
		this.value = new BitSet(totalPieces);
		this.maxIndex = totalPieces;
	}
	
	public void set(int pieceNb, boolean val) {
		if((pieceNb < maxIndex) && (pieceNb >= 0)) {
			this.value.set(pieceNb, val);
		} else {
			throw new IndexOutOfBoundsException();
		}
	}
	
	public boolean get(int pieceNb) {
		if((pieceNb < maxIndex) && (pieceNb >= 0)) {
			return this.value.get(pieceNb);
		} else {
			throw new IndexOutOfBoundsException();
		}
	}
	public BitSet getValue() {
		return this.value;
	}
	
	public void setValue(byte[] bitfield) {
		this.value = BitSet.valueOf(bitfield);
	}

	public static void reverse(byte[] in) {
	      if (in == null) {
	          return;
	      }
	      int i = 0;
	      int j = in.length - 1;
	      byte tmp;
	      while (j > i) {
	          tmp = in[j];
	          in[j] = in[i];
	          in[i] = tmp;
	          j--;
	          i++;
	      }
	  }

	
	@Override
	public String toString() {
		
		String s = "{";
		
		for(int i = 0; i < maxIndex; i++) {
			String val = "";
			if(value.get(i)) {
				val = "1";
			} else {
				val = "0";
			}
			s = s.concat(val);
			if(i != maxIndex - 1) {
				s = s.concat(", ");
			}
		}
		s = s.concat("}");
		
		return s;
	}
	
	
	
	
}
