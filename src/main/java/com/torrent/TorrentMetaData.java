package com.torrent;

import java.util.Date;
import java.util.List;

/**
 * torrent metadata model
 *
 * @author Asaad
 */

public class TorrentMetaData {

    private Long length;   //length of the file (bytes)
    private String name;
    private String comment;
    private String announceUrlString;

    private int pieceLength;
    private int numberOfPieces;
    private String pieces;
    private String SHA1Info;
    private byte[] SHA1InfoByte;
    private Date creationDate;
    private String createdBy;
    private List<String> piecesList;

    public List<String> getPiecesList() {
        return piecesList;
    }

    public void setPiecesList(List<String> piecesList) {
        this.piecesList = piecesList;
    }

    public String getSHA1Info() {
        return SHA1Info;
    }

    public void setSHA1Info(String SHA1Info) {
        this.SHA1Info = SHA1Info;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAnnounceUrlString() {
        return announceUrlString;
    }

    public void setAnnounceUrlString(String announceUrlString) {
        this.announceUrlString = announceUrlString;
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public int getPieceLength() {
        return pieceLength;
    }

    public void setPieceLength(int pieceLength) {
        this.pieceLength = pieceLength;
    }

    public int getNumberOfPieces() {
        return numberOfPieces;
    }

    public void setNumberOfPieces(int numberOfPieces) {
        this.numberOfPieces = numberOfPieces;
    }

    public String getPieces() {
        return pieces;
    }

    public void setPieces(String pieces) {
        this.pieces = pieces;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public byte[] getSHA1InfoByte() {
        return SHA1InfoByte;
    }

    public void setSHA1InfoByte(byte[] SHA1InfoByte) {
        this.SHA1InfoByte = SHA1InfoByte;
    }

    @Override
    public String toString() {
        return "name :  " + getName() + "\ncreationDate : " + getCreationDate() + "\ncreatedBy : " + getCreatedBy() + "\ncomment : " + getComment()
                + "\nannounce URl : " + getAnnounceUrlString() + "\nlength : " + getLength() + "\nSSH info : " + getSHA1Info() + "\nPiece length : " + getPieceLength() +
                "\nnumber of pieces : " + getNumberOfPieces() + "\n";
    }
}
