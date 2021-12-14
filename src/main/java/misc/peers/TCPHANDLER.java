package misc.peers;

import misc.messages.Message;
import misc.messages.PeerMessage;
import misc.torrent.TorrentMetaData;
import misc.tracker.TrackerHandler;
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

    //map port -> index of peer in List<PeerInfo>
    public final Map<Integer, Integer> channelIntegerMap = new HashMap<>();
    //TODO : assign one to each peer in peerstate or smthg
    //public Queue<Message> writeMessageQ= new LinkedList<>();
    //public boolean handshaken = false;

    public TCPHANDLER(TorrentMetaData torrentMetaData, List<PeerInfo> peerList) {

        this.torrentMetaData = torrentMetaData;
        this.peerDownloadHandler = new NIODownloadHandler(torrentMetaData);
        this.peerList = peerList;

        //TODO : tracker should not return our client as a peer
        peerDownloadHandler.leechTorrent(peerList);
    }

    private Message receiveMessage(SocketChannel socketChannel) {
        byte[] buffLen = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(buffLen);

        int bRead = 0;
        try {

            while (bRead < 4)
                bRead += socketChannel.read(buffer);
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

    private void sendMessage(SocketChannel socketChannel, Message message) {
        byte[] msg = PeerMessage.serialize(message);
        ByteBuffer writeBuf = ByteBuffer.wrap(msg);
        try {
            socketChannel.write(writeBuf);
        } catch (IOException e) {
            System.err.println("error : " + e.getMessage());
            e.printStackTrace();
        }
        DEBUG.log("sent message ", message.getID().toString(), "to peer number", String.valueOf(channelIntegerMap.get(socketChannel.socket().getPort())));
    }

    public void handleRead(SelectionKey key) {
        SocketChannel clntChan = (SocketChannel) key.channel();
        int peerIndex = channelIntegerMap.get(clntChan.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();
        //DEBUG.log("peerIndex", String.valueOf(peerIndex));
        if (!peerState.handshake) {
            HandShake hd = HandShake.readHandshake(clntChan);
            DEBUG.log("received handshake ", "from peer number", String.valueOf(peerIndex), hd.toString());
            peerState.handshake = true;
            //key.interestOps(SelectionKey.OP_WRITE);
            //clntChan.close();
        } else {
            DEBUG.log("recieving message", "from peer number", String.valueOf(peerIndex));
            Message message = receiveMessage(clntChan);
            DEBUG.log("recieved message ", message.getID().toString(), "from peer number", String.valueOf(peerIndex));

            if (message.getID() == PeerMessage.MsgType.UNCHOKE)
                peerState.weAreChokedByPeer = false;
            /* DEBUG.log("the bitfield payload", Utils.bytesToHex(message.getPayload()));*/
            peerDownloadHandler.messageHandler(message, peerState);
        }

        key.interestOps(SelectionKey.OP_WRITE);
    }

    public void handleWrite(SelectionKey key) {
        SocketChannel clntChan = (SocketChannel) key.channel();
        int peerIndex = channelIntegerMap.get(clntChan.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();
        // DEBUG.log("the peer ID", TrackerHandler.PEER_ID);
        if (!peerState.handshake) {
            DEBUG.log("sending handshake", "too peer number", String.valueOf(peerIndex));
            HandShake sentHand = new HandShake(Utils.hexStringToByteArray(torrentMetaData.getSHA1Info()), Utils.hexStringToByteArray(TrackerHandler.PEER_ID));
            ByteBuffer writeBuf = ByteBuffer.wrap(sentHand.createHandshakeMsg());
            try {
                clntChan.write(writeBuf);
            } catch (IOException e) {
                System.err.println("error cannot send handshake");
            }
            //handshaken = true;
        }
        else{
            if (!peerState.writeMessageQ.isEmpty()){
                Message writeMessage = peerState.writeMessageQ.poll();
                if (writeMessage == null)
                    System.out.println("null message");
                else{
                    sendMessage(clntChan, writeMessage);

                }
            }
            else{

                //TODO : make a request, handleRequestToMake
                handleRequest(key);

            }
        }

        if (!peerState.handshake)
            key.interestOps(SelectionKey.OP_READ );
        else
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE );
    }

    //TODO : algo de selection des pieces
    void handleRequest(SelectionKey key){
        SocketChannel clntChan = (SocketChannel) key.channel();
        int peerIndex = channelIntegerMap.get(clntChan.socket().getPort());
        PeerState peerState = peerList.get(peerIndex).getPeerState();
        if (peerState.requested || peerState.weAreChokedByPeer)
            return ;
        peerState.requested = true;
        Message request = new Message(PeerMessage.MsgType.REQUEST, 0, 0, 16384);
        peerState.writeMessageQ.add(request);

        Message request2 = new Message(PeerMessage.MsgType.REQUEST, 0, 16384, 16384);
        peerState.writeMessageQ.add(request2);

        //DEBUG.log("we made a request");
    }
}
