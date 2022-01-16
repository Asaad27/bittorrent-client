package misc.torrent;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static misc.download.TCPClient.torrentMetaData;

public class Piece {
    private final int BLOCK_SIZE = 16384;
    private PieceStatus status;
    private List<BlockStatus> blocks;
    private final int index;
    private int size;
    private int numberOfBlocks;
    private int lastBlockSize;
    private int numberOfPeerOwners;

    private final int torrentPieceSize = torrentMetaData.getPieceLength();
    private final int torrentNumberOfPieces = torrentMetaData.getNumberOfPieces();

    public Piece(int index) {
        this.index = index;
        initData();
    }

    public void initData(){
        numberOfPeerOwners = 0;
        if (index == torrentNumberOfPieces - 1){
            size = (torrentMetaData.getLength() % torrentPieceSize == 0) ? torrentPieceSize : (int) (torrentMetaData.getLength() % torrentPieceSize);
        }else{
            size = torrentPieceSize;
        }

        numberOfBlocks = (size + BLOCK_SIZE - 1) / BLOCK_SIZE;
        lastBlockSize = (size % BLOCK_SIZE != 0) ? size % BLOCK_SIZE : BLOCK_SIZE;

        blocks = Arrays.asList(new BlockStatus[numberOfBlocks]);

    }


    public int getBlockSize(){
        return BLOCK_SIZE;
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public int getLastBlockSize() {
        return lastBlockSize;
    }

    public PieceStatus getStatus() {
        return status;
    }

    public void setPieceStatus(PieceStatus status) {
        this.status = status;
    }

    public List<BlockStatus> getBlocks() {
        return blocks;
    }

    public void setBlocks(int index, BlockStatus status){
        blocks.set(index, status);
    }

    public int getPieceSize() {
        return size;
    }

    public int getNumberOfPeerOwners() {
        return numberOfPeerOwners;
    }

    public void setNumberOfPeerOwners(int numberOfPeerOwners) {
        this.numberOfPeerOwners = numberOfPeerOwners;
    }

    public void incrementNumOfPeerOwners(){
        numberOfPeerOwners++;
    }
    public void decrementNumOfPeerOwners(){
        numberOfPeerOwners--;
    }
}
