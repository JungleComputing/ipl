/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.impl.nameServer.NSProps;
import ibis.io.DummyInputStream;
import ibis.io.DummyOutputStream;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.Ibis;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisRuntimeException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.StaticProperties;
import ibis.util.IPUtils;
import ibis.util.IbisSocketFactory;
import ibis.util.RunProcess;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.apache.log4j.Logger;

public class NameServerClient extends ibis.impl.nameServer.NameServer
        implements Runnable, Protocol {

    private PortTypeNameServerClient portTypeNameServerClient;

    private ReceivePortNameServerClient receivePortNameServerClient;

    private ElectionClient electionClient;

    private ServerSocket serverSocket;

    private Ibis ibisImpl;

    private IbisIdentifier id;

    private volatile boolean stop = false;

    private InetAddress serverAddress;

    private String server;

    private int port;

    private String poolName;

    private InetAddress myAddress;

    static IbisSocketFactory socketFactory = IbisSocketFactory.createFactory();

    private static Logger logger
            = ibis.util.GetLogger.getLogger(NameServerClient.class.getName());

    static Socket nsConnect(InetAddress dest, int port, InetAddress me,
            boolean verbose, int timeout) throws IOException {
        Socket s = null;
        int cnt = 0;
        while (s == null) {
            try {
                cnt++;
                s = socketFactory.createSocket(dest, port, me, -1);
            } catch (ConnectionTimedOutException e) {
                if (cnt == 10 && verbose) {
                    // Rather arbitrary, 10 seconds, print warning
                    System.err.println("Nameserver client failed"
                            + " to connect to nameserver\n at " + dest
                            + ":" + port + ", will keep trying");
                } else if (cnt == timeout) {
                    if (verbose) {
                        System.err.println("Nameserver client failed"
                                + " to connect to nameserver\n at "
                                + dest + ":" + port);
                        System.err.println("Gave up after " + timeout
                                + " seconds");
                    }
                    throw e;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e2) {
                    // don't care
                }
            }
        }
        return s;
    }

    public NameServerClient() {
        /* do nothing */
    }

    void runNameServer(int port) {
        if (System.getProperty("os.name").matches(".*indows.*")) {
            // The code below does not work for windows2000, don't know why ...
            NameServer n = NameServer.createNameServer(true, false, false,
                    false, false);
            if (n != null) {
                n.setDaemon(true);
                n.start();
            }
        } else {
            // Start the nameserver in a separate jvm, so that it can keep
            // on running if this particular ibis instance dies.
            String javadir = System.getProperty("java.home");
            String javapath = System.getProperty("java.class.path");
            String filesep = System.getProperty("file.separator");
            String pathsep = System.getProperty("path.separator");

            if (javadir.endsWith("jre")) {
                javadir = javadir.substring(0, javadir.length()-4);
            }

            final String[] cmd = new String[] {
                    javadir + filesep + "bin" + filesep + "java",
                    "-classpath",
                    javapath + pathsep,
                    "-Dibis.name_server.port="+port,
                    "ibis.impl.nameServer.tcp.NameServer",
                    "-single",
                    "-no-retry",
                    "-silent",
                    "-no-poolserver"};

            Thread p = new Thread("NameServer starter") {
                public void run() {
                    RunProcess p = new RunProcess(cmd, new String[0]);
                }
            };

            p.setDaemon(true);
            p.start();
        }
    }

    protected void init(Ibis ibis) throws IOException,
            IbisConfigurationException {
        this.ibisImpl = ibis;
        this.id = ibisImpl.identifier();

        Properties p = System.getProperties();

        myAddress = IPUtils.getAlternateLocalHostAddress();

        server = p.getProperty(NSProps.s_host);
        if (server == null) {
            throw new IbisConfigurationException(
                    "property ibis.name_server.host is not specified");
        }

        if (server.equals("localhost")) {
            server = myAddress.getHostName();
        }

        poolName = p.getProperty(NSProps.s_key);
        if (poolName == null) {
            throw new IbisConfigurationException(
                    "property ibis.name_server.key is not specified");
        }

        String nameServerPortString = p.getProperty(NSProps.s_port);
        port = NameServer.TCP_IBIS_NAME_SERVER_PORT_NR;
        if (nameServerPortString != null) {
            try {
                port = Integer.parseInt(nameServerPortString);
                logger.debug("Using nameserver port: " + port);
            } catch (Exception e) {
                System.err.println("illegal nameserver port: "
                        + nameServerPortString + ", using default");
            }
        }

        serverAddress = InetAddress.getByName(server);

        if (myAddress.equals(serverAddress)) {
            // Try and start a nameserver ...
            runNameServer(port);
        }

        logger.debug("Found nameServerInet " + serverAddress);

        Socket s = nsConnect(serverAddress, port, myAddress, true, 60);

        serverSocket = socketFactory.createServerSocket(0, myAddress, true);

//	System.err.println("nsclient: serversocket port = " + serverSocket.getLocalPort());

        DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
        ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(dos));

        logger.debug("NameServerClient: contacting nameserver");
        out.writeByte(IBIS_JOIN);
        out.writeUTF(poolName);
        out.writeObject(id);
        out.writeObject(myAddress);
        out.writeInt(serverSocket.getLocalPort());
        out.flush();

        DummyInputStream di = new DummyInputStream(s.getInputStream());
        ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(di));

        int opcode = in.readByte();

        logger.debug("NameServerClient: nameserver reply, opcode " + opcode);

        switch (opcode) {
        case IBIS_REFUSED:
            socketFactory.close(in, out, s);
            throw new ConnectionRefusedException("NameServerClient: "
                    + id + " is not unique!");
        case IBIS_ACCEPTED:
            // read the ports for the other name servers and start the
            // receiver thread...
            int temp = in.readInt(); /* Port for the PortTypeNameServer */
            portTypeNameServerClient = new PortTypeNameServerClient(myAddress,
                    serverAddress, temp);

            temp = in.readInt(); /* Port for the ReceivePortNameServer */
            receivePortNameServerClient = new ReceivePortNameServerClient(
                    myAddress, serverAddress, temp);

            temp = in.readInt(); /* Port for the ElectionServer */
            electionClient = new ElectionClient(myAddress, serverAddress, temp);

            int poolSize = in.readInt();

            logger.debug("NameServerClient: accepted by nameserver, "
                        + "poolsize " + poolSize);

            for (int i = 0; i < poolSize; i++) {
                IbisIdentifier newid;
                try {
                    newid = (IbisIdentifier) in.readObject();
                } catch (ClassNotFoundException e) {
                    throw new IOException("Receive IbisIdent of unknown class "
                            + e);
                }
                logger.debug("NameServerClient: join of " + newid);
                ibisImpl.joined(newid);
                logger.debug("NameServerClient: join of " + newid + " DONE");
            }

            // at least read the tobedeleted stuff!
            int tmp = in.readInt();
            for (int i = 0; i < tmp; i++) {
                try {
                    in.readObject();
                } catch (ClassNotFoundException e) {
                    throw new IOException("Receive IbisIdent of unknown class "
                            + e);
                }
            }

            // Should we join ourselves?
            ibisImpl.joined(id);

            socketFactory.close(in, out, s);
            Thread t = new Thread(this, "NameServerClient accept thread");
            t.setDaemon(true);
            t.start();
            break;
        default:
            socketFactory.close(in, out, s);

            throw new StreamCorruptedException(
                    "NameServerClient: got illegal opcode " + opcode);
        }
    }

    public void maybeDead(IbisIdentifier ibisId) throws IOException {
        Socket s;
        try {
            s = socketFactory.createSocket(serverAddress, port, myAddress, -1);
        } catch (ConnectionTimedOutException e) {
            // Apparently, the nameserver left. Assume dead.
            return;
        }

        DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
        ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(dos));

        out.writeByte(IBIS_ISALIVE);
        out.writeUTF(poolName);
        out.writeObject(ibisId);
        out.flush();
        logger.debug("NS client: isAlive sent");

        socketFactory.close(null, out, s);
    }

    public void dead(IbisIdentifier corpse) throws IOException {
        Socket s;
        try {
            s = socketFactory.createSocket(serverAddress, port, myAddress, -1);
        } catch (ConnectionTimedOutException e) {
            return;
        }

        DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
        ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(dos));

        out.writeByte(IBIS_DEAD);
        out.writeUTF(poolName);
        out.writeObject(corpse);
        logger.debug("NS client: kill sent");

        socketFactory.close(null, out, s);
    }

    public boolean newPortType(String name, StaticProperties p)
            throws IOException {
        return portTypeNameServerClient.newPortType(name, p);
    }

    public long getSeqno(String name) throws IOException {
        return portTypeNameServerClient.getSeqno(name);
    }

    public void leave() {
        Socket s;
        logger.debug("NS client: leave");

        try {
            s = socketFactory.createSocket(serverAddress, port, myAddress,
                    5000);
        } catch (IOException e) {
            // Apparently, the nameserver left.
            return;
        }

        try {
            DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
            ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(dos));

            out.writeByte(IBIS_LEAVE);
            out.writeUTF(poolName);
            out.writeObject(id);
            out.flush();
            logger.debug("NS client: leave sent");

            DummyInputStream di = new DummyInputStream(s.getInputStream());
            ObjectInputStream in = new ObjectInputStream(
                    new BufferedInputStream(di));

            in.readByte();
            logger.debug("NS client: leave ack received");
            socketFactory.close(in, out, s);
        } catch (IOException e) {
            // ignored
        }

        logger.debug("NS client: leave DONE");

    }

    public void run() {
        logger.debug("NameServerClient: stread started");

        while (true) { // !stop

            Socket s;
            IbisIdentifier ibisId;

            try {
                s = socketFactory.accept(serverSocket);

                logger.debug("NameServerClient: incoming connection "
                        + "from " + s.toString());

            } catch (Exception e) {
                if (stop) {
                    logger.debug("NameServerClient: thread dying");
                    try {
                        serverSocket.close();
                    } catch (IOException e1) {
                        /* do nothing */
                    }
                    return;
                }
                throw new IbisRuntimeException(
                        "NameServerClient: got an error", e);
            }

            int opcode = 666;

            try {
                DummyInputStream di = new DummyInputStream(s.getInputStream());
                ObjectInputStream in = new ObjectInputStream(
                        new BufferedInputStream(di));

                opcode = in.readByte();
                logger.debug("NameServerClient: opcode " + opcode);

                switch (opcode) {
                case (IBIS_PING): {
                    DummyOutputStream dos
                            = new DummyOutputStream(s.getOutputStream());
                    DataOutputStream out = new DataOutputStream(
                            new BufferedOutputStream(dos));
                    out.writeUTF(poolName);
                    socketFactory.close(in, out, s);
                }
                    break;
                case (IBIS_JOIN):
                    ibisId = (IbisIdentifier) in.readObject();
                    logger.debug("NameServerClient: receive join request "
                            + ibisId);
                    socketFactory.close(in, null, s);
                    ibisImpl.joined(ibisId);
                    break;
                case (IBIS_LEAVE):
                    ibisId = (IbisIdentifier) in.readObject();
                    socketFactory.close(in, null, s);
                    if (ibisId.equals(this.id)) {
                        // received an ack from the nameserver that I left.
                        logger.debug("NameServerClient: thread dying");
                        return;
                    }
                    ibisImpl.left(ibisId);
                    break;
                case (IBIS_DEAD):
                    IbisIdentifier[] ids = (IbisIdentifier[]) in.readObject();
                    socketFactory.close(in, null, s);
                    ibisImpl.died(ids);

                    break;
                default:
                    System.out.println("NameServerClient: got an illegal "
                            + "opcode " + opcode);
                }
            } catch (Exception e1) {
                System.out.println("Got an exception in NameServerClient.run "
                        + "(opcode = " + opcode + ") " + e1.toString());
                if (stop) {
                    return;
                }
                e1.printStackTrace();

                if (s != null) {
                    socketFactory.close(null, null, s);
                }

            }
        }
    }

    public ReceivePortIdentifier lookupReceivePort(String name)
            throws IOException {
        return lookupReceivePort(name, 0);
    }

    public ReceivePortIdentifier lookupReceivePort(String name, long timeout)
            throws IOException {
        return receivePortNameServerClient.lookup(name, timeout);
    }

    public IbisIdentifier lookupIbis(String name) {
        return lookupIbis(name, 0);
    }

    public IbisIdentifier lookupIbis(String name, long timeout) {
        /* not implemented yet */
        return null;
    }

    public ReceivePortIdentifier[] listReceivePorts(IbisIdentifier ident) {
        /* not implemented yet */
        return new ReceivePortIdentifier[0];
    }

    public IbisIdentifier elect(String election) throws IOException,
            ClassNotFoundException {
        return (IbisIdentifier) electionClient.elect(election, id);
    }

    public IbisIdentifier getElectionResult(String election)
            throws IOException, ClassNotFoundException {
        return (IbisIdentifier) electionClient.elect(election, null);
    }

    //gosia	

    public void bind(String name, ReceivePortIdentifier rpi)
            throws IOException {
        receivePortNameServerClient.bind(name, rpi);
    }

    public void rebind(String name, ReceivePortIdentifier rpi)
            throws IOException {
        receivePortNameServerClient.rebind(name, rpi);
    }

    public void unbind(String name) throws IOException {
        receivePortNameServerClient.unbind(name);
    }

    public String[] listNames(String pattern) throws IOException {
        return receivePortNameServerClient.list(pattern);
    }
    //end gosia
}
