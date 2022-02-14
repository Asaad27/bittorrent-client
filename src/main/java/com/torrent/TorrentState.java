package com.torrent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.peers.ClientState;

import static com.download.TCPClient.torrentMetaData;


public class TorrentState {

    public final int BLOCK_SIZE = 16384;
    private static TorrentState instance = null;

    public LocalFileHandler localFileHandler;
    public final ConcurrentMap<Integer, AtomicInteger> pieceDownloadedBlocks = new ConcurrentHashMap<>();

    private long downloadedSize = 0;
    private long uploadedSize = 0;


    public List<Piece> pieces;
    public ClientState clientState;
    public boolean fileCheckedSuccess = false;

    private TorrentState(ClientState clientState) {
        initPiecesAndBlocks();
        this.clientState = clientState;
        localFileHandler = new LocalFileHandler(this, clientState);
        initStatus();

        initDownloadedSize();

    }

    private void initDownloadedSize() {
        for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
            downloadedSize += clientState.hasPiece(i) ? pieces.get(i).getPieceSize() : 0;
        }
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        System.out.println("% of file already DOWNLOADED : " + df.format(getDownloadedSize() * 1.0 / torrentMetaData.getLength() * 100) + "%");
    }

    public static TorrentState getInstance(ClientState clientState) {
        if (instance == null){
            instance = new TorrentState(clientState);
        }

        return instance;
    }

    private void initPiecesAndBlocks() {
        int numPieces = torrentMetaData.getNumberOfPieces();
        pieces = new ArrayList<>();
        for (int i = 0; i < numPieces; i++) {
            Piece piece = new Piece(i);
            pieces.add(piece);
        }
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }

    public long getUploadedSize() {
        return uploadedSize;
    }

    public void setUploadedSize(long uploadedSize) {
        this.uploadedSize = uploadedSize;
    }

    private void initStatus(){
        for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
            if(clientState.bitfield.hasPiece(i)) {
                pieces.get(i).setPieceStatus(PieceStatus.Verified);
            } else {
                pieces.get(i).setPieceStatus(PieceStatus.ToBeDownloaded);
            }
        }
    }

}
