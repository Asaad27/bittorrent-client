package misc.torrent;


import java.util.List;

public class Piece {
    private PieceStatus status;
    private List<BlockStatus> blocks;
    private int size;

    public Piece(PieceStatus status, List<BlockStatus> blocks, int size) {
        this.status = status;
        this.blocks = blocks;
        this.size = size;
    }

    public PieceStatus getStatus() {
        return status;
    }

    public void setStatus(PieceStatus status) {
        this.status = status;
    }

    public List<BlockStatus> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<BlockStatus> blocks) {
        this.blocks = blocks;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
