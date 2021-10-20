package misc.peers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PeerConnectionHandler {

    private Socket socket;
    private int peerClientPort;
    private String server = "127.0.0.1";
    private InputStream in;
    private OutputStream out;


    public PeerConnectionHandler() {
    }

    public PeerConnectionHandler(int peerClientPort) throws IOException {
        this.peerClientPort = peerClientPort;

        connect();
    }

    public PeerConnectionHandler(int peerClientPort, String server) throws IOException {
        this.peerClientPort = peerClientPort;
        this.server = server;

        connect();
    }

    public PeerConnectionHandler(Socket socket, int peerClientPort, String server) throws IOException {
        this.socket = socket;
        this.peerClientPort = peerClientPort;
        this.server = server;

        connect();
    }


    private void connect() throws IOException {
        socket = new Socket(server, peerClientPort);
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    public void sendMsg(String data){
        byte[] encData = data.getBytes();
        try {
            out.write(encData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receivedMsg(){

    }



}
