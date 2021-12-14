package misc.peers;

import misc.Main;
import misc.torrent.TorrentFileHandler;
import misc.torrent.TorrentMetaData;
import misc.tracker.TrackerHandler;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class TCPClient {

    public static void main(String args[]) throws Exception {

        TorrentFileHandler torrentHandler = null;
        TorrentMetaData torrentMetaData = null;

        torrentHandler = new TorrentFileHandler(new FileInputStream(args[0]));
        torrentMetaData = torrentHandler.ParseTorrent();
        System.out.println(torrentMetaData);

        URL announceURL = new URL(torrentMetaData.getAnnounceUrlString());
        TrackerHandler tracker = new TrackerHandler(announceURL, torrentMetaData.getSHA1InfoByte(), 12315, torrentMetaData.getNumberOfPieces());

        String server = "127.0.0.1";

        Selector selector = Selector.open();

        TCPHANDLER tcphandler = new TCPHANDLER(torrentMetaData);

        for (int i = 0; i < 2; i++) {
            SocketChannel clntChan = SocketChannel.open();
            clntChan.configureBlocking(false);
            clntChan.register(selector, SelectionKey.OP_CONNECT);
            //launch a connection to a peer
            boolean flag = clntChan.connect(new InetSocketAddress(server, (i == 0 ? Main.PORTCLIENT.VUZE.port : Main.PORTCLIENT.QBITTORRENT.port)));
            assert (flag);
        }


        while (true) {
            if (selector.select(3000) == 0) { // returns # of ready chans
                System.out.print(".");
                continue;
            }

            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next(); // Key is bit mask

                if (key.isConnectable()) {

                    SocketChannel clntChan = (SocketChannel) key.channel();

                    System.out.println(clntChan.getRemoteAddress());

                    clntChan.finishConnect();

                    key.interestOps(SelectionKey.OP_WRITE);

                }

                if (key.isValid() && key.isWritable()) {

                    tcphandler.handleWrite(key);

                }

                if (key.isReadable()) {
                    tcphandler.handleRead(key);

                }

                keyIter.remove(); // remove from set of selected keys
            }
        }
    }
}