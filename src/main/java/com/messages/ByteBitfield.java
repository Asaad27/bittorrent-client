package com.messages;

public class ByteBitfield {
    public byte[] value;
    public int bitfieldSize;

    public ByteBitfield(int numPieces) {
        bitfieldSize = (numPieces + 7) / 8;
        value = new byte[bitfieldSize];
    }


    public boolean hasPiece(int index) {

        int byteIndex = index / 8;
        int offset = index % 8;

        return ((value[byteIndex] >> (7 - offset)) & 1) != 0;
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

    public void unsetPiece(int index) {
        if (value == null) {
            //in case we did not receive the peer bitfield
            return;
        }
        int byteIndex = index / 8;
        int offset = index % 8;
        value[byteIndex] &= ~(1 << (7 - offset));
    }

    public void initLeecher() {
        for (int i = 0; i < bitfieldSize; ++i) {
            value[i] = 0;
        }
    }
}
