/* $Id$ */

package ibis.connect.routedMessages;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * HubLink manages the link with the control hub.
 */
public class HubLink extends Thread {

    static Logger logger = ibis.util.GetLogger.getLogger(HubLink.class.getName());

    private HubProtocol.HubWire wire;

    public final String localHostName;

    public final int localPort;

    private boolean hubRunning = true;

    private Map serverSockets = new Hashtable();

    private Map connectedSockets = new Hashtable();

    ArrayList available_ports = new ArrayList();

    private boolean newPortBusy = false;

    private int portnum = -2;

    protected synchronized int getPort() throws IOException {
        if (available_ports.size() != 0) {
            return ((Integer) available_ports.remove(0)).intValue();
        }
        while (newPortBusy) {
            try {
                wait();
            } catch (Exception e) {
                // ignored
            }
        }
        newPortBusy = true;

        sendPacket("", 0, new HubProtocol.HubPacketGetPort(0));

        while (available_ports.size() == 0) {
            try {
                wait();
            } catch (Exception e) {
                // ignored
            }
        }
        newPortBusy = false;
        notifyAll();
        return ((Integer) available_ports.remove(0)).intValue();
    }

    protected synchronized int newPort(int port) throws IOException {
        if (port == 0) {
            return getPort();
        }
        while (newPortBusy) {
            try {
                wait();
            } catch (Exception e) {
                // ignored
            }
        }

        newPortBusy = true;
        portnum = -2;

        sendPacket("", 0, new HubProtocol.HubPacketGetPort(port));

        while (portnum == -2) {
            try {
                wait();
            } catch (Exception e) {
                // ignored
            }
        }

        newPortBusy = false;
        notifyAll();

        if (portnum == -1) {
            throw new IOException("Port number already in use");
        }

        port = portnum;
        portnum = -2;
        return port;
    }

    /*
     * ServerSocket list management
     */
    protected synchronized void addServer(RoutedMessagesServerSocket s, int port) {
        serverSockets.put(new Integer(port), s);
    }

    protected synchronized void removeServer(int port) {
        serverSockets.remove(new Integer(port));
    }

    private synchronized RoutedMessagesServerSocket resolveServer(int port) {
        RoutedMessagesServerSocket s = (RoutedMessagesServerSocket) serverSockets
                .get(new Integer(port));
        if (s == null) {
            throw new Error("HubLink: bad server- port=" + port);
        }
        return s;
    }

    /*
     * Socket list management
     */
    protected synchronized void addSocket(RoutedMessagesSocket s, int port) {
        connectedSockets.put(new Integer(port), s);
    }

    protected synchronized void removeSocket(int port) {
        Integer m = new Integer(port);
        connectedSockets.remove(m);
        available_ports.add(m);
    }

    private synchronized RoutedMessagesSocket resolveSocket(int port)
            throws IOException {
        RoutedMessagesSocket s = (RoutedMessagesSocket) connectedSockets
                .get(new Integer(port));
        if (s == null) {
            throw new IOException("HubLink: bad socket- port=" + port);
        }
        return s;
    }

    public HubLink(String host, int port) throws IOException {
        logger.debug("# HubLink()");
        Socket s = new Socket(host, port);
        wire = new HubProtocol.HubWire(s);
        localHostName = wire.getLocalName();
        localPort = wire.getLocalPort();
        logger.debug("# HubLink() done.");
    }

    protected synchronized void stopHub() {
        if (hubRunning) {
            hubRunning = false;
            try {
                wire.close();
            } catch (IOException e) { /* discard exception */
            }
        }
    }

    protected synchronized void sendPacket(String destHost, int destPort,
            HubProtocol.HubPacket packet) throws IOException {
        wire.sendMessage(destHost, destPort, packet);
    }

    public void run() {
        while (hubRunning) {
            try {
                HubProtocol.HubPacket packet = wire.recvPacket();
                int action = packet.getType();
                switch (action) {
                case HubProtocol.CONNECT: {
                    HubProtocol.HubPacketConnect p = (HubProtocol.HubPacketConnect) packet;
                    logger.debug("# HubLink.run()- Got CONNECT for " + "host="
                            + localHostName + ":" + localPort + "; port="
                            + p.serverPort + "; from host=" + p.getHost()
                            + "; port=" + p.clientPort);
                    RoutedMessagesServerSocket s = resolveServer(p.serverPort);
                    if (s != null) {
                        // AD: TODO- investigate concurrency in CONNECT/ACCEPT
                        // Ceriel: Done.
                        s
                                .enqueueConnect(p.getHost(), p.clientPort, p
                                        .getPort());
                    } else {
                        logger.debug("# HubLink.run()- Got CONNECT "
                                + "for non-existingh port!");
                    }
                }
                    break;
                case HubProtocol.ACCEPT: {
                    HubProtocol.HubPacketAccept p = (HubProtocol.HubPacketAccept) packet;
                    logger.debug("# HubLink.run()- Got ACCEPT for "
                            + "clientPort=" + p.clientPort
                            + " from serverHost=" + p.serverHost
                            + "; servantPort=" + p.servantPort);
                    try {
                        RoutedMessagesSocket s = resolveSocket(p.clientPort);
                        s.enqueueAccept(p.servantPort, p.getPort());
                    } catch (Exception e) {
                        /*
                         * Exception may be discarded (socket has been closed
                         * while the CONNECT/ACCEPT were on the wire). Trace it
                         * anyway for pathologic behavior diagnosis.
                         */
                        logger.debug("# HubLink.run()- exception while "
                                + "resolving socket for ACCEPT!");
                    }
                }
                    break;
                case HubProtocol.REJECT: {
                    HubProtocol.HubPacketReject p = (HubProtocol.HubPacketReject) packet;
                    logger.debug("# HubLink.run()- Got REJECT for " + "port "
                            + p.clientPort + " from host " + p.serverHost);
                    try {
                        RoutedMessagesSocket s = resolveSocket(p.clientPort);
                        s.enqueueReject();
                    } catch (Exception e) { /* ignore */
                    }
                }
                    break;
                case HubProtocol.DATA: {
                    HubProtocol.HubPacketData p = (HubProtocol.HubPacketData) packet;
                    logger.debug("# HubLink.run()- Got DATA for " + "port = "
                            + p.port);
                    try {
                        RoutedMessagesSocket s = resolveSocket(p.port);
                        if (s.remotePort == p.senderport) {
                            s.enqueueFragment(p.b);
                        } else {
                            logger.debug("# HubLink.run()- Got "
                                    + "DATA on closed connection, port = "
                                    + p.port + ", sender = " + p.h + ":"
                                    + p.senderport);
                        }
                    } catch (Exception e) {
                        logger.debug("# HubLink.run()- Got DATA on "
                                + "closed connection, port = " + p.port
                                + ", sender = " + p.h + ":" + p.senderport);
                    }
                }
                    break;
                case HubProtocol.CLOSE: {
                    HubProtocol.HubPacketClose p = (HubProtocol.HubPacketClose) packet;
                    logger.debug("# HubLink.run()- Got CLOSE for " + "port = "
                            + p.closePort);
                    try {
                        RoutedMessagesSocket s = resolveSocket(p.closePort);
                        if (p.h.equals(s.remoteHostname)
                                && s.remotePort == p.localPort) {
                            s.enqueueClose();
                        } else {
                            logger.debug("# HubLink.run()- Got CLOSE of "
                                    + "old connection");
                        }
                    } catch (IOException e) { /* ignore */
                    }
                }
                    break;
                case HubProtocol.PUTPORT: {
                    HubProtocol.HubPacketPutPort p = (HubProtocol.HubPacketPutPort) packet;
                    synchronized (this) {
                        logger.debug("# HubLink.run()- Got PUTPORT " + "= "
                                + p.resultPort);
                        portnum = p.resultPort;
                        notifyAll();
                    }
                }
                    break;
                case HubProtocol.PORTSET: {
                    HubProtocol.HubPacketPortSet p = (HubProtocol.HubPacketPortSet) packet;
                    synchronized (this) {
                        logger.debug("# HubLink.run()- Got PORTSET");
                        available_ports = p.portset;
                        notifyAll();
                    }
                }
                    break;
                default:
                    logger.debug("# Got unknown action: " + action);
                    throw new Error("HubLink: bad data");
                }
            } catch (EOFException e) {
                System.out.println("# HubLink: EOF detected- exiting.");
                hubRunning = false;
            } catch (SocketException e) {
                System.out.println("# HubLink: Socket closed- exiting.");
                hubRunning = false;
            } catch (Exception e) {
                System.out.println("# HubLink: unexpected exception.");
                hubRunning = false;
                throw new Error(e);
            }
        }
    }
}