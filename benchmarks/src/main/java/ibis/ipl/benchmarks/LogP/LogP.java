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
package ibis.ipl.benchmarks.LogP;

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
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class Sender {
    SendPort sport;

    ReceivePort rport;

    Sender(ReceivePort rport, SendPort sport) {
        this.rport = rport;
        this.sport = sport;
    }

    void send(int count, int gapCount, int repeat) throws Exception {
        long time;
	long sendStart, sendTotal;
	long recvStart, recvTotal;
	long nanoStart, nanoTotal;
	long nanoStartTicks, nanoTotalTicks;
	int timeCount;

        System.out.println("LogP:" +
	    " repeat " + repeat +
	    " count " + count +
	    " gapCount " + gapCount +
	    " (times in usec)");

	// measure clock overhead of the o_r/o_s tests below
	timeCount = 100000;
	sendTotal = 0;
	nanoStart = System.nanoTime();
	nanoStartTicks = Native.timestamp();
        for (int i = 0; i < timeCount; i++) {
	    sendStart = System.nanoTime();
	    sendTotal += System.nanoTime() - sendStart;
        }
	nanoTotalTicks = Native.timestamp() - nanoStartTicks;
	nanoTotal = System.nanoTime() - nanoStart;
	double clock = (nanoTotal / 1000.0) / timeCount;
	double ratio = ((double) nanoTotalTicks / (double) nanoTotal);

	// rdtsc() overhead
	nanoStart = System.nanoTime();
        for (int i = 0; i < timeCount; i++) {
	    sendStart = Native.timestamp();
	    sendTotal += Native.timestamp() - sendStart;
        }
	nanoTotal = System.nanoTime() - nanoStart;
	double tick = (nanoTotal / 1000.0) / timeCount;

	System.out.println("LogP:" +
	    " overhead: nano " + String.format("%.3f", clock) +
	    " rdtsc " + String.format("%.3f", tick) +
	    "; rdtsc/nano tickrate ratio " + String.format("%.3f", ratio));

        for (int r = 0; r < repeat; r++) {
	    WriteMessage writeMessage;
	    ReadMessage readMessage;

	    // rtt and o_s (send overhead)
	    sendTotal = 0;
	    time = System.nanoTime();
            for (int i = 0; i < count; i++) {
		// do send, measuring overhead
	        sendStart = Native.timestamp();
                writeMessage = sport.newMessage();
                writeMessage.finish();
		sendTotal += Native.timestamp() - sendStart;

                readMessage = rport.receive();
                readMessage.finish();
            }
            time = System.nanoTime() - time;

            double rtt = (time / 1000.0) / count;
	    double sendOverhead = (sendTotal / ratio / 1000.0) / count;

	    time = System.nanoTime();
            for (int i = 0; i < gapCount; i++) {
                writeMessage = sport.newMessage();
                writeMessage.finish();
            }
            time = System.nanoTime() - time;

	    double g = (time / 1000.0) / gapCount;
	    // wait till everything is received:
	    readMessage = rport.receive();
	    readMessage.finish();

	    // o_r (receive overhead)
	    recvTotal = 0;
            for (int i = 0; i < count; i++) {
		writeMessage = sport.newMessage();
		writeMessage.finish();

		// busywait for 2*RTT so that the next message
		// should be pending when we read it away
		recvStart = System.nanoTime() +
		    (long) ((2.0 * rtt) * 1000.0);
		while (System.nanoTime() < recvStart) {
		    // nothing
		}

		// do receive, measuring overhead
	        recvStart = Native.timestamp();
                readMessage = rport.receive();
                readMessage.finish();
		recvTotal += Native.timestamp() - recvStart;
            }

	    double recvOverhead = (recvTotal / ratio / 1000.0) / count;

	    double ovhd = tick / 2;
            System.out.println("LogP:" + 
		" RTT " + String.format("%.2f", (rtt - ovhd)) +
		" os " +  String.format("%.2f", (sendOverhead - ovhd)) +
		" or " +  String.format("%.2f", (recvOverhead - ovhd)) +
		" g " +   String.format("%.2f", g));
        }
    }
}

class ExplicitReceiver {

    SendPort sport;

    ReceivePort rport;

    ExplicitReceiver(ReceivePort rport, SendPort sport) {
        this.rport = rport;
        this.sport = sport;
    }

    void receive(int count, int gapCount, int repeat) throws IOException {
	WriteMessage writeMessage;
	ReadMessage readMessage;

        for (int r = 0; r < repeat; r++) {
	    // rtt/o_s
            for (int i = 0; i < count; i++) {
                readMessage = rport.receive();
                readMessage.finish();

                writeMessage = sport.newMessage();
                writeMessage.finish();
            }

	    // g
            for (int i = 0; i < gapCount; i++) {
                readMessage = rport.receive();
                readMessage.finish();
            }
	    writeMessage = sport.newMessage();
	    writeMessage.finish();

	    // o_r
            for (int i = 0; i < count; i++) {
                readMessage = rport.receive();
                readMessage.finish();

                writeMessage = sport.newMessage();
                writeMessage.finish();
            }
        }
    }
}

class LogP {

    static Logger logger = LoggerFactory.getLogger(LogP.class);

    static Ibis ibis;

    static Registry registry;


    static void usage() {
        System.out.println("Usage: LogP [-ibis] [-none] [count] [gapcount]");
        System.exit(0);
    }

    public static void main(String[] args) {
        int count = -1;
        int gapCount = -1;
        int repeat = 10;
        int rank;
        boolean ibisSer = false;
        boolean noneSer = false;

        /* Parse commandline parameters. */
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-repeat")) {
                i++;
                repeat = Integer.parseInt(args[i]);
            } else if (args[i].equals("-ibis")) {
                ibisSer = true;
            } else if (args[i].equals("-none")) {
                noneSer = true;
            } else {
                if (count == -1) {
                    count = Integer.parseInt(args[i]);
                } else if (gapCount == -1) {
                    gapCount = Integer.parseInt(args[i]);
		} else {
                    usage();
                }
            }
        }

        if (count == -1) {
            count = 10000;
        }

        if (gapCount == -1) {
	    if (count <= 10000) {
            	gapCount = count;
	    } else {
		// by default limit it for the gap since it is one-way
            	gapCount = 10000;
	    }
        }

        try {

            IbisCapabilities s = new IbisCapabilities(
                    IbisCapabilities.ELECTIONS_STRICT
            );
            PortType t = new PortType(
                    noneSer ? PortType.SERIALIZATION_BYTE : PortType.SERIALIZATION_OBJECT,
                    PortType.COMMUNICATION_RELIABLE,
                    PortType.CONNECTION_ONE_TO_ONE,
                    PortType.RECEIVE_AUTO_UPCALLS, PortType.RECEIVE_EXPLICIT);
            Properties p = new Properties();
            if (ibisSer) {
                p.setProperty("ibis.serialization", "ibis");
            } else if (noneSer) {
            } else {
                p.setProperty("ibis.serialization", "sun");
            }

            ibis = IbisFactory.createIbis(s, p, true, null, t);
            registry = ibis.registry();

            SendPort sport = ibis.createSendPort(t, "send port");
            ReceivePort rport;
            logger.debug("LogP: pre elect");
            IbisIdentifier master = registry.elect("logp");
            IbisIdentifier remote;
            logger.debug("LogP: post elect");

            if (master.equals(ibis.identifier())) {
                logger.debug("LogP: I am master");
                remote = registry.getElectionResult("client");
                rank = 0;
            } else {
                logger.debug("LogP: I am slave");
                registry.elect("client");
                rank = 1;
                remote = master;
            }

            if (rank == 0) {
		rport = ibis.createReceivePort(t, "master");
		rport.enableConnections();

		sport.connect(remote, "slave");

		Sender sender = new Sender(rport, sport);
		sender.send(count, gapCount, repeat);
            } else {
                sport.connect(remote, "master");

		rport = ibis.createReceivePort(t, "slave");
		rport.enableConnections();

		ExplicitReceiver receiver = new ExplicitReceiver(rport, sport);
		receiver.receive(count, gapCount, repeat);
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
