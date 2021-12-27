package test;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import misc.messages.Bitfield;
import misc.messages.ByteBitfield;
import misc.torrent.*;
import org.junit.Test;

import misc.utils.Utils;

public class BitfieldTest {
	byte[] clientBitfield;
	Bitfield bitsetBitfield;
	ByteBitfield byteBitfield;

	public void setPiece(int index) {
		if (clientBitfield == null) {
			return;
		}
		int byteIndex = index / 8;
		int offset = index % 8;
		clientBitfield[byteIndex] |= 1 << (7 - offset);
		bitsetBitfield.set(index, true);
		byteBitfield.setPiece(index);
	}

	@Test
	public void localBitfield() throws IOException, NoSuchAlgorithmException {
		TorrentFileHandler torrent = new TorrentFileHandler(new FileInputStream("C:\\Users\\asaad_6stn3w\\Desktop\\torrents\\img.torrent"));
		TorrentMetaData metaData = torrent.ParseTorrent();
		System.out.println(metaData);

		int blockSize = 16384;
		int lastBlockSize = (metaData.getPieceLength() % blockSize != 0) ? metaData.getPieceLength() % blockSize : blockSize;
		int numOfBlocks = (metaData.getPieceLength() + blockSize - 1) / blockSize;
		int pieceSize = metaData.getPieceLength();
		int lastPieceSize = (metaData.getLength() % pieceSize == 0) ? pieceSize : (int) (metaData.getLength() % pieceSize);
		int numOfLastPieceBlocks = (lastPieceSize + blockSize -1) / blockSize;
		int remainingBlockSize = lastPieceSize % blockSize;
		pieceSize = metaData.getPieceLength();
		int numPieces = metaData.getNumberOfPieces();

		int bfldSize = (numPieces + 7) / 8 ;

		clientBitfield= new byte[bfldSize];
		bitsetBitfield = new Bitfield(numPieces);
		byteBitfield = new ByteBitfield(numPieces);

		for (int i = 0; i < numPieces; i++)
			setPiece(i);
		System.out.println(Utils.bytesToHex(bitsetBitfield.toByteArray()));
		System.out.println(Utils.bytesToHex(clientBitfield));
		System.out.println(Utils.bytesToHex(byteBitfield.value));

	}

}
