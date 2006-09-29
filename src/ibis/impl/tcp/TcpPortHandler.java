/* $Id$ */

/** This class handles all incoming connection requests.
 **/
package ibis.impl.tcp;

import ibis.connect.IbisSocketFactory;
import ibis.io.Conversion;
import ibis.io.DummyInputStream;
import ibis.io.DummyOutputStream;

import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.IbisError;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ReceivePortIdentifier;

import ibis.util.ThreadPool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

final class TcpPortHandler implements Runnable, TcpProtocol, Config {

    private ServerSocket systemServer;

    private ArrayList receivePorts;

    private final TcpIbisIdentifier me;

    private final int port;

    private boolean quiting = false;

    private final IbisSocketFactory socketFactory;

    TcpPortHandler(TcpIbisIdentifier me, IbisSocketFactory fac)
            throws IOException {
        this.me = me;

        socketFactory = fac;

        systemServer = socketFactory
                .createServerSocket(0, me.address(), true, null /* don't pass properties, this is not a socket that is used for an ibis port */);
        port = systemServer.getLocalPort();

        me.port = port;

        if (DEBUG) {
            System.out.println("--> PORTHANDLER: port = " + port);
        }

        receivePorts = new ArrayList();
        ThreadPool.createNew(this, "TcpPortHandler");
    }

    synchronized int register(TcpReceivePort p) {
        if (DEBUG) {
            System.err.println("--> TcpPortHandler registered " + p.name);
        }
        receivePorts.add(p);
        return port;
    }

    synchronized void deRegister(TcpReceivePort p) {
        if (DEBUG) {
            System.err.println("--> TcpPortHandler deregistered " + p.name);
        }
        if (!receivePorts.remove(p)) {
            throw new IbisError(
                    "Tcpporthandler: trying to remove unknown receiveport");
        }
    }

    ReceivePortIdentifier connect(TcpSendPort sp, TcpIbisIdentifier id,
            String name, TcpReceivePortIdentifier rip, int timeout)
            throws IOException {
        Socket s = null;

        long startTime = System.currentTimeMillis();

        try {
            if (DEBUG) {
                System.err.println("--> Creating socket for connection to "
                        + name + " at " + id + ", port = " + id.port);
            }

            do {
                s = socketFactory.createClientSocket(id.address(),
                        id.port, me.address(), 0, timeout, sp.properties());

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

                if (result == RECEIVER_ACCEPTED) {
                    int len = data_in.readInt();
                    buf = new byte[len];
                    data_in.readFully(buf, 0, len);
		    s1 = socketFactory.createBrokeredSocket(
			data_in, data_out, false,
			sp.properties());
                }

                data_out.close();
                data_in.close();
                sin.close();
                sout.close();
                s.close();

		switch(result) {
		case RECEIVER_ACCEPTED:
                    TcpReceivePortIdentifier recv = null;
                    try {
                        recv = (TcpReceivePortIdentifier)
                                Conversion.byte2object(buf);
                    } catch(ClassNotFoundException e) {
                        throw new IbisError("Wrong class in TcpPortHandler.connect", e);
                    }
                    if (rip != null) {
                        sp.addConn(rip, s1);
                        return rip;
                    }
                    sp.addConn(recv, s1);
                    return recv;
                case RECEIVER_ALREADYCONNECTED:
                    throw new AlreadyConnectedException(
                            "The sender was already connected to " + name
                            + " at " + id);
                case RECEIVER_TYPEMISMATCH:
                    throw new PortMismatchException(
                            "Cannot connect ports of different PortTypes");
		case RECEIVER_DENIED:
		    throw new ConnectionRefusedException("Could not connect");
		case RECEIVER_DISABLED:
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
		    throw new IbisError("Illegal opcode in TcpPorthandler.connect");
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
            throw new ConnectionRefusedException("Could not connect" + e);
        }
    }

    void quit() {
        try {
            quiting = true;
            /* Connect to the serversocket, so that the port handler
             * thread wakes up.
             */
            socketFactory.createClientSocket(me.address(), port, me.address(), 0, 
                    0, null);
        } catch (Exception e) {
            // Ignore
        }
    }

    private synchronized TcpReceivePort findReceivePort(
            TcpReceivePortIdentifier ident) {
    
        for (int i = 0; i < receivePorts.size(); i++) {
            TcpReceivePort temp = (TcpReceivePort) receivePorts.get(i);
            if (ident.equals(temp.identifier())) {
                return temp;
            }
        }
        return null;
    }

    private synchronized TcpReceivePort findReceivePort(
            String name) {
    
        for (int i = 0; i < receivePorts.size(); i++) {
            TcpReceivePort temp = (TcpReceivePort) receivePorts.get(i);
            if (name.equals(temp.identifier().name())) {
                return temp;
            }
        }
        return null;
    }

    /* returns: was it a close i.e. do we need to exit this thread */
    private void handleRequest(Socket s) throws Exception {
        if (DEBUG) {
            System.err.println("--> portHandler on " + me
                    + " got new connection from " + s.getInetAddress() + ":"
                    + s.getPort() + " on local port " + s.getLocalPort());
        }
        InputStream in = s.getInputStream();
        OutputStream out = s.getOutputStream();

        DataInputStream data_in = new DataInputStream(new BufferedInputStream(
	    new DummyInputStream(in)));
	DataOutputStream data_out = new DataOutputStream(new BufferedOutputStream(
            new DummyOutputStream(out)));

        String name = data_in.readUTF();

	int spLen = data_in.readInt();
	byte[] sp = new byte[spLen];
	data_in.readFully(sp, 0, sp.length);
	TcpSendPortIdentifier send = (TcpSendPortIdentifier)
            Conversion.byte2object(sp);

        /* First, try to find the receive port this message is for... */
        TcpReceivePort rp = findReceivePort(name);

        if (DEBUG) {
            System.err.println("--> S  RP = "
                    + (rp == null ? "not found" : rp.identifier().toString()));
        }

        int result;
        if (rp == null) {
            result = RECEIVER_DENIED;
        } else if (! send.type.equals(rp.identifier().type())) {
            result = RECEIVER_TYPEMISMATCH;
        } else if (rp.isConnectedTo(send)) {
            result = RECEIVER_ALREADYCONNECTED;
        } else {
            result = rp.connectionAllowed(send);
        }

        Socket s1 = null;
	data_out.writeByte(result);
        if (result == RECEIVER_ACCEPTED) {
            byte[] recv = Conversion.object2byte(rp.identifier());
            data_out.writeInt(recv.length);
            data_out.write(recv, 0, recv.length);
            data_out.flush();
            s1 = socketFactory.createBrokeredSocket(data_in, data_out, true, rp.properties());
        }
	data_out.flush();

        data_out.close();
        data_in.close();
        out.close();
        in.close();
        s.close();

        if (result == RECEIVER_ACCEPTED) {
            /* add the connection to the receiveport. */
            rp.connect(send, s1);

            if (DEBUG) {
                System.err.println("--> S connect done ");
            }
        }
    }

    public void run() {
        /* This thread handles incoming connection request from the
         * connect(TcpSendPort) call.
         */

        if (DEBUG) {
            System.err.println("--> TcpPortHandler running");
        }

        while (true) {
            Socket s = null;

            if (DEBUG) {
                System.err.println("--> PortHandler on " + me
                        + " doing new accept()");
            }

            try {
                s = systemServer.accept();
            } catch (Exception e) {
                /* if the accept itself fails, we have a fatal problem.
                 Close this receiveport.
                 */
                try {
                    System.err
                            .println("EEK: TcpPortHandler:run: got exception "
                                    + "in accept ReceivePort closing!: " + e);
                    e.printStackTrace();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

                cleanup();
                throw new IbisError("Fatal: PortHandler could not do an accept");
            }

            if (DEBUG) {
                System.err.println("--> PortHandler on " + me
                        + " through new accept()");
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
                            .println("EEK: TcpPortHandler:run: got exception "
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
}
