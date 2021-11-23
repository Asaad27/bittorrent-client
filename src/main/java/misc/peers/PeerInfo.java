package misc.peers;

import java.net.InetAddress;
import java.util.BitSet;

import misc.torrent.Bitfield;

public class PeerInfo {

    private final InetAddress addr;
    private final int port;
    private Bitfield bitfield;
    private PeerState peerState;

    public PeerInfo(InetAddress addr, int port, int totalPieces) {
        this.addr = addr;
        this.port = port;
        this.bitfield = new Bitfield(totalPieces);
    }

    public InetAddress getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public BitSet getBitfield() {
        return bitfield.getValue();
    }
    
    public void setBitfield(byte[] bitfield) {
    	this.bitfield.setValue(bitfield);
    }
    
    public void setBitfieldByPiece(int pieceNb, boolean value) {
    	this.bitfield.set(pieceNb, value);
    }

    @Override
    public String toString() {
        return "peer\nport : " + port + " addr : " + addr + "\n";
    }
}
