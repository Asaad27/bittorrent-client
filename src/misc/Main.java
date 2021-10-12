package misc;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Main {

	public static void main(String[] args) {
		
		// On initialise le TorrentFileHandler à partir du fichier Torrent d'entrée
		
		try {
			
			TorrentFileHandler torrentFile = new TorrentFileHandler(new FileInputStream(args[0]));	
			
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
			
		}
		
		

	}

}
