package ibis.ipl.impl.net;

import ibis.ipl.Ibis;

import java.util.Vector;

import java.io.IOException;
import ibis.ipl.InterruptedIOException;

/**
 * Provide a general purpose {@linkplain NetEvent event} FIFO.
 */
public class NetEventQueue {

        /**
         * Provide the queue backing data structure.
         */
        Vector v = null;

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
         * is empty, this method will {@link #wait wait} until an event is
         * added to the queue.
         *
         * @return the first event in the queue.
         * @exception InterruptedIOException if the method is
         * interrupted while waiting for an event.
         */
        synchronized public NetEvent get() throws InterruptedIOException {
                if (v.size() == 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new InterruptedIOException(e);
			}
                }

                return (NetEvent)v.remove(0);
        }

}
