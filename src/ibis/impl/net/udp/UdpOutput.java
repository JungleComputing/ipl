package ibis.impl.net.udp;

import ibis.impl.net.NetBufferFactory;
import ibis.impl.net.NetBufferedOutput;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;
import ibis.impl.net.NetSendBufferFactoryDefaultImpl;
import ibis.io.Conversion;
import ibis.util.IPUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Hashtable;

/**
 * The UDP output implementation.
 *
 * <BR><B>Note</B>: this first implementation does not use UDP broadcast capabilities.
 */
public final class UdpOutput extends NetBufferedOutput {

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
        private InetAddress    laddr  = null;

        /**
         * The local socket IP port.
         */
        private int            lport  =    0;

        /**
         * The local MTU.
         */
        private int            lmtu   =   0;

        /**
         * The remote socket IP address.
         */
        private InetAddress    raddr  = null;

        /**
         * The remote socket IP port.
         */
        private int            rport  =   0;

        /**
         * The remote MTU.
         */
        private int            rmtu   =   0;

        /**
         * The peer {@link ibis.impl.net.NetReceivePort NetReceivePort}
         * local number.
         */
        private Integer        rpn    = null;


        private long           seqno  = 0;  /* For out-of-order debugging */


        /**
         * Constructor.
         *
         * @param pt the properties of the output's 
         * {@link ibis.impl.net.NetReceivePort NetReceivePort}.
         * @param driver the TCP driver instance.
         */
        UdpOutput(NetPortType pt, NetDriver driver, String context) throws IOException {
                super(pt, driver, context);

                if (Driver.DEBUG) {
                        headerLength = 8;
                }
        }

        /*
         * Sets up an outgoing UDP connection.
         *
         * <BR><B>Note</B>: this function also negociate the mtu.
         * <BR><B>Note</B>: the current UDP mtu is arbitrarily fixed at 32kB.
         */
        public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();
                if (rpn != null) {
                        throw new Error("connection already established");
                }                

                rpn = cnx.getNum();
                if (Driver.DEBUG) {
		    System.err.println(this + ": setupConnection over " + cnx);
		}
        
		socket = new DatagramSocket(0, IPUtils.getLocalHostAddress());
		lmtu   = Math.min(socket.getSendBufferSize(), 32768);//TOCHANGE
		laddr  = socket.getLocalAddress();
		lport  = socket.getLocalPort();

                Hashtable lInfo = new Hashtable();
                lInfo.put("udp_address", laddr);
                lInfo.put("udp_port",    new Integer(lport));
                lInfo.put("udp_mtu",     new Integer(lmtu));
                Hashtable rInfo = null;

// System.err.println(this + ": setupConnection, now receive");
		ObjectInputStream is = new ObjectInputStream(cnx.getServiceLink().getInputSubStream(this, "udp-request"));
                try {
                        rInfo = (Hashtable)is.readObject();
                } catch (ClassNotFoundException e) {
                        throw new Error(e);
                }

		ObjectOutputStream os = new ObjectOutputStream(cnx.getServiceLink().getOutputSubStream(this, "udp-reply"));
		os.writeObject(lInfo);
		os.close();

		raddr =  (InetAddress)rInfo.get("udp_address");
		rport = ((Integer)    rInfo.get("udp_port")   ).intValue();
		rmtu  = ((Integer)    rInfo.get("udp_mtu")    ).intValue();

		mtu    = Math.min(lmtu, rmtu);
		if (factory == null) {
		    factory = new NetBufferFactory(mtu, new NetSendBufferFactoryDefaultImpl());
		} else {
		    factory.setMaximumTransferUnit(mtu);
		}
		packet = new DatagramPacket(new byte[0], 0, raddr, rport);

		/* Wait for the receiver */
		int ok = is.read();
		is.close();
		if (Driver.DEBUG) {
		    System.err.println(this + ": setupConnection over " + cnx + "; finish by receiving OK byte");
		}


                log.out();
        }

        public void sendByteBuffer(NetSendBuffer b) throws IOException {
                log.in();
                if (Driver.DEBUG) {
                        Conversion.defaultConversion.long2byte(seqno++, b.data, 0);
                }
                packet.setData(b.data, 0, b.length);
// System.err.println(this + ": send packet size " + b.length);
// Thread.dumpStack();
// System.err.print("]");
		socket.send(packet);
                if (! b.ownershipClaimed) {
                        b.free();
                }
                log.out();
        }

        public synchronized void close(Integer num) throws IOException {
                log.in();
                if (rpn == num) {
                        if (socket != null) {
                                socket.close();
                        }
                        rpn = null;
                }
                log.in();
        }
        

        public void free() throws IOException {
                log.in();
                if (socket != null) {
                        socket.close();
                }

                super.free();
                log.out();
        }
}
