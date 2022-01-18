package misc;

import misc.download.TCPClient;
import misc.utils.DEBUG;

public class MainBittorrent {

    public static void main(String[] args) {
        //DEBUG.switchIOToFile();
        DEBUG.init();
        TCPClient tcpClient = new TCPClient(args[0]);
        tcpClient.run();

    }
}
