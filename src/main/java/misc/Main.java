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
			URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());

			LocalFileHandler localFile = new LocalFileHandler(torrentMetaData.getName(), torrentMetaData.getNumberOfPieces(), torrentMetaData.getPiece_length(), torrentMetaData.getPieces());
			

			
			//TrackerHandler tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1InfoByte(), localFile, PORT);

			//System.out.println("looking for peers");
			//List<PeerInfo> peerLst = tracker.getPeerLst();
			//System.out.println("peerlist received");
			PeerConnectionHandler peerConnectionHandler = new PeerConnectionHandler(PORT, SERVER);

			peerConnectionHandler.initLeecher(torrentMetaData);

			peerConnectionHandler.doHandShake(Utils.hexStringToByteArray(torrentMetaData.getSHA1Info()), Utils.hexStringToByteArray(PEERID));


			//TODO : fix bitfield message, maybe use a bitset
			int size_bitfield = (int) Math.ceil( torrentMetaData.getNumberOfPieces() / 8 ) + 1;

			byte[] btfld = new byte[size_bitfield];
			for (int i = 0; i < size_bitfield; i++)
				btfld[i] = 0;
			//var bitfield = new Message(PeerMessage.MsgType.BITFIELD, PeerConnectionHandler.clientBitfield);
			//var bitfield = new Message(PeerMessage.MsgType.BITFIELD, btfld);
			//peerConnectionHandler.sendMessage(bitfield);

			var interested = new Message(PeerMessage.MsgType.INTERESTED);
			peerConnectionHandler.sendMessage(interested);


			var unchoke =  new Message(PeerMessage.MsgType.UNCHOKE);
			peerConnectionHandler.sendMessage(unchoke);

			var request =  new Message(PeerMessage.MsgType.REQUEST, 1, 1, torrentMetaData.getPiece_length()/2);
			peerConnectionHandler.sendMessage(request);

			//var notInter = new Message(PeerMessage.MsgType.NOTINTERESTED);
			//peerConnectionHandler.sendMessage(notInter);

		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		
		

	}

}
