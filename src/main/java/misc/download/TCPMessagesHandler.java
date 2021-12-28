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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;


public class TCPMessagesHandler {

    public static final int NUMBER_OF_PIECES_PER_REQUEST = 1;
    public static final int NUMBER_OF_REQUEST_PER_PEER = 10;

    public NIODownloadHandler peerDownloadHandler;
    public List<PeerInfo> peerList;
    public TorrentMetaData torrentMetaData;
    public ClientState clientState;


    public TCPMessagesHandler(TorrentMetaData torrentMetaData, List<PeerInfo> peerList, ClientState clientState, TorrentState torrentState, Observer observer) {

        this.torrentMetaData = torrentMetaData;
        this.peerDownloadHandler = new NIODownloadHandler(torrentMetaData, clientState, torrentState, peerList, observer);
        this.peerList = peerList;
        this.clientState = clientState;
        //TODO : tracker should not return our client as a peer
    }

    public  boolean fetchRequests() {

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

    private boolean sendMessage(SocketChannel socketChannel, Message message) {
        byte[] msg = PeerMessage.serialize(message);
        ByteBuffer writeBuf = ByteBuffer.wrap(msg);
        try {
            socketChannel.write(writeBuf);
        } catch (IOException e) {
            DEBUG.printError(e, getClass().getName());
            return false;
        }
        DEBUG.log("sent message ", message.toString(), "to peer number", String.valueOf(TCPClient.channelIntegerMap.get(socketChannel.socket().getPort())));

        return true;
    }

    public void handleRead(SelectionKey key) {
        SocketChannel clntChan = (SocketChannel) key.channel();
        int peerIndex = TCPClient.channelIntegerMap.get(clntChan.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();

        if (!peerState.sentHandshake) {
            HandShake hd = HandShake.readHandshake(clntChan);
            if (hd == null) {
                key.interestOps(SelectionKey.OP_READ);
                return;
            }

            DEBUG.log("received handshake ", "from peer number", String.valueOf(peerIndex), hd.toString());
            peerState.sentHandshake = true;

            if (!peerState.receivedHandshake) {
                key.interestOps(SelectionKey.OP_WRITE);
                TCPClient.waitingConnections.add(peerList.get(peerIndex));
            } else {
                if (!HandShake.validateHandShake(hd, torrentMetaData.getSHA1Info())) {
                    //TODO : cancel connexion
                    key.cancel();
                    peerState.killed = true;
                    try {
                        clntChan.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return;
        }
        Message message = receiveMessage(clntChan, key);
        if (message == null) {
            // DEBUG.log("no message received");
            return;
        }

        DEBUG.log("recieved message ", message.toString(), " from peer number", String.valueOf(peerIndex));

        if (message.getID() == PeerMessage.MsgType.PIECE) {
            peerState.waitingRequests--;
            peerState.requestReceivedFromPeer++;
        }


        peerDownloadHandler.stateMachine(message, peerState);

        key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);

    }

    public void handleWrite(SelectionKey key) {

        SocketChannel clientChannel = (SocketChannel) key.channel();
        int peerIndex = TCPClient.channelIntegerMap.get(clientChannel.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();

        if (peerState.receivedHandshake && !peerState.sentHandshake) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }
        //HANDSHAKE HANDLING
        if (!peerState.receivedHandshake) {
            DEBUG.log("sending handshake", "too peer number", String.valueOf(peerIndex));
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
                if (peerState.weAreChokedByPeer) {
                    key.interestOps(SelectionKey.OP_READ);
                    return;
                }
                while (!peerState.writeMessageQ.isEmpty() && peerState.waitingRequests < NUMBER_OF_REQUEST_PER_PEER) {
                    Message writeMessage = peerState.writeMessageQ.poll();
                    if (writeMessage == null)
                        System.out.println("null message to write");
                    else {
                        sendMessage(clientChannel, writeMessage);
                        if (writeMessage.getID() == PeerMessage.MsgType.REQUEST) {
                            peerState.waitingRequests++;
                            peerState.numberOfRequests++;
                            peerDownloadHandler.getTorrentState().getStatus().set(writeMessage.getIndex(), PieceStatus.Requested);
                        } else {
                            break;
                        }
                    }
                }
                if (peerState.waitingRequests > 0) {
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
            Message unchoke = new Message(PeerMessage.MsgType.UNCHOKE);
            peerState.welcomeQ.add(unchoke);
            Message interested = new Message(PeerMessage.MsgType.INTERESTED);
            peerState.welcomeQ.add(interested);


        } catch (IOException connectException) {
            peerState.killed = true;
            try {
                clientChannel.close();
            } catch (IOException e) {
                DEBUG.printError(e, getClass().getName());
            }
            key.cancel();
        }


        try {
            if (clientChannel.finishConnect())
                key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        } catch (IOException e) {
            DEBUG.printError(e, getClass().getName());
            peerState.killed = true;
            try {
                clientChannel.close();
            } catch (IOException ex) {
                DEBUG.printError(e, getClass().getName());
            }
            key.cancel();
        }

    }

    //todo : method to decide the next WRITE/READ INTEREST OPERATIONS

}
