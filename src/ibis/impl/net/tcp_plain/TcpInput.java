/* $Id$ */

package ibis.impl.net.tcp_plain;

import ibis.connect.socketFactory.ConnectionPropertiesProvider;
import ibis.impl.net.NetAllocator;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIO;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPort;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetReceivePort;
import ibis.impl.net.NetSendPort;
import ibis.ipl.ConnectionClosedException;
import ibis.ipl.DynamicProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * The TCP input implementation.
 */
public final class TcpInput extends NetInput {

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

    /**
     * The communication output stream.
     *
     * <BR><B>Note</B>: this stream is not really needed but may be used
     * for debugging purpose.
     */
    private OutputStream tcpOs = null;

    /**
     * The buffer block allocator.
     */
    private NetAllocator allocator = null;

    private boolean first = false;

    private byte firstbyte = 0;

    /**
     * Constructor.
     *
     * @param pt the properties of the input's
     * {@link ibis.impl.net.NetSendPort NetSendPort}.
     * @param driver the TCP driver instance.
     */
    TcpInput(NetPortType pt, NetDriver driver, String context,
            NetInputUpcall inputUpcall) throws IOException {
        super(pt, driver, context, inputUpcall);
        headerLength = 0;
    }

    /**
     * Sets up an incoming TCP connection.
     */
    public synchronized void setupConnection(NetConnection cnx)
            throws IOException {
        if (this.spn != null) {
            throw new Error("connection already established");
        }

        InputStream brokering_in = cnx.getServiceLink().getInputSubStream(this,
                "tcp_blk_brokering");
        OutputStream brokering_out = cnx.getServiceLink().getOutputSubStream(
                this, "tcp_blk_brokering");
        NetPort port = cnx.getPort();

        final DynamicProperties p;
        if (port != null) {
            if (port instanceof NetReceivePort) {
                p = ((NetReceivePort) port).properties();
            } else if (port instanceof NetSendPort) {
                p = ((NetSendPort) port).properties();
            } else {
                p = null;
            }
        } else {
            p = null;
        }

        final NetIO nn = this;
        ConnectionPropertiesProvider props = new ConnectionPropertiesProvider() {
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

        tcpSocket = NetIbis.socketFactory.createBrokeredSocket(brokering_in,
                brokering_out, true, props);

        brokering_in.close();
        brokering_out.close();

        tcpSocket.setSendBufferSize(0x8000);
        tcpSocket.setReceiveBufferSize(0x8000);
        tcpSocket.setTcpNoDelay(true);

        tcpIs = tcpSocket.getInputStream();
        tcpOs = tcpSocket.getOutputStream();

        mtu = 0;

        this.spn = cnx.getNum();
    }

    /**
     * {@inheritDoc}
     *
     * <BR><B>Note</B>: This TCP polling implementation uses the
     * {@link java.io.InputStream#available()} function to test whether
     * at least one data byte may be extracted without blocking.
     */
    public Integer doPoll(boolean block) throws IOException {
        if (spn == null) {
            return null;
        }

        if (block) {
            int i = tcpIs.read();

            if (i < 0) {
                throw new ConnectionClosedException("Broken pipe");
            }

            first = true;
            firstbyte = (byte) (i & 0xFF);

            return spn;
        } else if (tcpIs.available() > 0) {
            return spn;
        }

        return null;
    }

    public void initReceive(Integer num) throws IOException {
        //
    }

    /**
     * {@inheritDoc}
     *
     * <BR><B>Note</B>: this function may block if the expected data is
     * not there.
     */
    public byte readByte() throws IOException {
        byte b = 0;

        if (first) {
            first = false;
            b = firstbyte;
        } else {
            int i = tcpIs.read();
            if (i < 0) {
                throw new ConnectionClosedException("unexpected EOF");
            }
            b = (byte) (i & 0xFF);
        }

        return b;
    }

    public void doFinish() {
        //
    }

    public synchronized void doClose(Integer num) throws IOException {
        if (spn == num) {
            if (tcpOs != null) {
                tcpOs.close();
            }

            if (tcpIs != null) {
                tcpIs.close();
            }

            if (tcpSocket != null) {
                tcpSocket.close();
            }

            spn = null;
        }
    }

    public void doFree() throws IOException {
        if (tcpOs != null) {
            tcpOs.close();
        }

        if (tcpIs != null) {
            tcpIs.close();
        }

        if (tcpSocket != null) {
            tcpSocket.close();
        }

        spn = null;

        super.free();
    }

}
