package ibis.impl.net.tcp_blk;

import ibis.connect.socketFactory.ConnectProperties;
import ibis.impl.net.NetBuffer;
import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferedInput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIO;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPort;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetReceiveBuffer;
import ibis.impl.net.NetReceiveBufferFactoryDefaultImpl;
import ibis.impl.net.NetSendPortIdentifier;
import ibis.io.Conversion;
import ibis.ipl.ConnectionClosedException;
import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisIdentifier;
import ibis.util.TypedProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * The TCP input implementation (block version).
 */
public final class TcpInput extends NetBufferedInput {

    /**
     * Debug switch
     */
    private final static boolean DEBUG = false; // true;

    /**
     * The communication socket.
     */
    private Socket tcpSocket = null;

    /**
     * The peer {@link ibis.impl.net.NetSendPort NetSendPort}
     * local number.
     */
    private Integer spn = null;

    /**
     * The communication input stream.
     */
    private InputStream tcpIs = null;

    private IbisIdentifier partner;

    /**
     * The local MTU.
     */
    // private int                   lmtu            = 16 * 1024;
    // private int                   lmtu            = 32 * 1024;
    private int lmtu = TypedProperties.intProperty(Driver.tcpblk_mtu,
            Driver.DEFAULT_MTU);
    //private int                   lmtu            = 5*1024;
    //private int                   lmtu            = 256;
    {
        if (lmtu != Driver.DEFAULT_MTU) {
            System.err.println("net.tcp_blk.TcpInput.lmtu " + lmtu);
        }
    }

    /**
     * The remote MTU.
     */
    private int rmtu = 0;

    private NetReceiveBuffer buf = null;

    /**
     * Timeout value for "interruptible" poll
     */
    private static final int INTERRUPT_TIMEOUT = TypedProperties.intProperty(
            Driver.tcpblk_timeout, 1000); // 100; // ms

    private boolean interrupted = false;

    private boolean interruptible = false;

    private final static boolean READ_AHEAD = TypedProperties.booleanProperty(
            Driver.tcpblk_rdah, true);

    static {
        if (false) {
            System.err
                    .println("WARNING: Class net.tcp_blk.TcpInput (still) uses Conversion.defaultConversion");
        }
    }

    /**
     * Constructor.
     *
     * @param pt the properties of the input's
     * {@link ibis.impl.net.NetSendPort NetSendPort}.
     * @param driver the TCP driver instance.
     * @param inputUpcall the input upcall for upcall receives, or
     *        <code>null</code> for downcall receives
     */
    TcpInput(NetPortType pt, NetDriver driver, String context,
            NetInputUpcall inputUpcall) throws IOException {
        super(pt, driver, context, inputUpcall);
        headerLength = 4;
    }

    private Socket makeBrokeredConnection(NetConnection cnx) throws IOException {
        InputStream brokered_in = cnx.getServiceLink().getInputSubStream(this,
                "tcp_blk_brokering");
        OutputStream brokered_out = cnx.getServiceLink().getOutputSubStream(
                this, "tcp_blk_brokering");

        NetPort port = cnx.getPort();

        final DynamicProperties p;
        if (port != null) {
            p = port.properties();
        } else {
            p = null;
        }

        final NetIO nn = this;
        ConnectProperties props = new ConnectProperties() {
            public String getProperty(String name) {
                if (p != null) {
                    String result = (String) p.find(name);
                    if (result != null)
                        return result;
                }
                return nn.getProperty(name);
            }
        };

        Socket tcpSocket = NetIbis.socketFactory.createBrokeredSocket(
                brokered_in, brokered_out, true, props);

        brokered_in.close();
        brokered_out.close();

        return tcpSocket;
    }

    /**
     * Sets up an incoming TCP connection.
     */
    public synchronized void setupConnection(NetConnection cnx)
            throws IOException {
        log.in();
        if (this.spn != null) {
            throw new Error("connection already established");
        }

        NetSendPortIdentifier sendId = cnx.getSendId();
        partner = sendId.ibis();

        DataOutputStream os = new DataOutputStream(cnx.getServiceLink()
                .getOutputSubStream(this, "tcp_blk"));
        os.writeInt(lmtu);
        os.flush();
        os.close();

        DataInputStream is = new DataInputStream(cnx.getServiceLink()
                .getInputSubStream(this, "tcp_blk"));
        rmtu = is.readInt();
        int port = is.readInt();
        is.close();

        if (port == -1) {
            tcpSocket = makeBrokeredConnection(cnx);
            if (TcpConnectionCache.VERBOSE) {
                System.err.println(this + ": create new TcpInput " + tcpSocket
                        + "; cache output stream; remote ibis=" + partner);
            }
            ((Driver) driver).cacheOutput(partner, tcpSocket);
        } else {
            tcpSocket = ((Driver) driver).getCachedInput(partner, port);
            if (TcpConnectionCache.VERBOSE) {
                System.err.println(this + ": recycle TcpInput " + tcpSocket);
            }
            // recycleConnection(tcpSocket);
        }

        tcpSocket.setSendBufferSize(lmtu);
        tcpSocket.setTcpNoDelay(true);
        if (interruptible) {
            tcpSocket.setSoTimeout(INTERRUPT_TIMEOUT);
        }

        tcpIs = tcpSocket.getInputStream();

        mtu = Math.min(lmtu, rmtu);
        // Don't always create a new factory here, just specify the mtu.
        // Possibly a subclass overrode the factory, and we must leave
        // that factory in place.
        if (factory == null) {
            factory = new NetBufferFactory(mtu,
                    new NetReceiveBufferFactoryDefaultImpl());
        } else {
            factory.setMaximumTransferUnit(mtu);
        }

        this.spn = cnx.getNum();
        log.out();
    }

    public synchronized boolean pollIsInterruptible() throws IOException {
        return interruptible;
    }

    public synchronized void interruptPoll() throws IOException {
        interrupted = true;
    }

    public void setInterruptible(boolean interruptible) throws IOException {
        this.interruptible = interruptible;
        if (tcpSocket != null) {
            if (interruptible) {
                tcpSocket.setSoTimeout(INTERRUPT_TIMEOUT);
            } else {
                tcpSocket.setSoTimeout(0);
            }
        }
    }

    /*
     protected synchronized void switchToDowncallMode() throws IOException {
     installUpcallFunc(null);
     // System.err.println(Thread.currentThread() + ": " + this + ": setInterruptible, upcallFunc " + upcallFunc);
     }
     */

    public synchronized void switchToUpcallMode(NetInputUpcall upcallFunc)
            throws IOException {
        // System.err.println(Thread.currentThread() + ": " + this + ": switchToUpcallMode, upcallFunc " + upcallFunc);
        installUpcallFunc(upcallFunc);
    }

    private NetBuffer surplusHead;

    private NetBuffer surplusTail;

    private void surplusEnqueue(NetReceiveBuffer buf) {
        buf.next = null;
        if (surplusHead == null) {
            surplusHead = buf;
        } else {
            surplusTail.next = buf;
        }
        surplusTail = buf;
    }

    private NetReceiveBuffer surplusDequeue() {
        NetBuffer buf = surplusHead;
        if (buf != null) {
            surplusHead = buf.next;
        }
        return (NetReceiveBuffer) buf;
    }

    /**
     * @return the number of bytes missing in the last message
     */
    private int cacheSurplusBuffers(NetReceiveBuffer buf, int totalSize,
            int currentSize) {
        if (DEBUG) {
            System.err.println("First segment: start at " + 0 + " size "
                    + buf.length + " currentSize " + currentSize
                    + " totalSize " + totalSize);
        }
        if (totalSize <= currentSize) {
            return totalSize;
        }

        NetReceiveBuffer b;
        int copySize;
        do {
            b = createReceiveBuffer(0);
            if (DEBUG) {
                System.err.println("Created buffer size " + b.length
                        + " data.length " + b.data.length + " offset " + b.base
                        + " currentSize " + currentSize + " totalSize "
                        + totalSize);
            }
            surplusEnqueue(b);
            int sizeSize = totalSize - currentSize;
            if (sizeSize < Conversion.INT_SIZE) {
                // System.err.println("Return neg value " + (- sizeSize));
                System.arraycopy(buf.data, currentSize, b.data, b.base,
                        sizeSize);
                return -sizeSize;
            }
            int nextSize = Conversion.defaultConversion.byte2int(buf.data,
                    currentSize);
            copySize = Math.min(nextSize, totalSize - currentSize);
            System.arraycopy(buf.data, currentSize, b.data, b.base, copySize);
            b.length = nextSize;
            if (DEBUG) {
                System.err.println("Next segment: start at " + currentSize
                        + " size " + b.length + " missing "
                        + (nextSize - copySize));
            }
            currentSize += copySize;
        } while (totalSize > currentSize);
        // System.err.println("Return pos value " + copySize);

        return copySize;
    }

    /* Create a NetReceiveBuffer and do a blocking receive. */
    private NetReceiveBuffer receive() throws IOException {
        log.in();

        NetReceiveBuffer buf = surplusDequeue();
        if (buf != null) {
            return buf;
        }

        buf = createReceiveBuffer(0);
        int l = 0;
        int offset = 0;
        byte[] b;

        try {
            NetReceiveBuffer lastBuffer = buf;
            while (true) {
                b = lastBuffer.data;
                while (offset < Conversion.INT_SIZE) {
                    /*
                     * Try to read ahead as far as we can.
                     * If read() returns more than has been sent
                     * in one message, we buffer the extra buffers
                     * aside.
                     */
                    int result = 0;
                    try {
                        int readLength = READ_AHEAD ? b.length - offset
                                : Conversion.INT_SIZE;
                        result = tcpIs.read(b, offset, b.length - offset);
                    } catch (SocketTimeoutException e) {
                        if (interrupted) {
                            interrupted = false;
                            // throw Ibis.createInterruptedIOException(e);
                            // System.err.println(this + ": we are interrupted. Return null (or throw an InterruptedException?)");
                            return null;
                        }
                    }
                    if (result == -1) {
                        if (true || offset != 0) {
                            throw new ConnectionClosedException("broken pipe");
                        }
                    }

                    offset += result;
                }

                l = Conversion.defaultConversion.byte2int(b, 0);
                lastBuffer.length = l;
                // if (buf != lastBuffer) System.err.println("Now wanna read block " + l + " offset " + offset);

                /*
                 * Our read may have slurped in more than one buffer.
                 * Create a queue of the extra buffers.
                 */
                offset = cacheSurplusBuffers(lastBuffer, offset, l);
                if (offset > 0) {
                    break;
                }
                // offset < 0. We read halfway a size. Go on looping to
                // read that size and the buffer that belongs to it.
                lastBuffer = (NetReceiveBuffer) surplusTail;
                offset = -offset;
            }

            /*
             * If the last buffer read was incomplete, read the
             * rest now.
             * If there is a queue, offset is the offset into the tail
             * buffer that we must read fully because that is our
             * invariant.
             * If there is no queue, offset is the offset into the
             * current buffer, buf. That must also be read fully.
             */
            if (surplusHead != null) {
                NetBuffer tail = surplusTail;
                l = tail.length;
                b = tail.data;
            }

            while (offset < l) {
                int result = 0;
                try {
                    result = tcpIs.read(b, offset, l - offset);
                } catch (SocketTimeoutException e) {
                    if (interrupted) {
                        interrupted = false;
                        System.err
                                .println("Please store the data already read for the resume after the InterruptedIOException");
                        // throw Ibis.createInterruptedIOException(e);
                        return null;
                    }
                }
                if (result == -1) {
                    throw new ConnectionClosedException("broken pipe");
                }
                offset += result;
            }

        } catch (SocketException e) {
            String msg = e.getMessage();
            if (tcpSocket.isClosed() || msg.equalsIgnoreCase("socket closed")
                    || msg.equalsIgnoreCase("null fd object")) {
                throw new ConnectionClosedException(e);
            } else {
                throw e;
            }
        }
        if (DEBUG) {
            System.err.println(this + ": receive buffer size " + offset);
        }

        log.out();

        return buf;
    }

    /**
     * {@inheritDoc}
     *
     * <BR><B>Note</B>: This TCP polling implementation uses the
     * {@link java.io.InputStream#available()} function to test whether at least one
     * data byte may be extracted without blocking.
     */
    public Integer doPoll(boolean block) throws IOException {
        log.in();
        // We arrive over the normal route. Any interrupts can
        // be safely cleared. Or should we throw an
        // InterruptedIOException anyway?
        if (interrupted) {
            System.err.println("Clear the interrupted state anyway");
            interrupted = false;
        }

        if (spn == null) {
            log.out("not connected");
            return null;
        }

        if (block) {
            if (buf != null) {
                log.out("early return");
                return spn;
            }
            if ((buf = surplusDequeue()) != null) {
                log.out("early surplus return");
                return spn;
            }
            buf = receive();
            if (buf == null) {
                return null;
            }

            return spn;
        } else if (tcpIs.available() > 0) {
            return spn;
        }

        log.out();

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <BR><B>Note</B>: this function may block if the expected data is not there.
     */
    public NetReceiveBuffer receiveByteBuffer(int expectedLength)
            throws IOException {
        log.in();
        NetReceiveBuffer buf = this.buf;
        if (buf != null) {
            this.buf = null;
            log.out("early receive");
        } else {
            buf = receive();
        }

        log.out();
        return buf;
    }

    public void doFinish() throws IOException {
        log.in();
        //synchronized(this)
        {
            // System.err.print("doFinish: buf " + buf); if (buf != null) System.err.print("; [" + buf.base + ".." + buf.length + "]"); System.err.println();
            buf = null;
        }
        log.out();
    }

    public void doClose(Integer num) throws IOException {
        log.in();
        if (spn != null && spn == num) {
            if (!((Driver) driver).cacheInput(partner, tcpSocket)) {
                if (tcpIs != null) {
                    tcpIs.close();
                }

                if (tcpSocket != null) {
                    if (interruptible) {
                        System.err.println(this
                                + ": at close, still interruptible");
                    } else {
                        // System.err.println(this + ": at close, OK");
                    }
                    tcpSocket.close();
                }
            }
            spn = null;
        }
        log.out();
    }

    public void doFree() throws IOException {
        log.in();
        close(spn);
        log.out();
    }

}