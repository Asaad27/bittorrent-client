package misc.torrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TorrentState {

    public LocalFileHandler localFileHandler;
    public final ConcurrentMap<Integer, AtomicInteger> pieceDownloadedBlocks = new ConcurrentHashMap<>();
    public final int CHUNK_SIZE = 16384;
    public int numPieces = 0;
    TorrentMetaData torrentMetaData;
    public int blockSize = CHUNK_SIZE;
    private int lastBlockSize;
    private int numOfBlocks;
    private int pieceSize;
    private int lastPieceSize;
    //nombre de block de 16KIB
    private int numOfLastPieceBlocks;
    //le reste
    private int remainingBlockSize;

    private int downloadedSize = 0;

    public TorrentState(TorrentMetaData torrentMetaData) {
        this.torrentMetaData = torrentMetaData;
        initPiecesAndBlocks();
    }

    private void initPiecesAndBlocks()
    {
        lastBlockSize = (torrentMetaData.getPieceLength() % blockSize != 0) ? torrentMetaData.getPieceLength() % blockSize : blockSize;
        numOfBlocks = (torrentMetaData.getPieceLength() + blockSize - 1) / blockSize;
        pieceSize = torrentMetaData.getPieceLength();
        lastPieceSize = (torrentMetaData.getLength() % pieceSize == 0) ? pieceSize : (int) (torrentMetaData.getLength() % pieceSize);
        numOfLastPieceBlocks = (lastPieceSize + blockSize -1) / blockSize;
        remainingBlockSize = lastPieceSize % blockSize;
        pieceSize = torrentMetaData.getPieceLength();
        numPieces = torrentMetaData.getNumberOfPieces();
    }

    public int getLastBlockSize() {
        return lastBlockSize;
    }

    public TorrentState setLastBlockSize(int lastBlockSize) {
        this.lastBlockSize = lastBlockSize;
        return this;
    }

    public int getNumOfBlocks() {
        return numOfBlocks;
    }

    public TorrentState setNumOfBlocks(int numOfBlocks) {
        this.numOfBlocks = numOfBlocks;
        return this;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public TorrentState setPieceSize(int pieceSize) {
        this.pieceSize = pieceSize;
        return this;
    }

    public int getLastPieceSize() {
        return lastPieceSize;
    }

    public TorrentState setLastPieceSize(int lastPieceSize) {
        this.lastPieceSize = lastPieceSize;
        return this;
    }

    public int getNumOfLastPieceBlocks() {
        return numOfLastPieceBlocks;
    }

    public TorrentState setNumOfLastPieceBlocks(int numOfLastPieceBlocks) {
        this.numOfLastPieceBlocks = numOfLastPieceBlocks;
        return this;
    }

    public int getRemainingBlockSize() {
        return remainingBlockSize;
    }

    public TorrentState setRemainingBlockSize(int remainingBlockSize) {
        this.remainingBlockSize = remainingBlockSize;
        return this;
    }

    public int getDownloadedSize() {
        return downloadedSize;
    }

    public TorrentState setDownloadedSize(int downloadedSize) {
        this.downloadedSize = downloadedSize;
        return this;
    }
}
