package com.peers;

import com.messages.Message;

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
        super.lastResponseTime = System.currentTimeMillis();
    }

    public void updateTime(){
        super.lastResponseTime = System.currentTimeMillis();
    }

    public Long nonResponseTime(){
        return System.currentTimeMillis() - super.lastResponseTime;
    }

    public boolean isConnected(){
        return ((sentBitfield || interested || !weAreChokedByPeer) && welcomeQ.isEmpty());
    }

}
