package misc.peers;

import misc.torrent.TorrentMetaData;
import misc.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Handles the connexion between the client (case of leecher 100%) and the peer,
 * @author Asaad
 */

public class PeerDownloadHandler {

    private Socket socket;
    private int peerClientPort;
    private String server = "127.0.0.1";
    private InputStream in;
    private OutputStream out;

    public static byte[] clientBitfield = null;
    public static RandomAccessFile file = null;

    //request msg <len=0013><id=6><Piece_index><Chunk_offset><Chunk_length>
    public int pieceSize;
    public int numPieces = 0;
    public static final int CHUNK_SIZE = 16384;

    public static AtomicInteger requestedMsgs = new AtomicInteger(0);

    private List<String> recievedPieces = new ArrayList<>();


    /** connection et ouverture du socket  */
    public PeerDownloadHandler(int peerClientPort, String server) throws IOException {
        this.peerClientPort = peerClientPort;
        this.server = server;

        connect();
    }

    /**
     * end connexion when download is ended
     * TODO : check if download is ended
     */
    public  void endConnexion() {

        try {
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            sendMessage(new Message(PeerMessage.MsgType.NOTINTERESTED));
            sendMessage(new Message(PeerMessage.MsgType.CHOKE));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /** initialize file and leecher bitfield  */
    public void initLeecher(TorrentMetaData torrentMetaData){
        pieceSize = torrentMetaData.getPieceLength();
        numPieces = (int) ((torrentMetaData.getLength() + pieceSize - 1 ) /pieceSize);
        int bfldSize = numPieces / 8 + 1;
        clientBitfield = new byte[bfldSize];
        for (int i = 0; i < bfldSize; ++i) {
            clientBitfield[i] = 0;
        }

        try {
            file = new RandomAccessFile("C:\\Users\\asaad_6stn3w\\IdeaProjects\\equipe5new\\src\\main\\" + torrentMetaData.getName(), "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }



    }

    /** connect to peer */
    private void connect() throws IOException {
        socket = new Socket(server, peerClientPort);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        //in = new BufferedInputStream(new DataInputStream(socket.getInputStream()));
       // out = new BufferedOutputStream(new DataOutputStream(socket.getOutputStream()));
    }

    /** effectuer le handshake, valider la reponse et lancer un thread qui lit les messages recus */
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

        //lit et reponds aux messages du seeder
        Thread messageReader = new Thread(() -> {
            while (true) {
               /* try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                Message receivedMessage = receiveMessage();

                if (receivedMessage.ID == PeerMessage.MsgType.KEEPALIVE) {
                    System.out.println("KEEP ALIVE RECEIVED");
                } else if (receivedMessage.ID == PeerMessage.MsgType.CHOKE) {
                    System.out.println("CHOKE RECEIVED");
                } else if (receivedMessage.ID == PeerMessage.MsgType.UNCHOKE) {
                    System.out.println("UCHOKE RECEIVED");

                } else if (receivedMessage.ID == PeerMessage.MsgType.INTERESTED) {
                    System.out.println("INTERESTED RECEIVED");
                } else if (receivedMessage.ID == PeerMessage.MsgType.NOTINTERESTED) {
                    System.out.println("NOTINTERESTED RECEIVED");
                } else if (receivedMessage.ID == PeerMessage.MsgType.HAVE) {
                    System.out.println("HAVE RECEIVED");
                    if (!hasPiece(receivedMessage.getIndex())) {
                        Message interested = new Message(PeerMessage.MsgType.INTERESTED);
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
                    System.out.println("piece received**** index : " + receivedMessage.index + " begin : "+receivedMessage.getBegin() + " size : " + receivedMessage.getPayload().length);

                    requestedMsgs.decrementAndGet();
                    if (!hasPiece(receivedMessage.index)) {
                        try {
                            file.seek(((long) receivedMessage.getIndex()*pieceSize + receivedMessage.getBegin()));
                            file.write(receivedMessage.getPayload());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    //setPiece(receivedMessage.index);

                }
            }
        });

        messageReader.start();

        return true;
    }
    /**
     check if an index is set in the bitfield
     */
    public static boolean hasPiece(int index){
        int byteIndex = index/8;
        int offset = index % 8;

        return (clientBitfield[byteIndex]>>(7-offset)&1) != 0;
    }

    /** set a bit for the bitfield */
    public static void setPiece(int index){
        int byteIndex = index/8;
        int offset = index%8;
        clientBitfield[byteIndex] |= 1 <<(7-offset);
    }

    /** send message to peer */
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

    /** receive msg from peer *
     *
     * @return Message unpacked
     */
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
