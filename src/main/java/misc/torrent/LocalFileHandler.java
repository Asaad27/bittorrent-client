package misc.torrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import misc.messages.Bitfield;
import misc.messages.ByteBitfield;
import misc.messages.Message;
import misc.messages.PeerMessage;
import misc.peers.ClientState;
import misc.utils.DEBUG;
import misc.utils.Utils;

public class LocalFileHandler {

	private final File tempFile;
	private RandomAccessFile fileAccess;
	private final ByteBitfield bitfield;

	private final TorrentMetaData torrentMetaData;
	private final TorrentState torrentState;
	
	public LocalFileHandler(TorrentMetaData torrentMetaData, ByteBitfield bitfield, TorrentState torrentState) {

		this.torrentState = torrentState;
		this.torrentMetaData = torrentMetaData;
		this.bitfield = bitfield;

		tempFile = new File(torrentMetaData.getName());
		long fileLength = torrentMetaData.getLength();

		try {
			fileAccess = new RandomAccessFile(torrentMetaData.getName(), "rw");
			fileAccess.setLength((int) fileLength);
		} catch (IOException e) {
			DEBUG.printError(e, getClass().getName());
		}
		initBitfield();
	}
	
	public void initBitfield() {
		if(tempFile.exists()) {
			for(int i = 0; i < torrentMetaData.getNumberOfPieces() ; i++) {
				setPieceStatus(i, verifyPiece(i));
			}
		}

	}

	public  boolean verifyPiece(int index){
		int numPieces = torrentState.getNumberOfPieces();
		int lastPieceSize = torrentState.getLastPieceSize();
		int pieceSize = torrentState.getPieceSize();
		byte[] piece;
		if (index == numPieces-1)
			piece = new byte[lastPieceSize];
		else
			piece = new byte[pieceSize];
		try {
			fileAccess.seek((long) index * pieceSize);
			fileAccess.read(piece);

			MessageDigest digest=MessageDigest.getInstance("SHA-1");
			digest.update(piece);
			byte[] sha = digest.digest();
			String originalHash = torrentMetaData.getPiecesList().get(index);
			String downloadedHash = Utils.bytesToHex(sha);
			if (!downloadedHash.equals(originalHash))
			{
				System.err.println("piece id : " + index + "\noriginal hash : " + originalHash + "\ndownloaded hash : " + downloadedHash);
				return false;
			}

		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return true;
	}

	public  boolean verifyDownloadedFile(){
		int numPieces = torrentState.getNumberOfPieces();
		for (int i = 0; i < numPieces; i++)
		{
			if(!verifyPiece(i)) {
				//TODO : REDOWNLOAD AND NOTIFY
				setPieceStatus(i, false);
				return false;
			}
		}

		return true;
	}

	public void setPieceStatus(int pieceNb, boolean value) {
		if (value)
			bitfield.setPiece(pieceNb);
		else{
			if (bitfield.hasPiece(pieceNb))
				bitfield.unsetPiece(pieceNb);
		}
	}
	
	public ByteBitfield getBitfield() {
		return this.bitfield;
	}

	public boolean writePieceBlock(int index, int begin, byte[] payload){
		try {
			fileAccess.seek(((long) index * torrentState.getPieceSize() + begin));
			fileAccess.write(payload);
		} catch (IOException e) {
			DEBUG.printError(e, "write piece block");
			return false;
		}

		return true;
	}

	public byte[] readPieceBlock(int size, int index, int begin) {

		byte[] ans = new byte[size];
		try {
			fileAccess.seek((long) (index) * torrentState.getPieceSize() +begin);
			fileAccess.read(ans);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ans;
	}

	
	public void close() {
		try {
			fileAccess.close();
		} catch (IOException e){ e.printStackTrace(); }
	}
	
}
