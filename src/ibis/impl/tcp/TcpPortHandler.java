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
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.IbisError;
import ibis.util.ThreadPool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
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
    
    private void writeObject(OutputStream out, Object o) throws IOException { 
        
        byte[] tmp = Conversion.object2byte(o);
        
        int len = tmp.length;
        
        out.write((byte)(0xff & (len >> 24)));
        out.write((byte)(0xff & (len >> 16)));
        out.write((byte)(0xff & (len >> 8)));
        out.write((byte)(0xff & len));
        
        out.write(tmp);                
    }
    
    private void readFully(InputStream in, byte [] temp) throws IOException { 
        
        int read = 0;
        
        while (read < temp.length) { 
            int res = in.read(temp, read, temp.length-read);
            
            if (res == -1) { 
                throw new EOFException("EOF while reading object data!");
            } else { 
                read += res;
            }            
        }
    }
    
    private Object readObject(InputStream in) throws IOException { 
        
        int len = (((in.read() & 0xff) << 24) | 
                 ((in.read() & 0xff) << 16) |
                ((in.read() & 0xff) << 8) | 
                (in.read() & 0xff));
        
        byte [] tmp = new byte[len];        
        readFully(in, tmp);
        return Conversion.object2byte(tmp);
    }
    
    
    
    VirtualSocket connect(TcpSendPort sp, TcpReceivePortIdentifier receiver,
            int timeout) throws IOException {
        
        VirtualSocket s = null;
        
        long startTime = System.currentTimeMillis();
        
        try {
            if (DEBUG) {
                System.err.println("--> Creating socket for connection to "
                        + receiver);
            }
            
            do {
                s = socketFactory.createClientSocket(receiver.sa, timeout, 
                        sp.properties());
                
                InputStream sin = s.getInputStream();
                OutputStream sout = s.getOutputStream();
                
                DataOutputStream data_out = new DataOutputStream(
                        new BufferedOutputStream(new DummyOutputStream(sout)));
                DataInputStream data_in = new DataInputStream(
                        new BufferedInputStream(new DummyInputStream(sin)));
                
                byte[] recv = Conversion.object2byte(receiver);
                byte[] spIdent = Conversion.object2byte(sp.identifier());
                
                data_out.writeInt(recv.length);
                data_out.write(recv, 0, recv.length);
                data_out.writeInt(spIdent.length);
                data_out.write(spIdent, 0, spIdent.length);
                data_out.flush();
                                
                int result = data_in.readByte();                
                                
                switch(result) {
                case RECEIVER_ACCEPTED:
                    /*
                    VirtualSocket s1 = socketFactory.createBrokeredSocket(
                            data_in, data_out, false,
                            sp.properties());
                    
                    data_out.close();
                    data_in.close();
                    sin.close();
                    sout.close();
                    s.close();
                    return s1;
                    */
                  
                    // close should be caught by the Dummy streams
                    data_out.close();
                    data_in.close();                    
                    return s;                    
                case RECEIVER_DENIED:
                    data_out.close();
                    data_in.close();
                    sin.close();
                    sout.close();
                    s.close();
                    return null;
                case RECEIVER_DISABLED:
                    data_out.close();
                    data_in.close();
                    sin.close();
                    sout.close();
                    s.close();
                    
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
            } else { 
                if (DEBUG) {
                    System.err.println("--> findRecPort \"" + ident + "\" != \"" 
                            + temp.identifier());
                }                            
            }
            i++;
        }

        return rp;
    }

    /* returns: was it a close i.e. do we need to exit this thread */
    private void handleRequest(VirtualSocket s, InputStream in, OutputStream out)
        throws Exception {
        
        if (DEBUG) {
            System.err.println("--> portHandler on " + me
                    + " got new connection from " + s.getLocalSocketAddress() 
                    + ":" + s.getPort() + " on local port " + s.getLocalPort());
        }
        
        DataInputStream data_in = new DataInputStream(new BufferedInputStream(
                new DummyInputStream(in)));
        DataOutputStream data_out = new DataOutputStream(new BufferedOutputStream(
                new DummyOutputStream(out)));
        
        int recvLen = data_in.readInt();
        byte[] recv = new byte[recvLen];
        data_in.readFully(recv, 0, recv.length);
        TcpReceivePortIdentifier receive = (TcpReceivePortIdentifier)
        Conversion.byte2object(recv);
        
        int spLen = data_in.readInt();
        byte[] sp = new byte[spLen];
        data_in.readFully(sp, 0, sp.length);
        TcpSendPortIdentifier send = (TcpSendPortIdentifier)
        Conversion.byte2object(sp);
              
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
        
        data_out.writeByte(result);
        data_out.flush();
        
        if (result != RECEIVER_ACCEPTED) {
            data_out.close();
            data_in.close();
            out.close();
            in.close();
            s.close();
            return;
        }
        
        /*
        VirtualSocket s1 = socketFactory.createBrokeredSocket(data_in, data_out, true, rp.properties());
        data_out.close();
        data_in.close();
        out.close();
        in.close();
        s.close();        
                
        // add the connection to the receiveport.
        rp.connect(send, s1);
        */
        
        data_out.close();
        data_in.close();
       
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
