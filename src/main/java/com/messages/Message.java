package com.messages;

import com.utils.Utils;

/**
 * Model class for Messages
 * @author Asaad
 */

public class Message {
    public PeerMessage.MsgType ID;
    byte[] payload;
    private int index;
    private int begin;
    private int length;


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

    public byte[] getPayload() {
        return payload;
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

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }


    @Override
    public String toString() {
        switch (getID()){
            case CHOKE:
            case KEEPALIVE:
            case UNCHOKE:
            case INTERESTED:
            case UNINTERESTED:
                return ID.name();
            case HAVE:
                return ID.name() + " " + "piece : " + getIndex();
            case BITFIELD:
                return ID.name() + " " + " payload : " + Utils.bytesToHex(getPayload());
            case REQUEST:
            case CANCEL:
                return ID.name() + " " + " piece : " + getIndex() + " begin : " + getBegin() + " length : " + getLength();
            case PIECE:
                return ID.name() + " " + " piece : " + getIndex() + " begin : " + getBegin();
            default:
                return "unknown message";
        }
    }
}
