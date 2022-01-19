package misc.peers;
import misc.messages.ByteBitfield;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class State {
    public boolean choked = true;
    public boolean interested = false;
    public boolean weAreInterested = false;
    public boolean receivedHandshake = false;
    public boolean sentHandshake = false;
    public boolean sentBitfield = false;
    public boolean receivedBitfield = false;
    public boolean weAreChokedByPeer = true;




    public ByteBitfield bitfield = null;
    public int numberOfRequests = 0;
    public AtomicInteger queuedRequestsFromClient = new AtomicInteger(0);
    public AtomicInteger queuedRequestsFromPeer = new AtomicInteger(0);
    public int numberOfBlocksSent = 0;
    public int numberOfRequestsReceived = 0;
    public int numberOfBlocksReceived = 0;




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
