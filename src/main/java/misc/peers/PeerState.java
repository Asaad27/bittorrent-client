package misc.peers;

import misc.messages.Message;

import java.util.LinkedList;
import java.util.Queue;

/**
 * state of a peer
 *
 * @author Asaad
 */

public class PeerState extends State{
    public Queue<Message> writeMessageQ= new LinkedList<>();

    public PeerState(int numPieces) {
        super(numPieces);
    }
}
