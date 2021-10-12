package misc;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;

public class TrackerHandler {
	
	private final static String PEER_ID_HEAD = "-PYA501-";
	private final String PEER_ID;
	
	private URL announceURL;
	private String SHA1Info;
	private LocalFileHandler localFile;
	private int port;
	
	public TrackerHandler(URL announceURL, String SHA1Info, LocalFileHandler localFile, int port) {
		
		this.announceURL = announceURL;
		this.SHA1Info = SHA1Info;
		this.localFile = localFile;
		this.port = port;
		this.PEER_ID = genPeerId();
	}
	
	public List<PeerInfo> getPeerLst() throws IOException {
		
		HttpURLConnection conn =  (HttpURLConnection) announceURL.openConnection();
		
		sendHttpRequestToTracker(conn);
		
		
		return null;
	}
	
	public void sendHttpRequestToTracker(HttpURLConnection conn) throws ProtocolException, IOException {
		
		// On définit la méthode de Requête
		
		conn.setRequestMethod("GET");
		
		// On construit le dictionnaire de paramètres
		
		Map<String, String> params = new HashMap<>();
		params.put("info_hash", SHA1Info);
		params.put("peer_id", PEER_ID);
		params.put("port", Integer.toString(port));
		
		/*
		params.put("uploaded", localFile.getUploaded());
		params.put("downloaded", localFile.getDownloaded());
		params.put("left", localFile.getLeft());
		*/
		
		params.put("compact", "1");
		
		// TODO : Envoyer la requête HTTP
		
		
		Reader streamReader = null;
		
		int status = conn.getResponseCode();
		
		if (status > 299) {
			
			streamReader = new InputStreamReader(conn.getErrorStream());
			
		} else {
			
			streamReader = new InputStreamReader(conn.getInputStream());
			
		}
	}
	
	private void processHttpResponse() {
		
		Map<String, String> response = new HashMap<>();
		// TODO : Remplir la HashMap avec les infos de la réponse HTTP
		
		if (response.containsKey("failure reason")) {
			System.out.println(response.get("failure reason"));
			// Erreur
		} else {
			
			if (response.containsKey("warning message")) {
				System.out.println(response.get("warning message"));
			}
			
			/* Info in the response
			 * failure reason (optional)
 			 * warning message (optional)
			 * interval (int in seconds, interval that the client should wait)
			 * min interval (int in seconds minimum announce interval)
			 * tracker id (string)
			 * complete (numbers of peers with the entire file (seeders))
			 * incomplete (numbers of peers with incomplete file (leechers))
			 * peers : list of (up to 50) 6-bytes values, first 4 bytes : IP, last 2 bytes : port number (compact version)
			 */
		}
	}
	
	public String genPeerId() {
		
		String peer_id = PEER_ID_HEAD;
		Random r = new Random();
		String randomStr;
		int rand;
		
		while(peer_id.length() < 20) {
			
			rand = r.nextInt(9); 
			randomStr = Integer.toString(rand);

			peer_id.concat(randomStr);
		}
		
		return peer_id;
	}
}
