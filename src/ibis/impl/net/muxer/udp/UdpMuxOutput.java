/* $Id$ */

package ibis.impl.net.muxer.udp;

import ibis.connect.IPUtils;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIO;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;
import ibis.impl.net.muxer.MuxerKey;
import ibis.impl.net.muxer.MuxerOutput;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * The UDP output implementation.
 *
 * <BR><B>Note</B>: this first implementation does not use UDP broadcast capabilities.
 */
public final class UdpMuxOutput extends MuxerOutput {

    /**
     * The UDP socket.
     */
    private DatagramSocket socket = null;

    /**
     * The UDP message wrapper.
     */
    private DatagramPacket packet = null;

    /**
     * The local socket IP address.
     */
    private InetAddress laddr = null;

    /**
     * The local socket IP port.
     */
    private int lport = 0;

    /**
     * The local MTU.
     */
    private int lmtu = 0;

    private Integer rpn = null;

    private int liveConnections;

    /**
     * Constructor.
     *
     * @param portType the output's {@link ibis.impl.net.NetSendPort NetSendPort}.
     * @param driver the TCP driver instance.
     * @param context the context.
     */
    public UdpMuxOutput(NetPortType portType, NetDriver driver, String context)
            throws IOException {

        super(portType, driver, context);

        socket = new DatagramSocket(0, IPUtils.getLocalHostAddress());
        lmtu = Math.min(socket.getSendBufferSize(), 32768);//TOCHANGE
        laddr = socket.getLocalAddress();
        lport = socket.getLocalPort();

        packet = new DatagramPacket(new byte[0], 0, null, 0);
    }

    /*
     * Sets up an outgoing multiplexed connection.
     *
     * <BR><B>Note</B>: this function also negociate the mtu.
     */
    public void setupConnection(NetConnection cnx, NetIO io)
            throws IOException {

        if (Driver.DEBUG) {
            System.err.println(this
                    + ": Now enter UdpMuxOutput.setupConnection, cnx = " + cnx
                    + " suffix = " + "muxer.udp-");
            Thread.dumpStack();
        }

        liveConnections++;
        rpn = cnx.getNum();

        // ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "muxer.udp-" + rpn));
        ObjectOutputStream os = new ObjectOutputStream(
                cnx.getServiceLink().getOutputSubStream(io, ":down"));
        os.writeObject(laddr);
        os.writeInt(lport);
        os.writeInt(lmtu);
        os.close();

        if (Driver.DEBUG) {
            System.err.println(this
                    + ": in UdpMuxOutput.setupConnection, Integer = "
                    + cnx.getNum() + " start info receive");
        }

        // ObjectInputStream  is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "muxer.udp-" + rpn));
        ObjectInputStream is = new ObjectInputStream(
                cnx.getServiceLink().getInputSubStream(io, ":up"));
        InetAddress raddr;
        try {
            raddr = (InetAddress) is.readObject();
        } catch (ClassNotFoundException e) {
            throw new Error("I cannot create java.net.InetAddress", e);
        }
        int rport = is.readInt();
        int rmtu = is.readInt();
        int rKey = is.readInt();
        is.close();

        MuxerKey key = new UdpMuxerKey(raddr, rport, rKey);
        registerKey(cnx, key);

        mtu = Math.min(lmtu, rmtu);

        if (Driver.DEBUG) {
            System.err.println(this
                    + ": Now leave UdpMuxOutput.setupConnection, Integer = "
                    + cnx.getNum());
        }
    }

    public void disconnect(MuxerKey key) throws IOException {
        if (--liveConnections == 0) {
            free();
        }
    }

    synchronized public void sendByteBuffer(NetSendBuffer b)
            throws IOException {

        UdpMuxerKey uk = (UdpMuxerKey) b.connectionId;

        if (Driver.DEBUG_HUGE) {
            System.err.println("Send packet, key " + uk);
            System.err.println("Send packet size " + b.length);
        }

        packet.setAddress(uk.remoteAddress);
        packet.setPort(uk.remotePort);
        packet.setData(b.data, 0, b.length);
        if (Driver.DEBUG_HUGE) {
            System.err.print("|");
        }
        // System.err.print("w");
        socket.send(packet);
        if (!b.ownershipClaimed) {
            b.free();
        }
    }

    synchronized public void close(Integer num) throws IOException {
        if (rpn == num) {
            if (socket != null) {
                socket.close();
                socket = null;
            }
            rpn = null;
        }
    }

    public void free() throws IOException {
        if (rpn == null) {
            return;
        }
        super.free();
    }

}
