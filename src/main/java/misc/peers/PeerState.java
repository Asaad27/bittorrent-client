package misc.peers;

/**
 * state of a peer
 *
 * @author Asaad
 */

public class PeerState{
    public boolean choked;
    public boolean interested;
    public byte[] peerBitfield = null;

    public PeerState() {
    }

    public boolean hasPiece(int index) {
        int byteIndex = index / 8;
        int offset = index % 8;

        return ((peerBitfield[byteIndex]>>(7 - offset)) & 1) != 0;
    }

    /**
     * set a bit for the bitfield
     */
    public void setPiece(int index) {
        int byteIndex = index / 8;
        int offset = index % 8;
        peerBitfield[byteIndex] |= 1 << (7 - offset);
    }
}
