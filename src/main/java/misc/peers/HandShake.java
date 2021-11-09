package misc.peers;

import misc.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class HandShake implements Serializable {

    private final static String pstr = "BitTorrent protocol";
    private final static int pstrlen = pstr.length();
    private final static int HandshakeLength = 49 + pstrlen;
    private byte[] SHA1Info;
    private byte[] peerId;
    private byte[] reserved;
    private byte[] pstrbyte;

    public HandShake(byte[] SHA1Info, byte[] peerId) {
        this.SHA1Info = SHA1Info;
        this.peerId = peerId;

        reserved = new byte[8];
        for (int i = 0; i < 8; ++i)
            reserved[i] = 0;

        pstrbyte = pstr.getBytes();
    }

    private HandShake(byte[] SHA1Info, byte[] peerId, byte[] reserved, byte[] pstrbyte) {
        this.SHA1Info = SHA1Info;
        this.peerId = peerId;
        this.reserved = reserved;
        this.pstrbyte = pstrbyte;
    }



    public byte[] createHandshakeMsg(){
        ByteBuffer msg = ByteBuffer.allocate(HandshakeLength);

        msg.put((byte)pstrlen);
        msg.put(pstr.getBytes());


        msg.put(reserved);
        msg.put(SHA1Info);
        msg.put(peerId);

        msg.flip();

        return msg.array();
    }

    public static HandShake readHandshake(InputStream in) {
        if (in == null)
            System.err.println("error, null socket");

        byte[] receivedMsg = new byte[HandshakeLength];
        try {
            int byteRead = 0;
            while(byteRead < HandshakeLength) {
                assert in != null;
                byteRead += in.read(receivedMsg, byteRead, HandshakeLength - byteRead);
            }

        } catch (IOException e) {
            System.err.println("error reading received handshake");
        }


        ByteBuffer buffer = ByteBuffer.wrap(receivedMsg);

        byte[] tmp = new byte[1];
        buffer.get(tmp);
        byte[] pstrbyte = new byte[pstrlen];
        buffer.get(pstrbyte);


        byte[] reserved = new byte[8];
        buffer.get(reserved);

        byte[] SHA1Info = new byte[20];
        buffer.get(SHA1Info);

        byte[] peerId = new byte[20];
        buffer.get(peerId);

        return new HandShake(SHA1Info, peerId, reserved, pstrbyte);

    }

    public static boolean compareHandshakes(HandShake hand1, HandShake hand2){
        System.out.println(hand1);
        System.out.println(hand2);

        return Utils.bytesToHex(hand1.SHA1Info).equals(Utils.bytesToHex(hand2.SHA1Info));
    }



    @Override
    public String toString() {
        return  "pstrbyte: " + Utils.bytesToHex(pstrbyte) + " \n" + "reserved : "+ Utils.bytesToHex(reserved) + "\n"
                +" SHA1INFO: " + Utils.bytesToHex(SHA1Info) + " \n" + "peerID : "+ Utils.bytesToHex(peerId) + "\n";
    }

    public String getPstr() {
        return pstr;
    }

    public int getPstrlen() {
        return pstrlen;
    }

    public byte[] getSHA1Info() {
        return SHA1Info;
    }

    public void setSHA1Info(byte[] SHA1Info) {
        this.SHA1Info = SHA1Info;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public void setPeerId(byte[] peerId) {
        this.peerId = peerId;
    }
}
