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
import java.util.LinkedList;
import java.util.Queue;

public class TCPHANDLER {
    public boolean requested = false;
    public boolean weAreChokedByPeer = true;

    public NIODownloadHandler peerDownloadHandler;

    public TorrentMetaData torrentMetaData;
    //TODO : assign one to each peer in peerstate or smthg
    public Queue<Message> writeMessageQ= new LinkedList<>();
    public boolean handshaken = false;

    public TCPHANDLER(TorrentMetaData torrentMetaData) {
        //TODO : change
        this.torrentMetaData = torrentMetaData;
        this.peerDownloadHandler = new NIODownloadHandler(torrentMetaData);
        peerDownloadHandler.leechTorrent(writeMessageQ);
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
        DEBUG.log("sent message ", message.getID().toString());
    }

    public void handleRead(SelectionKey key) {
        SocketChannel clntChan = (SocketChannel) key.channel();
        if (!handshaken) {
            HandShake hd = HandShake.readHandshake(clntChan);
            DEBUG.log("received handshake ", hd.toString());
            handshaken = true;
            //key.interestOps(SelectionKey.OP_WRITE);
            //clntChan.close();
        } else {
            DEBUG.log("recieving message");
            Message message = receiveMessage(clntChan);
            DEBUG.log("recieved message ", message.getID().toString());
            if (message.getID() == PeerMessage.MsgType.UNCHOKE)
                weAreChokedByPeer = false;
            /* DEBUG.log("the bitfield payload", Utils.bytesToHex(message.getPayload()));*/
            peerDownloadHandler.messageHandler(message, writeMessageQ);
        }

        key.interestOps(SelectionKey.OP_WRITE);
    }

    public void handleWrite(SelectionKey key) {
        SocketChannel clntChan = (SocketChannel) key.channel();
        // DEBUG.log("the peer ID", TrackerHandler.PEER_ID);
        if (!handshaken) {
            DEBUG.log("sending handshake");
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
            if (!writeMessageQ.isEmpty()){
                Message writeMessage = writeMessageQ.poll();
                if (writeMessage == null)
                    System.out.println("null message");
                else{
                    sendMessage(clntChan, writeMessage);

                }
            }
            else{

                //TODO : make a request, handleRequestToMake
                handleRequest();

            }
        }

        if (!handshaken)
            key.interestOps(SelectionKey.OP_READ );
        else
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE );
    }

    void handleRequest(){
        if (requested || weAreChokedByPeer)
            return ;
        requested = true;
        Message request = new Message(PeerMessage.MsgType.REQUEST, 0, 0, 16384);
        writeMessageQ.add(request);

        Message request2 = new Message(PeerMessage.MsgType.REQUEST, 0, 16384, 16384);
        writeMessageQ.add(request2);

        DEBUG.log("we made a request");
    }
}
