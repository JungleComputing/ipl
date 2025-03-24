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
package ibis.ipl.benchmarks.pingPong;

import java.io.IOException;

/* $Id$ */

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

class PollingPingPong {

    static class Sender {
        SendPort sport;
        ReceivePort rport;

        Sender(ReceivePort rport, SendPort sport) {
            this.rport = rport;
            this.sport = sport;
        }

        void send(int count, int repeat) throws Exception {
            for (int r = 0; r < repeat; r++) {

                long time = System.currentTimeMillis();

                for (int i = 0; i < count; i++) {
                    WriteMessage writeMessage = sport.newMessage();
                    writeMessage.finish();

                    ReadMessage readMessage = null;
                    while (readMessage == null) {
                        readMessage = rport.poll();
                    }
                    readMessage.finish();
//		System.err.print(".");
                }

                time = System.currentTimeMillis() - time;

                double speed = (time * 1000.0) / count;
                System.err.println("Latency: " + count + " calls took " + (time / 1000.0) + " seconds, time/call = " + speed + " micros");
            }
        }
    }

    static class ExplicitReceiver {

        SendPort sport;

        ReceivePort rport;

        ExplicitReceiver(ReceivePort rport, SendPort sport) {
            this.rport = rport;
            this.sport = sport;
        }

        void receive(int count, int repeat) throws IOException {
            for (int r = 0; r < repeat; r++) {
                for (int i = 0; i < count; i++) {

                    ReadMessage readMessage = null;
                    while (readMessage == null) {
                        readMessage = rport.poll();
                    }
                    readMessage.finish();

                    WriteMessage writeMessage = sport.newMessage();
                    writeMessage.finish();
                }
            }
        }
    }

    static Ibis ibis;

    static Registry registry;

    public static void main(String[] args) {
        int count = 100000;
        int repeat = 10;
        int rank = 0;

        try {
            IbisCapabilities s = new IbisCapabilities(IbisCapabilities.CLOSED_WORLD, IbisCapabilities.ELECTIONS_STRICT);

            PortType t = new PortType(PortType.SERIALIZATION_OBJECT, PortType.CONNECTION_ONE_TO_ONE, PortType.COMMUNICATION_RELIABLE,
                    PortType.RECEIVE_EXPLICIT, PortType.RECEIVE_POLL);

            ibis = IbisFactory.createIbis(s, null, t);

            registry = ibis.registry();

            SendPort sport = ibis.createSendPort(t, "send port");
            ReceivePort rport;
//            Latency lat = null;

            IbisIdentifier master = registry.elect("latency");
            IbisIdentifier remote;

            if (master.equals(ibis.identifier())) {
                rank = 0;
                remote = registry.getElectionResult("client");
            } else {
                registry.elect("client");
                rank = 1;
                remote = master;
            }

            Sender sender = null;
            ExplicitReceiver receiver = null;
            if (rank == 0) {
                rport = ibis.createReceivePort(t, "test port");
                rport.enableConnections();
                sport.connect(remote, "test port");
                sender = new Sender(rport, sport);
                sender.send(count, repeat);
            } else {
                sport.connect(remote, "test port");
                rport = ibis.createReceivePort(t, "test port");
                rport.enableConnections();
                receiver = new ExplicitReceiver(rport, sport);
                receiver.receive(count, repeat);
            }

            /* free the send ports first */
            sport.close();
            rport.close();
            ibis.end();
        } catch (Exception e) {
            System.err.println("Got exception " + e);
            System.err.println("StackTrace:");
            e.printStackTrace();
        }
    }
}
