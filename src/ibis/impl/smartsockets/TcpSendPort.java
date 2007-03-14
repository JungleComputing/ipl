/* $Id: TcpSendPort.java 5108 2007-02-27 15:35:17Z ceriel $ */

package ibis.impl.smartsockets;

import ibis.impl.Ibis;
import ibis.impl.IbisIdentifier;
import ibis.impl.ReceivePortIdentifier;
import ibis.impl.SendPort;
import ibis.impl.SendPortConnectionInfo;
import ibis.impl.SendPortIdentifier;
import ibis.impl.WriteMessage;
import ibis.io.BufferedArrayOutputStream;
import ibis.io.Conversion;
import ibis.io.OutputStreamSplitter;
import ibis.io.Replacer;
import ibis.io.SplitterException;
import ibis.ipl.SendPortDisconnectUpcall;

import java.io.IOException;
import java.io.OutputStream;
import smartsockets.virtual.VirtualSocket;

final class TcpSendPort extends SendPort implements TcpProtocol {

    private class Conn extends SendPortConnectionInfo {
        VirtualSocket s;
        OutputStream out;

        Conn(VirtualSocket s, TcpSendPort port, ReceivePortIdentifier target)
                throws IOException {
            super(port, target);
            this.s = s;
            out = s.getOutputStream();
            splitter.add(out);
        }

        public void closeConnection() {
            try {
                s.close();
            } catch(Throwable e) {
                // ignored
            } finally {
                splitter.remove(out);
            }
        }
    }

    final OutputStreamSplitter splitter;

    final BufferedArrayOutputStream bufferedStream;

    TcpSendPort(Ibis ibis, TcpPortType type, String name,
            boolean connectionDowncalls, SendPortDisconnectUpcall cU)
            throws IOException {
        super(ibis, type, name, cU, connectionDowncalls);

        splitter = new OutputStreamSplitter(connectionDowncalls || (cU != null),
                false);

        bufferedStream = new BufferedArrayOutputStream(splitter);
        initStream(bufferedStream);
    }

    SendPortIdentifier getIdent() {
        return ident;
    }

    protected SendPortConnectionInfo doConnect(ReceivePortIdentifier receiver,
            long timeoutMillis) throws IOException {
        VirtualSocket s =
                ((TcpIbis)ibis).connect(this, receiver, (int) timeoutMillis);
        Conn c = new Conn(s, this, receiver);
        if (out != null) {
            out.writeByte(NEW_RECEIVER);
        }
        initStream(bufferedStream);
        return c;
    }

    protected void disconnectPort(ReceivePortIdentifier receiver,
            SendPortConnectionInfo conn) throws IOException {

        out.writeByte(CLOSE_ONE_CONNECTION);

        byte[] receiverBytes = receiver.toBytes();
        byte[] receiverLength = new byte[Conversion.INT_SIZE];
        Conversion.defaultConversion.int2byte(receiverBytes.length,
            receiverLength, 0);
        out.writeArray(receiverLength);
        out.writeArray(receiverBytes);
        out.flush();
    }

    protected void announceNewMessage() throws IOException {
        out.writeByte(NEW_MESSAGE);
        if (type.numbered) {
            out.writeLong(ibis.registry().getSeqno(name));
        }
    }

    protected void handleSendException(WriteMessage w, IOException e) {
        if (e instanceof SplitterException) {
            forwardLosses((SplitterException) e);
        }
    }

    private void forwardLosses(SplitterException e) {
        ReceivePortIdentifier[] ports = receivers.keySet().toArray(
                new ReceivePortIdentifier[0]);
        Exception[] exceptions = e.getExceptions();
        OutputStream[] streams = e.getStreams();

        for (int i = 0; i < ports.length; i++) {
            Conn c = (Conn) getInfo(ports[i]);
            for (int j = 0; j < streams.length; j++) {
                if (c.out == streams[j]) {
                    c.closeConnection();
                    lostConnection(ports[i], exceptions[j]);
                    break;
                }
            }
        }
    }

    protected void closePort() {
        try {
            out.writeByte(CLOSE_ALL_CONNECTIONS);
            out.close();
        } catch (Throwable e) {
            // ignored
        }

        out = null;
    }
}
