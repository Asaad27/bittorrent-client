package misc.peers;
import misc.messages.ByteBitfield;

public abstract class State {
    public boolean choked = true;
    public boolean interested;
    public boolean receivedHandshake = false;
    public boolean sentHandshake = false;
    public boolean receivedBitfield = false;
    public boolean sentBitfield = false;
    public boolean weAreChokedByPeer = true;
    public boolean killed = false;

    public ByteBitfield bitfield = null;
    public int numberNonResponseIter = 0;
    public int numberOfRequests = 0;
    

    public State(int numPieces) {
        bitfield = new ByteBitfield(numPieces);
    }



    public boolean hasPiece(int index) {
        return bitfield.hasPiece(index);
    }

    /**
     * set a bit for the bitfield
     */
    public void setPiece(int index) {
        bitfield.setPiece(index);
    }

}
