package ibis.impl.nio;

import ibis.io.SerializationOutputStream;
import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisError;
import ibis.ipl.IbisIOException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Replacer;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.GatheringByteChannel;
import java.util.ArrayList;

public final class NioSendPort implements SendPort, Config, Protocol {

    final NioPortType type;

    private NioSendPortIdentifier ident;

    private boolean aMessageIsAlive = false;

    private SerializationOutputStream out = null;

    private NioWriteMessage message;

    private long count = 0; //number of byte send since last resetCount();

    private final NioAccumulator accumulator;

    private final boolean connectionAdministration;

    private final SendPortConnectUpcall connectUpcall;

    private final NioIbis ibis;

    private ArrayList lostConnections = new ArrayList();

    private Replacer replacer = null;

    private boolean neverConnected;

    /**
     * Abstract class that implements a SendPort in the NioIbis Implementation
     *
     * NOTE: The subclass of this class should also look out for any lost
     * connections.
     */
    NioSendPort(NioIbis ibis, NioPortType type, String name,
            boolean connectionAdministration, SendPortConnectUpcall cU)
            throws IOException {
        this.type = type;
        this.ibis = ibis;
        this.connectionAdministration = connectionAdministration;
        this.connectUpcall = cU;

        ident = new NioSendPortIdentifier(name, type.name(),
                (NioIbisIdentifier) ibis.identifier());

        switch (type.sendPortImplementation) {
        case NioPortType.IMPLEMENTATION_BLOCKING:
            accumulator = new BlockingChannelNioAccumulator(this);
            break;
        case NioPortType.IMPLEMENTATION_NON_BLOCKING:
            accumulator = new NonBlockingChannelNioAccumulator(this);
            break;
        case NioPortType.IMPLEMENTATION_THREAD:
            accumulator
                    = new ThreadNioAccumulator(this, ibis.sendReceiveThread());
            break;
        default:
            throw new IbisError("unknown send port implementation type");
        }
    }

    /**
     * Generates a list of all "lost connections"
     */
    public synchronized ReceivePortIdentifier[] lostConnections() {
        ReceivePortIdentifier[] result;
        result = (ReceivePortIdentifier[]) lostConnections.toArray();
        lostConnections.clear();
        return result;
    }

    public final synchronized void connect(ReceivePortIdentifier receiver,
            long timeoutMillis) throws IOException {

        //FIXME: Retry on "receiveport not ready"

        if (DEBUG) {
            Debug.enter("connections", this, "Sendport " + this + " '"
                    + ident.name + "' connecting to " + receiver);
        }

        if (!type.name().equals(receiver.type())) {
            if (DEBUG) {
                Debug.exit("connections", this,
                        "!Cannot connect ports of different PortTypes");
            }
            throw new PortMismatchException("Cannot connect ports of "
                    + "different PortTypes");
        }

        if (aMessageIsAlive) {
            if (DEBUG) {
                Debug.exit("connections", this,
                        "!Cannot connect while a message is alive");
            }
            throw new IOException("A message was alive while adding a new "
                    + "connection");
        }

        if (timeoutMillis < 0) {
            if (DEBUG) {
                Debug.exit("connections", this, "!Negative timeout");
            }
            throw new IOException(
                    "NioSendport.connect(): timeout must be positive, or 0");
        }

        if (!type.oneToMany && (connectedTo().length > 0)) {
            if (DEBUG) {
                Debug.exit("connections", this,
                        "!Cannot connect, port already connected to a receiver"
                                + " and OneToMany not supported");
            }
            throw new IOException("This sendport is already connected to a"
                    + " receiveport, and doesn't support multicast");
        }

        NioReceivePortIdentifier rpi = (NioReceivePortIdentifier) receiver;

        // make the connection. Will throw an Exception if if failed
        Channel channel = ibis.factory.connect(this.ident, rpi, timeoutMillis);

        if (ASSERT) {
            if (!(channel instanceof GatheringByteChannel)) {
                if (DEBUG) {
                    Debug.exit("connections", this,
                            "!factory returned wrong type of channel");
                }
                throw new IbisError("factory returned wrong type of channel");
            }
        }

        //close output stream (if it exist). The new receiver needs the
        //stream headers and such.
        if (out != null) {
            if (DEBUG) {
                Debug.message("connections", this, "letting all the other"
                        + " receivers know there's a new connection");
            }
            out.writeByte(NEW_RECEIVER);
            out.flush();
            out.close();
            out = null;
        }

        //register this new connection with the sendport somehow.
        accumulator.add(rpi, (GatheringByteChannel) channel);

        neverConnected = false;

        if (DEBUG) {
            Debug.exit("connections", this, "done connecting");
        }
    }

    public void connect(ReceivePortIdentifier receiver) throws IOException {
        connect(receiver, 0);
    }

    public synchronized void disconnect(ReceivePortIdentifier receiver)
            throws IOException {
        if (!(receiver instanceof NioReceivePortIdentifier)) {
            throw new IOException("receiver not of this ibis");
        }

        if (aMessageIsAlive) {
            throw new IOException("A message was alive while adding a new "
                    + "connection");
        }

        if (out != null) {
            //tell out peer someone is going to have to disconnect
            out.writeByte(CLOSE_ONE_CONNECTION);

            // write serialization stream footer
            out.flush();
            out.close();
            out = null;

            //write identification of receiver which has to leave
            ((NioReceivePortIdentifier) receiver).writeTo(accumulator);
            accumulator.flush();
        }

        accumulator.remove((NioReceivePortIdentifier) receiver);
    }

    public synchronized void setReplacer(Replacer r) throws IOException {
        replacer = r;
        if (out != null) {
            out.setReplacer(r);
        }
    }

    public synchronized ibis.ipl.WriteMessage newMessage() throws IOException {
        long sequencenr = -1;

        if (neverConnected) {
            throw new IbisIOException("port is not connected");
        }

        while (aMessageIsAlive) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Ignore.
            }
        }
        aMessageIsAlive = true;

        if (type.numbered) {
            sequencenr = ibis.getSeqno(type.name);
        }

        if (DEBUG) {
            String message = "new write message (# " + sequencenr + ") from "
                    + ident + " to:";

            ReceivePortIdentifier[] connections = accumulator.connections();

            for (int i = 0; i < connections.length; i++) {
                message = message + " " + connections[i];
            }

            Debug.enter("messages", this, message);
        }

        if (out == null) {
            //set up a stream to send data through
            out = type.createSerializationOutputStream(accumulator);

            if (replacer != null) {
                out.setReplacer(replacer);
            }

            message = new NioWriteMessage(this, out);
        } else {
            out.reset();
        }

        out.writeByte(NEW_MESSAGE);

        if (type.numbered) {
            out.writeLong(sequencenr);
        }

        return message;
    }

    synchronized long finish() {
        long messageCount = accumulator.getAndResetBytesWritten();
        count += messageCount;

        aMessageIsAlive = false;

        notifyAll();

        if (DEBUG) {
            Debug.exit("messages", this, "end of write message, messageCount: "
                    + messageCount);
        }

        return messageCount;
    }

    synchronized void finish(IOException e) {
        //since we have no idea which connection caused the error,
        //we close them all.

        NioReceivePortIdentifier[] connections = accumulator.connections();

        try {
            for (int i = 0; i < connections.length; i++) {
                accumulator.remove(connections[i]);
            }
        } catch (IOException f) {
            // :(
        }

        aMessageIsAlive = false;

        notifyAll();

        if (DEBUG) {
            Debug.exit("messages", this, "!end of write message with error");
        }

    }

    public DynamicProperties properties() {
        return null;
    }

    public String name() {
        return ident.name;
    }

    public SendPortIdentifier identifier() {
        return ident;
    }

    public void close() throws IOException {
        if (aMessageIsAlive) {
            throw new IOException(
                    "Trying to free a sendport port while a message is alive!");
        }

        if (ident == null) {
            throw new IbisError("Port already freed");
        }

        try {
            if (out == null) {
                //create a new stream, just to say close :(
                out = type.createSerializationOutputStream(accumulator);
            }

            out.writeByte(CLOSE_ALL_CONNECTIONS);
            out.reset();
            out.flush();
            out.close();
        } catch (IOException e) {
            //IGNORE
        }
        accumulator.reallyClose();

        out = null;
        ident = null;

    }

    public long getCount() {
        return count;
    }

    public void resetCount() {
        count = 0;
    }

    /**
     * Called by the accumulator to tell us a connections was lost.
     *
     * @throws IOException when the connectionadministration is disabled.
     */
    protected synchronized void lostConnection(ReceivePortIdentifier peer,
            IOException reason) throws IOException {
        if (connectUpcall != null) {
            // do upcall(may block!)
            connectUpcall.lostConnection(this, peer, reason);
        } else if (connectionAdministration) {
            lostConnections.add(peer);
        } else {
            throw reason;
        }
    }

    /**
     * Generates a list of all the receiveports we are connected to
     */
    public synchronized ReceivePortIdentifier[] connectedTo() {
        return accumulator.connections();
    }
}
