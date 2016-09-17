import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Matthew on 12/09/2016.
 * watchdog timer for retransmission
 */
public class SenderWatchdog {

    private Timer timer;
    private Sender sender = null;
    private int timeout = 0;
    private int count = 0;
    private TimerTask timerTask;

    public SenderWatchdog(Sender sender, int timeout) {
        this.sender = sender;
        timer = new Timer();
        this.timeout = timeout;
    }

    /**
     * time's up
     */
    public void bite() {
        try {
            //System.out.println("Timer timeout!");
            sender.retransmit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * feed the watchdog
     */
    public void feed() {
        this.count = timeout;
    }


    /**
     * reset the watchdog
     */
    public void reset() {
        try {
            timerTask.cancel();
            timer.purge();
        } catch (IllegalStateException e) {

        } catch (NullPointerException e) {

        }
        feed();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                count--;
                if (count == 0) {
                    bite();
                }
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1);

    }


}
