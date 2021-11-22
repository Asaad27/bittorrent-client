package misc.peers;

import java.net.InetAddress;

public class PeerInfo {

    private final InetAddress addr;
    private final int port;
    private byte[] bitfield;
    private PeerState peerState;

    public PeerInfo(InetAddress addr, int port) {
        this.addr = addr;
        this.port = port;
        this.bitfield = new byte[]{0};
    }

    public InetAddress getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public byte[] getBitfield() {
        return bitfield;
    }

    public void setBitfield(byte[] bitfield) {
        this.bitfield = bitfield;
    }

    @Override
    public String toString() {
        return "peer\nport : " + port + " addr : " + addr + "\n";
    }
}
