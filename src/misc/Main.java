package misc;
import java.io.FileInputStream;
import java.net.URL;
import java.net.InetAddress;
import java.util.List;

public class Main {

	public static void main(String[] args) {
		
		// Port d'écoute Bittorent
		
		int PORT = 6881;
		
		// On initialise le TorrentFileHandler à partir du fichier Torrent d'entrée
		
		try {
			
			TorrentFileHandler torrentFile = new TorrentFileHandler(new FileInputStream(args[0]));	
			
			URL announceURL = new URL(torrentFile.getAnnounceURL());
			
			LocalFileHandler localFile = new LocalFileHandler(torrentFile.getFilename());
			
			// TODO : Local File check : Vérifier que le fichier n'est pas déjà téléchargé pour calculer les params de la requête HTTP
			
			TrackerHandler tracker = new TrackerHandler(announceURL, torrentFile.getSHA1Info(), localFile);
			
			List<InetAddress> peerLst = tracker.getPeerLst();
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		
		

	}

}
