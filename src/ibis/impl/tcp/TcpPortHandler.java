/* $Id$ */

/** This class handles all incoming connection requests.
 **/
package ibis.impl.tcp;

import ibis.connect.IbisSocketFactory;
import ibis.io.DummyInputStream;
import ibis.io.DummyOutputStream;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.IbisError;
import ibis.util.ThreadPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    Socket connect(TcpSendPort sp, TcpReceivePortIdentifier receiver,
            int timeout) throws IOException {
        Socket s = null;

        long startTime = System.currentTimeMillis();

        try {
            if (DEBUG) {
                System.err.println("--> Creating socket for connection to "
                        + receiver);
            }

            do {
                s = socketFactory.createClientSocket(receiver.ibis.address(),
                        receiver.port, me.address(), 0, timeout, sp.properties());

                InputStream sin = s.getInputStream();
                OutputStream sout = s.getOutputStream();

                ObjectOutputStream obj_out = new ObjectOutputStream(
                        new DummyOutputStream(sout));
                DataInputStream data_in = new DataInputStream(
                        new DummyInputStream(sin));

                obj_out.writeObject(receiver);
                obj_out.writeObject(sp.identifier());
                obj_out.flush();

                int result = data_in.readByte();
                if (result != RECEIVER_ACCEPTED) {
                    obj_out.flush();
                    obj_out.close();
                    data_in.close();
                    sin.close();
                    sout.close();
                    s.close();
                    if (result == RECEIVER_DENIED) {
                        return null;
                    }
                } else {

                    if (DEBUG) {
                        System.err.println("--> Sender Accepted");
                    }

                    /* the other side accepts the connection, finds the correct 
                     stream */
                    result = data_in.readByte();

                    if (result == NEW_CONNECTION) {
                        // close the object stream. The underlying stream will not be closed,
                        // thanks to the dummy stream in between.
                        obj_out.flush();
                        obj_out.close();
                        data_in.close();

                        Socket s1 = socketFactory.createBrokeredSocket(s
                                .getInputStream(), s.getOutputStream(), false,
                                sp.properties());

                        sin.close();
                        sout.close();
                        s.close();

                        return s1;
                    } else {
                        throw new IbisError(
                                "Illegal opcode in TcpPortHandler:connect");
                    }
                }
                if (timeout > 0
                        && System.currentTimeMillis() > startTime + timeout) {
                    throw new ConnectionTimedOutException("could not connect");
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
            } while (true);
        } catch (IOException e) {
            e.printStackTrace();
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
        TcpReceivePort rp = null;
        int i = 0;

        while (rp == null && i < receivePorts.size()) {

            TcpReceivePort temp = (TcpReceivePort) receivePorts.get(i);

            if (ident.equals(temp.identifier())) {
                if (DEBUG) {
                    System.err.println("--> findRecPort found " + ident
                            + " == " + temp.identifier());
                }
                rp = temp;
            }
            i++;
        }

        return rp;
    }

    /* returns: was it a close i.e. do we need to exit this thread */
    private void handleRequest(Socket s, InputStream in, OutputStream out)
            throws Exception {
        if (DEBUG) {
            System.err.println("--> portHandler on " + me
                    + " got new connection from " + s.getInetAddress() + ":"
                    + s.getPort() + " on local port " + s.getLocalPort());
        }

        ObjectInputStream obj_in = new ObjectInputStream(new DummyInputStream(
                in));
        DataOutputStream data_out = new DataOutputStream(new DummyOutputStream(
                out));

        if (DEBUG) {
            System.err.println("--> S Reading Data");
        }

        TcpReceivePortIdentifier receive = (TcpReceivePortIdentifier) obj_in
                .readObject();
        TcpSendPortIdentifier send = (TcpSendPortIdentifier) obj_in
                .readObject();
        TcpIbisIdentifier ibis = send.ibis;

        if (DEBUG) {
            System.out.println("--> got RP " + receive);
            System.out.println("--> got SP " + send);
            System.out.println("--> got ibis " + ibis);
            System.err.println("--> S finding RP");
        }

        /* First, try to find the receive port this message is for... */
        TcpReceivePort rp = findReceivePort(receive);

        if (DEBUG) {
            System.err.println("--> S  RP = "
                    + (rp == null ? "not found" : rp.identifier().toString()));
        }

        int result;
        if (rp == null) {
            result = RECEIVER_DENIED;
        } else {
            result = rp.connectionAllowed(send);
        }
        if (result != RECEIVER_ACCEPTED) {
            data_out.writeByte(result);
            data_out.flush();
            data_out.close();
            obj_in.close();
            out.close();
            in.close();
            s.close();
            return;
        }

        /* It accepts the connection, now we try to find an unused stream 
         originating at the sending machine */
        if (DEBUG) {
            System.err.println("--> S getting peer");
        }

        /* no unused stream found, so reuse current socket */
        data_out.writeByte(RECEIVER_ACCEPTED);
        data_out.writeByte(NEW_CONNECTION);
        data_out.flush();
        data_out.close();
        obj_in.close();

        Socket s1 = socketFactory.createBrokeredSocket(s.getInputStream(), s
                .getOutputStream(), true, rp.properties());
        s.close();

        /* add the connection to the receiveport. */
        rp.connect(send, s1);

        if (DEBUG) {
            System.err.println("--> S connect done ");
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

                InputStream sin = s.getInputStream();
                OutputStream sout = s.getOutputStream();

                handleRequest(s, sin, sout);

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