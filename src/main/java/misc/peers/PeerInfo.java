package misc.peers;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Objects;

public class PeerInfo {

    public final InetAddress addr;
    public int port;
    private PeerState peerState;

    public PeerInfo(InetAddress addr, int port, int totalPieces) {
        this.addr = addr;
        this.port = port;
        this.peerState = new PeerState(totalPieces);
    }

    public PeerInfo(InetAddress addr, int port) {
        this.addr = addr;
        this.port = port;
    }

    public PeerState getPeerState() {
        return peerState;
    }

    public int getPort() {
        return port;
    }


    @Override
    public String toString() {
        return "(port : " + port + " addr : " + addr + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        final PeerInfo peerInfo = (PeerInfo) obj;

        return this.port == peerInfo.getPort();

    }


}
