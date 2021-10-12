package misc;
import java.net.InetAddress;

public class PeerInfo {
	
	private InetAddress addr;
	private int port;
	
	public PeerInfo(InetAddress addr, int port) {
		this.addr = addr;
		this.port = port;
	}

	public InetAddress getAddr() {
		return addr;
	}

	public int getPort() {
		return port;
	}
	
}
