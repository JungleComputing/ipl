/* $Id$ */


import ibis.ipl.*;

import java.util.Properties;
import java.util.Random;

import java.io.IOException;

import org.apache.log4j.Logger;

class Computer extends Thread {

    boolean stop = false;

    long cycles = 0;

    long start = 0;

    final synchronized void printCycles(String temp) {

        long tmp = start;
        start = System.currentTimeMillis();

        double result = ((double) cycles) / ((start - tmp) / 1000.0);
        cycles = 0;

        System.err.println(temp + " cycles/s " + result);
    }

    final synchronized void reset() {
        start = System.currentTimeMillis();
        cycles = 0;
    }

    final synchronized void setStop() {
        stop = true;
    }

    final synchronized boolean getStop() {
        return stop;
    }

    final void flip(double[] src, double[] dst, double mult) {
        for (int i = 0; i < src.length; i++) {
            dst[i] = mult * src[src.length - i - 1];
        }
    }

    public void run() {

        double[] a = new double[4096];
        double[] b = new double[4096];

        for (int i = 0; i < 4096; i++) {
            a[i] = i * 0.8;
        }

        start = System.currentTimeMillis();

        while (!getStop()) {
            synchronized (this) {
                cycles++;
            }
            flip(a, b, 0.5);
            flip(a, b, 2.0);
        }
    }
}

class Sender {
    SendPort sport;

    ReceivePort rport;

    Sender(ReceivePort rport, SendPort sport) {
        this.rport = rport;
        this.sport = sport;
    }

    void send(int count, int repeat, Computer c) throws Exception {

        for (int r = 0; r < repeat; r++) {

            long time = System.currentTimeMillis();

            for (int i = 0; i < count; i++) {
                WriteMessage writeMessage = sport.newMessage();
                Latency.logger.debug("LAT: finish message");
                writeMessage.finish();
                Latency.logger.debug("LAT: message done");

                ReadMessage readMessage = rport.receive();
                readMessage.finish();
            }

            time = System.currentTimeMillis() - time;

            double speed = (time * 1000.0) / (double) count;
            System.err.println("Latency: " + count + " calls took "
                    + (time / 1000.0) + " seconds, time/call = " + speed
                    + " micros");
            if (c != null)
                c.printCycles("Sender");
        }
    }
}

class ExplicitReceiver {

    SendPort sport;

    ReceivePort rport;

    Computer c;

    ExplicitReceiver(ReceivePort rport, SendPort sport, Computer c) {
        this.rport = rport;
        this.sport = sport;
        this.c = c;
    }

    void receive(int count, int repeat) throws IOException {

        for (int r = 0; r < repeat; r++) {
            for (int i = 0; i < count; i++) {
                Latency.logger.debug("LAT: in receive");
                ReadMessage readMessage = rport.receive();
                Latency.logger.debug("LAT: receive done");
                readMessage.finish();
                Latency.logger.debug("LAT: finish done");

                WriteMessage writeMessage = sport.newMessage();
                writeMessage.finish();
            }
            if (c != null)
                c.printCycles("Server");
        }
    }
}

class UpcallReceiver implements Upcall {
    SendPort sport;

    Computer c;

    int count = 0;

    int max;

    int repeat;

    boolean earlyFinish;

    boolean delayedFinish;

    UpcallReceiver(SendPort sport, int max, boolean earlyFinish,
            boolean delayedFinish, int repeat, Computer c) {
        this.sport = sport;
        this.c = c;
        this.repeat = repeat;
        this.max = max;
        this.earlyFinish = earlyFinish;
        this.delayedFinish = delayedFinish;
    }

    public void upcall(ReadMessage readMessage) {

        //		System.err.println("Got readMessage!!");

        try {
            if (earlyFinish) {
                readMessage.finish();
            }

            WriteMessage writeMessage = sport.newMessage();
            writeMessage.finish();

            count++;

            if (c != null && (count % max == 0))
                c.printCycles("Server");

            if (count == max * repeat) {
                synchronized (this) {
                    notifyAll();
                }
            }

            if (delayedFinish) {
                readMessage.finish();
            }

        } catch (Exception e) {
            System.err.println("EEEEEK " + e);
            e.printStackTrace();
        }
    }

    synchronized void finish() {
        while (count < max * repeat) {
            try {
                //				System.err.println("Jikes");
                wait();
            } catch (Exception e) {
            }
        }

        System.err.println("Finished Receiver");

    }
}

class UpcallSender implements Upcall {
    SendPort sport;

    int count, max;

    long time;

    int repeat;

    Computer c;

    boolean earlyFinish;

    boolean delayedFinish;

    UpcallSender(SendPort sport, int count, boolean earlyFinish,
            boolean delayedFinish, int repeat, Computer c) {
        this.sport = sport;
        this.count = 0;
        this.max = count;
        this.repeat = repeat;
        this.c = c;
        this.earlyFinish = earlyFinish;
        this.delayedFinish = delayedFinish;
    }

    public void start() {
        try {
            System.err.println("Starting " + count);
            WriteMessage writeMessage = sport.newMessage();
            writeMessage.finish();
        } catch (Exception e) {
            System.err.println("EEEEEK " + e);
            e.printStackTrace();
        }
    }

    public void upcall(ReadMessage readMessage) {
        try {
            if (earlyFinish) {
                readMessage.finish();
            }

            //			System.err.println("Sending " + count);

            if (count == 0) {
                time = System.currentTimeMillis();
            }

            count++;

            if (count == max) {
                long temp = time;
                time = System.currentTimeMillis();
                double speed = ((time - temp) * 1000.0) / (double) max;
                System.err.println("Latency: " + max + " calls took "
                        + ((time - temp) / 1000.0) + " seconds, time/call = "
                        + speed + " micros");

                count = 0;
                repeat--;
                if (repeat == 0) {
                    synchronized (this) {
                        notifyAll();
                    }
                    return;
                }
            }

            Latency.logger.debug("SEND pre new");
            WriteMessage writeMessage = sport.newMessage();
            Latency.logger.debug("SEND pre fin");
            writeMessage.finish();
            Latency.logger.debug("SEND post fin");

            if (delayedFinish) {
                readMessage.finish();
            }

        } catch (Exception e) {
            System.err.println("EEEEEK " + e);
            e.printStackTrace();
        }

    }

    synchronized void finish() {
        while (repeat != 0) {
            try {
                //				System.err.println("EEK");
                wait();
            } catch (Exception e) {
            }
        }

        System.err.println("Finished Sender " + count + " " + max);
    }
}

class Latency {

    static Logger logger = Logger.getLogger(Latency.class.getName());

    static Ibis ibis;

    static Registry registry;

    public static void connect(SendPort s, ReceivePortIdentifier ident) {
        boolean success = false;
        do {
            try {
                s.connect(ident);
                success = true;
            } catch (Exception e) {
                try {
                    Thread.sleep(500);
                } catch (Exception e2) {
                    // ignore
                }
            }
        } while (!success);
    }

    public static ReceivePortIdentifier lookup(String name) throws Exception {

        ReceivePortIdentifier temp = null;

        do {
            temp = registry.lookupReceivePort(name);

            if (temp == null) {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    // ignore
                }
            }

        } while (temp == null);

        return temp;
    }

    static void usage() {
        System.out.println("Usage: Latency [-u] [-uu] [-ibis] [count]");
        System.exit(0);
    }

    public static void main(String[] args) {
        boolean upcalls = false;
        boolean upcallsend = false;
        boolean ibisSer = false;
        int count = -1;
        int repeat = 10;
        int rank = 0, remoteRank = 1;
        Random r = new Random();
        boolean compRec = false;
        boolean compSnd = false;
        Computer c = null;
        boolean earlyFinish = false;
        boolean delayedFinish = false;
        boolean noneSer = false;

        /* Parse commandline parameters. */
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-u")) {
                upcalls = true;
            } else if (args[i].equals("-uu")) {
                upcalls = true;
                upcallsend = true;
            } else if (args[i].equals("-repeat")) {
                i++;
                repeat = Integer.parseInt(args[i]);
            } else if (args[i].equals("-ibis")) {
                ibisSer = true;
            } else if (args[i].equals("-none")) {
                noneSer = true;
            } else if (args[i].equals("-comp-rec")) {
                compRec = true;
            } else if (args[i].equals("-comp-snd")) {
                compSnd = true;
            } else if (args[i].equals("-early-finish")) {
                earlyFinish = true;
            } else if (args[i].equals("-delayed-finish")) {
                delayedFinish = true;
            } else {
                if (count == -1) {
                    count = Integer.parseInt(args[i]);
                } else {
                    usage();
                }
            }
        }

        if (count == -1) {
            count = 10000;
        }

        try {

            StaticProperties s = new StaticProperties();
            if (ibisSer) {
                s.add("Serialization", "ibis");
            } else if (noneSer) {
                s.add("Serialization", "byte");
            } else
                s.add("Serialization", "sun");

            s.add("Communication",
                    "OneToOne, Reliable, AutoUpcalls, ExplicitReceipt");
            s.add("worldmodel", "open");
            ibis = Ibis.createIbis(s, null);

            registry = ibis.registry();

            PortType t = ibis.createPortType("test type", s);

            SendPort sport = t.createSendPort("send port");
            ReceivePort rport;
            Latency lat = null;

            logger.debug("LAT: pre elect");
            IbisIdentifier master = registry.elect("latency");
            logger.debug("LAT: post elect");

            if (master.equals(ibis.identifier())) {
                logger.debug("LAT: I am master");
                rank = 0;
                remoteRank = 1;
            } else {
                logger.debug("LAT: I am slave");
                rank = 1;
                remoteRank = 0;
            }

            if (rank == 0) {
                if (compSnd) {
                    c = new Computer();
                    c.setDaemon(true);
                    c.start();
                }

                if (!upcallsend) {
                    rport = t.createReceivePort("test port 0");
                    rport.enableConnections();
                    ReceivePortIdentifier ident = lookup("test port 1");
                    connect(sport, ident);
                    Sender sender = new Sender(rport, sport);

                    logger.debug("LAT: starting send test");
                    sender.send(count, repeat, c);
                } else {
                    UpcallSender sender = new UpcallSender(sport, count,
                            earlyFinish, delayedFinish, repeat, c);
                    rport = t.createReceivePort("test port 0", sender);
                    rport.enableConnections();

                    ReceivePortIdentifier ident = lookup("test port 1");
                    connect(sport, ident);

                    rport.enableUpcalls();

                    sender.start();
                    sender.finish();
                }
            } else {
                ReceivePortIdentifier ident = lookup("test port 0");
                connect(sport, ident);

                if (compRec) {
                    c = new Computer();
                    c.setDaemon(true);
                    c.start();
                }

                if (upcalls) {
                    UpcallReceiver receiver = new UpcallReceiver(sport, count,
                            earlyFinish, delayedFinish, repeat, c);
                    rport = t.createReceivePort("test port 1", receiver);
                    rport.enableConnections();
                    rport.enableUpcalls();
                    receiver.finish();
                } else {
                    rport = t.createReceivePort("test port 1");
                    rport.enableConnections();

                    ExplicitReceiver receiver = new ExplicitReceiver(rport,
                            sport, c);
                    logger.debug("LAT: starting test receiver");
                    receiver.receive(count, repeat);
                }
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
