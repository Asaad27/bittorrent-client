package misc.peers;

/**
 * Model class for Messages
 * @author Asaad
 */

public class Message {
    public PeerMessage.MsgType ID;
    byte[] payload;
    int index;
    int begin;
    int length;
    int blockSize;


    public Message(PeerMessage.MsgType ID) {
        this.ID = ID;
    }

    public Message(PeerMessage.MsgType ID, int index) {
        this.ID = ID;
        this.index = index;
    }

    public Message(PeerMessage.MsgType ID, byte[] payload) {
        this.ID = ID;
        this.payload = payload;
    }

    public Message(PeerMessage.MsgType ID, int index, int begin, int length) {
        this.ID = ID;
        this.index = index;
        this.begin = begin;
        this.length = length;
    }

    public Message(PeerMessage.MsgType ID, int index, int begin, byte[] payload) {
        this.ID = ID;
        this.payload = payload;
        this.index = index;
        this.begin = begin;
    }

    public PeerMessage.MsgType getID() {
        return ID;
    }

    public void setID(PeerMessage.MsgType ID) {
        this.ID = ID;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }
}
