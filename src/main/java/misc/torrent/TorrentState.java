package misc.torrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import misc.peers.ClientState;

public class TorrentState {

    private static TorrentState instance = null;

    public LocalFileHandler localFileHandler;
    public final ConcurrentMap<Integer, AtomicInteger> pieceDownloadedBlocks = new ConcurrentHashMap<>();
    private final int CHUNK_SIZE = 16384;
    private int numberOfPieces = 0;
    private final TorrentMetaData torrentMetaData;
    private final int blockSize = CHUNK_SIZE;

    private final int[] pieceCount;

    private int lastBlockSize;
    private int numOfBlocks;
    private int pieceSize;
    private int lastPieceSize;
    //nombre de block de 16KIB
    private int numOfLastPieceBlocks;
    //le reste
    private int remainingBlockSize;

    private int downloadedSize = 0;

    public List<PieceStatus> status;
    public ClientState clientState;
    public boolean fileCheckedSuccess = false;

    private TorrentState(TorrentMetaData torrentMetaData, ClientState clientState) {
        this.torrentMetaData = torrentMetaData;
        initPiecesAndBlocks();
        this.clientState = clientState;
        localFileHandler = new LocalFileHandler(torrentMetaData, clientState.bitfield, this);
        this.status = initStatus();
        this.pieceCount = new int[torrentMetaData.getNumberOfPieces()];

        initDownloadedSize();

    }

    private void initDownloadedSize() {
        for (int i = 0; i < torrentMetaData.getNumberOfPieces() - 1; i++){
            downloadedSize += clientState.hasPiece(i) ? getPieceSize() : 0;
        }
        downloadedSize += clientState.hasPiece(torrentMetaData.getNumberOfPieces()-1) ? getLastPieceSize() : 0;
    }

    public static TorrentState getInstance(TorrentMetaData torrentMetaData, ClientState clientState) {
        if (instance == null){
            instance = new TorrentState(torrentMetaData, clientState);
        }

        return instance;
    }

    private void initPiecesAndBlocks()
    {
        pieceSize = torrentMetaData.getPieceLength();

        lastBlockSize = (pieceSize % blockSize != 0) ? pieceSize % blockSize : blockSize;
        numOfBlocks = (pieceSize + blockSize - 1) / blockSize;
        lastPieceSize = (torrentMetaData.getLength() % pieceSize == 0) ? pieceSize : (int) (torrentMetaData.getLength() % pieceSize);
        numOfLastPieceBlocks = (lastPieceSize + blockSize -1) / blockSize;

        remainingBlockSize =( lastPieceSize % blockSize != 0) ? lastPieceSize%blockSize : blockSize;

        numberOfPieces = torrentMetaData.getNumberOfPieces();
    }

    public int getLastBlockSize() {
        return lastBlockSize;
    }

    public int getNumOfBlocks() {
        return numOfBlocks;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public int getNumOfLastPieceBlocks() {
        return numOfLastPieceBlocks;
    }

    public int getRemainingBlockSize() {
        return remainingBlockSize;
    }

    public int getNumberOfPieces() {
        return numberOfPieces;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int[] getPieceCount() {
        return pieceCount;
    }

    public int getDownloadedSize() {
        return downloadedSize;
    }

    public int getLastPieceSize() {
        return lastPieceSize;
    }

    public void setDownloadedSize(int downloadedSize) {
        this.downloadedSize = downloadedSize;
    }
    
    private List<PieceStatus> initStatus() {

		List<PieceStatus> status = new ArrayList<PieceStatus>();

		for(int i = 0; i < numberOfPieces; i++) {
			if(clientState.bitfield.hasPiece(i)) {
				status.add(PieceStatus.Verified);
			} else {
				status.add(PieceStatus.ToBeDownloaded);
			}
		}

		return status;
	}
	
	public List<PieceStatus> getStatus() {
		return this.status;
	}

}
