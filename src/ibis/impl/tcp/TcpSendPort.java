/* $Id$ */

package ibis.impl.tcp;

import ibis.impl.Ibis;
import ibis.impl.IbisIdentifier;
import ibis.impl.ReceivePortIdentifier;
import ibis.impl.SendPortConnectionInfo;
import ibis.impl.WriteMessage;
import ibis.io.BufferedArrayOutputStream;
import ibis.io.Conversion;
import ibis.io.OutputStreamSplitter;
import ibis.io.SplitterException;
import ibis.ipl.SendPortConnectUpcall;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

final class TcpSendPort extends ibis.impl.SendPort implements TcpProtocol {

    private class Conn extends SendPortConnectionInfo {
        Socket s;
        OutputStream out;

        Conn(Socket s) throws IOException {
            this.s = s;
            out = s.getOutputStream();
        }

        protected void closeConnection() {
            try {
                s.close();
            } catch(IOException e) {
                // ignored
            } finally {
                splitter.remove(out);
            }
        }
    }

    private final OutputStreamSplitter splitter;

    private final BufferedArrayOutputStream bufferedStream;

    TcpSendPort(Ibis ibis, TcpPortType type, String name,
            boolean connectionAdministration, SendPortConnectUpcall cU)
            throws IOException {
        super(ibis, type, name, connectionAdministration, cU);

        boolean connectAdmin = connectionAdministration || (cU != null);

        // if we keep administration, close connections when exception occurs.
        splitter = new OutputStreamSplitter(connectAdmin, connectAdmin);

        bufferedStream = new BufferedArrayOutputStream(splitter);

        initStream(bufferedStream);
    }

    protected ReceivePortIdentifier doConnect(IbisIdentifier id, String nm,
            long timeoutMillis) throws IOException {
        return ((TcpIbis)ibis).connect(this, id, nm, null, (int) timeoutMillis);
    }

    protected void doConnect(ReceivePortIdentifier receiver,
        long timeoutMillis) throws IOException {
        ((TcpIbis)ibis).connect(this, (IbisIdentifier) receiver.ibis(),
                receiver.name(), receiver, (int) timeoutMillis);
    }

    protected void disconnectPort(ReceivePortIdentifier receiver,
            SendPortConnectionInfo conn) throws IOException {

        out.writeByte(CLOSE_ONE_CONNECTION);

        byte[] receiverBytes = ((ReceivePortIdentifier)receiver).getBytes();
        byte[] receiverLength = new byte[Conversion.INT_SIZE];
        Conversion.defaultConversion.int2byte(receiverBytes.length,
            receiverLength, 0);
        out.writeArray(receiverLength);
        out.writeArray(receiverBytes);
        out.flush();
        // Don't close the stream.
    }

    protected void announceNewMessage() throws IOException {
        out.writeByte(NEW_MESSAGE);
        if (numbered) {
            out.writeLong(ibis.getSeqno(name));
        }
    }

    protected void handleSendException(WriteMessage w, IOException e)
            throws IOException {
        if (e instanceof SplitterException) {
            forwardLosses((SplitterException) e);
        } else {
            throw e;
        }
    }

    // If we have connectionUpcalls, forward exception to
    // upcalls. Otherwise, rethrow the exception to the user.
    private void forwardLosses(SplitterException e) throws IOException {
        ReceivePortIdentifier[] ports = receivers.keySet().toArray(
                new ReceivePortIdentifier[0]);

        for (int i = 0; i < ports.length; i++) {
            Conn c = (Conn) getInfo(ports[i]);
            for (int j = 0; j < e.count(); j++) {
                if (c.out == e.getStream(j)) {
                    lostConnection(ports[i], e.getException(j));
                    break;
                }
            }
        }

        if (connectUpcall == null) {
            // otherwise an upcall was/will be done
            throw e;
        }
    }

    protected void closePort() {
        try {
            out.writeByte(CLOSE_ALL_CONNECTIONS);
            out.reset();
            out.close();
        } catch (IOException e) {
            // ignored
        }

        out = null;
    }

    void addConn(ReceivePortIdentifier ri, Socket s) throws IOException {
        Conn c = new Conn(s);
        addConnectionInfo(ri, c);
        splitter.add(c.out);
        out.writeByte(NEW_RECEIVER);
        initStream(bufferedStream);
    }
}
