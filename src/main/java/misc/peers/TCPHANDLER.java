package misc.peers;

import misc.messages.Message;
import misc.messages.PeerMessage;
import misc.torrent.TorrentMetaData;
import misc.torrent.TorrentState;
import misc.tracker.TrackerHandler;
import misc.utils.DEBUG;
import misc.utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;

public class TCPHANDLER {
    //public boolean requested = false;
    //public boolean weAreChokedByPeer = true;

    public NIODownloadHandler peerDownloadHandler;
    public List<PeerInfo> peerList;
    public TorrentMetaData torrentMetaData;
    public ClientState clientState;

    //map port -> index of peer in List<PeerInfo>
    public final Map<Integer, Integer> channelIntegerMap = new HashMap<>();


    public TCPHANDLER(TorrentMetaData torrentMetaData, List<PeerInfo> peerList, ClientState clientState, TorrentState torrentState) {

        this.torrentMetaData = torrentMetaData;
        this.peerDownloadHandler = new NIODownloadHandler(torrentMetaData, clientState, torrentState, peerList);
        this.peerList = peerList;
        this.clientState = clientState;
        //TODO : tracker should not return our client as a peer
        //peerDownloadHandler.leechTorrent(peerList);
    }

    private Message receiveMessage(SocketChannel socketChannel, SelectionKey key) {
        System.err.println("here");
        byte[] buffLen = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(buffLen);

        int bRead = 0;
        try {
            while (bRead < 4)
            {
                bRead += socketChannel.read(buffer);
                if( bRead == 0 || bRead == -1){
                    return null;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();

        }

        assert (bRead == 4);
        buffer.flip();
        int len = buffer.getInt();
        //buffer.flip();
        byte[] finalData = new byte[4 + len];
        for (int i = 0; i < 4; i++)
            finalData[i] = buffer.array()[i];

        buffer = ByteBuffer.allocate(len);
        int byteRead = 0;
        while (byteRead < len) {
            try {
                byteRead += socketChannel.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //buffer.flip();
        for (int i = 0; i < len; i++)
            finalData[i + 4] = buffer.array()[i];

        assert (byteRead == len);

        return PeerMessage.deserialize(finalData);
    }

    private void sendMessage(SocketChannel socketChannel, Message message, SelectionKey key) {
        byte[] msg = PeerMessage.serialize(message);
        ByteBuffer writeBuf = ByteBuffer.wrap(msg);
        try {
            socketChannel.write(writeBuf);
        } catch (IOException e) {
            System.err.println("error : " + e.getMessage());
            e.printStackTrace();
        }
/*        switch (message.getID()){
            case BITFIELD:
            case INTERESTED:
                key.interestOps(SelectionKey.OP_WRITE);
                break;

            case REQUEST:
                key.interestOps(SelectionKey.OP_READ);
                break;
            case HAVE:
                key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                break;

        }*/
        DEBUG.log("sent message ", message.getID().toString(), "to peer number", String.valueOf(channelIntegerMap.get(socketChannel.socket().getPort())));
    }

    public void handleRead(SelectionKey key) {
        System.err.println("read");
        SocketChannel clntChan = (SocketChannel) key.channel();
        int peerIndex = channelIntegerMap.get(clntChan.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();
        //DEBUG.log("peerIndex", String.valueOf(peerIndex));

        if (!peerState.sentHandshake) {
            HandShake hd = HandShake.readHandshake(clntChan);
            if (hd == null){
                key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                return;
            }

            DEBUG.log("received handshake ", "from peer number", String.valueOf(peerIndex), hd.toString());
            peerState.sentHandshake = true;

            if (!peerState.receivedHandshake)
                key.interestOps(SelectionKey.OP_WRITE);
            key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);

        } else {
            DEBUG.log("recieving message", "from peer number", String.valueOf(peerIndex));
            Message message = receiveMessage(clntChan, key);
            if (message == null){
                DEBUG.log("no message received");
                key.interestOps(SelectionKey.OP_WRITE);
                return;
            }

            DEBUG.log("recieved message ", message.getID().toString(), "from peer number", String.valueOf(peerIndex));

            if (message.getID() == PeerMessage.MsgType.UNCHOKE) {
                peerState.weAreChokedByPeer = false;
            }

            /* DEBUG.log("the bitfield payload", Utils.bytesToHex(message.getPayload()));*/
            //send message to statemachine and handle it
            peerDownloadHandler.messageHandler(message, peerState);
            key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        }

    }

    //TODO : we need to write only a small number of requests
    public void handleWrite(SelectionKey key) {
        System.err.println("write");
        SocketChannel clntChan = (SocketChannel) key.channel();
        int peerIndex = channelIntegerMap.get(clntChan.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();
        if (peerState.receivedHandshake && !peerState.sentHandshake){
            key.interestOps(SelectionKey.OP_READ);
            return;
        }
        if (!peerState.receivedHandshake) {
            DEBUG.log("sending handshake", "too peer number", String.valueOf(peerIndex));
            HandShake sentHand = new HandShake(Utils.hexStringToByteArray(torrentMetaData.getSHA1Info()), Utils.hexStringToByteArray(TrackerHandler.PEER_ID));
            ByteBuffer writeBuf = ByteBuffer.wrap(sentHand.createHandshakeMsg());
            try {
                clntChan.write(writeBuf);
            } catch (IOException e) {
                System.err.println("error cannot send handshake");
                return;
            }
            //our peer received our handshake
            peerState.receivedHandshake = true;
            if (!peerState.sentHandshake)
                key.interestOps(SelectionKey.OP_READ);
            else
                key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            return;
        }
        else{
            if (!peerState.writeMessageQ.isEmpty()){
                Message writeMessage = peerState.writeMessageQ.poll();
                if (writeMessage == null)
                    System.out.println("null message");
                else{
                    sendMessage(clntChan, writeMessage, key);
                    if (writeMessage.getID() == PeerMessage.MsgType.REQUEST)
                    {
                        key.interestOps(SelectionKey.OP_READ);
                        return;
                    }

                }
            }
            //the messageQueue is empty, so we check if we can make a new request
            else{
                //we have no request to send, we wait for messages from peer
                if(!handleRequest(key)){
                    key.interestOps(SelectionKey.OP_READ);
                    return;
                }
            }
        }
        key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);

    }

    public boolean handleConnection(SelectionKey key){
        SocketChannel clntChan = (SocketChannel) key.channel();
        int peerIndex = channelIntegerMap.get(clntChan.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();

        boolean isConnected = false;
        try {
            DEBUG.log("connecting to " + clntChan.getRemoteAddress());
            isConnected = clntChan.finishConnect();

            if (!isConnected){
                DEBUG.loge("cannot connect to " + clntChan.getRemoteAddress());
                return false;
            }


            Message bitfield = new Message(PeerMessage.MsgType.BITFIELD, clientState.getBitfield());
            peerState.writeMessageQ.add(bitfield);
            Message unchoke = new Message(PeerMessage.MsgType.UNCHOKE);
            peerState.writeMessageQ.add(unchoke);
            Message interested = new Message(PeerMessage.MsgType.INTERESTED);
            peerState.writeMessageQ.add(interested);

            key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);


        } catch (IOException e) {
            e.printStackTrace();
        }


        return isConnected;
    }

    //TODO : algo de selection des pieces
    public boolean handleRequest(SelectionKey key){
        SocketChannel clntChan = (SocketChannel) key.channel();
        int peerIndex = channelIntegerMap.get(clntChan.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();

        if (peerState.weAreChokedByPeer)
        {
            //need to get unchoked
            Message unchoke = new Message(PeerMessage.MsgType.UNCHOKE);
            peerState.writeMessageQ.add(unchoke);

            //wait untill we are unchoked
            key.interestOps(SelectionKey.OP_READ);
            return true;
        }

        Iterator<Integer> it = peerState.piecesToRequest.iterator();
        boolean sent = false;
        while (it.hasNext()){
            Integer index = it.next();
            if (clientState.hasPiece(index))
            {
                //DEBUG.log("on a deja cette piece");
                continue;
            }
            else{
                //System.err.println("sending request for whole piece " + index);
                sent = sent || peerDownloadHandler.sendFullPieceRequest(index, peerState);
            }
        }

        return sent;
    }


}
