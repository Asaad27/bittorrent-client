package misc;
import misc.peers.PeerInfo;
import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;
import misc.tracker.TrackerHandler;
import misc.utils.LocalFileHandler;

import java.io.FileInputStream;
import java.net.URL;
import java.util.List;

public class Main {

	public static void main(String[] args) {
		
		// Port d'écoute Bittorent
		
		int PORT = 6881;
		
		// On initialise le TorrentFileHandler à partir du fichier Torrent d'entrée
		
		try {
			
			TorrentFileHandler torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
			TorrentMetaData torrentMetaData = torrentHandler.ParseTorrent();


			//URL announceURL = new URL(torrentFile.getAnnounceURL());
			URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());

			LocalFileHandler localFile = new LocalFileHandler(torrentMetaData.getName());
			
			// TODO : Local File check : Vérifier que le fichier n'est pas déjà téléchargé pour calculer les params de la requête HTTP
			
			TrackerHandler tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1Info(), localFile, PORT);
			
			List<PeerInfo> peerLst = tracker.getPeerLst();
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		
		

	}

}
