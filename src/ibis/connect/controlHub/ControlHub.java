/* $Id$ */

package ibis.connect.controlHub;

import ibis.connect.routedMessages.HubProtocol;
import ibis.connect.util.ConnectionProperties;
import ibis.util.TypedProperties;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

/** Incarnates a thread dedicated to HubWire management 
 * towards a given node.
 */
class NodeManager extends Thread {
    private String hostname;

    private HubProtocol.HubWire wire;

    private int hostport;

    private ControlHub hub;

    private int[] nmessages;

    private static final boolean STATS
            = TypedProperties.booleanProperty(ConnectionProperties.hub_stats);

    private static final int np = HubProtocol.getNPacketTypes();

    NodeManager(Socket s, ControlHub hub) throws IOException {
        wire = new HubProtocol.HubWire(s);
        hostname = wire.getPeerName();
        hostport = wire.getPeerPort();
        this.hub = hub;
        nmessages = new int[np + 1];
        for (int i = 0; i <= np; i++) {
            nmessages[i] = 0;
        }
    }

    protected void sendPacket(String host, int port, HubProtocol.HubPacket p)
            throws IOException {
        wire.sendMessage(host, port, p);
    }

    public void run() {
        boolean nodeRunning = true;
        hub.registerNode(hostname, hostport, this);
        while (nodeRunning) {
            try {
                HubProtocol.HubPacket packet = wire.recvPacket();

                int action = packet.getType();
                String destHost = packet.getHost();
                int destPort = packet.getPort();
                boolean send = true;

                nmessages[action]++;

                switch (action) {
                case HubProtocol.GETPORT: {
                    /* packet for the hub itself: obtain port number. */
                    HubProtocol.HubPacketGetPort p 
                            = (HubProtocol.HubPacketGetPort) packet;
                    if (p.proposedPort != 0) {
                        int prt = ControlHub.checkPort(hostname, hostport,
                                p.proposedPort);
                        sendPacket(hostname, hostport,
                                new HubProtocol.HubPacketPutPort(prt));
                    } else {
                        ArrayList a = ControlHub.getPorts(hostname, hostport);
                        sendPacket(hostname, hostport,
                                new HubProtocol.HubPacketPortSet(a));
                    }
                    send = false;
                }
                    break;

                case HubProtocol.PORTSET: {
                    /* packet for the hub itself: release port numbers. */
                    HubProtocol.HubPacketPortSet p
                            = (HubProtocol.HubPacketPortSet) packet;
                    ControlHub.removePort(hostname, hostport, p.portset);
                    send = false;
                }
                    break;

                case HubProtocol.CONNECT: {
                    /* need to figure out a destPort. */
                    HubProtocol.HubPacketConnect p
                            = (HubProtocol.HubPacketConnect) packet;
                    destPort = ControlHub.resolvePort(destHost, p.serverPort);
                    if (destPort == -1) {
                        sendPacket(destHost, destPort,
                                new HubProtocol.HubPacketReject(p.clientPort,
                                        destHost));
                    }
                    break;
                }

                default:
                    break;
                }

                if (send) {
                    NodeManager node = (NodeManager) ControlHub.resolveNode(
                            destHost, destPort);
                    /* packet to forward */
                    if (node == null) {
                        ControlHub.logger.error(
                                "# ControlHub: node not found: " + destHost
                                + ":" + destPort);
                    } else {
                        /* replaces the destination with the sender. */
                        node.sendPacket(hostname, hostport, packet);
                    }
                }
            } catch (EOFException e) {
                ControlHub.logger.error("# ControlHub: EOF detected for "
                        + hostname + ":" + hostport);
                nodeRunning = false;
            } catch (SocketException e) {
                ControlHub.logger.error("# ControlHub: error detected for "
                        + hostname + ":" + hostport + "; wire closed.");
                nodeRunning = false;
            } catch (Exception e) {
                throw new Error(e);
            }
        }
        try {
            wire.close();
        } catch (IOException e) {
            // ignored
        }
        if (STATS) {
            System.err.println("Wire statistics for host " + hostname + ":"
                    + hostport + ":");
            for (int i = 1; i <= np; i++) {
                System.err.println(HubProtocol.getPacketType(i) + ": "
                        + nmessages[i]);
            }
        }
        hub.unregisterNode(hostname, hostport);
    }
}

class Node {
    private int portno;

    private String hostname;

    public Node(int portno, String hostname) {
        this.portno = portno;
        this.hostname = hostname;
    }

    public boolean equals(Object o) {
        if (o instanceof Node) {
            Node po = (Node) o;
            return portno == po.portno && hostname.equals(po.hostname);
        }
        return false;
    }

    public int hashCode() {
        return hostname.hashCode();
    }
}

public class ControlHub extends Thread {
    // Hashtable of Ibisnodes; hash key is canonical hostname of node
    private static Map nodes = new Hashtable();

    private static Map portNodeMap = new Hashtable();

    public static Logger logger = ibis.util.GetLogger.getLogger(ControlHub.class.getName());

    private static int nodesNum = 0;

    public static final int defaultPort = 9828;

    ServerSocket server = null;

    public ControlHub() {
        int port = defaultPort;
        try {
            Properties p = System.getProperties();
            String portString = p.getProperty(ConnectionProperties.hub_port);
            if (portString != null) {
                port = Integer.parseInt(portString);
            } else {
                portString = p.getProperty("ibis.name_server.port");
                if (portString != null) {
                    port = Integer.parseInt(portString) + 2;
                }
            }

            server = new ServerSocket(port);
            System.err.println("\n# ControlHub: listening on "
                    + InetAddress.getLocalHost().getHostName() + ":"
                    + server.getLocalPort());
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static void showCount() {
        System.err.println("# ControlHub: " + nodesNum
                + " nodes currently connected");
    }

    public void registerNode(String nodename, int nodeport, Object node) {
        System.err.println("# ControlHub: new connection from " + nodename
                + ":" + nodeport);
        nodes.put(new Node(nodeport, nodename.toLowerCase()), node);
        nodesNum++;
        showCount();
    }

    public synchronized void waitForCount(int cnt) {
        while (nodesNum > cnt) {
            try {
                wait();
            } catch (InterruptedException e) {
                // ignored
            }
        }
    }

    public void unregisterNode(String nodename, int nodeport) {
        nodes.remove(new Node(nodeport, nodename.toLowerCase()));
        removeHostPort(nodename, nodeport);
        nodesNum--;
        showCount();
        synchronized (this) {
            this.notifyAll();
        }
    }

    public static Object resolveNode(String nodename, int nodeport) {
        return nodes.get(new Node(nodeport, nodename.toLowerCase()));
    }

    public static int checkPort(String hostname, int hostport, int portno) {
        hostname = hostname.toLowerCase();
        synchronized (portNodeMap) {
            Object o = portNodeMap.get(hostname);
            Hashtable h;
            if (o == null) {
                h = new Hashtable();
                portNodeMap.put(hostname, h);
            } else {
                h = (Hashtable) o;
            }
            if (h.containsKey(new Integer(portno))) {
                logger.debug("# ControlHub: could not give portno " + portno
                        + " to " + hostname + ":" + hostport);
                return -1;
            }
            logger.debug("# ControlHub: giving portno " + portno + " to "
                    + hostname + ":" + hostport);
            h.put(new Integer(portno), new Integer(hostport));
            return portno;
        }
    }

    public static ArrayList getPorts(String hostname, int hostport) {
        hostname = hostname.toLowerCase();
        synchronized (portNodeMap) {
            Object o = portNodeMap.get(hostname);
            Hashtable h;
            if (o == null) {
                h = new Hashtable();
                portNodeMap.put(hostname, h);
            } else {
                h = (Hashtable) o;
            }
            int i = 1;
            ArrayList a = new ArrayList();
            for (int j = 0; j < 10; j++) {
                while (h.containsKey(new Integer(i))) {
                    i++;
                }
                a.add(new Integer(i));
                h.put(new Integer(i), new Integer(hostport));
                logger.debug("# ControlHub: giving portno " + i + " to "
                        + hostname + ":" + hostport);
            }
            return a;
        }
    }

    public static void removePort(String hostname, int hostport, int portno) {
        hostname = hostname.toLowerCase();
        synchronized (portNodeMap) {
            Object o = portNodeMap.get(hostname);
            Hashtable h;
            if (o == null) {
                return;
            }
            h = (Hashtable) o;
            h.remove(new Integer(portno));
            logger.debug("# ControlHub: removing portno " + portno + " of "
                    + hostname + ":" + hostport);
        }
    }

    public static void removePort(String hostname, int hostport,
            ArrayList ports) {
        hostname = hostname.toLowerCase();
        synchronized (portNodeMap) {
            Object o = portNodeMap.get(hostname);
            Hashtable h;
            if (o == null) {
                return;
            }
            h = (Hashtable) o;
            for (int i = 0; i < ports.size(); i++) {
                h.remove(ports.get(i));
                logger.debug("# ControlHub: removing portno "
                        + ((Integer) ports.get(i)).intValue() + " of "
                        + hostname + ":" + hostport);
            }
        }
    }

    public static void removeHostPort(String hostname, int hostport) {
        hostname = hostname.toLowerCase();
        synchronized (portNodeMap) {
            Object o = portNodeMap.get(hostname);
            Hashtable h;
            if (o == null) {
                return;
            }
            h = (Hashtable) o;
            Enumeration keys = h.keys();
            while (keys.hasMoreElements()) {
                Integer i = (Integer) (keys.nextElement());
                Integer v = (Integer) h.get(i);
                if (v.intValue() == hostport) {
                    h.remove(i);
                }
            }
            logger.debug("# ControlHub: removing hostport " + hostport
                    + " of " + hostname);
        }
    }

    public static int resolvePort(String hostname, int portno) {
        hostname = hostname.toLowerCase();
        synchronized (portNodeMap) {
            Object o = portNodeMap.get(hostname);
            Hashtable h;
            if (o == null) {
                logger.error("# ControlHub: could not resolve " + portno
                        + " for host " + hostname);
                return -1;
            }
            h = (Hashtable) o;
            o = h.get(new Integer(portno));
            if (o == null) {
                logger.error("# ControlHub: could not resolve " + portno
                        + " for host " + hostname);
                return -1;
            }
            return ((Integer) o).intValue();
        }
    }

    public void run() {
        try {
            while (true) {
                Socket s = server.accept();
                try {
                    NodeManager node = new NodeManager(s, this);
                    node.start();
                } catch (Exception e) { /* ignore */
                }
            }
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static void main(String[] arg) {
        new ControlHub().run();
    }
}
