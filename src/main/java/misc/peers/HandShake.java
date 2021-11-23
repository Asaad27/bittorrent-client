package misc.peers;

import misc.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;


/**
 * Class to handle hanshake
 *
 * @author Asaad
 */

public class HandShake implements Serializable {

    private final static String pstr = "BitTorrent protocol";
    private final static int pstrlen = pstr.length();
    private final static int HandshakeLength = 49 + pstrlen;
    private byte[] SHA1Info;
    private byte[] peerId;
    private final byte[] reserved;
    private final byte[] pstrbyte;

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

    /**
     * read recieved handshake
     *
     * @param in input stream
     */
    
    
    /**
     * compare two handshakes to check if the connection is valid
     *
     * @param hand1 : sent handshake
     * @param hand2 : received handshake
     */
    public static boolean compareHandshakes(HandShake hand1, HandShake hand2) {
        System.out.println(hand1);
        System.out.println(hand2);

        return Utils.bytesToHex(hand1.SHA1Info).equals(Utils.bytesToHex(hand2.SHA1Info));
    }

    /**
     * create a handshake message
     *
     * @return handshake message in byte array
     */
    public byte[] createHandshakeMsg() {
        ByteBuffer msg = ByteBuffer.allocate(HandshakeLength);

        msg.put((byte) pstrlen);
        msg.put(pstr.getBytes());


        msg.put(reserved);
        msg.put(SHA1Info);
        msg.put(peerId);

        msg.flip();

        return msg.array();
    }

    @Override
    public String toString() {
        return "pstrbyte: " + Utils.bytesToHex(pstrbyte) + " \n" + "reserved : " + Utils.bytesToHex(reserved) + "\n"
                + " SHA1INFO: " + Utils.bytesToHex(SHA1Info) + " \n" + "peerID : " + Utils.bytesToHex(peerId) + "\n";
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
