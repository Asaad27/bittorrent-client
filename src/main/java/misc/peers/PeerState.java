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

    public int waitingRequests = 0;
    public int requestReceivedFromPeer = 0;

    public PeerState(int numPieces) {
        super(numPieces);
        piecesToRequest = new LinkedHashSet<>();
    }
    
    public void addPieceToRequest(int n) {
    	piecesToRequest.add(n);
    }

    public boolean isConnected(){
        return !weAreChokedByPeer && welcomeQ.isEmpty();
    }

    public void removePieceToRequest(int n) {
    	if(piecesToRequest.contains(n)) {
    		piecesToRequest.remove(n);
    	}
    }
}
