package misc.download;


import misc.peers.ClientState;
import misc.peers.PeerInfo;
import misc.torrent.Observer;
import misc.torrent.TorrentFileController;
import misc.torrent.TorrentMetaData;
import misc.torrent.TorrentState;
import misc.tracker.TrackerHandler;
import misc.utils.DEBUG;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class TCPClient implements Runnable{

    public static int OURPORT = 12347;
    public static String SERVER = "127.0.0.1";
    public static Queue<PeerInfo> waitingConnections = new LinkedList<>();
    public static TorrentContext torrentContext;


    public TorrentFileController torrentHandler;
    public static TorrentMetaData torrentMetaData;

    public ClientState clientState;
    public TorrentState torrentState;


    private TrackerHandler tracker;
    private Set<PeerInfo> peerInfoList;
    private final TCPMessagesHandler tcpMessagesHandler;
    private Selector selector;


    public TCPClient(String torrentPath) {

        Observer subject = new Observer();
        parseTorrent(torrentPath);
        peerInfoList = new HashSet<>();
        //generatePeerList(2001, 2002, 2003, 2004, 2005);
        getPeersFromTracker();
        //generatePeerList(27027);
        //generatePeerList(51413);
        //generatePeerList(2001);
        clientState = new ClientState(torrentMetaData.getNumberOfPieces());
        torrentState = TorrentState.getInstance(clientState);
        torrentContext = new TorrentContext(peerInfoList, torrentState, clientState, subject);
        tcpMessagesHandler = new TCPMessagesHandler(torrentMetaData, peerInfoList, clientState, torrentState, subject);

        initializeSelector();
    }

    @Override
    public void run(){

        while (true) {
            try {
                if (selector.select(3000) == 0) {
                    System.out.print(".");
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {

                SelectionKey key = keyIter.next();
                PeerInfo peerInfo = (PeerInfo) key.attachment();

                if(clientState.isDownloading){
                    tcpMessagesHandler.fetchRequests();
                }




                if (key.isValid() && key.isConnectable()) {
                    //TODO : connection errors, eq when port is wrong
                    tcpMessagesHandler.handleConnection(key);
                }

                if(key.isValid() && key.isAcceptable()){
                    //System.err.println("accepting....");
                    tcpMessagesHandler.handleAccept(key);
                }

                if (key.isValid() && key.isReadable()) {
                    //System.err.println("reading....");
                    try{
                        tcpMessagesHandler.handleRead(key);
                    }catch (CancelledKeyException e){
                        DEBUG.printError(e, getClass().getName());
                    }

                }

                if (key.isValid() && key.isWritable()) {
                    //System.err.println("writing...");
                    tcpMessagesHandler.handleWrite(key);
                }

                keyIter.remove();
            }
        }
    }

    private void initializeSelector(){
        try {
            selector = Selector.open();

            ServerSocketChannel passiveChannel = ServerSocketChannel.open();
            passiveChannel.socket().bind(new InetSocketAddress(OURPORT));
            passiveChannel.configureBlocking(false);
            passiveChannel.register(selector, SelectionKey.OP_ACCEPT);

            int index = 0;
            for (PeerInfo peerInfo : peerInfoList) {

                assert peerInfo.getPort() >= 0;
                SocketChannel clientChannel = SocketChannel.open();
                clientChannel.configureBlocking(false);
                clientChannel.register(selector, SelectionKey.OP_CONNECT, peerInfo);
                int port = peerInfo.getPort();
                clientChannel.connect(new InetSocketAddress(SERVER, port));
                peerInfo.index = index;
                index++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void parseTorrent(String torrentPath){
        try {
            torrentHandler = new TorrentFileController(new FileInputStream(torrentPath));
            torrentMetaData = torrentHandler.ParseTorrent();
        } catch (IOException | NoSuchAlgorithmException e) {
            DEBUG.printError(e, getClass().getName());
        }
        System.out.println(torrentMetaData);
    }

    //TODO : infinite loop
    public void getPeersFromTracker(){
        DEBUG.log("generating peers from tracker ...");

        try {
            URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());
            tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1InfoByte(), OURPORT, torrentMetaData.getNumberOfPieces());
            peerInfoList = tracker.getPeerLst();
            System.out.println(peerInfoList);

        } catch (IOException e) {
            e.printStackTrace();
        }
        //remove our client from the tracker response

        peerInfoList.removeIf(peerInfo -> peerInfo.getPort() == OURPORT);

        DEBUG.log("generated peers from tracker");

    }

    public void generatePeerList(int ...ports){
        for (int port: ports) {
            try {
                PeerInfo peer = new PeerInfo(InetAddress.getLocalHost(), port, torrentMetaData.getNumberOfPieces());
                peerInfoList.add(peer);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        System.out.println(peerInfoList);
    }

}