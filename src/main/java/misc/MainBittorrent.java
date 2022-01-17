package misc;

import misc.download.TCPClient;
import misc.utils.DEBUG;

public class MainBittorrent {

    //TODO : deal with cancel message
    //TODO : debug onDownloadeNDED
    //TODO : tracker interraction and fix infinite loop
    //TODO : timeout peers and send keepalives
    public static void main(String[] args) {
        //DEBUG.switchIOToFile();
        TCPClient tcpClient = new TCPClient(args[0]);
        tcpClient.run();

    }
}
