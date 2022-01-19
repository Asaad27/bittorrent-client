package misc.download;

import misc.torrent.TorrentState;

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
    public static double rate = -1;

    private long downloadSize;
    public DownloadRate(TorrentState torrentState) {
        this.torrentState = torrentState;
        downloadSize =  torrentState.getDownloadedSize();
        df.setMaximumFractionDigits(1);
    }

    @Override
    public void run() {

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                rate = (torrentState.getDownloadedSize() - downloadSize)*1.0/1024;
                downloadSize = torrentState.getDownloadedSize();
                System.out.println("==================" + df.format(rate) + "Mb/s");
            }
        };

        Timer timer = new Timer();
        timer.schedule(timerTask, 1000 * 3, 1000 * SCHEDULER);
    }
}
