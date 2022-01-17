package misc.tracker;
import misc.peers.PeerInfo;
import misc.utils.Utils;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;

public class TrackerHandler {
	
	private static String PEER_ID_HEAD = "-PYA501-";
	public static final  String PEER_ID = genPeerId();
	
	private URL announceURL;
	private byte[] SHA1Info;
	private int port;
	private int totalPieces;
	
	public TrackerHandler(URL announceURL, byte[] SHA1Info, int port, int totalPieces) {
		
		this.announceURL = announceURL;
		this.SHA1Info = SHA1Info;
		this.port = port;
		this.totalPieces = totalPieces;
	}
	
	public String buildQueryURI() {
		
		Map<String,String> params = new HashMap<>();
		
		params.put("peer_id", PEER_ID);
		params.put("port", Integer.toString(port));
		params.put("compact", "1");
		
		StringBuilder queryParams = new StringBuilder();
		
		queryParams.append("?");
		try {
			queryParams.append(URLEncoder.encode("info_hash", "US-ASCII"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		queryParams.append("=");
		queryParams.append(Utils.byteArrayToURLString(SHA1Info));
		queryParams.append("&");
		
		try {
			
	        for (Map.Entry<String, String> entry : params.entrySet()) {
	          queryParams.append(URLEncoder.encode(entry.getKey(), "US-ASCII"));
	          queryParams.append("=");
	          queryParams.append(URLEncoder.encode(entry.getValue(), "US-ASCII"));
	          queryParams.append("&");
	        }
	        
	        queryParams.deleteCharAt(queryParams.length() - 1); // On supprimme le dernier "&"
	        
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		String queryURI = announceURL.toString().concat(queryParams.toString());
		
		return queryURI;
	}
	
	public Set<PeerInfo> getPeerList() throws IOException {
		
		URL uri = new URL(buildQueryURI());
		// System.out.println(uri.toString());
		
		HttpURLConnection conn =  (HttpURLConnection) uri.openConnection();
		conn.setRequestMethod("GET");
		
		int status = conn.getResponseCode(); // On exécute la requête
		
		if (status > 299) {
			
			// Erreur HTTP
			
			System.out.println("Erreur " + String.valueOf(status));
			conn.disconnect();
			return null;
			
		} else {
			
			
			BDecoder reader = new BDecoder(conn.getInputStream());
			Map<String, BEncodedValue> response = reader.decodeMap().getMap();
			
			byte[] peers = response.get("peers").getBytes();
			
			conn.disconnect();
			
			return processHttpResponse(peers);
			
		}
		
	}
	
	
	private Set<PeerInfo> processHttpResponse(byte[] response) {
		
		Set<PeerInfo> lst = new HashSet<>();
		
		if(response.length % 6 == 0) {
			
			int peer_nb = response.length / 6;
			
			for(int i = 0; i < peer_nb ; i++) {
				
				byte[] addr_byte = {response[6 * i], response[6 * i + 1], response[6 * i + 2], response[6 * i + 3]};
				byte[] port_byte = {response[6 * i + 4], response[6 * i + 5]};
				
				
				try {
					InetAddress addr = InetAddress.getByAddress(addr_byte);
					ByteBuffer wrapped = ByteBuffer.wrap(port_byte);
					int port = wrapped.getShort();
					

					/*System.out.println("Peer Info n°" + i);
					System.out.println("Addresse IPv4 : " + addr.toString());
					System.out.println("Port : " + port);*/

					
					PeerInfo peer = new PeerInfo(addr, port, totalPieces);
					if (port >= 0)
						lst.add(peer);
					
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				
				
			}
			
		}

		//System.out.println("peer list : " + lst);
		
		return lst;
		
	}
	
	public static String genPeerId() {

		// System.out.println("Getting local Peer ID");
		String peerId = new String();
		peerId = peerId.concat(PEER_ID_HEAD);
		Random r = new Random();
		String randomStr;
		int rand;

		while(peerId.length() < 20) {
			
			rand = r.nextInt(9); 
			randomStr = Integer.toString(rand);

			peerId = peerId.concat(randomStr);
		}
	
		return peerId;
	}


}
