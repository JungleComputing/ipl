package ibis.impl.net.tcp_blk;

import ibis.connect.socketFactory.ConnectProperties;
import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferedOutput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIO;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetPort;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetReceivePortIdentifier;
import ibis.impl.net.NetSendBuffer;
import ibis.impl.net.NetSendBufferFactoryDefaultImpl;
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

/**
 * The TCP output implementation (block version).
 */
public final class TcpOutput extends NetBufferedOutput {

    /**
     * Debug switch
     */
    private final static boolean DEBUG = false; // true;

    /**
     * The communication socket.
     */
    private Socket tcpSocket = null;

    /**
     * The peer {@link ibis.impl.net.NetReceivePort NetReceivePort}
     * local number.
     */
    private Integer rpn = null;

    /**
     * The communication output stream.
     */
    private OutputStream tcpOs = null;

    /**
     * The local MTU.
     */
    // private int                      lmtu      = 16 * 1024;
    // private int                   lmtu            = 32 * 1024;
    private int lmtu = TypedProperties.intProperty(Driver.tcpblk_mtu,
            Driver.DEFAULT_MTU);
    //private int                      lmtu      = 5*1024;
    //private int                      lmtu      = 256;
    {
        if (lmtu != Driver.DEFAULT_MTU) {
            System.err.println("net.tcp_blk.TcpOutput.lmtu " + lmtu);
        }
    }

    /**
     * The remote MTU.
     */
    private int rmtu = 0;

    private IbisIdentifier partner;

    static {
        if (false) {
            System.err.println("WARNING: Class net.tcp_blk.TcpOutput (still)"
                    + " uses Conversion.defaultConversion");
        }
    }

    /**
     * Constructor.
     *
     * @param pt the properties of the output's
     * {@link ibis.impl.net.NetSendPort NetSendPort}.
     * @param driver the TCP driver instance.
     */
    TcpOutput(NetPortType pt, NetDriver driver, String context)
            throws IOException {
        super(pt, driver, context);
        headerLength = 4;
    }

    private Socket makeBrokeredConnection(NetConnection cnx)
            throws IOException {
        InputStream brokering_in = cnx.getServiceLink().getInputSubStream(this,
                "tcp_blk_brokering");
        OutputStream brokering_out = cnx.getServiceLink().getOutputSubStream(
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
                    if (result != null) {
                        return result;
                    }
                }
                return nn.getProperty(name);
            }
        };

        Socket tcpSocket = NetIbis.socketFactory.createBrokeredSocket(
                brokering_in, brokering_out, false, props);

        brokering_in.close();
        brokering_out.close();

        return tcpSocket;
    }

    /**
     * Sets up an outgoing TCP connection.
     */
    public synchronized void setupConnection(NetConnection cnx)
            throws IOException {
        log.in();

        if (this.rpn != null) {
            throw new Error("connection already established");
        }

        this.rpn = cnx.getNum();

        DataInputStream is = new DataInputStream(
                cnx.getServiceLink().getInputSubStream(this, "tcp_blk"));
        rmtu = is.readInt();
        is.close();

        NetReceivePortIdentifier recvId = cnx.getReceiveId();
        partner = recvId.ibis();
        tcpSocket = ((Driver) driver).getCachedOutput(partner);
        // System.err.println(this + ": cached socket(ibis=" + partner + ") " + tcpSocket);

        DataOutputStream os = new DataOutputStream(
                cnx.getServiceLink().getOutputSubStream(this, "tcp_blk"));
        os.writeInt(lmtu);
        if (tcpSocket == null) {
            os.writeInt(-1);
        } else {
            os.writeInt(tcpSocket.getLocalPort());
        }
        os.flush();
        os.close();

        if (tcpSocket == null) {
            tcpSocket = makeBrokeredConnection(cnx);
            if (TcpConnectionCache.VERBOSE) {
                System.err.println(this + ": create new TcpOutput " + tcpSocket
                        + "; cache input stream; remote ibis " + partner);
            }
            ((Driver) driver).cacheInput(partner, tcpSocket);
        } else {
            if (TcpConnectionCache.VERBOSE) {
                System.err.println(this + ": recycle TcpOutput " + tcpSocket);
            }
            // recycleConnection(tcpSocket);
        }

        tcpSocket.setSendBufferSize(lmtu);
        tcpSocket.setTcpNoDelay(true);

        tcpOs = tcpSocket.getOutputStream();

        mtu = Math.min(lmtu, rmtu);
        // Don't always create a new factory here, just specify the mtu.
        // Possibly a subclass overrode the factory, and we must leave
        // that factory in place.
        if (factory == null) {
            factory = new NetBufferFactory(mtu,
                    new NetSendBufferFactoryDefaultImpl());
        } else {
            factory.setMaximumTransferUnit(mtu);
        }
        log.out();
    }

    public long finish() throws IOException {
        log.in();
        super.finish();
        tcpOs.flush();
        log.out();
        // TODO: return byte count of message
        return 0;
    }

    public void sendByteBuffer(NetSendBuffer b) throws IOException {
        log.in();
        if (DEBUG) {
            System.err.print(this + ": write[" + b.length + "] = '");
            for (int i = 0; i < Math.min(32, b.length); i++) {
                System.err.print(b.data[i] + ",");
            }
            System.err.println("'");
        }

        try {
            Conversion.defaultConversion.int2byte(b.length, b.data, 0);
            tcpOs.write(b.data, 0, b.length);
            // tcpOs.flush();

            if (!b.ownershipClaimed) {
                b.free();
            }
        } catch (IOException e) {
            throw new ConnectionClosedException(e);
        }

        log.out();
    }

    public synchronized void close(Integer num) throws IOException {
        log.in();
        if (rpn != null && rpn == num) {
            if (!((Driver) driver).cacheOutput(partner, tcpSocket)) {
                if (tcpOs != null) {
                    tcpOs.close();
                }

                if (tcpSocket != null) {
                    tcpSocket.close();
                }
            }

            rpn = null;
        }
        log.out();
    }

    public void free() throws IOException {
        log.in();
        close(rpn);
        super.free();
        log.out();
    }

}
