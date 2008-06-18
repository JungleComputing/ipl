/* $Id$ */

package ibis.io;

import ibis.util.Timer;

public class SerializationTimer {

    private final String name;

    private int starts;

    private Timer timer = Timer.createTimer();

    private Timer suspend = Timer.createTimer();

    public SerializationTimer(String name) {
        this.name = name;
    }

    public void start() {
        if (starts++ == 0) {
            timer.start();
        }
    }

    public void stop() {
        if (--starts == 0) {
            timer.stop();
        }
    }

    public void suspend() {
        if (starts > 0) {
            timer.stop();
            suspend.start();
        }
    }

    public void resume() {
        if (starts > 0) {
            suspend.stop();
            timer.start();
        }
    }

    public void reset() {
        starts = 0;
        timer.reset();
        suspend.reset();
    }

    public void report(java.io.PrintStream s) {
        if (timer.nrTimes() > 0) {
            s.println("Timer \"" + name + "\" " + timer.totalTime() + " ("
                    + timer.nrTimes() + ");" + " suspend "
                    + suspend.totalTime() + " (" + suspend.nrTimes() + ")");
        }
    }

    public void report() {
        report(System.out);
    }

}
