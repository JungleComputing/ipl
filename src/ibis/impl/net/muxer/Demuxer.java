/* $Id$ */

package ibis.impl.net.muxer;

import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferedInput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetReceiveBuffer;
import ibis.impl.net.NetReceiveBufferFactoryDefaultImpl;
import ibis.io.Conversion;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

/**
 * The Multiplexer input implementation.
 */
public final class Demuxer extends NetBufferedInput {

    private Integer spn = null;

    private MuxerQueue myQueue;

    private long receiveSeqno; /* For out-of-order debugging */

    private static ibis.impl.net.NetDriver subDriver;

    private static MuxerInput demux;

    static {
        System.err.println("WARNING: Class net.muxer.Demuxer (still)"
                + " uses Conversion.defaultConversion");
    }

    /**
     * Constructor.
     */
    Demuxer(NetPortType pt, NetDriver driver, String context,
            NetInputUpcall inputUpcall) throws IOException {
        super(pt, driver, context, inputUpcall);

        synchronized (driver) {
            if (subDriver == null) {
                // String subDriverName = getMandatoryProperty("Driver");
                System.err.println("It should depend on Driver properties"
                        + " which muxer subinput is created");
                String subDriverName = "muxer.udp";
                subDriver = driver.getIbis().getDriver(subDriverName);
                System.err.println("The subDriver is " + subDriver);
                demux = (MuxerInput) newSubInput(subDriver, "muxer",
                        upcallFunc == null ? null : this);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Call this when the connection has been established, and the demuxer
     * queue is set. After we have set the buffer factory in our queue, it may
     * start delivering.
     */
    public void setBufferFactory(NetBufferFactory factory) {
        if (Driver.DEBUG) {
            System.err.println(this
                    + ": ++++++++++++++++++++++++++ set a new BufferFactory "
                    + factory);
            Thread.dumpStack();
        }
        // super.setBufferFactory(factory);
        // this.factory = factory;
        /* Pass on our factory to the global demuxer. Hopefully our factory
         * generates a buffer class that is widely used. */
        demux.setBufferFactory(factory);
        dumpBufferFactoryInfo();
        myQueue.setBufferFactory(factory);
    }

    /**
     * Sets up a connection over the underlying DemuxerInput.
     */
    /*
     * Because a blocking poll can be pending while we want
     * to connect, the ReceivePort's inputLock cannot be taken
     * during a connect.
     * This implies that the blocking poll _and_ setupConnection
     * must protect the data structures.
     */
    public synchronized void setupConnection(NetConnection cnx)
            throws IOException {
        if (Driver.DEBUG) {
            System.err.println(this + ": setup connection, serviceLink "
                    + cnx.getServiceLink());
        }

        if (this.spn != null) {
            throw new Error(Thread.currentThread() + ": " + this
                    + ": serviceLink " + cnx.getServiceLink()
                    + " -- connection already established");
        }

        /* Set up the connection; it creates a MuxerQueue that is remembered
         * by our cnx */
        demux.setupConnection(cnx, this);
        /* Drag up the MuxerQueue that demux has created for us. It is
         * remembered by our cnx. */
        myQueue = demux.locateQueue(cnx);

        if (Driver.DEBUG) {
            System.err.println(this + ": Input connect spn " + spn
                    + " creates queue " + myQueue);
            Thread.dumpStack();
        }

        headerOffset = demux.getHeaderLength();
        headerLength = headerOffset;
        if (Driver.DEBUG) {
            headerLength += Conversion.LONG_SIZE;
        }
        dataOffset = headerLength;

        mtu = demux.getMaximumTransfertUnit();

        if (factory == null) {
            factory = new NetBufferFactory(mtu,
                    new NetReceiveBufferFactoryDefaultImpl());
        } else {
            factory.setMaximumTransferUnit(mtu);
        }
        demux.startQueue(myQueue, factory);

        /* The demuxer has reset its buffers to a sufficient size. Let the
         * other side start sending if it feels like it. */
        ObjectOutputStream os = new ObjectOutputStream(
                cnx.getServiceLink().getOutputSubStream(this, "muxer"));
        os.writeInt(1);
        os.close();

        if (Driver.DEBUG) {
            System.err.println(this + ": my queue is " + myQueue);
        }

        this.spn = cnx.getNum();
    }

    private void checkReceiveSeqno(NetReceiveBuffer buffer) throws IOException {
        if (Driver.PACKET_SEQNO) {
            long rSeqno = Conversion.defaultConversion.byte2long(buffer.data,
                    buffer.base + Driver.SEQNO_OFFSET);
            if (rSeqno != receiveSeqno) {
                System.err.println("Seems a packet was lost: receive seqno "
                        + rSeqno + "; expect " + receiveSeqno);
                if (rSeqno < receiveSeqno) {
                    throw new StreamCorruptedException("Packet count goes back");
                }
            }
            receiveSeqno = rSeqno + 1;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <BR><B>Note</B>: This UDP polling implementation uses a timeout.
     * As the minimum timeout value is one
     * millisecond, an unsuccessful polling operation is rather expensive.
     */
    public Integer doPoll(boolean block) throws IOException {
        if (myQueue == null) {
            System.err.println(this + ": This CANNOT be true ..."
                    + " setupConnection does a handshake, right?");
            // Still connecting, presumably
            return null;
        }

        return myQueue.poll(block);
    }

    /**
     * {@inheritDoc}
     *
     * <BR><B>Note</B>: this function may block if the expected data is
     * not there.
     * <BR><B>Note</B>: The expectedLength argument is simply ignored
     * because the packet actually received might not be the one that
     * is expected.
     */
    public NetReceiveBuffer receiveByteBuffer(int expectedLength)
            throws IOException {

        NetReceiveBuffer b = myQueue.receiveByteBuffer(expectedLength);
        checkReceiveSeqno(b);
        return b;
    }

    public void receiveByteBuffer(NetReceiveBuffer userBuffer)
            throws IOException {

        if (Driver.DEBUG_HUGE) {
            System.err.println(this + ": receiveByteBuffer, my headerLength "
                    + headerLength);
            Thread.dumpStack();
        }
        myQueue.receiveByteBuffer(userBuffer);
        checkReceiveSeqno(userBuffer);
    }

    public void doFinish() throws IOException {
        myQueue.doFinish();
    }

    public synchronized void doClose(Integer num) throws IOException {
        if (Driver.DEBUG) {
            System.err.println(this + ": doClose.");
            Thread.dumpStack();
        }

        if (spn == num) {
            spn = null;
            demux.disconnect(myQueue);
            myQueue.free();
            if (upcallFunc != null) {
                // Yes, what? How do we stop this thread?
            }
        }
    }

    public void doFree() throws IOException {
        if (Driver.DEBUG) {
            System.err.println(this + ": doFree.");
            Thread.dumpStack();
        }
        if (spn == null) {
            return;
        }

        close(spn);

        super.free();
    }

}
