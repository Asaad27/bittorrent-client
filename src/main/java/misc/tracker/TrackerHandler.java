package misc.tracker;
import misc.peers.PeerInfo;
import misc.torrent.LocalFileHandler;
import misc.utils.Utils;

import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.net.ProtocolException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

public class TrackerHandler {
	
	private static String PEER_ID_HEAD = "-PYA501-";
	private String PEER_ID;
	
	private URL announceURL;
	private byte[] SHA1Info;
	private LocalFileHandler localFile;
	private int port;
	
	public TrackerHandler(URL announceURL, byte[] SHA1Info, LocalFileHandler localFile, int port) {
		
		this.announceURL = announceURL;
		this.SHA1Info = SHA1Info;
		this.localFile = localFile;
		this.port = port;
		this.PEER_ID = genPeerId();
	}
	
	public String buildQueryURI() {
		
		Map<String,String> params = new HashMap<>();
		
		params.put("peer_id", PEER_ID);
		params.put("port", Integer.toString(port));
		params.put("compact", "1");
		/*
		params.put("uploaded", localFile.getUploaded());
		params.put("downloaded", localFile.getDownloaded());
		params.put("left", localFile.getLeft());
		*/
		
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
	
	public List<PeerInfo> getPeerLst() throws IOException {
		
		URL uri = new URL(buildQueryURI());
		
		HttpURLConnection conn =  (HttpURLConnection) uri.openConnection();
		conn.setRequestMethod("GET");
		
		Reader streamReader;
		
		int status = conn.getResponseCode();
		
		if (status > 299) {
			
			streamReader = new InputStreamReader(conn.getErrorStream());
			
		} else {
			
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuffer content = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
			    content.append(inputLine);
			}
			in.close();
			
			System.out.println(content);
		}
	
		conn.disconnect();
		
		return null;
	}
	
	
	private void processHttpResponse(String response) {
		
		// TODO : Remplir la liste de peers à partir de la réponse HTTP
		
	}
	
	public String genPeerId() {

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
