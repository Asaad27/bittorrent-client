package misc.peers;

import misc.torrent.TorrentContext;
import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;
import misc.torrent.TorrentState;
import misc.tracker.TrackerHandler;
import misc.utils.DEBUG;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;

public class TCPClient {

    public static int CLIENTPORT = 12315;
    public static void main(String args[]) throws Exception {


        TorrentFileHandler torrentHandler = null;
        TorrentMetaData torrentMetaData = null;

        torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
        torrentMetaData = torrentHandler.ParseTorrent();
        System.out.println(torrentMetaData);

        URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());
        TrackerHandler tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1InfoByte(), CLIENTPORT, torrentMetaData.getNumberOfPieces());
        List<PeerInfo> peerInfoList = tracker.getPeerLst();
        //System.out.println(peerInfoList);
        //Remove our own client returned by tracker
        int tod = -1;
        for (int d = 0; d < peerInfoList.size(); d++){
/*            if(peerInfoList.get(d).getPort() < 0)  //TODO : delete, this is just for debugging
                peerInfoList.get(d).port = 63533;*/
            if (peerInfoList.get(d).getPort() == CLIENTPORT )
                tod = d;
        }
        if (tod != -1)
            peerInfoList.remove(tod);

        System.out.println(peerInfoList);

        String server = "127.0.0.1";
        
        ClientState clientState = new ClientState(torrentMetaData.getNumberOfPieces());
        TorrentState torrentState = new TorrentState(torrentMetaData, clientState);
        TorrentContext torrentContext = new TorrentContext(peerInfoList, torrentMetaData.getNumberOfPieces(), torrentState);

        Selector selector = Selector.open();

        TCPHANDLER tcphandler = new TCPHANDLER(torrentMetaData, peerInfoList, clientState, torrentState);

        for (int i = 0; i < peerInfoList.size(); i++) {

            assert peerInfoList.get(i).getPort() >= 0;

            SocketChannel clntChan = SocketChannel.open();
            clntChan.configureBlocking(false);
            clntChan.register(selector, SelectionKey.OP_CONNECT);

            //boolean flag = clntChan.connect(new InetSocketAddress(server, (i == 0 ? Main.PORTCLIENT.VUZE.port : Main.PORTCLIENT.QBITTORRENT.port)));
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
            torrentContext.updatePeerState();
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();

                if (key.isConnectable()) {

                    SocketChannel clntChan = (SocketChannel) key.channel();

                    System.out.println("remote adresses " + clntChan.getRemoteAddress());

                    boolean isConnected = clntChan.finishConnect();
                    assert isConnected;
                    //TODO : send handshake && bitfield && interested

                    key.interestOps(SelectionKey.OP_WRITE);
                }

                if (key.isValid() && key.isWritable()) {
                    //DEBUG.log("writing");
                    tcphandler.handleWrite(key);
                    //key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    //key.interestOps(SelectionKey.OP_READ);
                }

                if (key.isReadable()) {
                    //DEBUG.log("reading");
                    tcphandler.handleRead(key);
                    //key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                   // key.interestOps(SelectionKey.OP_WRITE);
                }

                keyIter.remove(); // remove from set of selected keys
            }
        }
    }
}