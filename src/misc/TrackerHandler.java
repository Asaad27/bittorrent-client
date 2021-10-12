package misc;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.io.IOException;
import java.net.HttpURLConnection;

public class TrackerHandler {
	
	private URL announceURL;
	private String SHA1Info;
	private LocalFileHandler localFile;
	
	public TrackerHandler(URL announceURL, String SHA1Info, LocalFileHandler localFile) {
		
		this.announceURL = announceURL;
		this.SHA1Info = SHA1Info;
		this.localFile = localFile;
		
	}
	
	public List<InetAddress> getPeerLst() throws IOException {
		
		HttpURLConnection conn =  (HttpURLConnection) announceURL.openConnection();
		
		sendHttpRequestToTracker(conn);
		
		
		return null;
	}
	
	public void sendHttpRequestToTracker(HttpURLConnection conn) throws ProtocolException {
		
		// On définit la méthode de Requête
		
		conn.setRequestMethod("GET");
		
		// On construit le dictionnaire de paramètres
		
		Map<String, String> params = new HashMap<>();
		
		
		
	}
	
	private void processHttpResponse() {
		
	}
}
