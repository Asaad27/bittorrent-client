package com;

import com.download.TCPClient;
import com.utils.DEBUG;

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
            if (arg.equalsIgnoreCase("-f")){
                downloadMode = DownloadMode.FAST;
            }
            else if (arg.equalsIgnoreCase("-s")){
                downloadMode = DownloadMode.SLOW;
            }
        }
    }
}
