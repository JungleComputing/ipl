package ibis.ipl.impl.stacking.lrmc.util;

// import ibis.util.Timer;

public class MessageQueue {

    private final int limit;

    private Message head;
    private Message tail;

    private int size = 0;

    private boolean finish = false;

    // Timer enqueueTimer = Timer.createTimer();

    public MessageQueue() {
        // no limit...
        this(Integer.MAX_VALUE);
    }

    public MessageQueue(int limit) {
        this.limit = limit;
    }

    public synchronized void terminate() {
        finish = true;
        // System.out.println("lrmc: wait " + enqueueTimer.nrTimes()
        // + " times, total " + Timer.format(enqueueTimer.totalTimeVal()));
        notifyAll();
    }

    public synchronized int size() {
        return size;
    }

    public synchronized void enqueue(Message m) {

        while (!finish && size >= limit) {
            // enqueueTimer.start();
            try {
                wait();
            } catch (Exception e) {
                // Ignored
                // } finally {
                // enqueueTimer.stop();
            }
        }

        if (finish) {
            return;
        }

        if (head == null) {
            head = tail = m;
            m.next = null;
            notifyAll();
        } else {
            tail.next = m;
            tail = m;
        }

        size++;

        // System.err.println("enqueue, q size = " + size);
    }

    public synchronized Message dequeue() {

        while (size == 0 && !finish) {
            try {
                wait();
            } catch (Exception e) {
                // Ignored
            }
        }

        if (finish) {
            return null;
        }

        Message tmp = head;
        head = head.next;
        tmp.next = null;

        size--;
        // System.err.println("dequeue, q size = " + size);

        if (size == limit - 1) {
            notifyAll();
        }

        return tmp;
    }

}
