package misc.peers;

import java.net.InetAddress;

public class PeerInfo {

    private final InetAddress addr;
    public int port;
    //private Bitfield bitfield;  //TODO : fix
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

    public InetAddress getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

   /*public BitSet getBitfield() {
        return bitfield.getValue();
    }*/
    
    /*public void setBitfield(byte[] bitfield) {
    	this.bitfield.setValue(bitfield);
    }
    
    public void setBitfieldByPiece(int pieceNb, boolean value) {
    	this.bitfield.set(pieceNb, value);
    }*/

    @Override
    public String toString() {
        return "peer\nport : " + port + " addr : " + addr + "\n";
    }
}
