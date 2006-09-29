/* $Id$ */

/** This class handles all incoming connection requests.
 **/
package ibis.impl.tcp;

import ibis.connect.virtual.VirtualServerSocket;
import ibis.connect.virtual.VirtualSocket;
import ibis.connect.virtual.VirtualSocketAddress;
import ibis.connect.virtual.VirtualSocketFactory;

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
import java.util.ArrayList;

final class TcpPortHandler implements Runnable, TcpProtocol, Config {

    private VirtualServerSocket systemServer;

    private ArrayList receivePorts;

    private final TcpIbisIdentifier me;

    // Removed -- the idea of having a single port doesn't work -- Jason
    // private final int port;
    
    // Added to replace the port -- Jason 
    private final VirtualSocketAddress sa; 

    private boolean quiting = false;

    private final VirtualSocketFactory socketFactory;

    TcpPortHandler(TcpIbisIdentifier me, VirtualSocketFactory fac)
            throws IOException {
        this.me = me;

        socketFactory = fac;

        /* We don't pass properties, since this is not a socket that is used 
         * for an ibis port */
        systemServer = socketFactory.createServerSocket(0, 0, true, null);

        // Removed -- the idea of having a single port doesn't work -- Jason 
        // port = systemServer.getLocalPort();
        
        // Added to replace the port -- Jason
        sa = systemServer.getLocalSocketAddress();

        me.sa = sa;

        if (DEBUG) {
            System.out.println("--> PORTHANDLER: socket address = " + sa);
        }

        receivePorts = new ArrayList();
        ThreadPool.createNew(this, "TcpPortHandler");
    }

    synchronized VirtualSocketAddress register(TcpReceivePort p) {
        if (DEBUG) {
            System.err.println("--> TcpPortHandler registered " + p.name);
        }
        receivePorts.add(p);
        return sa;
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
        
        VirtualSocket s = null;
        
        long startTime = System.currentTimeMillis();
        
        try {
            if (DEBUG) {
                System.err.println("--> Creating socket for connection to "
                        + name + " at " + id);
            }
            
            do {
                s = socketFactory.createClientSocket(id.sa, timeout, 
                        sp.properties());

                InputStream sin = s.getInputStream();
                OutputStream sout = s.getOutputStream();

                DataOutputStream data_out = new DataOutputStream(
                        new BufferedOutputStream(new DummyOutputStream(sout)));
                DataInputStream data_in = new DataInputStream(
                        new BufferedInputStream(new DummyInputStream(sin)));
                
                data_out.writeUTF(name);
                byte[] spbuf = Conversion.object2byte(sp.identifier());
                data_out.writeInt(spbuf.length);
                data_out.write(spbuf, 0, spbuf.length);
                data_out.flush();

                int result = data_in.readByte();                
                if (result == RECEIVER_ACCEPTED) {
                    int len = data_in.readInt();
                    byte[] buf = new byte[len];
                    TcpReceivePortIdentifier receive = null;

                    data_in.readFully(buf, 0, len);
                    data_out.close();
                    data_in.close();                    
                    try {
                        receive = (TcpReceivePortIdentifier)
                                Conversion.byte2object(buf);
                    } catch(ClassNotFoundException e) {
                        throw new IbisError("Wrong class in TcpPortHandler.connect", e);
                    }
                    if (rip != null) {
                        sp.addConn(rip, s);
                        return rip;
                    }
                    sp.addConn(receive, s);
                    return receive;
                }

                data_out.close();
                data_in.close();                    
                sin.close();
                sout.close();
                s.close();
                                
                switch(result) {
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
                        throw new ConnectionTimedOutException("Could not " +
                                "connect: receiveport was disabled");
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
            throw new ConnectionRefusedException("Could not connect: " + e);
        }
    }

    void quit() {
        try {
            quiting = true;
            /* Connect to the serversocket, so that the port handler
             * thread wakes up.
             */
            socketFactory.createClientSocket(sa,0, null);
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
    private void handleRequest(VirtualSocket s) throws Exception {

        TcpReceivePort rp = null;
        InputStream in = s.getInputStream();
        OutputStream out = s.getOutputStream();
        
        if (DEBUG) {
            System.err.println("--> portHandler on " + me
                    + " got new connection from " + s.getLocalSocketAddress() 
                    + ":" + s.getPort() + " on local port " + s.getLocalPort());
        }
        
        DataInputStream data_in = new DataInputStream(new BufferedInputStream(
                new DummyInputStream(in)));
        DataOutputStream data_out = new DataOutputStream(new BufferedOutputStream(
                new DummyOutputStream(out)));
        
        String name = data_in.readUTF();
        rp = findReceivePort(name);
        int spLen = data_in.readInt();
        byte[] sp = new byte[spLen];
        data_in.readFully(sp, 0, sp.length);
        TcpSendPortIdentifier send = (TcpSendPortIdentifier)
                Conversion.byte2object(sp);

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
        
        data_out.writeByte(result);
        if (result == RECEIVER_ACCEPTED) {
            byte[] recv = Conversion.object2byte(rp.identifier());
            data_out.writeInt(recv.length);
            data_out.write(recv, 0, recv.length);
        }

        data_out.flush();
        data_out.close();
        data_in.close();
        // There is a Dummy stream in between, so this does not close
        // the socket streams.
        
        if (result != RECEIVER_ACCEPTED) {
            in.close();
            out.close();
            s.close();
            return;
        }
        
        // add the connection to the receiveport.
        rp.connect(send, s);

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
            VirtualSocket s = null;

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
