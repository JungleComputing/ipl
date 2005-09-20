/* $Id$ */

package ibis.connect.routedMessages;

import ibis.util.IPUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/* On-the-wire protocol management for links between 
 * computing nodes and control hub. Used by the HubLink
 * manager and the ControlHub itself.
 */
public class HubProtocol {
    static Logger logger = ibis.util.GetLogger.getLogger(HubProtocol.class.getName());

    /** Connection request. */
    public static final int CONNECT = 1;

    /** Notification for an accepted connection. */
    public static final int ACCEPT = 2;

    /** Notification for a refused connection. */
    public static final int REJECT = 3;

    /** Data packet. */
    public static final int DATA = 4;

    /** Notification for socket close. */
    public static final int CLOSE = 5;

    /** Get port number from controlhub. */
    public static final int GETPORT = 6;

    /** Received port number from controlhub. */
    public static final int PUTPORT = 7;

    /** A port set. */
    public static final int PORTSET = 8;

    private static final String[] names = new String[] { "", "Connect",
            "Accept", "Reject", "Data", "Close", "Getport", "Putport",
            "PortSet" };

    public static String getPacketType(int i) {
        return names[i];
    }

    public static int getNPacketTypes() {
        return names.length - 1;
    }

    public static class HubWire {
        static Logger logger = ibis.util.GetLogger.getLogger(HubWire.class.getName());
        
        private Socket socket;

        private DataInputStream in;

        private DataOutputStream out;

        private String peerName;

        private int peerPort;

        private String localHostName;

        private int localPort;

        private boolean hubConnected = false;

        public HubWire(Socket s) throws IOException {
            socket = s;
            localHostName = IPUtils.getLocalHostAddress().getHostName();
            localPort = s.getLocalPort();
            out = new DataOutputStream(
                    new BufferedOutputStream(s.getOutputStream(), 4096));
            out.writeUTF(localHostName);
            out.writeInt(s.getLocalPort());
            out.flush();
            in = new DataInputStream(new BufferedInputStream(
                    s.getInputStream(), 4096));
            peerName = (in.readUTF()).toLowerCase();
            peerPort = in.readInt();
            if (logger.isInfoEnabled()) {
                String canonicalPeerName
                    = s.getInetAddress().getCanonicalHostName().toLowerCase();
                String msg = "# HubWire: new hub wire- local: " + localHostName
                        + "; remote: " + peerName;
                if (!canonicalPeerName.equals(peerName)) {
                    msg = msg + " (seen from hub: " + canonicalPeerName + ":"
                            + s.getPort() + ")";
                }
                logger.info(msg);
            }
            hubConnected = true;
        }

        public void close() throws IOException {
            logger.info("# HubWire: closing wire...");
            in.close();
            out.flush();
            out.close();
            socket.close();
            logger.info("# HubWire: closed.");
        }

        public String getPeerName() {
            return peerName;
        }

        public int getPeerPort() {
            return peerPort;
        }

        public String getLocalName() {
            return localHostName;
        }

        public int getLocalPort() {
            return localPort;
        }

        public HubPacket recvPacket() throws IOException {
            HubPacket p = null;
            synchronized (in) {
                int action = in.readInt();
                String host = in.readUTF();
                int port = in.readInt();
                switch (action) {
                case CONNECT:
                    p = HubPacketConnect.recv(in, host, port);
                    break;
                case ACCEPT:
                    p = HubPacketAccept.recv(in, host, port);
                    break;
                case REJECT:
                    p = HubPacketReject.recv(in, host, port);
                    break;
                case DATA:
                    p = HubPacketData.recv(in, host, port);
                    break;
                case CLOSE:
                    p = HubPacketClose.recv(in, host, port);
                    break;
                case PUTPORT:
                    p = HubPacketPutPort.recv(in, host, port);
                    break;
                case PORTSET:
                    p = HubPacketPortSet.recv(in);
                    break;
                case GETPORT:
                    p = HubPacketGetPort.recv(in, host, port);
                    break;
                default:
                    throw new Error("Received unknown type of HubPacket: "
                            + action);
                }
                p.h = host;
                p.p = port;
                if (action != p.getType()) {
                    throw new Error("Internal error, consistency check failed");
                }
            }
            return p;
        }

        public void sendMessage(HubPacket p) throws IOException {
            if (!hubConnected) {
                throw new IOException("hub not connected");
            }
            synchronized (out) {
                out.writeInt(p.getType());
                out.writeUTF(p.h);
                out.writeInt(p.p);
                p.send(out);
                out.flush();
            }
        }

        public void sendMessage(String destHost, int destPort, HubPacket p)
                throws IOException {
            p.h = destHost;
            p.p = destPort;
            sendMessage(p);
        }
    }

    public static abstract class HubPacket {
        protected String h;

        protected int p;

        public String getHost() {
            return h;
        }

        public int getPort() {
            return p;
        }

        abstract public int getType();

        abstract public void send(DataOutputStream out) throws IOException;
    }

    public static class HubPacketConnect extends HubPacket {
        public int serverPort;

        public int clientPort;

        public int getType() {
            return CONNECT;
        }

        HubPacketConnect(int serverPort, int clientPort) {
            this.serverPort = serverPort;
            this.clientPort = clientPort;
        }

        public void send(DataOutputStream out) throws IOException {
            logger.debug("# HubPacketConnect.send()- sending CONNECT to "
                            + "port " + serverPort + " on " + h + ":" + p);
            out.writeInt(serverPort);
            out.writeInt(clientPort);
        }

        static public HubPacket recv(DataInputStream in, String h, int p)
                throws IOException {
            int serverPort = in.readInt();
            int clientPort = in.readInt();
            logger.debug("# HubPacketConnect.recv()- got CONNECT to "
                            + "port " + serverPort + " from " + h + ":" + p);
            return new HubPacketConnect(serverPort, clientPort);
        }
    }

    public static class HubPacketAccept extends HubPacket {
        public int clientPort;

        public String serverHost;

        public int servantPort;

        public int getType() {
            return ACCEPT;
        }

        HubPacketAccept(int clientPort, String serverHost, int servantPort) {
            this.clientPort = clientPort;
            this.serverHost = serverHost;
            this.servantPort = servantPort;
        }

        public void send(DataOutputStream out) throws IOException {
            logger.debug("# HubPacketAccept.send()- sending ACCEPT to "
                            + "port " + clientPort + " on " + h + ":" + p);
            out.writeInt(clientPort);
            out.writeUTF(serverHost);
            out.writeInt(servantPort);
        }

        static public HubPacket recv(DataInputStream in, String h, int p)
                throws IOException {
            int clientPort = in.readInt();
            String serverHost = in.readUTF();
            int servantPort = in.readInt();
            logger.debug("# HubPacketAccept.recv()- got ACCEPT to port "
                    + servantPort + " from " + h + ":" + p);
            return new HubPacketAccept(clientPort, serverHost, servantPort);
        }
    }

    public static class HubPacketReject extends HubPacket {
        public int clientPort;

        public String serverHost;

        public int getType() {
            return REJECT;
        }

        public HubPacketReject(int clientPort, String serverHost) {
            this.clientPort = clientPort;
            this.serverHost = serverHost;
        }

        public void send(DataOutputStream out) throws IOException {
            logger.debug("# HubPacketReject.send()- sending REJECT to "
                            + "port " + clientPort + " on " + h + ":" + p);
            out.writeInt(clientPort);
            out.writeUTF(serverHost);
        }

        static public HubPacket recv(DataInputStream in, String h, int p)
                throws IOException {
            int clientPort = in.readInt();
            String serverHost = in.readUTF();
            logger.debug("# HubPacketReject.recv()- got REJECT to port "
                    + clientPort + " from " + h + ":" + p);
            return new HubPacketReject(clientPort, serverHost);
        }
    }

    public static class HubPacketGetPort extends HubPacket {
        public int proposedPort;

        public int getType() {
            return GETPORT;
        }

        HubPacketGetPort(int port) {
            proposedPort = port;
        }

        public void send(DataOutputStream out) throws IOException {
            logger.debug("# HubPacketGetPort.send()- sending GETPORT to "
                    + h + ":" + p);
            out.writeInt(proposedPort);
        }

        static public HubPacket recv(DataInputStream in, String h, int p)
                throws IOException {
            int port = in.readInt();
            logger.debug("# HubPacketGetPort.recv()- got GETPORT of "
                    + "port " + port + " from " + h + ":" + p);
            return new HubPacketGetPort(port);
        }
    }

    public static class HubPacketPutPort extends HubPacket {
        public int resultPort;

        public int getType() {
            return PUTPORT;
        }

        public HubPacketPutPort(int port) {
            resultPort = port;
        }

        public void send(DataOutputStream out) throws IOException {
            logger.debug("# HubPacketPutPort.send()- sending PUTPORT of "
                            + resultPort + " to " + h + ":" + p);
            out.writeInt(resultPort);
        }

        static public HubPacket recv(DataInputStream in, String h, int p)
                throws IOException {
            int port = in.readInt();
            logger.debug("# HubPacketPutPort.recv()- got PUTPORT of "
                    + "port " + port + " from " + h + ":" + p);
            return new HubPacketPutPort(port);
        }
    }

    public static class HubPacketPortSet extends HubPacket {
        public ArrayList portset;

        public int getType() {
            return PORTSET;
        }

        public HubPacketPortSet(ArrayList ports) {
            portset = ports;
        }

        public void send(DataOutputStream out) throws IOException {
            logger.debug("# HubPacketPortSet.send()- sending PORTSET");
            out.writeInt(portset.size());
            for (int i = 0; i < portset.size(); i++) {
                out.writeInt(((Integer) portset.get(i)).intValue());
            }
        }

        static public HubPacket recv(DataInputStream in) throws IOException {
            ArrayList portset = new ArrayList();
            int n = in.readInt();
            logger.debug("# HubPacketPortSet.recv()- got PORTSET");
            for (int i = 0; i < n; i++) {
                portset.add(new Integer(in.readInt()));
            }
            return new HubPacketPortSet(portset);
        }
    }

    public static class HubPacketData extends HubPacket {
        int port;

        int senderport;

        byte[] b;

        public int getType() {
            return DATA;
        }

        HubPacketData(int port, byte[] b, int sport) {
            this.port = port;
            this.b = b;
            senderport = sport;
        }

        public void send(DataOutputStream out) throws IOException {
            logger.debug("# HubPacketData.send()- sending- port=" + port
                    + "; size=" + b.length + " to " + h + ":" + p);
            out.writeInt(port);
            out.writeInt(senderport);
            out.writeInt(b.length);
            out.write(b);
        }

        static public HubPacket recv(DataInputStream in, String h, int p)
                throws IOException {
            int port = in.readInt();
            int sport = in.readInt();
            int len = in.readInt();
            byte[] b = new byte[len];
            in.readFully(b);
            logger.debug("# HubPacketData.recv()- got DATA - port="
                    + port + "; size=" + b.length + " from " + h + ":" + p);
            return new HubPacketData(port, b, sport);
        }
    }

    public static class HubPacketClose extends HubPacket {
        public int closePort;

        public int localPort;

        public int getType() {
            return CLOSE;
        }

        HubPacketClose(int port, int lport) {
            closePort = port;
            localPort = lport;
        }

        public void send(DataOutputStream out) throws IOException {
            logger.debug("# HubPacketConnect.send()- sending CLOSE of "
                    + "port " + closePort + " to " + h + ":" + p);
            out.writeInt(closePort);
            out.writeInt(localPort);
        }

        static public HubPacket recv(DataInputStream in, String h, int p)
                throws IOException {
            int port = in.readInt();
            int lport = in.readInt();
            logger.debug("# HubPacketConnect.recv()- got CLOSE of port "
                    + port + " from " + h + ":" + p);
            return new HubPacketClose(port, lport);
        }
    }
}
