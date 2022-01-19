package misc.peers;

import misc.messages.Message;

import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;


public class PeerState extends State {
	public LinkedHashSet<Integer> piecesToRequest;
	
    public Deque<Message> writeMessageQ= new LinkedList<>();
    public Queue<Message> welcomeQ = new LinkedList<>();



    public PeerState(int numPieces) {
        super(numPieces);
        piecesToRequest = new LinkedHashSet<>();
    }

    public boolean isConnected(){
        return ((sentBitfield || interested || !weAreChokedByPeer) && welcomeQ.isEmpty());
    }

}
