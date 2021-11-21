package misc;
import misc.peers.Message;
import misc.peers.PeerConnectionHandler;
import misc.peers.PeerMessage;
import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;
import misc.tracker.TrackerHandler;
import misc.utils.Utils;

import java.io.FileInputStream;
import java.net.URL;

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

			//LocalFileHandler localFile = new LocalFileHandler(torrentMetaData.getName(), torrentMetaData.getNumberOfPieces(), torrentMetaData.getPiece_length(), torrentMetaData.getPieces());
			

			
			//TrackerHandler tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1InfoByte(), localFile, PORT);

			//System.out.println("looking for peers");
			//List<PeerInfo> peerLst = tracker.getPeerLst();
			//System.out.println("peerlist received");
			PeerConnectionHandler peerConnectionHandler = new PeerConnectionHandler(PORT, SERVER);

			peerConnectionHandler.initLeecher(torrentMetaData);

			peerConnectionHandler.doHandShake(Utils.hexStringToByteArray(torrentMetaData.getSHA1Info()), Utils.hexStringToByteArray(PEERID));


			Message bitfield = new Message(PeerMessage.MsgType.BITFIELD, PeerConnectionHandler.clientBitfield);
			//var bitfield = new Message(PeerMessage.MsgType.BITFIELD, btfld);
			peerConnectionHandler.sendMessage(bitfield);

			Message interested = new Message(PeerMessage.MsgType.INTERESTED);
			peerConnectionHandler.sendMessage(interested);


			Message unchoke =  new Message(PeerMessage.MsgType.UNCHOKE);
			peerConnectionHandler.sendMessage(unchoke);

			/* divide the current piece into blocks */
			// if it's the last piece compute it's size
			int blockSize = PeerConnectionHandler.CHUNK_SIZE;
			int lastBlockSize = (torrentMetaData.getPieceLength() % blockSize != 0) ? torrentMetaData.getPieceLength()%blockSize : blockSize;
			int numOfBlocks =(torrentMetaData.getPieceLength() + blockSize - 1) / blockSize ;
			int pieceSize = torrentMetaData.getPieceLength();
			int lastPieceSize = (torrentMetaData.getLength() % pieceSize == 0) ?  pieceSize : (int) (torrentMetaData.getLength() % pieceSize);

			for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++)
			{
				/*if (i%5 == 0)
					Thread.sleep(5000);*/
				if (i == torrentMetaData.getNumberOfPieces()-1)
				{

					int lpoffset = 0, lpcountBlocks = 0;
					//nombre de block de 16KIB
					int numOfLastPieceBlocks = lastPieceSize/ blockSize;
					//le reste
					int remainingBlockSize = lastPieceSize%blockSize;
					for (int j = 0; j < numOfLastPieceBlocks; j++) {
						Message request =  new Message(PeerMessage.MsgType.REQUEST, i, lpoffset , blockSize);
						peerConnectionHandler.sendMessage(request);
						lpoffset += blockSize;
						lpcountBlocks++;

					}
					//we send the last block
					if (remainingBlockSize != 0)
					{
						Message request =  new Message(PeerMessage.MsgType.REQUEST, i, lpoffset , remainingBlockSize);
						peerConnectionHandler.sendMessage(request);
					}

				}
				else
				{
					int offset = 0, countBlocks = 0;
					while (offset < pieceSize)
					{
						//TODO ; last block size <-> blocksize
						Message request =  new Message(PeerMessage.MsgType.REQUEST, i, offset , blockSize);
						peerConnectionHandler.sendMessage(request);
						countBlocks++;
						offset += (countBlocks == numOfBlocks )? lastBlockSize : blockSize;
					}
				}

			}


			//var notInter = new Message(PeerMessage.MsgType.NOTINTERESTED);
			//peerConnectionHandler.sendMessage(notInter);

		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		
		

	}

}
