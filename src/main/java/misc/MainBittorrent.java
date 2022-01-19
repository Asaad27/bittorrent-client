package misc;

import misc.download.TCPClient;
import misc.utils.DEBUG;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.util.Optional;

public class MainBittorrent {

    public static DownloadMode downloadMode = DownloadMode.FAST;
    public static void main(String[] args) {
        //DEBUG.switchIOToFile();
        parseArgs(args);
        DEBUG.init();
        TCPClient tcpClient = new TCPClient(args[0]);
        tcpClient.setDownloadMode(downloadMode);

        tcpClient.run();

    }

    public static void parseArgs(String[] args){
        for (String arg : args){
            if (arg.equals("-f")){
                downloadMode = DownloadMode.FAST;
            }
            else {
                downloadMode = DownloadMode.SLOW;
            }
        }
    }
}
