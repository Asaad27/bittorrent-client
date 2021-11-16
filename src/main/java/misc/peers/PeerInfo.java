package misc.peers;
import java.net.InetAddress;

public class PeerInfo {
	
	private InetAddress addr;
	private int port;
	private byte[] bitfield;
	
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
	
}
