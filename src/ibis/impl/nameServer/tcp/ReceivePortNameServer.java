/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.connect.IbisSocketFactory;

import ibis.io.Conversion;

import ibis.ipl.IbisRuntimeException;

import ibis.util.ThreadPool;
import ibis.util.GetLogger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;

class ReceivePortNameServer extends Thread implements Protocol {

    private static final int MAXSOCKETS = 32;

    Logger logger
            = GetLogger.getLogger(ReceivePortNameServer.class.getName());

    private Hashtable ports;

    Hashtable requestedPorts;

    int socketCount = 0;

    boolean finishSweeper = false;

    private ServerSocket serverSocket;

    private boolean silent;

    private static class Port {
        String ibisName;
        byte[] port;

        public Port(String ibisName, byte[] port) {
            this.ibisName = ibisName;
            this.port = port;
        }
    }

    private class PortLookupRequest implements Runnable {

        private Socket s;

        private DataInputStream myIn;

        private DataOutputStream myOut;

        private String[] names;

        private byte[][] ports;

        private long timeout;
               
        private final boolean allowPartialResults;

        int unknown;

        private byte[] clientAddress;

        private int clientPort;

        PortLookupRequest(Socket s, DataInputStream in, DataOutputStream out,
                String[] names, byte[][] ports, long timeout, int unknown, 
                boolean allowPartialResults, byte[] clientAddress,
                int clientPort) {
            this.s = s;
            this.myIn = in;
            this.myOut = out;
            this.names = names;
            this.ports = ports;
            this.timeout = timeout;
            this.unknown = unknown;
            this.allowPartialResults = allowPartialResults;
            this.clientAddress = clientAddress;
            this.clientPort = clientPort;
        }
        
        public synchronized void addPort(String name, byte [] id) { 
         
            for (int j = 0; j < names.length; j++) {
                if (names[j].equals(name)) {
                    ports[j] = id;
                    unknown--;
                    if (unknown == 0) {
                        notify();
                    }
                    break;
                }
            }
        }        

        public void run() {
            boolean sentWait = false;
            synchronized(requestedPorts) {
                socketCount++;
                if (socketCount > MAXSOCKETS) {
                    logger.info("Sent wait: socketCount = " + socketCount);
                    socketCount--;
                    sentWait = true;
                    try {
                        myOut.writeByte(PORT_WAIT);
                    } catch(Throwable e) {
                        logger.error("PortLookupRequest failed to return"
                                + " result to " + s + ": got IOException", e);
                    } finally {
                        NameServer.closeConnection(myIn, myOut, s);
                        myIn = null;
                        myOut = null;
                        s = null;
                    }
                }
            }
            synchronized(this) {
                if (unknown > 0) {
                    try {
                        if (timeout > 0) {
                            this.wait(timeout);
                        } else {
                            this.wait();
                        }
                    } catch(Exception e) {
                        // Ignored
                    }
                }
            }

            writeResult();

            // Remove myself from requestedPorts
            synchronized(requestedPorts) {
                for (int i = 0; i < names.length; i++) {
                    ArrayList v = (ArrayList) requestedPorts.get(names[i]);
                    if (v != null) {
                        for (int j = v.size() - 1; j >= 0; j--) {
                            PortLookupRequest p = (PortLookupRequest) v.get(j);
                            if (p == this) {
                                v.remove(j);
                            }
                        }
                    }
                }
                if (! sentWait) {
                    socketCount--;
                }
            }
        }

        void writeResult() {
            try {
                if (myOut == null) {
                    logger.info("Setting up connection to client");
                    InetAddress client;
                    client = (InetAddress) Conversion.byte2object(clientAddress);
                    s = NameServerClient.socketFactory.createClientSocket(
                            client, clientPort, null, 
                            NameServer.CONNECT_TIMEOUT);
                    myOut = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                }
                if (unknown == 0 || allowPartialResults) {                 
                    myOut.writeByte(PORT_KNOWN);
                    myOut.writeInt(ports.length);
                    for (int i = 0; i < ports.length; i++) {
                        
                        if (ports[i] == null) { 
                            myOut.writeInt(0);
                        } else {                         
                            myOut.writeInt(ports[i].length);
                        
                            if (ports[i].length > 0) {                     
                                myOut.write(ports[i]);
                            }
                        }
                    }
                } else { 
                    myOut.writeByte(PORT_UNKNOWN);
                    myOut.writeInt(unknown);
                    for (int j = 0; j < ports.length; j++) {
                        if (ports[j] == null) {
                            myOut.writeUTF(names[j]);
                        }
                    }
                } 
                
            } catch (Throwable e) { 
                logger.error("PortLookupRequest failed to return"
                        + " result to " + s + ": got IOException", e);
            } finally {
                NameServer.closeConnection(myIn, myOut, s);
            }            
        }
    }

    ReceivePortNameServer(boolean silent, IbisSocketFactory socketFactory)
            throws IOException {
        ports = new Hashtable();
        requestedPorts = new Hashtable();
        this.silent = silent;
        serverSocket = socketFactory.createServerSocket(0, null, true, null);
        setName("ReceivePort Name Server");
        start();
    }

    int getPort() {
        return serverSocket.getLocalPort();
    }

    private void handlePortNew(DataInputStream in, DataOutputStream out)
            throws IOException {

        Port storedId;
        byte[] id;

        String ibisName = in.readUTF();
        String name = in.readUTF();
        int len = in.readInt();
        id = new byte[len];
        in.readFully(id, 0, len);

        /* Check wheter the name is in use. */
        storedId = (Port) ports.get(name);
        if (storedId == null) {
            ports.put(name, new Port(ibisName, id));
            out.writeByte(PORT_ACCEPTED);
            addPort(name, id, ibisName);
        } else {
            out.writeByte(PORT_REFUSED);
        }
    }

    //gosia
    private void handlePortRebind(DataInputStream in, DataOutputStream out)
            throws IOException {

        byte[] id;

        String ibisName = in.readUTF();
        String name = in.readUTF();
        int len = in.readInt();
        id = new byte[len];
        in.readFully(id, 0, len);

        /* Don't check whether the name is in use. */
        ports.put(name, new Port(ibisName, id));
        addPort(name, id, ibisName);
        out.writeByte(PORT_ACCEPTED);
    }

    private void addPort(String name, byte[] id, String ibisName) {
        
        ArrayList v = null;
            
        synchronized (requestedPorts) {
            v = (ArrayList) requestedPorts.remove(name);
        }
            
        if (v != null) {
            for (int i = 0; i < v.size(); i++) {
                PortLookupRequest p = (PortLookupRequest) v.get(i);
                p.addPort(name, id);
            }
        }
    }

    private void handlePortList(DataInputStream in, DataOutputStream out)
            throws IOException {

        ArrayList goodNames = new ArrayList();

        String pattern = in.readUTF();
        Enumeration names = null;
        
        names = ports.keys();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            if (name.matches(pattern)) {
                goodNames.add(name);
            }
        }

        out.writeByte(goodNames.size());
        for (int i = 0; i < goodNames.size(); i++) {
            out.writeUTF((String) goodNames.get(i));
        }
    }

    //end gosia	

    private void handlePortLookup(Socket s, DataInputStream in,
                DataOutputStream out) throws IOException {
        int addressLen = in.readInt();
        byte[] address = new byte[addressLen];
        in.readFully(address, 0, addressLen);
        int port = in.readInt();

        boolean allowPartialResults = in.readBoolean();
        int count = in.readInt();
        String[] names = new String[count];
        byte[][] prts = new byte[count][];
        int unknown = 0;

        for (int i = 0; i < count; i++) {
            names[i] = in.readUTF();
        }

        long timeout = in.readLong();

        for (int i = 0; i < count; i++) {
            Port p;
            p = (Port) ports.get(names[i]);
            if (p == null) {
                unknown++;
                prts[i] = null;
            } else {
                prts[i] = p.port;
            }
        }

        PortLookupRequest p = new PortLookupRequest(s, in, out, names, prts,
                timeout, unknown, allowPartialResults, address, port);

        if (unknown == 0) {            
            // TODO: clean this up ?
            p.writeResult();
            return;
        }

        ThreadPool.createNew(p, "PortLookupRequest thread");

        synchronized(requestedPorts) {
            for (int i = 0; i < count; i++) {
                if (prts[i] == null) {
                    ArrayList v = (ArrayList) requestedPorts.get(names[i]);
                    if (v == null) {
                        v = new ArrayList();
                        requestedPorts.put(names[i], v);
                    }
                    v.add(p);
                }
            }
        }
    }

    private void handlePortFree(DataInputStream in, DataOutputStream out)
            throws IOException {
        Port id;

        String name = in.readUTF();

        id = (Port) ports.get(name);

        if (id == null) {
            out.writeByte(1);
        }
        ports.remove(name);
        out.writeByte(0);
    }

    private void handlePortKill(DataInputStream in, DataOutputStream out)
            throws IOException {
        int cnt = in.readInt();
        String[] names = new String[cnt];

        for (int i = 0; i < cnt; i++) {
            names[i] = in.readUTF();
        }

        ArrayList v = new ArrayList();

        Enumeration portnames = ports.keys();
        while (portnames.hasMoreElements()) {
            String name = (String) portnames.nextElement();
            Port p = (Port) ports.get(name);
            for (int i = 0; i < cnt; i++) {
                if (p.ibisName.equals(names[i])) {
                    v.add(name);
                    break;
                }
            }
        }
        for (int i = 0; i < v.size(); i++) {
            String name = (String) v.get(i);
            ports.remove(name);
        }
        out.writeInt(0);
        out.flush();
    }

    public void run() {

        Socket s;
        int opcode;

        for (;;) {
            try {
                s = serverSocket.accept();
            } catch (Exception e) {
                throw new IbisRuntimeException(
                        "ReceivePortNameServer: got an error ", e);
            }

            DataInputStream in = null;
            DataOutputStream out = null;
            boolean mustClose = true;

            try {
                in = new DataInputStream(
                        new BufferedInputStream(s.getInputStream(), 4096));
                out = new DataOutputStream(
                        new BufferedOutputStream(s.getOutputStream(), 4096));

                opcode = in.readByte();

                switch (opcode) {
                case (PORT_NEW):
                    handlePortNew(in, out);
                    break;

                case (PORT_REBIND):
                    handlePortRebind(in, out);
                    break;

                case (PORT_LIST):
                    handlePortList(in, out);
                    break;

                case (PORT_FREE):
                    handlePortFree(in, out);
                    break;

                case (PORT_LOOKUP):
                    mustClose = false;
                    handlePortLookup(s, in, out);
                    break;

                case (PORT_KILL):
                    handlePortKill(in, out);
                    break;

                case (PORT_EXIT):
                    synchronized (ports) {
                        finishSweeper = true;
                        ports.notifyAll();
                    }
                    serverSocket.close();
                    return;
                default:
                    if (! silent) {
                        logger.error("ReceivePortNameServer: got an illegal "
                                + "opcode " + opcode);
                    }
                }
            } catch (Exception e1) {
                if (! silent) {
                    logger.error("Got an exception in "
                            + "ReceivePortNameServer.run", e1);
                }
            } finally {
                if (mustClose) {
                    NameServer.closeConnection(in, out, s);
                }
            }
        }
    }
}
