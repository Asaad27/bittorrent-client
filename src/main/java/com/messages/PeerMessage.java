package com.messages;


import java.nio.ByteBuffer;

/**
 * Class to handle messages sent and received from peer
 *
 * @author Asaad
 */

public class PeerMessage {

    /**
     * @param msg message to pack
     * @return unpacked message in byte array reader to be sent
     */

    public static byte[] serialize(Message msg) {
        ByteBuffer buffer = null;
        MsgType msgType = msg.ID;


        switch (msgType) {
            case KEEPALIVE:
                buffer = ByteBuffer.allocate(4);
                buffer.putInt(0);
                break;

            case CHOKE:
                buffer = ByteBuffer.allocate(5);
                buffer.putInt(1);
                buffer.put((byte) 0);
                break;

            case UNCHOKE:
                buffer = ByteBuffer.allocate(5);
                buffer.putInt(1);
                buffer.put((byte) 1);
                break;

            case INTERESTED:
                buffer = ByteBuffer.allocate(5);
                buffer.putInt(1);
                buffer.put((byte) 2);

                break;

            case UNINTERESTED:
                buffer = ByteBuffer.allocate(5);
                buffer.putInt(1);
                buffer.put((byte) 3);
                break;

            case HAVE:
                buffer = ByteBuffer.allocate(9);
                buffer.putInt(5);
                buffer.put((byte) 4);
                buffer.putInt(msg.getIndex());
                break;

            case BITFIELD:
                buffer = ByteBuffer.allocate(5 + msg.payload.length);
                buffer.putInt(1 + msg.payload.length);
                buffer.put((byte) 5);
                buffer.put(msg.payload, 0, msg.payload.length);
                break;

            case PIECE:
                buffer = ByteBuffer.allocate(13 + msg.payload.length);
                buffer.putInt(9 + msg.payload.length);
                buffer.put((byte) 7);
                buffer.putInt(msg.getIndex());
                buffer.putInt(msg.getBegin());
                buffer.put(msg.payload, 0, msg.payload.length);
                break;

            case REQUEST:
                buffer = ByteBuffer.allocate(17);
                buffer.putInt(13);
                buffer.put((byte) 6);
                buffer.putInt(msg.getIndex());
                buffer.putInt(msg.getBegin());
                buffer.putInt(msg.getLength());
                break;

            case CANCEL:
                buffer = ByteBuffer.allocate(17);
                buffer.putInt(13);
                buffer.put((byte) 8);
                buffer.putInt(msg.getIndex());
                buffer.putInt(msg.getBegin());
                buffer.putInt(msg.getLength());
                break;

            default:
                System.err.println("MSG TYPE UNKNOWN");
                return new byte[1];
        }


        return buffer.array();
    }

    /**
     * @param msg message to unpack
     * @return Unpacked message
     */
    public static Message deserialize(byte[] msg) {
        ByteBuffer buffer = ByteBuffer.wrap(msg);

        int len = buffer.getInt();
        if (len == 0)
            return new Message(MsgType.KEEPALIVE);
        byte id = buffer.get();
        if (len == 1)
            return new Message(MsgType.getById(id));
        //Have
        if (id == 4) {
            return new Message(MsgType.HAVE, buffer.getInt());
        }
        //bitfield
        if (id == 5) {
            byte[] bitfield = new byte[len - 1];
            buffer.get(bitfield, 0, len - 1);
            return new Message(MsgType.BITFIELD, bitfield);
        }
        //request
        if (id == 6) {
            return new Message(MsgType.REQUEST, buffer.getInt(), buffer.getInt(), buffer.getInt());
        }
        //piece
        if (id == 7) {
            int index = buffer.getInt();
            int begin = buffer.getInt();
            byte[] payload = new byte[len - 9];

            buffer.get(payload, 0, len - 9);
            return new Message(MsgType.PIECE, index, begin, payload);
        }

        //Cancel
        if (id == 8) {
            return new Message(MsgType.CANCEL, buffer.getInt(), buffer.getInt(), buffer.getInt());
        }


        System.err.println("no matching id");
        return null;

    }

    public enum MsgType {
        CHOKE,
        UNCHOKE,
        INTERESTED,
        UNINTERESTED,
        HAVE,
        BITFIELD,
        REQUEST,
        PIECE,
        CANCEL,
        PORT,
        KEEPALIVE;
        private static final MsgType[] values = MsgType.values();

        public static MsgType getById(int id) {
            return values[id];
        }

        public int getID() {
            return ordinal();
        }
    }


}
