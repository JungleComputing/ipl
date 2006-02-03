/* $Id$ */

package ibis.impl.net.muxer;

import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferedInput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIO;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetReceiveBuffer;
import ibis.impl.net.NetVector;
import ibis.io.Conversion;
import ibis.ipl.ConnectionClosedException;

import java.io.IOException;
import java.util.Hashtable;

public abstract class MuxerInput extends NetBufferedInput implements Runnable {

    /**
     * Indicate whether a separate thread is desired to perform all polling
     * of the MuxerInput.
     */
    protected static final boolean USE_POLLER_THREAD = false;

    private int pollerThreads;

    private Thread poller;

    protected int max_mtu = 0;

    protected int min_mtu = Integer.MAX_VALUE;

    private int max_ever_mtu = -1;

    private int liveConnections;

    private int upcallReceivers;

    private final static int defaultPollTimeout = 30; // ms.

    private int pollTimeout = defaultPollTimeout;

    static {
        System.err.println("WARNING: Class net.muxer.MuxerInput (still)"
                + " uses Conversion.defaultConversion");
    }

    /**
     * Call this from all subclass constructors.
     */
    protected MuxerInput(NetPortType portType, NetDriver driver,
            String context, NetInputUpcall inputUpcall) {
        super(portType, driver, context, inputUpcall);
        mtu = 0;
        headerLength = Driver.HEADER_SIZE;

        /* ... please be patient, we'll find out how setup works *
         String s = null;
         if ((s = getProperty("PollingTimeout")) != null) {
         pollTimeout = Integer.valueOf(s).intValue();
         }
         * but only ma~nana */

        if (USE_POLLER_THREAD) {
            poller = new Thread(this);
            poller.setDaemon(true);
            poller.setName("UDP multiplexer poller");
            poller.start();
        }
    }

    private void receive() throws IOException {
        NetReceiveBuffer buffer = receiveByteBuffer(max_mtu);
        int rKey = Conversion.defaultConversion.byte2int(buffer.data,
                buffer.base + Driver.KEY_OFFSET);
        MuxerQueue q = locateQueue(rKey);
        if (q == null) {
            throw new ConnectionClosedException(
                    "Message arrives for MuxerInput that is closed");
        }
        if (Driver.DEBUG) {
            System.err.println("Receive downcall UDP packet len "
                    + buffer.length + " data " + buffer.data + "; key " + rKey
                    + " /bound to " + q);
        }
        q.enqueue(buffer);
    }

    /**
     * @param timeout poll timeout in msec. 0 signifies indefinite timeout.
     */
    abstract protected Integer doPoll(int timeout) throws IOException;

    public Integer doPoll(boolean block) throws IOException {

        if (!USE_POLLER_THREAD) {
            synchronized (this) {
                pollerThreads++;
            }
        }

        if (!block) {
            System.err.println("Call doPoll(0)");
        }
        Integer spn = doPoll(block ? 0 : pollTimeout);
        if (!block) {
            System.err.println("Returned from doPoll(0)");
        }

        if (!USE_POLLER_THREAD) {
            synchronized (this) {
                pollerThreads--;
            }
        }

        return spn;
    }

    /**
     * Test whether some other thread is busy polling or blocking.
     * If so, return immediately.
     * If not, perform poll(block).
     *
     * @param block If no other thread is polling, perform a poll.
     *        This parameter indicates whether it is to be a blocking
     *        poll or a nonblocking poll.
     */
    protected Integer attemptPoll(boolean block) throws IOException {
        boolean proceed;
        Integer r = null;

        synchronized (this) {
            if (liveConnections == 0) {
                // Before start or past shutdown
                return null;
            }

            proceed = (pollerThreads == 0);
            if (proceed) {
                pollerThreads++;
            }
        }
        // if (! proceed) System.err.print(proceed ? "v" : "-");

        if (proceed) {
            if ((r = doPoll(block ? 0 : pollTimeout)) != null) {
                receive();
            }

            synchronized (this) {
                pollerThreads--;
            }
        }

        return r;
    }

    private NetVector keyHash = new NetVector();

    private Hashtable cnxKeyHash = new Hashtable();

    public void setupConnection(NetConnection cnx) throws IOException {
        setupConnection(cnx, this);
    }

    public abstract void setupConnection(NetConnection cnx, NetIO io)
            throws IOException;

    /**
     * This should be called from setupConnection in the subclass, before
     * any communication takes place.
     */
    synchronized protected MuxerQueue createQueue(Object key, Integer spn)
            throws IOException {
        MuxerQueue q = new MuxerQueue(this, spn);
        int connectionKey = keyHash.add(q);
        if (Driver.DEBUG) {
            System.err.println(this + ": register " + q + " at key "
                    + connectionKey);
            Thread.dumpStack();
        }
        q.setConnectionKey(connectionKey);
        liveConnections++;
        cnxKeyHash.put(key, q);

        return q;
    }

    synchronized protected MuxerQueue locateQueue(Object key) {
        return (MuxerQueue) cnxKeyHash.get(key);
    }

    public void startQueue(MuxerQueue queue, NetBufferFactory factory)
            throws IOException {
        if (max_ever_mtu == -1) {
            max_ever_mtu = max_mtu;
        } else if (max_mtu > max_ever_mtu) {
            throw new IOException("Cannot increase mtu beyond " + max_ever_mtu);
        }
        factory.setMaximumTransferUnit(max_ever_mtu);
        queue.setBufferFactory(factory);
        this.factory = factory;
        synchronized (this) {
            if (upcallReceivers++ == 0) {
                notify();
            }
        }
    }

    public void disconnect(MuxerQueue q) throws IOException {
        if (Driver.DEBUG) {
            Thread.dumpStack();
            System.err.println("Now disconnect localQueue " + q.connectionKey()
                    + " liveConnections was " + liveConnections());
        }
        releaseQueue(q);
        if (USE_POLLER_THREAD) {
            System.err.println(this + ": interrupt...");
            poller.interrupt();
        }
    }

    synchronized protected MuxerQueue locateQueue(int n) {
        return (MuxerQueue) keyHash.get(n);
    }

    synchronized protected void releaseQueue(MuxerQueue key)
            throws IOException {
        System.err.println(this + ": disconnect; connections was "
                + liveConnections);
        keyHash.delete(key);
        liveConnections--;
        if (liveConnections == 0) {
            free();
        }
    }

    /* Call synchronized(this) */
    protected int liveConnections() {
        return liveConnections;
    }

    /**
     * The poller thread.
     */
    public void run() {
        synchronized (this) {
            while (upcallReceivers == 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Just go on waiting
                }
            }
        }

        if (Driver.DEBUG) {
            System.err.println(this + ": poller thread runs");
        }

        while (liveConnections > 0) {
            try {
                if (Driver.DEBUG) {
                    System.err.println(this
                            + ": poller thread does a blocking receive");
                }
                while (doPoll(0) == null) {
                    /* try again */
                }

                receive();

            } catch (Exception e) {
                System.err.println("************************"
                        + " Poller thread handles exception " + e);
            }
        }

        System.err.println(this + ": poller thread quits");
    }

}
