/* $Id$ */

public final class Test implements myServer {

    boolean done = false;

    public Test() {
        super();
    }

    public void one(Object o) {
        // empty
    }

    public Object two(Object o) {
        return o;
    }

    public synchronized void done() {
        done = true;
        notifyAll();
    }

    public synchronized void pleaseWait() {
        while (!done) {
            try {
                wait();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}