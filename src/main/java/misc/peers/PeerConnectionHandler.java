package misc.peers;

import misc.torrent.TorrentMetaData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;

public class PeerConnectionHandler {

    private Socket socket;
    private int peerClientPort;
    private String server = "127.0.0.1";
    private InputStream in;
    private OutputStream out;
    private PeerState peerState;
    public  static byte[] clientBitfield = null;
    public static RandomAccessFile file = null;

    private static final int PIECESIZE = 32*1024;

    public int indexPiece = 0;
    public int numPieces = 0;

    public PeerConnectionHandler() {
    }

    public void initLeecher(TorrentMetaData torrentMetaData){
        numPieces = torrentMetaData.getPiece_length()/PIECESIZE + 1;
        int bfldSize = numPieces / 8 + 1;
        clientBitfield = new byte[bfldSize];
        for (int i = 0; i < bfldSize; ++i) {
            clientBitfield[i] = 0;
        }

    }


    public PeerConnectionHandler(int peerClientPort) throws IOException {
        this.peerClientPort = peerClientPort;

        connect();
    }

    public PeerConnectionHandler(int peerClientPort, String server) throws IOException {
        this.peerClientPort = peerClientPort;
        this.server = server;

        connect();
    }

    private void connect() throws IOException {
        socket = new Socket(server, peerClientPort);
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }



    public boolean doHandShake(byte[] SHA1info, byte[] peersId){

        HandShake sentHand = new HandShake(SHA1info, peersId);


        byte[] handMsg = sentHand.createHandshakeMsg();
        try {
            out.write(sentHand.createHandshakeMsg(), 0, handMsg.length);
            out.flush();
        } catch (IOException e) {
            System.err.println("error cannot send handshake");
            return false;
        }

        //wait for handshake answer

        HandShake receivedHand = HandShake.readHandshake(in);


        if(!HandShake.compareHandshakes(sentHand, receivedHand))
        {
            System.err.println("HandShake response is not valid");
            return false;
        }

        System.out.println("handShake validated");
        Thread messageReader = new Thread(() -> {
            while (true) {
                Message receivedMessage = receiveMessage();

                if (receivedMessage.ID == PeerMessage.MsgType.KEEPALIVE) {
                    System.out.println("KEEP ALIVE RECEIVED");
                } else if (receivedMessage.ID == PeerMessage.MsgType.CHOKE) {
                    System.out.println("CHOKE RECEIVED");
                } else if (receivedMessage.ID == PeerMessage.MsgType.UNCHOKE) {
                    System.out.println("UCHOKE RECEIVED");
                    //TODO : offset within a block ?
                    var request = new Message(PeerMessage.MsgType.REQUEST, indexPiece, indexPiece + PIECESIZE, PIECESIZE);
                    sendMessage(request);
                } else if (receivedMessage.ID == PeerMessage.MsgType.INTERESTED) {
                    System.out.println("INTERESTED RECEIVED");
                } else if (receivedMessage.ID == PeerMessage.MsgType.NOTINTERESTED) {
                    System.out.println("NOTINTERESTED RECEIVED");
                } else if (receivedMessage.ID == PeerMessage.MsgType.HAVE) {
                    System.out.println("HAVE RECEIVED");
                    if (!hasPiece(receivedMessage.getIndex())) {
                        var interested = new Message(PeerMessage.MsgType.INTERESTED);
                        sendMessage(interested);
                    }

                } else if (receivedMessage.ID == PeerMessage.MsgType.BITFIELD) {
                    System.out.println("bitfield received");
                }
                else if (receivedMessage.ID == PeerMessage.MsgType.PIECE){
                    System.out.println("piece received");
                    if (!hasPiece(receivedMessage.index)) {
                        try {
                            file.seek(receivedMessage.getBegin());
                            file.write(receivedMessage.getPayload());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    setPiece(receivedMessage.index);


                }
            }
        });

        messageReader.start();

        return true;
    }

    /*public void sendBitfield(){
        byte[] msg = PeerMessage.serialize(new Message(PeerMessage.MsgType.BITFIELD, clientBitfield));
        try {
            out.write(msg, 0, msg.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Bitfield sent");
    }*/

    // check if an index is set in the bitfield
    public static boolean hasPiece(int index){
        int byteIndex = index/8;
        int offset = index % 8;

        return (clientBitfield[byteIndex]>>(7-offset)&1) != 0;
    }

    //set a bit for the bitfield
    public static void setPiece(int index){
        int byteIndex = index/8;
        int offset = index%8;
        clientBitfield[byteIndex] |= 1 <<(7-offset);
    }


    public void sendMessage(Message message){
        byte[] msg = PeerMessage.serialize(message);
        /*System.out.println(msg.length);
        System.out.println(message.ID);
        System.out.println(Arrays.toString(msg));*/
        try {
            out.write(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(message.ID.toString() + " sent");

    }

    public Message receiveMessage(){
        byte[] buffLen = new byte[4];
        ByteBuffer buffer;

        int bRead = 0;
        try {
            bRead = in.read(buffLen, 0, 4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert (bRead == 4);
        buffer = ByteBuffer.wrap(buffLen);
        int len = buffer.getInt();

        byte[] data = new byte[4+len];
        for (int i = 0; i < 4; i++)
            data[i] = buffLen[i];

        int byteRead = 0;
        while(byteRead < len) {
            assert in != null;
            try {
                byteRead += in.read(data, 4+byteRead, len-byteRead);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        assert(byteRead == len);

        return PeerMessage.deserialize(data);
    }




}
