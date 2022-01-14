package misc.download;


import misc.peers.ClientState;
import misc.peers.PeerInfo;
import misc.torrent.Observer;
import misc.torrent.TorrentContext;
import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;
import misc.torrent.TorrentState;
import misc.tracker.TrackerHandler;
import misc.utils.DEBUG;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class TCPClient implements Runnable{

    public static int OURPORT = 12327;
    public static String SERVER = "127.0.0.1";
    public static Queue<PeerInfo> waitingConnections = new LinkedList<>();
    public static TorrentContext torrentContext;
    //TODO : implement
    public static Set<PeerInfo> peerInfoSet;
    public TorrentFileHandler torrentHandler;
    public static TorrentMetaData torrentMetaData;

    public ClientState clientState;
    public TorrentState torrentState;
    //map port -> index of peer in List<PeerInfo>
    public static final Map<Integer, Integer> channelIntegerMap = new HashMap<>();

    private TrackerHandler tracker;
    private List<PeerInfo> peerInfoList;
    private final TCPMessagesHandler tcpMessagesHandler;
    private Selector selector;


    public TCPClient(String torrentPath) {

        Observer subject = new Observer();
        parseTorrent(torrentPath);
        peerInfoList = new ArrayList<>();
        generatePeerList(2001, 2002, 2003, 2004, 2005);
        //trackerList();
        //generatePeerList(26000);
        clientState = new ClientState(torrentMetaData.getNumberOfPieces());
        torrentState = TorrentState.getInstance(torrentMetaData, clientState);
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

                if(tcpMessagesHandler.fetchRequests()){
                    //System.err.println("request fetched");
                }

                SelectionKey key = keyIter.next();

                if (key.isValid() && key.isConnectable()) {
                    //TODO : connection errors, eq when port is wrong
                    tcpMessagesHandler.handleConnection(key);
                }

                if(key.isValid() && key.isAcceptable()){
                    tcpMessagesHandler.handleAccept(key, channelIntegerMap);
                }

                if (key.isValid() && key.isReadable()) {
                    tcpMessagesHandler.handleRead(key);
                }

                if (key.isValid() && key.isWritable()) {
                    tcpMessagesHandler.handleWrite(key);
                }

                keyIter.remove();
            }
        }
    }

    private void initializeSelector(){
        try {
            selector = Selector.open();

            for (int i = 0; i < peerInfoList.size(); i++) {

                assert peerInfoList.get(i).getPort() >= 0;
                SocketChannel clientChannel = SocketChannel.open();
                clientChannel.configureBlocking(false);
                clientChannel.register(selector, SelectionKey.OP_CONNECT, peerInfoList.get(i));

                int port = peerInfoList.get(i).getPort();

                boolean flag = clientChannel.connect(new InetSocketAddress(SERVER, port));
                DEBUG.log(String.valueOf(flag), String.valueOf(i), "port : ", String.valueOf(peerInfoList.get(i).getPort()));
                peerInfoList.get(i).index = i;
                channelIntegerMap.put(port, i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void parseTorrent(String torrentPath){
        try {
            torrentHandler = new TorrentFileHandler(new FileInputStream(torrentPath));
            torrentMetaData = torrentHandler.ParseTorrent();
        } catch (IOException | NoSuchAlgorithmException e) {
            DEBUG.printError(e, getClass().getName());
        }
        System.out.println(torrentMetaData);
    }

    public void trackerList(){
        try {
            URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());
            tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1InfoByte(), OURPORT, torrentMetaData.getNumberOfPieces());
            peerInfoList = tracker.getPeerLst();
            System.out.println(peerInfoList);

        } catch (IOException e) {
            e.printStackTrace();
        }
        //remove our client from the tracker response

        int tod = -1;
        for (int d = 0; d < peerInfoList.size(); d++){
            if (peerInfoList.get(d).getPort() == OURPORT)
                tod = d;
        }
        if (tod != -1)
            peerInfoList.remove(tod);

        //add peers to the set
        peerInfoSet.addAll(peerInfoList);
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
    }

}