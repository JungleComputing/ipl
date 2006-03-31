/* $Id$ */

package ibis.impl.tcp;

import ibis.io.BufferedArrayOutputStream;
import ibis.io.Conversion;
import ibis.io.OutputStreamSplitter;
import ibis.io.SplitterException;
import ibis.io.SerializationBase;
import ibis.io.SerializationOutput;
import ibis.io.Replacer;
import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.IbisError;
import ibis.ipl.PortMismatchException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

final class TcpSendPort implements SendPort, Config, TcpProtocol {

    private static class Conn {
        OutputStream out;
        Socket s;
        TcpReceivePortIdentifier ident;

        public boolean equals(Object o) {
            if (!(o instanceof Conn)) {
                return false;
            }

            Conn other = (Conn) o;

            return other.ident.equals(ident);
        }

        public int hashCode() {
            return ident.hashCode();
        }
    }

    TcpPortType type;

    private TcpSendPortIdentifier ident;

    private String name;

    private boolean aMessageIsAlive = false;

    private TcpIbis ibis;

    private OutputStreamSplitter splitter;

    private SerializationOutput out;

    private ArrayList receivers = new ArrayList(); /* a list of Conn objects */

    private TcpWriteMessage message;

    private boolean connectionAdministration = false;

    private SendPortConnectUpcall connectUpcall = null;

    private ArrayList lostConnections = new ArrayList(); /* a list of TcpReceivePortIdentifier objects */

    private Replacer replacer;

    BufferedArrayOutputStream bufferedStream;

    long count;

    private java.util.Map props = new HashMap();
    
    TcpSendPort(TcpIbis ibis, TcpPortType type, String name,
            boolean connectionAdministration, SendPortConnectUpcall cU) {

        this.name = name;
        this.type = type;
        this.ibis = ibis;
        if (cU != null) {
            connectionAdministration = true;
        }
        this.connectionAdministration = connectionAdministration;
        this.connectUpcall = cU;

        ident = new TcpSendPortIdentifier(name, type.name(),
                (TcpIbisIdentifier) type.ibis.identifier());

        // if we keep administration, close connections when exception occurs.
        splitter = new OutputStreamSplitter(connectionAdministration,
                connectionAdministration);

        bufferedStream = new BufferedArrayOutputStream(splitter);

        out = SerializationBase.createSerializationOutput(type.ser,
                bufferedStream);
        message = new TcpWriteMessage(this, out, connectionAdministration);
    }

    /** returns the type that was used to create this port */
    public PortType getType() {
        return type;
    }

    public long getCount() {
        return count;
    }

    public void resetCount() {
        count = 0;
    }

    public synchronized void connect(ReceivePortIdentifier receiver,
            long timeoutMillis) throws IOException {
        /* first check the types */
        if (!type.name().equals(receiver.type())) {
            throw new PortMismatchException(
                    "Cannot connect ports of different PortTypes");
        }

        if (aMessageIsAlive) {
            throw new IOException(
                    "A message was alive while adding a new connection");
        }

        if (DEBUG) {
            System.err.println("Sendport " + this + " '" + name
                    + "' connecting to " + receiver);
        }

        // we have a new receiver, now add it to our tables.
        TcpReceivePortIdentifier ri = (TcpReceivePortIdentifier) receiver;
        Conn c = new Conn();
        c.ident = ri;

        if (receivers.contains(c)) {
            throw new AlreadyConnectedException(
                    "This sendport was already connected to " + receiver);
        }

        c.s = ibis.tcpPortHandler.connect(this, ri, (int) timeoutMillis);
        if (c.s == null) {
            throw new ConnectionRefusedException("Could not connect");
        }
        
        c.out = c.s.getOutputStream();

        if (DEBUG) {
            System.err.println(name + " adding Connection to " + ri);
        }

        out.writeByte(NEW_RECEIVER);

        if (DEBUG) {
            System.err.println(name + " Sending NEW_RECEIVER");
        }

        out.flush();
        out.close();

        receivers.add(c);
        splitter.add(c.out);

        out = SerializationBase.createSerializationOutput(type.ser,
                bufferedStream);
        if (replacer != null) {
            out.setReplacer(replacer);
        }

        message = new TcpWriteMessage(this, out, connectionAdministration);

        if (DEBUG) {
            System.err.println("Sendport '" + name + "' connecting to "
                    + receiver + " done");
        }
    }

    public synchronized void setReplacer(Replacer r) throws IOException {
        replacer = r;
        out.setReplacer(r);
    }

    public void connect(ReceivePortIdentifier receiver) throws IOException {
        connect(receiver, 0);
    }

    public synchronized void disconnect(ReceivePortIdentifier receiver)
            throws IOException {
        byte[] receiverBytes;
        byte[] receiverLength;
        Conn connection = null;

        //find connection to "receiver"
        for (int i = 0; i < receivers.size(); i++) {
            Conn temp = (Conn) receivers.get(i);
            if (temp.ident.equals(receiver)) {
                connection = temp;
                break;
            }
        }
        if (connection == null) {
            throw new IOException("Cannot disconnect from " + receiver
                    + " since we are not connected with it");
        }

        receivers.remove(connection);

        //close 
        try {
            out.writeByte(CLOSE_ONE_CONNECTION);

            receiverBytes = Conversion.object2byte(receiver);
            receiverLength = new byte[Conversion.INT_SIZE];
            Conversion.defaultConversion.int2byte(receiverBytes.length,
                    receiverLength, 0);
            out.writeArray(receiverLength);
            out.writeArray(receiverBytes);
            out.flush();
            out.close();
        } finally {
            splitter.remove(connection.out);
        }

        try {
            connection.s.close();
        } catch (Exception x) {
            // ignore
        }
    }

    public ibis.ipl.WriteMessage newMessage() throws IOException {
        TcpWriteMessage res = null;

        if (out == null) {
            throw new IOException("newMessage call on closed sendport");
        }

        synchronized (this) {
            // if (receivers.size() == 0) {
            //     throw new IbisIOException("port is not connected");
            // }

            while (aMessageIsAlive) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }

            aMessageIsAlive = true;

            res = message;
            res.isFinished = false;
        }

        out.writeByte(NEW_MESSAGE);
        if (type.numbered) {
            long seqno = ibis.getSeqno(type.name);
            out.writeLong(seqno);
        }
        return res;
    }

    synchronized void finishMessage() {
        SplitterException e = splitter.getExceptions();
        if (e != null) {
            try {
                message.forwardLosses(e);
            } catch (Exception e2) {
                throw new IbisError("forwardLosses threw exception: " + e, e);
            }
        }
        aMessageIsAlive = false;
        notifyAll();
    }

    public Map properties() {
        return props;
    }

    public Object getProperty(String key) {
        return props.get(key);
    }

    public void setProperties(Map properties) {
        props = properties;
    }
    
    public void setProperty(String key, Object val) {
        props.put(key, val);
    }
    
    public String name() {
        return name;
    }

    public SendPortIdentifier identifier() {
        return ident;
    }

    public synchronized void close() throws IOException {
        if (aMessageIsAlive) {
            throw new IOException(
                    "Trying to close a sendport port while a message is alive!");
        }

        if (ident == null) {
            throw new IbisError("Port already closed");
        }

        if (DEBUG) {
            System.err.println(type.ibis.identifier()
                    + ": SendPort.close start");
        }

        try {
            out.writeByte(CLOSE_ALL_CONNECTIONS);
            out.reset();
            out.flush();
            out.close();
        } catch (IOException e) {
            // System.err.println("Error in TcpSendPort.close: " + e);
            // e.printStackTrace();
        }

        for (int i = 0; i < receivers.size(); i++) {
            Conn c = (Conn) receivers.get(i);
            try {
                c.s.close();
            } catch (Exception x) {
                // ignore
            }
        }

        receivers = null;
        splitter = null;
        out = null;
        ident = null;

        if (DEBUG) {
            System.err.println(type.ibis.identifier()
                    + ": SendPort.close DONE");
        }
    }

    public synchronized ReceivePortIdentifier[] connectedTo() {
        Conn[] connections = (Conn[]) receivers.toArray(new Conn[0]);
        ReceivePortIdentifier[] res
                = new ReceivePortIdentifier[connections.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = connections[i].ident;
        }

        return res;
    }

    // called by the writeMessage
    // the stream has already been removed from the splitter.
    // we must remove it from our receivers table and inform the user.
    void lostConnection(OutputStream s, Exception cause, boolean report) {
        TcpReceivePortIdentifier rec = null;

        if (DEBUG) {
            System.out.println("sendport " + name + " lost connection!");
        }

        synchronized (this) {
            for (int i = 0; i < receivers.size(); i++) {
                Conn c = (Conn) receivers.get(i);
                if (c.out == s) {
                    receivers.remove(i);
                    rec = c.ident;
                    break;
                }
            }

            if (rec == null && report) {
                // Strange, we can't seem to find the connection in the
                // connection list.
                // Maybe we already reported the error?
                throw new IbisError(
                        "could not find connection in lostConnection");
            }
            if (report && connectUpcall == null) {
                lostConnections.add(rec);
                return;
            }
        }

        if (report) {
            // don't hold lock during upcall
            connectUpcall.lostConnection(this, rec, cause);
        }
    }

    public synchronized ReceivePortIdentifier[] lostConnections() {
        ReceivePortIdentifier[] result =
                (ReceivePortIdentifier[]) lostConnections.toArray(new ReceivePortIdentifier[0]);
        lostConnections.clear();
        return result;
    }
}
