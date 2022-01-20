package misc.download;

import misc.download.strategies.DownloadStrategy;
import misc.messages.HandShake;
import misc.messages.Message;
import misc.messages.PeerMessage;
import misc.peers.ClientState;
import misc.peers.PeerInfo;
import misc.peers.PeerState;
import misc.torrent.*;
import misc.tracker.TrackerHandler;
import misc.utils.DEBUG;
import misc.utils.Utils;

import java.awt.image.WritableRenderedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import static java.lang.System.exit;
import static java.lang.System.in;
import static misc.messages.PeerMessage.MsgType.*;

//TODO : secure mode vs fast mode

/**
 * handles read and write operations to channels
 */
public class TCPMessagesHandler {

    public static int NUMBER_OF_PIECES_PER_REQUEST = 5;  //50mb/S foe 2
    public static int NUMBER_OF_REQUEST_PER_PEER = 60; //Safe mode : 6
    public static int NUMBER_OF_READ_MSG_PER_PEER = 60;   //safe mode : 6

    public NIODownloadHandler peerDownloadHandler;
    public Set<PeerInfo> peerList;
    public TorrentMetaData torrentMetaData;
    public ClientState clientState;
    public Observer observer;
    public TorrentState torrentState;

    public TCPMessagesHandler(TorrentMetaData torrentMetaData, Set<PeerInfo> peerList, ClientState clientState, TorrentState torrentState, Observer observer) {

        this.torrentMetaData = torrentMetaData;
        this.peerDownloadHandler = new NIODownloadHandler(clientState, torrentState, peerList, observer);
        this.peerList = peerList;
        this.clientState = clientState;
        this.observer = observer;
        this.torrentState = torrentState;

    }

    /**
     * search for requests to give to peers
     * @return True if request could be fetched
     */
    public boolean fetchRequests() {

       /* boolean everyoneIsConnected = true;
        for (PeerInfo peer : peerList) {
            PeerState peerState = peer.getPeerState();
            everyoneIsConnected = everyoneIsConnected && peerState.isConnected();
        }
        if (!everyoneIsConnected) {
            return false;
        }*/

        boolean fetched = false;
        if (!TCPClient.torrentContext.getStrategy().getName().equals("ENDGAME")) {
            fetched = generateRequests();
        } else {
            TCPClient.torrentContext.updatePeerState();
        }

        return fetched;
    }


    public boolean generateRequests() {

        boolean sent = false;
        boolean generated;

        if (peerDownloadHandler.getClientState().piecesToRequest.size() < NUMBER_OF_PIECES_PER_REQUEST) {
            generated = TCPClient.torrentContext.updatePeerState();

            Iterator<Integer> it = peerDownloadHandler.getClientState().piecesToRequest.iterator();

            if (it.hasNext()) {
                Integer index = it.next();
                sent = peerDownloadHandler.sendBlockRequests(DownloadStrategy.peersByPieceIndex(peerList, index), index);
            }
        }

        return sent;
    }

    private Message receiveMessage(SocketChannel socketChannel, SelectionKey key) {

        byte[] buffLen = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(buffLen);

        int bRead = 0;
        try {
            while (bRead < 4) {
                bRead += socketChannel.read(buffer);
                if (bRead == -1) {
                    PeerInfo peerInfo = (PeerInfo) key.attachment();
                    PeerState peerState = peerInfo.getPeerState();
                    /*boolean cancelKey = true;

                    for (int i = 0; i < torrentMetaData.getNumberOfPieces(); i++) {
                        if ((!clientState.hasPiece(i) && peerState.hasPiece(i) || (clientState.hasPiece(i) && !peerState.hasPiece(i)))) {
                            cancelKey = false;
                        }
                    }

                    if (cancelKey && peerState.writeMessageQ.isEmpty())
                        cancelKey(key);
                    else {
                        key.interestOps(SelectionKey.OP_WRITE);
                    }*/
                    long nonResponseTime = peerState.nonResponseTime();
                    if (nonResponseTime/1000 >=  10 && nonResponseTime/1000 <= 15){
                        Message keepAlive = new Message(PeerMessage.MsgType.KEEPALIVE);
                        peerState.writeMessageQ.add(keepAlive);
                        if (key.isValid()){
                            key.interestOps(SelectionKey.OP_WRITE);
                        }
                    }else if (nonResponseTime > 1000 * 15){
                        cancelKey(key);
                    }

                    return null;
                }
                if (bRead == 0) {
                    return null;
                }
            }

        } catch (IOException e) {
            System.out.println("remote closed connection");
            cancelKey(key);
            return null;
        } catch (BufferUnderflowException e) {
            DEBUG.printError(e, getClass().getName());
            return null;
        }

        if (bRead != 4)
            return null;

        buffer.flip();
        int len;
        try {
            len = buffer.getInt();
        } catch (BufferUnderflowException e) {
            DEBUG.printError(e, getClass().getName());
            return null;
        }

        if (len < 0)
            return null;
        //buffer.flip();
        byte[] finalData = new byte[4 + len];
        System.arraycopy(buffer.array(), 0, finalData, 0, 4);

        buffer = ByteBuffer.allocate(len);
        int byteRead = 0;
        while (byteRead < len) {
            try {
                byteRead += socketChannel.read(buffer);
            } catch (IOException | BufferUnderflowException e) {
                DEBUG.printError(e, getClass().getName());
                return null;
            }
        }
        //buffer.flip();
        System.arraycopy(buffer.array(), 0, finalData, 4, len);

        if (byteRead != len)
            return null;

        return PeerMessage.deserialize(finalData);
    }

    private void sendMessage(SocketChannel socketChannel, PeerInfo peerInfo, Message message, SelectionKey key) {

        byte[] msg = PeerMessage.serialize(message);
        ByteBuffer writeBuf = ByteBuffer.wrap(msg);
        try {
            socketChannel.write(writeBuf);
        } catch (IOException e) {
            DEBUG.printError(e, getClass().getName());
            DEBUG.logf("--->sending message ", message.toString(), "to peer number", String.valueOf(peerInfo.getPort()));
            if (message.getID() == KEEPALIVE)
                cancelKey(key);
            return;
        }


        DEBUG.logf("--->sent message ", message.toString(), "to peer number", String.valueOf(peerInfo.getPort()));


    }

    public void handleRead(SelectionKey key) {

        SocketChannel clientChannel = (SocketChannel) key.channel();
        PeerInfo peerInfo = (PeerInfo) key.attachment();
        PeerState peerState = peerInfo.getPeerState();


        /*if (peerState.queuedRequestsFromPeer.get() >= NUMBER_OF_REQUEST_PER_PEER && peerState.queuedRequestsFromClient.get() < NUMBER_OF_REQUEST_PER_PEER) {
            System.out.print(".");
            if (key.isValid()) {
                key.interestOps(SelectionKey.OP_WRITE);
            }
            return;
        }*/

        if (!peerState.sentHandshake) {
            HandShake hd = HandShake.readHandshake(clientChannel);
            if (hd == null) {
                if (key.isValid()) {
                    key.interestOps(SelectionKey.OP_READ);
                }
                return;
            }

            DEBUG.logf("<---received handshake ", "from peer number", String.valueOf(peerInfo.getPort()), hd.toString());

            peerState.sentHandshake = true;

            if (!peerState.receivedHandshake) {
                if (key.isValid()) {
                    key.interestOps(SelectionKey.OP_WRITE);
                }

                TCPClient.waitingConnections.add(peerInfo);
            }
            if (!HandShake.validateHandShake(hd, torrentMetaData.getSHA1Info())) {

                System.out.println("handshake invalid, removing peer");
                cancelKey(key);
                peerList.remove(peerInfo);

                try {
                    clientChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return;
            }


            if (key.isValid()) {
                key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            }
            return;
        }
        int receivedMessages = 0;
        Message message;
        do {
            receivedMessages++;
            message = receiveMessage(clientChannel, key);
            if (message == null) {
                if (key.isValid()) {
                    key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                }

                // DEBUG.log("no message received");
                return;
            }

            if (ClientState.isDownloading || ClientState.isSeeder) {
                DEBUG.logf("<---received message ", message.toString(), " from peer number", String.valueOf(peerInfo.getPort()));
            }

            peerDownloadHandler.stateMachine(message, peerState);

            if (key.isValid()) {
                key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            }
        } while (receivedMessages <= NUMBER_OF_READ_MSG_PER_PEER);

    }

    public void handleWrite(SelectionKey key) {

        SocketChannel clientChannel = (SocketChannel) key.channel();

        PeerInfo peerInfo = (PeerInfo) key.attachment();
        PeerState peerState = peerInfo.getPeerState();

        if (peerState.receivedHandshake && !peerState.sentHandshake) {
            if (key.isValid()) {
                key.interestOps(SelectionKey.OP_READ);
            }
            return;
        }
        //HANDSHAKE HANDLING

        if (!peerState.receivedHandshake) {
            DEBUG.logf("--->sending handshake", "to peer number", String.valueOf(peerInfo.getPort()));
            HandShake sentHand = new HandShake(Utils.hexStringToByteArray(torrentMetaData.getSHA1Info()), Utils.hexStringToByteArray(TrackerHandler.PEER_ID));
            ByteBuffer writeBuf = ByteBuffer.wrap(sentHand.createHandshakeMsg());
            try {
                clientChannel.write(writeBuf);
            } catch (IOException e) {
                System.err.println("error cannot send handshake");
                DEBUG.printError(e, getClass().getName());
                return;
            }

            //our peer received our handshake
            peerState.receivedHandshake = true;
            if (!peerState.sentHandshake) {
                TCPClient.waitingConnections.add(peerInfo);
                if (key.isValid()) {
                    key.interestOps(SelectionKey.OP_READ);
                }
            }

            return;
        } else {
            if (!peerState.welcomeQ.isEmpty()) {
                while (!peerState.welcomeQ.isEmpty()) {
                    Message writeMessage = peerState.welcomeQ.poll();
                    sendMessage(clientChannel, peerInfo, writeMessage, key);
                    peerDownloadHandler.sentStateMachine(writeMessage, peerState);
                }
                TCPClient.waitingConnections.remove(peerInfo);
            } else if (!peerState.writeMessageQ.isEmpty()) {
                if (peerState.weAreChokedByPeer && peerState.writeMessageQ.peek().ID == PeerMessage.MsgType.REQUEST) {
                    if (key.isValid()) {
                        key.interestOps(SelectionKey.OP_READ);
                    }
                    return;
                }

                while (!peerState.writeMessageQ.isEmpty() && peerState.queuedRequestsFromClient.get() < NUMBER_OF_REQUEST_PER_PEER) {
                    Message writeMessage = peerState.writeMessageQ.poll();

                    if (writeMessage == null)
                        System.err.println("null message to write");
                    else if (writeMessage.getID() == PeerMessage.MsgType.REQUEST) {

                        int pid = writeMessage.getIndex();
                        int bid = writeMessage.getBegin() / torrentState.BLOCK_SIZE;
                        Piece piece = torrentState.pieces.get(pid);
                        if (!(piece.getBlocks().get(bid) == BlockStatus.Downloaded) && !clientState.hasPiece(pid)) {
                            sendMessage(clientChannel, peerInfo, writeMessage, key);
                            peerDownloadHandler.sentStateMachine(writeMessage, peerState);
                        }

                    } else {
                        sendMessage(clientChannel, peerInfo, writeMessage, key);
                        peerDownloadHandler.sentStateMachine(writeMessage, peerState);
                    }

                }
                if (peerState.queuedRequestsFromClient.get() > 0) {
                    if (key.isValid()) {
                        key.interestOps(SelectionKey.OP_READ);
                    }
                    return;
                }
            }
        }
        if (key.isValid()) {
            key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        }

    }

    public void handleConnection(SelectionKey key) {

        SocketChannel clientChannel = (SocketChannel) key.channel();
        PeerInfo peerInfo = (PeerInfo) key.attachment();
        PeerState peerState = peerInfo.getPeerState();

        boolean isConnected;
        try {
            DEBUG.log("connecting to " + clientChannel.getRemoteAddress());
            isConnected = clientChannel.finishConnect();

            if (!isConnected) {
                DEBUG.loge("cannot connect to " + clientChannel.getRemoteAddress());
                return;
            }


            Message bitfield = new Message(PeerMessage.MsgType.BITFIELD, clientState.getBitfield());
            peerState.welcomeQ.add(bitfield);

        } catch (IOException connectException) {
            removePeer(peerInfo);
            try {
                clientChannel.close();
            } catch (IOException e) {
                DEBUG.printError(e, getClass().getName());
            }

            System.out.println("peer cannot connect, removing key");
            cancelKey(key);
            return;
        }

        try {
            if (clientChannel.finishConnect()) {
                if (key.isValid()) {
                    key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                }
            }

        } catch (IOException e) {
            DEBUG.printError(e, getClass().getName());
            removePeer(peerInfo);

            try {
                clientChannel.close();
            } catch (IOException ex) {
                DEBUG.printError(e, getClass().getName());
            }
            System.out.println("cannot connect to peer, removing key");
            cancelKey(key);

        }

    }


    public void cancelKey(SelectionKey key) {
        System.out.println("canceling key");
        key.cancel();
        PeerInfo peerInfo = (PeerInfo) key.attachment();
        removePeer(peerInfo);
        //exit(0);

    }

    public void handleAccept(SelectionKey key) {
        SocketChannel clientChannel;
        try {
            clientChannel = ((ServerSocketChannel) key.channel()).accept();
            clientChannel.configureBlocking(false);
            int port = clientChannel.socket().getPort();
            InetAddress address = clientChannel.socket().getInetAddress();
            PeerInfo peerInfo = new PeerInfo(address, port, torrentMetaData.getNumberOfPieces());

            clientChannel.register(key.selector(), SelectionKey.OP_READ, peerInfo);
            //peerList.add(new PeerInfo(InetAddress.getLocalHost(), port, torrentMetaData.getNumberOfPieces()));
            addPeer(peerInfo);


            PeerState peerState = peerInfo.getPeerState();
            Message bitfield = new Message(PeerMessage.MsgType.BITFIELD, clientState.getBitfield());
            peerState.welcomeQ.add(bitfield);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addPeer(PeerInfo peerInfo) {
        peerList.add(peerInfo);
    }

    public void removePeer(PeerInfo peerInfo) {

        if (peerInfo.getPeerState().sentBitfield)
            observer.notifyAllObserves(Events.PEER_DISCONNECTED, peerInfo.getPeerState());
        //give his request messages to others
        peerList.remove(peerInfo);

        Stack<Integer> pieceToRemove = new Stack<>();
        for (Integer pid : clientState.piecesToRequest) {
            Piece piece = torrentState.pieces.get(pid);
            if (!clientState.hasPiece(pid)) {
                piece.setPieceStatus(PieceStatus.ToBeDownloaded);
                pieceToRemove.add(pid);
            }
        }

        for (Integer pid : pieceToRemove) {
            clientState.piecesToRequest.remove(pid);
        }

        for (Message message : peerInfo.getPeerState().writeMessageQ) {
            if (message.getID() == PeerMessage.MsgType.REQUEST) {
                int id = message.getIndex();
                Piece piece = torrentState.pieces.get(id);
                int blockId = message.getBegin() / torrentState.BLOCK_SIZE;
                piece.getBlocks().set(blockId, BlockStatus.ToBeDownloaded);
                if (!clientState.hasPiece(id))
                    piece.setPieceStatus(PieceStatus.ToBeDownloaded);
                List<PeerInfo> valuablePeers = DownloadStrategy.peersByPieceIndex(peerList, id);
                if (valuablePeers.size() != 0)
                    valuablePeers.get(0).getPeerState().writeMessageQ.addFirst(message);
            }
        }

    }

}
