package misc;
import misc.peers.Message;
import misc.peers.PeerConnectionHandler;
import misc.peers.PeerInfo;
import misc.peers.PeerMessage;
import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;
import misc.tracker.TrackerHandler;
import misc.torrent.LocalFileHandler;
import misc.utils.Utils;

import java.io.FileInputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class Main {

	public static void main(String[] args) {
		
		// Port d'écoute Bittorent
		
		int PORT = 40784;
		String SERVER = "10.245.226.251";

		String PEERID = "2d415a353737302d63724b546962774f6b6c4261";
		// On initialise le TorrentFileHandler à partir du fichier Torrent d'entrée

		
		try {
			
			TorrentFileHandler torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
			TorrentMetaData torrentMetaData = torrentHandler.ParseTorrent();

			System.out.println(torrentMetaData.toString());
			// TODO : check URL protocol, only use TCP
			URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());

			LocalFileHandler localFile = new LocalFileHandler(torrentMetaData.getName());
			
			// TODO : Local File check : Vérifier que le fichier n'est pas déjà téléchargé pour calculer les params de la requête HTTP
			
			//TrackerHandler tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1Info(), localFile, PORT);

			//System.out.println("looking for peers");
			//List<PeerInfo> peerLst = tracker.getPeerLst();

			/* for testing */
			PeerConnectionHandler peerConnectionHandler = new PeerConnectionHandler(PORT, SERVER);

			peerConnectionHandler.doHandShake(Utils.hexStringToByteArray(torrentMetaData.getSHA1Info()), Utils.hexStringToByteArray(PEERID));

			peerConnectionHandler.initLeecher(torrentMetaData);


			//Message rcvMsg = peerConnectionHandler.receiveMessage();
			//System.out.println("recvd " + rcvMsg.ID);

			peerConnectionHandler.sendMessage(new Message(PeerMessage.MsgType.BITFIELD, PeerConnectionHandler.clientBitfield));


			var unchoke =  new Message(PeerMessage.MsgType.UNCHOKE);
			peerConnectionHandler.sendMessage(unchoke);








			/*ByteBuffer byteBuffer = ByteBuffer.allocate(9);
			byteBuffer.putInt(1);
			byteBuffer.put((byte)3);
			byteBuffer.putInt(4);
			byte[] bytes = byteBuffer.array();

			ByteBuffer bfr = ByteBuffer.wrap(bytes);
			System.out.println(bfr.getInt());
			System.out.println(bfr.get());
			System.out.println(bfr.getInt());*/

	/*		int PIECESIZE = 32*1024;
			int numPieces = (int) (torrentMetaData.getLength()/PIECESIZE + 1);
			int bfldSize = numPieces / 8 + 1;

			System.out.println(numPieces);
			System.out.println(bfldSize);*/


		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		
		

	}

}
