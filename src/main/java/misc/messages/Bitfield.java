package misc.messages;

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
	public BitSet getValue() {
		return this.value;
	}
	
	public void setValue(byte[] bitfield) {
		this.value = BitSet.valueOf(bitfield);
	}
	
	public byte[] toByteArray() {
		byte[] array = this.value.toByteArray();
		reverse(array);
		return array;
		
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
		
		String s = new String("{");
		
		for(int i = 0; i < maxindex; i++) {
			String val = new String();
			if(value.get(i)) {
				val = "1";
			} else {
				val = "0";
			}
			s = s.concat(val);
			if(i != maxindex - 1) {
				s = s.concat(", ");
			}
		}
		s = s.concat("}");
		
		return s;
	}
	
	
	
	
}
