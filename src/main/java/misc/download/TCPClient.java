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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

public class TCPClient {

    public static int CLIENTPORT = 12327;
    public static Queue<PeerInfo> waitingConnections = new LinkedList<>();
    public static TorrentContext torrentContext;
    public static void main(String[] args) throws Exception {


        TorrentFileHandler torrentHandler = null;
        TorrentMetaData torrentMetaData = null;

        torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
        torrentMetaData = torrentHandler.ParseTorrent();
        System.out.println(torrentMetaData);

        URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());
        TrackerHandler tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1InfoByte(), CLIENTPORT, torrentMetaData.getNumberOfPieces());
        /*List<PeerInfo> peerInfoList = tracker.getPeerLst();
        //System.out.println(peerInfoList);
        //Remove our own client returned by tracker
        int tod = -1;
        for (int d = 0; d < peerInfoList.size(); d++){
*//*            if(peerInfoList.get(d).getPort() < 0)  //TODO : delete, this is just for debugging
                peerInfoList.get(d).port = 63533;*//*
            if (peerInfoList.get(d).getPort() == CLIENTPORT )
                tod = d;
        }
        if (tod != -1)
            peerInfoList.remove(tod);*/

        List<PeerInfo> peerInfoList = new ArrayList<>();

        //PeerInfo qbitorrent = new PeerInfo(InetAddress.getLocalHost(), 12316, torrentMetaData.getNumberOfPieces());
        //PeerInfo vuze = new PeerInfo(InetAddress.getLocalHost(), 12369, torrentMetaData.getNumberOfPieces());
        //PeerInfo transmission = new PeerInfo(InetAddress.getLocalHost(), 51413, torrentMetaData.getNumberOfPieces());
        //peerInfoList.add(qbitorrent);
        //peerInfoList.add(vuze);
        //peerInfoList.add(transmission);

        PeerInfo peer1 = new PeerInfo(InetAddress.getLocalHost(), 2001, torrentMetaData.getNumberOfPieces());
        PeerInfo peer2 = new PeerInfo(InetAddress.getLocalHost(), 2002, torrentMetaData.getNumberOfPieces());
        PeerInfo peer3 = new PeerInfo(InetAddress.getLocalHost(), 2003, torrentMetaData.getNumberOfPieces());
        peerInfoList.add(peer1);
        peerInfoList.add(peer2);
        peerInfoList.add(peer3);
        System.out.println(peerInfoList);

        String server = "127.0.0.1";

        //DEBUG.switchIOToFile();

        Observer subject = new Observer();
        ClientState clientState = new ClientState(torrentMetaData.getNumberOfPieces());
        TorrentState torrentState = TorrentState.getInstance(torrentMetaData, clientState);
        torrentContext = new TorrentContext(peerInfoList, torrentState, subject);

        Selector selector = Selector.open();

        TCPHANDLER tcphandler = new TCPHANDLER(torrentMetaData, peerInfoList, clientState, torrentState, subject);

        for (int i = 0; i < peerInfoList.size(); i++) {

            assert peerInfoList.get(i).getPort() >= 0;

            SocketChannel clntChan = SocketChannel.open();
            clntChan.configureBlocking(false);
            clntChan.register(selector, SelectionKey.OP_CONNECT);

            int port = peerInfoList.get(i).getPort();

            boolean flag = clntChan.connect(new InetSocketAddress(server, port));
            DEBUG.log(String.valueOf(flag), String.valueOf(i), "port : ", String.valueOf(peerInfoList.get(i).getPort()));
            tcphandler.channelIntegerMap.put(port, i);
        }

        while (true) {
            if (selector.select(3000) == 0) {
                System.out.print(".");
                continue;
            }


            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {

                if(TCPHANDLER.fetchRequests()){
                    System.err.println("request fetched");
                }

                SelectionKey key = keyIter.next();

                if (key.isConnectable()) {
                    tcphandler.handleConnection(key);
                }

                if (key.isValid() && key.isReadable()) {
                    tcphandler.handleRead(key);
                }

                if (key.isValid() && key.isWritable()) {
                    tcphandler.handleWrite(key);
                }

                keyIter.remove();
            }
        }
    }
}