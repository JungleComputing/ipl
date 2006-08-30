/* $Id$ */

// problem: blocking receive does not work.
// solution:
// A receiveport has a single slot for a message.
// The connectionhandler reads the opcode, and notifies receiveport.
// In a blocking receive, a wait is done until the slot is filled.

package ibis.impl.tcp;

import ibis.io.BufferedArrayInputStream;
import ibis.io.Conversion;
import ibis.io.SerializationBase;
import ibis.io.SerializationInput;
import ibis.ipl.IbisError;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

final class ConnectionHandler implements Runnable, TcpProtocol { //, Config {
    private static boolean DEBUG = false;

    private TcpReceivePort port;

    final BufferedArrayInputStream bufferedInput;

    TcpSendPortIdentifier origin;

    private SerializationInput in;

    private InputStream input;
    private Socket s;
    
    TcpReadMessage m;

    volatile boolean iMustDie = false;

    TcpIbis ibis;

    ConnectionHandler(TcpIbis ibis, TcpSendPortIdentifier origin,
            TcpReceivePort port, Socket s) throws IOException {
        this.port = port;
        this.origin = origin;
        this.s = s;
        this.input = s.getInputStream();
        this.ibis = ibis;

        bufferedInput = new BufferedArrayInputStream(input);

        createNewStream();
    }

    private void createNewStream() {
        in = SerializationBase.createSerializationInput(port.type.ser,
                bufferedInput);

        m = new TcpReadMessage(port, in, origin, this);
    }

    void close(Exception e) {
        if (in != null) {
            try {
                in.close();
            } catch (Exception z) {
                // Ignore.
            }
        }
        in = null;
        try {
            s.close();
        } catch (Exception x) {
            // ignore
        }
        if (!iMustDie) {
            // if we came in through a forced close, the port already knows
            // that we are gone.
            port.leave(this, e);
        }
    }

    /* called by forced close */
    void die() {
        iMustDie = true;
    }

    //	static int msgCounter = 0;
    public void run() {
        try {
            reader();
        } catch (IOException e) {
            if (DEBUG) {
                System.err.println("ConnectionHandler.run : " + port.name
                        + " Caught exception " + e);
                System.err.println("I am connected to " + origin);
                e.printStackTrace();
            }

            close(e);
        } catch (Throwable t) {
            close(null);
            throw new IbisError(t);
        }
    }

    void reader() throws IOException {
        byte opcode = -1;

        while (!iMustDie) {
            if (DEBUG) {
                System.err.println("handler " + this + " for port: "
                        + port.name + " woke up");
            }
            opcode = in.readByte();
            if (iMustDie) {
                // in this case, a forced close was done, and my port is gone..
                close(null);
                return;
            }

            if (DEBUG) {
                System.err.println("handler " + this + " for port: "
                        + port.name + ", READ BYTE " + opcode);
            }

            switch (opcode) {
            case NEW_RECEIVER:
                if (DEBUG) {
                    System.err.println(port.name + ": Got a NEW_RECEIVER");
                }
                in.close();
                createNewStream();
                break;
            case NEW_MESSAGE:
                if (DEBUG) {
                    System.err.println("handler " + this
                            + " GOT a new MESSAGE " + m + " on port "
                            + port.name);
                }

                if (port.setMessage(m)) {
                    // The port created a new reader thread, I must exit.
                    // Also when there is no separate connectionhandler thread.
                    return;
                }

                // If the upcall did not release the message, cool, 
                // no need to create a new thread, we are the reader.
                break;
            case CLOSE_ALL_CONNECTIONS:
                if (DEBUG) {
                    System.err.println(port.name + ": Got a FREE from "
                            + origin);
                }
                close(null);
                return;
            case CLOSE_ONE_CONNECTION:
                TcpReceivePortIdentifier identifier;
                byte[] receiverLength = new byte[Conversion.INT_SIZE];
                byte[] receiverBytes;
                // identifier of the receiveport from which a sendport is
                // disconnecting comes next.
                in.readArray(receiverLength);
                receiverBytes = new byte[Conversion.defaultConversion.byte2int(
                        receiverLength, 0)];
                in.readArray(receiverBytes);
                try {
                    identifier = (TcpReceivePortIdentifier)
                            Conversion.byte2object(receiverBytes);
                    if (identifier.equals(port.identifier())) {
                        if (DEBUG) {
                            System.err.println(port.name
                                    + ": got a disconnect from: " + origin);
                        }
                        close(null);
                        return;
                    }
                    /*
                     * This is wrong. If the sendport is not disconnecting
                     * from me, I don't need to close (or the sendport that
                     * disconnects should create a new stream as well).
                     * (Ceriel)
                    in.close();
                    createNewStream();
                    */
                } catch(ClassNotFoundException e) {
                    System.err.println("TcpIbis: internal error, "
                            + port.name + ": disconnect from: " + origin
                            + " failed: " + e);
                }
                break;
            default:
                throw new IbisError(port.name + " EEK TcpReceivePort: "
                        + "run: got illegal opcode: " + opcode + " from: "
                        + origin);
            }
        }
    }
}
