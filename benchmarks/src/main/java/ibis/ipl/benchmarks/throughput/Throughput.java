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
package ibis.ipl.benchmarks.throughput;

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

import java.io.IOException;
import java.nio.ByteBuffer;

public class Throughput {

    int count = 1000;

    int nIters = 10;

    int transferSize = 0;

    int rank;

    int remoteRank;

    int windowSize = Integer.MAX_VALUE;

    ReceivePort rport;

    SendPort sport;

    byte[] data = null;
    ByteBuffer b = null;

    boolean bb = false;

    public static void main(String[] args) {
        new Throughput(args).run();
    }

    void send() throws IOException {
        int w = windowSize;

        System.err.println("count = " + count + " len = " + transferSize);
        for (int i = 0; i < count; i++) {
            WriteMessage writeMessage = sport.newMessage();
	    if (data != null) {
                writeMessage.writeArray(data);
	    } else if (b != null) {
                writeMessage.writeByteBuffer(b);
            }
            writeMessage.finish();

            if (--w == 0) {
                System.err.println("EEEEEEEEEEEK");
                ReadMessage readMessage = rport.receive();
		if (data != null) {
		    readMessage.readArray(data);
		} else if (b != null) {
                    readMessage.readByteBuffer(b);
                }
                readMessage.finish();
                w = windowSize;
            }
        }
        ReadMessage readMessage = rport.receive();
        readMessage.finish();
    }

    void rcve() throws IOException {
        int w = windowSize;
        for (int i = 0; i < count; i++) {
            ReadMessage readMessage = rport.receive();
	    if (data != null) {
	        readMessage.readArray(data);
	    } else if (b != null) {
                readMessage.readByteBuffer(b);
            }
            readMessage.finish();

            if (--w == 0) {
                System.err.println("EEEEEEEEEEEK");
                WriteMessage writeMessage = sport.newMessage();
		if (data != null) {
                    writeMessage.writeArray(data);
		} else if (b != null) {
                    writeMessage.writeByteBuffer(b);
                }
                writeMessage.finish();
                w = windowSize;
            }
        }
        WriteMessage writeMessage = sport.newMessage();
        writeMessage.finish();
    }

    Throughput(String[] args) {
        /* parse the commandline */
        int options = 0;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-bb")) {
                bb = true;
            } else if (args[i].equals("-window")) {
                windowSize = Integer.parseInt(args[++i]);
                if (windowSize <= 0) {
                    windowSize = Integer.MAX_VALUE;
                }
            } else if (args[i].equals("-iters")) {
                nIters = Integer.parseInt(args[++i]);
                if (nIters <= 0) {
                    nIters = 10;
                }
            } else if (options == 0) {
                count = Integer.parseInt(args[i]);
                options++;
            } else if (options == 1) {
                transferSize = Integer.parseInt(args[i]);
                options++;
            }
        }

        if (options != 2) {
            System.err.println("Throughput <count> <size>");
            System.exit(11);
        }

	if (transferSize > 0) {
            if (! bb) {
                data = new byte[transferSize];
            } else {
                b = ByteBuffer.allocateDirect(transferSize);
            }
	}
    }

    public void run() {
        try {
            IbisCapabilities s = new IbisCapabilities(
                    IbisCapabilities.ELECTIONS_STRICT
                    );
            PortType t = new PortType(
                    PortType.SERIALIZATION_BYTE,
                    PortType.CONNECTION_ONE_TO_ONE,
                    PortType.COMMUNICATION_RELIABLE,
                    PortType.RECEIVE_EXPLICIT);
                     
            Ibis ibis = IbisFactory.createIbis(s, null, true, null, t);

            Registry r = ibis.registry();

            IbisIdentifier master = r.elect("throughput");
            IbisIdentifier remote;

            if (master.equals(ibis.identifier())) {
                rank = 0;
                remote = r.getElectionResult("1");
            } else {
                r.elect("1");
                rank = 1;
                remote = master;
            }

            rport = ibis.createReceivePort(t, "test port");
            rport.enableConnections();
            sport = ibis.createSendPort(t);
            sport.connect(remote, "test port");

            if (rank == 0) {
                for (int i = 0; i < nIters; i++) {
                    long time = System.currentTimeMillis();
                    send();
                    time = System.currentTimeMillis() - time;
                    double speed = (time * 1000.0) / count;
                    System.out.print("Latency: " + count + " calls took "
                            + (time / 1000.0) + " seconds, time/call = " + speed
                            + " micros, ");
                    if (data != null || b != null) {
                        double dataSent = ((double) transferSize * (count + count
                                / windowSize))
                                / (1024.0 * 1024.0);
                        System.out.println("Throughput: "
                                + (dataSent / (time / 1000.0)) + " MByte/s");
                    } else {
                        System.out.println("");
                    }
                }
            } else {
                for (int i = 0; i < nIters; i++) {
                    rcve();
                }
            }

            /* free the send ports first */
            sport.close();
            rport.close();
            ibis.end();
        } catch (Exception e) {
            System.out.println("Got exception " + e);
            System.out.println("StackTrace:");
            e.printStackTrace();
        }
    }
}
