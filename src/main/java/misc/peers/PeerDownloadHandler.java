package misc.peers;

import misc.torrent.TorrentMetaData;
import misc.tracker.TrackerHandler;
import misc.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Handles the connexion between the client (case of leecher 100%) and the peer,
 * @author Asaad
 */

public class PeerDownloadHandler {

    public static final int CHUNK_SIZE = 16384;
    public static byte[] clientBitfield = null;
    public static RandomAccessFile file = null;
    //variable used to keep number of requested messages up to 5, because some bittorrent clients choke the leecher when he sends more than 5 before getting answered
    public static AtomicInteger requestedMsgs = new AtomicInteger(0);
    private final AtomicBoolean isDownloading = new AtomicBoolean(true);
    //request msg <len=0013><id=6><Piece_index><Chunk_offset><Chunk_length>
    private TorrentMetaData torrentMetaData;
    public int numPieces = 0;
    private Socket socket;
    private final int peerClientPort;
    private String server = "127.0.0.1";
    private InputStream in;
    private OutputStream out;


    /* divide the current piece into blocks */
    // if it's the last piece compute it's size
    private int blockSize = CHUNK_SIZE;
    private int lastBlockSize;
    private int numOfBlocks;
    private int pieceSize;
    private int lastPieceSize;
    //nombre de block de 16KIB
    private int numOfLastPieceBlocks;
    //le reste
    private int remainingBlockSize;

    private int downloadedSize = 0;

    private ConcurrentMap<Integer, AtomicInteger> pieceDownloadedBlocks = new ConcurrentHashMap<Integer, AtomicInteger>();

    /**
     * connection et ouverture du socket
     */
    public PeerDownloadHandler(int peerClientPort, String server, TorrentMetaData torrentMetaData) throws IOException {
        this.peerClientPort = peerClientPort;
        this.server = server;
        this.torrentMetaData = torrentMetaData;

        initPiecesAndBlocks();
        connect();
    }

    /**
     * connect to peer
     */
    private void connect() throws IOException {
        socket = new Socket(server, peerClientPort);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        //in = new BufferedInputStream(new DataInputStream(socket.getInputStream()));
        // out = new BufferedOutputStream(new DataOutputStream(socket.getOutputStream()));
    }

    /**
     * end connexion when download is ended
     */
    public void endConnexion() {

        System.out.println("DOWNLOAD FINISHED");


        try {
            sendMessage(new Message(PeerMessage.MsgType.NOTINTERESTED));
            Thread.sleep(3000);
            isDownloading.set(false);
            file.close();
            out.close();
            in.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("bitfield : ");
        System.out.println(Utils.bytesToHex(clientBitfield));

    }

    /**
    * pre-compute sizes of pieces, and blocks per pieces
     */
    private void initPiecesAndBlocks()
    {
        lastBlockSize = (torrentMetaData.getPieceLength() % blockSize != 0) ? torrentMetaData.getPieceLength() % blockSize : blockSize;
        numOfBlocks = (torrentMetaData.getPieceLength() + blockSize - 1) / blockSize;
        pieceSize = torrentMetaData.getPieceLength();
        lastPieceSize = (torrentMetaData.getLength() % pieceSize == 0) ? pieceSize : (int) (torrentMetaData.getLength() % pieceSize);
        numOfLastPieceBlocks = (lastPieceSize + blockSize -1) / blockSize;
        remainingBlockSize = lastPieceSize % blockSize;
        pieceSize = torrentMetaData.getPieceLength();
        numPieces = torrentMetaData.getNumberOfPieces();
    }


    /**
     * initialize file and leecher bitfield
     */
    //TODO : case of non 100% leecher, aka read from file an create bitfield
    public void initLeecher(TorrentMetaData torrentMetaData) {
        int bfldSize = (numPieces+7)/ 8 ;
        clientBitfield = new byte[bfldSize];
        for (int i = 0; i < bfldSize; ++i) {
            clientBitfield[i] = 0;
        }
        try {
            file = new RandomAccessFile("src\\main\\" + torrentMetaData.getName(), "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * initialize file and seeder bitfield
     * @param torrentMetaData
     * @param path path of the file described in the metadata
     */
    private void initSeeder(TorrentMetaData torrentMetaData, String path){
        pieceSize = torrentMetaData.getPieceLength();
        numPieces = torrentMetaData.getNumberOfPieces();
        int bfldSize = (numPieces + 7) / 8 ;
        clientBitfield = new byte[bfldSize];
        for (int i = 0; i < bfldSize; ++i) {
            clientBitfield[i] = 1;
        }
        try {
            file = new RandomAccessFile(path, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Thread qui gere la lecture des messages recus
     */
    private void messageReader() {
        while (isDownloading.get()) {
            Message receivedMessage = receiveMessage();

            if (receivedMessage.ID == PeerMessage.MsgType.KEEPALIVE) {
                System.out.println("KEEP ALIVE RECEIVED");
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
            } else if (receivedMessage.ID == PeerMessage.MsgType.PIECE) {
               // System.out.println("****piece received**** index : " + receivedMessage.index + " begin : " + receivedMessage.getBegin() + " size : " + receivedMessage.getPayload().length);
                requestedMsgs.decrementAndGet();
                downloadedSize += receivedMessage.getPayload().length;
                if (!hasPiece(receivedMessage.index)) {
                    try {
                        file.seek(((long) receivedMessage.getIndex() * pieceSize + receivedMessage.getBegin()));
                        file.write(receivedMessage.getPayload());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    pieceDownloadedBlocks.putIfAbsent(receivedMessage.index, new AtomicInteger(0));
                    int downloadedBlocks = pieceDownloadedBlocks.get(receivedMessage.index).incrementAndGet();
                    DecimalFormat df = new DecimalFormat();
                    df.setMaximumFractionDigits(2);
                    if (receivedMessage.index == numPieces-1)
                    {
                        if (downloadedBlocks == numOfLastPieceBlocks)
                        {
                            System.out.println("PIECE DOWNLOADED : " + receivedMessage.getIndex() + "\t" + df.format(downloadedSize*1.0/torrentMetaData.getLength()*100) + "% downloaded" );
                            setPiece(receivedMessage.getIndex());
                            Message have = new Message(PeerMessage.MsgType.HAVE, numPieces-1);
                            try {
                                sendMessage(have);
                            } catch (IOException e) {
                                System.err.println(e.getMessage());
                            }
                        }
                    }
                    else
                    {
                        if (downloadedBlocks == numOfBlocks)
                        {
                            System.out.println("PIECE DOWNLOADED : " + receivedMessage.getIndex() + "\t" + df.format(downloadedSize*1.0/torrentMetaData.getLength()*100) + "% downloaded");
                            setPiece(receivedMessage.getIndex());
                            Message have = new Message(PeerMessage.MsgType.HAVE, receivedMessage.index);
                            try {
                                sendMessage(have);
                            } catch (IOException e) {
                                System.err.println(e.getMessage());
                            }
                        }
                    }
                }
                if (downloadedSize == torrentMetaData.getLength())
                    endConnexion();
            }
        }
    }
    /**
     * check if an index is set in the bitfield
     */
    public boolean hasPiece(int index) {
        int byteIndex = index / 8;
        int offset = index % 8;

        return ((clientBitfield[byteIndex]>>(7 - offset)) & 1) != 0;
    }

    /**
     * set a bit for the bitfield
     */
    public void setPiece(int index) {
        int byteIndex = index / 8;
        int offset = index % 8;
        clientBitfield[byteIndex] |= 1 << (7 - offset);
    }

    /**
     * effectuer le handshake, valider la reponse et lancer un thread qui lit les messages recus
     * @param peersId our peerid
     * @param SHA1info sha1 hash of the info dictionnary
     * @return boolean describing the success of the handshake
     */
    public boolean doHandShake(byte[] SHA1info, byte[] peersId) {

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

        if (!HandShake.compareHandshakes(sentHand, receivedHand)) {
            System.err.println("HandShake response is not valid");
            return false;
        }
        System.out.println("handShake validated");

        //lit et reponds aux messages du seeder
        Thread messageReader = new Thread(this::messageReader);

        messageReader.start();

        return true;
    }

    /**
     * send message to peer
     */
    public void sendMessage(Message message) throws IOException {
        byte[] msg = PeerMessage.serialize(message);
        try {
            out.write(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("error : " + e.getMessage());
            e.printStackTrace();
        }
        //System.out.println(message.ID.toString() + " sent" + " content : " + Utils.bytesToHex(msg));
    }

    /**
     * receive msg from peer
     * @return Message unpacked
     */
    public Message receiveMessage() {
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

        byte[] data = new byte[4 + len];
        for (int i = 0; i < 4; i++)
            data[i] = buffLen[i];

        int byteRead = 0;
        while (byteRead < len) {
            assert in != null;
            try {
                byteRead += in.read(data, 4 + byteRead, len - byteRead);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        assert (byteRead == len);

        return PeerMessage.deserialize(data);
    }

    /**
     * cas Leecher 100%
     */
    public void downloadTorrent() {



        // Port d'écoute Bittorent
        int PORT = peerClientPort;
        String SERVER = server;

        String PEERID = TrackerHandler.genPeerId();
        // On initialise le TorrentFileHandler à partir du fichier Torrent d'entrée

        try {

            System.out.println(torrentMetaData.toString());
            URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());

            //LocalFileHandler localFile = new LocalFileHandler(torrentMetaData.getName(), torrentMetaData.getNumberOfPieces(), torrentMetaData.getPiece_length(), torrentMetaData.getPieces());
            //TrackerHandler tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1InfoByte(), localFile, PORT);
            //System.out.println("looking for peers");
            //List<PeerInfo> peerLst = tracker.getPeerLst();
            //System.out.println("peerlist received");

            initLeecher(torrentMetaData);

            doHandShake(Utils.hexStringToByteArray(torrentMetaData.getSHA1Info()), Utils.hexStringToByteArray(PEERID));


            Message bitfield = new Message(PeerMessage.MsgType.BITFIELD, PeerDownloadHandler.clientBitfield);
            sendMessage(bitfield);

            Message interested = new Message(PeerMessage.MsgType.INTERESTED);
            sendMessage(interested);

            Message unchoke = new Message(PeerMessage.MsgType.UNCHOKE);
            sendMessage(unchoke);


            //TODO : traiter le cas ou la taille de la piece n'est pas divisible par la taille du block
            for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
                if (hasPiece(i))
                    continue;
                if (i == torrentMetaData.getNumberOfPieces() - 1) {

                    int lpoffset = 0, lpcountBlocks = 0;
                    for (int j = 0; j < numOfLastPieceBlocks-1; j++) {
                        Message request = new Message(PeerMessage.MsgType.REQUEST, i, lpoffset, blockSize);
                        sendMessage(request);
                        lpoffset += blockSize;
                        lpcountBlocks++;
                    }
                    //we send the last block
                    if (remainingBlockSize != 0) {
                        Message request = new Message(PeerMessage.MsgType.REQUEST, i, lpoffset, remainingBlockSize);
                        sendMessage(request);

                    }

                } else {
                    int offset = 0, countBlocks = 0;
                    while (offset < pieceSize) {
                        while (PeerDownloadHandler.requestedMsgs.get() >= 5) {
                            //notifyPieceCompleted()
                        }

                        requestedMsgs.incrementAndGet();

                        Message request = new Message(PeerMessage.MsgType.REQUEST, i, offset, blockSize);
                        sendMessage(request);
                        countBlocks++;
                        offset += (countBlocks == numOfBlocks) ? lastBlockSize : blockSize;
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
