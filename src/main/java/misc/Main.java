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
		int PORT = 59407;

		String SERVER = "127.0.0.1";


		String PEERID = TrackerHandler.genPeerId();
		// On initialise le TorrentFileHandler à partir du fichier Torrent d'entrée

		
		try {
			
			TorrentFileHandler torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
			TorrentMetaData torrentMetaData = torrentHandler.ParseTorrent();

			System.out.println(torrentMetaData.toString());
			// TODO : check URL protocol, only use TCP
			URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());

			LocalFileHandler localFile = new LocalFileHandler(torrentMetaData.getName());
			

			
			//TrackerHandler tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1InfoByte(), localFile, PORT);

			//System.out.println("looking for peers");
			//List<PeerInfo> peerLst = tracker.getPeerLst();
			//System.out.println("peerlist received");
			PeerConnectionHandler peerConnectionHandler = new PeerConnectionHandler(PORT, SERVER);

			peerConnectionHandler.doHandShake(Utils.hexStringToByteArray(torrentMetaData.getSHA1Info()), Utils.hexStringToByteArray(PEERID));

			peerConnectionHandler.initLeecher(torrentMetaData);


			//var bitfield = new Message(PeerMessage.MsgType.BITFIELD, PeerConnectionHandler.clientBitfield);
			//peerConnectionHandler.sendMessage(bitfield);

			//var unchoke =  new Message(PeerMessage.MsgType.UNCHOKE);
			//peerConnectionHandler.sendMessage(unchoke);

			//Thread.sleep(5000);
			var interested = new Message(PeerMessage.MsgType.INTERESTED);
			peerConnectionHandler.sendMessage(interested);

			/*var rcvd = peerConnectionHandler.receiveMessage();
			System.out.println("recieved : " + rcvd.ID);

			var rcvd2 = peerConnectionHandler.receiveMessage();
			System.out.println("recieved : " + rcvd2.ID);*/

			var request =  new Message(PeerMessage.MsgType.REQUEST, 0, 0, torrentMetaData.getPiece_length());
			peerConnectionHandler.sendMessage(request);

			var unchoke =  new Message(PeerMessage.MsgType.UNCHOKE);
			peerConnectionHandler.sendMessage(unchoke);

			/*ByteBuffer buf = ByteBuffer.allocate(8); // two 4-byte integers
			buf.put((byte) 1).putInt( 2);
			buf.rewind();
			peerConnectionHandler.sendMessage(buf);*/

		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		
		

	}

}
