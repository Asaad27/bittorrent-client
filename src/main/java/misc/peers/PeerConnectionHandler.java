package misc.peers;

import misc.torrent.TorrentMetaData;
import misc.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;

public class PeerConnectionHandler {

    private Socket socket;
    private int peerClientPort;
    private String server = "127.0.0.1";
    private InputStream in;
    private OutputStream out;
    public  static byte[] clientBitfield = null;
    public static RandomAccessFile file = null;

    public int pieceSize;
    public int indexPiece = 0;
    public int numPieces = 0;
    public int offset = 4;

    public PeerConnectionHandler() {
    }

    /** initialize file and leecher bitfield  **/
    public void initLeecher(TorrentMetaData torrentMetaData){
        pieceSize = torrentMetaData.getPiece_length();
        numPieces = torrentMetaData.getPiece_length()/pieceSize + 1;
        int bfldSize = numPieces / 8 + 1;
        clientBitfield = new byte[bfldSize];
        for (int i = 0; i < bfldSize; ++i) {
            clientBitfield[i] = 0;
        }

        try {
            file = new RandomAccessFile("C:\\Users\\asaad_6stn3w\\IdeaProjects\\equipe5new\\src\\main\\java" + torrentMetaData.getName(), "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /** connection et ouverture du socket  **/
    public PeerConnectionHandler(int peerClientPort, String server) throws IOException {
        this.peerClientPort = peerClientPort;
        this.server = server;

        connect();
    }

    private void connect() throws IOException {
        socket = new Socket(server, peerClientPort);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        //in = new BufferedInputStream(new DataInputStream(socket.getInputStream()));
       // out = new BufferedOutputStream(new DataOutputStream(socket.getOutputStream()));
    }


    /** effectuer le handshake, valider la reponse et lancer un thread qui lit les messages recus **/
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

        //lit et repond aux messages du seeder
        Thread messageReader = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message receivedMessage = receiveMessage();

                if (receivedMessage.ID == PeerMessage.MsgType.KEEPALIVE) {
                    System.out.println("KEEP ALIVE RECEIVED");
                } else if (receivedMessage.ID == PeerMessage.MsgType.CHOKE) {
                    System.out.println("CHOKE RECEIVED");
                } else if (receivedMessage.ID == PeerMessage.MsgType.UNCHOKE) {
                    System.out.println("UCHOKE RECEIVED");
                    //TODO : offset within a block ******
                    var request = new Message(PeerMessage.MsgType.REQUEST, indexPiece, offset, 16384);
                    try {
                        sendMessage(request);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    indexPiece += (offset==0 ? 1 : 0 );
                    offset = (offset == 4 ? 0 : 4);

                } else if (receivedMessage.ID == PeerMessage.MsgType.INTERESTED) {
                    System.out.println("INTERESTED RECEIVED");
                } else if (receivedMessage.ID == PeerMessage.MsgType.NOTINTERESTED) {
                    System.out.println("NOTINTERESTED RECEIVED");
                } else if (receivedMessage.ID == PeerMessage.MsgType.HAVE) {
                    System.out.println("HAVE RECEIVED");
                    if (!hasPiece(receivedMessage.getIndex())) {
                        var interested = new Message(PeerMessage.MsgType.INTERESTED);
                        try {
                            sendMessage(interested);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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

    /**
     check if an index is set in the bitfield
     **/
    public static boolean hasPiece(int index){
        int byteIndex = index/8;
        int offset = index % 8;

        return (clientBitfield[byteIndex]>>(7-offset)&1) != 0;
    }


    /** set a bit for the bitfield **/
    public static void setPiece(int index){
        int byteIndex = index/8;
        int offset = index%8;
        clientBitfield[byteIndex] |= 1 <<(7-offset);
    }

    public void sendMessage(ByteBuffer msg){

        try {
            out.write(msg.array());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("custom message sent");

    }

    public void sendMessage(Message message) throws IOException {
        byte[] msg = PeerMessage.serialize(message);
        try {
            out.write(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("error : " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println(message.ID.toString() + " sent" + " content : " + Utils.bytesToHex(msg));

    }

    public void sendMessage(byte[] msg) {
        try {
            out.write(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("error : " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("bitfield and interested "+ " sent" + " content : " + Utils.bytesToHex(msg));
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

        //assert
        assert(byteRead == len);

        return PeerMessage.deserialize(data);
    }



}
