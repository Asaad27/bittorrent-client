package misc.download;


import misc.DownloadMode;
import misc.peers.ClientState;
import misc.peers.PeerInfo;
import misc.torrent.Observer;
import misc.torrent.TorrentFileController;
import misc.torrent.TorrentMetaData;
import misc.torrent.TorrentState;
import misc.tracker.TrackerHandler;
import misc.tracker.TrackerPeriodic;
import misc.utils.DEBUG;

import javax.management.InstanceAlreadyExistsException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static misc.download.TCPMessagesHandler.NUMBER_OF_PIECES_PER_REQUEST;

/**
 * CLient main class
 */
public class TCPClient implements Runnable {

    public static final int OURPORT = 12274;

    public static Queue<PeerInfo> waitingConnections = new LinkedList<>();
    public static TorrentContext torrentContext;
    public static TorrentMetaData torrentMetaData;
    private final TCPMessagesHandler tcpMessagesHandler;
    private final TrackerHandler tracker;
    private final Set<PeerInfo> peerInfoList;
    private final AtomicBoolean connectToTracker = new AtomicBoolean(false);
    public TorrentFileController torrentHandler;
    public ClientState clientState;
    public TorrentState torrentState;
    private Selector selector;

    public TCPClient(String torrentPath) {

        Observer subject = new Observer();
        parseTorrent(torrentPath);
        peerInfoList = new HashSet<>();


        clientState = new ClientState(torrentMetaData.getNumberOfPieces());
        torrentState = TorrentState.getInstance(clientState);
        torrentContext = new TorrentContext(peerInfoList, torrentState, clientState, subject);
        tcpMessagesHandler = new TCPMessagesHandler(torrentMetaData, peerInfoList, clientState, torrentState, subject);
        tracker = new TrackerHandler(createAnnounceURL(), torrentMetaData.getSHA1InfoByte(), OURPORT, torrentMetaData.getNumberOfPieces());

        //Set<PeerInfo> peers = generatePeerList(6868);
        Set<PeerInfo> peers = getPeersFromTracker();
        initializeSelector(peers);

        TrackerPeriodic trackerPeriodic = new TrackerPeriodic(connectToTracker);
        trackerPeriodic.run();
        DownloadRate downloadRate = new DownloadRate(torrentState);
        downloadRate.run();

    }

    /**
     * Main thread that runs selector
     */
    @Override
    public void run() {

        while (true) {
            if (connectToTracker.getAndSet(false)) {
                Set<PeerInfo> newPeers;
                try {
                    newPeers = tracker.getPeerList();
                    addToSelector(newPeers);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

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
                //PeerInfo peerInfo = (PeerInfo) key.attachment();

                if (ClientState.isDownloading && receivedAllBitfields())
                    tcpMessagesHandler.fetchRequests();


                if (key.isValid() && key.isConnectable()) {
                    tcpMessagesHandler.handleConnection(key);
                }

                if (key.isValid() && key.isAcceptable()) {
                    tcpMessagesHandler.handleAccept(key);
                }

                if (key.isValid() && key.isReadable()) {
                    try {
                        tcpMessagesHandler.handleRead(key);
                    } catch (CancelledKeyException e) {
                        DEBUG.printError(e, getClass().getName());
                    }

                }

                if (key.isValid() && key.isWritable()) {
                    tcpMessagesHandler.handleWrite(key);
                }

                keyIter.remove();
            }
        }
    }

    /**
     * initialize selector
     * @param peers initial peer list
     */
    private void initializeSelector(Set<PeerInfo> peers) {
        try {
            selector = Selector.open();

            ServerSocketChannel passiveChannel = ServerSocketChannel.open();
            passiveChannel.socket().bind(new InetSocketAddress(OURPORT));
            passiveChannel.configureBlocking(false);
            passiveChannel.register(selector, SelectionKey.OP_ACCEPT);

            addToSelector(peers);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * check, and add newPeers to the selector
     * @param newPeers new peers to add
     */
    public void addToSelector(Set<PeerInfo> newPeers) throws IOException {
        System.out.println("querying for peers");

        if (newPeers == null)
            return;
        System.out.println(newPeers);
        for (PeerInfo newPeer : newPeers) {
            boolean contains = false;
            for (PeerInfo oldPeer : peerInfoList)
                if (oldPeer.equals(newPeer)) {
                    contains = true;
                    break;
                }
            if (contains)
                continue;
            System.out.println("new peer to be added" + newPeer);
            peerInfoList.add(newPeer);
            assert newPeer.getPort() >= 0;
            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_CONNECT, newPeer);
            int port = newPeer.getPort();
            clientChannel.connect(new InetSocketAddress(newPeer.addr, port));
        }

    }

    /**
     * Parse torrent metainfo file
     * @param torrentPath path of the torrent file
     */
    public void parseTorrent(String torrentPath) {
        try {
            torrentHandler = new TorrentFileController(new FileInputStream(torrentPath));
            torrentMetaData = torrentHandler.ParseTorrent();
        } catch (IOException | NoSuchAlgorithmException e) {
            DEBUG.printError(e, getClass().getName());
        }
        System.out.println(torrentMetaData);
    }


    /**
     * Contact tracker and getPeers from it
     * @return peers received from tracker
     */
    public Set<PeerInfo> getPeersFromTracker() {

        Set<PeerInfo> newPeers = null;
        try {
            newPeers = tracker.getPeerList();
            System.out.println(newPeers);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
        //remove our client from the tracker response


        DEBUG.logf("generated peers from tracker");

        return newPeers;
    }

    /**
     * generate announceUrl from meta info file
     * @return URL of the announce
     */
    public URL createAnnounceURL() {
        URL announceURL = null;
        try {
            announceURL = new URL(torrentMetaData.getAnnounceUrlString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return announceURL;
    }

    /**
     * manually generate peers without contacting tracker
     * @param ports ports of each peer to add
     * @return peers
     */
    public Set<PeerInfo> generatePeerList(int... ports) {
        Set<PeerInfo> list = new HashSet<>();
        for (int port : ports) {
            try {
                InetAddress global = InetAddress.getByName("10.188.226.253");
                InetAddress local = InetAddress.getLocalHost();
                PeerInfo peer = new PeerInfo(local, port, torrentMetaData.getNumberOfPieces());
                list.add(peer);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return list;
    }

    /**
     * set the download mode
     * @param mode FAST : unlimited download and upload speed
     *             SLOW : limited but more secure download/upload speed
     */
    public void setDownloadMode(DownloadMode mode){
        switch (mode){
            case FAST:
                setFastMode();
                break;
            case SLOW:
                setSlowMode();
                break;
            default:
                break;
        }
    }

    private void setFastMode(){
        NIODownloadHandler.BLOCKS_PER_PEER = 100;
        TCPMessagesHandler.NUMBER_OF_READ_MSG_PER_PEER = 100;
        TCPMessagesHandler.NUMBER_OF_REQUEST_PER_PEER = 100;
        NUMBER_OF_PIECES_PER_REQUEST = 6;
    }

    private void setSlowMode(){
        NIODownloadHandler.BLOCKS_PER_PEER = 6;
        TCPMessagesHandler.NUMBER_OF_READ_MSG_PER_PEER = 6;
        TCPMessagesHandler.NUMBER_OF_REQUEST_PER_PEER = 6;
        NUMBER_OF_PIECES_PER_REQUEST = 3;
    }

    public boolean receivedAllBitfields() {
        for (PeerInfo peer : peerInfoList) {
            if (!peer.getPeerState().sentBitfield)
                return false;
        }
        return true;
    }


}