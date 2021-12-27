package misc.download;

import misc.messages.Message;
import misc.messages.PeerMessage;
import misc.peers.ClientState;
import misc.messages.HandShake;
import misc.peers.PeerInfo;
import misc.peers.PeerState;
import misc.torrent.*;
import misc.torrent.Observer;
import misc.tracker.TrackerHandler;
import misc.utils.DEBUG;
import misc.utils.Utils;

import java.io.IOException;
import java.nio.BufferUnderflowException;
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


    public TCPHANDLER(TorrentMetaData torrentMetaData, List<PeerInfo> peerList, ClientState clientState, TorrentState torrentState, Observer observer) {

        this.torrentMetaData = torrentMetaData;
        this.peerDownloadHandler = new NIODownloadHandler(torrentMetaData, clientState, torrentState, peerList, observer);
        this.peerList = peerList;
        this.clientState = clientState;
        //TODO : tracker should not return our client as a peer
        //peerDownloadHandler.leechTorrent(peerList);
    }

    private Message receiveMessage(SocketChannel socketChannel, SelectionKey key) {
        byte[] buffLen = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(buffLen);

        int bRead = 0;
        try {
            while (bRead < 4)
            {
                bRead += socketChannel.read(buffer);
                if(bRead == -1){
                    socketChannel.close();
                    key.cancel();
                    return null;
                }
                if (bRead == 0){
                    return null;
                }
            }

        } catch (IOException e){
            System.err.println("remote closed connection");
            key.cancel();
            return null;
        } catch(BufferUnderflowException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            return null;
        }

        assert (bRead == 4);
        buffer.flip();
        int len = 0;
        try{
            len = buffer.getInt();
        }catch (BufferUnderflowException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
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
                e.printStackTrace();
                System.err.println(e.getMessage());
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

    private boolean sendMessage(SocketChannel socketChannel, Message message, SelectionKey key) {
        byte[] msg = PeerMessage.serialize(message);
        ByteBuffer writeBuf = ByteBuffer.wrap(msg);
        try {
            socketChannel.write(writeBuf);
        } catch (IOException e) {
            System.err.println("error : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        DEBUG.log("sent message ", message.getID().toString(), "to peer number", String.valueOf(channelIntegerMap.get(socketChannel.socket().getPort())));
        if (message.getID() == PeerMessage.MsgType.REQUEST)
            DEBUG.log("piece requested : " + message.getIndex());
        if (message.getID() == PeerMessage.MsgType.HAVE)
            DEBUG.log("have number : " + message.getIndex());



        return true;
    }

    int btfld = 0;
    public void handleRead(SelectionKey key) {
        //System.err.println("read");
        SocketChannel clntChan = (SocketChannel) key.channel();
        int peerIndex = channelIntegerMap.get(clntChan.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();
        //DEBUG.log("peerIndex", String.valueOf(peerIndex));

        if (!peerState.sentHandshake) {
            HandShake hd = HandShake.readHandshake(clntChan);
            if (hd == null){
               key.interestOps(SelectionKey.OP_READ);
                return;
            }

            DEBUG.log("received handshake ", "from peer number", String.valueOf(peerIndex), hd.toString());
            peerState.sentHandshake = true;

            if (!peerState.receivedHandshake)
            {
                key.interestOps(SelectionKey.OP_WRITE);
                //TCPClient.waitingConnections.add(peerList.get(peerIndex));
            }

            return;
            //key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        }
            //DEBUG.log("recieving message", "from peer number", String.valueOf(peerIndex));
        Message message = receiveMessage(clntChan, key);
        if (message == null){
            DEBUG.log("no message received");
            return;
        }

        DEBUG.log("recieved message ", message.getID().toString(), (message.getID() == PeerMessage.MsgType.PIECE ? String.valueOf(message.getIndex()) : ""), " from peer number", String.valueOf(peerIndex));

        if (message.getID() == PeerMessage.MsgType.PIECE){
            peerState.waitingRequests--;
        }


        peerDownloadHandler.messageHandler(message, peerState);

        key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);

    }

    //TODO : we need to write only a small number of requests
    public void handleWrite(SelectionKey key) {
        //System.err.println("write");
        SocketChannel clntChan = (SocketChannel) key.channel();
        int peerIndex = channelIntegerMap.get(clntChan.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();

        if (peerState.receivedHandshake && !peerState.sentHandshake){
            key.interestOps(SelectionKey.OP_READ);
            return;
        }
        //HANDSHAKE HANDLING
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
            if (!peerState.sentHandshake){
                TCPClient.waitingConnections.add(peerList.get(peerIndex));
                key.interestOps(SelectionKey.OP_READ);
            }

            return;
        }

        else{
            if (!peerState.welcomeQ.isEmpty()){
                while(!peerState.welcomeQ.isEmpty()){
                    Message writeMessage = peerState.welcomeQ.poll();
                    sendMessage(clntChan, writeMessage, key);
                }
                TCPClient.waitingConnections.remove(peerList.get(peerIndex));
            }

            else if (!peerState.writeMessageQ.isEmpty()){
                if (peerState.weAreChokedByPeer) {
                    key.interestOps(SelectionKey.OP_READ);
                    return;
                }
                while (!peerState.writeMessageQ.isEmpty() && peerState.waitingRequests < 10) {
                    Message writeMessage = peerState.writeMessageQ.poll();
                    if (writeMessage == null)
                        System.out.println("null message to write");
                    else{
                        sendMessage(clntChan, writeMessage, key);
                        if (writeMessage.getID() == PeerMessage.MsgType.REQUEST)
                        {
                            peerState.waitingRequests++;
                            peerState.numberOfRequests++;
                            NIODownloadHandler.torrentState.getStatus().set(writeMessage.getIndex(), PieceStatus.Requested);
                        } else{
                            break;
                        }
                    }
                }
                if (peerState.waitingRequests > 0)
                {
                    key.interestOps(SelectionKey.OP_READ);
                    return;
                }

            }
        }
       key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);

    }

    public void handleConnection(SelectionKey key){
        SocketChannel clntChan = (SocketChannel) key.channel();
        int peerIndex = channelIntegerMap.get(clntChan.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();

        boolean isConnected = false;
        try {
            DEBUG.log("connecting to " + clntChan.getRemoteAddress());
            isConnected = clntChan.finishConnect();

            if (!isConnected){
                DEBUG.loge("cannot connect to " + clntChan.getRemoteAddress());
                return;
            }


            Message bitfield = new Message(PeerMessage.MsgType.BITFIELD, clientState.getBitfield());
            peerState.welcomeQ.add(bitfield);
            Message unchoke = new Message(PeerMessage.MsgType.UNCHOKE);
            peerState.welcomeQ.add(unchoke);
            Message interested = new Message(PeerMessage.MsgType.INTERESTED);
            peerState.welcomeQ.add(interested);


        } catch (IOException e) {
            e.printStackTrace();

        }
        //Todo : catch connexion errors

        try {
            if (clntChan.finishConnect())
                key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //TODO : algo de selection des pieces
    public static boolean fetchRequests(){

        boolean everyoneIsConnected = true;
        for (PeerInfo peer: NIODownloadHandler.peerInfoList) {
            everyoneIsConnected = everyoneIsConnected && peer.getPeerState().isConnected();
        }
        if (!everyoneIsConnected){
            return false;
        }

        boolean sent = false;
        if (NIODownloadHandler.clientState.piecesToRequest.size() < 4){
            TCPClient.torrentContext.updatePeerState();


            Iterator<Integer> it = NIODownloadHandler.clientState.piecesToRequest.iterator();

            if(it.hasNext()){
                Integer index = it.next();
                sent = NIODownloadHandler.sendBlockRequests(DownloadStrat.peersByPieceIndex(NIODownloadHandler.peerInfoList, index), index);
            }
        }
        return sent;
    }

    //todo : method to decide the next WRITE/READ INTEREST OPERATIONS

}
