package misc.torrent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import misc.messages.ByteBitfield;
import misc.peers.ClientState;
import misc.utils.DEBUG;
import misc.utils.Utils;

import static misc.download.TCPClient.torrentMetaData;

public class LocalFileHandler {

	private final File tempFile;
	private RandomAccessFile fileAccess;
	private final ByteBitfield bitfield;
	private final ClientState clientState;


	private final TorrentState torrentState;
	
	public LocalFileHandler(TorrentState torrentState, ClientState clientState) {

		this.torrentState = torrentState;
		this.clientState = clientState;
		this.bitfield = clientState.bitfield;

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
		DEBUG.logf("generating BITFIELD");
		boolean isSeeder = true;
		if(tempFile.exists()) {
			for(int i = 0; i < torrentMetaData.getNumberOfPieces() ; i++) {
				boolean verified = verifyPiece(i);
				setBitfield(i, verified);
				isSeeder = isSeeder && verified;
			}
		}
		DEBUG.logf("BITFIELD generated : " + Utils.bytesToHex(bitfield.value));

		if (isSeeder){
			clientState.isSeeder = true;
			clientState.isDownloading = false;
		}

	}

	public  boolean verifyPiece(int index){
		Piece pieceToVerify = torrentState.pieces.get(index);
		int pieceSize = pieceToVerify.getPieceSize();
		byte[] piece = new byte[pieceSize];

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
				DEBUG.logf("piece id : " + index + " not downloaded");
				return false;
			}

		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}


		DEBUG.logf("piece id : " + index + " valid, downloaded");

		return true;
	}


	public  boolean verifyDownloadedFile(){
		System.out.println("Checking File");
		int numPieces = torrentMetaData.getNumberOfPieces();
		for (int i = 0; i < numPieces; i++)
		{
			Piece piece = torrentState.pieces.get(i);
			if(!verifyPiece(i)) {
				setBitfield(i, false);
				long downloadedSize = torrentState.getDownloadedSize();

				torrentState.setDownloadedSize(downloadedSize - (piece.getPieceSize()));
				piece.setPieceStatus(PieceStatus.ToBeDownloaded);


				return false;
			}
		}

		System.out.println("FILE CHECK SUCCESS 100%");
		torrentState.fileCheckedSuccess = true;
		return true;
	}

	public void setBitfield(int pieceNb, boolean value) {
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
			fileAccess.seek(((long) index * torrentMetaData.getPieceLength() + begin));
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
			fileAccess.seek((long) (index) * torrentMetaData.getPieceLength() +begin);
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
