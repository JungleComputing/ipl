/* $Id$ */


import ibis.ipl.*;

import java.util.Properties;
import java.util.Random;
import java.util.HashMap;

import java.io.IOException;

interface Config {
    static final boolean DEBUG = false;
}

class Sender extends Thread implements Config {
    int count, repeat;

    Ibis ibis;

    PortType t;

    boolean sendTree;

    Sender(Ibis ibis, PortType t, int count, int repeat, boolean sendTree) {
        this.ibis = ibis;
        this.t = t;
        this.count = count;
        this.repeat = repeat;
        this.sendTree = sendTree;
    }

    public void run() {
        DITree tree = null;
        try {
            if (sendTree) {
                tree = new DITree(1023);
            }

            SendPort sport = t.createSendPort("send port");
            ReceivePort rport;

            ReceivePortIdentifier ident = ibis.registry().lookupReceivePort(
                    "receive port");
            sport.connect(ident);

            System.err.println(this
                    + ": Connection established -- I'm a Sender");
            long totalTime = System.currentTimeMillis();

            for (int r = 0; r < repeat; r++) {

                long time = System.currentTimeMillis();

                for (int i = 0; i < count; i++) {
                    WriteMessage writeMessage = sport.newMessage();
                    if (DEBUG) {
                        System.out.println("LAT: send message");
                    }
                    if (sendTree) {
                        writeMessage.writeObject(tree);
                    } else {
                        writeMessage.writeObject("total world domination");
                    }

                    if (DEBUG) {
                        System.out.println("LAT: finish message");
                    }
                    writeMessage.finish();
                    if (DEBUG) {
                        System.out.println("LAT: message done");
                    }
                }

                time = System.currentTimeMillis() - time;

                double speed = (time * 1000.0) / (double) count;
                System.err.println("SENDER: " + count + " msgs took "
                        + (time / 1000.0) + " seconds, time/msg = " + speed
                        + " micros");
            }

            totalTime = System.currentTimeMillis() - totalTime;
            System.err.println("SENDER: TOTAL TIME is " + (totalTime / 1000.0)
                    + " seconds");

            System.err.println("sender done, freeing sport");
            sport.close();
            System.err.println("sender done, terminating ibis");
            ibis.end();
            System.err.println("sender done, exit");
        } catch (Exception e) {
            System.err.println("got exception: " + e);
            e.printStackTrace();
        }
    }
}

class Receiver implements Upcall {
    int count;

    int repeat;

    boolean done = false;

    boolean doFinish;

    Ibis ibis;

    PortType t;

    int msgs = 0;

    int senders;

    Receiver(Ibis ibis, PortType t, int count, int repeat, int senders,
            boolean doFinish) {
        this.ibis = ibis;
        this.t = t;
        this.count = count;
        this.repeat = repeat;
        this.senders = senders;
        System.err.println(this + ": I'm a Receiver");

        try {
            ReceivePort rport = t.createReceivePort("receive port", this);
            rport.enableConnections();

            long time = System.currentTimeMillis();
            rport.enableUpcalls();
            finish();
            time = System.currentTimeMillis() - time;
            double speed = (time * 1000.0) / (double) count;
            System.err.println("RECEIVEVER: " + count + " msgs took "
                    + (time / 1000.0) + " seconds");

            System.err.println("receiver done, freeing rport");
            rport.close();
            System.err.println("receiver done, terminating ibis");
            ibis.end();
            System.err.println("receiver exit");
        } catch (Exception e) {
            System.err.println("got exception: " + e);
            e.printStackTrace();
        }
    }

    public void upcall(ReadMessage readMessage) {
        //		System.err.println("Got readMessage!!");

        try {
            readMessage.readObject();

            if (doFinish) {
                readMessage.finish();
            }

            synchronized (this) {
                msgs++;
                if (msgs == count * repeat * senders) {
                    done = true;
                    notifyAll();
                }
            }
        } catch (Exception e) {
            System.err.println("EEEEEK " + e);
            e.printStackTrace();
        }
    }

    synchronized void finish() {
        while (!done) {
            try {
                wait();
            } catch (Exception e) {
            }
        }

        System.err.println("Finished Receiver");
    }
}

class ConcurrentSenders implements Config {

    static Ibis ibis;

    static Registry registry;

    static void usage() {
        System.out.println("Usage: ConcurrentReceives [-ibis]");
        System.exit(0);
    }

    public static void main(String[] args) {
        boolean doFinish = false;
        boolean sendTree = false;
        int count = 100;
        int repeat = 10;
        int rank = 0;
        int senders = 2;
        Random r = new Random();

        /* Parse commandline parameters. */
        for (int i = 0; i < args.length; i++) {
            if (false) {
            } else if (args[i].equals("-tree")) {
                sendTree = true;
            } else if (args[i].equals("-count")) {
                try {
                    count = Integer.parseInt(args[++i]);
                } catch (Exception e) {
                    System.err.println(args[i] + " must be integer");
                }
            } else if (args[i].equals("-repeat")) {
                try {
                    repeat = Integer.parseInt(args[++i]);
                } catch (Exception e) {
                    System.err.println(args[i] + " must be integer");
                }
            } else if (args[i].equals("-senders")) {
                try {
                    senders = Integer.parseInt(args[++i]);
                } catch (Exception e) {
                    System.err.println("senders must be integer");
                }
            } else if (args[i].equals("-finish")) {
                doFinish = true;
            } else {
                usage();
            }
        }

        try {
            StaticProperties sp = new StaticProperties();
            sp.add("serialization", "object");
            sp
                    .add("communication",
                            "OneToOne, ManyToOne, OneToMany, Reliable, ExplicitReceipt, AutoUpcalls");
            sp.add("worldmodel", "closed");

            ibis = Ibis.createIbis(sp, null);

            registry = ibis.registry();

            if (DEBUG) {
                System.out.println("LAT: pre elect");
            }
            IbisIdentifier master = registry.elect("latency");
            if (DEBUG) {
                System.out.println("LAT: post elect");
            }

            if (master.equals(ibis.identifier())) {
                if (DEBUG) {
                    System.out.println("LAT: I am master");
                }
                rank = 0;
            } else {
                if (DEBUG) {
                    System.out.println("LAT: I am slave");
                }
                rank = 1;
            }

            PortType t = ibis.createPortType("test type", sp);

            if (rank == 0) {
                new Receiver(ibis, t, count, repeat, senders, doFinish);
            } else {
                // start N senders
                for (int i = 0; i < senders; i++) {
                    new Sender(ibis, t, count, repeat, sendTree).start();
                }
            }

        } catch (Exception e) {
            System.err.println("Got exception " + e);
            System.err.println("StackTrace:");
            e.printStackTrace();
        }
    }
}