
import ibis.gmi.*;

public class Data extends GroupMember implements i_Data {

    boolean gotData = false;

    boolean done = false;

    int turn = 0;

    int max;

    int count;

    int num_done;

    int hosts;

    int fake;

    double time = 0.0;

    i_Data group;

    Data(int count, int hosts) {
        this.max = count;
        this.hosts = hosts;
    }

    void init(i_Data group) {
        this.group = group;
    }

    public synchronized void foo() {
        bar_time = System.currentTimeMillis();
        fake++;
        //		notifyAll();
    }

    public void invokeRep() {
        group.bar();
        count++;
    }

    public synchronized void done(double result) {

        num_done++;
        if (time < result) {
            time = result;
        }

        if (num_done < hosts) {
            while (num_done < hosts) {
                try {
                    wait();
                } catch (Exception e) {
                }
            }
        } else {
            System.out.println("mcast lat = " + time);
            notifyAll();
        }
    }

    long bar_time = 0;

    int bar_count = -1;

    public synchronized void bar() {
        bar_time = System.currentTimeMillis();
        bar_count++;
        notify();
    }

    synchronized long readBar(int num) {

        while (num > bar_count) {
            try {
                wait();
            } catch (Exception e) {
            }
        }

        return bar_time;
    }

    public void barrier() {
        // Is a barrier because it is a combined invocation with a reply.
    }
}