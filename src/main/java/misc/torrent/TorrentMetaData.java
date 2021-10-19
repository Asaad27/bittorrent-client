package misc.torrent;

import java.util.List;

/** torrent metadata model **/

public class TorrentMetaData {

    private Long length;   //length of the file (bytes)
    private String name;
    private String comment;
    private String announceUrlString;

    private int piece_length;
    private List<String> pieces;
    private String SHA1Info;
    private String creationDate;
    private String createdBy;


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

    public int getPiece_length() {
        return piece_length;
    }

    public void setPiece_length(int piece_length) {
        this.piece_length = piece_length;
    }

    public List<String> getPieces() {
        return pieces;
    }

    public void setPieces(List<String> pieces) {
        this.pieces = pieces;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return "name :  " + getName() + "\ncreationDate : " + getCreationDate() + "\ncreatedBy : " + getCreatedBy() + "\ncomment : " + getComment()
                + "\nannounce URl : " + getAnnounceUrlString() + "\nlength : " + getLength() + "\nSSH info : " + getSHA1Info();
    }
}
