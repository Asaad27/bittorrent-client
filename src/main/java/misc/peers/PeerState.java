package misc.peers;

import misc.messages.Message;

import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;



public class PeerState extends State {
	public LinkedHashSet<Integer> piecesToRequest;
	
    public Deque<Message> writeMessageQ= new LinkedList<>();

    public PeerState(int numPieces) {
        super(numPieces);
        piecesToRequest = new LinkedHashSet<>();
    }
    
    public void addPieceToRequest(int n) {
    	piecesToRequest.add(n);
    }
    
    public void removePieceToRequest(int n) {
    	if(piecesToRequest.contains(n)) {
    		piecesToRequest.remove(n);
    	}
    }
}
