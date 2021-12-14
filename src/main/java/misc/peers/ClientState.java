package misc.peers;

import misc.messages.Message;
import misc.torrent.ByteBitfield;
import misc.tracker.TrackerHandler;

import java.util.LinkedList;
import java.util.Queue;

public class ClientState extends State{


    public ClientState(int numPieces) {
        super(numPieces);
    }


    public byte[] getBitfield(){
        return this.bitfield.value;
    }
}
