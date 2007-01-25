/* $Id$ */

package ibis.impl.tcp;

import ibis.connect.IbisSocketFactory;
import ibis.impl.Ibis;
import ibis.impl.IbisIdentifier;
import ibis.impl.ReceivePort;
import ibis.impl.ReceivePortIdentifier;
import ibis.impl.SendPortIdentifier;
import ibis.io.Conversion;
import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.PortType;
import ibis.ipl.Registry;
import ibis.ipl.ResizeHandler;
import ibis.ipl.StaticProperties;
import ibis.util.ThreadPool;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public final class TcpIbis extends Ibis implements Runnable, TcpProtocol {

    private ServerSocket systemServer;

    private InetSocketAddress myAddress;

    private boolean quiting = false;

    private final IbisSocketFactory socketFactory;

    private HashMap<IbisIdentifier, InetSocketAddress> addresses
            = new HashMap<IbisIdentifier, InetSocketAddress>();

    public TcpIbis(ResizeHandler r, StaticProperties p1, StaticProperties p2)
            throws IOException {

        super(r, p1, p2);

        if (DEBUG) {
            System.err.println("In TcpIbis constructor");
        }

        InetAddress addr = IPUtils.getLocalHostAddress();
        if (addr == null) {
            System.err.println("ERROR: could not get my own IP address, "
                    + "exiting.");
            System.exit(1);
        }

        socketFactory = IbisSocketFactory.getFactory();

        systemServer = socketFactory.createServerSocket(0, addr, true,
                null /* don't pass properties, this is not a socket that is used for an ibis port */);
        int port = systemServer.getLocalPort();

        myAddress = new InetSocketAddress(addr, port);

        if (DEBUG) {
            System.out.println("--> TcpIbis: port = " + port);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        out.writeUTF(myAddress.getAddress().getHostAddress());
        out.writeInt(myAddress.getPort());
        out.flush();
        out.close();

        init(bos.toByteArray());

        ThreadPool.createNew(this, "TcpIbis");

        if (DEBUG) {
            System.err.println("Out of TcpIbis constructor, ident = "
                    + identifier());
        }
        /*
        try {
            Runtime.getRuntime().addShutdownHook(new TcpShutdown());
        } catch (Exception e) {
            System.err.println("Warning: could not register tcp shutdown hook");
        }
        */
    }

    protected PortType newPortType(StaticProperties p)
            throws PortMismatchException {

        return new TcpPortType(this, p);
    }

    ibis.ipl.ReceivePortIdentifier connect(TcpSendPort sp,
            IbisIdentifier id, String name, ibis.ipl.ReceivePortIdentifier rip,
            int timeout) throws IOException {
        Socket s = null;

        InetSocketAddress idAddr;
        int port;

        synchronized(addresses) {
            idAddr = (InetSocketAddress) addresses.get(id);
            if (idAddr == null) {
                byte[] b = id.getData();
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
                String addr = in.readUTF();
                port = in.readInt();
                in.close();
                try {
                    idAddr = new InetSocketAddress(InetAddress.getByName(addr),
                            port);
                } catch(Exception e) {
                    throw new IOException("Could not get address from " + id);
                }
                addresses.put(id, idAddr);
            }
        }

        port = idAddr.getPort();

        long startTime = System.currentTimeMillis();

        try {
            if (DEBUG) {
                System.err.println("--> Creating socket for connection to "
                        + name + " at " + id + ", port = " + port);
            }

            do {
                s = socketFactory.createClientSocket(idAddr.getAddress(), port,
                        myAddress.getAddress(), 0, timeout,
                        sp.properties());

                InputStream sin = s.getInputStream();
                OutputStream sout = s.getOutputStream();
                DataOutputStream data_out = new DataOutputStream(
                        new BufferedOutputStream(sout));
                DataInputStream data_in = new DataInputStream(
                        new BufferedInputStream(sin));

		byte[] spIdent = Conversion.object2byte(sp.identifier());

                data_out.writeUTF(name);
		data_out.writeInt(spIdent.length);
		data_out.write(spIdent, 0, spIdent.length);
		data_out.flush();

                int result = data_in.readByte();
                byte[] buf = null;
                Socket s1 = null;

                if (result == ReceivePort.ACCEPTED) {
                    int len = data_in.readInt();
                    buf = new byte[len];
                    data_in.readFully(buf, 0, len);
		    s1 = socketFactory.createBrokeredSocket(
			data_in, data_out, false,
			sp.properties());
                }

                data_out.close();
                data_in.close();
                s.close();

		switch(result) {
		case ReceivePort.ACCEPTED:
                    ReceivePortIdentifier recv = null;
                    try {
                        recv = (ReceivePortIdentifier)
                                Conversion.byte2object(buf);
                    } catch(ClassNotFoundException e) {
                        throw new Error("Wrong class in TcpIbis.connect", e);
                    }
                    if (rip != null) {
                        recv = (ReceivePortIdentifier) rip;
                    }
                    sp.addConn(recv, s1);
                    return recv;
                case ReceivePort.ALREADY_CONNECTED:
                    throw new AlreadyConnectedException(
                            "The sender was already connected to " + name
                            + " at " + id);
                case ReceivePort.TYPE_MISMATCH:
                    throw new PortMismatchException(
                            "Cannot connect ports of different PortTypes");
		case ReceivePort.DENIED:
                    if (rip != null) {
                        throw new ConnectionRefusedException("Receiver denied connection");
                    }
                    // fall through
		case ReceivePort.DISABLED:
		    // and try again if we did not reach the timeout...
		    if (timeout > 0
                        && System.currentTimeMillis() > startTime + timeout) {
			throw new ConnectionTimedOutException("could not connect");
		    }
		    try {
			Thread.sleep(100);
		    } catch (InterruptedException e) {
			// ignore
		    }
		    break;
		default:
		    throw new Error("Illegal opcode in TcpPorthandler.connect");
		}

            } while (true);
        } catch (IOException e) {
            // e.printStackTrace();
            try {
                if (s != null) {
                    s.close();
                }
            } catch (Exception e2) {
                // Ignore.
            }
            // just rethrow the original exception, otherwise, we loose the type! --Rob
            throw e; // new ConnectionRefusedException("Could not connect: " + e);
        }
    }

    protected void quit() {
        try {
            quiting = true;
            /* Connect to the serversocket, so that the port handler
             * thread wakes up.
             */
            InetAddress addr = myAddress.getAddress();
            int port = myAddress.getPort();
            socketFactory.createClientSocket(addr, port, addr, 0, 0, null);
        } catch (Exception e) {
            // Ignore
        }
    }

    /* returns: was it a close i.e. do we need to exit this thread */
    private void handleRequest(Socket s) throws Exception {
        if (DEBUG) {
            System.err.println("--> portHandler got new connection from "
                    + s.getInetAddress() + ":"
                    + s.getPort() + " on local port " + s.getLocalPort());
        }
        InputStream in = s.getInputStream();
        OutputStream out = s.getOutputStream();

        DataInputStream data_in = new DataInputStream(new BufferedInputStream(in));
	DataOutputStream data_out = new DataOutputStream(new BufferedOutputStream(out));

        String name = data_in.readUTF();

	int spLen = data_in.readInt();
	byte[] sp = new byte[spLen];
	data_in.readFully(sp, 0, sp.length);
	SendPortIdentifier send = (SendPortIdentifier)
            Conversion.byte2object(sp);

        /* First, try to find the receive port this message is for... */
        TcpReceivePort rp = (TcpReceivePort) findReceivePort(name);

        if (DEBUG) {
            System.err.println("--> S  RP = "
                    + (rp == null ? "not found" : rp.identifier().toString()));
        }

        int result;
        if (rp == null) {
            result = ReceivePort.DENIED;
        } else if (rp.isConnectedTo(send)) {
            result = ReceivePort.ALREADY_CONNECTED;
        } else {
            result = rp.connectionAllowed(send);
        }

        Socket s1 = null;
	data_out.writeByte(result);
        if (result == ReceivePort.ACCEPTED) {
            byte[] recv = Conversion.object2byte(rp.identifier());
            data_out.writeInt(recv.length);
            data_out.write(recv, 0, recv.length);
            data_out.flush();
            s1 = socketFactory.createBrokeredSocket(data_in, data_out, true, rp.properties());
            /* add the connection to the receiveport. */
            rp.connect(send, s1);

            if (DEBUG) {
                System.err.println("--> S connect done ");
            }
        }

        data_out.close();
        data_in.close();
        s.close();
    }

    public void run() {
        /* This thread handles incoming connection request from the
         * connect(TcpSendPort) call.
         */

        if (DEBUG) {
            System.err.println("--> TcpIbis running");
        }

        while (true) {
            Socket s = null;

            if (DEBUG) {
                System.err.println("--> PortHandler doing new accept()");
            }

            try {
                s = systemServer.accept();
            } catch (Exception e) {
                /* if the accept itself fails, we have a fatal problem.
                 Close this receiveport.
                 */
                try {
                    System.err
                            .println("EEK: TcpIbis:run: got exception "
                                    + "in accept ReceivePort closing!: " + e);
                    e.printStackTrace();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

                cleanup();
                throw new Error("Fatal: PortHandler could not do an accept");
            }

            if (DEBUG) {
                System.err.println("--> PortHandler through new accept()");
            }
            try {
                if (quiting) {
                    if (DEBUG) {
                        System.err.println("--> it is a quit");
                    }

                    systemServer.close();
                    s.close();
                    if (DEBUG) {
                        System.err.println("--> it is a quit: RETURN");
                    }

                    cleanup();
                    return;
                }

                handleRequest(s);

            } catch (Exception e) {
                try {
                    System.err
                            .println("EEK: TcpIbis:run: got exception "
                                    + "(closing this socket only: " + e);
                    e.printStackTrace();
                    if (s != null) {
                        s.close();
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private void cleanup() {
        try {
            if (systemServer != null) {
                systemServer.close();
            }
            systemServer = null;
        } catch (Exception e) {
            // Ignore
        }
    }

    public void poll() {
        // Empty implementation, as TCP Ibis has interrupts.
    }

    class TcpShutdown extends Thread {
        public void run() {
            end();
        }
    }
}
