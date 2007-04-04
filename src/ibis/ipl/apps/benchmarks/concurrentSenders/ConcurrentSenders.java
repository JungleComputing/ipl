/* $Id$ */


import ibis.ipl.*;

import java.util.Properties;
import java.util.Random;
import java.util.HashMap;

import java.io.IOException;

import org.apache.log4j.Logger;

class Sender extends Thread {
    int count, repeat;

    Ibis ibis;

    CapabilitySet t;

    boolean sendTree;

    IbisIdentifier master;

    Sender(Ibis ibis, CapabilitySet t, int count, int repeat, boolean sendTree,
            IbisIdentifier master) {
        this.ibis = ibis;
        this.t = t;
        this.count = count;
        this.repeat = repeat;
        this.sendTree = sendTree;
        this.master = master;
    }

    public void run() {
        DITree tree = null;
        try {
            if (sendTree) {
                tree = new DITree(1023);
            }

            SendPort sport = ibis.createSendPort(t, "send port");
            sport.connect(master, "receive port");

            System.err.println(this
                    + ": Connection established -- I'm a Sender");
            long totalTime = System.currentTimeMillis();

            for (int r = 0; r < repeat; r++) {

                long time = System.currentTimeMillis();

                for (int i = 0; i < count; i++) {
                    WriteMessage writeMessage = sport.newMessage();
                    ConcurrentSenders.logger.debug("LAT: send message");
                    if (sendTree) {
                        writeMessage.writeObject(tree);
                    } else {
                        writeMessage.writeObject("total world domination");
                    }

                    ConcurrentSenders.logger.debug("LAT: finish message");
                    writeMessage.finish();
                    ConcurrentSenders.logger.debug("LAT: message done");
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

class Receiver implements MessageUpcall {
    int count;

    int repeat;

    boolean done = false;

    boolean doFinish;

    Ibis ibis;

    CapabilitySet t;

    int msgs = 0;

    int senders;

    Receiver(Ibis ibis, CapabilitySet t, int count, int repeat, int senders,
            boolean doFinish) {
        this.ibis = ibis;
        this.t = t;
        this.count = count;
        this.repeat = repeat;
        this.senders = senders;
        System.err.println(this + ": I'm a Receiver");

        try {
            ReceivePort rport = ibis.createReceivePort(t, "receive port", this);
            rport.enableConnections();

            long time = System.currentTimeMillis();
            rport.enableMessageUpcalls();
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

class ConcurrentSenders implements PredefinedCapabilities {

    static Ibis ibis;

    static Logger logger = Logger.getLogger(ConcurrentSenders.class.getName());

    static Registry registry;

    static IbisIdentifier master;

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
            CapabilitySet sp = new CapabilitySet(SERIALIZATION_OBJECT,
                    WORLDMODEL_CLOSED, COMMUNICATION_RELIABLE,
                    RECEIVE_AUTO_UPCALLS, RECEIVE_EXPLICIT,
                    CONNECTION_ONE_TO_ONE, CONNECTION_ONE_TO_MANY);

            ibis = IbisFactory.createIbis(sp, null, null, null);

            registry = ibis.registry();

            logger.debug("LAT: pre elect");
            master = registry.elect("latency");
            logger.debug("LAT: post elect");

            if (master.equals(ibis.identifier())) {
                logger.debug("LAT: I am master");
                rank = 0;
            } else {
                logger.debug("LAT: I am slave");
                rank = 1;
            }

            CapabilitySet t = sp;

            if (rank == 0) {
                new Receiver(ibis, t, count, repeat, senders, doFinish);
            } else {
                // start N senders
                for (int i = 0; i < senders; i++) {
                    new Sender(ibis, t, count, repeat, sendTree, master).start();
                }
            }

        } catch (Exception e) {
            System.err.println("Got exception " + e);
            System.err.println("StackTrace:");
            e.printStackTrace();
        }
    }
}
