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

import java.io.IOException;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

/**
 * This program is to be run as two instances. One is a server, the other a
 * client. The client sends a hello message to the server. The server prints it.
 * This version uses explicit receive.
 */

public class Hello {

    PortType portType = new PortType(PortType.COMMUNICATION_RELIABLE, PortType.SERIALIZATION_DATA, PortType.RECEIVE_EXPLICIT,
            PortType.CONNECTION_ONE_TO_ONE);

    IbisCapabilities ibisCapabilities = new IbisCapabilities(IbisCapabilities.ELECTIONS_STRICT);

    private void server(Ibis myIbis) throws IOException {

        // Create a receive port and enable connections.
        ReceivePort receiver = myIbis.createReceivePort(portType, "server");
        receiver.enableConnections();

        // Read the message.
        ReadMessage r = receiver.receive();
        String s = r.readString();
        r.finish();
        System.out.println("Server received: " + s);

        // Close receive port.
        receiver.close();
    }

    private void client(Ibis myIbis, IbisIdentifier server) throws IOException {

        // Create a send port for sending requests and connect.
        SendPort sender = myIbis.createSendPort(portType);
        sender.connect(server, "server");

        // Send the message.
        WriteMessage w = sender.newMessage();
        w.writeString("Hi there");
        w.finish();

        // Close ports.
        sender.close();
    }

    private void run() throws Exception {
        // Create an ibis instance.
        Ibis ibis = IbisFactory.createIbis(ibisCapabilities, null, portType);

        // Elect a server
        IbisIdentifier server = ibis.registry().elect("Server");

        System.out.println("Server is " + server);

        // If I am the server, run server, else run client.
        if (server.equals(ibis.identifier())) {
            server(ibis);
        } else {
            client(ibis, server);
        }

        // End ibis.
        ibis.end();
    }

    public static void main(String args[]) {
        try {
            new Hello().run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
