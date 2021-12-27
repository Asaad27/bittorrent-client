package misc.torrent;

import misc.peers.PeerState;

import java.util.LinkedList;
import java.util.List;

public class Observer {

    public List<IObservable> observables = new LinkedList<>();

    public void attach(IObservable instance) {
        observables.add(instance);
    }

    public void notifyAllObservers(Events event, PeerState peerState){
        switch (event){
            case PEER_CONNECTED:
                for (IObservable observable: observables) {
                    observable.peerConnection(peerState);
                }
                break;
            case PEER_DISCONNECTED:
                for (IObservable observable: observables) {
                    observable.peerDisconnection(peerState);
                }
                break;
        }
    }

    public void notifyAllObservers(int index){
        for (IObservable observable: observables) {
            observable.peerHasPiece(index);
        }
    }

}
