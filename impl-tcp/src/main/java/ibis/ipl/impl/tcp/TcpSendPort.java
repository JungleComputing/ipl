/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.ipl.impl.tcp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import ibis.io.BufferedArrayOutputStream;
import ibis.io.Conversion;
import ibis.io.OutputStreamSplitter;
import ibis.io.SplitterException;
import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortConnectionInfo;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.ipl.impl.WriteMessage;

final class TcpSendPort extends SendPort implements TcpProtocol {

    private class Conn extends SendPortConnectionInfo {
        IbisSocket s;

        OutputStream out;

        Conn(IbisSocket s, TcpSendPort port, ReceivePortIdentifier target) throws IOException {
            super(port, target);
            this.s = s;
            out = s.getOutputStream();
            splitter.add(out);
        }

        @Override
        public void closeConnection() {
            try {
                s.close();
            } catch (Throwable e) {
                // ignored
            } finally {
                try {
                    splitter.remove(out);
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    final OutputStreamSplitter splitter;

    final BufferedArrayOutputStream bufferedStream;

    TcpSendPort(Ibis ibis, PortType type, String name, SendPortDisconnectUpcall cU, Properties props) throws IOException {
        super(ibis, type, name, cU, props);
        addValidKey("nParallelStreams");

        splitter = new OutputStreamSplitter(
                !type.hasCapability(PortType.CONNECTION_ONE_TO_ONE) && !type.hasCapability(PortType.CONNECTION_MANY_TO_ONE),
                type.hasCapability(PortType.CONNECTION_ONE_TO_MANY) || type.hasCapability(PortType.CONNECTION_MANY_TO_MANY));

        bufferedStream = new BufferedArrayOutputStream(splitter);
        initStream(bufferedStream);
    }

    @Override
    protected long totalWritten() {
        return splitter.bytesWritten();
    }

    @Override
    protected void resetWritten() {
        splitter.resetBytesWritten();
    }

    SendPortIdentifier getIdent() {
        return ident;
    }

    @Override
    protected SendPortConnectionInfo doConnect(ReceivePortIdentifier receiver, long timeoutMillis, boolean fillTimeout) throws IOException {
        IbisSocket s = ((TcpIbis) ibis).connect(this, receiver, (int) timeoutMillis, fillTimeout);
        Conn c = new Conn(s, this, receiver);
        if (out != null) {
            out.writeByte(NEW_RECEIVER);
        }
        initStream(bufferedStream);
        return c;
    }

    @Override
    protected void sendDisconnectMessage(ReceivePortIdentifier receiver, SendPortConnectionInfo conn) throws IOException {
        out.writeByte(CLOSE_ONE_CONNECTION);

        byte[] receiverBytes = receiver.toBytes();
        byte[] receiverLength = new byte[Conversion.INT_SIZE];
        Conversion.defaultConversion.int2byte(receiverBytes.length, receiverLength, 0);
        out.writeArray(receiverLength);
        out.writeArray(receiverBytes);
        out.flush();
        // FIXME!
        //
        // This is here to make sure the close is processed before a new
        // connections can be made (by this sendport). Without this ack,
        // an application that uses a single sendport that connects/disconnects
        // for each message may get an 'AlreadyConnectedException', because the
        // connect overtakes the disconnect...
        //
        // Unfortunately, it also causes a deadlock in 1-to-1 explict receive
        // applications -- J
        Conn c = (Conn) conn;
        c.s.getInputStream().read();
    }

    @Override
    protected void announceNewMessage() throws IOException {
        out.writeByte(NEW_MESSAGE);
        if (type.hasCapability(PortType.COMMUNICATION_NUMBERED)) {
            out.writeLong(ibis.registry().getSequenceNumber(name));
        }
    }

    @Override
    protected void finishMessage(WriteMessage w, long cnt) throws IOException {
        if (type.hasCapability(PortType.CONNECTION_ONE_TO_MANY) || type.hasCapability(PortType.CONNECTION_MANY_TO_MANY)) {
            // exception may have been saved by the splitter. Get them
            // now.
            SplitterException e = splitter.getExceptions();
            if (e != null) {
                gotSendException(w, e);
            }
        }
        super.finishMessage(w, cnt);
    }

    @Override
    protected void handleSendException(WriteMessage w, IOException x) {
        ReceivePortIdentifier[] ports = null;
        synchronized (this) {
            ports = receivers.keySet().toArray(new ReceivePortIdentifier[0]);
        }

        if (x instanceof SplitterException) {
            SplitterException e = (SplitterException) x;

            Exception[] exceptions = e.getExceptions();
            OutputStream[] streams = e.getStreams();

            for (ReceivePortIdentifier port : ports) {
                Conn c = (Conn) getInfo(port);
                for (int j = 0; j < streams.length; j++) {
                    if (c.out == streams[j]) {
                        lostConnection(port, exceptions[j]);
                        break;
                    }
                }
            }
        } else {
            // Just close all connections. ???
            for (ReceivePortIdentifier port : ports) {
                lostConnection(port, x);
            }
        }
    }

    @Override
    protected void closePort() {

        try {
            out.writeByte(CLOSE_ALL_CONNECTIONS);
            out.close();
            bufferedStream.close();
        } catch (Throwable e) {
            // ignored
        }

        out = null;
    }

}
