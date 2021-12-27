package misc.messages;

public class ByteBitfield {
    public byte[] value;
    public int bfldSize;

    public ByteBitfield(int numPieces) {
        bfldSize = (numPieces + 7) / 8 ;
        value = new byte[bfldSize];
    }


    public boolean hasPiece(int index) {

        int byteIndex = index / 8;
        int offset = index % 8;

        return ((value[byteIndex]>>(7 - offset)) & 1) != 0;
    }

    /**
     * set a bit for the bitfield
     */
    public void setPiece(int index) {
        if (value == null) {
            //in case we did not receive the peer bitfield
            return;
        }
        int byteIndex = index / 8;
        int offset = index % 8;
        value[byteIndex] |= 1 << (7 - offset);
    }

    public void initLeecher(){
        for (int i = 0; i < bfldSize; ++i) {
            value[i] = 0;
        }
    }
}
