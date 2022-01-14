package misc.download;

import misc.download.strategies.DownloadStrat;
import misc.messages.HandShake;
import misc.messages.Message;
import misc.messages.PeerMessage;
import misc.peers.ClientState;
import misc.peers.PeerInfo;
import misc.peers.PeerState;
import misc.torrent.Observer;
import misc.torrent.PieceStatus;
import misc.torrent.TorrentMetaData;
import misc.torrent.TorrentState;
import misc.tracker.TrackerHandler;
import misc.utils.DEBUG;
import misc.utils.Utils;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//TODO : welcome Messages, should depend upon the state of our torrent file, the state of the peers
//TODO : timeout handshake
public class TCPMessagesHandler {

    public static final int NUMBER_OF_PIECES_PER_REQUEST = 1;
    public static final int NUMBER_OF_REQUEST_PER_PEER = 100;

    public NIODownloadHandler peerDownloadHandler;
    public List<PeerInfo> peerList;
    public TorrentMetaData torrentMetaData;
    public ClientState clientState;


    public TCPMessagesHandler(TorrentMetaData torrentMetaData, List<PeerInfo> peerList, ClientState clientState, TorrentState torrentState, Observer observer) {

        this.torrentMetaData = torrentMetaData;
        this.peerDownloadHandler = new NIODownloadHandler(torrentMetaData, clientState, torrentState, peerList, observer);
        this.peerList = peerList;
        this.clientState = clientState;

    }

    public boolean fetchRequests() {

        boolean everyoneIsConnected = true;
        for (PeerInfo peer : peerList) {
            PeerState peerState = peer.getPeerState();
            if (peerState.killed)
                continue;
            everyoneIsConnected = everyoneIsConnected && peerState.isConnected();
        }
        if (!everyoneIsConnected) {
            return false;
        }

        boolean fetched;
        if (!TCPClient.torrentContext.getStrategy().getName().equals("ENDGAME")) {
            fetched = generateRequests();
        } else {
            fetched = true;
        }

        return fetched;
    }

    public boolean generateRequests() {
        boolean sent = false;
        if (peerDownloadHandler.getClientState().piecesToRequest.size() < NUMBER_OF_PIECES_PER_REQUEST) {
            TCPClient.torrentContext.updatePeerState();

            Iterator<Integer> it = peerDownloadHandler.getClientState().piecesToRequest.iterator();

            if (it.hasNext()) {
                Integer index = it.next();
                sent = peerDownloadHandler.sendBlockRequests(DownloadStrat.peersByPieceIndex(peerList, index), index);
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
                    socketChannel.close();
                    key.cancel();
                    return null;
                }
                if (bRead == 0) {
                    return null;
                }
            }

        } catch (IOException e) {
            System.err.println("remote closed connection");
            key.cancel();
            return null;
        } catch (BufferUnderflowException e) {
            DEBUG.printError(e, getClass().getName());
            return null;
        }

        assert (bRead == 4);
        buffer.flip();
        int len;
        try {
            len = buffer.getInt();
        } catch (BufferUnderflowException e) {
            DEBUG.printError(e, getClass().getName());
            return null;
        }

        //buffer.flip();
        byte[] finalData = new byte[4 + len];
        for (int i = 0; i < 4; i++)
            finalData[i] = buffer.array()[i];

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
        for (int i = 0; i < len; i++)
            finalData[i + 4] = buffer.array()[i];

        if (byteRead != len)
            return null;

        return PeerMessage.deserialize(finalData);
    }

    private void sendMessage(SocketChannel socketChannel, Message message) {
        byte[] msg = PeerMessage.serialize(message);
        ByteBuffer writeBuf = ByteBuffer.wrap(msg);
        try {
            socketChannel.write(writeBuf);
        } catch (IOException e) {
            DEBUG.printError(e, getClass().getName());
            return;
        }
        DEBUG.log("--->sent message ", message.toString(), "to peer number", String.valueOf(TCPClient.channelIntegerMap.get(socketChannel.socket().getPort())));

    }

    public void handleRead(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        /*int peerIndex = TCPClient.channelIntegerMap.get(clientChannel.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();*/

        PeerInfo peerInfo = (PeerInfo) key.attachment();
        PeerState peerState = peerInfo.getPeerState();
        int peerIndex = peerInfo.index;


        if (peerState.queuedRequestsFromPeer.get()>= NUMBER_OF_REQUEST_PER_PEER && peerState.queuedRequestsFromClient.get() < NUMBER_OF_REQUEST_PER_PEER)
        {
            key.interestOps(SelectionKey.OP_WRITE);
            return;
        }

        if (!peerState.sentHandshake) {
            HandShake hd = HandShake.readHandshake(clientChannel);
            if (hd == null) {
                key.interestOps(SelectionKey.OP_READ);
                return;
            }

            DEBUG.log("<---received handshake ", "from peer number", String.valueOf(peerIndex), hd.toString());
            peerState.sentHandshake = true;

            if (!peerState.receivedHandshake) {
                key.interestOps(SelectionKey.OP_WRITE);
                TCPClient.waitingConnections.add(peerInfo);
            } else {
                if (!HandShake.validateHandShake(hd, torrentMetaData.getSHA1Info())) {

                    key.cancel();
                    peerState.killed = true;
                    try {
                        clientChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            //TODO : last edit
            key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            return;
        }
        Message message = receiveMessage(clientChannel, key);
        if (message == null) {
            //TODO : last edit
            //key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            // DEBUG.log("no message received");
            return;
        }

        DEBUG.log("<---recieved message ", message.toString(), " from peer number", String.valueOf(peerIndex));

        if (message.getID() == PeerMessage.MsgType.PIECE) {
            peerState.queuedRequestsFromClient.decrementAndGet();
            peerState.numberOfBlocksSent++;
        }
        if (message.getID() == PeerMessage.MsgType.REQUEST) {
            peerState.numberOfRequestsReceived++;
            peerState.queuedRequestsFromPeer.incrementAndGet();
        }



        peerDownloadHandler.stateMachine(message, peerState);

        key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);

    }

    public void handleWrite(SelectionKey key) {

        SocketChannel clientChannel = (SocketChannel) key.channel();
       /* int peerIndex = TCPClient.channelIntegerMap.get(clientChannel.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();*/

        PeerInfo peerInfo = (PeerInfo) key.attachment();
        PeerState peerState = peerInfo.getPeerState();
        int peerIndex = peerInfo.index;

        if (peerState.receivedHandshake && !peerState.sentHandshake) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }
        //HANDSHAKE HANDLING
        //TODO : compare handshakes
        if (!peerState.receivedHandshake) {
            DEBUG.log("--->sending handshake", "too peer number", String.valueOf(peerIndex));
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
                TCPClient.waitingConnections.add(peerList.get(peerIndex));
                key.interestOps(SelectionKey.OP_READ);
            }

            return;
        } else {
            if (!peerState.welcomeQ.isEmpty()) {
                while (!peerState.welcomeQ.isEmpty()) {
                    Message writeMessage = peerState.welcomeQ.poll();
                    sendMessage(clientChannel, writeMessage);
                }
                TCPClient.waitingConnections.remove(peerList.get(peerIndex));
            } else if (!peerState.writeMessageQ.isEmpty()) {
                if (peerState.weAreChokedByPeer && peerState.writeMessageQ.peek().ID == PeerMessage.MsgType.REQUEST) {
                    key.interestOps(SelectionKey.OP_READ);
                    return;
                }

                //TODO : NUMBER OF UNTREATED REQUESTS SHOULD BE BOUNDED, AKA DO THE SAME THING HERE IN THE CASE OF READ REQUESTS

                while (!peerState.writeMessageQ.isEmpty() && peerState.queuedRequestsFromClient.get() < NUMBER_OF_REQUEST_PER_PEER) {
                    Message writeMessage = peerState.writeMessageQ.poll();
                    if (writeMessage == null)
                        System.err.println("null message to write");
                    else {
                        sendMessage(clientChannel, writeMessage);
                        if (writeMessage.getID() == PeerMessage.MsgType.REQUEST) {
                            peerState.queuedRequestsFromClient.incrementAndGet();
                            peerState.numberOfRequests++;
                            peerDownloadHandler.getTorrentState().getStatus().set(writeMessage.getIndex(), PieceStatus.Requested);
                        } else if (writeMessage.getID() == PeerMessage.MsgType.PIECE) {
                            peerState.numberOfBlocksReceived++;
                            peerState.queuedRequestsFromPeer.decrementAndGet();
                        } else {
                            break;
                        }
                    }
                }
                if (peerState.queuedRequestsFromClient.get() > 0) {
                    key.interestOps(SelectionKey.OP_READ);
                    return;
                }
            }
        }
        key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);

    }

    public void handleConnection(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        int peerIndex = TCPClient.channelIntegerMap.get(clientChannel.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();

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
            //TODO : test
            /*Message unchoke = new Message(PeerMessage.MsgType.UNCHOKE);
            peerState.welcomeQ.add(unchoke);
            Message interested = new Message(PeerMessage.MsgType.INTERESTED);
            peerState.welcomeQ.add(interested);*/


        } catch (IOException connectException) {
            removePeer(peerList.get(peerIndex));
            try {
                clientChannel.close();
            } catch (IOException e) {
                DEBUG.printError(e, getClass().getName());
            }

            key.cancel();
            return;
        }


        try {
            if (clientChannel.finishConnect())
                key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        } catch (IOException e) {
            DEBUG.printError(e, getClass().getName());
            removePeer(peerList.get(peerIndex));

            try {
                clientChannel.close();
            } catch (IOException ex) {
                DEBUG.printError(e, getClass().getName());
            }
            key.cancel();

        }

    }

    public void handleAccept(SelectionKey key, Map<Integer, Integer> channelIntegerMap) {
        SocketChannel clientChannel;
        try {
            clientChannel = ((ServerSocketChannel) key.channel()).accept();
            clientChannel.configureBlocking(false);
            int port = clientChannel.socket().getPort();
            InetAddress address = clientChannel.socket().getInetAddress();
            PeerInfo peerInfo = new PeerInfo(address, port, torrentMetaData.getNumberOfPieces());

            clientChannel.register(key.selector(), SelectionKey.OP_READ , peerInfo);
            //peerList.add(new PeerInfo(InetAddress.getLocalHost(), port, torrentMetaData.getNumberOfPieces()));
            addPeer(peerInfo);
            channelIntegerMap.put(port, peerList.size() - 1);
            peerInfo.index = peerList.size() - 1;

            PeerState peerState = peerList.get(peerList.size() - 1).getPeerState();
            Message bitfield = new Message(PeerMessage.MsgType.BITFIELD, clientState.getBitfield());
            peerState.welcomeQ.add(bitfield);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  void addPeer(PeerInfo peerInfo){
        if (TCPClient.peerInfoSet.contains(peerInfo))
            return;
        TCPClient.peerInfoSet.add(peerInfo);
        peerList.add(peerInfo);
        peerInfo.getPeerState().isLeecher = true;
    }

    public  void removePeer(PeerInfo peerInfo){
        TCPClient.peerInfoSet.remove(peerInfo);
        peerInfo.getPeerState().killed = true;
    }

}
