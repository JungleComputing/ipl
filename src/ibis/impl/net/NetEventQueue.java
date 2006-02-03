/* $Id$ */

package ibis.impl.net;

import java.util.Vector;

/**
 * Provide a general purpose {@linkplain ibis.impl.net.NetEvent event} FIFO.
 */
public class NetEventQueue {

    /**
     * Provide the queue backing data structure.
     */
    private Vector v = null;

    private boolean end = false;

    /**
     * Construct an empty queue.
     */
    public NetEventQueue() {
        v = new Vector();
    }

    /**
     * Append an event to the queue.
     *
     * This method is synchronized with {@link #get}
     *
     * @param event the event to append to the queue.
     */
    synchronized public void put(NetEvent event) {
        v.add(event);
        notifyAll();
    }

    /**
     * Extract the event at the head of the queue.
     *
     * This method is synchronized with {@link #put}. If the queue
     * is empty, this method will wait until an event is
     * added to the queue.
     *
     * @return the first event in the queue.
     * @exception InterruptedIOException if the method is
     * interrupted while waiting for an event.
     */
    synchronized public NetEvent get() throws InterruptedIOException {
        while (v.size() == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Ignore
            }
            if (v.size() == 0 && end) {
                throw new InterruptedIOException();
            }
        }

        return (NetEvent) v.remove(0);
    }

    synchronized public void end() {
        end = true;
        notifyAll();
    }

}
