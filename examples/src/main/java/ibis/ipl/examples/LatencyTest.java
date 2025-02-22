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
package ibis.ipl.examples;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

/**
 * Example of a client application. The server waits until a request comes in,
 * and sends a reply (in this case the current time). This application shows a
 * combination of two port types. One is a many-to-one port with upcalls, the
 * other a one-to-one port with explicit receive.
 */

public class LatencyTest {

    static final int COUNT = 100000;

    IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.CLOSED_WORLD, IbisCapabilities.ELECTIONS_STRICT, IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

    /**
     * Port type used for sending a request to the server
     */
    PortType portType = new PortType(PortType.COMMUNICATION_RELIABLE,
            PortType.SERIALIZATION_BYTE, PortType.RECEIVE_EXPLICIT,
            PortType.CONNECTION_ONE_TO_ONE);

    private final Ibis myIbis;
    SendPort sendPort;
    ReceivePort receivePort;

    /**
     * Constructor. Actually does all the work too :)
     */
    private LatencyTest() throws Exception {
        // Create an ibis instance.
        // Notice createIbis uses varargs for its parameters.
        myIbis = IbisFactory.createIbis(ibisCapabilities, null,
                portType);

        // Elect a server
        IbisIdentifier server = myIbis.registry().elect("Server");

        // If I am the server, run server, else run client.
        if (server.equals(myIbis.identifier())) {
            server();
        } else {
            client(server);
        }

        // End ibis.
        myIbis.end();
    }

    private void server() throws Exception {

        myIbis.registry().waitUntilPoolClosed();

        IbisIdentifier client = null;
        IbisIdentifier[] ibises = myIbis.registry().joinedIbises();
        for(IbisIdentifier i : ibises) {
            if(!i.equals(myIbis.identifier())) {
                client = i;
                break;
            }
        }

        if(client == null) {
            System.err.println("eek");
            System.exit(1);
        }

        // Create a receive port, pass ourselves as the message upcall
        // handler
        receivePort = myIbis.createReceivePort(portType,
                "server");
        // enable connections
        receivePort.enableConnections();

        System.err.println("server started");

        // Create a send port for sending the request and connect.
        sendPort = myIbis.createSendPort(portType);
        sendPort.connect(client, "client");
        

        // warmup
        for(int i=0; i<COUNT; i++) {
            ReadMessage request = receivePort.receive();
            request.readByte();
            request.finish();
           
            WriteMessage reply = sendPort.newMessage();
            reply.writeByte((byte)1);
            reply.finish();
        }

        // start test
        long start = System.nanoTime();

        for(int i=0; i<COUNT; i++) {
            ReadMessage request = receivePort.receive();
            request.readByte();
            request.finish();
           
            WriteMessage reply = sendPort.newMessage();
            reply.writeByte((byte)1);
            reply.finish();
        }

        long elapsed = System.nanoTime() - start;
        double microsPerCall = (double) elapsed / (1000.0 * COUNT);
        System.err.println("roundtrip latency with downcalls = " + microsPerCall + " us / call");

        // Close receive port.
        sendPort.close();
        receivePort.close();
    }

    private void client(IbisIdentifier server) throws IOException {

        // Create a send port for sending the request and connect.
        sendPort = myIbis.createSendPort(portType);
        sendPort.connect(server, "server");

        // Create a receive port for receiving the reply from the server
        receivePort = myIbis.createReceivePort(portType, "client");
        receivePort.enableConnections();

        // warmup
        for(int i=0; i<COUNT; i++) {
            WriteMessage request = sendPort.newMessage();
            request.writeByte((byte)0);
            request.finish();

            ReadMessage reply = receivePort.receive();
            reply.readByte();
            reply.finish();
        }

        // real test
        for(int i=0; i<COUNT; i++) {
            WriteMessage request = sendPort.newMessage();
            request.writeByte((byte)0);
            request.finish();

            ReadMessage reply = receivePort.receive();
            reply.readByte();
            reply.finish();
        }
     
        // Close ports.
        sendPort.close();
        receivePort.close();
    }

    public static void main(String args[]) {
        try {
            new LatencyTest();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
