/* $Id$ */

package ibis.impl.net.tcp_plain;

import ibis.connect.socketFactory.ConnectProperties;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIO;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPort;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetReceivePort;
import ibis.impl.net.NetSendPort;
import ibis.ipl.DynamicProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * The TCP output implementation (block version).
 */
public final class TcpOutput extends NetOutput {

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
     * The communication input stream.
     *
     * Note: this stream is not really needed but may be used for debugging
     *       purpose.
     */
    private InputStream tcpIs = null;

    /**
     * The communication output stream.
     */
    private OutputStream tcpOs = null;

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
        headerLength = 0;
    }

    /**
     * Sets up an outgoing TCP connection.
     */
    public synchronized void setupConnection(NetConnection cnx)
            throws IOException {
        if (this.rpn != null) {
            throw new Error("connection already established");
        }

        this.rpn = cnx.getNum();

        InputStream brokering_in = cnx.getServiceLink().getInputSubStream(this,
                "tcp_plain_brokering");
        OutputStream brokering_out = cnx.getServiceLink().getOutputSubStream(
                this, "tcp_plain_brokering");
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

        tcpSocket = NetIbis.socketFactory.createBrokeredSocket(brokering_in,
                brokering_out, false, props);

        brokering_in.close();
        brokering_out.close();

        tcpSocket.setSendBufferSize(0x8000);
        tcpSocket.setReceiveBufferSize(0x8000);
        tcpSocket.setTcpNoDelay(true);

        tcpOs = tcpSocket.getOutputStream();
        tcpIs = tcpSocket.getInputStream();

        mtu = 0;
    }

    public long finish() throws IOException {
        super.finish();
        tcpOs.flush();
        // TODO: return byte count of message
        return 0;
    }

    public void writeByte(byte b) throws IOException {
        //System.err.println("TcpOutput: writing byte "+b);
        tcpOs.write(b);
    }

    public synchronized void close(Integer num) throws IOException {
        if (rpn == num) {
            if (tcpOs != null) {
                tcpOs.close();
            }

            if (tcpIs != null) {
                tcpIs.close();
            }

            if (tcpSocket != null) {
                tcpSocket.close();
            }

            rpn = null;
        }
    }

    public void free() throws IOException {
        if (tcpOs != null) {
            tcpOs.close();
        }

        if (tcpIs != null) {
            tcpIs.close();
        }

        if (tcpSocket != null) {
            tcpSocket.close();
        }

        rpn = null;

        super.free();
    }

}
