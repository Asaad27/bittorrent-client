package com.download;

import com.torrent.TorrentState;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

/**
 * computes download rate in Mb/s
 */
public class DownloadRate implements Runnable {

    public static final int SCHEDULER = 1;
    public TorrentState torrentState;
    DecimalFormat df = new DecimalFormat();
    public static double downloadRate = 0;
    public static double uploadRate = 0;

    private long downloadSize;
    private long uploadSize;

    public DownloadRate(TorrentState torrentState) {
        this.torrentState = torrentState;
        downloadSize =  torrentState.getDownloadedSize();
        uploadSize = torrentState.getUploadedSize();
        df.setMaximumFractionDigits(1);
    }

    @Override
    public void run() {

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                uploadRate = (torrentState.getUploadedSize() - uploadSize)*1.0/1000;
                uploadSize = torrentState.getUploadedSize();
                downloadRate = (torrentState.getDownloadedSize() - downloadSize)*1.0/1000;
                downloadSize = torrentState.getDownloadedSize();
            }
        };

        Timer timer = new Timer();
        timer.schedule(timerTask, 1000 * SCHEDULER, 1000 * SCHEDULER);
    }
}
